package io.teaql.core.utils;

/**
 * 远程缓存接口 (Remote Cache Interface)
 * 用于标记或扩展基础 Cache 接口，专用于分布式/远程缓存实现（如 Redis）。
 */
public interface RemoteCache<K, V> extends Cache<K, V> {
    /**
     * 刷新远程缓存中指定键的过期时间
     * @param key 键
     * @param timeout 超时时间
     */
    void expire(K key, long timeout);
}
