package com.web3lab.wallet.common.exception;

/**
 * 业务异常。
 *
 * <p>用于表达参数格式正确但不满足业务约束的场景，例如余额不足、重复申请单号等。</p>
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
