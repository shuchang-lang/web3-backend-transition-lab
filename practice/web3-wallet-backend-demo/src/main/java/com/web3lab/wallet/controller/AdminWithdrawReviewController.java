package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.withdraw.WithdrawOrderAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.ApproveWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.RejectWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.WithdrawOrderResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台提现审核对外接口。
 */
@RestController
@RequestMapping("/admin/withdraw/review")
public class AdminWithdrawReviewController {

    private final WithdrawOrderAppService withdrawOrderAppService;

    public AdminWithdrawReviewController(WithdrawOrderAppService withdrawOrderAppService) {
        this.withdrawOrderAppService = withdrawOrderAppService;
    }

    /**
     * 审核通过提现订单。
     *
     * @param request 审核通过请求
     * @return 提现订单响应
     */
    @PostMapping("/approve")
    public ApiResponse<WithdrawOrderResponse> approve(@Valid @RequestBody ApproveWithdrawOrderRequest request) {
        return ApiResponse.success(withdrawOrderAppService.approve(request));
    }

    /**
     * 审核拒绝提现订单。
     *
     * @param request 审核拒绝请求
     * @return 提现订单响应
     */
    @PostMapping("/reject")
    public ApiResponse<WithdrawOrderResponse> reject(@Valid @RequestBody RejectWithdrawOrderRequest request) {
        return ApiResponse.success(withdrawOrderAppService.reject(request));
    }
}
