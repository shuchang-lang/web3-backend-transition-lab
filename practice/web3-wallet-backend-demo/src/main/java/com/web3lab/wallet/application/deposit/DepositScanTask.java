package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.application.scan.ChainScanProgressAppService;
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
    private final WalletDefaultsProperties walletDefaultsProperties;
    private final WalletWeb3Properties walletWeb3Properties;

    public DepositScanTask(ChainScanProgressAppService chainScanProgressAppService,
                           Web3Gateway web3Gateway,
                           DepositCandidateAppService depositCandidateAppService,
                           DepositSettlementAppService depositSettlementAppService,
                           WalletDefaultsProperties walletDefaultsProperties,
                           WalletWeb3Properties walletWeb3Properties) {
        this.chainScanProgressAppService = chainScanProgressAppService;
        this.web3Gateway = web3Gateway;
        this.depositCandidateAppService = depositCandidateAppService;
        this.depositSettlementAppService = depositSettlementAppService;
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
        ChainScanProgressResponse progress = chainScanProgressAppService.getOrInitProgress(
                null,
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK
        );
        if (!walletWeb3Properties.readyForDepositScan() || !web3Gateway.supportsTransferScan()) {
            log.info("跳过 ERC-20 充值扫描，当前未配置可用的 RPC 或代币合约地址，gateway={}", web3Gateway.clientName());
            return progress;
        }

        long fromBlock = resolveFromBlock(progress.getLastScannedBlock());
        long latestBlock = web3Gateway.getLatestBlockNumber();
        if (latestBlock < fromBlock) {
            log.info("当前暂无可扫描区块，fromBlock={}, latestBlock={}", fromBlock, latestBlock);
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
        log.info("完成一轮 ERC-20 充值扫描，chain={}, fromBlock={}, toBlock={}, detectedCount={}, creditedCount={}",
                walletDefaultsProperties.chain(), fromBlock, toBlock, detectedCount, creditedCount);
        return chainScanProgressAppService.updateProgress(
                walletDefaultsProperties.chain(),
                ChainScanProgressAppService.ERC20_DEPOSIT_SCAN_TASK,
                toBlock
        );
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
}
