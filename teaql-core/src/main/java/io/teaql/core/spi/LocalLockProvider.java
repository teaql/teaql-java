package io.teaql.core.spi;

import java.util.concurrent.locks.Lock;

public interface LocalLockProvider {
    /**
     * 获取指定的本地锁实例
     * @param name 锁名称
     * @return 本地锁实例
     * @throws LockException
     */
    Lock getLock(String name) throws LockException;

    default boolean tryLock(String namespace, String key, long timeoutMillis, long expireMillis)
            throws LockException {
        try {
            return getLock(namespace + ":" + key)
                    .tryLock(Math.max(timeoutMillis, 0), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    default boolean tryLock(String namespace, String key, Object owner,
            long timeoutMillis, long expireMillis) throws LockException {
        return tryLock(namespace, key, timeoutMillis, expireMillis);
    }

    default void unlock(String namespace, String key) throws LockException {
        getLock(namespace + ":" + key).unlock();
    }

    default void unlock(String namespace, String key, Object owner) throws LockException {
        unlock(namespace, key);
    }
}
