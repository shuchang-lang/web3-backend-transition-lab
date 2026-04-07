package com.web3lab.wallet.application.deposit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.controller.dto.DepositReorgCheckResponse;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.deposit.DepositStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.web3.DepositLogChainCheckResult;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 充值链重组风险检查任务入口。
 *
 * <p>Day22 第一版只负责识别“历史充值日志已经无法在链上自洽复原”的场景，
 * 并把记录标记为 `REORG_SUSPECTED`，为后续补偿动作留出清晰边界。</p>
 */
@Component
public class DepositReorgCheckTask {

    /**
     * 充值链重组检查任务名称。
     */
    public static final String DEPOSIT_REORG_CHECK_TASK = "erc20_deposit_reorg_check";

    private static final Logger log = LoggerFactory.getLogger(DepositReorgCheckTask.class);

    private final DepositRecordMapper depositRecordMapper;
    private final Web3Gateway web3Gateway;

    public DepositReorgCheckTask(DepositRecordMapper depositRecordMapper, Web3Gateway web3Gateway) {
        this.depositRecordMapper = depositRecordMapper;
        this.web3Gateway = web3Gateway;
    }

    /**
     * 执行一轮充值链重组风险检查。
     *
     * @return 任务结果
     */
    @Transactional
    public DepositReorgCheckResponse runOnce() {
        if (!web3Gateway.supportsDepositReorgCheck()) {
            return new DepositReorgCheckResponse(
                    DEPOSIT_REORG_CHECK_TASK,
                    null,
                    0,
                    0,
                    false,
                    "当前未配置可用的充值链上核验能力，跳过本轮 reorg 检查"
            );
        }

        List<DepositRecord> depositRecords = depositRecordMapper.selectList(
                Wrappers.<DepositRecord>lambdaQuery()
                        .in(DepositRecord::getStatus,
                                DepositStatusConstants.PENDING_CONFIRM,
                                DepositStatusConstants.CREDITED)
                        .orderByAsc(DepositRecord::getId)
        );
        if (depositRecords.isEmpty()) {
            return new DepositReorgCheckResponse(
                    DEPOSIT_REORG_CHECK_TASK,
                    web3Gateway.getLatestBlockNumber(),
                    0,
                    0,
                    true,
                    "当前没有需要检查的充值记录"
            );
        }

        long latestBlock = web3Gateway.getLatestBlockNumber();
        int suspectedCount = 0;
        for (DepositRecord depositRecord : depositRecords) {
            DepositLogChainCheckResult checkResult = web3Gateway.inspectDepositTransferLog(
                    depositRecord.getTokenContract(),
                    depositRecord.getTxHash(),
                    depositRecord.getLogIndex(),
                    depositRecord.getBlockNumber()
            );
            if (!checkResult.isReorgSuspected()) {
                continue;
            }
            depositRecord.markReorgSuspected();
            depositRecordMapper.updateById(depositRecord);
            suspectedCount++;
            log.warn("充值记录疑似发生链重组，depositId={}, txHash={}, reason={}",
                    depositRecord.getId(), depositRecord.getTxHash(), checkResult.getReason());
        }

        return new DepositReorgCheckResponse(
                DEPOSIT_REORG_CHECK_TASK,
                latestBlock,
                depositRecords.size(),
                suspectedCount,
                true,
                suspectedCount > 0 ? "已标记疑似链重组充值记录" : "本轮未发现疑似链重组充值记录"
        );
    }
}
