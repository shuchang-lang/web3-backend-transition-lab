package com.web3lab.wallet.config;

import com.web3lab.wallet.infrastructure.web3.NoopWeb3Gateway;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import com.web3lab.wallet.infrastructure.web3.Web3jWeb3Gateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web3 网关注入配置。
 *
 * <p>仓库默认允许“无真实链节点”启动，因此这里根据配置动态装配真实网关或空实现，
 * 方便本地演示、单元测试和后续逐步接入 RPC。</p>
 */
@Configuration
public class Web3GatewayConfiguration {

    /**
     * 创建 Web3 网关 Bean。
     *
     * @param walletWeb3Properties 链交互配置
     * @return 真实 web3j 网关或占位空网关
     */
    @Bean
    public Web3Gateway web3Gateway(WalletWeb3Properties walletWeb3Properties) {
        if (walletWeb3Properties.hasRpcUrl()) {
            return new Web3jWeb3Gateway(walletWeb3Properties);
        }
        return new NoopWeb3Gateway();
    }
}
