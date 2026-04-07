package com.web3lab.wallet.application.withdraw;

import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.WithdrawTaskRunResponse;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawExecutionTaskTest {

    @Mock
    private WithdrawExecutionAppService withdrawExecutionAppService;

    @Mock
    private WithdrawBroadcastLock withdrawBroadcastLock;

    @Mock
    private Web3Gateway web3Gateway;

    @Test
    void shouldSkipBroadcastWhenAnotherBroadcastTaskIsRunning() {
        WithdrawExecutionTask task = newTask();
        when(web3Gateway.supportsWithdrawBroadcast()).thenReturn(true);
        when(withdrawBroadcastLock.tryLock()).thenReturn(false);

        WithdrawTaskRunResponse response = task.runBroadcastOnce();

        assertFalse(response.getExecuted());
        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getUpdatedCount());
        assertEquals("当前已有另一轮提现广播任务在执行中，已跳过本轮请求", response.getRemark());
        verify(withdrawExecutionAppService, never()).broadcastPendingOrders();
        verify(withdrawBroadcastLock, never()).unlock();
    }

    @Test
    void shouldBroadcastSequentiallyAndReleaseLock() {
        WithdrawExecutionTask task = newTask();
        when(web3Gateway.supportsWithdrawBroadcast()).thenReturn(true);
        when(withdrawBroadcastLock.tryLock()).thenReturn(true);
        when(withdrawExecutionAppService.broadcastPendingOrders())
                .thenReturn(new WithdrawExecutionAppService.BroadcastBatchResult(2, 1));

        WithdrawTaskRunResponse response = task.runBroadcastOnce();

        assertTrue(response.getExecuted());
        assertEquals(2, response.getProcessedCount());
        assertEquals(1, response.getUpdatedCount());
        assertEquals("已按串行方式完成一轮提现广播", response.getRemark());
        verify(withdrawExecutionAppService).broadcastPendingOrders();
        verify(withdrawBroadcastLock).unlock();
    }

    @Test
    void shouldReleaseLockWhenNoPendingWithdrawExists() {
        WithdrawExecutionTask task = newTask();
        when(web3Gateway.supportsWithdrawBroadcast()).thenReturn(true);
        when(withdrawBroadcastLock.tryLock()).thenReturn(true);
        when(withdrawExecutionAppService.broadcastPendingOrders())
                .thenReturn(new WithdrawExecutionAppService.BroadcastBatchResult(0, 0));

        WithdrawTaskRunResponse response = task.runBroadcastOnce();

        assertFalse(response.getExecuted());
        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getUpdatedCount());
        assertEquals("当前没有待广播的提现订单", response.getRemark());
        verify(withdrawExecutionAppService).broadcastPendingOrders();
        verify(withdrawBroadcastLock).unlock();
    }

    private WithdrawExecutionTask newTask() {
        return new WithdrawExecutionTask(
                withdrawExecutionAppService,
                withdrawBroadcastLock,
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
    }
}
