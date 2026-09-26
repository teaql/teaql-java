package io.teaql.core.reference;

import java.time.Instant;

/** Decoded identity after scope binding and current authorization have both succeeded. */
public record ResolvedRoundTripReference(
        InternalEntityIdentity identity,
        ReferenceMode mode,
        String keyId,
        Instant issuedAt,
        Instant expiresAt) {
    public String entityType() { return identity.entityType(); }
    public long id() { return identity.id(); }
    public long version() { return identity.version(); }
}
