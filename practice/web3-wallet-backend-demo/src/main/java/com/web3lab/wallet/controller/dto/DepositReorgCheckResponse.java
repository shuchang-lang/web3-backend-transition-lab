package com.web3lab.wallet.controller.dto;

import lombok.Data;

/**
 * 充值 reorg 风险检查任务响应。
 */
@Data
public class DepositReorgCheckResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 当前链头区块。
     */
    private Long latestBlock;

    /**
     * 本轮检查的充值记录数量。
     */
    private Integer processedCount;

    /**
     * 本轮标记为 reorg 风险的数量。
     */
    private Integer suspectedCount;

    /**
     * 本轮是否真正执行了链上检查。
     */
    private Boolean executed;

    /**
     * 结果说明。
     */
    private String remark;

    public DepositReorgCheckResponse() {
    }

    public DepositReorgCheckResponse(String taskName, Long latestBlock, Integer processedCount, Integer suspectedCount,
                                     Boolean executed, String remark) {
        this.taskName = taskName;
        this.latestBlock = latestBlock;
        this.processedCount = processedCount;
        this.suspectedCount = suspectedCount;
        this.executed = executed;
        this.remark = remark;
    }
}
