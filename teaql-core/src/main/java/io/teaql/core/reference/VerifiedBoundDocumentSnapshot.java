package io.teaql.core.reference;

import java.time.Instant;
import java.util.Objects;

/** Authenticated tqd2 authority. Current application permission must still be checked. */
public record VerifiedBoundDocumentSnapshot(
        BoundDocumentSnapshot document, String keyId, Instant issuedAt, Instant expiresAt) {
    public VerifiedBoundDocumentSnapshot {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
    @Override public String toString() { return "VerifiedBoundDocumentSnapshot[redacted]"; }
}
