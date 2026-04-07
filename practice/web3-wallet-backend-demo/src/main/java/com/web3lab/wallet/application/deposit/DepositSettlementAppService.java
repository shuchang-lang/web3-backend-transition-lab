package com.web3lab.wallet.application.deposit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.deposit.DepositStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 充值确认与正式入账应用服务。
 *
 * <p>它负责把 `deposit_record` 从“候选充值”继续推进到“达到确认阈值后正式入账”，
 * 并同步更新余额主表和账户流水。</p>
 */
@Service
public class DepositSettlementAppService {

    private final DepositRecordMapper depositRecordMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountBillMapper accountBillMapper;

    public DepositSettlementAppService(DepositRecordMapper depositRecordMapper,
                                       AccountBalanceMapper accountBalanceMapper,
                                       AccountBillMapper accountBillMapper) {
        this.depositRecordMapper = depositRecordMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.accountBillMapper = accountBillMapper;
    }

    /**
     * 刷新待确认充值的确认数，并把达到阈值的记录正式入账。
     *
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param latestBlock 当前链头区块
     * @param confirmationsThreshold 正式入账所需确认数
     * @return 本轮正式入账数量
     */
    @Transactional
    public int refreshConfirmationsAndCredit(String chain, String tokenSymbol, long latestBlock,
                                             int confirmationsThreshold) {
        List<DepositRecord> pendingRecords = depositRecordMapper.selectList(
                Wrappers.<DepositRecord>lambdaQuery()
                        .eq(DepositRecord::getChain, chain)
                        .eq(DepositRecord::getTokenSymbol, tokenSymbol)
                        .eq(DepositRecord::getStatus, DepositStatusConstants.PENDING_CONFIRM)
                        .orderByAsc(DepositRecord::getId)
        );
        if (pendingRecords.isEmpty()) {
            return 0;
        }

        int creditedCount = 0;
        for (DepositRecord depositRecord : pendingRecords) {
            long confirmations = calculateConfirmations(latestBlock, depositRecord.getBlockNumber());
            if (confirmations < confirmationsThreshold) {
                depositRecord.refreshConfirmations(confirmations);
                depositRecordMapper.updateById(depositRecord);
                continue;
            }

            AccountBalance accountBalance = findOrCreateBalance(
                    depositRecord.getUserId(),
                    depositRecord.getChain(),
                    depositRecord.getTokenSymbol()
            );
            accountBalance.creditAvailable(depositRecord.getAmount());
            accountBalanceMapper.updateById(accountBalance);

            // 充值入账流水使用 deposit_record 主键做幂等主键，后续账务排查也更容易反查。
            accountBillMapper.insert(AccountBill.createDepositCredit(
                    depositRecord.getUserId(),
                    depositRecord.getChain(),
                    depositRecord.getTokenSymbol(),
                    String.valueOf(depositRecord.getId()),
                    depositRecord.getAmount(),
                    accountBalance.getAvailableBalance(),
                    accountBalance.getFrozenBalance(),
                    "ERC-20 充值正式入账"
            ));

            depositRecord.markCredited(confirmations);
            depositRecordMapper.updateById(depositRecord);
            creditedCount++;
        }
        return creditedCount;
    }

    /**
     * 计算充值当前确认数。
     *
     * <p>最小版实现按 `latestBlock - depositBlock + 1` 计算，
     * 这里先不处理链重组回退，只作为 Day16 的正式入账门槛。</p>
     *
     * @param latestBlock 当前链头区块
     * @param depositBlock 充值所在区块
     * @return 当前确认数
     */
    private long calculateConfirmations(long latestBlock, Long depositBlock) {
        if (depositBlock == null || latestBlock < depositBlock) {
            return 0L;
        }
        return latestBlock - depositBlock + 1L;
    }

    /**
     * 查询余额主表，不存在时补一条零余额记录。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @return 余额主表实体
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
