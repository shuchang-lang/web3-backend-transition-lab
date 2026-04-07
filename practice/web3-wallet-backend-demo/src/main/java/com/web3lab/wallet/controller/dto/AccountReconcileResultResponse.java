package com.web3lab.wallet.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自动对账结果查询响应。
 */
@Data
public class AccountReconcileResultResponse {

    /**
     * 对账结果主键。
     */
    private Long id;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务批次号。
     */
    private String taskBatchNo;

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
     * 当前可用余额。
     */
    private BigDecimal availableBalance;

    /**
     * 当前冻结余额。
     */
    private BigDecimal frozenBalance;

    /**
     * 当前总余额。
     */
    private BigDecimal totalBalance;

    /**
     * 已正式入账的充值总金额。
     */
    private BigDecimal creditedDepositAmount;

    /**
     * 已成功提现并正式扣减的总金额。
     */
    private BigDecimal successfulWithdrawAmount;

    /**
     * 当前仍应体现在冻结余额中的待完成提现总金额。
     */
    private BigDecimal pendingWithdrawFrozenAmount;

    /**
     * 按资产口径折算后的流水净变动金额。
     */
    private BigDecimal billAssetDeltaAmount;

    /**
     * 最新一条流水快照中的可用余额。
     */
    private BigDecimal latestBillAvailableAfter;

    /**
     * 最新一条流水快照中的冻结余额。
     */
    private BigDecimal latestBillFrozenAfter;

    /**
     * 当前资产下的流水条数。
     */
    private Integer billCount;

    /**
     * 是否与业务表汇总结果一致。
     */
    private Boolean consistentWithBusinessTables;

    /**
     * 是否与资产口径流水净额一致。
     */
    private Boolean consistentWithAssetDeltaBills;

    /**
     * 是否与最新流水快照一致。
     */
    private Boolean consistentWithLatestBillSnapshot;

    /**
     * 是否与待完成提现冻结金额一致。
     */
    private Boolean consistentWithPendingWithdraws;

    /**
     * 当前整体是否通过最小对账校验。
     */
    private Boolean consistent;

    /**
     * 不一致原因汇总。
     */
    private String mismatchReason;

    /**
     * 记录创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间。
     */
    private LocalDateTime updatedAt;
}
