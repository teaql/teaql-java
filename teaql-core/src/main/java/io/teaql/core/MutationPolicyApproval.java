package io.teaql.core;

import java.time.Instant;
import java.util.Objects;

public record MutationPolicyApproval(
        MutationPolicyIdentity policy, String approvedBy, Instant approvedAt) {
    public MutationPolicyApproval {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(approvedBy, "approvedBy");
        Objects.requireNonNull(approvedAt, "approvedAt");
    }
}
