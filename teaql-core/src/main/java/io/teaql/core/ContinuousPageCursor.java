package io.teaql.core;

import java.time.Instant;
import java.util.Map;

/** Server-owned, observable checkpoint for a continuous page query. */
public record ContinuousPageCursor(
        int formatVersion,
        String cursorId,
        String namespace,
        String queryFingerprint,
        String entity,
        String orderField,
        String direction,
        Object boundary,
        long sourceOffset,
        long nextOffset,
        int pageSize,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        Map<String, String> ownerContext,
        Map<String, String> observableContext) {

    public static final int CURRENT_FORMAT_VERSION = 1;

    public ContinuousPageCursor {
        ownerContext = ownerContext == null ? Map.of() : Map.copyOf(ownerContext);
        observableContext = observableContext == null ? Map.of() : Map.copyOf(observableContext);
    }

    public boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
