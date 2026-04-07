package com.web3lab.wallet.application.account;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.application.task.TaskAuditLogAppService;
import com.web3lab.wallet.controller.dto.AccountAssetReconcileResponse;
import com.web3lab.wallet.controller.dto.AccountReconcileTaskResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.account.AccountReconcileResult;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.domain.deposit.DepositStatusConstants;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountReconcileResultMapper;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户资产自动对账任务入口。
 *
 * <p>Day27 第一版先延续当前项目的后台任务风格，提供“手动触发一轮任务 + 结果落库”的最小闭环，
 * 为后续接入真正的 `@Scheduled` 定时调度保留稳定入口。</p>
 */
@Component
public class AccountReconcileTask {

    /**
     * 自动对账任务名称。
     */
    public static final String ACCOUNT_RECONCILE_TASK = "account_asset_auto_reconcile";

    private static final Logger log = LoggerFactory.getLogger(AccountReconcileTask.class);

    private static final DateTimeFormatter TASK_BATCH_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountBillMapper accountBillMapper;
    private final DepositRecordMapper depositRecordMapper;
    private final WithdrawOrderMapper withdrawOrderMapper;
    private final AccountReconcileAppService accountReconcileAppService;
    private final AccountReconcileResultMapper accountReconcileResultMapper;
    private final TaskAuditLogAppService taskAuditLogAppService;

    public AccountReconcileTask(AccountBalanceMapper accountBalanceMapper,
                                AccountBillMapper accountBillMapper,
                                DepositRecordMapper depositRecordMapper,
                                WithdrawOrderMapper withdrawOrderMapper,
                                AccountReconcileAppService accountReconcileAppService,
                                AccountReconcileResultMapper accountReconcileResultMapper,
                                TaskAuditLogAppService taskAuditLogAppService) {
        this.accountBalanceMapper = accountBalanceMapper;
        this.accountBillMapper = accountBillMapper;
        this.depositRecordMapper = depositRecordMapper;
        this.withdrawOrderMapper = withdrawOrderMapper;
        this.accountReconcileAppService = accountReconcileAppService;
        this.accountReconcileResultMapper = accountReconcileResultMapper;
        this.taskAuditLogAppService = taskAuditLogAppService;
    }

    /**
     * 执行一轮自动对账任务。
     *
     * @return 任务执行结果
     */
    @Transactional
    public AccountReconcileTaskResponse runOnce() {
        String taskBatchNo = buildTaskBatchNo();
        try {
            List<AssetTarget> assetTargets = collectAssetTargets();
            if (assetTargets.isEmpty()) {
                taskAuditLogAppService.recordSkipped(
                        ACCOUNT_RECONCILE_TASK,
                        taskBatchNo,
                        "当前没有需要自动对账的资产目标",
                        "本轮自动对账没有扫描到业务资产"
                );
                return new AccountReconcileTaskResponse(
                        ACCOUNT_RECONCILE_TASK,
                        taskBatchNo,
                        0,
                        0,
                        0,
                        false,
                        "当前没有需要自动对账的资产目标"
                );
            }

            int consistentCount = 0;
            int inconsistentCount = 0;
            for (AssetTarget assetTarget : assetTargets) {
                AccountAssetReconcileResponse response = accountReconcileAppService.reconcile(
                        assetTarget.userId(),
                        assetTarget.chain(),
                        assetTarget.tokenSymbol()
                );
                AccountReconcileResult result = AccountReconcileResult.fromResponse(
                        ACCOUNT_RECONCILE_TASK,
                        taskBatchNo,
                        response
                );
                accountReconcileResultMapper.insert(result);

                if (Boolean.TRUE.equals(response.getConsistent())) {
                    consistentCount++;
                } else {
                    inconsistentCount++;
                    log.warn("自动对账发现资产不一致，batchNo={}, userId={}, chain={}, tokenSymbol={}, reason={}",
                            taskBatchNo,
                            response.getUserId(),
                            response.getChain(),
                            response.getTokenSymbol(),
                            response.getMismatchReason());
                }
            }

            String remark = inconsistentCount > 0
                    ? "已完成自动对账，本轮发现 " + inconsistentCount + " 个不一致资产"
                    : "已完成自动对账，本轮资产全部一致";
            log.info("自动对账任务完成，batchNo={}, processedAssetCount={}, inconsistentCount={}",
                    taskBatchNo, assetTargets.size(), inconsistentCount);
            taskAuditLogAppService.recordSuccess(
                    ACCOUNT_RECONCILE_TASK,
                    taskBatchNo,
                    assetTargets.size(),
                    consistentCount,
                    inconsistentCount,
                    0,
                    buildMetricSnapshot(consistentCount, inconsistentCount),
                    remark
            );
            return new AccountReconcileTaskResponse(
                    ACCOUNT_RECONCILE_TASK,
                    taskBatchNo,
                    assetTargets.size(),
                    consistentCount,
                    inconsistentCount,
                    true,
                    remark
            );
        } catch (RuntimeException ex) {
            taskAuditLogAppService.recordFailure(
                    ACCOUNT_RECONCILE_TASK,
                    taskBatchNo,
                    0,
                    0,
                    0,
                    1,
                    "",
                    resolveFailureReason(ex),
                    "自动对账任务执行失败"
            );
            throw ex;
        }
    }

    /**
     * 汇总本轮需要纳入自动对账的资产目标。
     *
     * <p>Day27 第一版采用“业务痕迹并集”的策略，把余额主表、流水、正式入账充值、提现订单里的
     * `userId + chain + tokenSymbol` 聚合起来，避免只扫 `account_balance` 导致遗漏历史资产。</p>
     *
     * @return 去重后的资产目标集合
     */
    private List<AssetTarget> collectAssetTargets() {
        Set<AssetTarget> assetTargets = new LinkedHashSet<>();

        List<AccountBalance> accountBalances = accountBalanceMapper.selectList(
                Wrappers.<AccountBalance>lambdaQuery().orderByAsc(AccountBalance::getId)
        );
        for (AccountBalance accountBalance : accountBalances) {
            assetTargets.add(new AssetTarget(
                    accountBalance.getUserId(),
                    accountBalance.getChain(),
                    accountBalance.getTokenSymbol()
            ));
        }

        List<AccountBill> accountBills = accountBillMapper.selectList(
                Wrappers.<AccountBill>lambdaQuery().orderByAsc(AccountBill::getId)
        );
        for (AccountBill accountBill : accountBills) {
            assetTargets.add(new AssetTarget(
                    accountBill.getUserId(),
                    accountBill.getChain(),
                    accountBill.getTokenSymbol()
            ));
        }

        List<DepositRecord> creditedDeposits = depositRecordMapper.selectList(
                Wrappers.<DepositRecord>lambdaQuery()
                        .eq(DepositRecord::getStatus, DepositStatusConstants.CREDITED)
                        .orderByAsc(DepositRecord::getId)
        );
        for (DepositRecord depositRecord : creditedDeposits) {
            assetTargets.add(new AssetTarget(
                    depositRecord.getUserId(),
                    depositRecord.getChain(),
                    depositRecord.getTokenSymbol()
            ));
        }

        List<WithdrawOrder> withdrawOrders = withdrawOrderMapper.selectList(
                Wrappers.<WithdrawOrder>lambdaQuery().orderByAsc(WithdrawOrder::getId)
        );
        for (WithdrawOrder withdrawOrder : withdrawOrders) {
            assetTargets.add(new AssetTarget(
                    withdrawOrder.getUserId(),
                    withdrawOrder.getChain(),
                    withdrawOrder.getTokenSymbol()
            ));
        }

        List<AssetTarget> orderedTargets = new ArrayList<>(assetTargets);
        orderedTargets.sort(Comparator
                .comparing(AssetTarget::userId)
                .thenComparing(AssetTarget::chain)
                .thenComparing(AssetTarget::tokenSymbol));
        return orderedTargets;
    }

    /**
     * 生成当前任务批次号。
     *
     * @return 任务批次号
     */
    private String buildTaskBatchNo() {
        return ACCOUNT_RECONCILE_TASK + "-" + LocalDateTime.now().format(TASK_BATCH_NO_FORMATTER);
    }

    /**
     * 构建自动对账指标快照。
     *
     * @param consistentCount 一致资产数量
     * @param inconsistentCount 不一致资产数量
     * @return 指标快照文本
     */
    private String buildMetricSnapshot(int consistentCount, int inconsistentCount) {
        return "consistentCount=" + consistentCount + ",inconsistentCount=" + inconsistentCount;
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

    /**
     * 自动对账扫描目标。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     */
    private record AssetTarget(Long userId, String chain, String tokenSymbol) {
    }
}
