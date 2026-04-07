package com.web3lab.wallet.infrastructure.web3;

import lombok.Data;

/**
 * 提现交易回执查询结果。
 */
@Data
public class WithdrawTransactionReceiptResult {

    /**
     * 链上交易哈希。
     */
    private String txHash;

    /**
     * 当前是否已经拿到链上回执。
     */
    private boolean mined;

    /**
     * 当前链上执行是否成功。
     */
    private boolean success;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 创建待回执结果。
     *
     * @param txHash 链上交易哈希
     * @return 回执结果
     */
    public static WithdrawTransactionReceiptResult pending(String txHash) {
        WithdrawTransactionReceiptResult result = new WithdrawTransactionReceiptResult();
        result.setTxHash(txHash);
        result.setMined(false);
        result.setSuccess(false);
        return result;
    }

    /**
     * 创建成功回执结果。
     *
     * @param txHash 链上交易哈希
     * @return 回执结果
     */
    public static WithdrawTransactionReceiptResult success(String txHash) {
        WithdrawTransactionReceiptResult result = new WithdrawTransactionReceiptResult();
        result.setTxHash(txHash);
        result.setMined(true);
        result.setSuccess(true);
        return result;
    }

    /**
     * 创建失败回执结果。
     *
     * @param txHash 链上交易哈希
     * @param failReason 失败原因
     * @return 回执结果
     */
    public static WithdrawTransactionReceiptResult failed(String txHash, String failReason) {
        WithdrawTransactionReceiptResult result = new WithdrawTransactionReceiptResult();
        result.setTxHash(txHash);
        result.setMined(true);
        result.setSuccess(false);
        result.setFailReason(failReason);
        return result;
    }
}
