package io.teaql.core.spi;

/**
 * Provider interface for remote cache implementations.
 */

public interface RemoteCacheProvider {
    /**
     * Get the remote distributed cache instance for the specified namespace.
     * @param namespace the namespace for isolating different cache data
     * @return the remote cache instance
     * @throws CacheException if cache retrieval fails
     */
    <K, V> RemoteCache<K, V> getCache(String namespace) throws CacheException;
}
