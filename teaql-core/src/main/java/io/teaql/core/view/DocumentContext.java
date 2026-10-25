package io.teaql.core.view;

import io.teaql.core.reference.*;
import java.time.Instant;
import java.util.Objects;

/** Trusted outbound document boundary, supplied by the optional document implementation. */
public record DocumentContext(TrustedReferencePrincipal principal,ReferenceDocumentScope scope,Instant expiresAt) {
    public DocumentContext { Objects.requireNonNull(principal); Objects.requireNonNull(scope); Objects.requireNonNull(expiresAt); }
    @Override public String toString() { return "DocumentContext[redacted]"; }
}
