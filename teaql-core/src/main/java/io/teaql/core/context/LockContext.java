package io.teaql.core.context;

/**
 * Lock operations context interface.
 * Provides local and remote distributed lock capabilities.
 */
public interface LockContext {
    
    // ==========================================
    // Remote Lock (distributed)
    // ==========================================
    boolean tryRemoteLock(String key, long timeoutMillis, long expireMillis);
    
    void unlockRemote(String key);
    
    // ==========================================
    // Local Lock (in-memory)
    // ==========================================
    boolean tryLocalLock(String key, long timeoutMillis, long expireMillis);
    
    void unlockLocal(String key);
}
