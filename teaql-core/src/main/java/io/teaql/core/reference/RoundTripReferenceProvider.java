package io.teaql.core.reference;

import io.teaql.core.UserContext;
import java.time.Duration;

/** Provider SPI; implementations must authenticate, not merely obscure, identities. */
public interface RoundTripReferenceProvider {
    ExternalEntityReference issue(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            InternalEntityIdentity identity,
            Duration lifetime);

    ResolvedRoundTripReference resolve(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            ExternalEntityReference reference,
            String expectedEntityType);

    ReferenceMode mode();
}
