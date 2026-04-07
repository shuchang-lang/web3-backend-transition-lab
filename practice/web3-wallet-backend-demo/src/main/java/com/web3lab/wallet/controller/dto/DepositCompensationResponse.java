package com.web3lab.wallet.controller.dto;

import lombok.Data;

/**
 * 充值补偿任务响应。
 */
@Data
public class DepositCompensationResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 本轮纳入补偿处理的充值记录数量。
     */
    private Integer processedCount;

    /**
     * 本轮实际完成冲正的充值记录数量。
     */
    private Integer compensatedCount;

    /**
     * 本轮是否真正执行了补偿。
     */
    private Boolean executed;

    /**
     * 任务说明。
     */
    private String remark;

    public DepositCompensationResponse() {
    }

    public DepositCompensationResponse(String taskName, Integer processedCount, Integer compensatedCount,
                                       Boolean executed, String remark) {
        this.taskName = taskName;
        this.processedCount = processedCount;
        this.compensatedCount = compensatedCount;
        this.executed = executed;
        this.remark = remark;
    }
}
