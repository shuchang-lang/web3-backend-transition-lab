package com.web3lab.wallet.application.address;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.common.exception.ResourceNotFoundException;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.CreateWalletAddressRequest;
import com.web3lab.wallet.controller.dto.WalletAddressResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.address.WalletAddress;
import com.web3lab.wallet.infrastructure.address.DemoAddressGenerator;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.WalletAddressMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 充值地址应用服务，负责地址分配和地址查询。
 */
@Service
public class WalletAddressAppService {

    private final WalletAddressMapper walletAddressMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final DemoAddressGenerator demoAddressGenerator;
    private final WalletDefaultsProperties walletDefaultsProperties;

    public WalletAddressAppService(WalletAddressMapper walletAddressMapper,
                                   AccountBalanceMapper accountBalanceMapper,
                                   DemoAddressGenerator demoAddressGenerator,
                                   WalletDefaultsProperties walletDefaultsProperties) {
        this.walletAddressMapper = walletAddressMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.demoAddressGenerator = demoAddressGenerator;
        this.walletDefaultsProperties = walletDefaultsProperties;
    }

    /**
     * 分配充值地址，并在首次分配时同步初始化余额主表。
     *
     * @param request 创建充值地址请求
     * @return 充值地址响应
     */
    @Transactional
    public WalletAddressResponse allocateAddress(CreateWalletAddressRequest request) {
        String chain = resolveOrDefault(request.getChain(), walletDefaultsProperties.chain());
        String tokenSymbol = resolveOrDefault(request.getTokenSymbol(), walletDefaultsProperties.tokenSymbol());

        WalletAddress existing = findAddress(request.getUserId(), chain, tokenSymbol);
        if (existing != null) {
            ensureBalanceExists(request.getUserId(), chain, tokenSymbol);
            return toResponse(existing);
        }

        WalletAddress walletAddress = WalletAddress.create(
                request.getUserId(),
                chain,
                tokenSymbol,
                demoAddressGenerator.nextAddress(),
                "ACTIVE"
        );
        walletAddressMapper.insert(walletAddress);
        ensureBalanceExists(request.getUserId(), chain, tokenSymbol);
        return toResponse(walletAddress);
    }

    /**
     * 查询指定用户当前资产对应的充值地址。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @return 充值地址响应
     */
    public WalletAddressResponse getAddress(Long userId, String chain, String tokenSymbol) {
        String resolvedChain = resolveOrDefault(chain, walletDefaultsProperties.chain());
        String resolvedTokenSymbol = resolveOrDefault(tokenSymbol, walletDefaultsProperties.tokenSymbol());

        WalletAddress walletAddress = findAddress(userId, resolvedChain, resolvedTokenSymbol);
        if (walletAddress == null) {
            throw new ResourceNotFoundException("未找到对应的充值地址，请先创建地址");
        }
        return toResponse(walletAddress);
    }

    /**
     * 确保余额主表存在。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     */
    private void ensureBalanceExists(Long userId, String chain, String tokenSymbol) {
        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, userId)
                .eq(AccountBalance::getChain, chain)
                .eq(AccountBalance::getTokenSymbol, tokenSymbol));
        if (accountBalance == null) {
            accountBalanceMapper.insert(AccountBalance.createZero(userId, chain, tokenSymbol));
        }
    }

    /**
     * 按用户和资产维度查询充值地址。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @return 充值地址实体，不存在时返回 null
     */
    private WalletAddress findAddress(Long userId, String chain, String tokenSymbol) {
        return walletAddressMapper.selectOne(Wrappers.<WalletAddress>lambdaQuery()
                .eq(WalletAddress::getUserId, userId)
                .eq(WalletAddress::getChain, chain)
                .eq(WalletAddress::getTokenSymbol, tokenSymbol)
                .last("LIMIT 1"));
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

    /**
     * 将实体对象转换为对外响应。
     *
     * @param walletAddress 充值地址实体
     * @return 充值地址响应
     */
    private WalletAddressResponse toResponse(WalletAddress walletAddress) {
        return new WalletAddressResponse(
                walletAddress.getId(),
                walletAddress.getUserId(),
                walletAddress.getChain(),
                walletAddress.getTokenSymbol(),
                walletAddress.getAddress(),
                walletAddress.getStatus(),
                walletAddress.getCreatedAt()
        );
    }
}
