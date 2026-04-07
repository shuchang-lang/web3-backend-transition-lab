package com.web3lab.wallet.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核通过提现订单请求。
 */
@Data
public class ApproveWithdrawOrderRequest {

    /**
     * 客户端请求号。
     */
    @NotBlank(message = "不能为空")
    private String requestNo;

    /**
     * 审核人。
     */
    @NotBlank(message = "不能为空")
    private String reviewBy;
}
