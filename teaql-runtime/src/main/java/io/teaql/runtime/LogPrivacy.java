package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared log projection boundary; execution and business event values are never mutated. */
public final class LogPrivacy {
    public static final String ENVIRONMENT = "TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS";
    public static final String ACKNOWLEDGEMENT = "I_UNDERSTAND_SENSITIVE_DATA_MAY_BE_WRITTEN_TO_DISK";
    public static final String REDACTED = "[REDACTED]";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private LogPrivacy() {}

    public static boolean plaintextEnabled() {
        if (!ACKNOWLEDGEMENT.equals(System.getenv(ENVIRONMENT))) return false;
        if (WARNED.compareAndSet(false, true)) System.err.println(
                "[TeaQL WARNING] Sensitive plaintext logging enabled; business data may be written to disk. Credentials remain redacted.");
        return true;
    }

    public static boolean credential(String name) {
        String key = Objects.toString(name, "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        return List.of("password", "passwd", "passphrase", "privatekey", "secret", "accesstoken",
                "refreshtoken", "idtoken", "apikey", "authorization", "credential", "sessiontoken", "magiclinktoken")
                .stream().anyMatch(key::contains);
    }

    public static boolean hasCredentials(Object value) {
        if (value instanceof Map<?, ?> map) return map.entrySet().stream()
                .anyMatch(e -> credential(String.valueOf(e.getKey())) || hasCredentials(e.getValue()));
        if (value instanceof Iterable<?> items) for (Object item : items) if (hasCredentials(item)) return true;
        return false;
    }

    private static void collect(Object value, List<String> result) {
        if (value == null) return;
        if (value instanceof Map<?, ?> map) { map.values().forEach(item -> collect(item, result)); return; }
        if (value instanceof Iterable<?> items) { items.forEach(item -> collect(item, result)); return; }
        String text = String.valueOf(value);
        if (!text.isEmpty()) result.add(text);
    }

    public static String scrub(String text, Collection<?> values) {
        if (text == null) return null;
        List<String> strings = new ArrayList<>();
        values.forEach(value -> collect(value, strings));
        strings.sort(Comparator.comparingInt(String::length).reversed());
        for (String value : strings) text = text.replace(value, REDACTED);
        return text;
    }

    public static List<TraceNode> trace(List<TraceNode> nodes, Collection<?> values) {
        if (nodes == null) return List.of();
        return nodes.stream().map(node -> new TraceNode(node.getKind(),
                scrub(node.getName(), values), scrub(node.getComment(), values))).toList();
    }

    public static ExecutionMetadata sql(ExecutionMetadata source, boolean allow) {
        boolean reveal = allow && !credential(source.getParameterizedQuery())
                && !credential(source.getDebugQuery()) && !hasCredentials(source.getParameters());
        List<?> secrets = reveal ? List.of() : source.getParameters();
        ExecutionMetadata safe = new ExecutionMetadata();
        safe.setBackend(source.getBackend()); safe.setOperation(source.getOperation());
        safe.setStartedAt(source.getStartedAt()); safe.setEndedAt(source.getEndedAt());
        safe.setElapsedUs(source.getElapsedUs()); safe.setAffectedRows(source.getAffectedRows());
        safe.setResultCount(source.getResultCount()); safe.setResultSummary(scrub(source.getResultSummary(), secrets));
        safe.setBackendRequestId(scrub(source.getBackendRequestId(), secrets));
        safe.setComment(scrub(source.getComment(), secrets)); safe.setPurpose(scrub(source.getPurpose(), secrets));
        safe.setAuditReason(scrub(source.getAuditReason(), secrets)); safe.setTraceChain(trace(source.getTraceChain(), secrets));
        String sql = source.getParameterizedQuery();
        if (!reveal && sql != null && (sql.matches("(?s).*[0-9'\"`$].*") || sql.contains("--") || sql.contains("/*")))
            sql = "[REDACTED SQL; NOT REPLAYABLE]";
        safe.setParameterizedQuery(sql);
        safe.setParameters(reveal ? source.getParameters() : Collections.nCopies(source.getParameterCount(), null));
        safe.setDebugQuery(reveal ? source.getDebugQuery() : null);
        return safe;
    }

    public static RawAuditEvent audit(RawAuditEvent source, boolean allow) {
        List<Object> secrets = new ArrayList<>();
        List<AuditFieldChange> changes = source.changes().stream().map(change -> {
            boolean mask = !allow || credential(change.field()) || hasCredentials(change.oldValue()) || hasCredentials(change.newValue());
            if (!mask) return change;
            secrets.add(change.oldValue()); secrets.add(change.newValue());
            return new AuditFieldChange(change.field(), change.oldValue() == null ? null : REDACTED,
                    change.newValue() == null ? null : REDACTED);
        }).toList();
        return new RawAuditEvent(source.kind(), source.entityType(), source.entityId(), changes,
                trace(source.traceChain(), secrets), scrub(source.actor(), secrets), source.category(),
                scrub(source.reason(), secrets), source.resultingVersion(), source.occurredAt());
    }
}
