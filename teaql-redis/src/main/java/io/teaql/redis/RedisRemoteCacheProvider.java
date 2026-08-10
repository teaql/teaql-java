package io.teaql.redis;

import io.teaql.core.spi.RemoteCacheProvider;
import io.teaql.core.spi.CacheException;
import io.teaql.core.utils.RemoteCache;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RedisRemoteCacheProvider implements RemoteCacheProvider {
    @Override
    public <K, V> RemoteCache<K, V> getCache(String namespace) throws CacheException {
        RedisRemoteCache<K, V> cache = new RedisRemoteCache<>();
        // TODO: 从配置中心或上下文中注入真正的 JedisPool 和类型信息
        cache.init(null, namespace, new ObjectMapper(), null);
        return cache;
    }
}
