package com.web3lab.wallet.application.account;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.AccountBalanceResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 账户余额应用服务，负责对外提供余额查询能力。
 */
@Service
public class AccountBalanceAppService {

    private final AccountBalanceMapper accountBalanceMapper;
    private final WalletDefaultsProperties walletDefaultsProperties;

    public AccountBalanceAppService(AccountBalanceMapper accountBalanceMapper,
                                    WalletDefaultsProperties walletDefaultsProperties) {
        this.accountBalanceMapper = accountBalanceMapper;
        this.walletDefaultsProperties = walletDefaultsProperties;
    }

    /**
     * 查询指定用户在目标链和币种下的余额快照。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @return 账户余额响应
     */
    public AccountBalanceResponse getBalance(Long userId, String chain, String tokenSymbol) {
        String resolvedChain = resolveOrDefault(chain, walletDefaultsProperties.chain());
        String resolvedTokenSymbol = resolveOrDefault(tokenSymbol, walletDefaultsProperties.tokenSymbol());

        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, userId)
                .eq(AccountBalance::getChain, resolvedChain)
                .eq(AccountBalance::getTokenSymbol, resolvedTokenSymbol)
                .last("LIMIT 1"));

        if (accountBalance == null) {
            return new AccountBalanceResponse(
                    userId,
                    resolvedChain,
                    resolvedTokenSymbol,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null
            );
        }

        return new AccountBalanceResponse(
                accountBalance.getUserId(),
                accountBalance.getChain(),
                accountBalance.getTokenSymbol(),
                accountBalance.getAvailableBalance(),
                accountBalance.getFrozenBalance(),
                accountBalance.getUpdatedAt()
        );
    }

    /**
     * 在调用方未显式传值时回落到系统默认配置。
     *
     * @param value 原始输入值
     * @param defaultValue 默认值
     * @return 解析后的最终值
     */
    private String resolveOrDefault(String value, String defaultValue) {
        return Optional.ofNullable(value)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(defaultValue);
    }
}
