package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.application.scan.ChainScanProgressAppService;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.ChainScanProgressResponse;
import com.web3lab.wallet.infrastructure.web3.Erc20TransferLog;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositScanTaskTest {

    @Mock
    private ChainScanProgressAppService chainScanProgressAppService;

    @Mock
    private Web3Gateway web3Gateway;

    @Mock
    private DepositCandidateAppService depositCandidateAppService;

    @Mock
    private DepositSettlementAppService depositSettlementAppService;

    @Test
    void shouldSkipRealScanWhenRpcConfigIsMissing() {
        DepositScanTask depositScanTask = new DepositScanTask(
                chainScanProgressAppService,
                web3Gateway,
                depositCandidateAppService,
                depositSettlementAppService,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT"),
                new WalletWeb3Properties("", "", 0L, 200, 6, 6, "", 11155111L, 120000L, 3, 10L, 2)
        );
        ChainScanProgressResponse initProgress = new ChainScanProgressResponse(
                1L,
                "ETH_SEPOLIA",
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                0L,
                LocalDateTime.of(2026, 4, 7, 13, 0)
        );
        when(chainScanProgressAppService.getOrInitProgress(null, ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK))
                .thenReturn(initProgress);

        ChainScanProgressResponse response = depositScanTask.runOnce();

        assertEquals(0L, response.getLastScannedBlock());
        verify(web3Gateway, never()).getLatestBlockNumber();
        verify(depositCandidateAppService, never()).detectAndStore(eq("ETH_SEPOLIA"), eq("USDT"), eq(6), anyList());
        verify(depositSettlementAppService, never()).refreshConfirmationsAndCredit("ETH_SEPOLIA", "USDT", 0L, 6);
    }

    @Test
    void shouldScanNextBlockWindowAndAdvanceProgress() {
        DepositScanTask depositScanTask = new DepositScanTask(
                chainScanProgressAppService,
                web3Gateway,
                depositCandidateAppService,
                depositSettlementAppService,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT"),
                new WalletWeb3Properties("http://localhost:8545", "0xToken", 100L, 50, 6, 6, "", 11155111L, 120000L, 3, 10L, 2)
        );
        ChainScanProgressResponse initProgress = new ChainScanProgressResponse(
                1L,
                "ETH_SEPOLIA",
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                120L,
                LocalDateTime.of(2026, 4, 7, 13, 5)
        );
        ChainScanProgressResponse updatedProgress = new ChainScanProgressResponse(
                1L,
                "ETH_SEPOLIA",
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                170L,
                LocalDateTime.of(2026, 4, 7, 13, 10)
        );
        when(chainScanProgressAppService.getOrInitProgress(null, ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK))
                .thenReturn(initProgress);
        when(web3Gateway.supportsTransferScan()).thenReturn(true);
        when(web3Gateway.getLatestBlockNumber()).thenReturn(180L);
        when(web3Gateway.getErc20TransferLogs("0xToken", 121L, 170L)).thenReturn(List.of(
                Erc20TransferLog.create(
                        "0xToken",
                        "0x1111111111111111111111111111111111111111",
                        "0x2222222222222222222222222222222222222222",
                        BigInteger.TEN,
                        "0xTxHash",
                        0,
                        130L
                )
        ));
        when(depositCandidateAppService.detectAndStore(eq("ETH_SEPOLIA"), eq("USDT"), eq(6), anyList()))
                .thenReturn(1);
        when(depositSettlementAppService.refreshConfirmationsAndCredit("ETH_SEPOLIA", "USDT", 180L, 6))
                .thenReturn(1);
        when(chainScanProgressAppService.updateProgress(
                "ETH_SEPOLIA",
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                170L
        )).thenReturn(updatedProgress);

        ChainScanProgressResponse response = depositScanTask.runOnce();

        assertEquals(170L, response.getLastScannedBlock());
        verify(web3Gateway).getErc20TransferLogs("0xToken", 121L, 170L);
        verify(depositSettlementAppService).refreshConfirmationsAndCredit("ETH_SEPOLIA", "USDT", 180L, 6);
        verify(chainScanProgressAppService).updateProgress(
                "ETH_SEPOLIA",
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                170L
        );
    }
}
