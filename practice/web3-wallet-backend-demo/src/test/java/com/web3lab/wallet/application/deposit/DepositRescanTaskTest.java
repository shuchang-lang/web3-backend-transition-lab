package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.common.exception.BusinessException;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.DepositRescanResponse;
import com.web3lab.wallet.infrastructure.web3.Erc20TransferLog;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositRescanTaskTest {

    @Mock
    private Web3Gateway web3Gateway;

    @Mock
    private DepositCandidateAppService depositCandidateAppService;

    @Mock
    private DepositSettlementAppService depositSettlementAppService;

    @Test
    void shouldSkipManualRescanWhenRpcConfigIsMissing() {
        DepositRescanTask depositRescanTask = new DepositRescanTask(
                web3Gateway,
                depositCandidateAppService,
                depositSettlementAppService,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT"),
                new WalletWeb3Properties("", "", 0L, 200, 6, 6, "", 11155111L, 120000L, 3, 10L, 2)
        );

        DepositRescanResponse response = depositRescanTask.runOnce(100L, 120L);

        assertFalse(response.getExecuted());
        assertEquals("当前未配置可用的 RPC 或代币合约地址，无法执行手动补扫", response.getRemark());
        verify(web3Gateway, never()).getLatestBlockNumber();
        verify(depositCandidateAppService, never()).detectAndStore(eq("ETH_SEPOLIA"), eq("USDT"), eq(6), anyList());
        verify(depositSettlementAppService, never()).refreshConfirmationsAndCredit("ETH_SEPOLIA", "USDT", 0L, 6);
    }

    @Test
    void shouldRejectInvalidManualRescanRange() {
        DepositRescanTask depositRescanTask = new DepositRescanTask(
                web3Gateway,
                depositCandidateAppService,
                depositSettlementAppService,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT"),
                new WalletWeb3Properties("http://localhost:8545", "0xToken", 0L, 200, 6, 6, "", 11155111L, 120000L, 3, 10L, 2)
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> depositRescanTask.runOnce(120L, 100L));

        assertEquals("fromBlock 不能大于 toBlock", exception.getMessage());
    }

    @Test
    void shouldRescanRequestedRangeAndClipToLatestBlock() {
        DepositRescanTask depositRescanTask = new DepositRescanTask(
                web3Gateway,
                depositCandidateAppService,
                depositSettlementAppService,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT"),
                new WalletWeb3Properties("http://localhost:8545", "0xToken", 0L, 200, 6, 6, "", 11155111L, 120000L, 3, 10L, 2)
        );
        when(web3Gateway.supportsTransferScan()).thenReturn(true);
        when(web3Gateway.getLatestBlockNumber()).thenReturn(150L);
        when(web3Gateway.getErc20TransferLogs("0xToken", 100L, 150L)).thenReturn(List.of(
                Erc20TransferLog.create(
                        "0xToken",
                        "0x1111111111111111111111111111111111111111",
                        "0x2222222222222222222222222222222222222222",
                        BigInteger.TEN,
                        "0xTxHash",
                        0,
                        120L
                )
        ));
        when(depositCandidateAppService.detectAndStore(eq("ETH_SEPOLIA"), eq("USDT"), eq(6), anyList())).thenReturn(1);
        when(depositSettlementAppService.refreshConfirmationsAndCredit("ETH_SEPOLIA", "USDT", 150L, 6)).thenReturn(1);

        DepositRescanResponse response = depositRescanTask.runOnce(100L, 180L);

        assertTrue(response.getExecuted());
        assertEquals(150L, response.getEffectiveToBlock());
        assertEquals(150L, response.getLatestBlock());
        assertEquals(1, response.getDetectedCount());
        assertEquals(1, response.getCreditedCount());
        assertEquals("补扫结束区块超过当前链头，已自动裁剪到最新区块", response.getRemark());
        verify(web3Gateway).getErc20TransferLogs("0xToken", 100L, 150L);
        verify(depositSettlementAppService).refreshConfirmationsAndCredit("ETH_SEPOLIA", "USDT", 150L, 6);
    }
}
