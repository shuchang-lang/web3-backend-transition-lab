package com.web3lab.wallet.application.withdraw;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.common.task.TaskFailureLevelConstants;
import com.web3lab.wallet.config.WalletWeb3Properties;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.domain.withdraw.WithdrawStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import com.web3lab.wallet.infrastructure.web3.Web3Gateway;
import com.web3lab.wallet.infrastructure.web3.WithdrawBroadcastResult;
import com.web3lab.wallet.infrastructure.web3.WithdrawTransactionReceiptResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 提现执行应用服务。
 *
 * <p>它负责把审核通过的提现订单继续推进到“已提交广播”，
 * 并在拿到链上回执后决定正式扣减冻结余额还是执行失败回退。</p>
 */
@Service
public class WithdrawExecutionAppService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawExecutionAppService.class);

    private final WithdrawOrderMapper withdrawOrderMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountBillMapper accountBillMapper;
    private final Web3Gateway web3Gateway;
    private final WalletWeb3Properties walletWeb3Properties;

    public WithdrawExecutionAppService(WithdrawOrderMapper withdrawOrderMapper,
                                       AccountBalanceMapper accountBalanceMapper,
                                       AccountBillMapper accountBillMapper,
                                       Web3Gateway web3Gateway,
                                       WalletWeb3Properties walletWeb3Properties) {
        this.withdrawOrderMapper = withdrawOrderMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.accountBillMapper = accountBillMapper;
        this.web3Gateway = web3Gateway;
        this.walletWeb3Properties = walletWeb3Properties;
    }

    /**
     * 广播待执行的提现订单。
     *
     * <p>当前最小版本只处理 `PENDING_BROADCAST` 状态订单。
     * 如果节点或签名环境异常，本轮会记录日志并保留订单在待广播状态，等待下一轮重试。</p>
     *
     * @return 本轮扫描数量与成功推进数量
     */
    @Transactional
    public BroadcastBatchResult broadcastPendingOrders() {
        // Day25 第一版在任务入口层先拿本地串行锁，这里继续按订单 ID 顺序逐笔处理，
        // 让同一批次内的 nonce 预留和广播顺序保持稳定、可解释。
        List<WithdrawOrder> pendingOrders = withdrawOrderMapper.selectList(
                Wrappers.<WithdrawOrder>lambdaQuery()
                        .eq(WithdrawOrder::getStatus, WithdrawStatusConstants.PENDING_BROADCAST)
                        .orderByAsc(WithdrawOrder::getId)
        );
        if (pendingOrders.isEmpty()) {
            return new BroadcastBatchResult(0, 0);
        }

        long nextAvailableNonce = resolveNextAvailableNonce();
        int updatedCount = 0;
        for (WithdrawOrder withdrawOrder : pendingOrders) {
            long reservedNonce = reserveNonceIfNecessary(withdrawOrder, nextAvailableNonce);
            nextAvailableNonce = Math.max(nextAvailableNonce, reservedNonce + 1L);
            try {
                // Web3 广播阶段继续复用订单里已预留的 nonce，避免重试时反复切换 nonce 造成链上顺序混乱。
                WithdrawBroadcastResult broadcastResult = web3Gateway.broadcastErc20Withdraw(
                        walletWeb3Properties.depositTokenContract(),
                        withdrawOrder.getToAddress(),
                        withdrawOrder.getAmount(),
                        walletWeb3Properties.resolvedTokenDecimals(),
                        reservedNonce
                );
                withdrawOrder.markBroadcastSubmitted(broadcastResult.getTxHash(), broadcastResult.getNonce());
                withdrawOrderMapper.updateById(withdrawOrder);
                updatedCount++;
            } catch (RuntimeException exception) {
                handleBroadcastFailure(withdrawOrder, exception);
                withdrawOrderMapper.updateById(withdrawOrder);
                log.warn("提现广播失败，requestNo={}, reason={}", withdrawOrder.getRequestNo(), exception.getMessage());
            }
        }
        return new BroadcastBatchResult(pendingOrders.size(), updatedCount);
    }

    /**
     * 同步已广播提现订单的链上回执。
     *
     * <p>当前最小版本只处理 `BROADCAST_SUBMITTED` 状态订单：
     * 成功则真正扣减冻结余额，失败则解冻回退。</p>
     *
     * @return 本轮扫描数量与实际推进数量
     */
    @Transactional
    public BroadcastBatchResult syncSubmittedReceipts() {
        List<WithdrawOrder> submittedOrders = withdrawOrderMapper.selectList(
                Wrappers.<WithdrawOrder>lambdaQuery()
                        .eq(WithdrawOrder::getStatus, WithdrawStatusConstants.BROADCAST_SUBMITTED)
                        .orderByAsc(WithdrawOrder::getId)
        );
        if (submittedOrders.isEmpty()) {
            return new BroadcastBatchResult(0, 0);
        }

        int updatedCount = 0;
        for (WithdrawOrder withdrawOrder : submittedOrders) {
            WithdrawTransactionReceiptResult receiptResult = web3Gateway.getWithdrawTransactionReceipt(withdrawOrder.getTxHash());
            if (!receiptResult.isMined()) {
                continue;
            }
            if (receiptResult.isSuccess()) {
                settleSuccess(withdrawOrder);
            } else {
                rollbackFailedWithdraw(withdrawOrder, receiptResult.getFailReason());
            }
            updatedCount++;
        }
        return new BroadcastBatchResult(submittedOrders.size(), updatedCount);
    }

    /**
     * 巡检长时间未返回回执的提现订单。
     *
     * <p>Day24 第一版只处理 `BROADCAST_SUBMITTED` 状态订单：
     * 1. 未达到超时阈值，则继续等待
     * 2. 达到超时阈值后，先重查一次回执
     * 3. 已经有回执，则直接落成功 / 失败结算
     * 4. 仍无回执，则按重试次数决定“回退待广播”还是“转人工处理”</p>
     *
     * @return 巡检结果统计
     */
    @Transactional
    public TimeoutInspectionBatchResult inspectTimeoutSubmittedOrders() {
        List<WithdrawOrder> submittedOrders = withdrawOrderMapper.selectList(
                Wrappers.<WithdrawOrder>lambdaQuery()
                        .eq(WithdrawOrder::getStatus, WithdrawStatusConstants.BROADCAST_SUBMITTED)
                        .orderByAsc(WithdrawOrder::getId)
        );
        if (submittedOrders.isEmpty()) {
            return new TimeoutInspectionBatchResult(0, 0, 0, 0, 0);
        }

        long timeoutMinutes = walletWeb3Properties.resolvedWithdrawReceiptTimeoutMinutes();
        int maxRetryCount = walletWeb3Properties.resolvedWithdrawReceiptMaxTimeoutRetryCount();
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(timeoutMinutes);

        int continueWaitingCount = 0;
        int resolvedCount = 0;
        int retryableCount = 0;
        int manualHandleCount = 0;

        for (WithdrawOrder withdrawOrder : submittedOrders) {
            if (!isReceiptTimedOut(withdrawOrder, timeoutBefore)) {
                continueWaitingCount++;
                continue;
            }
            if (!StringUtils.hasText(withdrawOrder.getTxHash())) {
                withdrawOrder.markManualHandleRequired("已提交广播订单缺少 txHash，需人工处理");
                withdrawOrderMapper.updateById(withdrawOrder);
                manualHandleCount++;
                continue;
            }

            // 超时后先重查一次链上回执，避免把“其实已经落链但节点返回延迟”的订单误打回重试。
            WithdrawTransactionReceiptResult receiptResult = web3Gateway
                    .getWithdrawTransactionReceipt(withdrawOrder.getTxHash());
            if (receiptResult.isMined()) {
                if (receiptResult.isSuccess()) {
                    settleSuccess(withdrawOrder);
                } else {
                    rollbackFailedWithdraw(withdrawOrder, receiptResult.getFailReason());
                }
                resolvedCount++;
                continue;
            }

            if (resolveReceiptCheckRetryCount(withdrawOrder) >= maxRetryCount) {
                withdrawOrder.markManualHandleRequired(buildManualHandleReason(timeoutMinutes, maxRetryCount));
                withdrawOrderMapper.updateById(withdrawOrder);
                manualHandleCount++;
                continue;
            }

            withdrawOrder.markReceiptTimeoutRetryable(buildTimeoutRetryableReason(timeoutMinutes));
            withdrawOrderMapper.updateById(withdrawOrder);
            retryableCount++;
        }

        return new TimeoutInspectionBatchResult(
                submittedOrders.size(),
                continueWaitingCount,
                resolvedCount,
                retryableCount,
                manualHandleCount
        );
    }

    /**
     * 处理链上执行成功的提现订单。
     *
     * @param withdrawOrder 提现订单
     */
    private void settleSuccess(WithdrawOrder withdrawOrder) {
        AccountBalance accountBalance = requireBalance(withdrawOrder);
        BigDecimal totalAmount = withdrawOrder.getAmount().add(withdrawOrder.getFee());
        accountBalance.deductFrozen(totalAmount);
        accountBalanceMapper.updateById(accountBalance);

        accountBillMapper.insert(AccountBill.createWithdrawDeduct(
                withdrawOrder.getUserId(),
                withdrawOrder.getChain(),
                withdrawOrder.getTokenSymbol(),
                withdrawOrder.getRequestNo(),
                totalAmount.negate(),
                accountBalance.getAvailableBalance(),
                accountBalance.getFrozenBalance(),
                "提现链上成功，正式扣减冻结金额"
        ));

        withdrawOrder.markSuccess();
        withdrawOrderMapper.updateById(withdrawOrder);
    }

    /**
     * 处理链上执行失败的提现订单。
     *
     * @param withdrawOrder 提现订单
     * @param failReason 失败原因
     */
    private void rollbackFailedWithdraw(WithdrawOrder withdrawOrder, String failReason) {
        AccountBalance accountBalance = requireBalance(withdrawOrder);
        BigDecimal totalAmount = withdrawOrder.getAmount().add(withdrawOrder.getFee());
        accountBalance.unfreezeToAvailable(totalAmount);
        accountBalanceMapper.updateById(accountBalance);

        accountBillMapper.insert(AccountBill.createWithdrawUnfreeze(
                withdrawOrder.getUserId(),
                withdrawOrder.getChain(),
                withdrawOrder.getTokenSymbol(),
                withdrawOrder.getRequestNo(),
                totalAmount,
                accountBalance.getAvailableBalance(),
                accountBalance.getFrozenBalance(),
                "提现链上失败，解冻 amount + fee"
        ));

        withdrawOrder.markFailed(failReason == null ? "链上执行失败" : failReason);
        withdrawOrderMapper.updateById(withdrawOrder);
    }

    /**
     * 查询提现订单对应的余额主表。
     *
     * @param withdrawOrder 提现订单
     * @return 余额主表实体
     */
    private AccountBalance requireBalance(WithdrawOrder withdrawOrder) {
        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, withdrawOrder.getUserId())
                .eq(AccountBalance::getChain, withdrawOrder.getChain())
                .eq(AccountBalance::getTokenSymbol, withdrawOrder.getTokenSymbol())
                .last("LIMIT 1"));
        if (accountBalance == null) {
            throw new IllegalStateException("未找到对应余额主表，无法推进提现执行链路");
        }
        return accountBalance;
    }

    /**
     * 解析当前批次可用的起始 nonce。
     *
     * <p>这里同时参考节点返回的 pending nonce 和本地订单里已预留的最大 nonce，
     * 这样即使上一轮广播失败，下一轮也能避免把新订单分配到已经预留过的 nonce 上。</p>
     *
     * @return 当前批次起始 nonce
     */
    private long resolveNextAvailableNonce() {
        long chainSuggestedNonce = web3Gateway.getSuggestedWithdrawNonce();
        Long localMaxReservedNonce = withdrawOrderMapper.selectList(
                        Wrappers.<WithdrawOrder>lambdaQuery()
                                .in(WithdrawOrder::getStatus,
                                        WithdrawStatusConstants.PENDING_BROADCAST,
                                        WithdrawStatusConstants.BROADCAST_SUBMITTED)
                                .isNotNull(WithdrawOrder::getNonce)
                ).stream()
                .map(WithdrawOrder::getNonce)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (localMaxReservedNonce == null) {
            return chainSuggestedNonce;
        }
        return Math.max(chainSuggestedNonce, localMaxReservedNonce + 1L);
    }

    /**
     * 为待广播订单预留 nonce。
     *
     * <p>如果订单此前已经预留过 nonce，则本轮重试继续复用，避免反复切换 nonce
     * 造成链上顺序和排障复杂度上升。</p>
     *
     * @param withdrawOrder 提现订单
     * @param nextAvailableNonce 当前批次建议使用的下一个 nonce
     * @return 本订单实际使用的 nonce
     */
    private long reserveNonceIfNecessary(WithdrawOrder withdrawOrder, long nextAvailableNonce) {
        if (withdrawOrder.getNonce() != null) {
            return withdrawOrder.getNonce();
        }
        withdrawOrder.reserveNonce(nextAvailableNonce);
        withdrawOrderMapper.updateById(withdrawOrder);
        return nextAvailableNonce;
    }

    /**
     * 判断订单是否已经超过回执等待阈值。
     *
     * @param withdrawOrder 提现订单
     * @param timeoutBefore 超时临界时间
     * @return true 表示已经超时
     */
    private boolean isReceiptTimedOut(WithdrawOrder withdrawOrder, LocalDateTime timeoutBefore) {
        return withdrawOrder.getUpdatedAt() != null && withdrawOrder.getUpdatedAt().isBefore(timeoutBefore);
    }

    /**
     * 解析当前订单已经发生过的回执超时重试次数。
     *
     * @param withdrawOrder 提现订单
     * @return 非空重试次数
     */
    private int resolveReceiptCheckRetryCount(WithdrawOrder withdrawOrder) {
        return withdrawOrder.getReceiptCheckRetryCount() == null ? 0 : withdrawOrder.getReceiptCheckRetryCount();
    }

    /**
     * 处理提现广播失败时的任务分层。
     *
     * <p>Day26 第一版只把失败分层真正落到“提现广播”链路：人工级错误直接转人工，
     * 其余节点 / 网络类错误允许自动重试，但会受到最大重试上限约束。</p>
     *
     * @param withdrawOrder 提现订单
     * @param exception 广播异常
     */
    private void handleBroadcastFailure(WithdrawOrder withdrawOrder, RuntimeException exception) {
        String failReason = extractBroadcastFailReason(exception);
        String failureLevel = classifyBroadcastFailure(failReason);
        int nextRetryCount = resolveBroadcastRetryCount(withdrawOrder) + 1;
        int maxRetryCount = walletWeb3Properties.resolvedWithdrawBroadcastMaxRetryCount();

        if (TaskFailureLevelConstants.MANUAL_HANDLE_REQUIRED.equals(failureLevel)) {
            withdrawOrder.markBroadcastManualHandleRequired(buildManualBroadcastFailureReason(failReason));
            return;
        }
        if (nextRetryCount >= maxRetryCount) {
            withdrawOrder.markBroadcastManualHandleRequired(
                    buildBroadcastRetryExceededReason(failReason, maxRetryCount)
            );
            return;
        }
        withdrawOrder.markBroadcastRetryableFailure(failReason);
    }

    /**
     * 解析当前订单已发生的广播失败自动重试次数。
     *
     * @param withdrawOrder 提现订单
     * @return 非空自动重试次数
     */
    private int resolveBroadcastRetryCount(WithdrawOrder withdrawOrder) {
        return withdrawOrder.getBroadcastRetryCount() == null ? 0 : withdrawOrder.getBroadcastRetryCount();
    }

    /**
     * 按广播错误信息对失败等级进行分类。
     *
     * <p>这里优先识别典型的链上参数错误、资金不足、签名人异常和替换交易价格问题，
     * 这些错误通常不是简单等下一轮任务重跑就能恢复，因此直接归入人工处理层。
     * 其余错误先按节点 / 网络类波动处理，保留自动重试机会。</p>
     *
     * @param failReason 广播失败原因
     * @return 失败分层
     */
    private String classifyBroadcastFailure(String failReason) {
        if (!StringUtils.hasText(failReason)) {
            return TaskFailureLevelConstants.RETRYABLE;
        }
        String normalizedReason = failReason.toLowerCase(Locale.ROOT);
        if (normalizedReason.contains("nonce too low")
                || normalizedReason.contains("insufficient funds")
                || normalizedReason.contains("invalid sender")
                || normalizedReason.contains("replacement transaction underpriced")) {
            return TaskFailureLevelConstants.MANUAL_HANDLE_REQUIRED;
        }
        return TaskFailureLevelConstants.RETRYABLE;
    }

    /**
     * 解析广播异常说明，避免空 message 导致订单上只留下空白失败原因。
     *
     * @param exception 广播异常
     * @return 可落库的失败说明
     */
    private String extractBroadcastFailReason(RuntimeException exception) {
        if (exception == null) {
            return "提现广播失败，未获取到具体异常信息";
        }
        if (StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }
        return "提现广播失败，异常类型：" + exception.getClass().getSimpleName();
    }

    /**
     * 构造“广播失败但达到最大自动重试次数”的说明文案。
     *
     * @param failReason 广播失败原因
     * @param maxRetryCount 最大自动重试次数
     * @return 说明文案
     */
    private String buildBroadcastRetryExceededReason(String failReason, int maxRetryCount) {
        return "提现广播失败且已达到最大自动重试次数(" + maxRetryCount + ")，转人工处理，原因：" + failReason;
    }

    /**
     * 构造“命中人工处理级错误”的说明文案。
     *
     * @param failReason 广播失败原因
     * @return 说明文案
     */
    private String buildManualBroadcastFailureReason(String failReason) {
        return "提现广播失败命中人工处理级错误，原因：" + failReason;
    }

    /**
     * 构造“回执超时但仍可重试”的说明文案。
     *
     * @param timeoutMinutes 超时分钟数
     * @return 说明文案
     */
    private String buildTimeoutRetryableReason(long timeoutMinutes) {
        return "提现回执等待超过 " + timeoutMinutes + " 分钟，转回待广播重试";
    }

    /**
     * 构造“超过最大重试次数，转人工处理”的说明文案。
     *
     * @param timeoutMinutes 超时分钟数
     * @param maxRetryCount 最大重试次数
     * @return 说明文案
     */
    private String buildManualHandleReason(long timeoutMinutes, int maxRetryCount) {
        return "提现回执等待超过 " + timeoutMinutes + " 分钟且已达到最大重试次数(" + maxRetryCount + ")，转人工处理";
    }

    /**
     * 提现任务批处理结果。
     */
    public record BroadcastBatchResult(int processedCount, int updatedCount) {
    }

    /**
     * 提现回执超时巡检统计结果。
     */
    public record TimeoutInspectionBatchResult(int processedCount,
                                               int continueWaitingCount,
                                               int resolvedCount,
                                               int retryableCount,
                                               int manualHandleCount) {
    }
}
