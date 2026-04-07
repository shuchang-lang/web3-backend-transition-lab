package com.web3lab.wallet.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 账户余额响应。
 */
@Data
public class AccountBalanceResponse {

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
     * 可用余额。
     */
    private BigDecimal availableBalance;

    /**
     * 冻结余额。
     */
    private BigDecimal frozenBalance;

    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;

    public AccountBalanceResponse() {
    }

    public AccountBalanceResponse(Long userId, String chain, String tokenSymbol, BigDecimal availableBalance,
                                  BigDecimal frozenBalance, LocalDateTime updatedAt) {
        this.userId = userId;
        this.chain = chain;
        this.tokenSymbol = tokenSymbol;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
        this.updatedAt = updatedAt;
    }
}
