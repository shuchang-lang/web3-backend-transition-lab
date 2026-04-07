package com.web3lab.wallet.application.withdraw;

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
    private final Web3Gateway web3Gateway;
    private final WalletWeb3Properties walletWeb3Properties;

    public WithdrawTimeoutCheckTask(WithdrawExecutionAppService withdrawExecutionAppService,
                                    Web3Gateway web3Gateway,
                                    WalletWeb3Properties walletWeb3Properties) {
        this.withdrawExecutionAppService = withdrawExecutionAppService;
        this.web3Gateway = web3Gateway;
        this.walletWeb3Properties = walletWeb3Properties;
    }

    /**
     * 执行一轮提现回执超时巡检。
     *
     * @return 巡检结果
     */
    public WithdrawTimeoutCheckResponse runOnce() {
        if (!walletWeb3Properties.hasRpcUrl()) {
            log.info("跳过提现回执超时巡检，当前未配置可用 RPC，gateway={}", web3Gateway.clientName());
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
    }
}
