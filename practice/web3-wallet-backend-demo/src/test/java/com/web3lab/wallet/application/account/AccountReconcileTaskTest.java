package com.web3lab.wallet.application.account;

import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import com.web3lab.wallet.controller.dto.AccountReconcileTaskResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountReconcileResult;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountReconcileResultMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountReconcileTaskTest {

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private AccountBillMapper accountBillMapper;

    @Mock
    private DepositRecordMapper depositRecordMapper;

    @Mock
    private WithdrawOrderMapper withdrawOrderMapper;

    @Mock
    private AccountReconcileAppService accountReconcileAppService;

    @Mock
    private AccountReconcileResultMapper accountReconcileResultMapper;

    @Test
    void shouldPersistDistinctAssetResultsWhenRunningAutoReconcileTask() {
        AccountBalance balance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        AccountBill bill = AccountBill.createDepositCredit(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "DEP-1",
                new BigDecimal("20"),
                new BigDecimal("20"),
                BigDecimal.ZERO,
                "充值入账"
        );
        DepositRecord creditedDeposit = DepositRecord.createPending(
                1002L,
                "ETH_SEPOLIA",
                "USDC",
                "0xToken",
                "0xFrom",
                "0xTo",
                "0xTxHash",
                0,
                new BigDecimal("12"),
                100L
        );
        creditedDeposit.markCredited(6L);
        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1003L,
                "BSC_TESTNET",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.2"),
                "REQ-1"
        );

        when(accountBalanceMapper.selectList(any())).thenReturn(List.of(balance));
        when(accountBillMapper.selectList(any())).thenReturn(List.of(bill));
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(creditedDeposit));
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));
        when(accountReconcileAppService.reconcile(1001L, "ETH_SEPOLIA", "USDT"))
                .thenReturn(buildResponse(1001L, "ETH_SEPOLIA", "USDT", true, ""));
        when(accountReconcileAppService.reconcile(1002L, "ETH_SEPOLIA", "USDC"))
                .thenReturn(buildResponse(1002L, "ETH_SEPOLIA", "USDC", false, "余额主表和业务表汇总结果不一致"));
        when(accountReconcileAppService.reconcile(1003L, "BSC_TESTNET", "USDT"))
                .thenReturn(buildResponse(1003L, "BSC_TESTNET", "USDT", true, ""));

        AccountReconcileTask task = newTask();
        AccountReconcileTaskResponse response = task.runOnce();

        assertTrue(response.getExecuted());
        assertEquals(3, response.getProcessedAssetCount());
        assertEquals(2, response.getConsistentCount());
        assertEquals(1, response.getInconsistentCount());
        assertTrue(response.getRemark().contains("1 个不一致资产"));

        ArgumentCaptor<AccountReconcileResult> captor = ArgumentCaptor.forClass(AccountReconcileResult.class);
        verify(accountReconcileResultMapper, times(3)).insert((AccountReconcileResult) captor.capture());
        List<AccountReconcileResult> persistedResults = captor.getAllValues();
        assertEquals(3, persistedResults.size());
        assertNotNull(persistedResults.get(0).getTaskBatchNo());
        assertTrue(persistedResults.stream()
                .allMatch(result -> AccountReconcileTask.ACCOUNT_RECONCILE_TASK.equals(result.getTaskName())));
        assertTrue(persistedResults.stream()
                .allMatch(result -> persistedResults.get(0).getTaskBatchNo().equals(result.getTaskBatchNo())));
        assertTrue(persistedResults.stream().anyMatch(result -> Boolean.FALSE.equals(result.getConsistent())));
    }

    @Test
    void shouldSkipWhenNoAssetTargetsFound() {
        when(accountBalanceMapper.selectList(any())).thenReturn(List.of());
        when(accountBillMapper.selectList(any())).thenReturn(List.of());
        when(depositRecordMapper.selectList(any())).thenReturn(List.of());
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of());

        AccountReconcileTask task = newTask();
        AccountReconcileTaskResponse response = task.runOnce();

        assertFalse(response.getExecuted());
        assertEquals(0, response.getProcessedAssetCount());
        assertEquals(0, response.getConsistentCount());
        assertEquals(0, response.getInconsistentCount());
        verify(accountReconcileResultMapper, never()).insert(any(AccountReconcileResult.class));
        verifyNoInteractions(accountReconcileAppService);
    }

    private AccountReconcileTask newTask() {
        return new AccountReconcileTask(
                accountBalanceMapper,
                accountBillMapper,
                depositRecordMapper,
                withdrawOrderMapper,
                accountReconcileAppService,
                accountReconcileResultMapper
        );
    }

    private AccountAssetReconcileResponse buildResponse(Long userId, String chain, String tokenSymbol,
                                                        boolean consistent, String mismatchReason) {
        AccountAssetReconcileResponse response = new AccountAssetReconcileResponse();
        response.setUserId(userId);
        response.setChain(chain);
        response.setTokenSymbol(tokenSymbol);
        response.setAvailableBalance(new BigDecimal("10"));
        response.setFrozenBalance(BigDecimal.ZERO);
        response.setTotalBalance(new BigDecimal("10"));
        response.setCreditedDepositAmount(new BigDecimal("10"));
        response.setSuccessfulWithdrawAmount(BigDecimal.ZERO);
        response.setPendingWithdrawFrozenAmount(BigDecimal.ZERO);
        response.setBillAssetDeltaAmount(new BigDecimal("10"));
        response.setLatestBillAvailableAfter(new BigDecimal("10"));
        response.setLatestBillFrozenAfter(BigDecimal.ZERO);
        response.setBillCount(1);
        response.setConsistentWithBusinessTables(consistent);
        response.setConsistentWithAssetDeltaBills(true);
        response.setConsistentWithLatestBillSnapshot(true);
        response.setConsistentWithPendingWithdraws(true);
        response.setConsistent(consistent);
        response.setMismatchReason(mismatchReason);
        return response;
    }
}
