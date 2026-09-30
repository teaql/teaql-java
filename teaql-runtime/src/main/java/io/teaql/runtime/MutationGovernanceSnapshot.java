package io.teaql.runtime;

import io.teaql.core.MutationPolicyIdentity;
import java.util.List;

public record MutationGovernanceSnapshot(
        String executionId,
        String requestKey,
        MutationPolicySource source,
        MutationPolicyIdentity policy,
        MutationPolicyApprovalStatus approvalStatus,
        List<String> warningCodes,
        List<MutationOperationSummary> operations) {
    public MutationGovernanceSnapshot {
        warningCodes = List.copyOf(warningCodes == null ? List.of() : warningCodes);
        operations = List.copyOf(operations == null ? List.of() : operations);
    }
}
