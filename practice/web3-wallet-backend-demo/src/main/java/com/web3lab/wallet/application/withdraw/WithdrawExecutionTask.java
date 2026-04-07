package com.web3lab.wallet.application.withdraw;

import com.web3lab.wallet.application.task.TaskAuditLogAppService;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.WithdrawTaskRunResponse;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 提现执行任务入口。
 *
 * <p>当前最小版本把提现执行拆成两段任务：
 * 1. 广播待执行订单
 * 2. 同步已广播订单回执</p>
 */
@Component
public class WithdrawExecutionTask {

    /**
     * 提现广播任务名称。
     */
    public static final String WITHDRAW_BROADCAST_TASK = "withdraw_broadcast";

    /**
     * 提现回执同步任务名称。
     */
    public static final String WITHDRAW_RECEIPT_SYNC_TASK = "withdraw_receipt_sync";

    private static final Logger log = LoggerFactory.getLogger(WithdrawExecutionTask.class);

    private final WithdrawExecutionAppService withdrawExecutionAppService;
    private final WithdrawBroadcastLock withdrawBroadcastLock;
    private final TaskAuditLogAppService taskAuditLogAppService;
    private final Web3Gateway web3Gateway;
    private final WalletWeb3Properties walletWeb3Properties;

    public WithdrawExecutionTask(WithdrawExecutionAppService withdrawExecutionAppService,
                                 WithdrawBroadcastLock withdrawBroadcastLock,
                                 TaskAuditLogAppService taskAuditLogAppService,
                                 Web3Gateway web3Gateway,
                                 WalletWeb3Properties walletWeb3Properties) {
        this.withdrawExecutionAppService = withdrawExecutionAppService;
        this.withdrawBroadcastLock = withdrawBroadcastLock;
        this.taskAuditLogAppService = taskAuditLogAppService;
        this.web3Gateway = web3Gateway;
        this.walletWeb3Properties = walletWeb3Properties;
    }

    /**
     * 执行一轮提现广播任务。
     *
     * @return 任务执行响应
     */
    public WithdrawTaskRunResponse runBroadcastOnce() {
        String taskBatchNo = taskAuditLogAppService.nextBatchNo(WITHDRAW_BROADCAST_TASK);
        try {
            if (!walletWeb3Properties.readyForWithdrawBroadcast() || !web3Gateway.supportsWithdrawBroadcast()) {
                log.info("跳过提现广播任务，当前未配置可用的私钥或合约地址，gateway={}", web3Gateway.clientName());
                taskAuditLogAppService.recordSkipped(
                        WITHDRAW_BROADCAST_TASK,
                        taskBatchNo,
                        "当前未配置可用的私钥或代币合约地址",
                        "已跳过提现广播任务"
                );
                return new WithdrawTaskRunResponse(
                        WITHDRAW_BROADCAST_TASK,
                        0,
                        0,
                        LocalDateTime.now(),
                        false,
                        "当前未配置可用的私钥或代币合约地址，已跳过提现广播任务"
                );
            }

            if (!withdrawBroadcastLock.tryLock()) {
                log.info("跳过提现广播任务，当前已有另一轮广播任务在执行中");
                taskAuditLogAppService.recordSkipped(
                        WITHDRAW_BROADCAST_TASK,
                        taskBatchNo,
                        "当前已有另一轮提现广播任务在执行中",
                        "已跳过本轮提现广播请求"
                );
                return new WithdrawTaskRunResponse(
                        WITHDRAW_BROADCAST_TASK,
                        0,
                        0,
                        LocalDateTime.now(),
                        false,
                        "当前已有另一轮提现广播任务在执行中，已跳过本轮请求"
                );
            }
            try {
                WithdrawExecutionAppService.BroadcastBatchResult result = withdrawExecutionAppService.broadcastPendingOrders();
                if (result.processedCount() == 0) {
                    taskAuditLogAppService.recordSkipped(
                            WITHDRAW_BROADCAST_TASK,
                            taskBatchNo,
                            "当前没有待广播的提现订单",
                            "本轮没有可执行的提现广播订单"
                    );
                    return new WithdrawTaskRunResponse(
                            WITHDRAW_BROADCAST_TASK,
                            0,
                            0,
                            LocalDateTime.now(),
                            false,
                            "当前没有待广播的提现订单"
                    );
                }
                int warningCount = Math.max(result.processedCount() - result.updatedCount(), 0);
                taskAuditLogAppService.recordSuccess(
                        WITHDRAW_BROADCAST_TASK,
                        taskBatchNo,
                        result.processedCount(),
                        result.updatedCount(),
                        warningCount,
                        0,
                        buildBroadcastMetricSnapshot(result.processedCount(), result.updatedCount()),
                        "已按串行方式完成一轮提现广播"
                );
                return new WithdrawTaskRunResponse(
                        WITHDRAW_BROADCAST_TASK,
                        result.processedCount(),
                        result.updatedCount(),
                        LocalDateTime.now(),
                        true,
                        "已按串行方式完成一轮提现广播"
                );
            } finally {
                withdrawBroadcastLock.unlock();
            }
        } catch (RuntimeException ex) {
            taskAuditLogAppService.recordFailure(
                    WITHDRAW_BROADCAST_TASK,
                    taskBatchNo,
                    0,
                    0,
                    0,
                    1,
                    "",
                    resolveFailureReason(ex),
                    "提现广播任务执行失败"
            );
            throw ex;
        }
    }

    /**
     * 执行一轮提现回执同步任务。
     *
     * @return 任务执行响应
     */
    public WithdrawTaskRunResponse runReceiptSyncOnce() {
        if (!walletWeb3Properties.hasRpcUrl()) {
            log.info("跳过提现回执同步任务，当前未配置可用 RPC，gateway={}", web3Gateway.clientName());
            return new WithdrawTaskRunResponse(
                    WITHDRAW_RECEIPT_SYNC_TASK,
                    0,
                    0,
                    LocalDateTime.now(),
                    false,
                    "当前未配置可用 RPC，已跳过提现回执同步任务"
            );
        }

        WithdrawExecutionAppService.BroadcastBatchResult result = withdrawExecutionAppService.syncSubmittedReceipts();
        if (result.processedCount() == 0) {
            return new WithdrawTaskRunResponse(
                    WITHDRAW_RECEIPT_SYNC_TASK,
                    0,
                    0,
                    LocalDateTime.now(),
                    false,
                    "当前没有需要同步回执的提现订单"
            );
        }
        return new WithdrawTaskRunResponse(
                WITHDRAW_RECEIPT_SYNC_TASK,
                result.processedCount(),
                result.updatedCount(),
                LocalDateTime.now(),
                true,
                "已完成一轮提现回执同步"
        );
    }

    /**
     * 构建提现广播指标快照。
     *
     * @param processedCount 处理数量
     * @param updatedCount 推进成功数量
     * @return 指标快照文本
     */
    private String buildBroadcastMetricSnapshot(int processedCount, int updatedCount) {
        return "processedCount=" + processedCount + ",updatedCount=" + updatedCount;
    }

    /**
     * 解析任务失败原因。
     *
     * @param ex 异常对象
     * @return 失败原因
     */
    private String resolveFailureReason(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
