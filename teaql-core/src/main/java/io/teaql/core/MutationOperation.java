package io.teaql.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MutationOperation(
        MutationOperationKind kind,
        EntityKey entity,
        Long originalVersion,
        Map<String, Object> changedValues) {
    public MutationOperation {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(entity, "entity");
        changedValues = Collections.unmodifiableMap(new LinkedHashMap<>(
                changedValues == null ? Map.of() : changedValues));
    }
}
