package com.web3lab.wallet.application.withdraw;

import com.web3lab.wallet.application.task.TaskAuditLogAppService;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.WithdrawTimeoutCheckResponse;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawTimeoutCheckTaskTest {

    @Mock
    private WithdrawExecutionAppService withdrawExecutionAppService;

    @Mock
    private Web3Gateway web3Gateway;

    @Mock
    private TaskAuditLogAppService taskAuditLogAppService;

    @Test
    void shouldSkipWhenRpcIsNotConfigured() {
        WithdrawTimeoutCheckTask task = new WithdrawTimeoutCheckTask(
                withdrawExecutionAppService,
                taskAuditLogAppService,
                web3Gateway,
                new WalletWeb3Properties(
                        "",
                        "0xToken",
                        0L,
                        200,
                        6,
                        6,
                        "0xabc",
                        11155111L,
                        120000L,
                        3,
                        10L,
                        2
                )
        );
        when(web3Gateway.clientName()).thenReturn("mock");
        when(taskAuditLogAppService.nextBatchNo(WithdrawTimeoutCheckTask.WITHDRAW_TIMEOUT_CHECK_TASK))
                .thenReturn("withdraw_receipt_timeout_check-20260407222000001");

        WithdrawTimeoutCheckResponse response = task.runOnce();

        assertFalse(response.getExecuted());
        assertEquals(0, response.getProcessedCount());
        assertEquals("当前未配置可用 RPC，已跳过提现回执超时巡检", response.getRemark());
        verify(withdrawExecutionAppService, never()).inspectTimeoutSubmittedOrders();
        verify(taskAuditLogAppService).recordSkipped(
                WithdrawTimeoutCheckTask.WITHDRAW_TIMEOUT_CHECK_TASK,
                "withdraw_receipt_timeout_check-20260407222000001",
                "当前未配置可用 RPC",
                "已跳过提现回执超时巡检"
        );
    }

    @Test
    void shouldReturnAggregatedTimeoutCheckResponse() {
        WithdrawTimeoutCheckTask task = new WithdrawTimeoutCheckTask(
                withdrawExecutionAppService,
                taskAuditLogAppService,
                web3Gateway,
                new WalletWeb3Properties(
                        "http://localhost:8545",
                        "0xToken",
                        0L,
                        200,
                        6,
                        6,
                        "0xabc",
                        11155111L,
                        120000L,
                        3,
                        10L,
                        2
                )
        );
        when(taskAuditLogAppService.nextBatchNo(WithdrawTimeoutCheckTask.WITHDRAW_TIMEOUT_CHECK_TASK))
                .thenReturn("withdraw_receipt_timeout_check-20260407222000002");
        when(withdrawExecutionAppService.inspectTimeoutSubmittedOrders())
                .thenReturn(new WithdrawExecutionAppService.TimeoutInspectionBatchResult(3, 1, 1, 1, 0));

        WithdrawTimeoutCheckResponse response = task.runOnce();

        assertTrue(response.getExecuted());
        assertEquals(3, response.getProcessedCount());
        assertEquals(1, response.getContinueWaitingCount());
        assertEquals(1, response.getResolvedCount());
        assertEquals(1, response.getRetryableCount());
        assertEquals(0, response.getManualHandleCount());
        assertEquals("已完成提现回执超时巡检与异常治理", response.getRemark());
        verify(taskAuditLogAppService).recordSuccess(
                eq(WithdrawTimeoutCheckTask.WITHDRAW_TIMEOUT_CHECK_TASK),
                eq("withdraw_receipt_timeout_check-20260407222000002"),
                eq(3),
                eq(1),
                eq(1),
                eq(0),
                contains("continueWaitingCount=1"),
                eq("已完成提现回执超时巡检与异常治理")
        );
    }
}
