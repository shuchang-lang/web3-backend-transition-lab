package com.web3lab.wallet.application.task;

import com.web3lab.wallet.common.task.TaskAuditStatusConstants;
import com.web3lab.wallet.controller.dto.TaskAuditLogResponse;
import com.web3lab.wallet.controller.dto.TaskMetricOverviewResponse;
import com.web3lab.wallet.domain.task.TaskAuditLog;
import com.web3lab.wallet.infrastructure.persistence.TaskAuditLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAuditLogAppServiceTest {

    @Mock
    private TaskAuditLogMapper taskAuditLogMapper;

    @Test
    void shouldListLatestAuditLogsByTaskName() {
        TaskAuditLog latest = buildLog(
                10L,
                "withdraw_broadcast",
                "withdraw_broadcast-20260407223000001",
                TaskAuditStatusConstants.SUCCESS,
                2,
                1,
                1,
                0,
                "processedCount=2,updatedCount=1",
                null,
                "已按串行方式完成一轮提现广播"
        );
        when(taskAuditLogMapper.selectList(any())).thenReturn(List.of(latest));

        TaskAuditLogAppService service = new TaskAuditLogAppService(taskAuditLogMapper);
        List<TaskAuditLogResponse> responses = service.listLatestByTaskName("withdraw_broadcast", 5);

        assertEquals(1, responses.size());
        assertEquals("withdraw_broadcast", responses.get(0).getTaskName());
        assertEquals("SUCCESS", responses.get(0).getTaskStatus());
        assertEquals("processedCount=2,updatedCount=1", responses.get(0).getMetricSnapshot());
    }

    @Test
    void shouldBuildMetricOverviewForCoreTasks() {
        List<TaskAuditLog> depositLogs = List.of(
                buildLog(1L, "erc20_deposit_scan", "batch-1", TaskAuditStatusConstants.SUCCESS,
                        2, 1, 0, 0, "fromBlock=1", null, "已完成一轮 ERC-20 充值扫描"),
                buildLog(2L, "erc20_deposit_scan", "batch-0", TaskAuditStatusConstants.SKIPPED,
                        0, 0, 0, 0, "", "当前暂无可扫描区块", "本轮充值扫描没有新增区块窗口")
        );
        List<TaskAuditLog> broadcastLogs = List.of(
                buildLog(3L, "withdraw_broadcast", "batch-2", TaskAuditStatusConstants.FAILED,
                        0, 0, 0, 1, "", "rpc timeout", "提现广播任务执行失败")
        );
        List<TaskAuditLog> timeoutLogs = List.of();
        List<TaskAuditLog> reconcileLogs = List.of(
                buildLog(4L, "account_asset_auto_reconcile", "batch-3", TaskAuditStatusConstants.SUCCESS,
                        3, 2, 1, 0, "consistentCount=2,inconsistentCount=1", null, "已完成自动对账，本轮发现 1 个不一致资产")
        );
        when(taskAuditLogMapper.selectList(any()))
                .thenReturn(depositLogs)
                .thenReturn(broadcastLogs)
                .thenReturn(timeoutLogs)
                .thenReturn(reconcileLogs);

        TaskAuditLogAppService service = new TaskAuditLogAppService(taskAuditLogMapper);
        List<TaskMetricOverviewResponse> responses = service.listMetricOverview();

        assertEquals(4, responses.size());
        assertEquals("erc20_deposit_scan", responses.get(0).getTaskName());
        assertEquals(2, responses.get(0).getTotalRunCount());
        assertEquals(1, responses.get(0).getSuccessRunCount());
        assertEquals(1, responses.get(0).getSkippedRunCount());
        assertEquals("withdraw_broadcast", responses.get(1).getTaskName());
        assertEquals(1, responses.get(1).getFailedRunCount());
        assertEquals("rpc timeout", responses.get(1).getLatestFailureReason());
        assertEquals("account_asset_auto_reconcile", responses.get(3).getTaskName());
        assertEquals("batch-3", responses.get(3).getLatestTaskBatchNo());
        assertTrue(responses.get(3).getLatestMetricSnapshot().contains("inconsistentCount=1"));
    }

    private TaskAuditLog buildLog(Long id, String taskName, String taskBatchNo, String taskStatus,
                                  Integer processedCount, Integer successCount, Integer warningCount,
                                  Integer failCount, String metricSnapshot, String failureReason, String remark) {
        TaskAuditLog log = new TaskAuditLog();
        log.setId(id);
        log.setTaskName(taskName);
        log.setTaskBatchNo(taskBatchNo);
        log.setTaskStatus(taskStatus);
        log.setProcessedCount(processedCount);
        log.setSuccessCount(successCount);
        log.setWarningCount(warningCount);
        log.setFailCount(failCount);
        log.setMetricSnapshot(metricSnapshot);
        log.setFailureReason(failureReason);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.of(2026, 4, 7, 22, 30));
        log.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 22, 30));
        return log;
    }
}
