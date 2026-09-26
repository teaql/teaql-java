package io.teaql.runtime.reference;

import io.teaql.core.UserContext;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.TrustedReferencePrincipal;

/** Re-evaluated at issue and consume time; a valid token is never an authorization grant. */
@FunctionalInterface
public interface RoundTripReferenceAuthorizationPolicy {
    void authorize(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            InternalEntityIdentity identity);
}
