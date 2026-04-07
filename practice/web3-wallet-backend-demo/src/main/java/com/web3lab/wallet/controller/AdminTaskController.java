package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.deposit.DepositCompensationTask;
import com.web3lab.wallet.application.deposit.DepositReorgCheckTask;
import com.web3lab.wallet.application.deposit.DepositRescanTask;
import com.web3lab.wallet.application.deposit.DepositScanTask;
import com.web3lab.wallet.application.scan.ChainScanProgressAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.ChainScanProgressResponse;
import com.web3lab.wallet.controller.dto.DepositCompensationResponse;
import com.web3lab.wallet.controller.dto.DepositReorgCheckResponse;
import com.web3lab.wallet.controller.dto.DepositRescanRequest;
import com.web3lab.wallet.controller.dto.DepositRescanResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台任务对外接口。
 */
@RestController
@RequestMapping("/admin")
public class AdminTaskController {

    private final DepositScanTask depositScanTask;
    private final DepositRescanTask depositRescanTask;
    private final DepositReorgCheckTask depositReorgCheckTask;
    private final DepositCompensationTask depositCompensationTask;
    private final ChainScanProgressAppService chainScanProgressAppService;

    public AdminTaskController(DepositScanTask depositScanTask,
                               DepositRescanTask depositRescanTask,
                               DepositReorgCheckTask depositReorgCheckTask,
                               DepositCompensationTask depositCompensationTask,
                               ChainScanProgressAppService chainScanProgressAppService) {
        this.depositScanTask = depositScanTask;
        this.depositRescanTask = depositRescanTask;
        this.depositReorgCheckTask = depositReorgCheckTask;
        this.depositCompensationTask = depositCompensationTask;
        this.chainScanProgressAppService = chainScanProgressAppService;
    }

    /**
     * 手动触发一次 ERC-20 充值扫描任务。
     *
     * @return 当前扫描进度
     */
    @PostMapping("/deposit/scan/run")
    public ApiResponse<ChainScanProgressResponse> runDepositScan() {
        return ApiResponse.success(depositScanTask.runOnce());
    }

    /**
     * 手动补扫指定区块区间的 ERC-20 充值日志。
     *
     * @param request 手动补扫请求
     * @return 补扫结果
     */
    @PostMapping("/deposit/rescan/run")
    public ApiResponse<DepositRescanResponse> runDepositRescan(@Valid @RequestBody DepositRescanRequest request) {
        return ApiResponse.success(depositRescanTask.runOnce(request.getFromBlock(), request.getToBlock()));
    }

    /**
     * 手动触发一轮充值链重组风险检查。
     *
     * @return 检查结果
     */
    @PostMapping("/deposit/reorg/check/run")
    public ApiResponse<DepositReorgCheckResponse> runDepositReorgCheck() {
        return ApiResponse.success(depositReorgCheckTask.runOnce());
    }

    /**
     * 手动触发一轮充值链重组补偿任务。
     *
     * @return 补偿结果
     */
    @PostMapping("/deposit/compensate/run")
    public ApiResponse<DepositCompensationResponse> runDepositCompensation() {
        return ApiResponse.success(depositCompensationTask.runOnce());
    }

    /**
     * 查询指定扫描任务的当前进度。
     *
     * @param taskName 任务名称
     * @param chain 链编码，未传时使用系统默认链
     * @return 扫描进度响应
     */
    @GetMapping("/scan/progress/{taskName}")
    public ApiResponse<ChainScanProgressResponse> getProgress(@PathVariable String taskName,
                                                              @RequestParam(required = false) String chain) {
        return ApiResponse.success(chainScanProgressAppService.getOrInitProgress(chain, taskName));
    }
}
