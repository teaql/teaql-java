package io.teaql.core.context;

/**
 * Cache operations context interface.
 * Provides local and remote cache capabilities.
 */
public interface CacheContext {
    
    // ==========================================
    // Remote Cache (distributed, shared across nodes)
    // ==========================================
    default void putToRemoteCache(String key, Object value) {
        putToRemoteCache(key, value, 0);
    }
    
    void putToRemoteCache(String key, Object value, int timeToLiveInSeconds);
    
    <T> T getFromRemoteCache(String key, Class<T> clazz);
    
    void removeFromRemoteCache(String key);
    
    // ==========================================
    // Local Cache (in-memory)
    // ==========================================
    default void putToLocalCache(String key, Object value) {
        putToLocalCache(key, value, 0);
    }
    
    void putToLocalCache(String key, Object value, int timeToLiveInSeconds);
    
    <T> T getFromLocalCache(String key, Class<T> clazz);
    
    void removeFromLocalCache(String key);
}
