package io.teaql.core.utils;

import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

public class LRUCache<K, V> implements Cache<K, V> {
    private final com.google.common.cache.Cache<K, V> delegate;

    public LRUCache(int capacity, long timeout) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        if (capacity > 0) {
            builder.maximumSize(capacity);
        } else if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (timeout > 0) {
            builder.expireAfterWrite(timeout, TimeUnit.MILLISECONDS);
        }
        this.delegate = builder.build();
    }

    @Override
    public void put(K key, V value) {
        if (key != null && value != null) {
            delegate.put(key, value);
        }
    }

    @Override
    public void put(K key, V value, long timeout) {
        put(key, value);
    }

    @Override
    public V get(K key) {
        return key != null ? delegate.getIfPresent(key) : null;
    }

    @Override
    public V get(K key, boolean isUpdate) {
        return get(key);
    }

    @Override
    public V get(K key, java.util.function.Supplier<? extends V> supplier) {
        if (key == null) {
            return null;
        }
        if (supplier == null) {
            throw new RuntimeException("Supplier is null");
        }
        V val = delegate.getIfPresent(key);
        if (val == null) {
            val = supplier.get();
            if (val != null) {
                delegate.put(key, val);
            }
        }
        return val;
    }

    @Override
    public void remove(K key) {
        if (key != null) {
            delegate.invalidate(key);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return key != null && delegate.getIfPresent(key) != null;
    }
}
