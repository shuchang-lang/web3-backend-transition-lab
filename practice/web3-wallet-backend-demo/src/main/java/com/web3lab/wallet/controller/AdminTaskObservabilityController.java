package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.task.TaskAuditLogAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.TaskAuditLogResponse;
import com.web3lab.wallet.controller.dto.TaskMetricOverviewResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台任务可观测性查询接口。
 */
@RestController
@RequestMapping("/admin/task-observability")
public class AdminTaskObservabilityController {

    private final TaskAuditLogAppService taskAuditLogAppService;

    public AdminTaskObservabilityController(TaskAuditLogAppService taskAuditLogAppService) {
        this.taskAuditLogAppService = taskAuditLogAppService;
    }

    /**
     * 查询指定任务最近的审计日志。
     *
     * @param taskName 任务名称
     * @param limit 返回条数，默认 10，最大 20
     * @return 审计日志列表
     */
    @GetMapping("/audit/{taskName}")
    public ApiResponse<List<TaskAuditLogResponse>> getLatestAuditLogs(@PathVariable String taskName,
                                                                      @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(taskAuditLogAppService.listLatestByTaskName(taskName, limit));
    }

    /**
     * 查询核心后台任务的指标概览。
     *
     * @return 任务指标概览列表
     */
    @GetMapping("/metrics/overview")
    public ApiResponse<List<TaskMetricOverviewResponse>> getMetricOverview() {
        return ApiResponse.success(taskAuditLogAppService.listMetricOverview());
    }
}
