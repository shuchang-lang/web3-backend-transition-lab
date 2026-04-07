package com.web3lab.wallet.infrastructure.web3;

import com.web3lab.wallet.config.WalletWeb3Properties;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

/**
 * 基于 web3j 的 Web3 网关实现。
 *
 * <p>当前覆盖两类最小能力：
 * 1. 读取 ERC-20 Transfer 日志，服务充值监听
 * 2. 广播 ERC-20 提现交易并跟踪回执，服务最小提现执行链路</p>
 */
public class Web3jWeb3Gateway implements Web3Gateway {

    /**
     * ERC-20 Transfer(address,address,uint256) 事件主题哈希。
     *
     * <p>充值监听第一版只消费这类日志，因为平台是否收到代币到账，本质上就是识别打给平台地址的 Transfer 事件。</p>
     */
    private static final String ERC20_TRANSFER_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private final WalletWeb3Properties walletWeb3Properties;
    private final Web3j web3j;
    private final Credentials withdrawCredentials;

    public Web3jWeb3Gateway(WalletWeb3Properties walletWeb3Properties) {
        this.walletWeb3Properties = walletWeb3Properties;
        this.web3j = Web3j.build(new HttpService(walletWeb3Properties.rpcUrl()));
        this.withdrawCredentials = walletWeb3Properties.hasWithdrawHotWalletPrivateKey()
                ? Credentials.create(walletWeb3Properties.withdrawHotWalletPrivateKey())
                : null;
    }

    /**
     * 关闭 web3j 持有的 HTTP 连接资源。
     */
    @PreDestroy
    public void shutdown() {
        web3j.shutdown();
    }

    /**
     * 返回当前网关标识。
     *
     * @return 网关名称
     */
    @Override
    public String clientName() {
        return "web3j-http-gateway";
    }

    /**
     * web3j 网关具备真实日志扫描能力。
     *
     * @return true
     */
    @Override
    public boolean supportsTransferScan() {
        return true;
    }

    /**
     * 调用节点获取最新区块号。
     *
     * @return 最新区块号
     */
    @Override
    public Long getLatestBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber().longValue();
        } catch (IOException exception) {
            throw new IllegalStateException("调用节点查询最新区块失败", exception);
        }
    }

    /**
     * 按区块窗口读取指定代币合约的 Transfer 日志。
     *
     * @param contractAddress 代币合约地址
     * @param fromBlock 起始区块
     * @param toBlock 结束区块
     * @return 标准化后的 Transfer 日志列表
     */
    @Override
    public List<Erc20TransferLog> getErc20TransferLogs(String contractAddress, Long fromBlock, Long toBlock) {
        try {
            EthFilter ethFilter = new EthFilter(
                    new DefaultBlockParameterNumber(BigInteger.valueOf(fromBlock)),
                    new DefaultBlockParameterNumber(BigInteger.valueOf(toBlock)),
                    normalizeAddress(contractAddress)
            );
            ethFilter.addSingleTopic(ERC20_TRANSFER_TOPIC);
            EthLog ethLog = web3j.ethGetLogs(ethFilter).send();
            return ethLog.getLogs().stream()
                    .map(logResult -> (Log) logResult.get())
                    .map(this::toTransferLog)
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            throw new IllegalStateException("调用节点查询 ERC-20 Transfer 日志失败", exception);
        }
    }

    /**
     * 当前 web3j 网关具备充值日志链上核验能力。
     *
     * @return true
     */
    @Override
    public boolean supportsDepositReorgCheck() {
        return true;
    }

    /**
     * 核验历史充值日志是否仍然存在。
     *
     * <p>Day22 第一版先做最小检查：
     * 1. 交易回执是否还存在
     * 2. 回执区块号是否和业务侧记录一致
     * 3. 回执日志里是否还能找到指定 `logIndex` 和合约地址</p>
     *
     * @param contractAddress 代币合约地址
     * @param txHash 链上交易哈希
     * @param logIndex 日志索引
     * @param expectedBlockNumber 业务侧记录的原始区块号
     * @return 核验结果
     */
    @Override
    public DepositLogChainCheckResult inspectDepositTransferLog(String contractAddress, String txHash, Integer logIndex,
                                                                Long expectedBlockNumber) {
        try {
            EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(txHash).send();
            Optional<TransactionReceipt> optionalReceipt = ethGetTransactionReceipt.getTransactionReceipt();
            if (optionalReceipt.isEmpty()) {
                return DepositLogChainCheckResult.reorgSuspected("链上已查不到该充值交易回执");
            }
            TransactionReceipt receipt = optionalReceipt.get();
            Long actualBlockNumber = receipt.getBlockNumber() == null ? null : receipt.getBlockNumber().longValue();
            if (expectedBlockNumber != null && !expectedBlockNumber.equals(actualBlockNumber)) {
                return DepositLogChainCheckResult.reorgSuspected("充值交易所在区块已变化，疑似发生链重组");
            }
            boolean matched = receipt.getLogs().stream().anyMatch(log ->
                    matchesDepositTransferLog(log, contractAddress, logIndex)
            );
            if (!matched) {
                return DepositLogChainCheckResult.reorgSuspected("交易回执中已找不到原始充值日志");
            }
            return DepositLogChainCheckResult.present();
        } catch (IOException exception) {
            throw new IllegalStateException("调用节点核验充值日志失败", exception);
        }
    }

    /**
     * 当前网关是否具备提现广播能力。
     *
     * @return true 表示已经具备热钱包私钥
     */
    @Override
    public boolean supportsWithdrawBroadcast() {
        return withdrawCredentials != null && StringUtils.hasText(walletWeb3Properties.depositTokenContract());
    }

    /**
     * 查询节点建议使用的下一个提现 nonce。
     *
     * <p>这里读取的是 pending 视角下的交易数，能够覆盖“本节点已接受但尚未上链”的交易。</p>
     *
     * @return 建议的下一个 nonce
     */
    @Override
    public Long getSuggestedWithdrawNonce() {
        if (!supportsWithdrawBroadcast()) {
            return 0L;
        }
        try {
            return web3j.ethGetTransactionCount(
                    withdrawCredentials.getAddress(),
                    DefaultBlockParameterName.PENDING
            ).send().getTransactionCount().longValue();
        } catch (IOException exception) {
            throw new IllegalStateException("调用节点查询提现 nonce 失败", exception);
        }
    }

    /**
     * 广播一笔 ERC-20 提现交易。
     *
     * <p>当前最小版本直接读取节点 nonce 和 gasPrice，构造 ERC-20 transfer 方法调用并签名广播。
     * 真实生产系统通常还会进一步补 EIP-1559 费用策略、nonce 锁和签名服务边界。</p>
     *
     * @param contractAddress 代币合约地址
     * @param toAddress 提现目标地址
     * @param amount 提现金额
     * @param tokenDecimals 代币小数位
     * @param nonce 本次广播使用的链上 nonce
     * @return 广播结果
     */
    @Override
    public WithdrawBroadcastResult broadcastErc20Withdraw(String contractAddress, String toAddress,
                                                          BigDecimal amount, int tokenDecimals, Long nonce) {
        if (!supportsWithdrawBroadcast()) {
            throw new IllegalStateException("当前环境未配置提现广播私钥");
        }
        try {
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger tokenAmount = amount.movePointRight(tokenDecimals).toBigIntegerExact();
            String data = FunctionEncoder.encode(new Function(
                    "transfer",
                    List.<Type>of(new Address(normalizeAddress(toAddress)), new Uint256(tokenAmount)),
                    Collections.emptyList()
            ));
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    BigInteger.valueOf(nonce),
                    gasPrice,
                    BigInteger.valueOf(walletWeb3Properties.resolvedWithdrawGasLimit()),
                    normalizeAddress(contractAddress),
                    BigInteger.ZERO,
                    data
            );
            byte[] signedMessage = TransactionEncoder.signMessage(
                    rawTransaction,
                    walletWeb3Properties.resolvedWithdrawChainId(),
                    withdrawCredentials
            );
            EthSendTransaction sendTransaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedMessage)).send();
            if (sendTransaction.hasError()) {
                throw new IllegalStateException("节点拒绝广播：" + sendTransaction.getError().getMessage());
            }
            return WithdrawBroadcastResult.create(sendTransaction.getTransactionHash(), nonce);
        } catch (IOException exception) {
            throw new IllegalStateException("调用节点广播提现交易失败", exception);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("提现金额与代币精度不匹配，无法编码为链上最小单位", exception);
        }
    }

    /**
     * 查询提现交易当前回执状态。
     *
     * @param txHash 链上交易哈希
     * @return 回执查询结果
     */
    @Override
    public WithdrawTransactionReceiptResult getWithdrawTransactionReceipt(String txHash) {
        try {
            EthGetTransactionReceipt ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(txHash).send();
            Optional<TransactionReceipt> optionalReceipt = ethGetTransactionReceipt.getTransactionReceipt();
            if (optionalReceipt.isEmpty()) {
                return WithdrawTransactionReceiptResult.pending(txHash);
            }
            TransactionReceipt receipt = optionalReceipt.get();
            if (receipt.isStatusOK()) {
                return WithdrawTransactionReceiptResult.success(txHash);
            }
            return WithdrawTransactionReceiptResult.failed(txHash, "链上交易执行失败");
        } catch (IOException exception) {
            throw new IllegalStateException("调用节点查询提现交易回执失败", exception);
        }
    }

    /**
     * 将 web3j 原始日志转换为业务层可消费的 Transfer 日志对象。
     *
     * @param log web3j 原始日志
     * @return 标准化 Transfer 日志
     */
    private Erc20TransferLog toTransferLog(Log log) {
        List<String> topics = log.getTopics();
        if (topics == null || topics.size() < 3) {
            throw new IllegalStateException("Transfer 日志主题数量不足，无法解析地址字段");
        }
        return Erc20TransferLog.create(
                normalizeAddress(log.getAddress()),
                decodeIndexedAddress(topics.get(1)),
                decodeIndexedAddress(topics.get(2)),
                Numeric.toBigInt(log.getData()),
                log.getTransactionHash(),
                log.getLogIndex() == null ? 0 : log.getLogIndex().intValue(),
                log.getBlockNumber() == null ? 0L : log.getBlockNumber().longValue()
        );
    }

    /**
     * 判断某条回执日志是否仍然对应业务侧记录的充值事件。
     *
     * @param log 回执日志
     * @param contractAddress 代币合约地址
     * @param logIndex 业务侧记录的日志索引
     * @return true 表示仍然命中原始充值日志
     */
    private boolean matchesDepositTransferLog(Log log, String contractAddress, Integer logIndex) {
        if (log == null) {
            return false;
        }
        boolean sameContract = normalizeAddress(contractAddress).equals(normalizeAddress(log.getAddress()));
        boolean sameLogIndex = log.getLogIndex() != null && logIndex != null && logIndex.intValue() == log.getLogIndex().intValue();
        return sameContract && sameLogIndex;
    }

    /**
     * 解析 indexed address topic。
     *
     * <p>ERC-20 Transfer 事件的 from 和 to 都放在 topic 中，且按 32 字节左填充，
     * 因此这里只取末尾 20 字节作为真实地址。</p>
     *
     * @param topicHex topic 十六进制字符串
     * @return 规范化后的 EVM 地址
     */
    private String decodeIndexedAddress(String topicHex) {
        String cleanHex = Numeric.cleanHexPrefix(topicHex);
        String addressHex = cleanHex.substring(cleanHex.length() - 40);
        return normalizeAddress(addressHex);
    }

    /**
     * 统一地址大小写与 0x 前缀。
     *
     * @param address 原始地址
     * @return 标准化地址
     */
    private String normalizeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return address;
        }
        return Numeric.prependHexPrefix(Numeric.cleanHexPrefix(address)).toLowerCase(Locale.ROOT);
    }
}
