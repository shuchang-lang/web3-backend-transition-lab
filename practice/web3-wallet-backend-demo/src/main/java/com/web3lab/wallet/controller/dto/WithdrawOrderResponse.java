package com.web3lab.wallet.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 提现订单响应。
 */
@Data
public class WithdrawOrderResponse {

    /**
     * 提现订单主键。
     */
    private Long id;

    /**
     * 平台用户 ID。
     */
    private Long userId;

    /**
     * 链编码。
     */
    private String chain;

    /**
     * 币种符号。
     */
    private String tokenSymbol;

    /**
     * 提现目标地址。
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
     * 客户端请求号。
     */
    private String requestNo;

    /**
     * 提现订单状态。
     */
    private String status;

    /**
     * 审核状态。
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
    private LocalDateTime createdAt;

    public WithdrawOrderResponse() {
    }

    public WithdrawOrderResponse(Long id, Long userId, String chain, String tokenSymbol, String toAddress,
                                 BigDecimal amount, BigDecimal fee, String requestNo, String status,
                                 String reviewStatus, String reviewBy, LocalDateTime reviewTime, String txHash,
                                 Long nonce, Integer broadcastRetryCount, Integer receiptCheckRetryCount,
                                 String failReason,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.chain = chain;
        this.tokenSymbol = tokenSymbol;
        this.toAddress = toAddress;
        this.amount = amount;
        this.fee = fee;
        this.requestNo = requestNo;
        this.status = status;
        this.reviewStatus = reviewStatus;
        this.reviewBy = reviewBy;
        this.reviewTime = reviewTime;
        this.txHash = txHash;
        this.nonce = nonce;
        this.broadcastRetryCount = broadcastRetryCount;
        this.receiptCheckRetryCount = receiptCheckRetryCount;
        this.failReason = failReason;
        this.createdAt = createdAt;
    }
}
