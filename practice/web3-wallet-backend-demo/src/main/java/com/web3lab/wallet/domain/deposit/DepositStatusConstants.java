package com.web3lab.wallet.domain.deposit;

/**
 * 充值状态常量定义。
 */
public final class DepositStatusConstants {

    /**
     * 待确认状态。
     */
    public static final String PENDING_CONFIRM = "PENDING_CONFIRM";

    /**
     * 已入账状态。
     */
    public static final String CREDITED = "CREDITED";

    /**
     * 疑似发生链重组状态。
     */
    public static final String REORG_SUSPECTED = "REORG_SUSPECTED";

    /**
     * 链重组充值已完成冲正状态。
     */
    public static final String REORG_COMPENSATED = "REORG_COMPENSATED";

    /**
     * 已忽略状态。
     */
    public static final String IGNORED = "IGNORED";

    private DepositStatusConstants() {
    }
}
