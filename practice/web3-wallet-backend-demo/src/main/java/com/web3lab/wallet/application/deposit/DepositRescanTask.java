package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.common.exception.BusinessException;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.controller.dto.DepositRescanResponse;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 手动补扫充值任务入口。
 *
 * <p>它用于处理“指定区块区间回放”的场景，和日常按断点推进的扫描任务分开，
 * 避免补扫过程中误推进正常扫描进度。</p>
 */
@Component
public class DepositRescanTask {

    /**
     * 手动补扫任务名称。
     */
    public static final String MANUAL_DEPOSIT_RESCAN_TASK = "erc20_deposit_manual_rescan";

    private static final Logger log = LoggerFactory.getLogger(DepositRescanTask.class);

    private final Web3Gateway web3Gateway;
    private final DepositCandidateAppService depositCandidateAppService;
    private final DepositSettlementAppService depositSettlementAppService;
    private final WalletDefaultsProperties walletDefaultsProperties;
    private final WalletWeb3Properties walletWeb3Properties;

    public DepositRescanTask(Web3Gateway web3Gateway,
                             DepositCandidateAppService depositCandidateAppService,
                             DepositSettlementAppService depositSettlementAppService,
                             WalletDefaultsProperties walletDefaultsProperties,
                             WalletWeb3Properties walletWeb3Properties) {
        this.web3Gateway = web3Gateway;
        this.depositCandidateAppService = depositCandidateAppService;
        this.depositSettlementAppService = depositSettlementAppService;
        this.walletDefaultsProperties = walletDefaultsProperties;
        this.walletWeb3Properties = walletWeb3Properties;
    }

    /**
     * 执行一次指定区块区间的充值补扫。
     *
     * <p>本方法只负责“回放区块 -> 识别候选充值 -> 顺带刷新确认数”，
     * 不会更新 `chain_scan_progress`，避免人工补扫污染正常调度断点。</p>
     *
     * @param fromBlock 补扫起始区块
     * @param toBlock 补扫结束区块
     * @return 手动补扫结果
     */
    public DepositRescanResponse runOnce(Long fromBlock, Long toBlock) {
        validateRange(fromBlock, toBlock);
        if (!walletWeb3Properties.readyForDepositScan() || !web3Gateway.supportsTransferScan()) {
            return new DepositRescanResponse(
                    MANUAL_DEPOSIT_RESCAN_TASK,
                    fromBlock,
                    toBlock,
                    null,
                    null,
                    0,
                    0,
                    false,
                    "当前未配置可用的 RPC 或代币合约地址，无法执行手动补扫"
            );
        }

        long latestBlock = web3Gateway.getLatestBlockNumber();
        if (latestBlock < fromBlock) {
            return new DepositRescanResponse(
                    MANUAL_DEPOSIT_RESCAN_TASK,
                    fromBlock,
                    toBlock,
                    null,
                    latestBlock,
                    0,
                    0,
                    false,
                    "当前链头低于补扫起点，本轮没有可回放区块"
            );
        }

        long effectiveToBlock = Math.min(latestBlock, toBlock);
        int detectedCount = depositCandidateAppService.detectAndStore(
                walletDefaultsProperties.chain(),
                walletDefaultsProperties.tokenSymbol(),
                walletWeb3Properties.resolvedTokenDecimals(),
                web3Gateway.getErc20TransferLogs(walletWeb3Properties.depositTokenContract(), fromBlock, effectiveToBlock)
        );
        int creditedCount = depositSettlementAppService.refreshConfirmationsAndCredit(
                walletDefaultsProperties.chain(),
                walletDefaultsProperties.tokenSymbol(),
                latestBlock,
                walletWeb3Properties.resolvedConfirmationsThreshold()
        );
        log.info("完成一轮手动充值补扫，chain={}, fromBlock={}, toBlock={}, effectiveToBlock={}, detectedCount={}, creditedCount={}",
                walletDefaultsProperties.chain(), fromBlock, toBlock, effectiveToBlock, detectedCount, creditedCount);
        return new DepositRescanResponse(
                MANUAL_DEPOSIT_RESCAN_TASK,
                fromBlock,
                toBlock,
                effectiveToBlock,
                latestBlock,
                detectedCount,
                creditedCount,
                true,
                effectiveToBlock < toBlock ? "补扫结束区块超过当前链头，已自动裁剪到最新区块" : "补扫完成"
        );
    }

    /**
     * 校验手动补扫区间是否合法。
     *
     * @param fromBlock 补扫起始区块
     * @param toBlock 补扫结束区块
     */
    private void validateRange(Long fromBlock, Long toBlock) {
        if (fromBlock == null || toBlock == null) {
            throw new BusinessException("补扫区块区间不能为空");
        }
        if (fromBlock < 0L || toBlock < 0L) {
            throw new BusinessException("补扫区块不能小于 0");
        }
        if (fromBlock > toBlock) {
            throw new BusinessException("fromBlock 不能大于 toBlock");
        }
    }
}
