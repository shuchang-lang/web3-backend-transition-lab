package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.withdraw.WithdrawOrderAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.CreateWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.WithdrawOrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提现订单对外接口。
 */
@RestController
@RequestMapping("/withdraw")
public class WithdrawOrderController {

    private final WithdrawOrderAppService withdrawOrderAppService;

    public WithdrawOrderController(WithdrawOrderAppService withdrawOrderAppService) {
        this.withdrawOrderAppService = withdrawOrderAppService;
    }

    /**
     * 发起提现申请。
     *
     * @param request 创建提现订单请求
     * @return 提现订单响应
     */
    @PostMapping("/apply")
    public ApiResponse<WithdrawOrderResponse> apply(@Valid @RequestBody CreateWithdrawOrderRequest request) {
        return ApiResponse.success(withdrawOrderAppService.apply(request));
    }

    /**
     * 查询指定用户的提现订单列表。
     *
     * @param userId 平台用户 ID
     * @return 提现订单列表
     */
    @GetMapping("/list")
    public ApiResponse<List<WithdrawOrderResponse>> list(@RequestParam Long userId) {
        return ApiResponse.success(withdrawOrderAppService.listByUserId(userId));
    }

    /**
     * 按请求号查询提现订单。
     *
     * @param requestNo 客户端请求号
     * @return 提现订单响应
     */
    @GetMapping("/{requestNo}")
    public ApiResponse<WithdrawOrderResponse> getByRequestNo(@PathVariable String requestNo) {
        return ApiResponse.success(withdrawOrderAppService.getByRequestNo(requestNo));
    }
}
