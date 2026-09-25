package io.teaql.core.reference;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;

/**
 * Framework-neutral serialization boundary. Spring, JAX-RS and other Web
 * adapters call this codec; cryptographic behavior does not live in a Web layer.
 */
public final class RoundTripReferenceCodec {
    private final RoundTripReferenceProvider provider;

    public RoundTripReferenceCodec(RoundTripReferenceProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider must not be null");
        this.provider = provider;
    }

    public RoundTripReference serialize(UserContext context, Entity entity) {
        if (entity == null || entity.getId() == null || entity.getVersion() == null) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.INVALID_ENTITY_IDENTITY,
                    "A round-trip reference requires a persisted entity with id and version");
        }
        return provider.issue(
                context,
                new InternalEntityIdentity(entity.typeName(), entity.getId(), entity.getVersion()));
    }

    public ResolvedRoundTripReference deserialize(
            UserContext context, String reference, String expectedEntityType) {
        return provider.resolve(
                context,
                new RoundTripReference(reference),
                expectedEntityType);
    }
}
