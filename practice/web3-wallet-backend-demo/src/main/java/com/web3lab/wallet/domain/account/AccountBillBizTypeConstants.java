package com.web3lab.wallet.domain.account;

/**
 * 账户流水业务类型常量定义。
 */
public final class AccountBillBizTypeConstants {

    /**
     * 充值正式入账。
     */
    public static final String DEPOSIT_CREDIT = "DEPOSIT_CREDIT";

    /**
     * 充值链重组冲正扣减。
     */
    public static final String DEPOSIT_REORG_DEDUCT = "DEPOSIT_REORG_DEDUCT";

    /**
     * 提现申请冻结。
     */
    public static final String WITHDRAW_FREEZE = "WITHDRAW_FREEZE";

    /**
     * 提现审核拒绝解冻。
     */
    public static final String WITHDRAW_UNFREEZE = "WITHDRAW_UNFREEZE";

    /**
     * 提现链上成功扣减。
     */
    public static final String WITHDRAW_DEDUCT = "WITHDRAW_DEDUCT";

    private AccountBillBizTypeConstants() {
    }
}
