package com.web3lab.wallet.application.deposit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.common.exception.ResourceNotFoundException;
import com.web3lab.wallet.controller.dto.DepositRecordResponse;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 充值记录应用服务，负责提供充值记录查询能力。
 */
@Service
public class DepositRecordAppService {

    private final DepositRecordMapper depositRecordMapper;

    public DepositRecordAppService(DepositRecordMapper depositRecordMapper) {
        this.depositRecordMapper = depositRecordMapper;
    }

    /**
     * 查询指定用户的充值记录列表。
     *
     * @param userId 平台用户 ID
     * @return 充值记录列表
     */
    public List<DepositRecordResponse> listByUserId(Long userId) {
        return depositRecordMapper.selectList(Wrappers.<DepositRecord>lambdaQuery()
                        .eq(DepositRecord::getUserId, userId)
                        .orderByDesc(DepositRecord::getId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按交易哈希查询充值记录。
     *
     * @param txHash 链上交易哈希
     * @return 充值记录响应
     */
    public DepositRecordResponse getByTxHash(String txHash) {
        DepositRecord depositRecord = depositRecordMapper.selectOne(Wrappers.<DepositRecord>lambdaQuery()
                .eq(DepositRecord::getTxHash, txHash)
                .last("LIMIT 1"));
        if (depositRecord == null) {
            throw new ResourceNotFoundException("未找到对应的充值记录");
        }
        return toResponse(depositRecord);
    }

    /**
     * 将充值记录实体转换为对外响应。
     *
     * @param depositRecord 充值记录实体
     * @return 充值记录响应
     */
    private DepositRecordResponse toResponse(DepositRecord depositRecord) {
        return new DepositRecordResponse(
                depositRecord.getId(),
                depositRecord.getUserId(),
                depositRecord.getChain(),
                depositRecord.getTokenSymbol(),
                depositRecord.getTokenContract(),
                depositRecord.getFromAddress(),
                depositRecord.getToAddress(),
                depositRecord.getTxHash(),
                depositRecord.getLogIndex(),
                depositRecord.getAmount(),
                depositRecord.getBlockNumber(),
                depositRecord.getConfirmations(),
                depositRecord.getStatus(),
                depositRecord.getCreditedAt(),
                depositRecord.getCreatedAt()
        );
    }
}
