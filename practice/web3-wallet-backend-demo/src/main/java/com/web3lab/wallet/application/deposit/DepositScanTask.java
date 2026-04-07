package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.application.scan.ChainScanProgressAppService;
import com.web3lab.wallet.application.task.TaskAuditLogAppService;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.ChainScanProgressResponse;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 充值扫描任务入口。
 *
 * <p>当前版本开始真正串起“扫描区块 -> 读取 ERC-20 Transfer 日志 -> 识别候选充值 -> 更新扫描断点”
 * -> 推进确认数 -> 正式入账”这条最小主链路。</p>
 */
@Component
public class DepositScanTask {

    private static final Logger log = LoggerFactory.getLogger(DepositScanTask.class);

    private final ChainScanProgressAppService chainScanProgressAppService;
    private final Web3Gateway web3Gateway;
    private final DepositCandidateAppService depositCandidateAppService;
    private final DepositSettlementAppService depositSettlementAppService;
    private final TaskAuditLogAppService taskAuditLogAppService;
    private final WalletDefaultsProperties walletDefaultsProperties;
    private final WalletWeb3Properties walletWeb3Properties;

    public DepositScanTask(ChainScanProgressAppService chainScanProgressAppService,
                           Web3Gateway web3Gateway,
                           DepositCandidateAppService depositCandidateAppService,
                           DepositSettlementAppService depositSettlementAppService,
                           TaskAuditLogAppService taskAuditLogAppService,
                           WalletDefaultsProperties walletDefaultsProperties,
                           WalletWeb3Properties walletWeb3Properties) {
        this.chainScanProgressAppService = chainScanProgressAppService;
        this.web3Gateway = web3Gateway;
        this.depositCandidateAppService = depositCandidateAppService;
        this.depositSettlementAppService = depositSettlementAppService;
        this.taskAuditLogAppService = taskAuditLogAppService;
        this.walletDefaultsProperties = walletDefaultsProperties;
        this.walletWeb3Properties = walletWeb3Properties;
    }

    /**
     * 执行一次最小充值扫描任务。
     *
     * <p>如果本地尚未配置 RPC 地址或充值代币合约地址，本方法只会初始化扫描进度而不会真的访问链上，
     * 这样仓库在未接入真实节点时也可以正常启动和演示。</p>
     *
     * @return 当前扫描进度
     */
    public ChainScanProgressResponse runOnce() {
        String taskName = ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK;
        String taskBatchNo = taskAuditLogAppService.nextBatchNo(taskName);
        try {
            ChainScanProgressResponse progress = chainScanProgressAppService.getOrInitProgress(null, taskName);
            if (!walletWeb3Properties.readyForDepositScan() || !web3Gateway.supportsTransferScan()) {
                log.info("跳过 ERC-20 充值扫描，当前未配置可用的 RPC 或代币合约地址，gateway={}", web3Gateway.clientName());
                taskAuditLogAppService.recordSkipped(
                        taskName,
                        taskBatchNo,
                        "当前未配置可用的 RPC 或代币合约地址",
                        "已跳过 ERC-20 充值扫描"
                );
                return progress;
            }

            long fromBlock = resolveFromBlock(progress.getLastScannedBlock());
            long latestBlock = web3Gateway.getLatestBlockNumber();
            if (latestBlock < fromBlock) {
                log.info("当前暂无可扫描区块，fromBlock={}, latestBlock={}", fromBlock, latestBlock);
                taskAuditLogAppService.recordSkipped(
                        taskName,
                        taskBatchNo,
                        "当前暂无可扫描区块",
                        "本轮充值扫描没有新增区块窗口"
                );
                return progress;
            }

            long toBlock = Math.min(latestBlock, fromBlock + walletWeb3Properties.resolvedScanStep() - 1L);
            int detectedCount = depositCandidateAppService.detectAndStore(
                    walletDefaultsProperties.chain(),
                    walletDefaultsProperties.tokenSymbol(),
                    walletWeb3Properties.resolvedTokenDecimals(),
                    web3Gateway.getErc20TransferLogs(walletWeb3Properties.depositTokenContract(), fromBlock, toBlock)
            );
            int creditedCount = depositSettlementAppService.refreshConfirmationsAndCredit(
                    walletDefaultsProperties.chain(),
                    walletDefaultsProperties.tokenSymbol(),
                    latestBlock,
                    walletWeb3Properties.resolvedConfirmationsThreshold()
            );
            ChainScanProgressResponse updatedProgress = chainScanProgressAppService.updateProgress(
                    walletDefaultsProperties.chain(),
                    taskName,
                    toBlock
            );
            log.info("完成一轮 ERC-20 充值扫描，chain={}, fromBlock={}, toBlock={}, detectedCount={}, creditedCount={}",
                    walletDefaultsProperties.chain(), fromBlock, toBlock, detectedCount, creditedCount);
            taskAuditLogAppService.recordSuccess(
                    taskName,
                    taskBatchNo,
                    detectedCount,
                    creditedCount,
                    0,
                    0,
                    buildMetricSnapshot(fromBlock, toBlock, latestBlock, detectedCount, creditedCount),
                    "已完成一轮 ERC-20 充值扫描"
            );
            return updatedProgress;
        } catch (RuntimeException ex) {
            taskAuditLogAppService.recordFailure(
                    taskName,
                    taskBatchNo,
                    0,
                    0,
                    0,
                    1,
                    "",
                    resolveFailureReason(ex),
                    "ERC-20 充值扫描执行失败"
            );
            throw ex;
        }
    }

    /**
     * 计算本轮扫描的起始区块。
     *
     * <p>首次扫描时从配置中的起始区块开始，后续扫描则从“上次扫描完成区块 + 1”继续推进，
     * 这是最小版断点续跑的核心约束。</p>
     *
     * @param lastScannedBlock 上一次扫描完成的区块
     * @return 本轮起始区块
     */
    private long resolveFromBlock(Long lastScannedBlock) {
        if (lastScannedBlock == null || lastScannedBlock <= 0L) {
            return walletWeb3Properties.resolvedScanStartBlock();
        }
        return lastScannedBlock + 1L;
    }

    /**
     * 构建充值扫描指标快照。
     *
     * @param fromBlock 本轮起始区块
     * @param toBlock 本轮结束区块
     * @param latestBlock 当前最新区块
     * @param detectedCount 识别到的候选充值数量
     * @param creditedCount 正式入账数量
     * @return 指标快照文本
     */
    private String buildMetricSnapshot(long fromBlock, long toBlock, long latestBlock,
                                       int detectedCount, int creditedCount) {
        return "fromBlock=" + fromBlock
                + ",toBlock=" + toBlock
                + ",latestBlock=" + latestBlock
                + ",detectedCount=" + detectedCount
                + ",creditedCount=" + creditedCount;
    }

    /**
     * 解析任务失败原因。
     *
     * @param ex 异常对象
     * @return 失败原因
     */
    private String resolveFailureReason(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
