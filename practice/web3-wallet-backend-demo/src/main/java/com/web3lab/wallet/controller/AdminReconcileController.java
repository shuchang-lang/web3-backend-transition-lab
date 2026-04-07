package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.account.AccountReconcileAppService;
import com.web3lab.wallet.application.account.AccountReconcileResultAppService;
import com.web3lab.wallet.application.account.AccountReconcileTask;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import com.web3lab.wallet.controller.dto.AccountReconcileResultResponse;
import com.web3lab.wallet.controller.dto.AccountReconcileTaskResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台对账查询接口。
 */
@RestController
@RequestMapping("/admin/reconcile")
public class AdminReconcileController {

    private final AccountReconcileAppService accountReconcileAppService;
    private final AccountReconcileTask accountReconcileTask;
    private final AccountReconcileResultAppService accountReconcileResultAppService;

    public AdminReconcileController(AccountReconcileAppService accountReconcileAppService,
                                    AccountReconcileTask accountReconcileTask,
                                    AccountReconcileResultAppService accountReconcileResultAppService) {
        this.accountReconcileAppService = accountReconcileAppService;
        this.accountReconcileTask = accountReconcileTask;
        this.accountReconcileResultAppService = accountReconcileResultAppService;
    }

    /**
     * 查询指定用户资产的最小对账结果。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码，未传时使用默认链
     * @param tokenSymbol 币种符号，未传时使用默认币种
     * @return 对账结果响应
     */
    @GetMapping("/account/{userId}")
    public ApiResponse<AccountAssetReconcileResponse> reconcileAccount(@PathVariable Long userId,
                                                                       @RequestParam(required = false) String chain,
                                                                       @RequestParam(required = false) String tokenSymbol) {
        return ApiResponse.success(accountReconcileAppService.reconcile(userId, chain, tokenSymbol));
    }

    /**
     * 手动触发一轮账户资产自动对账任务。
     *
     * <p>Day27 第一版先保留后台手动触发入口，后续如果切到真正的定时调度，
     * 也可以继续复用同一个 `runOnce()` 任务实现。</p>
     *
     * @return 任务执行结果
     */
    @PostMapping("/task/run")
    public ApiResponse<AccountReconcileTaskResponse> runReconcileTask() {
        return ApiResponse.success(accountReconcileTask.runOnce());
    }

    /**
     * 查询指定用户最近一轮自动对账结果。
     *
     * @param userId 平台用户 ID
     * @return 最近一轮自动对账结果
     */
    @GetMapping("/result/latest/{userId}")
    public ApiResponse<List<AccountReconcileResultResponse>> getLatestResultByUserId(@PathVariable Long userId) {
        return ApiResponse.success(accountReconcileResultAppService.listLatestByUserId(userId));
    }

    /**
     * 查询指定批次号下的自动对账结果。
     *
     * @param taskBatchNo 任务批次号
     * @return 对应批次的自动对账结果
     */
    @GetMapping("/result/batch/{taskBatchNo}")
    public ApiResponse<List<AccountReconcileResultResponse>> getBatchResults(@PathVariable String taskBatchNo) {
        return ApiResponse.success(accountReconcileResultAppService.listByTaskBatchNo(taskBatchNo));
    }
}
