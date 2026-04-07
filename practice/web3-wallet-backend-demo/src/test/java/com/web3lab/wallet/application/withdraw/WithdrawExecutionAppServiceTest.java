package com.web3lab.wallet.application.withdraw;

import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountBillBizTypeConstants;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.domain.withdraw.WithdrawStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import com.web3lab.wallet.infrastructure.web3.WithdrawBroadcastResult;
import com.web3lab.wallet.infrastructure.web3.WithdrawTransactionReceiptResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawExecutionAppServiceTest {

    @Mock
    private WithdrawOrderMapper withdrawOrderMapper;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @Mock
    private AccountBillMapper accountBillMapper;

    @Mock
    private Web3Gateway web3Gateway;

    @Test
    void shouldReserveNonceAndBroadcastApprovedWithdrawAndSaveTxHash() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20001"
        );
        withdrawOrder.setId(1L);
        withdrawOrder.approve("risk_admin");
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder), List.of());
        when(web3Gateway.getSuggestedWithdrawNonce()).thenReturn(12L);
        when(web3Gateway.broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 12L))
                .thenReturn(WithdrawBroadcastResult.create("0xTxHash", 12L));

        WithdrawExecutionAppService.BroadcastBatchResult result = service.broadcastPendingOrders();

        assertEquals(1, result.processedCount());
        assertEquals(1, result.updatedCount());

        ArgumentCaptor<WithdrawOrder> captor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper, times(2)).updateById(captor.capture());
        verify(web3Gateway).broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 12L);
        WithdrawOrder updatedOrder = captor.getValue();
        assertEquals("BROADCAST_SUBMITTED", updatedOrder.getStatus());
        assertEquals("0xTxHash", updatedOrder.getTxHash());
        assertEquals(12L, updatedOrder.getNonce());
        verify(accountBalanceMapper, never()).updateById(any(AccountBalance.class));
        verify(accountBillMapper, never()).insert(any(AccountBill.class));
    }

    @Test
    void shouldReuseReservedNonceWhenRetryBroadcast() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20001-RETRY"
        );
        withdrawOrder.setId(11L);
        withdrawOrder.approve("risk_admin");
        withdrawOrder.reserveNonce(18L);
        withdrawOrder.setFailReason("上一次节点超时");
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder), List.of(withdrawOrder));
        when(web3Gateway.getSuggestedWithdrawNonce()).thenReturn(9L);
        when(web3Gateway.broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 18L))
                .thenReturn(WithdrawBroadcastResult.create("0xRetryTxHash", 18L));

        WithdrawExecutionAppService.BroadcastBatchResult result = service.broadcastPendingOrders();

        assertEquals(1, result.processedCount());
        assertEquals(1, result.updatedCount());

        ArgumentCaptor<WithdrawOrder> captor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(captor.capture());
        verify(web3Gateway).broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 18L);
        assertEquals("BROADCAST_SUBMITTED", captor.getValue().getStatus());
        assertEquals("0xRetryTxHash", captor.getValue().getTxHash());
        assertEquals(18L, captor.getValue().getNonce());
        assertNull(captor.getValue().getFailReason());
    }

    @Test
    void shouldKeepPendingBroadcastWhenBroadcastFailureIsRetryable() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20001-FAIL"
        );
        withdrawOrder.setId(12L);
        withdrawOrder.approve("risk_admin");
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder), List.of());
        when(web3Gateway.getSuggestedWithdrawNonce()).thenReturn(21L);
        when(web3Gateway.broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 21L))
                .thenThrow(new IllegalStateException("upstream rpc timeout"));

        WithdrawExecutionAppService.BroadcastBatchResult result = service.broadcastPendingOrders();

        assertEquals(1, result.processedCount());
        assertEquals(0, result.updatedCount());

        ArgumentCaptor<WithdrawOrder> captor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper, times(2)).updateById(captor.capture());
        verify(web3Gateway).broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 21L);
        WithdrawOrder updatedOrder = captor.getValue();
        assertEquals("PENDING_BROADCAST", updatedOrder.getStatus());
        assertEquals(21L, updatedOrder.getNonce());
        assertEquals("upstream rpc timeout", updatedOrder.getFailReason());
        assertEquals(1, updatedOrder.getBroadcastRetryCount());
        assertNull(updatedOrder.getTxHash());
    }

    @Test
    void shouldMoveToManualHandleWhenBroadcastFailureIsManualLevel() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20001-MANUAL"
        );
        withdrawOrder.setId(13L);
        withdrawOrder.approve("risk_admin");
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder), List.of());
        when(web3Gateway.getSuggestedWithdrawNonce()).thenReturn(22L);
        when(web3Gateway.broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 22L))
                .thenThrow(new IllegalStateException("insufficient funds for gas * price + value"));

        WithdrawExecutionAppService.BroadcastBatchResult result = service.broadcastPendingOrders();

        assertEquals(1, result.processedCount());
        assertEquals(0, result.updatedCount());

        ArgumentCaptor<WithdrawOrder> captor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper, times(2)).updateById(captor.capture());
        WithdrawOrder updatedOrder = captor.getValue();
        assertEquals(WithdrawStatusConstants.MANUAL_HANDLE_REQUIRED, updatedOrder.getStatus());
        assertEquals(22L, updatedOrder.getNonce());
        assertEquals(1, updatedOrder.getBroadcastRetryCount());
        assertEquals("提现广播失败命中人工处理级错误，原因：insufficient funds for gas * price + value",
                updatedOrder.getFailReason());
    }

    @Test
    void shouldMoveToManualHandleWhenRetryableBroadcastFailureReachesMaxRetryCount() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20001-RETRY-LIMIT"
        );
        withdrawOrder.setId(14L);
        withdrawOrder.approve("risk_admin");
        withdrawOrder.reserveNonce(31L);
        withdrawOrder.setBroadcastRetryCount(2);
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder), List.of(withdrawOrder));
        when(web3Gateway.getSuggestedWithdrawNonce()).thenReturn(25L);
        when(web3Gateway.broadcastErc20Withdraw("0xToken", "0xReceiver", new BigDecimal("5"), 6, 31L))
                .thenThrow(new IllegalStateException("upstream rpc timeout"));

        WithdrawExecutionAppService.BroadcastBatchResult result = service.broadcastPendingOrders();

        assertEquals(1, result.processedCount());
        assertEquals(0, result.updatedCount());

        ArgumentCaptor<WithdrawOrder> captor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(captor.capture());
        WithdrawOrder updatedOrder = captor.getValue();
        assertEquals(WithdrawStatusConstants.MANUAL_HANDLE_REQUIRED, updatedOrder.getStatus());
        assertEquals(31L, updatedOrder.getNonce());
        assertEquals(3, updatedOrder.getBroadcastRetryCount());
        assertEquals("提现广播失败且已达到最大自动重试次数(3)，转人工处理，原因：upstream rpc timeout",
                updatedOrder.getFailReason());
    }

    @Test
    void shouldDeductFrozenBalanceWhenReceiptIsSuccessful() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20002"
        );
        withdrawOrder.setId(2L);
        withdrawOrder.approve("risk_admin");
        withdrawOrder.markBroadcastSubmitted("0xTxHash", 13L);
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));
        when(web3Gateway.getWithdrawTransactionReceipt("0xTxHash"))
                .thenReturn(WithdrawTransactionReceiptResult.success("0xTxHash"));

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("14.9"));
        accountBalance.setFrozenBalance(new BigDecimal("5.1"));
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        WithdrawExecutionAppService.BroadcastBatchResult result = service.syncSubmittedReceipts();

        assertEquals(1, result.processedCount());
        assertEquals(1, result.updatedCount());

        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceMapper).updateById(balanceCaptor.capture());
        assertEquals(0, balanceCaptor.getValue().getAvailableBalance().compareTo(new BigDecimal("14.9")));
        assertEquals(0, balanceCaptor.getValue().getFrozenBalance().compareTo(BigDecimal.ZERO));

        ArgumentCaptor<AccountBill> billCaptor = ArgumentCaptor.forClass(AccountBill.class);
        verify(accountBillMapper).insert(billCaptor.capture());
        assertEquals(AccountBillBizTypeConstants.WITHDRAW_DEDUCT, billCaptor.getValue().getBizType());
        assertEquals(0, billCaptor.getValue().getChangeAmount().compareTo(new BigDecimal("-5.1")));

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(orderCaptor.capture());
        assertEquals("SUCCESS", orderCaptor.getValue().getStatus());
    }

    @Test
    void shouldUnfreezeWhenReceiptIsFailed() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                "REQ-20003"
        );
        withdrawOrder.setId(3L);
        withdrawOrder.approve("risk_admin");
        withdrawOrder.markBroadcastSubmitted("0xTxHash", 14L);
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));
        when(web3Gateway.getWithdrawTransactionReceipt("0xTxHash"))
                .thenReturn(WithdrawTransactionReceiptResult.failed("0xTxHash", "execution reverted"));

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("14.9"));
        accountBalance.setFrozenBalance(new BigDecimal("5.1"));
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        WithdrawExecutionAppService.BroadcastBatchResult result = service.syncSubmittedReceipts();

        assertEquals(1, result.processedCount());
        assertEquals(1, result.updatedCount());

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
        assertEquals("FAILED", orderCaptor.getValue().getStatus());
        assertEquals("execution reverted", orderCaptor.getValue().getFailReason());
    }

    @Test
    void shouldKeepWaitingWhenSubmittedOrderHasNotTimedOut() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = timedOutSubmittedOrder("REQ-20004-WAIT", 15L, 0);
        withdrawOrder.setUpdatedAt(LocalDateTime.now().minusMinutes(3));
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));

        WithdrawExecutionAppService.TimeoutInspectionBatchResult result = service.inspectTimeoutSubmittedOrders();

        assertEquals(1, result.processedCount());
        assertEquals(1, result.continueWaitingCount());
        assertEquals(0, result.resolvedCount());
        assertEquals(0, result.retryableCount());
        assertEquals(0, result.manualHandleCount());
        verify(web3Gateway, never()).getWithdrawTransactionReceipt(any());
        verify(withdrawOrderMapper, never()).updateById(any(WithdrawOrder.class));
    }

    @Test
    void shouldFallbackToPendingBroadcastWhenReceiptTimeoutIsRetryable() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = timedOutSubmittedOrder("REQ-20004-RETRY", 16L, 0);
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));
        when(web3Gateway.getWithdrawTransactionReceipt("0xTimeoutTxHash-REQ-20004-RETRY"))
                .thenReturn(WithdrawTransactionReceiptResult.pending("0xTimeoutTxHash-REQ-20004-RETRY"));

        WithdrawExecutionAppService.TimeoutInspectionBatchResult result = service.inspectTimeoutSubmittedOrders();

        assertEquals(1, result.processedCount());
        assertEquals(0, result.continueWaitingCount());
        assertEquals(0, result.resolvedCount());
        assertEquals(1, result.retryableCount());
        assertEquals(0, result.manualHandleCount());

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(orderCaptor.capture());
        assertEquals(WithdrawStatusConstants.PENDING_BROADCAST, orderCaptor.getValue().getStatus());
        assertEquals(1, orderCaptor.getValue().getReceiptCheckRetryCount());
        assertEquals("提现回执等待超过 10 分钟，转回待广播重试", orderCaptor.getValue().getFailReason());
    }

    @Test
    void shouldMoveToManualHandleWhenReceiptTimeoutRetryCountExceeded() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = timedOutSubmittedOrder("REQ-20004-MANUAL", 17L, 2);
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));
        when(web3Gateway.getWithdrawTransactionReceipt("0xTimeoutTxHash-REQ-20004-MANUAL"))
                .thenReturn(WithdrawTransactionReceiptResult.pending("0xTimeoutTxHash-REQ-20004-MANUAL"));

        WithdrawExecutionAppService.TimeoutInspectionBatchResult result = service.inspectTimeoutSubmittedOrders();

        assertEquals(1, result.processedCount());
        assertEquals(0, result.continueWaitingCount());
        assertEquals(0, result.resolvedCount());
        assertEquals(0, result.retryableCount());
        assertEquals(1, result.manualHandleCount());

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(orderCaptor.capture());
        assertEquals(WithdrawStatusConstants.MANUAL_HANDLE_REQUIRED, orderCaptor.getValue().getStatus());
        assertEquals(2, orderCaptor.getValue().getReceiptCheckRetryCount());
        assertEquals("提现回执等待超过 10 分钟且已达到最大重试次数(2)，转人工处理",
                orderCaptor.getValue().getFailReason());
    }

    @Test
    void shouldSettleSuccessWhenReceiptAppearsDuringTimeoutInspection() {
        WithdrawExecutionAppService service = newService();

        WithdrawOrder withdrawOrder = timedOutSubmittedOrder("REQ-20004-RESOLVED", 18L, 1);
        when(withdrawOrderMapper.selectList(any())).thenReturn(List.of(withdrawOrder));
        when(web3Gateway.getWithdrawTransactionReceipt("0xTimeoutTxHash-REQ-20004-RESOLVED"))
                .thenReturn(WithdrawTransactionReceiptResult.success("0xTimeoutTxHash-REQ-20004-RESOLVED"));

        AccountBalance accountBalance = AccountBalance.createZero(1001L, "ETH_SEPOLIA", "USDT");
        accountBalance.setId(1L);
        accountBalance.setAvailableBalance(new BigDecimal("14.9"));
        accountBalance.setFrozenBalance(new BigDecimal("5.1"));
        when(accountBalanceMapper.selectOne(any())).thenReturn(accountBalance);

        WithdrawExecutionAppService.TimeoutInspectionBatchResult result = service.inspectTimeoutSubmittedOrders();

        assertEquals(1, result.processedCount());
        assertEquals(0, result.continueWaitingCount());
        assertEquals(1, result.resolvedCount());
        assertEquals(0, result.retryableCount());
        assertEquals(0, result.manualHandleCount());

        ArgumentCaptor<WithdrawOrder> orderCaptor = ArgumentCaptor.forClass(WithdrawOrder.class);
        verify(withdrawOrderMapper).updateById(orderCaptor.capture());
        assertEquals(WithdrawStatusConstants.SUCCESS, orderCaptor.getValue().getStatus());
        verify(accountBillMapper).insert(any(AccountBill.class));
        verify(accountBalanceMapper).updateById(any(AccountBalance.class));
    }

    private WithdrawOrder timedOutSubmittedOrder(String requestNo, Long nonce, int retryCount) {
        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0xReceiver",
                new BigDecimal("5"),
                new BigDecimal("0.1"),
                requestNo
        );
        withdrawOrder.setId(40L);
        withdrawOrder.approve("risk_admin");
        withdrawOrder.markBroadcastSubmitted("0xTimeoutTxHash-" + requestNo, nonce);
        withdrawOrder.setReceiptCheckRetryCount(retryCount);
        withdrawOrder.setUpdatedAt(LocalDateTime.now().minusMinutes(15));
        return withdrawOrder;
    }

    private WithdrawExecutionAppService newService() {
        return new WithdrawExecutionAppService(
                withdrawOrderMapper,
                accountBalanceMapper,
                accountBillMapper,
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
