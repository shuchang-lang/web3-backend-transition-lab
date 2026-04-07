package com.web3lab.wallet.controller.dto;

import lombok.Data;

/**
 * 自动对账任务执行响应。
 */
@Data
public class AccountReconcileTaskResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务批次号。
     */
    private String taskBatchNo;

    /**
     * 本轮纳入自动对账的资产数量。
     */
    private Integer processedAssetCount;

    /**
     * 本轮对账一致的资产数量。
     */
    private Integer consistentCount;

    /**
     * 本轮对账不一致的资产数量。
     */
    private Integer inconsistentCount;

    /**
     * 本轮是否真正执行了对账。
     */
    private Boolean executed;

    /**
     * 任务说明。
     */
    private String remark;

    public AccountReconcileTaskResponse() {
    }

    public AccountReconcileTaskResponse(String taskName, String taskBatchNo, Integer processedAssetCount,
                                        Integer consistentCount, Integer inconsistentCount,
                                        Boolean executed, String remark) {
        this.taskName = taskName;
        this.taskBatchNo = taskBatchNo;
        this.processedAssetCount = processedAssetCount;
        this.consistentCount = consistentCount;
        this.inconsistentCount = inconsistentCount;
        this.executed = executed;
        this.remark = remark;
    }
}
