package com.web3lab.wallet.controller.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 提现任务执行响应。
 */
@Data
public class WithdrawTaskRunResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 本轮扫描到的候选订单数量。
     */
    private Integer processedCount;

    /**
     * 本轮实际推进成功的订单数量。
     */
    private Integer updatedCount;

    /**
     * 任务执行时间。
     */
    private LocalDateTime runAt;

    /**
     * 本轮是否真正执行了任务主体。
     */
    private Boolean executed;

    /**
     * 任务说明。
     */
    private String remark;

    public WithdrawTaskRunResponse() {
    }

    public WithdrawTaskRunResponse(String taskName, Integer processedCount, Integer updatedCount, LocalDateTime runAt) {
        this(taskName, processedCount, updatedCount, runAt, true, "");
    }

    public WithdrawTaskRunResponse(String taskName, Integer processedCount, Integer updatedCount, LocalDateTime runAt,
                                   Boolean executed, String remark) {
        this.taskName = taskName;
        this.processedCount = processedCount;
        this.updatedCount = updatedCount;
        this.runAt = runAt;
        this.executed = executed;
        this.remark = remark;
    }
}
