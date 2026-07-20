package io.teaql.core.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimedCache<K, V> implements Cache<K, V> {
    private final long timeout;
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>();

    public TimedCache(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public synchronized void put(K key, V value) {
        put(key, value, timeout);
    }

    @Override
    public synchronized void put(K key, V value, long timeout) {
        if (key != null && value != null) {
            removeExpired();
            entries.put(key, new Entry<>(value, expireAt(timeout)));
        }
    }

    @Override
    public synchronized V get(K key) {
        if (key == null) {
            return null;
        }
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            entries.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public V get(K key, boolean isUpdate) {
        return get(key);
    }

    @Override
    public synchronized V get(K key, java.util.function.Supplier<? extends V> supplier) {
        if (key == null) {
            return null;
        }
        if (supplier == null) {
            throw new RuntimeException("Supplier is null");
        }
        V val = get(key);
        if (val == null) {
            val = supplier.get();
            if (val != null) {
                put(key, val);
            }
        }
        return val;
    }

    @Override
    public synchronized void remove(K key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    @Override
    public synchronized boolean containsKey(K key) {
        return get(key) != null;
    }

    private long expireAt(long timeout) {
        return timeout > 0 ? System.currentTimeMillis() + timeout : 0;
    }

    private void removeExpired() {
        Iterator<Map.Entry<K, Entry<V>>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    private static final class Entry<V> {
        private final V value;
        private final long expireAt;

        private Entry(V value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        private boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() >= expireAt;
        }
    }
}
