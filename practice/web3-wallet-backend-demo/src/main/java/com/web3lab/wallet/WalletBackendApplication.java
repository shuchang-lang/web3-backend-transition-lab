package com.web3lab.wallet;

import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.config.WalletWeb3Properties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.web3lab.wallet.infrastructure.persistence")
@EnableConfigurationProperties({WalletDefaultsProperties.class, WalletWeb3Properties.class})
public class WalletBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletBackendApplication.class, args);
    }
}
