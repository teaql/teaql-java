package io.teaql.core;

import java.util.List;
import java.util.Objects;

public record MutationPlan(
        String executionId,
        String requestKey,
        String rootEntityType,
        String auditReason,
        List<MutationOperation> operations) {
    public MutationPlan {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(requestKey, "requestKey");
        Objects.requireNonNull(rootEntityType, "rootEntityType");
        operations = List.copyOf(operations == null ? List.of() : operations);
    }
}
