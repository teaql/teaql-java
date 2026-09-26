package io.teaql.core.reference;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import java.time.Duration;

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

    public Object serialize(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            Entity entity,
            Duration lifetime) {
        if (entity == null || entity.getId() == null || entity.getVersion() == null) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.INVALID_ENTITY_IDENTITY,
                    "A round-trip reference requires a persisted entity with id and version");
        }
        return ReferenceWireCodec.serialize(provider.issue(
                context,
                principal,
                scope,
                new InternalEntityIdentity(entity.typeName(), entity.getId(), entity.getVersion()),
                lifetime));
    }

    public ResolvedRoundTripReference deserialize(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            Object reference,
            String expectedEntityType) {
        return provider.resolve(
                context,
                principal,
                scope,
                ReferenceWireCodec.deserialize(reference, provider.mode()),
                expectedEntityType);
    }

    public ReferenceMode mode() { return provider.mode(); }
}
