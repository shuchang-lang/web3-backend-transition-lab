package com.web3lab.wallet.application.account;

import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountBillBizTypeConstants;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountReconcileAppServiceTest {

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private AccountBillMapper accountBillMapper;

    @Mock
    private DepositRecordMapper depositRecordMapper;

    @Mock
    private WithdrawOrderMapper withdrawOrderMapper;

    @Test
    void shouldReturnConsistentResultWhenBalanceMatchesBusinessAndBills() {
        AccountReconcileAppService service = newService();

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setAvailableBalance(new BigDecimal("15"));
        accountBalance.setFrozenBalance(new BigDecimal("5"));

        AccountBill depositBill = AccountBill.createDepositCredit(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "DEP-1",
                new BigDecimal("20"),
                new BigDecimal("20"),
                BigDecimal.ZERO,
                "充值入账"
        );
        AccountBill freezeBill = AccountBill.createWithdrawFreeze(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "WD-1",
                new BigDecimal("-5"),
                new BigDecimal("15"),
                new BigDecimal("5"),
                "提现冻结"
        );

        DepositRecord depositRecord = DepositRecord.createPending(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xToken",
                "0xFrom",
                "0xTo",
                "0xTxHash",
                0,
                new BigDecimal("20"),
                100L
        );
        depositRecord.markCredited(6L);

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("4.8"),
                new BigDecimal("0.2"),
                "REQ-30001"
        );

        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);
        when(accountBillMapper.selectList(any())).thenReturn(List.of(depositBill, freezeBill));
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));

        AccountAssetReconcileResponse response = service.reconcile(1001L, null, null);

        assertTrue(response.getConsistent());
        assertTrue(response.getConsistentWithBusinessTables());
        assertTrue(response.getConsistentWithAssetDeltaBills());
        assertTrue(response.getConsistentWithLatestBillSnapshot());
        assertTrue(response.getConsistentWithPendingWithdraws());
        assertEquals(0, response.getTotalBalance().compareTo(new BigDecimal("20")));
        assertEquals(0, response.getPendingWithdrawFrozenAmount().compareTo(new BigDecimal("5.0")));
        assertEquals("", response.getMismatchReason());
    }

    @Test
    void shouldReturnMismatchReasonWhenBalanceDoesNotMatchLatestBillOrPendingWithdraw() {
        AccountReconcileAppService service = newService();

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setAvailableBalance(new BigDecimal("12"));
        accountBalance.setFrozenBalance(new BigDecimal("1"));

        AccountBill depositBill = AccountBill.createDepositCredit(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "DEP-2",
                new BigDecimal("20"),
                new BigDecimal("20"),
                BigDecimal.ZERO,
                "充值入账"
        );
        AccountBill deductBill = AccountBill.createWithdrawDeduct(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "WD-2",
                new BigDecimal("-7"),
                new BigDecimal("13"),
                BigDecimal.ZERO,
                "提现成功扣减"
        );

        DepositRecord depositRecord = DepositRecord.createPending(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xToken",
                "0xFrom",
                "0xTo",
                "0xTxHash2",
                1,
                new BigDecimal("20"),
                101L
        );
        depositRecord.markCredited(6L);

        WithdrawOrder successOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver1",
                new BigDecimal("6.5"),
                new BigDecimal("0.5"),
                "REQ-30002"
        );
        successOrder.approve("risk_admin");
        successOrder.markBroadcastSubmitted("0xTxHashSuccess", 8L);
        successOrder.markSuccess();

        WithdrawOrder pendingOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver2",
                new BigDecimal("2"),
                new BigDecimal("0.5"),
                "REQ-30003"
        );
        pendingOrder.approve("risk_admin");

        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);
        when(accountBillMapper.selectList(any())).thenReturn(List.of(depositBill, deductBill));
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(successOrder, pendingOrder));

        AccountAssetReconcileResponse response = service.reconcile(1001L, null, null);

        assertFalse(response.getConsistent());
        assertTrue(response.getConsistentWithBusinessTables());
        assertTrue(response.getConsistentWithAssetDeltaBills());
        assertFalse(response.getConsistentWithLatestBillSnapshot());
        assertFalse(response.getConsistentWithPendingWithdraws());
        assertTrue(response.getMismatchReason().contains("余额主表和最新流水快照不一致"));
        assertTrue(response.getMismatchReason().contains("冻结余额和待完成提现总额不一致"));
        assertEquals(0, response.getPendingWithdrawFrozenAmount().compareTo(new BigDecimal("2.5")));
    }

    @Test
    void shouldIncludeDepositReorgDeductWhenReconcilingBillAssetDelta() {
        AccountReconcileAppService service = newService();

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setAvailableBalance(new BigDecimal("13"));
        accountBalance.setFrozenBalance(BigDecimal.ZERO);

        AccountBill depositBill = AccountBill.createDepositCredit(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "DEP-3",
                new BigDecimal("20"),
                new BigDecimal("20"),
                BigDecimal.ZERO,
                "充值入账"
        );
        AccountBill reorgDeductBill = AccountBill.createDepositReorgDeduct(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "DEP-3",
                new BigDecimal("-7"),
                new BigDecimal("13"),
                BigDecimal.ZERO,
                "链重组冲正"
        );
        reorgDeductBill.setBizType(AccountBillBizTypeConstants.DEPOSIT_REORG_DEDUCT);

        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);
        when(accountBillMapper.selectList(any())).thenReturn(List.of(depositBill, reorgDeductBill));
        when(depositRecordMapper.selectList(any())).thenReturn(List.of());
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of());

        AccountAssetReconcileResponse response = service.reconcile(1001L, null, null);

        assertEquals(0, response.getTotalBalance().compareTo(new BigDecimal("13")));
        assertEquals(0, response.getBillAssetDeltaAmount().compareTo(new BigDecimal("13")));
        assertTrue(response.getConsistentWithAssetDeltaBills());
        assertTrue(response.getConsistentWithLatestBillSnapshot());
        assertTrue(response.getConsistentWithPendingWithdraws());
        assertFalse(response.getConsistent());
        assertTrue(response.getMismatchReason().contains("余额主表和业务表汇总结果不一致"));
    }

    private AccountReconcileAppService newService() {
        return new AccountReconcileAppService(
                accountBalanceMapper,
                accountBillMapper,
                depositRecordMapper,
                withdrawOrderMapper,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT")
        );
    }
}
