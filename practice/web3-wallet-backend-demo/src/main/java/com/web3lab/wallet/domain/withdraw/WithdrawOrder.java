package com.web3lab.wallet.domain.withdraw;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 提现订单实体，对应用户发起的出金申请。
 */
@Data
@TableName("withdraw_order")
public class WithdrawOrder {

    /**
     * 提现订单主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台用户 ID。
     */
    private Long userId;

    /**
     * 链编码，例如 ETH_SEPOLIA。
     */
    private String chain;

    /**
     * 币种符号，例如 USDT。
     */
    private String tokenSymbol;

    /**
     * 用户提现目标地址。
     */
    private String toAddress;

    /**
     * 提现金额。
     */
    private BigDecimal amount;

    /**
     * 手续费金额。
     */
    private BigDecimal fee;

    /**
     * 客户端请求号，用于申请幂等。
     */
    private String requestNo;

    /**
     * 提现订单状态，当前阶段见 {@link WithdrawStatusConstants}。
     */
    private String status;

    /**
     * 审核状态，当前阶段见 {@link WithdrawReviewStatusConstants}。
     */
    private String reviewStatus;

    /**
     * 审核人。
     */
    private String reviewBy;

    /**
     * 审核时间。
     */
    private LocalDateTime reviewTime;

    /**
     * 链上交易哈希。
     */
    private String txHash;

    /**
     * 广播使用的链上 nonce。
     */
    private Long nonce;

    /**
     * 广播失败后已自动重试的次数。
     */
    private Integer broadcastRetryCount;

    /**
     * 回执超时后已打回待广播重试的次数。
     */
    private Integer receiptCheckRetryCount;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 记录创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建待审核提现订单。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param toAddress 提现目标地址
     * @param amount 提现金额
     * @param fee 手续费金额
     * @param requestNo 客户端请求号
     * @return 待审核提现订单
     */
    public static WithdrawOrder createPendingReview(Long userId, String chain, String tokenSymbol, String toAddress,
                                                    BigDecimal amount, BigDecimal fee, String requestNo) {
        WithdrawOrder withdrawOrder = new WithdrawOrder();
        withdrawOrder.setUserId(userId);
        withdrawOrder.setChain(chain);
        withdrawOrder.setTokenSymbol(tokenSymbol);
        withdrawOrder.setToAddress(toAddress);
        withdrawOrder.setAmount(amount);
        withdrawOrder.setFee(fee);
        withdrawOrder.setRequestNo(requestNo);
        withdrawOrder.setStatus(WithdrawStatusConstants.PENDING_REVIEW);
        withdrawOrder.setReviewStatus(WithdrawReviewStatusConstants.PENDING);
        withdrawOrder.setBroadcastRetryCount(0);
        withdrawOrder.setReceiptCheckRetryCount(0);
        return withdrawOrder;
    }

    /**
     * 推进为审核通过、待广播状态。
     *
     * @param reviewBy 审核人
     */
    public void approve(String reviewBy) {
        this.status = WithdrawStatusConstants.PENDING_BROADCAST;
        this.reviewStatus = WithdrawReviewStatusConstants.APPROVED;
        this.reviewBy = reviewBy;
        this.reviewTime = LocalDateTime.now();
    }

    /**
     * 推进为审核拒绝状态。
     *
     * @param reviewBy 审核人
     * @param rejectReason 拒绝原因
     */
    public void reject(String reviewBy, String rejectReason) {
        this.status = WithdrawStatusConstants.REJECTED;
        this.reviewStatus = WithdrawReviewStatusConstants.REJECTED;
        this.reviewBy = reviewBy;
        this.reviewTime = LocalDateTime.now();
        this.failReason = rejectReason;
    }

    /**
     * 标记为已提交链上广播。
     *
     * @param txHash 链上交易哈希
     * @param nonce 广播使用的链上 nonce
     */
    public void markBroadcastSubmitted(String txHash, Long nonce) {
        this.status = WithdrawStatusConstants.BROADCAST_SUBMITTED;
        this.txHash = txHash;
        this.nonce = nonce;
        this.failReason = null;
    }

    /**
     * 标记为链上执行成功。
     */
    public void markSuccess() {
        this.status = WithdrawStatusConstants.SUCCESS;
        this.failReason = null;
    }

    /**
     * 标记为链上执行失败。
     *
     * @param failReason 失败原因
     */
    public void markFailed(String failReason) {
        this.status = WithdrawStatusConstants.FAILED;
        this.failReason = failReason;
    }

    /**
     * 预留广播 nonce。
     *
     * <p>广播前先把 nonce 记录进订单，能够让任务在重试时继续复用同一个 nonce，
     * 避免多次重试过程中反复分配新 nonce 导致顺序错乱。</p>
     *
     * @param nonce 本次预留的链上 nonce
     */
    public void reserveNonce(Long nonce) {
        this.nonce = nonce;
    }

    /**
     * 记录本次广播失败原因，并保留在待广播状态等待下次重试。
     *
     * @param failReason 失败原因
     */
    public void markBroadcastRetryableFailure(String failReason) {
        this.status = WithdrawStatusConstants.PENDING_BROADCAST;
        this.failReason = failReason;
        this.broadcastRetryCount = currentBroadcastRetryCount() + 1;
    }

    /**
     * 记录广播失败并转人工处理。
     *
     * @param failReason 转人工原因
     */
    public void markBroadcastManualHandleRequired(String failReason) {
        this.status = WithdrawStatusConstants.MANUAL_HANDLE_REQUIRED;
        this.failReason = failReason;
        this.broadcastRetryCount = currentBroadcastRetryCount() + 1;
    }

    /**
     * 记录“回执长时间未返回”的可重试异常，并打回待广播状态。
     *
     * <p>当前 Day24 第一版不会直接清空已记录的 `txHash`，目的是保留上一轮广播痕迹，
     * 便于后续人工排障；下一轮真正重新广播成功后，会用新的链上哈希覆盖这里的值。</p>
     *
     * @param failReason 超时说明
     */
    public void markReceiptTimeoutRetryable(String failReason) {
        this.status = WithdrawStatusConstants.PENDING_BROADCAST;
        this.failReason = failReason;
        this.receiptCheckRetryCount = currentReceiptCheckRetryCount() + 1;
    }

    /**
     * 标记为需要人工处理。
     *
     * @param failReason 转人工原因
     */
    public void markManualHandleRequired(String failReason) {
        this.status = WithdrawStatusConstants.MANUAL_HANDLE_REQUIRED;
        this.failReason = failReason;
    }

    /**
     * 解析当前回执超时重试次数。
     *
     * @return 非空重试次数
     */
    private int currentReceiptCheckRetryCount() {
        return receiptCheckRetryCount == null ? 0 : receiptCheckRetryCount;
    }

    /**
     * 解析当前广播失败自动重试次数。
     *
     * @return 非空重试次数
     */
    private int currentBroadcastRetryCount() {
        return broadcastRetryCount == null ? 0 : broadcastRetryCount;
    }
}
