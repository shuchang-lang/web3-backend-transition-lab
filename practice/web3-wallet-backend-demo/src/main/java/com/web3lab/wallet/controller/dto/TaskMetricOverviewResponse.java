package com.web3lab.wallet.controller.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务指标概览响应。
 */
@Data
public class TaskMetricOverviewResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 最近一轮任务批次号。
     */
    private String latestTaskBatchNo;

    /**
     * 最近一轮任务状态。
     */
    private String latestTaskStatus;

    /**
     * 总运行次数。
     */
    private Integer totalRunCount;

    /**
     * 成功运行次数。
     */
    private Integer successRunCount;

    /**
     * 跳过运行次数。
     */
    private Integer skippedRunCount;

    /**
     * 失败运行次数。
     */
    private Integer failedRunCount;

    /**
     * 累计处理总量。
     */
    private Integer totalProcessedCount;

    /**
     * 累计成功推进数量。
     */
    private Integer totalSuccessCount;

    /**
     * 累计预警数量。
     */
    private Integer totalWarningCount;

    /**
     * 累计失败数量。
     */
    private Integer totalFailCount;

    /**
     * 最近一轮处理总量。
     */
    private Integer latestProcessedCount;

    /**
     * 最近一轮成功推进数量。
     */
    private Integer latestSuccessCount;

    /**
     * 最近一轮预警数量。
     */
    private Integer latestWarningCount;

    /**
     * 最近一轮失败数量。
     */
    private Integer latestFailCount;

    /**
     * 最近一轮指标快照文本。
     */
    private String latestMetricSnapshot;

    /**
     * 最近一轮失败原因。
     */
    private String latestFailureReason;

    /**
     * 最近一轮任务说明。
     */
    private String latestRemark;

    /**
     * 最近一轮执行时间。
     */
    private LocalDateTime latestRunAt;
}
