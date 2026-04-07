package com.web3lab.wallet.infrastructure.address;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Demo 地址生成器。
 *
 * <p>当前阶段仅用于生成看起来像 EVM 地址的占位值，后续接入真实钱包服务后由真实地址分配逻辑替换。</p>
 */
@Component
public class DemoAddressGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成一个 20 字节的十六进制地址字符串。
     *
     * @return EVM 风格地址
     */
    public String nextAddress() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return "0x" + HexFormat.of().formatHex(bytes);
    }
}
