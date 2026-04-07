package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.withdraw.WithdrawTimeoutCheckTask;
import com.web3lab.wallet.application.withdraw.WithdrawExecutionTask;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.WithdrawTimeoutCheckResponse;
import com.web3lab.wallet.controller.dto.WithdrawTaskRunResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台提现执行任务对外接口。
 */
@RestController
@RequestMapping("/admin/withdraw")
public class AdminWithdrawTaskController {

    private final WithdrawExecutionTask withdrawExecutionTask;
    private final WithdrawTimeoutCheckTask withdrawTimeoutCheckTask;

    public AdminWithdrawTaskController(WithdrawExecutionTask withdrawExecutionTask,
                                       WithdrawTimeoutCheckTask withdrawTimeoutCheckTask) {
        this.withdrawExecutionTask = withdrawExecutionTask;
        this.withdrawTimeoutCheckTask = withdrawTimeoutCheckTask;
    }

    /**
     * 手动触发一轮提现广播任务。
     *
     * @return 任务执行响应
     */
    @PostMapping("/broadcast/run")
    public ApiResponse<WithdrawTaskRunResponse> runBroadcast() {
        return ApiResponse.success(withdrawExecutionTask.runBroadcastOnce());
    }

    /**
     * 手动触发一轮提现回执同步任务。
     *
     * @return 任务执行响应
     */
    @PostMapping("/receipt/sync")
    public ApiResponse<WithdrawTaskRunResponse> runReceiptSync() {
        return ApiResponse.success(withdrawExecutionTask.runReceiptSyncOnce());
    }

    /**
     * 手动触发一轮提现回执超时巡检任务。
     *
     * @return 巡检结果
     */
    @PostMapping("/timeout/check/run")
    public ApiResponse<WithdrawTimeoutCheckResponse> runTimeoutCheck() {
        return ApiResponse.success(withdrawTimeoutCheckTask.runOnce());
    }
}
