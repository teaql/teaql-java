package io.teaql.core.reference;

import java.time.Instant;
import java.util.Objects;

/** Authenticated document authority. The application must still check current permissions. */
public record VerifiedDocumentSnapshot(
        DocumentSnapshot snapshot, String keyId, Instant issuedAt, Instant expiresAt) {
    public VerifiedDocumentSnapshot {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
    @Override public String toString() { return "VerifiedDocumentSnapshot[redacted]"; }
}
