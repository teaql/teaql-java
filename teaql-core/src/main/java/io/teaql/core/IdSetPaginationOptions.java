package io.teaql.core;

/** Local-only controls for complete ordered ID-set pagination. */
public record IdSetPaginationOptions(String namespace, int ttlSeconds, int maxIds) {
    public static final int DEFAULT_TTL_SECONDS = 600;
    public static final int DEFAULT_MAX_IDS = 3_000_000;

    public IdSetPaginationOptions {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("ID set namespace must not be empty");
        }
        if (ttlSeconds <= 0) throw new IllegalArgumentException("ID set ttlSeconds must be positive");
        if (maxIds <= 0) throw new IllegalArgumentException("ID set maxIds must be positive");
    }

    public static IdSetPaginationOptions defaults() {
        return new IdSetPaginationOptions("default", DEFAULT_TTL_SECONDS, DEFAULT_MAX_IDS);
    }
}
