package com.web3lab.wallet.infrastructure.web3;

import lombok.Data;

/**
 * 提现链上广播结果。
 */
@Data
public class WithdrawBroadcastResult {

    /**
     * 链上交易哈希。
     */
    private String txHash;

    /**
     * 本次广播使用的链上 nonce。
     */
    private Long nonce;

    /**
     * 创建广播结果。
     *
     * @param txHash 链上交易哈希
     * @param nonce 本次广播使用的链上 nonce
     * @return 广播结果
     */
    public static WithdrawBroadcastResult create(String txHash, Long nonce) {
        WithdrawBroadcastResult result = new WithdrawBroadcastResult();
        result.setTxHash(txHash);
        result.setNonce(nonce);
        return result;
    }
}
