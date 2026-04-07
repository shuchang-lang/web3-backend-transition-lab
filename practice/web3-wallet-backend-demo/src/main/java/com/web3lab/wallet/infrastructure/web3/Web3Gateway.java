package com.web3lab.wallet.infrastructure.web3;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web3 网关抽象。
 *
 * <p>后续所有链上读写、日志查询、回执跟踪等能力都应该通过这个边界向下接入，
 * 避免业务层直接依赖具体 SDK。</p>
 */
public interface Web3Gateway {

    /**
     * 返回当前链网关实现名称。
     *
     * @return 网关名称
     */
    String clientName();

    /**
     * 当前网关是否具备 ERC-20 Transfer 日志扫描能力。
     *
     * @return true 表示已经接入真实链节点
     */
    boolean supportsTransferScan();

    /**
     * 查询节点当前最新区块号。
     *
     * @return 最新区块号
     */
    Long getLatestBlockNumber();

    /**
     * 按区块范围读取指定 ERC-20 合约的 Transfer 日志。
     *
     * @param contractAddress 代币合约地址
     * @param fromBlock 起始区块
     * @param toBlock 结束区块
     * @return 标准化后的 Transfer 日志列表
     */
    List<Erc20TransferLog> getErc20TransferLogs(String contractAddress, Long fromBlock, Long toBlock);

    /**
     * 当前网关是否具备充值日志链上核验能力。
     *
     * @return true 表示可以检查历史充值日志是否仍然存在
     */
    boolean supportsDepositReorgCheck();

    /**
     * 核验历史充值记录对应的链上日志是否仍然有效。
     *
     * <p>Day22 的最小版本主要用于识别“交易回执不存在 / 所在区块变化 / 指定日志缺失”
     * 这几类典型 reorg 风险场景。</p>
     *
     * @param contractAddress 代币合约地址
     * @param txHash 链上交易哈希
     * @param logIndex 日志索引
     * @param expectedBlockNumber 业务侧记录的原始区块号
     * @return 核验结果
     */
    DepositLogChainCheckResult inspectDepositTransferLog(String contractAddress, String txHash, Integer logIndex,
                                                         Long expectedBlockNumber);

    /**
     * 当前网关是否具备提现链上广播能力。
     *
     * @return true 表示已经具备真实广播条件
     */
    boolean supportsWithdrawBroadcast();

    /**
     * 查询当前提现热钱包在节点侧建议使用的下一个 nonce。
     *
     * @return 建议的下一个 nonce
     */
    Long getSuggestedWithdrawNonce();

    /**
     * 广播一笔 ERC-20 提现交易。
     *
     * @param contractAddress 代币合约地址
     * @param toAddress 提现目标地址
     * @param amount 提现金额
     * @param tokenDecimals 代币小数位
     * @param nonce 本次广播使用的链上 nonce
     * @return 广播结果
     */
    WithdrawBroadcastResult broadcastErc20Withdraw(String contractAddress, String toAddress,
                                                   BigDecimal amount, int tokenDecimals, Long nonce);

    /**
     * 查询提现交易回执。
     *
     * @param txHash 链上交易哈希
     * @return 回执查询结果
     */
    WithdrawTransactionReceiptResult getWithdrawTransactionReceipt(String txHash);
}
