package com.web3lab.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wallet.defaults")
public record WalletDefaultsProperties(String chain, String tokenSymbol) {
}
