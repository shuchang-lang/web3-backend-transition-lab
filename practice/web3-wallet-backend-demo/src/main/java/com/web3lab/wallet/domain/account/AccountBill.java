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
 * 账户流水实体，对应平台内部账本里的资金变动明细。
 */
@Data
@TableName("account_bill")
public class AccountBill {

    /**
     * 流水主键。
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
     * 业务类型，当前阶段见 {@link AccountBillBizTypeConstants}。
     */
    private String bizType;

    /**
     * 业务主键，用于做流水幂等。
     */
    private String bizId;

    /**
     * 本次变动金额。
     */
    private BigDecimal changeAmount;

    /**
     * 变动后可用余额。
     */
    private BigDecimal availableAfter;

    /**
     * 变动后冻结余额。
     */
    private BigDecimal frozenAfter;

    /**
     * 业务备注。
     */
    private String remark;

    /**
     * 记录创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 创建充值入账流水。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param bizId 业务主键
     * @param changeAmount 本次变动金额
     * @param availableAfter 变动后可用余额
     * @param frozenAfter 变动后冻结余额
     * @param remark 业务备注
     * @return 账户流水实体
     */
    public static AccountBill createDepositCredit(Long userId, String chain, String tokenSymbol, String bizId,
                                                  BigDecimal changeAmount, BigDecimal availableAfter,
                                                  BigDecimal frozenAfter, String remark) {
        AccountBill accountBill = new AccountBill();
        accountBill.setUserId(userId);
        accountBill.setChain(chain);
        accountBill.setTokenSymbol(tokenSymbol);
        accountBill.setBizType(AccountBillBizTypeConstants.DEPOSIT_CREDIT);
        accountBill.setBizId(bizId);
        accountBill.setChangeAmount(changeAmount);
        accountBill.setAvailableAfter(availableAfter);
        accountBill.setFrozenAfter(frozenAfter);
        accountBill.setRemark(remark);
        return accountBill;
    }

    /**
     * 创建充值链重组冲正流水。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param bizId 业务主键
     * @param changeAmount 本次总资产变动金额，冲正时为负数
     * @param availableAfter 变动后可用余额
     * @param frozenAfter 变动后冻结余额
     * @param remark 业务备注
     * @return 账户流水实体
     */
    public static AccountBill createDepositReorgDeduct(Long userId, String chain, String tokenSymbol, String bizId,
                                                       BigDecimal changeAmount, BigDecimal availableAfter,
                                                       BigDecimal frozenAfter, String remark) {
        AccountBill accountBill = new AccountBill();
        accountBill.setUserId(userId);
        accountBill.setChain(chain);
        accountBill.setTokenSymbol(tokenSymbol);
        accountBill.setBizType(AccountBillBizTypeConstants.DEPOSIT_REORG_DEDUCT);
        accountBill.setBizId(bizId);
        accountBill.setChangeAmount(changeAmount);
        accountBill.setAvailableAfter(availableAfter);
        accountBill.setFrozenAfter(frozenAfter);
        accountBill.setRemark(remark);
        return accountBill;
    }

    /**
     * 创建提现冻结流水。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param bizId 业务主键
     * @param changeAmount 本次可用余额变动金额，冻结时为负数
     * @param availableAfter 变动后可用余额
     * @param frozenAfter 变动后冻结余额
     * @param remark 业务备注
     * @return 账户流水实体
     */
    public static AccountBill createWithdrawFreeze(Long userId, String chain, String tokenSymbol, String bizId,
                                                   BigDecimal changeAmount, BigDecimal availableAfter,
                                                   BigDecimal frozenAfter, String remark) {
        AccountBill accountBill = new AccountBill();
        accountBill.setUserId(userId);
        accountBill.setChain(chain);
        accountBill.setTokenSymbol(tokenSymbol);
        accountBill.setBizType(AccountBillBizTypeConstants.WITHDRAW_FREEZE);
        accountBill.setBizId(bizId);
        accountBill.setChangeAmount(changeAmount);
        accountBill.setAvailableAfter(availableAfter);
        accountBill.setFrozenAfter(frozenAfter);
        accountBill.setRemark(remark);
        return accountBill;
    }

    /**
     * 创建提现解冻流水。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param bizId 业务主键
     * @param changeAmount 本次可用余额变动金额，解冻时为正数
     * @param availableAfter 变动后可用余额
     * @param frozenAfter 变动后冻结余额
     * @param remark 业务备注
     * @return 账户流水实体
     */
    public static AccountBill createWithdrawUnfreeze(Long userId, String chain, String tokenSymbol, String bizId,
                                                     BigDecimal changeAmount, BigDecimal availableAfter,
                                                     BigDecimal frozenAfter, String remark) {
        AccountBill accountBill = new AccountBill();
        accountBill.setUserId(userId);
        accountBill.setChain(chain);
        accountBill.setTokenSymbol(tokenSymbol);
        accountBill.setBizType(AccountBillBizTypeConstants.WITHDRAW_UNFREEZE);
        accountBill.setBizId(bizId);
        accountBill.setChangeAmount(changeAmount);
        accountBill.setAvailableAfter(availableAfter);
        accountBill.setFrozenAfter(frozenAfter);
        accountBill.setRemark(remark);
        return accountBill;
    }

    /**
     * 创建提现成功扣减流水。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param bizId 业务主键
     * @param changeAmount 本次总资产变动金额，提现成功时为负数
     * @param availableAfter 变动后可用余额
     * @param frozenAfter 变动后冻结余额
     * @param remark 业务备注
     * @return 账户流水实体
     */
    public static AccountBill createWithdrawDeduct(Long userId, String chain, String tokenSymbol, String bizId,
                                                   BigDecimal changeAmount, BigDecimal availableAfter,
                                                   BigDecimal frozenAfter, String remark) {
        AccountBill accountBill = new AccountBill();
        accountBill.setUserId(userId);
        accountBill.setChain(chain);
        accountBill.setTokenSymbol(tokenSymbol);
        accountBill.setBizType(AccountBillBizTypeConstants.WITHDRAW_DEDUCT);
        accountBill.setBizId(bizId);
        accountBill.setChangeAmount(changeAmount);
        accountBill.setAvailableAfter(availableAfter);
        accountBill.setFrozenAfter(frozenAfter);
        accountBill.setRemark(remark);
        return accountBill;
    }
}
