package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.controller.dto.DepositCompensationResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountBillBizTypeConstants;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.deposit.DepositStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositCompensationTaskTest {

    @Mock
    private DepositRecordMapper depositRecordMapper;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private AccountBillMapper accountBillMapper;

    @Test
    void shouldReturnNoOpWhenNoCompensatableDepositExists() {
        DepositCompensationTask depositCompensationTask = new DepositCompensationTask(
                depositRecordMapper,
                accountBalanceMapper,
                accountBillMapper
        );
        when(depositRecordMapper.selectList(any())).thenReturn(List.of());

        DepositCompensationResponse response = depositCompensationTask.runOnce();

        assertFalse(response.getExecuted());
        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getCompensatedCount());
        assertEquals("当前没有需要冲正的已入账异常充值记录", response.getRemark());
        verify(accountBalanceMapper, never()).selectOne(any());
        verify(accountBillMapper, never()).insert(any(AccountBill.class));
    }

    @Test
    void shouldCompensateCreditedReorgDepositAndInsertBill() {
        DepositCompensationTask depositCompensationTask = new DepositCompensationTask(
                depositRecordMapper,
                accountBalanceMapper,
                accountBillMapper
        );
        DepositRecord depositRecord = creditedReorgDeposit(new BigDecimal("12.5"));
        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("20"));

        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        DepositCompensationResponse response = depositCompensationTask.runOnce();

        assertTrue(response.getExecuted());
        assertEquals(1, response.getProcessedCount());
        assertEquals(1, response.getCompensatedCount());
        assertEquals("已完成疑似链重组充值冲正", response.getRemark());

        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceMapper).updateById(balanceCaptor.capture());
        assertEquals(0, balanceCaptor.getValue().getAvailableBalance().compareTo(new BigDecimal("7.5")));
        assertEquals(0, balanceCaptor.getValue().getFrozenBalance().compareTo(BigDecimal.ZERO));

        ArgumentCaptor<AccountBill> billCaptor = ArgumentCaptor.forClass(AccountBill.class);
        verify(accountBillMapper).insert(billCaptor.capture());
        AccountBill accountBill = billCaptor.getValue();
        assertEquals(AccountBillBizTypeConstants.DEPOSIT_REORG_DEDUCT, accountBill.getBizType());
        assertEquals("10", accountBill.getBizId());
        assertEquals(0, accountBill.getChangeAmount().compareTo(new BigDecimal("-12.5")));
        assertEquals(0, accountBill.getAvailableAfter().compareTo(new BigDecimal("7.5")));
        assertEquals("充值链重组风险冲正，扣回此前已入账金额", accountBill.getRemark());

        ArgumentCaptor<DepositRecord> depositCaptor = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRecordMapper).updateById(depositCaptor.capture());
        assertEquals(DepositStatusConstants.REORG_COMPENSATED, depositCaptor.getValue().getStatus());
        assertNotNull(depositCaptor.getValue().getCreditedAt());
    }

    @Test
    void shouldCreateZeroBalanceAndAllowNegativeAvailableWhenBalanceDoesNotExist() {
        DepositCompensationTask depositCompensationTask = new DepositCompensationTask(
                depositRecordMapper,
                accountBalanceMapper,
                accountBillMapper
        );
        DepositRecord depositRecord = creditedReorgDeposit(new BigDecimal("8"));

        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));
        when(accountBalanceMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            AccountBalance inserted = invocation.getArgument(0);
            assertEquals(0, inserted.getAvailableBalance().compareTo(BigDecimal.ZERO));
            assertEquals(0, inserted.getFrozenBalance().compareTo(BigDecimal.ZERO));
            return 1;
        }).when(accountBalanceMapper).insert(any(AccountBalance.class));

        DepositCompensationResponse response = depositCompensationTask.runOnce();

        assertTrue(response.getExecuted());
        assertEquals(1, response.getProcessedCount());
        assertEquals(1, response.getCompensatedCount());

        verify(accountBalanceMapper).insert(any(AccountBalance.class));

        ArgumentCaptor<AccountBalance> updatedCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceMapper).updateById(updatedCaptor.capture());
        assertEquals(0, updatedCaptor.getValue().getAvailableBalance().compareTo(new BigDecimal("-8")));

        ArgumentCaptor<AccountBill> billCaptor = ArgumentCaptor.forClass(AccountBill.class);
        verify(accountBillMapper).insert(billCaptor.capture());
        assertEquals(0, billCaptor.getValue().getAvailableAfter().compareTo(new BigDecimal("-8")));

        ArgumentCaptor<DepositRecord> depositCaptor = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRecordMapper).updateById(depositCaptor.capture());
        assertEquals(DepositStatusConstants.REORG_COMPENSATED, depositCaptor.getValue().getStatus());
    }

    private DepositRecord creditedReorgDeposit(BigDecimal amount) {
        DepositRecord depositRecord = DepositRecord.createPending(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xToken",
                "0xFrom",
                "0xTo",
                "0xTxHash",
                0,
                amount,
                100L
        );
        depositRecord.setId(10L);
        depositRecord.markCredited(6L);
        depositRecord.markReorgSuspected();
        return depositRecord;
    }
}
