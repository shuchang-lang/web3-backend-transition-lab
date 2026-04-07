package com.web3lab.wallet.application.account;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountBillBizTypeConstants;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.deposit.DepositStatusConstants;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.domain.withdraw.WithdrawStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 账户资产对账应用服务。
 *
 * <p>当前第一版重点做三类最小核对：
 * 1. 余额主表和业务表汇总是否一致
 * 2. 余额主表和流水快照是否一致
 * 3. 冻结余额和待完成提现总额是否一致</p>
 */
@Service
public class AccountReconcileAppService {

    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountBillMapper accountBillMapper;
    private final DepositRecordMapper depositRecordMapper;
    private final WithdrawOrderMapper withdrawOrderMapper;
    private final WalletDefaultsProperties walletDefaultsProperties;

    public AccountReconcileAppService(AccountBalanceMapper accountBalanceMapper,
                                      AccountBillMapper accountBillMapper,
                                      DepositRecordMapper depositRecordMapper,
                                      WithdrawOrderMapper withdrawOrderMapper,
                                      WalletDefaultsProperties walletDefaultsProperties) {
        this.accountBalanceMapper = accountBalanceMapper;
        this.accountBillMapper = accountBillMapper;
        this.depositRecordMapper = depositRecordMapper;
        this.withdrawOrderMapper = withdrawOrderMapper;
        this.walletDefaultsProperties = walletDefaultsProperties;
    }

    /**
     * 查询指定用户资产的最小对账结果。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码，未传时回落到默认链
     * @param tokenSymbol 币种符号，未传时回落到默认币种
     * @return 对账结果响应
     */
    public AccountAssetReconcileResponse reconcile(Long userId, String chain, String tokenSymbol) {
        String resolvedChain = resolveOrDefault(chain, walletDefaultsProperties.chain());
        String resolvedTokenSymbol = resolveOrDefault(tokenSymbol, walletDefaultsProperties.tokenSymbol());

        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, userId)
                .eq(AccountBalance::getChain, resolvedChain)
                .eq(AccountBalance::getTokenSymbol, resolvedTokenSymbol)
                .last("LIMIT 1"));
        List<AccountBill> accountBills = accountBillMapper.selectList(Wrappers.<AccountBill>lambdaQuery()
                .eq(AccountBill::getUserId, userId)
                .eq(AccountBill::getChain, resolvedChain)
                .eq(AccountBill::getTokenSymbol, resolvedTokenSymbol)
                .orderByAsc(AccountBill::getId));
        List<DepositRecord> creditedDeposits = depositRecordMapper.selectList(Wrappers.<DepositRecord>lambdaQuery()
                .eq(DepositRecord::getUserId, userId)
                .eq(DepositRecord::getChain, resolvedChain)
                .eq(DepositRecord::getTokenSymbol, resolvedTokenSymbol)
                .eq(DepositRecord::getStatus, DepositStatusConstants.CREDITED));
        List<WithdrawOrder> withdrawOrders = withdrawOrderMapper.selectList(Wrappers.<WithdrawOrder>lambdaQuery()
                .eq(WithdrawOrder::getUserId, userId)
                .eq(WithdrawOrder::getChain, resolvedChain)
                .eq(WithdrawOrder::getTokenSymbol, resolvedTokenSymbol)
                .orderByAsc(WithdrawOrder::getId));

        BigDecimal availableBalance = accountBalance == null ? BigDecimal.ZERO : zeroIfNull(accountBalance.getAvailableBalance());
        BigDecimal frozenBalance = accountBalance == null ? BigDecimal.ZERO : zeroIfNull(accountBalance.getFrozenBalance());
        BigDecimal totalBalance = availableBalance.add(frozenBalance);
        BigDecimal creditedDepositAmount = creditedDeposits.stream()
                .map(DepositRecord::getAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal successfulWithdrawAmount = withdrawOrders.stream()
                .filter(withdrawOrder -> WithdrawStatusConstants.SUCCESS.equals(withdrawOrder.getStatus()))
                .map(this::resolveWithdrawTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingWithdrawFrozenAmount = withdrawOrders.stream()
                .filter(this::shouldRemainFrozen)
                .map(this::resolveWithdrawTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal billAssetDeltaAmount = accountBills.stream()
                .map(this::resolveBillAssetDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AccountBill latestBill = accountBills.isEmpty() ? null : accountBills.get(accountBills.size() - 1);
        BigDecimal latestBillAvailableAfter = latestBill == null ? BigDecimal.ZERO : zeroIfNull(latestBill.getAvailableAfter());
        BigDecimal latestBillFrozenAfter = latestBill == null ? BigDecimal.ZERO : zeroIfNull(latestBill.getFrozenAfter());
        BigDecimal expectedTotalByBusiness = creditedDepositAmount.subtract(successfulWithdrawAmount);

        boolean consistentWithBusinessTables = totalBalance.compareTo(expectedTotalByBusiness) == 0;
        boolean consistentWithAssetDeltaBills = totalBalance.compareTo(billAssetDeltaAmount) == 0;
        boolean consistentWithLatestBillSnapshot = latestBill == null
                ? availableBalance.compareTo(BigDecimal.ZERO) == 0 && frozenBalance.compareTo(BigDecimal.ZERO) == 0
                : availableBalance.compareTo(latestBillAvailableAfter) == 0
                && frozenBalance.compareTo(latestBillFrozenAfter) == 0;
        boolean consistentWithPendingWithdraws = frozenBalance.compareTo(pendingWithdrawFrozenAmount) == 0;
        boolean consistent = consistentWithBusinessTables
                && consistentWithAssetDeltaBills
                && consistentWithLatestBillSnapshot
                && consistentWithPendingWithdraws;

        AccountAssetReconcileResponse response = new AccountAssetReconcileResponse();
        response.setUserId(userId);
        response.setChain(resolvedChain);
        response.setTokenSymbol(resolvedTokenSymbol);
        response.setAvailableBalance(availableBalance);
        response.setFrozenBalance(frozenBalance);
        response.setTotalBalance(totalBalance);
        response.setCreditedDepositAmount(creditedDepositAmount);
        response.setSuccessfulWithdrawAmount(successfulWithdrawAmount);
        response.setPendingWithdrawFrozenAmount(pendingWithdrawFrozenAmount);
        response.setBillAssetDeltaAmount(billAssetDeltaAmount);
        response.setLatestBillAvailableAfter(latestBillAvailableAfter);
        response.setLatestBillFrozenAfter(latestBillFrozenAfter);
        response.setBillCount(accountBills.size());
        response.setConsistentWithBusinessTables(consistentWithBusinessTables);
        response.setConsistentWithAssetDeltaBills(consistentWithAssetDeltaBills);
        response.setConsistentWithLatestBillSnapshot(consistentWithLatestBillSnapshot);
        response.setConsistentWithPendingWithdraws(consistentWithPendingWithdraws);
        response.setConsistent(consistent);
        response.setMismatchReason(buildMismatchReason(
                consistentWithBusinessTables,
                consistentWithAssetDeltaBills,
                consistentWithLatestBillSnapshot,
                consistentWithPendingWithdraws
        ));
        return response;
    }

    /**
     * 解析单条提现订单的总冻结口径。
     *
     * @param withdrawOrder 提现订单
     * @return `amount + fee`
     */
    private BigDecimal resolveWithdrawTotalAmount(WithdrawOrder withdrawOrder) {
        return zeroIfNull(withdrawOrder.getAmount()).add(zeroIfNull(withdrawOrder.getFee()));
    }

    /**
     * 判断提现订单当前是否仍应占用冻结余额。
     *
     * @param withdrawOrder 提现订单
     * @return true 表示仍应冻结
     */
    private boolean shouldRemainFrozen(WithdrawOrder withdrawOrder) {
        return WithdrawStatusConstants.PENDING_REVIEW.equals(withdrawOrder.getStatus())
                || WithdrawStatusConstants.PENDING_BROADCAST.equals(withdrawOrder.getStatus())
                || WithdrawStatusConstants.BROADCAST_SUBMITTED.equals(withdrawOrder.getStatus());
    }

    /**
     * 按“总资产是否变化”的口径折算流水影响。
     *
     * <p>冻结和解冻只是在可用余额与冻结余额之间搬移，不改变总资产，
     * 因此这里只把充值入账和提现成功扣减纳入总资产变化。</p>
     *
     * @param accountBill 账户流水
     * @return 总资产净变动金额
     */
    private BigDecimal resolveBillAssetDelta(AccountBill accountBill) {
        if (AccountBillBizTypeConstants.DEPOSIT_CREDIT.equals(accountBill.getBizType())
                || AccountBillBizTypeConstants.DEPOSIT_REORG_DEDUCT.equals(accountBill.getBizType())
                || AccountBillBizTypeConstants.WITHDRAW_DEDUCT.equals(accountBill.getBizType())) {
            return zeroIfNull(accountBill.getChangeAmount());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 生成最小对账失败原因汇总。
     *
     * @param consistentWithBusinessTables 是否与业务表一致
     * @param consistentWithAssetDeltaBills 是否与资产口径流水一致
     * @param consistentWithLatestBillSnapshot 是否与最新流水快照一致
     * @param consistentWithPendingWithdraws 是否与待完成提现冻结一致
     * @return 失败原因，全部一致时返回空字符串
     */
    private String buildMismatchReason(boolean consistentWithBusinessTables,
                                       boolean consistentWithAssetDeltaBills,
                                       boolean consistentWithLatestBillSnapshot,
                                       boolean consistentWithPendingWithdraws) {
        List<String> mismatchReasons = new ArrayList<>();
        if (!consistentWithBusinessTables) {
            mismatchReasons.add("余额主表和业务表汇总结果不一致");
        }
        if (!consistentWithAssetDeltaBills) {
            mismatchReasons.add("余额主表和资产口径流水净额不一致");
        }
        if (!consistentWithLatestBillSnapshot) {
            mismatchReasons.add("余额主表和最新流水快照不一致");
        }
        if (!consistentWithPendingWithdraws) {
            mismatchReasons.add("冻结余额和待完成提现总额不一致");
        }
        return String.join("；", mismatchReasons);
    }

    /**
     * 空值保护，避免金额计算时出现空指针。
     *
     * @param value 原始金额
     * @return 非空金额
     */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 在调用方未显式传值时回落到系统默认配置。
     *
     * @param value 原始输入值
     * @param defaultValue 默认值
     * @return 解析后的最终值
     */
    private String resolveOrDefault(String value, String defaultValue) {
        return Optional.ofNullable(value)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(defaultValue);
    }
}
