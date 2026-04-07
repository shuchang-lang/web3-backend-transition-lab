package com.web3lab.wallet.application.deposit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.controller.dto.DepositCompensationResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.deposit.DepositStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 充值链重组补偿任务入口。
 *
 * <p>Day23 第一版只处理“已经正式入账、后来又被标记为 `REORG_SUSPECTED`”的充值记录，
 * 通过一条最小冲正流水把此前增加的可用余额扣回去，并把充值记录推进到终态。</p>
 */
@Component
public class DepositCompensationTask {

    /**
     * 充值补偿任务名称。
     */
    public static final String DEPOSIT_COMPENSATION_TASK = "erc20_deposit_compensation";

    private static final Logger log = LoggerFactory.getLogger(DepositCompensationTask.class);

    private final DepositRecordMapper depositRecordMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountBillMapper accountBillMapper;

    public DepositCompensationTask(DepositRecordMapper depositRecordMapper,
                                   AccountBalanceMapper accountBalanceMapper,
                                   AccountBillMapper accountBillMapper) {
        this.depositRecordMapper = depositRecordMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.accountBillMapper = accountBillMapper;
    }

    /**
     * 执行一轮充值补偿任务。
     *
     * @return 任务结果
     */
    @Transactional
    public DepositCompensationResponse runOnce() {
        List<DepositRecord> riskDeposits = depositRecordMapper.selectList(
                Wrappers.<DepositRecord>lambdaQuery()
                        .eq(DepositRecord::getStatus, DepositStatusConstants.REORG_SUSPECTED)
                        .isNotNull(DepositRecord::getCreditedAt)
                        .orderByAsc(DepositRecord::getId)
        );
        if (riskDeposits.isEmpty()) {
            return new DepositCompensationResponse(
                    DEPOSIT_COMPENSATION_TASK,
                    0,
                    0,
                    false,
                    "当前没有需要冲正的已入账异常充值记录"
            );
        }

        int compensatedCount = 0;
        for (DepositRecord depositRecord : riskDeposits) {
            AccountBalance accountBalance = findOrCreateBalance(
                    depositRecord.getUserId(),
                    depositRecord.getChain(),
                    depositRecord.getTokenSymbol()
            );
            accountBalance.deductAvailable(depositRecord.getAmount());
            accountBalanceMapper.updateById(accountBalance);

            accountBillMapper.insert(AccountBill.createDepositReorgDeduct(
                    depositRecord.getUserId(),
                    depositRecord.getChain(),
                    depositRecord.getTokenSymbol(),
                    String.valueOf(depositRecord.getId()),
                    depositRecord.getAmount().negate(),
                    accountBalance.getAvailableBalance(),
                    accountBalance.getFrozenBalance(),
                    "充值链重组风险冲正，扣回此前已入账金额"
            ));

            depositRecord.markReorgCompensated();
            depositRecordMapper.updateById(depositRecord);
            compensatedCount++;
            log.warn("已完成异常充值冲正，depositId={}, txHash={}, amount={}",
                    depositRecord.getId(), depositRecord.getTxHash(), depositRecord.getAmount());
        }
        return new DepositCompensationResponse(
                DEPOSIT_COMPENSATION_TASK,
                riskDeposits.size(),
                compensatedCount,
                true,
                "已完成疑似链重组充值冲正"
        );
    }

    /**
     * 查询余额主表，不存在时补一条零余额记录。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @return 余额主表
     */
    private AccountBalance findOrCreateBalance(Long userId, String chain, String tokenSymbol) {
        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, userId)
                .eq(AccountBalance::getChain, chain)
                .eq(AccountBalance::getTokenSymbol, tokenSymbol)
                .last("LIMIT 1"));
        if (accountBalance != null) {
            return accountBalance;
        }

        AccountBalance created = AccountBalance.createZero(userId, chain, tokenSymbol);
        accountBalanceMapper.insert(created);
        return created;
    }
}
