package io.teaql.core.context;

/**
 * Lock operations context interface.
 * Provides local and remote distributed lock capabilities.
 */
public interface LockContext {
    
    // ==========================================
    // Remote Lock (分布式锁)
    // ==========================================
    boolean tryRemoteLock(String key, long timeoutMillis, long expireMillis);
    
    void unlockRemote(String key);
    
    // ==========================================
    // Local Lock (本地锁)
    // ==========================================
    boolean tryLocalLock(String key, long timeoutMillis, long expireMillis);
    
    void unlockLocal(String key);
}
