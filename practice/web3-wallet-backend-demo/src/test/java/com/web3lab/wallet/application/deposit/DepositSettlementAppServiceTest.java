package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountBillBizTypeConstants;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositSettlementAppServiceTest {

    @Mock
    private DepositRecordMapper depositRecordMapper;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private AccountBillMapper accountBillMapper;

    @InjectMocks
    private DepositSettlementAppService depositSettlementAppService;

    @Test
    void shouldOnlyRefreshConfirmationsWhenThresholdNotReached() {
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
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));

        int creditedCount = depositSettlementAppService.refreshConfirmationsAndCredit(
                "ETH_SEPOLIA",
                "USDT",
                102L,
                6
        );

        assertEquals(0, creditedCount);
        ArgumentCaptor<DepositRecord> captor = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRecordMapper).updateById(captor.capture());
        DepositRecord updatedRecord = captor.getValue();
        assertEquals(3L, updatedRecord.getConfirmations());
        assertEquals("PENDING_CONFIRM", updatedRecord.getStatus());
        verify(accountBalanceMapper, never()).updateById(any(AccountBalance.class));
        verify(accountBillMapper, never()).insert(any(AccountBill.class));
    }

    @Test
    void shouldCreditDepositWhenConfirmationsThresholdReached() {
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
        when(depositRecordMapper.selectList(any())).thenReturn(List.of(depositRecord));

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        int creditedCount = depositSettlementAppService.refreshConfirmationsAndCredit(
                "ETH_SEPOLIA",
                "USDT",
                105L,
                6
        );

        assertEquals(1, creditedCount);

        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceMapper).updateById(balanceCaptor.capture());
        assertEquals(0, balanceCaptor.getValue().getAvailableBalance().compareTo(new BigDecimal("12.5")));

        ArgumentCaptor<AccountBill> billCaptor = ArgumentCaptor.forClass(AccountBill.class);
        verify(accountBillMapper).insert(billCaptor.capture());
        AccountBill accountBill = billCaptor.getValue();
        assertEquals(AccountBillBizTypeConstants.DEPOSIT_CREDIT, accountBill.getBizType());
        assertEquals("10", accountBill.getBizId());
        assertEquals(0, accountBill.getAvailableAfter().compareTo(new BigDecimal("12.5")));

        ArgumentCaptor<DepositRecord> depositCaptor = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRecordMapper).updateById(depositCaptor.capture());
        DepositRecord creditedRecord = depositCaptor.getValue();
        assertEquals("CREDITED", creditedRecord.getStatus());
        assertEquals(6L, creditedRecord.getConfirmations());
        assertNotNull(creditedRecord.getCreditedAt());
    }
}
