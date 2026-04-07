package com.web3lab.wallet.domain.task;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务审计日志实体，对应后台任务每一轮执行留下的观测快照。
 */
@Data
@TableName("task_audit_log")
public class TaskAuditLog {

    /**
     * 审计日志主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务批次号，用于串联同一轮执行上下文。
     */
    private String taskBatchNo;

    /**
     * 任务状态，当前阶段见 TaskAuditStatusConstants。
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
     * 本轮预警数量，例如发现异常但未完全失败。
     */
    private Integer warningCount;

    /**
     * 本轮失败数量。
     */
    private Integer failCount;

    /**
     * 指标快照文本，用于记录任务特有的关键计数。
     */
    private String metricSnapshot;

    /**
     * 失败原因或跳过原因。
     */
    private String failureReason;

    /**
     * 本轮任务说明。
     */
    private String remark;

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
}
