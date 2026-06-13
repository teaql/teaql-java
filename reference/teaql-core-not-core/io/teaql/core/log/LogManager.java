package io.teaql.core.log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.teaql.core.UserContext;
import io.teaql.core.ExecutionMetadata;

/**
 * Dual-layer log manager.
 *
 * ═══════════════════════════════════════════════════════
 * Layer 1: Runtime layer (mandatory, env-var controlled, not customizable)
 * ═══════════════════════════════════════════════════════
 *   Contains raw, unmasked execution logs and audit information.
 *   Controlled by environment variables, no code:
 *     TEAQL_LOG_ENDPOINT  - file path (empty = no write)
 *     TEAQL_LOG_FORMAT    - human (default) | json
 *     TEAQL_LOG_SELECT    - true to log SELECT/QUERY (default false)
 *     TEAQL_LOG_MUTATION  - false to skip mutations (default true)
 *   Written audit info contains raw field values, unmasked.
 *
 * ═══════════════════════════════════════════════════════
 * Layer 2: App layer (customizable)
 * ═══════════════════════════════════════════════════════
 *   Application receives masked logs and audit events.
 *   Register LogSink and AuditSink to customize behavior:
 *     - Display on UI
 *     - Send to message queue
 *     - Write to database
 */
public class LogManager {

    // ==========================================
    // Layer 1: Runtime layer (env vars, read-only, not customizable)
    // ==========================================

    private static final String LOG_ENDPOINT = System.getenv("TEAQL_LOG_ENDPOINT");
    private static final LogFormatter FORMATTER = LogFormatter.getFormatter();
    private static final boolean MUTATION_LOG_ENABLED = !"false".equals(System.getenv("TEAQL_LOG_MUTATION"));
    private static final boolean SELECT_LOG_ENABLED = "true".equals(System.getenv("TEAQL_LOG_SELECT"));

    /**
     * Runtime layer: record execution log (raw, unmasked).
     * Called by Repository layer, writes to TEAQL_LOG_ENDPOINT file.
     */
    public void recordExecutionLog(ExecutionMetadata entry) {
        if (LOG_ENDPOINT == null || LOG_ENDPOINT.isEmpty()) return;
        boolean isSelect = entry.getOperation() == io.teaql.core.DataServiceOperation.QUERY || 
                           entry.getOperation() == io.teaql.core.DataServiceOperation.AGGREGATION;
        if (isSelect && !SELECT_LOG_ENABLED) return;
        if (!isSelect && !MUTATION_LOG_ENABLED) return;

        String traceChain = entry.getComment() != null ? entry.getComment() : "";
        String content = FORMATTER.formatExecutionLog(traceChain, entry);
        writeToEndpoint(content);

        // Also dispatch to App layer (masked)
        ExecutionMetadata safeEntry = sanitizeExecutionLog(entry);
        for (LogSink sink : logSinks) {
            sink.onExecutionLog(safeEntry);
        }
    }

    /**
     * Runtime layer: record audit event (raw, unmasked).
     * Called by Repository layer, writes to TEAQL_LOG_ENDPOINT file.
     *
     * @param maskFields fields to mask (from EntityDescriptor.audit_mask_fields)
     * @param maxValueLen max value length (from EntityDescriptor.audit_value_max_len)
     */
    public void emitAuditEvent(UserContext ctx, AuditEvent event,
                                List<String> maskFields, Integer maxValueLen) {
        // Layer 1: write to file (raw, unmasked)
        if (LOG_ENDPOINT != null && !LOG_ENDPOINT.isEmpty()) {
            String content = FORMATTER.formatAuditLog(event);
            writeToEndpoint(content);
        }

        // Layer 2: dispatch to App layer (masked)
        AuditEvent safeEvent = sanitizeAuditEvent(event, maskFields, maxValueLen);
        for (AuditEventSink sink : auditSinks) {
            sink.onEvent(ctx, safeEvent);
        }
    }

    /**
     * Convenience method: use default masking when no masking config is provided.
     */
    public void emitAuditEvent(UserContext ctx, AuditEvent event) {
        emitAuditEvent(ctx, event, List.of(), null);
    }

    // ==========================================
    // Masking (aligned with Rust build_safe_event)
    // ==========================================

    /**
     * Mask execution log.
     */
    private ExecutionMetadata sanitizeExecutionLog(ExecutionMetadata entry) {
        ExecutionMetadata safe = new ExecutionMetadata();
        safe.setBackend(entry.getBackend());
        safe.setOperation(entry.getOperation());
        safe.setStartedAt(entry.getStartedAt());
        safe.setEndedAt(entry.getEndedAt());
        safe.setAffectedRows(entry.getAffectedRows());
        safe.setResultCount(entry.getResultCount());
        safe.setBackendRequestId(entry.getBackendRequestId());
        safe.setTraceChain(entry.getTraceChain());
        safe.setComment(entry.getComment());

        String debugQuery = entry.getDebugQuery();
        if (debugQuery != null && debugQuery.length() > 500) {
            debugQuery = debugQuery.substring(0, 200) + "...(truncated)..." + debugQuery.substring(debugQuery.length() - 200);
        }
        safe.setDebugQuery(debugQuery);
        return safe;
    }

    /**
     * Mask audit event. Uses audit_mask_fields and audit_value_max_len from EntityDescriptor.
     * Aligned with Rust RawAuditEvent::build_safe_event.
     */
    private AuditEvent sanitizeAuditEvent(AuditEvent event, List<String> maskFields, Integer maxValueLen) {
        Map<String, Object> safeValues = maskMap(event.getValues(), maskFields, maxValueLen);
        Map<String, Object> safeOldValues = maskMap(event.getOldValues(), maskFields, maxValueLen);
        Map<String, Object> safeNewValues = maskMap(event.getNewValues(), maskFields, maxValueLen);

        List<AuditEvent.PropertyChange> safeChanges = new ArrayList<>();
        for (AuditEvent.PropertyChange change : event.getChanges()) {
            safeChanges.add(new AuditEvent.PropertyChange(
                change.getField(),
                maskValue(change.getField(), change.getOldValue(), maskFields, maxValueLen),
                maskValue(change.getField(), change.getNewValue(), maskFields, maxValueLen)));
        }

        return new AuditEvent(
            event.getKind(), event.getEntity(), safeValues,
            event.getUpdatedFields(), safeOldValues, safeNewValues,
            safeChanges, event.getComment());
    }

    private Map<String, Object> maskMap(Map<String, Object> map, List<String> maskFields, Integer maxLen) {
        if (map == null) return null;
        Map<String, Object> result = new java.util.HashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            result.put(e.getKey(), maskValue(e.getKey(), e.getValue(), maskFields, maxLen));
        }
        return result;
    }

    /**
     * Single field masking. Aligned with Rust mask_audit_value + limit_audit_value.
     *
     * @param field     field name
     * @param value     raw value
     * @param maskFields fields to mask (from EntityDescriptor.audit_mask_fields)
     * @param maxLen    max value length (from EntityDescriptor.audit_value_max_len)
     */
    private Object maskValue(String field, Object value, List<String> maskFields, Integer maxLen) {
        if (value == null) return null;

        String s = String.valueOf(value);

        // 1. Field-level masking (aligned with Rust: should_mask = audit_mask_fields.contains(field))
        if (maskFields.contains(field)) {
            return maskAuditValue(s);
        }

        // 2. Length truncation (aligned with Rust: limit_audit_value)
        if (maxLen != null && s.length() > maxLen) {
            return limitAuditValue(s, maxLen);
        }

        return value;
    }

    /**
     * Mask value. Aligned with Rust mask_audit_value.
     * Digits: replace all with *
     * Short strings: replace all with *
     * Long strings: keep first 2 and last 2, replace middle with *
     */
    private String maskAuditValue(String value) {
        if (value.isEmpty()) return "";

        if (value.chars().allMatch(Character::isDigit)) {
            return "*".repeat(value.length());
        }

        if (value.length() < 8) {
            return "*".repeat(value.length());
        }

        String prefix = value.substring(0, 2);
        String suffix = value.substring(value.length() - 2);
        String middle = "*".repeat(value.length() - 4);
        return prefix + middle + suffix;
    }

    /**
     * Truncate value. Aligned with Rust limit_audit_value.
     * Keep first half and last half, connect with ...
     */
    private String limitAuditValue(String value, int maxLen) {
        if (value.length() <= maxLen) return value;
        if (maxLen <= 3) return "*".repeat(maxLen);

        int keepLen = maxLen - 3; // "..." length
        int headLen = keepLen / 2;
        int tailLen = keepLen - headLen;

        String head = value.substring(0, headLen);
        String tail = value.substring(value.length() - tailLen);
        return head + "..." + tail;
    }

    // ==========================================
    // Layer 2: App layer (customizable)
    // ==========================================

    private final List<LogSink> logSinks = new CopyOnWriteArrayList<>();
    private final List<AuditEventSink> auditSinks = new CopyOnWriteArrayList<>();

    /**
     * App layer: register log sink (receives masked logs).
     */
    public void addLogSink(LogSink sink) {
        logSinks.add(sink);
    }

    /**
     * App layer: register audit sink (receives masked audit events).
     */
    public void addAuditSink(AuditEventSink sink) {
        auditSinks.add(sink);
    }

    public List<AuditEventSink> getAuditSinks() {
        return new ArrayList<>(auditSinks);
    }

    public List<LogSink> getLogSinks() {
        return new ArrayList<>(logSinks);
    }

    // ==========================================
    // File output (Layer 1)
    // ==========================================

    private static void writeToEndpoint(String content) {
        if (LOG_ENDPOINT == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_ENDPOINT, true))) {
            writer.println(content);
        } catch (IOException ignored) {
        }
    }
}
