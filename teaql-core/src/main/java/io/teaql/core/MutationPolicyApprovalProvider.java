package io.teaql.core;

import java.util.Optional;

@FunctionalInterface
public interface MutationPolicyApprovalProvider {
    Optional<MutationPolicyApproval> findApproval(MutationPolicyIdentity policy);

    static MutationPolicyApprovalProvider none() { return policy -> Optional.empty(); }
}
