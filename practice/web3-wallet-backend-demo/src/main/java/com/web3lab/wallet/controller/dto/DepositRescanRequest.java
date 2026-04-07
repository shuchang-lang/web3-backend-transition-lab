package com.web3lab.wallet.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动补扫充值区块区间请求。
 */
@Data
public class DepositRescanRequest {

    /**
     * 补扫起始区块。
     */
    @NotNull(message = "不能为空")
    @Min(value = 0L, message = "不能小于 0")
    private Long fromBlock;

    /**
     * 补扫结束区块。
     */
    @NotNull(message = "不能为空")
    @Min(value = 0L, message = "不能小于 0")
    private Long toBlock;
}
