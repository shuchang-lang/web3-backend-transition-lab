package com.web3lab.wallet.domain.withdraw;

/**
 * 提现审核状态常量定义。
 */
public final class WithdrawReviewStatusConstants {

    /**
     * 待审核状态。
     */
    public static final String PENDING = "PENDING";

    /**
     * 审核通过状态。
     */
    public static final String APPROVED = "APPROVED";

    /**
     * 审核拒绝状态。
     */
    public static final String REJECTED = "REJECTED";

    private WithdrawReviewStatusConstants() {
    }
}
