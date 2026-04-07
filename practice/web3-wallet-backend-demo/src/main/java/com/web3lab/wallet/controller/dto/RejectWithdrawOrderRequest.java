package com.web3lab.wallet.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核拒绝提现订单请求。
 */
@Data
public class RejectWithdrawOrderRequest {

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

    /**
     * 拒绝原因。
     */
    @NotBlank(message = "不能为空")
    private String rejectReason;
}
