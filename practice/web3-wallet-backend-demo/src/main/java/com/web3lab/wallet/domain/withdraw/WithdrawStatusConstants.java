package com.web3lab.wallet.domain.withdraw;

/**
 * 提现订单状态常量定义。
 */
public final class WithdrawStatusConstants {

    /**
     * 待审核状态。
     */
    public static final String PENDING_REVIEW = "PENDING_REVIEW";

    /**
     * 审核通过、待广播状态。
     */
    public static final String PENDING_BROADCAST = "PENDING_BROADCAST";

    /**
     * 已提交链上广播，等待回执状态。
     */
    public static final String BROADCAST_SUBMITTED = "BROADCAST_SUBMITTED";

    /**
     * 审核拒绝状态。
     */
    public static final String REJECTED = "REJECTED";

    /**
     * 链上执行成功状态。
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * 链上执行失败状态。
     */
    public static final String FAILED = "FAILED";

    /**
     * 需要人工处理状态。
     */
    public static final String MANUAL_HANDLE_REQUIRED = "MANUAL_HANDLE_REQUIRED";

    private WithdrawStatusConstants() {
    }
}
