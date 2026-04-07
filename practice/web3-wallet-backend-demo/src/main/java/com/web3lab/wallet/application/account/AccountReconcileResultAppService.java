package com.web3lab.wallet.application.account;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.controller.dto.AccountReconcileResultResponse;
import com.web3lab.wallet.domain.account.AccountReconcileResult;
import com.web3lab.wallet.infrastructure.persistence.AccountReconcileResultMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 自动对账结果查询应用服务。
 */
@Service
public class AccountReconcileResultAppService {

    private final AccountReconcileResultMapper accountReconcileResultMapper;

    public AccountReconcileResultAppService(AccountReconcileResultMapper accountReconcileResultMapper) {
        this.accountReconcileResultMapper = accountReconcileResultMapper;
    }

    /**
     * 查询指定用户最近一轮自动对账结果。
     *
     * <p>这里返回的是“该用户最近一次任务批次”的全部资产结果，
     * 避免不同资产分别取最新一条时混入不同批次的数据。</p>
     *
     * @param userId 平台用户 ID
     * @return 最近一轮自动对账结果
     */
    public List<AccountReconcileResultResponse> listLatestByUserId(Long userId) {
        AccountReconcileResult latest = accountReconcileResultMapper.selectOne(
                Wrappers.<AccountReconcileResult>lambdaQuery()
                        .eq(AccountReconcileResult::getUserId, userId)
                        .orderByDesc(AccountReconcileResult::getId)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            return List.of();
        }
        return toResponses(accountReconcileResultMapper.selectList(
                Wrappers.<AccountReconcileResult>lambdaQuery()
                        .eq(AccountReconcileResult::getUserId, userId)
                        .eq(AccountReconcileResult::getTaskBatchNo, latest.getTaskBatchNo())
                        .orderByAsc(AccountReconcileResult::getId)
        ));
    }

    /**
     * 查询指定批次号下的自动对账结果。
     *
     * @param taskBatchNo 任务批次号
     * @return 该批次下的全部资产结果
     */
    public List<AccountReconcileResultResponse> listByTaskBatchNo(String taskBatchNo) {
        return toResponses(accountReconcileResultMapper.selectList(
                Wrappers.<AccountReconcileResult>lambdaQuery()
                        .eq(AccountReconcileResult::getTaskBatchNo, taskBatchNo)
                        .orderByAsc(AccountReconcileResult::getId)
        ));
    }

    /**
     * 把持久化实体转换为对外响应。
     *
     * @param results 对账结果实体列表
     * @return 对账结果响应列表
     */
    private List<AccountReconcileResultResponse> toResponses(List<AccountReconcileResult> results) {
        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 转换单条自动对账结果响应。
     *
     * @param result 对账结果实体
     * @return 对账结果响应
     */
    private AccountReconcileResultResponse toResponse(AccountReconcileResult result) {
        AccountReconcileResultResponse response = new AccountReconcileResultResponse();
        response.setId(result.getId());
        response.setTaskName(result.getTaskName());
        response.setTaskBatchNo(result.getTaskBatchNo());
        response.setUserId(result.getUserId());
        response.setChain(result.getChain());
        response.setTokenSymbol(result.getTokenSymbol());
        response.setAvailableBalance(result.getAvailableBalance());
        response.setFrozenBalance(result.getFrozenBalance());
        response.setTotalBalance(result.getTotalBalance());
        response.setCreditedDepositAmount(result.getCreditedDepositAmount());
        response.setSuccessfulWithdrawAmount(result.getSuccessfulWithdrawAmount());
        response.setPendingWithdrawFrozenAmount(result.getPendingWithdrawFrozenAmount());
        response.setBillAssetDeltaAmount(result.getBillAssetDeltaAmount());
        response.setLatestBillAvailableAfter(result.getLatestBillAvailableAfter());
        response.setLatestBillFrozenAfter(result.getLatestBillFrozenAfter());
        response.setBillCount(result.getBillCount());
        response.setConsistentWithBusinessTables(result.getConsistentWithBusinessTables());
        response.setConsistentWithAssetDeltaBills(result.getConsistentWithAssetDeltaBills());
        response.setConsistentWithLatestBillSnapshot(result.getConsistentWithLatestBillSnapshot());
        response.setConsistentWithPendingWithdraws(result.getConsistentWithPendingWithdraws());
        response.setConsistent(result.getConsistent());
        response.setMismatchReason(result.getMismatchReason());
        response.setCreatedAt(result.getCreatedAt());
        response.setUpdatedAt(result.getUpdatedAt());
        return response;
    }
}
