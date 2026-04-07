package com.web3lab.wallet.domain.deposit;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 充值记录实体，对应链上候选充值和正式入账记录。
 */
@Data
@TableName("deposit_record")
public class DepositRecord {

    /**
     * 充值记录主键。
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
     * 代币合约地址。
     */
    private String tokenContract;

    /**
     * 转出地址。
     */
    private String fromAddress;

    /**
     * 转入地址。
     */
    private String toAddress;

    /**
     * 链上交易哈希。
     */
    private String txHash;

    /**
     * 日志索引，同一交易内用于区分不同事件。
     */
    private Integer logIndex;

    /**
     * 充值金额。
     */
    private BigDecimal amount;

    /**
     * 所在区块号。
     */
    private Long blockNumber;

    /**
     * 当前确认数。
     */
    private Long confirmations;

    /**
     * 充值状态，当前阶段见 {@link DepositStatusConstants}。
     */
    private String status;

    /**
     * 正式入账时间。
     */
    private LocalDateTime creditedAt;

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
     * 创建候选充值记录。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param tokenContract 代币合约地址
     * @param fromAddress 转出地址
     * @param toAddress 转入地址
     * @param txHash 交易哈希
     * @param logIndex 日志索引
     * @param amount 充值金额
     * @param blockNumber 区块号
     * @return 候选充值记录
     */
    public static DepositRecord createPending(Long userId, String chain, String tokenSymbol, String tokenContract,
                                              String fromAddress, String toAddress, String txHash, Integer logIndex,
                                              BigDecimal amount, Long blockNumber) {
        DepositRecord depositRecord = new DepositRecord();
        depositRecord.setUserId(userId);
        depositRecord.setChain(chain);
        depositRecord.setTokenSymbol(tokenSymbol);
        depositRecord.setTokenContract(tokenContract);
        depositRecord.setFromAddress(fromAddress);
        depositRecord.setToAddress(toAddress);
        depositRecord.setTxHash(txHash);
        depositRecord.setLogIndex(logIndex);
        depositRecord.setAmount(amount);
        depositRecord.setBlockNumber(blockNumber);
        depositRecord.setConfirmations(0L);
        depositRecord.setStatus(DepositStatusConstants.PENDING_CONFIRM);
        return depositRecord;
    }

    /**
     * 刷新当前充值记录的确认数。
     *
     * @param confirmations 最新确认数
     */
    public void refreshConfirmations(Long confirmations) {
        this.confirmations = confirmations;
    }

    /**
     * 将当前充值记录推进为已入账。
     *
     * @param confirmations 正式入账时对应的确认数
     */
    public void markCredited(Long confirmations) {
        this.confirmations = confirmations;
        this.status = DepositStatusConstants.CREDITED;
        this.creditedAt = LocalDateTime.now();
    }

    /**
     * 标记为疑似发生链重组。
     *
     * <p>Day22 第一版先只负责识别并标记风险，不在这里直接做资金冲正，
     * 这样可以把“发现问题”和“补偿问题”拆成两个更清晰的阶段。</p>
     */
    public void markReorgSuspected() {
        this.status = DepositStatusConstants.REORG_SUSPECTED;
    }

    /**
     * 标记为已完成链重组冲正。
     *
     * <p>Day23 第一版会把已经正式入账、后来又被识别为链重组风险的充值记录，
     * 推进到这个终态，避免同一条风险记录被重复冲正。</p>
     */
    public void markReorgCompensated() {
        this.status = DepositStatusConstants.REORG_COMPENSATED;
    }
}
