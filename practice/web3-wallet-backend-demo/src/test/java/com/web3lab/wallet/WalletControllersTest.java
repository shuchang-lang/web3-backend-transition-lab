package com.web3lab.wallet;

import com.web3lab.wallet.application.account.AccountBalanceAppService;
import com.web3lab.wallet.application.account.AccountReconcileAppService;
import com.web3lab.wallet.application.account.AccountReconcileResultAppService;
import com.web3lab.wallet.application.account.AccountReconcileTask;
import com.web3lab.wallet.application.deposit.DepositCompensationTask;
import com.web3lab.wallet.application.deposit.DepositReorgCheckTask;
import com.web3lab.wallet.application.deposit.DepositRecordAppService;
import com.web3lab.wallet.application.deposit.DepositRescanTask;
import com.web3lab.wallet.application.deposit.DepositScanTask;
import com.web3lab.wallet.application.scan.ChainScanProgressAppService;
import com.web3lab.wallet.application.address.WalletAddressAppService;
import com.web3lab.wallet.application.withdraw.WithdrawExecutionTask;
import com.web3lab.wallet.application.withdraw.WithdrawOrderAppService;
import com.web3lab.wallet.application.withdraw.WithdrawTimeoutCheckTask;
import com.web3lab.wallet.controller.AdminReconcileController;
import com.web3lab.wallet.controller.AdminWithdrawReviewController;
import com.web3lab.wallet.controller.AdminWithdrawTaskController;
import com.web3lab.wallet.common.exception.ApiExceptionHandler;
import com.web3lab.wallet.controller.AccountBalanceController;
import com.web3lab.wallet.controller.AdminTaskController;
import com.web3lab.wallet.controller.DepositRecordController;
import com.web3lab.wallet.controller.WithdrawOrderController;
import com.web3lab.wallet.controller.WalletAddressController;
import com.web3lab.wallet.controller.dto.AccountBalanceResponse;
import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import com.web3lab.wallet.controller.dto.AccountReconcileResultResponse;
import com.web3lab.wallet.controller.dto.AccountReconcileTaskResponse;
import com.web3lab.wallet.controller.dto.ChainScanProgressResponse;
import com.web3lab.wallet.controller.dto.CreateWalletAddressRequest;
import com.web3lab.wallet.controller.dto.DepositCompensationResponse;
import com.web3lab.wallet.controller.dto.DepositReorgCheckResponse;
import com.web3lab.wallet.controller.dto.DepositRecordResponse;
import com.web3lab.wallet.controller.dto.DepositRescanResponse;
import com.web3lab.wallet.controller.dto.ApproveWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.CreateWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.WithdrawTimeoutCheckResponse;
import com.web3lab.wallet.controller.dto.WithdrawTaskRunResponse;
import com.web3lab.wallet.controller.dto.WithdrawOrderResponse;
import com.web3lab.wallet.controller.dto.WalletAddressResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllersTest {

    @Mock
    private WalletAddressAppService walletAddressAppService;

    @Mock
    private AccountBalanceAppService accountBalanceAppService;

    @Mock
    private DepositRecordAppService depositRecordAppService;

    @Mock
    private ChainScanProgressAppService chainScanProgressAppService;

    @Mock
    private DepositScanTask depositScanTask;

    @Mock
    private DepositRescanTask depositRescanTask;

    @Mock
    private DepositReorgCheckTask depositReorgCheckTask;

    @Mock
    private DepositCompensationTask depositCompensationTask;

    @Mock
    private WithdrawOrderAppService withdrawOrderAppService;

    @Mock
    private WithdrawExecutionTask withdrawExecutionTask;

    @Mock
    private WithdrawTimeoutCheckTask withdrawTimeoutCheckTask;

    @Mock
    private AccountReconcileAppService accountReconcileAppService;

    @Mock
    private AccountReconcileTask accountReconcileTask;

    @Mock
    private AccountReconcileResultAppService accountReconcileResultAppService;

    @InjectMocks
    private WalletAddressController walletAddressController;

    @InjectMocks
    private AccountBalanceController accountBalanceController;

    @InjectMocks
    private DepositRecordController depositRecordController;

    @InjectMocks
    private AdminTaskController adminTaskController;

    @InjectMocks
    private AdminReconcileController adminReconcileController;

    @InjectMocks
    private WithdrawOrderController withdrawOrderController;

    @InjectMocks
    private AdminWithdrawReviewController adminWithdrawReviewController;

    @InjectMocks
    private AdminWithdrawTaskController adminWithdrawTaskController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        walletAddressController,
                        accountBalanceController,
                        depositRecordController,
                        adminTaskController,
                        adminReconcileController,
                        withdrawOrderController,
                        adminWithdrawReviewController,
                        adminWithdrawTaskController
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldAllocateAddress() throws Exception {
        when(walletAddressAppService.allocateAddress(any(CreateWalletAddressRequest.class)))
                .thenReturn(new WalletAddressResponse(
                        1L,
                        1001L,
                        "ETH_SEPOLIA",
                        "USDT",
                        "0x1234567890abcdef1234567890abcdef12345678",
                        "ACTIVE",
                        LocalDateTime.of(2026, 4, 7, 10, 0)
                ));

        mockMvc.perform(post("/wallet/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.chain").value("ETH_SEPOLIA"))
                .andExpect(jsonPath("$.data.tokenSymbol").value("USDT"));
    }

    @Test
    void shouldQueryBalance() throws Exception {
        when(accountBalanceAppService.getBalance(1001L, null, null))
                .thenReturn(new AccountBalanceResponse(
                        1001L,
                        "ETH_SEPOLIA",
                        "USDT",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        LocalDateTime.of(2026, 4, 7, 10, 5)
                ));

        mockMvc.perform(get("/account/balance/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableBalance").value(0))
                .andExpect(jsonPath("$.data.frozenBalance").value(0));
    }

    @Test
    void shouldQueryDepositList() throws Exception {
        when(depositRecordAppService.listByUserId(1001L))
                .thenReturn(List.of(new DepositRecordResponse(
                        10L,
                        1001L,
                        "ETH_SEPOLIA",
                        "USDT",
                        "0xToken",
                        "0xFrom",
                        "0xTo",
                        "0xTxHash",
                        0,
                        new BigDecimal("12.500000000000000000"),
                        123456L,
                        3L,
                        "PENDING_CONFIRM",
                        null,
                        LocalDateTime.of(2026, 4, 7, 12, 0)
                )));

        mockMvc.perform(get("/deposit/list").param("userId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(1001))
                .andExpect(jsonPath("$.data[0].txHash").value("0xTxHash"));
    }

    @Test
    void shouldRunDepositScanTask() throws Exception {
        when(depositScanTask.runOnce())
                .thenReturn(new ChainScanProgressResponse(
                        1L,
                        "ETH_SEPOLIA",
                        ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                        0L,
                        LocalDateTime.of(2026, 4, 7, 12, 10)
                ));

        mockMvc.perform(post("/admin/deposit/scan/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("erc20_deposit_scan"));
    }

    @Test
    void shouldRunDepositRescanTask() throws Exception {
        when(depositRescanTask.runOnce(100L, 120L))
                .thenReturn(new DepositRescanResponse(
                        DepositRescanTask.MANUAL_DEPOSIT_RESCAN_TASK,
                        100L,
                        120L,
                        120L,
                        150L,
                        2,
                        1,
                        true,
                        "补扫完成"
                ));

        mockMvc.perform(post("/admin/deposit/rescan/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromBlock": 100,
                                  "toBlock": 120
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("erc20_deposit_manual_rescan"))
                .andExpect(jsonPath("$.data.detectedCount").value(2));
    }

    @Test
    void shouldRunDepositReorgCheckTask() throws Exception {
        when(depositReorgCheckTask.runOnce())
                .thenReturn(new DepositReorgCheckResponse(
                        DepositReorgCheckTask.DEPOSIT_REORG_CHECK_TASK,
                        200L,
                        3,
                        1,
                        true,
                        "已标记疑似链重组充值记录"
                ));

        mockMvc.perform(post("/admin/deposit/reorg/check/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("erc20_deposit_reorg_check"))
                .andExpect(jsonPath("$.data.suspectedCount").value(1));
    }

    @Test
    void shouldRunDepositCompensationTask() throws Exception {
        when(depositCompensationTask.runOnce())
                .thenReturn(new DepositCompensationResponse(
                        DepositCompensationTask.DEPOSIT_COMPENSATION_TASK,
                        2,
                        2,
                        true,
                        "已完成疑似链重组充值冲正"
                ));

        mockMvc.perform(post("/admin/deposit/compensate/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("erc20_deposit_compensation"))
                .andExpect(jsonPath("$.data.compensatedCount").value(2));
    }

    @Test
    void shouldQueryAccountReconcileReport() throws Exception {
        AccountAssetReconcileResponse response = new AccountAssetReconcileResponse();
        response.setUserId(1001L);
        response.setChain("ETH_SEPOLIA");
        response.setTokenSymbol("USDT");
        response.setAvailableBalance(new BigDecimal("15"));
        response.setFrozenBalance(new BigDecimal("5"));
        response.setTotalBalance(new BigDecimal("20"));
        response.setCreditedDepositAmount(new BigDecimal("20"));
        response.setSuccessfulWithdrawAmount(BigDecimal.ZERO);
        response.setPendingWithdrawFrozenAmount(new BigDecimal("5"));
        response.setBillAssetDeltaAmount(new BigDecimal("20"));
        response.setLatestBillAvailableAfter(new BigDecimal("15"));
        response.setLatestBillFrozenAfter(new BigDecimal("5"));
        response.setBillCount(2);
        response.setConsistentWithBusinessTables(true);
        response.setConsistentWithAssetDeltaBills(true);
        response.setConsistentWithLatestBillSnapshot(true);
        response.setConsistentWithPendingWithdraws(true);
        response.setConsistent(true);
        response.setMismatchReason("");
        when(accountReconcileAppService.reconcile(1001L, null, null)).thenReturn(response);

        mockMvc.perform(get("/admin/reconcile/account/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.consistent").value(true))
                .andExpect(jsonPath("$.data.billCount").value(2));
    }

    @Test
    void shouldRunAccountReconcileTask() throws Exception {
        when(accountReconcileTask.runOnce())
                .thenReturn(new AccountReconcileTaskResponse(
                        AccountReconcileTask.ACCOUNT_RECONCILE_TASK,
                        "account_asset_auto_reconcile-20260407210000001",
                        3,
                        2,
                        1,
                        true,
                        "已完成自动对账，本轮发现 1 个不一致资产"
                ));

        mockMvc.perform(post("/admin/reconcile/task/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("account_asset_auto_reconcile"))
                .andExpect(jsonPath("$.data.inconsistentCount").value(1));
    }

    @Test
    void shouldQueryLatestAccountReconcileResults() throws Exception {
        AccountReconcileResultResponse response = new AccountReconcileResultResponse();
        response.setId(11L);
        response.setTaskName(AccountReconcileTask.ACCOUNT_RECONCILE_TASK);
        response.setTaskBatchNo("account_asset_auto_reconcile-20260407210000001");
        response.setUserId(1001L);
        response.setChain("ETH_SEPOLIA");
        response.setTokenSymbol("USDT");
        response.setAvailableBalance(new BigDecimal("15"));
        response.setFrozenBalance(new BigDecimal("5"));
        response.setTotalBalance(new BigDecimal("20"));
        response.setCreditedDepositAmount(new BigDecimal("20"));
        response.setSuccessfulWithdrawAmount(BigDecimal.ZERO);
        response.setPendingWithdrawFrozenAmount(new BigDecimal("5"));
        response.setBillAssetDeltaAmount(new BigDecimal("20"));
        response.setLatestBillAvailableAfter(new BigDecimal("15"));
        response.setLatestBillFrozenAfter(new BigDecimal("5"));
        response.setBillCount(2);
        response.setConsistentWithBusinessTables(true);
        response.setConsistentWithAssetDeltaBills(true);
        response.setConsistentWithLatestBillSnapshot(true);
        response.setConsistentWithPendingWithdraws(true);
        response.setConsistent(true);
        response.setMismatchReason("");
        response.setCreatedAt(LocalDateTime.of(2026, 4, 7, 21, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 21, 0));
        when(accountReconcileResultAppService.listLatestByUserId(1001L)).thenReturn(List.of(response));

        mockMvc.perform(get("/admin/reconcile/result/latest/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskBatchNo").value("account_asset_auto_reconcile-20260407210000001"))
                .andExpect(jsonPath("$.data[0].consistent").value(true));
    }

    @Test
    void shouldQueryAccountReconcileBatchResults() throws Exception {
        AccountReconcileResultResponse response = new AccountReconcileResultResponse();
        response.setId(12L);
        response.setTaskName(AccountReconcileTask.ACCOUNT_RECONCILE_TASK);
        response.setTaskBatchNo("account_asset_auto_reconcile-20260407210000001");
        response.setUserId(1002L);
        response.setChain("ETH_SEPOLIA");
        response.setTokenSymbol("USDC");
        response.setAvailableBalance(new BigDecimal("10"));
        response.setFrozenBalance(BigDecimal.ZERO);
        response.setTotalBalance(new BigDecimal("10"));
        response.setCreditedDepositAmount(new BigDecimal("12"));
        response.setSuccessfulWithdrawAmount(new BigDecimal("2"));
        response.setPendingWithdrawFrozenAmount(BigDecimal.ZERO);
        response.setBillAssetDeltaAmount(new BigDecimal("10"));
        response.setLatestBillAvailableAfter(new BigDecimal("10"));
        response.setLatestBillFrozenAfter(BigDecimal.ZERO);
        response.setBillCount(3);
        response.setConsistentWithBusinessTables(true);
        response.setConsistentWithAssetDeltaBills(true);
        response.setConsistentWithLatestBillSnapshot(true);
        response.setConsistentWithPendingWithdraws(true);
        response.setConsistent(true);
        response.setMismatchReason("");
        response.setCreatedAt(LocalDateTime.of(2026, 4, 7, 21, 1));
        response.setUpdatedAt(LocalDateTime.of(2026, 4, 7, 21, 1));
        when(accountReconcileResultAppService.listByTaskBatchNo("account_asset_auto_reconcile-20260407210000001"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/admin/reconcile/result/batch/account_asset_auto_reconcile-20260407210000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(1002))
                .andExpect(jsonPath("$.data[0].tokenSymbol").value("USDC"));
    }

    @Test
    void shouldApplyWithdrawOrder() throws Exception {
        when(withdrawOrderAppService.apply(any(CreateWithdrawOrderRequest.class)))
                .thenReturn(new WithdrawOrderResponse(
                        20L,
                        1001L,
                        "ETH_SEPOLIA",
                        "USDT",
                        "0xReceiver",
                        new BigDecimal("5"),
                        new BigDecimal("0.1"),
                        "REQ-10001",
                        "PENDING_REVIEW",
                        "PENDING",
                        null,
                        null,
                        null,
                        null,
                        0,
                        0,
                        null,
                        LocalDateTime.of(2026, 4, 7, 15, 20)
                ));

        mockMvc.perform(post("/withdraw/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1001,
                                  "toAddress": "0xReceiver",
                                  "amount": 5,
                                  "fee": 0.1,
                                  "requestNo": "REQ-10001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestNo").value("REQ-10001"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    @Test
    void shouldApproveWithdrawOrder() throws Exception {
        when(withdrawOrderAppService.approve(any(ApproveWithdrawOrderRequest.class)))
                .thenReturn(new WithdrawOrderResponse(
                        20L,
                        1001L,
                        "ETH_SEPOLIA",
                        "USDT",
                        "0xReceiver",
                        new BigDecimal("5"),
                        new BigDecimal("0.1"),
                        "REQ-10001",
                        "PENDING_BROADCAST",
                        "APPROVED",
                        "risk_admin",
                        LocalDateTime.of(2026, 4, 7, 16, 10),
                        null,
                        null,
                        0,
                        0,
                        null,
                        LocalDateTime.of(2026, 4, 7, 15, 20)
                ));

        mockMvc.perform(post("/admin/withdraw/review/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestNo": "REQ-10001",
                                  "reviewBy": "risk_admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.status").value("PENDING_BROADCAST"));
    }

    @Test
    void shouldRunWithdrawBroadcastTask() throws Exception {
        when(withdrawExecutionTask.runBroadcastOnce())
                .thenReturn(new WithdrawTaskRunResponse(
                        WithdrawExecutionTask.WITHDRAW_BROADCAST_TASK,
                        2,
                        1,
                        LocalDateTime.of(2026, 4, 7, 17, 0)
                ));

        mockMvc.perform(post("/admin/withdraw/broadcast/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("withdraw_broadcast"))
                .andExpect(jsonPath("$.data.updatedCount").value(1));
    }

    @Test
    void shouldRunWithdrawTimeoutCheckTask() throws Exception {
        when(withdrawTimeoutCheckTask.runOnce())
                .thenReturn(new WithdrawTimeoutCheckResponse(
                        WithdrawTimeoutCheckTask.WITHDRAW_TIMEOUT_CHECK_TASK,
                        3,
                        1,
                        1,
                        1,
                        0,
                        true,
                        "已完成提现回执超时巡检与异常治理"
                ));

        mockMvc.perform(post("/admin/withdraw/timeout/check/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskName").value("withdraw_receipt_timeout_check"))
                .andExpect(jsonPath("$.data.retryableCount").value(1))
                .andExpect(jsonPath("$.data.resolvedCount").value(1));
    }
}
