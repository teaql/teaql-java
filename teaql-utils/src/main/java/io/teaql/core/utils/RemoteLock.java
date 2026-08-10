package io.teaql.core.utils;

/**
 * 远程分布式锁接口 (Remote Lock Interface)
 * 用于跨节点加锁解锁（如基于 Redis 的分布式锁）。
 */
public interface RemoteLock {
    /**
     * 尝试获取锁
     * @param key 锁标识
     * @param timeoutMillis 等待超时时间(毫秒)
     * @param expireMillis 锁的自动过期时间(毫秒)
     * @return true 如果获取成功, 否则 false
     */
    boolean tryLock(String key, long timeoutMillis, long expireMillis);

    /**
     * 释放锁
     * @param key 锁标识
     */
    void unlock(String key);
}
