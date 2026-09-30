package io.teaql.runtime;

import io.teaql.core.EntityKey;
import io.teaql.core.MutationOperationKind;
import java.util.List;

public record MutationOperationSummary(
        MutationOperationKind kind, EntityKey entity, List<String> changedFields) {
    public MutationOperationSummary {
        changedFields = List.copyOf(changedFields == null ? List.of() : changedFields);
    }
}
