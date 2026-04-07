package com.web3lab.wallet.common.task;

/**
 * 任务失败分层常量定义。
 */
public final class TaskFailureLevelConstants {

    /**
     * 可自动重试层。
     */
    public static final String RETRYABLE = "RETRYABLE";

    /**
     * 需要人工处理层。
     */
    public static final String MANUAL_HANDLE_REQUIRED = "MANUAL_HANDLE_REQUIRED";

    private TaskFailureLevelConstants() {
    }
}
