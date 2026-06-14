package io.teaql.core.utils;

public interface Cache<K, V> {
    void put(K key, V value);
    void put(K key, V value, long timeout);
    V get(K key);
    V get(K key, boolean isUpdate);
    V get(K key, java.util.function.Supplier<? extends V> supplier);
    void remove(K key);
    boolean containsKey(K key);
}
