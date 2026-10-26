package io.teaql.core.reference;

import io.teaql.core.UserContext;
import java.time.Duration;

/** Provider SPI; implementations must authenticate, not merely obscure, identities. */
public interface RoundTripReferenceProvider {
    /** Optional tqd1 document snapshot, separate from entity-level tqr1 references. */
    default String issueDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal,
            DocumentSnapshot snapshot, Duration lifetime) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "ROUND_TRIP_REFERENCE_PROVIDER_NOT_CONFIGURED");
    }

    default VerifiedDocumentSnapshot consumeDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal, String token,
            String expectedAggregateType, String expectedPurpose) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "ROUND_TRIP_REFERENCE_PROVIDER_NOT_CONFIGURED");
    }

    /** Optional tqd2 snapshot; an editable Blob surface must use this distinct protocol. */
    default String issueBoundDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal,
            BoundDocumentSnapshot document, Duration lifetime) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "ROUND_TRIP_REFERENCE_PROVIDER_NOT_CONFIGURED");
    }

    default VerifiedBoundDocumentSnapshot consumeBoundDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal, String token,
            String expectedAggregateType, String expectedPurpose) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "ROUND_TRIP_REFERENCE_PROVIDER_NOT_CONFIGURED");
    }

    /** Optional tqs1 selection, checked against an authenticated tqd2 document. */
    default String issueBlobSelection(
            UserContext context, TrustedReferencePrincipal principal,
            String documentToken, BlobSelectionClaim claim, Duration lifetime) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "ROUND_TRIP_REFERENCE_PROVIDER_NOT_CONFIGURED");
    }

    /** Cryptographic/document check; the caller must check current Blob state before saving. */
    default VerifiedBlobSelection consumeBlobSelection(
            UserContext context, TrustedReferencePrincipal principal,
            String documentToken, String token,
            BlobSelectionTarget expectedTarget) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "ROUND_TRIP_REFERENCE_PROVIDER_NOT_CONFIGURED");
    }

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
