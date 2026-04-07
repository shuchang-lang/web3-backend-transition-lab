package com.web3lab.wallet.domain.account;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自动对账结果实体，对应每轮任务沉淀下来的资产一致性快照。
 */
@Data
@TableName("account_reconcile_result")
public class AccountReconcileResult {

    /**
     * 对账结果主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务批次号，用于串起同一轮自动对账结果。
     */
    private String taskBatchNo;

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
     * 当前可用余额。
     */
    private BigDecimal availableBalance;

    /**
     * 当前冻结余额。
     */
    private BigDecimal frozenBalance;

    /**
     * 当前总余额，口径为 `available + frozen`。
     */
    private BigDecimal totalBalance;

    /**
     * 已正式入账的充值总金额。
     */
    private BigDecimal creditedDepositAmount;

    /**
     * 已成功提现并正式扣减的总金额，口径为 `amount + fee`。
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 根据单资产对账结果创建一条可落库的任务快照。
     *
     * @param taskName 任务名称
     * @param taskBatchNo 任务批次号
     * @param response 单资产对账结果
     * @return 对账结果实体
     */
    public static AccountReconcileResult fromResponse(String taskName, String taskBatchNo,
                                                      AccountAssetReconcileResponse response) {
        AccountReconcileResult result = new AccountReconcileResult();
        result.setTaskName(taskName);
        result.setTaskBatchNo(taskBatchNo);
        result.setUserId(response.getUserId());
        result.setChain(response.getChain());
        result.setTokenSymbol(response.getTokenSymbol());
        result.setAvailableBalance(response.getAvailableBalance());
        result.setFrozenBalance(response.getFrozenBalance());
        result.setTotalBalance(response.getTotalBalance());
        result.setCreditedDepositAmount(response.getCreditedDepositAmount());
        result.setSuccessfulWithdrawAmount(response.getSuccessfulWithdrawAmount());
        result.setPendingWithdrawFrozenAmount(response.getPendingWithdrawFrozenAmount());
        result.setBillAssetDeltaAmount(response.getBillAssetDeltaAmount());
        result.setLatestBillAvailableAfter(response.getLatestBillAvailableAfter());
        result.setLatestBillFrozenAfter(response.getLatestBillFrozenAfter());
        result.setBillCount(response.getBillCount());
        result.setConsistentWithBusinessTables(response.getConsistentWithBusinessTables());
        result.setConsistentWithAssetDeltaBills(response.getConsistentWithAssetDeltaBills());
        result.setConsistentWithLatestBillSnapshot(response.getConsistentWithLatestBillSnapshot());
        result.setConsistentWithPendingWithdraws(response.getConsistentWithPendingWithdraws());
        result.setConsistent(response.getConsistent());
        result.setMismatchReason(response.getMismatchReason());
        return result;
    }
}
