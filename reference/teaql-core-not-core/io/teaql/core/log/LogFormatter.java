package io.teaql.core.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.DataServiceOperation;

/**
 * Log formatter. Supports two formats:
 * - HumanReaderFormatter: human-readable format
 * - DebugReaderFormatter: machine-readable format
 *
 * Design aligned with teaql-rs LogFormatter trait.
 */
public interface LogFormatter {

    String formatExecutionLog(String traceChain, ExecutionMetadata entry);
    String formatAuditLog(AuditEvent event);

    // --- Human-readable format ---
    LogFormatter HUMAN = new HumanReaderFormatter();

    // --- Machine-readable format ---
    LogFormatter DEBUG = new DebugReaderFormatter();

    /**
     * Select formatter based on environment variable.
     */
    static LogFormatter getFormatter() {
        String format = System.getenv("TEAQL_LOG_FORMAT");
        if ("json".equals(format) || "debug".equals(format)) {
            return DEBUG;
        }
        return HUMAN;
    }

    /**
     * Human-readable formatter.
     */
    class HumanReaderFormatter implements LogFormatter {
        private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

        @Override
        public String formatExecutionLog(String traceChain, ExecutionMetadata entry) {
            String ts = entry.getStartedAt() != null ? TS.format(entry.getStartedAt()) : TS.format(Instant.now());
            java.time.Duration elapsed = entry.getEndedAt() != null && entry.getStartedAt() != null ? 
                java.time.Duration.between(entry.getStartedAt(), entry.getEndedAt()) : java.time.Duration.ZERO;
            long elapsedUs = elapsed.toNanos() / 1000;
            String trace = traceChain.isEmpty() ? "" : " - [" + traceChain + "]";
            String resultSummary = entry.getResultCount() != null ? entry.getResultCount() + " rows" : 
                entry.getAffectedRows() != null ? entry.getAffectedRows() + " affected" : "N/A";
            String queryStr = entry.getDebugQuery() != null ? entry.getDebugQuery().replace("\n", " ") : "";
            return String.format("[%s]-[%5dµs]-[DEBUG]-ExecutionLogEntry%s - [%s]\n          %s",
                ts, elapsedUs, trace, resultSummary, queryStr);
        }

        @Override
        public String formatAuditLog(AuditEvent event) {
            String ts = TS.format(Instant.now());
            String trace = event.getComment() != null ? " (Trace: " + event.getComment() + ")" : "";

            String fields = event.getChanges().stream()
                .filter(c -> !c.getField().startsWith("_"))
                .map(c -> c.getField() + ": " + (c.getNewValue() != null ? c.getNewValue() : "null"))
                .collect(Collectors.joining(", "));
            String fieldsPart = fields.isEmpty() ? "" : " {" + fields + "}";

            Object entityId = event.getValues() != null ? event.getValues().get("id") : "Unknown";

            return String.format("[%s]-[AUDIT]-Entity [%s:%s] %s%s%s",
                ts, event.getEntity(), entityId, event.getKind(), trace, fieldsPart);
        }
    }

    /**
     * Machine-readable formatter.
     */
    class DebugReaderFormatter implements LogFormatter {
        @Override
        public String formatExecutionLog(String traceChain, ExecutionMetadata entry) {
            String resultSummary = entry.getResultCount() != null ? entry.getResultCount() + " rows" : 
                entry.getAffectedRows() != null ? entry.getAffectedRows() + " affected" : "N/A";
            return String.format("[EXECUTION_LOG] %s - Entry: {backend=%s, op=%s, query=%s, result=%s}",
                traceChain, entry.getBackend(), entry.getOperation(), entry.getDebugQuery(), resultSummary);
        }

        @Override
        public String formatAuditLog(AuditEvent event) {
            return String.format("[AUDIT_LOG] %s - Event: {kind=%s, entity=%s, changes=%d}",
                event.getComment(), event.getKind(), event.getEntity(), event.getChanges().size());
        }
    }
}
