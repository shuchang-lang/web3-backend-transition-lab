package com.web3lab.wallet.application.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.application.account.AccountReconcileTask;
import com.web3lab.wallet.application.scan.ChainScanProgressAppService;
import com.web3lab.wallet.application.withdraw.WithdrawExecutionTask;
import com.web3lab.wallet.application.withdraw.WithdrawTimeoutCheckTask;
import com.web3lab.wallet.common.task.TaskAuditStatusConstants;
import com.web3lab.wallet.controller.dto.TaskAuditLogResponse;
import com.web3lab.wallet.controller.dto.TaskMetricOverviewResponse;
import com.web3lab.wallet.domain.task.TaskAuditLog;
import com.web3lab.wallet.infrastructure.persistence.TaskAuditLogMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务审计日志应用服务。
 */
@Service
public class TaskAuditLogAppService {

    private static final DateTimeFormatter TASK_BATCH_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final List<String> CORE_TASK_NAMES = List.of(
            ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
            WithdrawExecutionTask.WITHDRAW_BROADCAST_TASK,
            WithdrawTimeoutCheckTask.WITHDRAW_TIMEOUT_CHECK_TASK,
            AccountReconcileTask.ACCOUNT_RECONCILE_TASK
    );

    private final TaskAuditLogMapper taskAuditLogMapper;

    public TaskAuditLogAppService(TaskAuditLogMapper taskAuditLogMapper) {
        this.taskAuditLogMapper = taskAuditLogMapper;
    }

    /**
     * 生成任务批次号。
     *
     * @param taskName 任务名称
     * @return 任务批次号
     */
    public String nextBatchNo(String taskName) {
        return taskName + "-" + LocalDateTime.now().format(TASK_BATCH_NO_FORMATTER);
    }

    /**
     * 记录一条成功执行的任务审计日志。
     *
     * @param taskName 任务名称
     * @param taskBatchNo 任务批次号
     * @param processedCount 处理总量
     * @param successCount 成功推进数量
     * @param warningCount 预警数量
     * @param failCount 失败数量
     * @param metricSnapshot 指标快照
     * @param remark 任务说明
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String taskName, String taskBatchNo, Integer processedCount,
                              Integer successCount, Integer warningCount, Integer failCount,
                              String metricSnapshot, String remark) {
        persist(taskName, taskBatchNo, TaskAuditStatusConstants.SUCCESS, processedCount,
                successCount, warningCount, failCount, metricSnapshot, null, remark);
    }

    /**
     * 记录一条跳过执行的任务审计日志。
     *
     * @param taskName 任务名称
     * @param taskBatchNo 任务批次号
     * @param failureReason 跳过原因
     * @param remark 任务说明
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSkipped(String taskName, String taskBatchNo, String failureReason, String remark) {
        persist(taskName, taskBatchNo, TaskAuditStatusConstants.SKIPPED, 0, 0, 0, 0,
                "", failureReason, remark);
    }

    /**
     * 记录一条失败执行的任务审计日志。
     *
     * @param taskName 任务名称
     * @param taskBatchNo 任务批次号
     * @param processedCount 处理总量
     * @param successCount 成功推进数量
     * @param warningCount 预警数量
     * @param failCount 失败数量
     * @param metricSnapshot 指标快照
     * @param failureReason 失败原因
     * @param remark 任务说明
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String taskName, String taskBatchNo, Integer processedCount,
                              Integer successCount, Integer warningCount, Integer failCount,
                              String metricSnapshot, String failureReason, String remark) {
        persist(taskName, taskBatchNo, TaskAuditStatusConstants.FAILED, processedCount,
                successCount, warningCount, failCount, metricSnapshot, failureReason, remark);
    }

    /**
     * 查询指定任务最近的审计日志。
     *
     * @param taskName 任务名称
     * @param limit 返回条数
     * @return 审计日志列表
     */
    public List<TaskAuditLogResponse> listLatestByTaskName(String taskName, Integer limit) {
        int resolvedLimit = resolveLimit(limit);
        return taskAuditLogMapper.selectList(
                        Wrappers.<TaskAuditLog>lambdaQuery()
                                .eq(TaskAuditLog::getTaskName, taskName)
                                .orderByDesc(TaskAuditLog::getId)
                                .last("LIMIT " + resolvedLimit)
                ).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询核心任务指标概览。
     *
     * @return 核心任务指标概览
     */
    public List<TaskMetricOverviewResponse> listMetricOverview() {
        return CORE_TASK_NAMES.stream().map(this::buildOverview).collect(Collectors.toList());
    }

    /**
     * 构建指定任务的指标概览。
     *
     * @param taskName 任务名称
     * @return 任务指标概览
     */
    private TaskMetricOverviewResponse buildOverview(String taskName) {
        List<TaskAuditLog> logs = taskAuditLogMapper.selectList(
                Wrappers.<TaskAuditLog>lambdaQuery()
                        .eq(TaskAuditLog::getTaskName, taskName)
                        .orderByDesc(TaskAuditLog::getId)
        );
        TaskMetricOverviewResponse response = new TaskMetricOverviewResponse();
        response.setTaskName(taskName);
        response.setTotalRunCount(logs.size());
        response.setSuccessRunCount((int) logs.stream()
                .filter(log -> TaskAuditStatusConstants.SUCCESS.equals(log.getTaskStatus()))
                .count());
        response.setSkippedRunCount((int) logs.stream()
                .filter(log -> TaskAuditStatusConstants.SKIPPED.equals(log.getTaskStatus()))
                .count());
        response.setFailedRunCount((int) logs.stream()
                .filter(log -> TaskAuditStatusConstants.FAILED.equals(log.getTaskStatus()))
                .count());
        response.setTotalProcessedCount(logs.stream().map(TaskAuditLog::getProcessedCount).mapToInt(this::zeroIfNull).sum());
        response.setTotalSuccessCount(logs.stream().map(TaskAuditLog::getSuccessCount).mapToInt(this::zeroIfNull).sum());
        response.setTotalWarningCount(logs.stream().map(TaskAuditLog::getWarningCount).mapToInt(this::zeroIfNull).sum());
        response.setTotalFailCount(logs.stream().map(TaskAuditLog::getFailCount).mapToInt(this::zeroIfNull).sum());

        if (!logs.isEmpty()) {
            TaskAuditLog latest = logs.get(0);
            response.setLatestTaskBatchNo(latest.getTaskBatchNo());
            response.setLatestTaskStatus(latest.getTaskStatus());
            response.setLatestProcessedCount(latest.getProcessedCount());
            response.setLatestSuccessCount(latest.getSuccessCount());
            response.setLatestWarningCount(latest.getWarningCount());
            response.setLatestFailCount(latest.getFailCount());
            response.setLatestMetricSnapshot(latest.getMetricSnapshot());
            response.setLatestFailureReason(latest.getFailureReason());
            response.setLatestRemark(latest.getRemark());
            response.setLatestRunAt(latest.getCreatedAt());
        }
        return response;
    }

    /**
     * 持久化一条任务审计日志。
     *
     * @param taskName 任务名称
     * @param taskBatchNo 任务批次号
     * @param taskStatus 任务状态
     * @param processedCount 处理总量
     * @param successCount 成功推进数量
     * @param warningCount 预警数量
     * @param failCount 失败数量
     * @param metricSnapshot 指标快照
     * @param failureReason 失败原因
     * @param remark 任务说明
     */
    private void persist(String taskName, String taskBatchNo, String taskStatus, Integer processedCount,
                         Integer successCount, Integer warningCount, Integer failCount,
                         String metricSnapshot, String failureReason, String remark) {
        TaskAuditLog taskAuditLog = new TaskAuditLog();
        taskAuditLog.setTaskName(taskName);
        taskAuditLog.setTaskBatchNo(taskBatchNo);
        taskAuditLog.setTaskStatus(taskStatus);
        taskAuditLog.setProcessedCount(zeroIfNull(processedCount));
        taskAuditLog.setSuccessCount(zeroIfNull(successCount));
        taskAuditLog.setWarningCount(zeroIfNull(warningCount));
        taskAuditLog.setFailCount(zeroIfNull(failCount));
        taskAuditLog.setMetricSnapshot(metricSnapshot == null ? "" : metricSnapshot);
        taskAuditLog.setFailureReason(failureReason);
        taskAuditLog.setRemark(remark);
        taskAuditLogMapper.insert(taskAuditLog);
    }

    /**
     * 转换任务审计日志响应。
     *
     * @param taskAuditLog 审计日志实体
     * @return 审计日志响应
     */
    private TaskAuditLogResponse toResponse(TaskAuditLog taskAuditLog) {
        TaskAuditLogResponse response = new TaskAuditLogResponse();
        response.setId(taskAuditLog.getId());
        response.setTaskName(taskAuditLog.getTaskName());
        response.setTaskBatchNo(taskAuditLog.getTaskBatchNo());
        response.setTaskStatus(taskAuditLog.getTaskStatus());
        response.setProcessedCount(taskAuditLog.getProcessedCount());
        response.setSuccessCount(taskAuditLog.getSuccessCount());
        response.setWarningCount(taskAuditLog.getWarningCount());
        response.setFailCount(taskAuditLog.getFailCount());
        response.setMetricSnapshot(taskAuditLog.getMetricSnapshot());
        response.setFailureReason(taskAuditLog.getFailureReason());
        response.setRemark(taskAuditLog.getRemark());
        response.setCreatedAt(taskAuditLog.getCreatedAt());
        return response;
    }

    /**
     * 解析查询条数限制。
     *
     * @param limit 原始输入
     * @return 规整后的限制条数
     */
    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 10;
        }
        return Math.min(limit, 20);
    }

    /**
     * 空值转零。
     *
     * @param value 原始值
     * @return 非空整数
     */
    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
