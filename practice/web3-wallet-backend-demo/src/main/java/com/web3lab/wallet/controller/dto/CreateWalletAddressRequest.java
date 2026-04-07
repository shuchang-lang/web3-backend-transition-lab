package com.web3lab.wallet.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建充值地址请求。
 */
@Data
public class CreateWalletAddressRequest {

    /**
     * 平台用户 ID。
     */
    @NotNull(message = "不能为空")
    private Long userId;

    /**
     * 链编码，为空时使用系统默认链。
     */
    private String chain;

    /**
     * 币种符号，为空时使用系统默认币种。
     */
    private String tokenSymbol;

    public CreateWalletAddressRequest() {
    }

    public CreateWalletAddressRequest(Long userId, String chain, String tokenSymbol) {
        this.userId = userId;
        this.chain = chain;
        this.tokenSymbol = tokenSymbol;
    }
}
