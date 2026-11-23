package io.teaql.runtime;

import io.teaql.core.TraceNode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One immutable, model-named fact frozen from a provider readback before commit. */
public record CommittedMutationFact(
        String entityType, String entityId, String operation,
        String baseVersion, String committedVersion,
        Map<String, Object> fields, List<TraceNode> traceChain) {

    public CommittedMutationFact {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(committedVersion, "committedVersion");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(fields, "fields")));
        traceChain = List.copyOf(Objects.requireNonNull(traceChain, "traceChain"));
    }

    @Override public String toString() {
        return "CommittedMutationFact[entityType=" + entityType
                + ", operation=" + operation + ", fields=<redacted>, traceChain=<redacted>]";
    }
}
