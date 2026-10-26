package io.teaql.core.view;

import io.teaql.core.FrameworkInternal;
import io.teaql.core.UserContext;
import io.teaql.core.reference.ContextBoundDocumentService;
import io.teaql.core.reference.ContextBoundDocumentService.ContextualDocument;
import io.teaql.core.reference.RoundTripReferenceCodec;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.reference.VerifiedDocumentSnapshot;
import java.util.Map;
import java.util.Objects;

/** An issued document adopted by its installed view service after a successful open/accept. */
public final class DocumentView {
    private final Object issuer;
    private final ContextualDocument document;
    private final VerifiedDocumentSnapshot verified;
    private final TrustedReferencePrincipal principal;
    private final ContextBoundDocumentService service;
    private final RoundTripReferenceCodec codec;
    private final ViewOptions.BusinessCompletion completion;

    private DocumentView(Object issuer, ContextualDocument document,
            VerifiedDocumentSnapshot verified, TrustedReferencePrincipal principal,
            ContextBoundDocumentService service, RoundTripReferenceCodec codec,
            ViewOptions.BusinessCompletion completion) {
        this.issuer = Objects.requireNonNull(issuer);
        this.document = Objects.requireNonNull(document);
        this.verified = Objects.requireNonNull(verified);
        this.principal = Objects.requireNonNull(principal);
        this.service = Objects.requireNonNull(service);
        this.codec = Objects.requireNonNull(codec);
        this.completion = Objects.requireNonNull(completion);
    }

    @FrameworkInternal("Only the installed view service can adopt the successful service result")
    public static DocumentView issue(Object issuer, ContextualDocument document,
            VerifiedDocumentSnapshot verified, TrustedReferencePrincipal principal,
            ContextBoundDocumentService service, RoundTripReferenceCodec codec,
            ViewOptions.BusinessCompletion completion) {
        return new DocumentView(issuer, document, verified, principal, service, codec, completion);
    }

    public ViewOptions.BusinessCompletion businessCompletion() { return completion; }

    @FrameworkInternal("Check private presentation provenance before using an adopted document")
    public boolean matches(Object expectedIssuer) { return issuer == expectedIssuer; }

    @FrameworkInternal("The private issuer binds this source to one installed view service")
    public ContextualDocument document(Object expectedIssuer) {
        requireIssuer(expectedIssuer);
        return document;
    }

    @FrameworkInternal("The private issuer binds this source to one installed view service")
    public DocumentContext context(Object expectedIssuer) {
        requireIssuer(expectedIssuer);
        return new DocumentContext(principal, verified.snapshot().scope(), verified.expiresAt());
    }

    @FrameworkInternal("The private issuer binds this source to one installed view service")
    public Map<String, String> credentials(Object expectedIssuer) {
        requireIssuer(expectedIssuer);
        return Map.of("businessId", document.businessId(), "documentToken", document.documentToken());
    }

    /** Re-authenticates the same token and current application authority without reissuing it. */
    @FrameworkInternal("Validation for an already issued outbound document")
    public void validateCurrent(Object expectedIssuer, UserContext context) {
        requireIssuer(expectedIssuer);
        if (!principal.equals(context.trustedReferencePrincipal())
                || service != context.contextBoundDocumentService()
                || codec != context.roundTripReferences()) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.CONTEXT_MISMATCH, "CONTEXT_MISMATCH");
        }
        VerifiedDocumentSnapshot current = context.consumeDocumentSnapshot(
                document.documentToken(), verified.snapshot().scope().aggregateType(),
                verified.snapshot().scope().purpose());
        if (!verified.equals(current)) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.CONTEXT_MISMATCH, "CONTEXT_MISMATCH");
        }
        service.validatePreparedDocument(context, principal, current);
    }

    private void requireIssuer(Object expectedIssuer) {
        if (issuer != expectedIssuer) throw new IllegalArgumentException("Foreign document view issuer");
    }

    @Override public String toString() { return "DocumentView[redacted]"; }
}
