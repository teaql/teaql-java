package io.teaql.core.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MapUtil {

    public static <K, V> MapBuilder<K, V> builder() {
        return new MapBuilder<>();
    }

    public static <K, V> MapBuilder<K, V> builder(K p0, V p1) {
        MapBuilder<K, V> builder = new MapBuilder<>();
        builder.put(p0, p1);
        return builder;
    }

    public static <K, V> MapBuilder<K, V> builder(java.util.Map<K, V> p0) {
        return new MapBuilder<>(p0);
    }

    public static java.lang.Boolean getBool(java.util.Map<?, ?> p0, java.lang.Object p1) {
        return getBool(p0, p1, null);
    }

    public static java.lang.Boolean getBool(java.util.Map<?, ?> p0, java.lang.Object p1, java.lang.Boolean p2) {
        if (p0 == null) {
            return p2;
        }
        Object val = p0.get(p1);
        if (val == null) {
            return p2;
        }
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return BooleanUtil.toBoolean(val.toString());
    }

    public static <K, V> java.lang.String joinIgnoreNull(java.util.Map<K, V> p0, java.lang.String p1, java.lang.String p2, java.lang.String... p3) {
        if (p0 == null || p0.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        Set<String> ignore = new HashSet<>();
        if (p3 != null) {
            for (String s : p3) {
                if (s != null) ignore.add(s);
            }
        }
        for (Map.Entry<K, V> entry : p0.entrySet()) {
            K key = entry.getKey();
            V val = entry.getValue();
            if (key == null || val == null) continue;
            if (ignore.contains(key.toString())) continue;
            if (!first) {
                sb.append(p1);
            }
            sb.append(key).append(p2).append(val);
            first = false;
        }
        return sb.toString();
    }

    public static <K, V> java.util.HashMap<K, V> of(K p0, V p1) {
        return of(p0, p1, false);
    }

    public static <K, V> java.util.HashMap<K, V> of(K p0, V p1, boolean p2) {
        java.util.HashMap<K, V> map = p2 ? new java.util.LinkedHashMap<>() : new java.util.HashMap<>();
        map.put(p0, p1);
        return map;
    }

    public static java.util.HashMap<java.lang.Object, java.lang.Object> of(java.lang.Object[] p0) {
        if (p0 == null || p0.length == 0) {
            return new java.util.HashMap<>();
        }
        if (p0.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments: " + p0.length);
        }
        java.util.HashMap<Object, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < p0.length; i += 2) {
            map.put(p0[i], p0[i + 1]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> java.util.Map<K, V> createMap(java.lang.Class<?> p0) {
        if (p0 == null) {
            return new java.util.HashMap<>();
        }
        try {
            return (java.util.Map<K, V>) p0.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    public static <K, V> java.util.Map<K, V> empty() {
        return java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static <K, V, T extends java.util.Map<K, V>> T empty(java.lang.Class<?> p0) {
        if (p0 == null) {
            return (T) java.util.Collections.emptyMap();
        }
        if (p0 == java.util.Map.class) {
            return (T) java.util.Collections.emptyMap();
        }
        try {
            return (T) p0.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return (T) java.util.Collections.emptyMap();
        }
    }

    @SafeVarargs
    public static <K, V> java.util.Map<K, V> of(io.teaql.core.utils.Pair<K, V>... p0) {
        if (p0 == null || p0.length == 0) return new java.util.HashMap<>();
        java.util.HashMap<K, V> map = new java.util.HashMap<>();
        for (io.teaql.core.utils.Pair<K, V> pair : p0) {
            if (pair != null) {
                map.put(pair.getKey(), pair.getValue());
            }
        }
        return map;
    }

    public static <K, V> java.util.TreeMap<K, V> sort(java.util.Map<K, V> p0) {
        if (p0 == null) return null;
        return new java.util.TreeMap<>(p0);
    }

    public static <K, V> java.util.TreeMap<K, V> sort(java.util.Map<K, V> p0, java.util.Comparator<? super K> p1) {
        if (p0 == null) return null;
        java.util.TreeMap<K, V> map = new java.util.TreeMap<>(p1);
        map.putAll(p0);
        return map;
    }

}
