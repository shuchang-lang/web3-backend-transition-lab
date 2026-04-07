package com.web3lab.wallet.application.withdraw;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * 提现广播本地串行锁。
 *
 * <p>Day25 第一版先使用单 JVM 内的本地公平锁，
 * 避免同一实例内多个广播任务同时竞争热钱包 nonce。
 * 如果后续要支持多实例部署，再把这里升级成数据库锁或分布式锁。</p>
 */
@Component
public class WithdrawBroadcastLock {

    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * 尝试获取提现广播锁。
     *
     * @return true 表示本轮成功拿到锁
     */
    public boolean tryLock() {
        return lock.tryLock();
    }

    /**
     * 释放提现广播锁。
     */
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
