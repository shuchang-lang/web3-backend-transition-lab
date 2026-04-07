package com.web3lab.wallet.controller.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 充值地址响应。
 */
@Data
public class WalletAddressResponse {

    /**
     * 地址记录主键。
     */
    private Long id;

    /**
     * 平台用户 ID。
     */
    private Long userId;

    /**
     * 链编码。
     */
    private String chain;

    /**
     * 币种符号。
     */
    private String tokenSymbol;

    /**
     * 平台分配的充值地址。
     */
    private String address;

    /**
     * 地址状态。
     */
    private String status;

    /**
     * 地址创建时间。
     */
    private LocalDateTime createdAt;

    public WalletAddressResponse() {
    }

    public WalletAddressResponse(Long id, Long userId, String chain, String tokenSymbol, String address, String status,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.chain = chain;
        this.tokenSymbol = tokenSymbol;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
    }
}
