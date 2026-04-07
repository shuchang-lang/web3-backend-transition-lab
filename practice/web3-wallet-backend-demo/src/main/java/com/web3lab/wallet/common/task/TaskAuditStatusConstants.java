package com.web3lab.wallet.common.task;

/**
 * 任务审计状态常量定义。
 */
public final class TaskAuditStatusConstants {

    /**
     * 任务成功执行完成。
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * 任务被跳过，没有真正执行主体逻辑。
     */
    public static final String SKIPPED = "SKIPPED";

    /**
     * 任务执行过程中发生失败。
     */
    public static final String FAILED = "FAILED";

    private TaskAuditStatusConstants() {
    }
}
