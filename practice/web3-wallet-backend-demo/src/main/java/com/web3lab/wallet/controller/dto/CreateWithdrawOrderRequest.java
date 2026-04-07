package com.web3lab.wallet.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 创建提现订单请求。
 */
@Data
public class CreateWithdrawOrderRequest {

    /**
     * 平台用户 ID。
     */
    @NotNull(message = "不能为空")
    private Long userId;

    /**
     * 提现目标地址。
     */
    @NotBlank(message = "不能为空")
    private String toAddress;

    /**
     * 提现金额。
     */
    @NotNull(message = "不能为空")
    @DecimalMin(value = "0.000000000000000001", message = "必须大于 0")
    private BigDecimal amount;

    /**
     * 手续费金额。
     */
    @NotNull(message = "不能为空")
    @DecimalMin(value = "0", message = "不能小于 0")
    private BigDecimal fee;

    /**
     * 客户端请求号。
     */
    @NotBlank(message = "不能为空")
    private String requestNo;

    /**
     * 链编码，为空时使用系统默认链。
     */
    private String chain;

    /**
     * 币种符号，为空时使用系统默认币种。
     */
    private String tokenSymbol;
}
