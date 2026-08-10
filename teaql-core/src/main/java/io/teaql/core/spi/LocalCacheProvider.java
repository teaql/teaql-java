package io.teaql.core.spi;

import io.teaql.core.utils.Cache;

public interface LocalCacheProvider {
    /**
     * 获取指定命名空间的本地内存缓存实例
     * @param namespace 命名空间，用于隔离不同的缓存数据
     * @return 本地缓存实例
     * @throws CacheException
     */
    <K, V> Cache<K, V> getCache(String namespace) throws CacheException;
}
