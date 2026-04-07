package com.web3lab.wallet.infrastructure.web3;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Web3 网关空实现。
 *
 * <p>在没有配置真实 RPC 节点时，应用会退化到这个实现，
 * 让仓库仍然可以完成地址管理、余额查询和文档演示。</p>
 */
public class NoopWeb3Gateway implements Web3Gateway {

    /**
     * 返回当前占位网关名称，便于后续启动和排查时确认链交互层是否已经装配。
     *
     * @return 占位网关名称
     */
    @Override
    public String clientName() {
        return "day15-noop-web3-gateway";
    }

    /**
     * 占位实现不具备真实链扫描能力。
     *
     * @return false
     */
    @Override
    public boolean supportsTransferScan() {
        return false;
    }

    /**
     * 占位实现没有真实链节点，因此默认返回 0。
     *
     * @return 默认区块号 0
     */
    @Override
    public Long getLatestBlockNumber() {
        return 0L;
    }

    /**
     * 占位实现不返回任何链上日志。
     *
     * @param contractAddress 代币合约地址
     * @param fromBlock 起始区块
     * @param toBlock 结束区块
     * @return 空列表
     */
    @Override
    public List<Erc20TransferLog> getErc20TransferLogs(String contractAddress, Long fromBlock, Long toBlock) {
        return Collections.emptyList();
    }

    /**
     * 占位实现不具备充值日志链上核验能力。
     *
     * @return false
     */
    @Override
    public boolean supportsDepositReorgCheck() {
        return false;
    }

    /**
     * 占位实现无法核验历史充值日志。
     *
     * @param contractAddress 代币合约地址
     * @param txHash 链上交易哈希
     * @param logIndex 日志索引
     * @param expectedBlockNumber 业务侧记录的区块号
     * @return 不支持结果
     */
    @Override
    public DepositLogChainCheckResult inspectDepositTransferLog(String contractAddress, String txHash,
                                                                Integer logIndex, Long expectedBlockNumber) {
        return DepositLogChainCheckResult.unsupported("当前未配置真实充值日志核验能力");
    }

    /**
     * 占位实现不具备真实提现广播能力。
     *
     * @return false
     */
    @Override
    public boolean supportsWithdrawBroadcast() {
        return false;
    }

    /**
     * 占位实现没有真实提现热钱包，因此默认返回 0。
     *
     * @return 默认 nonce 0
     */
    @Override
    public Long getSuggestedWithdrawNonce() {
        return 0L;
    }

    /**
     * 占位实现不会真的广播链上交易。
     *
     * @param contractAddress 代币合约地址
     * @param toAddress 提现目标地址
     * @param amount 提现金额
     * @param tokenDecimals 代币小数位
     * @param nonce 本次广播使用的链上 nonce
     * @return 不会返回真实结果
     */
    @Override
    public WithdrawBroadcastResult broadcastErc20Withdraw(String contractAddress, String toAddress,
                                                          BigDecimal amount, int tokenDecimals, Long nonce) {
        throw new IllegalStateException("当前未配置真实提现广播能力");
    }

    /**
     * 占位实现没有真实链节点，因此默认返回待回执状态。
     *
     * @param txHash 链上交易哈希
     * @return 待回执结果
     */
    @Override
    public WithdrawTransactionReceiptResult getWithdrawTransactionReceipt(String txHash) {
        return WithdrawTransactionReceiptResult.pending(txHash);
    }
}
