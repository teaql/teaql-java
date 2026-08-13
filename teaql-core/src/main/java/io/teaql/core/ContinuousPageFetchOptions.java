package io.teaql.core;

import java.util.Objects;

/**
 * Explicit acceptance of best-effort stateful pagination optimization.
 * This is a local runtime hint, not a correctness or snapshot guarantee.
 */
public final class ContinuousPageFetchOptions {
    public static final int DEFAULT_TTL_SECONDS = 600;

    private final String namespace;
    private final int ttlSeconds;

    public ContinuousPageFetchOptions(String namespace, int ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("continuous page fetch TTL must be positive");
        }
        this.namespace = normalizeNamespace(namespace);
        this.ttlSeconds = ttlSeconds;
    }

    public static ContinuousPageFetchOptions defaults() {
        return new ContinuousPageFetchOptions("default", DEFAULT_TTL_SECONDS);
    }

    public String namespace() {
        return namespace;
    }

    public int ttlSeconds() {
        return ttlSeconds;
    }

    private static String normalizeNamespace(String namespace) {
        String value = Objects.requireNonNullElse(namespace, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("continuous page fetch namespace must not be blank");
        }
        if (value.length() > 128) {
            throw new IllegalArgumentException("continuous page fetch namespace is too long");
        }
        return value;
    }
}
