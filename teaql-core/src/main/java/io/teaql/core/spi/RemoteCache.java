package io.teaql.core.spi;

import io.teaql.core.utils.Cache;

/**
 * Remote Cache Interface
 * Extends the base Cache interface for distributed/remote cache implementations (e.g., Redis).
 */
public interface RemoteCache<K, V> extends Cache<K, V> {
    /**
     * Refresh the expiration time of the specified key in the remote cache.
     * @param key the cache key
     * @param timeout the timeout duration in milliseconds
     */
    void expire(K key, long timeout);
}
