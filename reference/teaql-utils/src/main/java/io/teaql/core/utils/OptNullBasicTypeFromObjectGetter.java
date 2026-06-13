package io.teaql.core.utils;

public interface OptNullBasicTypeFromObjectGetter<K> {
    Object getObj(K key, Object defaultValue);

    default Object getObj(K key) {
        return getObj(key, null);
    }

    default String getStr(K key, String defaultValue) {
        Object value = getObj(key);
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    default String getStr(K key) {
        return getStr(key, null);
    }

    default Boolean getBool(K key, Boolean defaultValue) {
        Object value = getObj(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    default Boolean getBool(K key) {
        return getBool(key, null);
    }

    default Integer getInt(K key, Integer defaultValue) {
        Object value = getObj(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    default Integer getInt(K key) {
        return getInt(key, null);
    }

    default Long getLong(K key, Long defaultValue) {
        Object value = getObj(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    default Long getLong(K key) {
        return getLong(key, null);
    }
}
