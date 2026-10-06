package io.teaql.runtime.log;

import io.teaql.core.TraceNode;
import java.util.List;
import java.util.stream.Collectors;

public class JsonReaderFormatter implements LogFormatter {
    private String formatTraceChain(List<TraceNode> traceChain) {
        if (traceChain == null || traceChain.isEmpty()) {
            return "[]";
        }
        return "[" + traceChain.stream()
                .map(t -> (CharSequence)("{\"kind\":\"" + t.getKind()
                        + "\",\"name\":\"" + escapeJson(t.getName())
                        + "\",\"entityId\":" + t.getEntityId()
                        + ",\"value\":\"" + escapeJson(t.getComment()) + "\"}"))
                .collect(Collectors.joining(",")) + "]";
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '"') result.append('\\').append(c);
            else if (c < 32) result.append(String.format("\\u%04x", (int) c));
            else result.append(c);
        }
        return result.toString();
    }

    @Override
    public String formatExecutionLog(io.teaql.core.ExecutionMetadata metadata) {
        metadata = io.teaql.runtime.LogPrivacy.sql(metadata, io.teaql.runtime.LogPrivacy.plaintextEnabled());
        return String.format("{\"type\":\"EXEC_LOG\",\"tracePath\":%s,\"mutationLineage\":%s,\"backend\":\"%s\",\"operation\":\"%s\",\"comment\":\"%s\",\"purpose\":\"%s\",\"auditReason\":\"%s\",\"elapsedUs\":%d,\"resultCount\":%s,\"affectedRows\":%s,\"summary\":\"%s\",\"sql\":\"%s\",\"logMode\":\"%s\",\"maskedParameters\":%s,\"sqlOmissionReason\":\"%s\",\"executionOutcome\":\"%s\",\"batchOutcome\":\"%s\"}",
                formatTraceChain(metadata.getTraceChain()),
                formatTraceChain(metadata.getMutationLineage()),
                escapeJson(metadata.getBackend()), metadata.getOperation(),
                escapeJson(metadata.getComment()), escapeJson(metadata.getPurpose()),
                escapeJson(metadata.getAuditReason()), metadata.getElapsedUs(),
                metadata.getResultCount(), metadata.getAffectedRows(),
                escapeJson(metadata.getResultSummary()), escapeJson(metadata.getDebugQuery()),
                escapeJson(metadata.getLogMode()), metadata.getParameterMasked(),
                escapeJson(metadata.getSqlOmissionReason()),
                escapeJson(metadata.getExecutionOutcome() == null ? "unknown" : metadata.getExecutionOutcome()),
                escapeJson(metadata.getBatchOutcome() == null ? "not_applicable" : metadata.getBatchOutcome()));
    }

    @Override
    public String formatAuditLog(List<TraceNode> traceChain, AuditEvent event) {
        return String.format("{\"type\":\"AUDIT_LOG\",\"trace\":%s,\"entity\":\"%s\",\"id\":\"%s\",\"kind\":\"%s\"}",
                formatTraceChain(traceChain),
                escapeJson(event.getEntityType()),
                formatEntityId(event),
                escapeJson(event.getMutationKind()));
    }

    protected String formatEntityId(AuditEvent event) {
        return event.getEntityId() != null ? escapeJson(event.getEntityId().toString()) : "null";
    }
}
