package io.teaql.core.reference;

import java.time.Instant;
import java.util.Objects;

/** Authenticated selection; acceptance must still check current file state and authority. */
public record VerifiedBlobSelection(
        BlobSelectionClaim claim, String keyId, Instant issuedAt, Instant expiresAt) {
    public VerifiedBlobSelection {
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
    @Override public String toString() { return "VerifiedBlobSelection[redacted]"; }
}
