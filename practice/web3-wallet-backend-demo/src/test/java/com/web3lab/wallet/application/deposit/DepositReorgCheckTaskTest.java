package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.controller.dto.DepositReorgCheckResponse;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.web3.DepositLogChainCheckResult;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositReorgCheckTaskTest {

    @Mock
    private DepositRecordMapper depositRecordMapper;

    @Mock
    private Web3Gateway web3Gateway;

    @Test
    void shouldSkipWhenGatewayDoesNotSupportDepositReorgCheck() {
        DepositReorgCheckTask depositReorgCheckTask = new DepositReorgCheckTask(depositRecordMapper, web3Gateway);
        when(web3Gateway.supportsDepositReorgCheck()).thenReturn(false);

        DepositReorgCheckResponse response = depositReorgCheckTask.runOnce();

        assertFalse(response.getExecuted());
        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getSuspectedCount());
        verify(depositRecordMapper, never()).selectList(any());
    }

    @Test
    void shouldMarkDepositAsReorgSuspectedWhenChainLogIsMissing() {
        DepositReorgCheckTask depositReorgCheckTask = new DepositReorgCheckTask(depositRecordMapper, web3Gateway);
        DepositRecord depositRecord = DepositRecord.createPending(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xToken",
                "0xFrom",
                "0xTo",
                "0xTxHash",
                0,
                new BigDecimal("12.5"),
                100L
        );
        depositRecord.setId(10L);
        when(web3Gateway.supportsDepositReorgCheck()).thenReturn(true);
        when(web3Gateway.getLatestBlockNumber()).thenReturn(120L);
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));
        when(web3Gateway.inspectDepositTransferLog("0xToken", "0xTxHash", 0, 100L))
                .thenReturn(DepositLogChainCheckResult.reorgSuspected("链上已查不到该充值交易回执"));

        DepositReorgCheckResponse response = depositReorgCheckTask.runOnce();

        assertTrue(response.getExecuted());
        assertEquals(1, response.getProcessedCount());
        assertEquals(1, response.getSuspectedCount());
        assertEquals("已标记疑似链重组充值记录", response.getRemark());

        ArgumentCaptor<DepositRecord> captor = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRecordMapper).updateById(captor.capture());
        assertEquals("REORG_SUSPECTED", captor.getValue().getStatus());
    }

    @Test
    void shouldKeepDepositUnchangedWhenChainLogStillExists() {
        DepositReorgCheckTask depositReorgCheckTask = new DepositReorgCheckTask(depositRecordMapper, web3Gateway);
        DepositRecord depositRecord = DepositRecord.createPending(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xToken",
                "0xFrom",
                "0xTo",
                "0xTxHash",
                0,
                new BigDecimal("12.5"),
                100L
        );
        depositRecord.setId(10L);
        when(web3Gateway.supportsDepositReorgCheck()).thenReturn(true);
        when(web3Gateway.getLatestBlockNumber()).thenReturn(120L);
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));
        when(web3Gateway.inspectDepositTransferLog("0xToken", "0xTxHash", 0, 100L))
                .thenReturn(DepositLogChainCheckResult.present());

        DepositReorgCheckResponse response = depositReorgCheckTask.runOnce();

        assertTrue(response.getExecuted());
        assertEquals(1, response.getProcessedCount());
        assertEquals(0, response.getSuspectedCount());
        assertEquals("本轮未发现疑似链重组充值记录", response.getRemark());
        verify(depositRecordMapper, never()).updateById(any(DepositRecord.class));
    }
}
