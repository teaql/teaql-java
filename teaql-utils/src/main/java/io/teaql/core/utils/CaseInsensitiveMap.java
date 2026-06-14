package io.teaql.core.utils;

import java.util.HashMap;
import java.util.Map;

public class CaseInsensitiveMap<K, V> extends HashMap<K, V> {
    private final org.apache.commons.collections4.map.CaseInsensitiveMap<K, V> delegate;

    public CaseInsensitiveMap() {
        this.delegate = new org.apache.commons.collections4.map.CaseInsensitiveMap<>();
    }

    public CaseInsensitiveMap(Map<? extends K, ? extends V> m) {
        this.delegate = new org.apache.commons.collections4.map.CaseInsensitiveMap<>(m);
    }

    @Override
    public V get(Object key) {
        return delegate.get(key);
    }

    @Override
    public V put(K key, V value) {
        return delegate.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }

    @Override
    public V remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public java.util.Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public java.util.Collection<V> values() {
        return delegate.values();
    }

    @Override
    public java.util.Set<Map.Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }
}
