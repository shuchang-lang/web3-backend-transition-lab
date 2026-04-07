package com.web3lab.wallet.application.account;

import com.web3lab.wallet.controller.dto.AccountReconcileResultResponse;
import com.web3lab.wallet.domain.account.AccountReconcileResult;
import com.web3lab.wallet.infrastructure.persistence.AccountReconcileResultMapper;
import java.math.BigDecimal;
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
class AccountReconcileResultAppServiceTest {

    @Mock
    private AccountReconcileResultMapper accountReconcileResultMapper;

    @Test
    void shouldListLatestBatchResultsByUserId() {
        AccountReconcileResult latest = buildResult(5L, "batch-2", 1001L, "ETH_SEPOLIA", "USDT", true);
        AccountReconcileResult anotherAsset = buildResult(6L, "batch-2", 1001L, "ETH_SEPOLIA", "USDC", false);

        when(accountReconcileResultMapper.selectOne(any())).thenReturn(latest);
        when(accountReconcileResultMapper.selectList(any())).thenReturn(List.of(latest, anotherAsset));

        AccountReconcileResultAppService service = new AccountReconcileResultAppService(accountReconcileResultMapper);
        List<AccountReconcileResultResponse> responses = service.listLatestByUserId(1001L);

        assertEquals(2, responses.size());
        assertEquals("batch-2", responses.get(0).getTaskBatchNo());
        assertEquals("USDC", responses.get(1).getTokenSymbol());
        assertTrue(Boolean.FALSE.equals(responses.get(1).getConsistent()));
    }

    @Test
    void shouldListResultsByTaskBatchNo() {
        AccountReconcileResult result = buildResult(7L, "batch-3", 1002L, "BSC_TESTNET", "USDT", true);
        when(accountReconcileResultMapper.selectList(any())).thenReturn(List.of(result));

        AccountReconcileResultAppService service = new AccountReconcileResultAppService(accountReconcileResultMapper);
        List<AccountReconcileResultResponse> responses = service.listByTaskBatchNo("batch-3");

        assertEquals(1, responses.size());
        assertEquals(1002L, responses.get(0).getUserId());
        assertEquals("BSC_TESTNET", responses.get(0).getChain());
        assertTrue(Boolean.TRUE.equals(responses.get(0).getConsistent()));
    }

    private AccountReconcileResult buildResult(Long id, String taskBatchNo, Long userId,
                                               String chain, String tokenSymbol, boolean consistent) {
        AccountReconcileResult result = new AccountReconcileResult();
        result.setId(id);
        result.setTaskName(AccountReconcileTask.ACCOUNT_RECONCILE_TASK);
        result.setTaskBatchNo(taskBatchNo);
        result.setUserId(userId);
        result.setChain(chain);
        result.setTokenSymbol(tokenSymbol);
        result.setAvailableBalance(new BigDecimal("10"));
        result.setFrozenBalance(BigDecimal.ZERO);
        result.setTotalBalance(new BigDecimal("10"));
        result.setCreditedDepositAmount(new BigDecimal("10"));
        result.setSuccessfulWithdrawAmount(BigDecimal.ZERO);
        result.setPendingWithdrawFrozenAmount(BigDecimal.ZERO);
        result.setBillAssetDeltaAmount(new BigDecimal("10"));
        result.setLatestBillAvailableAfter(new BigDecimal("10"));
        result.setLatestBillFrozenAfter(BigDecimal.ZERO);
        result.setBillCount(1);
        result.setConsistentWithBusinessTables(consistent);
        result.setConsistentWithAssetDeltaBills(consistent);
        result.setConsistentWithLatestBillSnapshot(consistent);
        result.setConsistentWithPendingWithdraws(consistent);
        result.setConsistent(consistent);
        result.setMismatchReason(consistent ? "" : "余额主表和业务表汇总结果不一致");
        result.setCreatedAt(LocalDateTime.of(2026, 4, 7, 20, 0));
        result.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 20, 0));
        return result;
    }
}
