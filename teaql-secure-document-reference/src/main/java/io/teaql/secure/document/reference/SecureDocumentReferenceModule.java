package io.teaql.secure.document.reference;

import io.teaql.core.reference.RoundTripReferenceProvider;
import io.teaql.runtime.TeaQLRuntime;
import java.util.Objects;

/** Explicit, passive installation of context-bound reference protection. */
public final class SecureDocumentReferenceModule {
    public static final String CAPABILITY = "secure_document.v1";

    private final RoundTripReferenceProvider provider;

    private SecureDocumentReferenceModule(RoundTripReferenceProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /** Keys/configuration and application authorization are supplied at trusted startup. */
    public static SecureDocumentReferenceModule of(RoundTripReferenceProvider provider) {
        return new SecureDocumentReferenceModule(provider);
    }

    /** Registers only the provider; never loads a document, migrates schema or issues a token. */
    public TeaQLRuntime.Builder installInto(TeaQLRuntime.Builder builder) {
        return Objects.requireNonNull(builder, "builder").roundTripReferenceProvider(provider);
    }
}
