package io.teaql.core.reference;

import io.teaql.core.UserContext;

/** Provider SPI; implementations must authenticate, not merely obscure, identities. */
public interface RoundTripReferenceProvider {
    RoundTripReference issue(
            UserContext context, InternalEntityIdentity identity);

    ResolvedRoundTripReference resolve(
            UserContext context,
            RoundTripReference reference,
            String expectedEntityType);
}
