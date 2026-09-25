package io.teaql.core.reference;

/** Decoded identity. This value is not an authorization decision. */
public record ResolvedRoundTripReference(String entityType, long id, long version) {
    public InternalEntityIdentity identity() {
        return new InternalEntityIdentity(entityType, id, version);
    }
}
