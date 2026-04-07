package com.web3lab.wallet.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 充值记录响应。
 */
@Data
public class DepositRecordResponse {

    /**
     * 充值记录主键。
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
     * 日志索引。
     */
    private Integer logIndex;

    /**
     * 充值金额。
     */
    private BigDecimal amount;

    /**
     * 区块号。
     */
    private Long blockNumber;

    /**
     * 当前确认数。
     */
    private Long confirmations;

    /**
     * 充值状态。
     */
    private String status;

    /**
     * 正式入账时间。
     */
    private LocalDateTime creditedAt;

    /**
     * 记录创建时间。
     */
    private LocalDateTime createdAt;

    public DepositRecordResponse() {
    }

    public DepositRecordResponse(Long id, Long userId, String chain, String tokenSymbol, String tokenContract,
                                 String fromAddress, String toAddress, String txHash, Integer logIndex,
                                 BigDecimal amount, Long blockNumber, Long confirmations, String status,
                                 LocalDateTime creditedAt, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.chain = chain;
        this.tokenSymbol = tokenSymbol;
        this.tokenContract = tokenContract;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.txHash = txHash;
        this.logIndex = logIndex;
        this.amount = amount;
        this.blockNumber = blockNumber;
        this.confirmations = confirmations;
        this.status = status;
        this.creditedAt = creditedAt;
        this.createdAt = createdAt;
    }
}
