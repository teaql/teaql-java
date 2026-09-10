package io.teaql.core.context;

/**
 * Cache operations context interface.
 * Provides local and remote cache capabilities.
 */
public interface CacheContext {
    
    // ==========================================
    // Remote Cache (分布式级，跨节点共享)
    // ==========================================
    default void putToRemoteCache(String key, Object value) {
        putToRemoteCache(key, value, 0);
    }
    
    void putToRemoteCache(String key, Object value, int timeToLiveInSeconds);
    
    <T> T getFromRemoteCache(String key, Class<T> clazz);
    
    void removeFromRemoteCache(String key);
    
    // ==========================================
    // Local Cache (本地缓存)
    // ==========================================
    default void putToLocalCache(String key, Object value) {
        putToLocalCache(key, value, 0);
    }
    
    void putToLocalCache(String key, Object value, int timeToLiveInSeconds);
    
    <T> T getFromLocalCache(String key, Class<T> clazz);
    
    void removeFromLocalCache(String key);
}
