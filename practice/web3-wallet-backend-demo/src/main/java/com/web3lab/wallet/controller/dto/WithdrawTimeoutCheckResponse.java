package com.web3lab.wallet.controller.dto;

import lombok.Data;

/**
 * 提现回执超时巡检响应。
 */
@Data
public class WithdrawTimeoutCheckResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 本轮扫描到的已广播订单数量。
     */
    private Integer processedCount;

    /**
     * 本轮继续等待回执的订单数量。
     */
    private Integer continueWaitingCount;

    /**
     * 本轮重查回执后直接完成结算的订单数量。
     */
    private Integer resolvedCount;

    /**
     * 本轮被打回待广播重试的订单数量。
     */
    private Integer retryableCount;

    /**
     * 本轮被转入人工处理的订单数量。
     */
    private Integer manualHandleCount;

    /**
     * 本轮是否真正执行了巡检。
     */
    private Boolean executed;

    /**
     * 任务说明。
     */
    private String remark;

    public WithdrawTimeoutCheckResponse() {
    }

    public WithdrawTimeoutCheckResponse(String taskName, Integer processedCount, Integer continueWaitingCount,
                                        Integer resolvedCount, Integer retryableCount, Integer manualHandleCount,
                                        Boolean executed, String remark) {
        this.taskName = taskName;
        this.processedCount = processedCount;
        this.continueWaitingCount = continueWaitingCount;
        this.resolvedCount = resolvedCount;
        this.retryableCount = retryableCount;
        this.manualHandleCount = manualHandleCount;
        this.executed = executed;
        this.remark = remark;
    }
}
