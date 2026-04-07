package com.web3lab.wallet.domain.account;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 账户余额实体，对应平台内部账本里的余额主表。
 */
@Data
@TableName("account_balance")
public class AccountBalance {

    /**
     * 余额记录主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台用户 ID。
     */
    private Long userId;

    /**
     * 链编码，例如 ETH_SEPOLIA。
     */
    private String chain;

    /**
     * 币种符号，例如 USDT。
     */
    private String tokenSymbol;

    /**
     * 可用余额。
     */
    private BigDecimal availableBalance;

    /**
     * 冻结余额。
     */
    private BigDecimal frozenBalance;

    /**
     * 记录创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建默认零余额账户。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @return 零余额账户
     */
    public static AccountBalance createZero(Long userId, String chain, String tokenSymbol) {
        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setUserId(userId);
        accountBalance.setChain(chain);
        accountBalance.setTokenSymbol(tokenSymbol);
        accountBalance.setAvailableBalance(BigDecimal.ZERO);
        accountBalance.setFrozenBalance(BigDecimal.ZERO);
        return accountBalance;
    }

    /**
     * 增加可用余额。
     *
     * <p>当前最小钱包后端只处理充值入账，因此这里先把增加可用余额的业务语义收敛到实体内部，
     * 避免应用层直接散落金额加法。</p>
     *
     * @param amount 本次增加金额
     */
    public void creditAvailable(BigDecimal amount) {
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        availableBalance = availableBalance.add(amount);
    }

    /**
     * 扣减可用余额。
     *
     * <p>Day23 的充值冲正第一版允许这里出现负数可用余额，
     * 用来表达“这笔原本已入账的资金已经被用户消耗，当前账户形成待追偿风险敞口”的场景。</p>
     *
     * @param amount 本次扣减金额
     */
    public void deductAvailable(BigDecimal amount) {
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        availableBalance = availableBalance.subtract(amount);
    }

    /**
     * 将可用余额转入冻结余额。
     *
     * <p>当前最小提现版本采用“申请即冻结”的做法，先把 `amount + fee` 从可用余额转入冻结余额，
     * 后续审核通过、广播成功或失败回退时，再继续推进相应状态。</p>
     *
     * @param amount 本次冻结金额
     */
    public void freezeAvailable(BigDecimal amount) {
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        if (frozenBalance == null) {
            frozenBalance = BigDecimal.ZERO;
        }
        availableBalance = availableBalance.subtract(amount);
        frozenBalance = frozenBalance.add(amount);
    }

    /**
     * 将冻结余额退回可用余额。
     *
     * <p>当前最小提现回退版本用于“审核拒绝 -> 解冻回退”场景，
     * 先把此前冻结的 `amount + fee` 原路退回到可用余额。</p>
     *
     * @param amount 本次解冻金额
     */
    public void unfreezeToAvailable(BigDecimal amount) {
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        if (frozenBalance == null) {
            frozenBalance = BigDecimal.ZERO;
        }
        availableBalance = availableBalance.add(amount);
        frozenBalance = frozenBalance.subtract(amount);
    }

    /**
     * 从冻结余额中正式扣减金额。
     *
     * <p>当前最小提现执行版本里，链上广播成功并拿到成功回执后，
     * 会把此前冻结的 `amount + fee` 从冻结余额中真正扣掉。</p>
     *
     * @param amount 本次扣减金额
     */
    public void deductFrozen(BigDecimal amount) {
        if (frozenBalance == null) {
            frozenBalance = BigDecimal.ZERO;
        }
        frozenBalance = frozenBalance.subtract(amount);
    }
}
