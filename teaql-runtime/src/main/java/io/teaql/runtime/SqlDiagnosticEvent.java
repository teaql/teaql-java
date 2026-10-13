package io.teaql.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit allowlist applied after the existing mask projection. No metadata/object spreading. */
final class SqlDiagnosticEvent {
    private static final ObjectMapper JSON = new ObjectMapper();
    static RuntimeTelemetry.Diagnostic project(ExecutionMetadata raw) throws JsonProcessingException {
        ExecutionMetadata safe = LogPrivacy.sql(raw, false);
        Map<String, Object> attributes = new LinkedHashMap<>();
        put(attributes, "teaql.sql.debug", safe.getDebugQuery());
        // The export channel always uses the mask policy; explicitly PLAIN parameters
        // remain allowed by that policy. Unsafe inline SQL is omitted, never exported raw.
        put(attributes, "teaql.sql.log_mode", "OMITTED".equals(safe.getLogMode()) ? "omitted" : "masked");
        put(attributes, "teaql.intent.comment", safe.getComment() != null ? safe.getComment() : safe.getAuditReason());
        put(attributes, "teaql.intent.purpose", safe.getPurpose());
        put(attributes, "teaql.intent.audit_reason", safe.getAuditReason());
        put(attributes, "teaql.sql.operation", safe.getStatementOperation());
        put(attributes, "teaql.sql.outcome", safe.getExecutionOutcome());
        put(attributes, "teaql.sql.batch_outcome", safe.getBatchOutcome());
        put(attributes, "teaql.sql.elapsed_us", safe.getElapsedUs());
        put(attributes, "teaql.sql.result_count", safe.getResultCount());
        put(attributes, "teaql.sql.affected_rows", safe.getAffectedRows());
        put(attributes, "teaql.trace_chain", JSON.writeValueAsString(path(safe.getRequestTraceChain())));
        put(attributes, "teaql.trace_path", JSON.writeValueAsString(path(safe.getTracePath())));
        put(attributes, "teaql.mutation_lineage", JSON.writeValueAsString(path(safe.getMutationLineage())));
        return new RuntimeTelemetry.Diagnostic(raw.getSqlExecutionCorrelation(), attributes);
    }
    private static void put(Map<String, Object> values, String key, Object value) { if (value != null) values.put(key, value); }
    private static List<Map<String, Object>> path(List<TraceNode> nodes) {
        if (nodes == null) return List.of();
        return nodes.stream().map(node -> {
            Map<String, Object> fields = new LinkedHashMap<>();
            put(fields, "kind", node.getKind() == null ? null : node.getKind().name().toLowerCase(java.util.Locale.ROOT));
            put(fields, "name", node.getName());
            put(fields, "detail", node.getComment());
            // TraceNode.entityId is intentionally never exported.
            return fields;
        }).toList();
    }
}
