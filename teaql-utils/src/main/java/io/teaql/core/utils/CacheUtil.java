package io.teaql.core.utils;

public class CacheUtil {
    public static <K, V> TimedCache<K, V> newTimedCache(long timeout) {
        return new TimedCache<>(timeout);
    }

    public static <K, V> LRUCache<K, V> newLRUCache(int capacity, long timeout) {
        return new LRUCache<>(capacity, timeout);
    }
}
