package com.web3lab.wallet.application.withdraw;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.common.exception.BusinessException;
import com.web3lab.wallet.common.exception.ResourceNotFoundException;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.ApproveWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.CreateWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.RejectWithdrawOrderRequest;
import com.web3lab.wallet.controller.dto.WithdrawOrderResponse;
import com.web3lab.wallet.domain.account.AccountBalance;
import com.web3lab.wallet.domain.account.AccountBill;
import com.web3lab.wallet.domain.withdraw.WithdrawOrder;
import com.web3lab.wallet.domain.withdraw.WithdrawReviewStatusConstants;
import com.web3lab.wallet.infrastructure.persistence.AccountBalanceMapper;
import com.web3lab.wallet.infrastructure.persistence.AccountBillMapper;
import com.web3lab.wallet.infrastructure.persistence.WithdrawOrderMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 提现订单应用服务。
 *
 * <p>当前阶段先完成“用户申请提现 -> 校验余额 -> 冻结金额 -> 生成待审核订单”
 * 这条最小出金起点，为后续审核、广播和失败回退预留好状态承接层。</p>
 */
@Service
public class WithdrawOrderAppService {

    private final WithdrawOrderMapper withdrawOrderMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountBillMapper accountBillMapper;
    private final WalletDefaultsProperties walletDefaultsProperties;

    public WithdrawOrderAppService(WithdrawOrderMapper withdrawOrderMapper,
                                   AccountBalanceMapper accountBalanceMapper,
                                   AccountBillMapper accountBillMapper,
                                   WalletDefaultsProperties walletDefaultsProperties) {
        this.withdrawOrderMapper = withdrawOrderMapper;
        this.accountBalanceMapper = accountBalanceMapper;
        this.accountBillMapper = accountBillMapper;
        this.walletDefaultsProperties = walletDefaultsProperties;
    }

    /**
     * 创建提现申请，并同步冻结对应余额。
     *
     * <p>当前最小版本假设手续费和提现资产属于同一币种，因此冻结金额使用 `amount + fee`。
     * 真实生产系统里，ERC-20 代币和 gas 币种可能并不一致，后续再进一步拆开。</p>
     *
     * @param request 创建提现订单请求
     * @return 提现订单响应
     */
    @Transactional
    public WithdrawOrderResponse apply(CreateWithdrawOrderRequest request) {
        String chain = resolveOrDefault(request.getChain(), walletDefaultsProperties.chain());
        String tokenSymbol = resolveOrDefault(request.getTokenSymbol(), walletDefaultsProperties.tokenSymbol());

        WithdrawOrder existing = findByRequestNo(request.getRequestNo());
        if (existing != null) {
            if (!Objects.equals(existing.getUserId(), request.getUserId())) {
                throw new BusinessException("requestNo 已存在，请更换后重试");
            }
            return toResponse(existing);
        }

        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, request.getUserId())
                .eq(AccountBalance::getChain, chain)
                .eq(AccountBalance::getTokenSymbol, tokenSymbol)
                .last("LIMIT 1"));
        if (accountBalance == null) {
            throw new BusinessException("未找到可用余额记录，无法发起提现申请");
        }

        BigDecimal totalFreezeAmount = request.getAmount().add(request.getFee());
        if (accountBalance.getAvailableBalance().compareTo(totalFreezeAmount) < 0) {
            throw new BusinessException("可用余额不足，无法发起提现申请");
        }

        accountBalance.freezeAvailable(totalFreezeAmount);
        accountBalanceMapper.updateById(accountBalance);

        WithdrawOrder withdrawOrder = WithdrawOrder.createPendingReview(
                request.getUserId(),
                chain,
                tokenSymbol,
                request.getToAddress(),
                request.getAmount(),
                request.getFee(),
                request.getRequestNo()
        );
        withdrawOrderMapper.insert(withdrawOrder);

        // 提现申请阶段先冻结余额，真正扣减和链上广播由后续审核通过流程继续推进。
        accountBillMapper.insert(AccountBill.createWithdrawFreeze(
                request.getUserId(),
                chain,
                tokenSymbol,
                request.getRequestNo(),
                totalFreezeAmount.negate(),
                accountBalance.getAvailableBalance(),
                accountBalance.getFrozenBalance(),
                "提现申请冻结 amount + fee"
        ));
        return toResponse(withdrawOrder);
    }

    /**
     * 查询指定用户的提现订单列表。
     *
     * @param userId 平台用户 ID
     * @return 提现订单列表
     */
    public List<WithdrawOrderResponse> listByUserId(Long userId) {
        return withdrawOrderMapper.selectList(Wrappers.<WithdrawOrder>lambdaQuery()
                        .eq(WithdrawOrder::getUserId, userId)
                        .orderByDesc(WithdrawOrder::getId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按请求号查询提现订单。
     *
     * @param requestNo 客户端请求号
     * @return 提现订单响应
     */
    public WithdrawOrderResponse getByRequestNo(String requestNo) {
        WithdrawOrder withdrawOrder = findByRequestNo(requestNo);
        if (withdrawOrder == null) {
            throw new ResourceNotFoundException("未找到对应的提现订单");
        }
        return toResponse(withdrawOrder);
    }

    /**
     * 审核通过提现订单。
     *
     * <p>当前阶段审核通过后只把订单推进到“待广播”状态，
     * 冻结余额继续保留，等待后续真正广播链上交易时再扣减。</p>
     *
     * @param request 审核通过请求
     * @return 提现订单响应
     */
    @Transactional
    public WithdrawOrderResponse approve(ApproveWithdrawOrderRequest request) {
        WithdrawOrder withdrawOrder = requireByRequestNo(request.getRequestNo());
        if (WithdrawReviewStatusConstants.APPROVED.equals(withdrawOrder.getReviewStatus())) {
            return toResponse(withdrawOrder);
        }
        if (!WithdrawReviewStatusConstants.PENDING.equals(withdrawOrder.getReviewStatus())) {
            throw new BusinessException("当前提现订单状态不支持审核通过");
        }

        withdrawOrder.approve(request.getReviewBy());
        withdrawOrderMapper.updateById(withdrawOrder);
        return toResponse(withdrawOrder);
    }

    /**
     * 审核拒绝提现订单，并回退冻结余额。
     *
     * <p>审核拒绝的核心不是只改一条订单状态，而是要把此前申请阶段冻结的 `amount + fee`
     * 原路退回到可用余额，否则账面资金会一直处于错误冻结状态。</p>
     *
     * @param request 审核拒绝请求
     * @return 提现订单响应
     */
    @Transactional
    public WithdrawOrderResponse reject(RejectWithdrawOrderRequest request) {
        WithdrawOrder withdrawOrder = requireByRequestNo(request.getRequestNo());
        if (WithdrawReviewStatusConstants.REJECTED.equals(withdrawOrder.getReviewStatus())) {
            return toResponse(withdrawOrder);
        }
        if (!WithdrawReviewStatusConstants.PENDING.equals(withdrawOrder.getReviewStatus())) {
            throw new BusinessException("当前提现订单状态不支持审核拒绝");
        }

        AccountBalance accountBalance = accountBalanceMapper.selectOne(Wrappers.<AccountBalance>lambdaQuery()
                .eq(AccountBalance::getUserId, withdrawOrder.getUserId())
                .eq(AccountBalance::getChain, withdrawOrder.getChain())
                .eq(AccountBalance::getTokenSymbol, withdrawOrder.getTokenSymbol())
                .last("LIMIT 1"));
        if (accountBalance == null) {
            throw new BusinessException("未找到对应余额记录，无法执行审核拒绝回退");
        }

        BigDecimal totalUnfreezeAmount = withdrawOrder.getAmount().add(withdrawOrder.getFee());
        if (accountBalance.getFrozenBalance().compareTo(totalUnfreezeAmount) < 0) {
            throw new BusinessException("冻结余额不足，无法执行审核拒绝回退");
        }

        accountBalance.unfreezeToAvailable(totalUnfreezeAmount);
        accountBalanceMapper.updateById(accountBalance);

        accountBillMapper.insert(AccountBill.createWithdrawUnfreeze(
                withdrawOrder.getUserId(),
                withdrawOrder.getChain(),
                withdrawOrder.getTokenSymbol(),
                withdrawOrder.getRequestNo(),
                totalUnfreezeAmount,
                accountBalance.getAvailableBalance(),
                accountBalance.getFrozenBalance(),
                "提现审核拒绝，解冻 amount + fee"
        ));

        withdrawOrder.reject(request.getReviewBy(), request.getRejectReason());
        withdrawOrderMapper.updateById(withdrawOrder);
        return toResponse(withdrawOrder);
    }

    /**
     * 按请求号查询提现订单实体。
     *
     * @param requestNo 客户端请求号
     * @return 提现订单实体，不存在时返回 null
     */
    private WithdrawOrder findByRequestNo(String requestNo) {
        return withdrawOrderMapper.selectOne(Wrappers.<WithdrawOrder>lambdaQuery()
                .eq(WithdrawOrder::getRequestNo, requestNo)
                .last("LIMIT 1"));
    }

    /**
     * 按请求号查询提现订单，不存在时抛出异常。
     *
     * @param requestNo 客户端请求号
     * @return 提现订单实体
     */
    private WithdrawOrder requireByRequestNo(String requestNo) {
        WithdrawOrder withdrawOrder = findByRequestNo(requestNo);
        if (withdrawOrder == null) {
            throw new ResourceNotFoundException("未找到对应的提现订单");
        }
        return withdrawOrder;
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

    /**
     * 将提现订单实体转换为对外响应。
     *
     * @param withdrawOrder 提现订单实体
     * @return 提现订单响应
     */
    private WithdrawOrderResponse toResponse(WithdrawOrder withdrawOrder) {
        return new WithdrawOrderResponse(
                withdrawOrder.getId(),
                withdrawOrder.getUserId(),
                withdrawOrder.getChain(),
                withdrawOrder.getTokenSymbol(),
                withdrawOrder.getToAddress(),
                withdrawOrder.getAmount(),
                withdrawOrder.getFee(),
                withdrawOrder.getRequestNo(),
                withdrawOrder.getStatus(),
                withdrawOrder.getReviewStatus(),
                withdrawOrder.getReviewBy(),
                withdrawOrder.getReviewTime(),
                withdrawOrder.getTxHash(),
                withdrawOrder.getNonce(),
                withdrawOrder.getBroadcastRetryCount(),
                withdrawOrder.getReceiptCheckRetryCount(),
                withdrawOrder.getFailReason(),
                withdrawOrder.getCreatedAt()
        );
    }
}
