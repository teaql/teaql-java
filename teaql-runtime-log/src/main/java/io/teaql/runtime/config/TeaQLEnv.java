package io.teaql.runtime.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TeaQLEnv {
    private static final Map<String, String> ENV_CACHE;

    static {
        Map<String, String> tempCache = new HashMap<>();
        // Load from system properties first, fallback to environment variables
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith("TEAQL_")) {
                tempCache.put(key, entry.getValue().toString());
            }
        }
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("TEAQL_") && !tempCache.containsKey(key)) {
                tempCache.put(key, entry.getValue());
            }
        }
        ENV_CACHE = Collections.unmodifiableMap(tempCache);
    }

    public static String get(String key) {
        return ENV_CACHE.get(key);
    }

    public static String get(String key, String defaultValue) {
        return ENV_CACHE.getOrDefault(key, defaultValue);
    }

    public static long getSizeInBytes(String key, long defaultBytes) {
        String val = get(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultBytes;
        }
        val = val.trim().toUpperCase();
        try {
            if (val.endsWith("K") || val.endsWith("KB")) {
                return Long.parseLong(val.replaceAll("[A-Z]", "")) * 1024;
            }
            if (val.endsWith("M") || val.endsWith("MB")) {
                return Long.parseLong(val.replaceAll("[A-Z]", "")) * 1024 * 1024;
            }
            if (val.endsWith("G") || val.endsWith("GB")) {
                return Long.parseLong(val.replaceAll("[A-Z]", "")) * 1024 * 1024 * 1024;
            }
            return Long.parseLong(val);
        } catch (Exception e) {
            return defaultBytes;
        }
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
