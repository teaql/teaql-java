package io.teaql.core;

import java.util.Objects;

public record MutationPolicyIdentity(String id, String version, String fingerprint) {
    public MutationPolicyIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(fingerprint, "fingerprint");
    }
}
