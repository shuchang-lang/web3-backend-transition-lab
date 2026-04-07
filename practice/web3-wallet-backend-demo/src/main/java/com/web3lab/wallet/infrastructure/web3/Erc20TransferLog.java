package com.web3lab.wallet.infrastructure.web3;

import java.math.BigInteger;
import lombok.Data;

/**
 * ERC-20 Transfer 事件标准化结果。
 *
 * <p>这里屏蔽 web3j 原始日志对象细节，让应用层只关心充值识别所需的业务字段。</p>
 */
@Data
public class Erc20TransferLog {

    /**
     * 代币合约地址。
     */
    private String contractAddress;

    /**
     * 转出地址。
     */
    private String fromAddress;

    /**
     * 转入地址。
     */
    private String toAddress;

    /**
     * 原始最小单位金额。
     */
    private BigInteger rawAmount;

    /**
     * 交易哈希。
     */
    private String txHash;

    /**
     * 日志索引。
     */
    private Integer logIndex;

    /**
     * 所在区块号。
     */
    private Long blockNumber;

    /**
     * 创建标准化 Transfer 日志。
     *
     * @param contractAddress 代币合约地址
     * @param fromAddress 转出地址
     * @param toAddress 转入地址
     * @param rawAmount 原始最小单位金额
     * @param txHash 交易哈希
     * @param logIndex 日志索引
     * @param blockNumber 区块号
     * @return 标准化 Transfer 日志
     */
    public static Erc20TransferLog create(String contractAddress, String fromAddress, String toAddress,
                                          BigInteger rawAmount, String txHash, Integer logIndex, Long blockNumber) {
        Erc20TransferLog transferLog = new Erc20TransferLog();
        transferLog.setContractAddress(contractAddress);
        transferLog.setFromAddress(fromAddress);
        transferLog.setToAddress(toAddress);
        transferLog.setRawAmount(rawAmount);
        transferLog.setTxHash(txHash);
        transferLog.setLogIndex(logIndex);
        transferLog.setBlockNumber(blockNumber);
        return transferLog;
    }
}
