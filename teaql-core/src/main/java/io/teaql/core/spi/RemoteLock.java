package io.teaql.core.spi;

/**
 * Remote Lock Interface
 * Provides cross-node lock/unlock capabilities (e.g., Redis-based distributed lock).
 */
public interface RemoteLock {
    /**
     * Try to acquire the lock.
     * @param key the lock identifier
     * @param timeoutMillis the wait timeout in milliseconds
     * @param expireMillis the lock auto-expiration time in milliseconds
     * @return true if the lock was acquired, false otherwise
     */
    boolean tryLock(String key, long timeoutMillis, long expireMillis);

    /**
     * Release the lock.
     * @param key the lock identifier
     */
    void unlock(String key);
}
