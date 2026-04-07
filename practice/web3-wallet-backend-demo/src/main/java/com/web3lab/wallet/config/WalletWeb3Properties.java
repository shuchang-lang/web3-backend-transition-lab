package com.web3lab.wallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 钱包后端链交互配置。
 *
 * <p>这里集中维护最小版充值扫描所需的链节点地址、代币合约地址和扫描窗口参数，
 * 避免业务代码中散落魔法值。</p>
 */
@ConfigurationProperties(prefix = "wallet.web3")
public record WalletWeb3Properties(String rpcUrl,
                                   String depositTokenContract,
                                   Long scanStartBlock,
                                   Integer scanStep,
                                   Integer tokenDecimals,
                                   Integer confirmationsThreshold,
                                   String withdrawHotWalletPrivateKey,
                                   Long withdrawChainId,
                                   Long withdrawGasLimit,
                                   Integer withdrawBroadcastMaxRetryCount,
                                   Long withdrawReceiptTimeoutMinutes,
                                   Integer withdrawReceiptMaxTimeoutRetryCount) {

    /**
     * 是否已经配置可用的 RPC 地址。
     *
     * @return true 表示可以初始化真实 web3j 网关
     */
    public boolean hasRpcUrl() {
        return StringUtils.hasText(rpcUrl);
    }

    /**
     * 当前环境是否满足最小充值扫描条件。
     *
     * @return true 表示同时具备 RPC 地址和代币合约地址
     */
    public boolean readyForDepositScan() {
        return hasRpcUrl() && StringUtils.hasText(depositTokenContract);
    }

    /**
     * 解析扫描起始区块，避免出现负数配置。
     *
     * @return 最终生效的起始区块
     */
    public long resolvedScanStartBlock() {
        return scanStartBlock == null ? 0L : Math.max(scanStartBlock, 0L);
    }

    /**
     * 解析单次扫描窗口大小，避免出现非法的 0 或负数。
     *
     * @return 最终生效的扫描窗口
     */
    public int resolvedScanStep() {
        return scanStep == null || scanStep <= 0 ? 200 : scanStep;
    }

    /**
     * 解析代币精度。
     *
     * @return 最终生效的小数位数
     */
    public int resolvedTokenDecimals() {
        return tokenDecimals == null || tokenDecimals < 0 ? 18 : tokenDecimals;
    }

    /**
     * 解析充值正式入账所需确认数。
     *
     * @return 最终生效的确认数阈值
     */
    public int resolvedConfirmationsThreshold() {
        return confirmationsThreshold == null || confirmationsThreshold <= 0 ? 6 : confirmationsThreshold;
    }

    /**
     * 是否已经配置提现广播私钥。
     *
     * @return true 表示可以发起真实链上广播
     */
    public boolean hasWithdrawHotWalletPrivateKey() {
        return StringUtils.hasText(withdrawHotWalletPrivateKey);
    }

    /**
     * 当前环境是否满足最小提现广播条件。
     *
     * @return true 表示同时具备 RPC、代币合约和热钱包私钥
     */
    public boolean readyForWithdrawBroadcast() {
        return hasRpcUrl() && StringUtils.hasText(depositTokenContract) && hasWithdrawHotWalletPrivateKey();
    }

    /**
     * 解析提现广播链 ID。
     *
     * @return 最终生效的链 ID
     */
    public long resolvedWithdrawChainId() {
        return withdrawChainId == null || withdrawChainId <= 0 ? 11155111L : withdrawChainId;
    }

    /**
     * 解析提现广播 gas limit。
     *
     * @return 最终生效的 gas limit
     */
    public long resolvedWithdrawGasLimit() {
        return withdrawGasLimit == null || withdrawGasLimit <= 0 ? 120000L : withdrawGasLimit;
    }

    /**
     * 解析提现广播失败后的最大自动重试次数。
     *
     * @return 最终生效的最大重试次数
     */
    public int resolvedWithdrawBroadcastMaxRetryCount() {
        return withdrawBroadcastMaxRetryCount == null || withdrawBroadcastMaxRetryCount < 0 ? 3
                : withdrawBroadcastMaxRetryCount;
    }

    /**
     * 解析提现回执超时巡检阈值分钟数。
     *
     * @return 最终生效的超时分钟数
     */
    public long resolvedWithdrawReceiptTimeoutMinutes() {
        return withdrawReceiptTimeoutMinutes == null || withdrawReceiptTimeoutMinutes <= 0 ? 10L
                : withdrawReceiptTimeoutMinutes;
    }

    /**
     * 解析提现回执超时后的最大重试次数。
     *
     * <p>这里的“重试次数”指的是：订单因为长时间拿不到回执，被巡检任务打回 `PENDING_BROADCAST`
     * 后允许再次广播的次数。超过这个上限后，订单会进入人工处理状态。</p>
     *
     * @return 最终生效的最大重试次数
     */
    public int resolvedWithdrawReceiptMaxTimeoutRetryCount() {
        return withdrawReceiptMaxTimeoutRetryCount == null || withdrawReceiptMaxTimeoutRetryCount < 0 ? 2
                : withdrawReceiptMaxTimeoutRetryCount;
    }
}
