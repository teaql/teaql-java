package io.teaql.core.spi;

import io.teaql.core.utils.Cache;
import io.teaql.core.utils.LRUCache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Process-local, thread-safe cache provider used when no custom provider is registered. */
public final class DefaultLocalCacheProvider implements LocalCacheProvider {
    public static final DefaultLocalCacheProvider INSTANCE = new DefaultLocalCacheProvider();

    private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    private DefaultLocalCacheProvider() {}

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String namespace) {
        return (Cache<K, V>) caches.computeIfAbsent(namespace, ignored -> new LRUCache<>(10_000, 0));
    }
}
