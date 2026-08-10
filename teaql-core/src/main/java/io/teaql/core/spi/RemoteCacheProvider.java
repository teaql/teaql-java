package io.teaql.core.spi;

import io.teaql.core.utils.RemoteCache;

public interface RemoteCacheProvider {
    /**
     * 获取指定命名空间的远程分布式缓存实例
     * @param namespace 命名空间，用于隔离不同的缓存数据
     * @return 远程缓存实例
     * @throws CacheException
     */
    <K, V> RemoteCache<K, V> getCache(String namespace) throws CacheException;
}
