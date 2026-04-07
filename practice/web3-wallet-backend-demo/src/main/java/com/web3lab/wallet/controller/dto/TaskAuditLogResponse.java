package com.web3lab.wallet.controller.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务审计日志查询响应。
 */
@Data
public class TaskAuditLogResponse {

    /**
     * 审计日志主键。
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
     * 任务状态。
     */
    private String taskStatus;

    /**
     * 本轮处理总量。
     */
    private Integer processedCount;

    /**
     * 本轮成功推进数量。
     */
    private Integer successCount;

    /**
     * 本轮预警数量。
     */
    private Integer warningCount;

    /**
     * 本轮失败数量。
     */
    private Integer failCount;

    /**
     * 指标快照文本。
     */
    private String metricSnapshot;

    /**
     * 失败原因或跳过原因。
     */
    private String failureReason;

    /**
     * 任务说明。
     */
    private String remark;

    /**
     * 记录创建时间。
     */
    private LocalDateTime createdAt;
}
