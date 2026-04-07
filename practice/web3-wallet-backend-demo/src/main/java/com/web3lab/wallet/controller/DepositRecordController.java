package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.deposit.DepositRecordAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.DepositRecordResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 充值记录对外接口。
 */
@RestController
@RequestMapping("/deposit")
public class DepositRecordController {

    private final DepositRecordAppService depositRecordAppService;

    public DepositRecordController(DepositRecordAppService depositRecordAppService) {
        this.depositRecordAppService = depositRecordAppService;
    }

    /**
     * 查询指定用户的充值记录列表。
     *
     * @param userId 平台用户 ID
     * @return 充值记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<DepositRecordResponse>> list(@RequestParam Long userId) {
        return ApiResponse.success(depositRecordAppService.listByUserId(userId));
    }

    /**
     * 按交易哈希查询充值记录。
     *
     * @param txHash 链上交易哈希
     * @return 充值记录响应
     */
    @GetMapping("/{txHash}")
    public ApiResponse<DepositRecordResponse> getByTxHash(@PathVariable String txHash) {
        return ApiResponse.success(depositRecordAppService.getByTxHash(txHash));
    }
}
