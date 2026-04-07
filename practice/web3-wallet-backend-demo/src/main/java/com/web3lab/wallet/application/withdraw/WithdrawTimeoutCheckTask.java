package com.web3lab.wallet.application.withdraw;

import com.web3lab.wallet.application.task.TaskAuditLogAppService;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.WithdrawTimeoutCheckResponse;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 提现回执超时巡检任务入口。
 */
@Component
public class WithdrawTimeoutCheckTask {

    /**
     * 提现回执超时巡检任务名称。
     */
    public static final String WITHDRAW_TIMEOUT_CHECK_TASK = "withdraw_receipt_timeout_check";

    private static final Logger log = LoggerFactory.getLogger(WithdrawTimeoutCheckTask.class);

    private final WithdrawExecutionAppService withdrawExecutionAppService;
    private final TaskAuditLogAppService taskAuditLogAppService;
    private final Web3Gateway web3Gateway;
    private final WalletWeb3Properties walletWeb3Properties;

    public WithdrawTimeoutCheckTask(WithdrawExecutionAppService withdrawExecutionAppService,
                                    TaskAuditLogAppService taskAuditLogAppService,
                                    Web3Gateway web3Gateway,
                                    WalletWeb3Properties walletWeb3Properties) {
        this.withdrawExecutionAppService = withdrawExecutionAppService;
        this.taskAuditLogAppService = taskAuditLogAppService;
        this.web3Gateway = web3Gateway;
        this.walletWeb3Properties = walletWeb3Properties;
    }

    /**
     * 执行一轮提现回执超时巡检。
     *
     * @return 巡检结果
     */
    public WithdrawTimeoutCheckResponse runOnce() {
        String taskBatchNo = taskAuditLogAppService.nextBatchNo(WITHDRAW_TIMEOUT_CHECK_TASK);
        try {
            if (!walletWeb3Properties.hasRpcUrl()) {
                log.info("跳过提现回执超时巡检，当前未配置可用 RPC，gateway={}", web3Gateway.clientName());
                taskAuditLogAppService.recordSkipped(
                        WITHDRAW_TIMEOUT_CHECK_TASK,
                        taskBatchNo,
                        "当前未配置可用 RPC",
                        "已跳过提现回执超时巡检"
                );
                return new WithdrawTimeoutCheckResponse(
                        WITHDRAW_TIMEOUT_CHECK_TASK,
                        0,
                        0,
                        0,
                        0,
                        0,
                        false,
                        "当前未配置可用 RPC，已跳过提现回执超时巡检"
                );
            }

            WithdrawExecutionAppService.TimeoutInspectionBatchResult result =
                    withdrawExecutionAppService.inspectTimeoutSubmittedOrders();
            if (result.processedCount() == 0) {
                taskAuditLogAppService.recordSkipped(
                        WITHDRAW_TIMEOUT_CHECK_TASK,
                        taskBatchNo,
                        "当前没有需要巡检的已广播提现订单",
                        "本轮回执超时巡检没有处理对象"
                );
                return new WithdrawTimeoutCheckResponse(
                        WITHDRAW_TIMEOUT_CHECK_TASK,
                        0,
                        0,
                        0,
                        0,
                        0,
                        false,
                        "当前没有需要巡检的已广播提现订单"
                );
            }
            String remark = result.retryableCount() > 0 || result.manualHandleCount() > 0 || result.resolvedCount() > 0
                    ? "已完成提现回执超时巡检与异常治理"
                    : "已完成提现回执超时巡检，本轮订单继续等待回执";
            taskAuditLogAppService.recordSuccess(
                    WITHDRAW_TIMEOUT_CHECK_TASK,
                    taskBatchNo,
                    result.processedCount(),
                    result.resolvedCount(),
                    result.retryableCount(),
                    result.manualHandleCount(),
                    buildMetricSnapshot(
                            result.continueWaitingCount(),
                            result.resolvedCount(),
                            result.retryableCount(),
                            result.manualHandleCount()
                    ),
                    remark
            );
            return new WithdrawTimeoutCheckResponse(
                    WITHDRAW_TIMEOUT_CHECK_TASK,
                    result.processedCount(),
                    result.continueWaitingCount(),
                    result.resolvedCount(),
                    result.retryableCount(),
                    result.manualHandleCount(),
                    true,
                    remark
            );
        } catch (RuntimeException ex) {
            taskAuditLogAppService.recordFailure(
                    WITHDRAW_TIMEOUT_CHECK_TASK,
                    taskBatchNo,
                    0,
                    0,
                    0,
                    1,
                    "",
                    resolveFailureReason(ex),
                    "提现回执超时巡检执行失败"
            );
            throw ex;
        }
    }

    /**
     * 构建回执超时巡检指标快照。
     *
     * @param continueWaitingCount 继续等待数量
     * @param resolvedCount 已解决数量
     * @param retryableCount 可重试数量
     * @param manualHandleCount 转人工数量
     * @return 指标快照文本
     */
    private String buildMetricSnapshot(int continueWaitingCount, int resolvedCount,
                                       int retryableCount, int manualHandleCount) {
        return "continueWaitingCount=" + continueWaitingCount
                + ",resolvedCount=" + resolvedCount
                + ",retryableCount=" + retryableCount
                + ",manualHandleCount=" + manualHandleCount;
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
