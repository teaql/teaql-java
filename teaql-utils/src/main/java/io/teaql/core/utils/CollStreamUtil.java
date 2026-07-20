package io.teaql.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CollStreamUtil {

    public static <E, T> java.util.List<T> toList(java.util.Collection<E> p0, java.util.function.Function<E, T> p1) {
        return toList(p0, p1, false);
    }

    public static <E, T> java.util.List<T> toList(java.util.Collection<E> p0, java.util.function.Function<E, T> p1, boolean p2) {
        if (p0 == null) return Collections.emptyList();
        if (p1 == null) throw new NullPointerException("Function cannot be null");
        java.util.List<T> res = new java.util.ArrayList<>();
        for (E item : p0) {
            if (item == null && p2) continue;
            T mapped = p1.apply(item);
            if (mapped == null && p2) continue;
            res.add(mapped);
        }
        return res;
    }

    public static <E, K> java.util.Map<K, java.util.List<E>> groupByKey(java.util.Collection<E> p0, java.util.function.Function<E, K> p1) {
        return groupByKey(p0, p1, false);
    }

    public static <E, K> java.util.Map<K, java.util.List<E>> groupByKey(java.util.Collection<E> p0, java.util.function.Function<E, K> p1, boolean p2) {
        if (p0 == null) return Collections.emptyMap();
        if (p1 == null) throw new NullPointerException("Function cannot be null");
        java.util.Map<K, java.util.List<E>> res = new java.util.LinkedHashMap<>();
        for (E item : p0) {
            if (item == null && p2) continue;
            K key = p1.apply(item);
            if (key == null && p2) continue;
            res.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(item);
        }
        return res;
    }

    public static <V, K> java.util.Map<K, V> toIdentityMap(java.util.Collection<V> p0, java.util.function.Function<V, K> p1) {
        return toIdentityMap(p0, p1, false);
    }

    public static <V, K> java.util.Map<K, V> toIdentityMap(java.util.Collection<V> p0, java.util.function.Function<V, K> p1, boolean p2) {
        if (p0 == null) return Collections.emptyMap();
        if (p1 == null) throw new NullPointerException("Function cannot be null");
        java.util.Map<K, V> res = new java.util.LinkedHashMap<>();
        for (V item : p0) {
            if (item == null && p2) continue;
            K key = p1.apply(item);
            if (key == null && p2) continue;
            res.put(key, item);
        }
        return res;
    }

    public static <E, K, V> java.util.Map<K, V> toMap(java.util.Collection<E> p0, java.util.function.Function<E, K> p1, java.util.function.Function<E, V> p2) {
        return toMap(p0, p1, p2, false);
    }

    public static <E, K, V> java.util.Map<K, V> toMap(java.util.Collection<E> p0, java.util.function.Function<E, K> p1, java.util.function.Function<E, V> p2, boolean p3) {
        if (p0 == null) return Collections.emptyMap();
        if (p1 == null || p2 == null) throw new NullPointerException("Function cannot be null");
        java.util.Map<K, V> res = new java.util.LinkedHashMap<>();
        for (E item : p0) {
            if (item == null && p3) continue;
            K key = p1.apply(item);
            V val = p2.apply(item);
            if ((key == null || val == null) && p3) continue;
            res.put(key, val);
        }
        return res;
    }

    public static <E, T> java.util.Set<T> toSet(java.util.Collection<E> p0, java.util.function.Function<E, T> p1) {
        return toSet(p0, p1, false);
    }

    public static <E, T> java.util.Set<T> toSet(java.util.Collection<E> p0, java.util.function.Function<E, T> p1, boolean p2) {
        if (p0 == null) return Collections.emptySet();
        if (p1 == null) throw new NullPointerException("Function cannot be null");
        java.util.Set<T> res = new java.util.LinkedHashSet<>();
        for (E item : p0) {
            if (item == null && p2) continue;
            T mapped = p1.apply(item);
            if (mapped == null && p2) continue;
            res.add(mapped);
        }
        return res;
    }

}
