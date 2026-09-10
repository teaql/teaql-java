package io.teaql.core.spi;

import io.teaql.core.utils.Cache;

public interface LocalCacheProvider {
    /**
     * Get the local in-memory cache instance for the specified namespace.
     * @param namespace the namespace for isolating different cache data
     * @return the local cache instance
     * @throws CacheException if cache retrieval fails
     */
    <K, V> Cache<K, V> getCache(String namespace) throws CacheException;
}
