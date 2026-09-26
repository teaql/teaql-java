package io.teaql.core.reference;

import java.nio.charset.StandardCharsets;

/** Trusted server-side identity used to bind an external reference to its current actor. */
public record TrustedReferencePrincipal(
        String authenticationRealm,
        String subject,
        String domainRootType,
        long domainRootId) {
    public TrustedReferencePrincipal {
        requireText(authenticationRealm, "authenticationRealm");
        requireText(subject, "subject");
        requireText(domainRootType, "domainRootType");
        if (domainRootId <= 0) throw new IllegalArgumentException("domainRootId must be positive");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.getBytes(StandardCharsets.UTF_8).length > 4096) {
            throw new IllegalArgumentException(name + " must be non-blank and trimmed");
        }
    }
}
