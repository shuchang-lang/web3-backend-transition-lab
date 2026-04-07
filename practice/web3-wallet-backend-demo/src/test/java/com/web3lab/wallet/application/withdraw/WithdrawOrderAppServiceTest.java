package com.web3lab.wallet.application.withdraw;

import com.web3lab.wallet.common.exception.BusinessException;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.ApproveWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.CreateWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.RejectWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.WithdrawOrderResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountBillBizTypeConstants;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawOrderAppServiceTest {

    @Mock
    private WithdrawOrderMapper withdrawOrderMapper;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private AccountBillMapper accountBillMapper;

    @Test
    void shouldFreezeBalanceAndCreateWithdrawOrder() {
        WithdrawOrderAppService withdrawOrderAppService = newService();
        when(withdrawOrderMapper.selectOne(any())).thenReturn(null);

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("20"));
        accountBalance.setFrozenBalance(BigDecimal.ZERO);
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        CreateWithdrawOrderRequest request = new CreateWithdrawOrderRequest();
        request.setUserId(1001L);
        request.setToAddress("0xReceiver");
        request.setAmount(new BigDecimal("5"));
        request.setFee(new BigDecimal("0.1"));
        request.setRequestNo("REQ-10001");

        WithdrawOrderResponse response = withdrawOrderAppService.apply(request);

        assertEquals("REQ-10001", response.getRequestNo());
        assertEquals("PENDING_REVIEW", response.getStatus());

        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceMapper).updateById(balanceCaptor.capture());
        assertEquals(0, balanceCaptor.getValue().getAvailableBalance().compareTo(new BigDecimal("14.9")));
        assertEquals(0, balanceCaptor.getValue().getFrozenBalance().compareTo(new BigDecimal("5.1")));

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).insert(orderCaptor.capture());
        assertEquals("PENDING", orderCaptor.getValue().getReviewStatus());

        ArgumentCaptor<AccountBill> billCaptor = ArgumentCaptor.forClass(AccountBill.class);
        verify(accountBillMapper).insert(billCaptor.capture());
        assertEquals(AccountBillBizTypeConstants.WITHDRAW_FREEZE, billCaptor.getValue().getBizType());
        assertEquals(0, billCaptor.getValue().getChangeAmount().compareTo(new BigDecimal("-5.1")));
    }

    @Test
    void shouldRejectWithdrawWhenAvailableBalanceIsInsufficient() {
        WithdrawOrderAppService withdrawOrderAppService = newService();
        when(withdrawOrderMapper.selectOne(any())).thenReturn(null);

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("2"));
        accountBalance.setFrozenBalance(BigDecimal.ZERO);
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        CreateWithdrawOrderRequest request = new CreateWithdrawOrderRequest();
        request.setUserId(1001L);
        request.setToAddress("0xReceiver");
        request.setAmount(new BigDecimal("5"));
        request.setFee(new BigDecimal("0.1"));
        request.setRequestNo("REQ-10002");

        BusinessException exception = assertThrows(BusinessException.class, () -> withdrawOrderAppService.apply(request));

        assertEquals("可用余额不足，无法发起提现申请", exception.getMessage());
        verify(accountBalanceMapper, never()).updateById(any(AccountBalance.class));
        verify(withdrawOrderMapper, never()).insert(any(WithdrawOrder.class));
        verify(accountBillMapper, never()).insert(any(AccountBill.class));
    }

    @Test
    void shouldApproveWithdrawOrderToPendingBroadcast() {
        WithdrawOrderAppService withdrawOrderAppService = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-10003"
        );
        withdrawOrder.setId(30L);
        when(withdrawOrderMapper.selectOne(any())).thenReturn(withdrawOrder);

        ApproveWithdrawOrderRequest request = new ApproveWithdrawOrderRequest();
        request.setRequestNo("REQ-10003");
        request.setReviewBy("risk_admin");

        WithdrawOrderResponse response = withdrawOrderAppService.approve(request);

        assertEquals("PENDING_BROADCAST", response.getStatus());
        assertEquals("APPROVED", response.getReviewStatus());
        assertEquals("risk_admin", response.getReviewBy());

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(orderCaptor.capture());
        assertEquals("PENDING_BROADCAST", orderCaptor.getValue().getStatus());
        assertEquals("APPROVED", orderCaptor.getValue().getReviewStatus());
        verify(accountBalanceMapper, never()).updateById(any(AccountBalance.class));
        verify(accountBillMapper, never()).insert(any(AccountBill.class));
    }

    @Test
    void shouldRejectWithdrawAndUnfreezeBalance() {
        WithdrawOrderAppService withdrawOrderAppService = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-10004"
        );
        withdrawOrder.setId(40L);
        when(withdrawOrderMapper.selectOne(any())).thenReturn(withdrawOrder);

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("14.9"));
        accountBalance.setFrozenBalance(new BigDecimal("5.1"));
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        RejectWithdrawOrderRequest request = new RejectWithdrawOrderRequest();
        request.setRequestNo("REQ-10004");
        request.setReviewBy("risk_admin");
        request.setRejectReason("命中风控规则");

        WithdrawOrderResponse response = withdrawOrderAppService.reject(request);

        assertEquals("REJECTED", response.getStatus());
        assertEquals("REJECTED", response.getReviewStatus());
        assertEquals("命中风控规则", response.getFailReason());

        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceMapper).updateById(balanceCaptor.capture());
        assertEquals(0, balanceCaptor.getValue().getAvailableBalance().compareTo(new BigDecimal("20.0")));
        assertEquals(0, balanceCaptor.getValue().getFrozenBalance().compareTo(BigDecimal.ZERO));

        ArgumentCaptor<AccountBill> billCaptor = ArgumentCaptor.forClass(AccountBill.class);
        verify(accountBillMapper).insert(billCaptor.capture());
        assertEquals(AccountBillBizTypeConstants.WITHDRAW_UNFREEZE, billCaptor.getValue().getBizType());
        assertEquals(0, billCaptor.getValue().getChangeAmount().compareTo(new BigDecimal("5.1")));

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(orderCaptor.capture());
        assertEquals("REJECTED", orderCaptor.getValue().getStatus());
        assertEquals("REJECTED", orderCaptor.getValue().getReviewStatus());
        assertEquals("命中风控规则", orderCaptor.getValue().getFailReason());
    }

    private WithdrawOrderAppService newService() {
        return new WithdrawOrderAppService(
                withdrawOrderMapper,
                accountBalanceMapper,
                accountBillMapper,
                new WalletDefaultsProperties("ETH_SEPOLIA", "USDT")
        );
    }
}
