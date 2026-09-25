package io.teaql.runtime.reference;

import io.teaql.core.UserContext;
import io.teaql.core.reference.*;

/** Development/test-only readable representation. Never install in production. */
public final class RawRoundTripReferenceProvider implements RoundTripReferenceProvider {
    @Override
    public RoundTripReference issue(UserContext context, InternalEntityIdentity identity) {
        if (identity.entityType().contains(".")) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.INVALID_ENTITY_IDENTITY,
                    "Raw diagnostic entity type must not contain '.'");
        }
        return new RoundTripReference("raw1." + identity.entityType() + "."
                + identity.id() + "." + identity.version());
    }

    @Override
    public ResolvedRoundTripReference resolve(
            UserContext context, RoundTripReference reference, String expectedEntityType) {
        String[] parts = reference.value().split("\\.", -1);
        try {
            if (parts.length != 4 || !"raw1".equals(parts[0]) || !parts[1].equals(expectedEntityType)) {
                throw new IllegalArgumentException("shape/type mismatch");
            }
            InternalEntityIdentity identity = new InternalEntityIdentity(
                    parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]));
            return new ResolvedRoundTripReference(
                    identity.entityType(), identity.id(), identity.version());
        } catch (RuntimeException error) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.INVALID_REFERENCE,
                    "Invalid raw diagnostic round-trip reference", error);
        }
    }
}
