package io.teaql.core;

import java.time.Instant;

/** Verified, boundary-safe identity recovered from an opaque entity reference. */
public record EntityReferenceClaims(
        String entityType,
        long id,
        long version,
        Instant issuedAt,
        Instant expiresAt,
        String purpose,
        int keyVersion) {}
