package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import io.teaql.core.SqlParameterLogPolicy;
import io.teaql.core.utils.SqlLogRenderer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared log projection boundary; execution and business event values are never mutated. */
public final class LogPrivacy {
    public static final String ENVIRONMENT = "TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS";
    public static final String ACKNOWLEDGEMENT = "I_UNDERSTAND_SENSITIVE_DATA_MAY_BE_WRITTEN_TO_DISK";
    public static final String REDACTED = "[REDACTED]";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final String DEBUG_LABEL = "-- TeaQL DEBUG PLAINTEXT; EXPLICIT OPT-IN\n";
    private LogPrivacy() {}

    public static boolean plaintextEnabled() {
        if (!ACKNOWLEDGEMENT.equals(System.getenv(ENVIRONMENT))) return false;
        if (WARNED.compareAndSet(false, true)) System.err.println(
                "[TeaQL WARNING] Sensitive plaintext logging enabled; business data may be written to disk. Credentials remain redacted.");
        return true;
    }

    public static boolean credential(String name) {
        return io.teaql.core.utils.SensitiveLogNames.credential(name);
    }

    public static boolean hasCredentials(Object value) {
        if (value instanceof Map<?, ?> map) return map.entrySet().stream()
                .anyMatch(e -> credential(String.valueOf(e.getKey())) || hasCredentials(e.getValue()));
        if (value instanceof Iterable<?> items) for (Object item : items) if (hasCredentials(item)) return true;
        if (value != null && value.getClass().isArray())
            for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
                if (hasCredentials(java.lang.reflect.Array.get(value, i))) return true;
        return false;
    }

    private static void collect(Object value, List<String> result) {
        if (value == null) return;
        if (value instanceof Map<?, ?> map) { map.values().forEach(item -> collect(item, result)); return; }
        if (value instanceof Iterable<?> items) { items.forEach(item -> collect(item, result)); return; }
        if (value.getClass().isArray()) {
            for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
                collect(java.lang.reflect.Array.get(value, i), result);
            return;
        }
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
                scrub(node.getName(), values), node.getEntityId(), scrub(node.getComment(), values))).toList();
    }

    public static ExecutionMetadata sql(ExecutionMetadata source, boolean allow) {
        String mode = source.getLogMode();
        boolean debugSource = (mode != null && mode.startsWith("DEBUG"))
                || (source.getDebugQuery() != null && source.getDebugQuery().startsWith(DEBUG_LABEL));
        if (Set.of("PLAIN", "MASKED", "OMITTED").contains(mode == null ? "" : mode)) allow = false;
        boolean orphanedDebug = false;
        if (!allow && debugSource) {
            ExecutionMetadata remembered = SqlLogProjectionCache.safeCopy(source);
            if (remembered != null) return remembered;
            orphanedDebug = true;
        }
        List<SqlParameterLogPolicy> policies = source.getParameterLogPolicies();
        boolean invalidPolicies = !policies.isEmpty() && policies.size() != source.getParameterCount();
        boolean invalidFlags = !source.getParameterMasked().isEmpty()
                && source.getParameterMasked().size() != source.getParameterCount();
        boolean legacyCredential = (policies.isEmpty() || !source.isGeneratedSql()) && (credential(source.getParameterizedQuery())
                || credential(source.getDebugQuery()));
        List<Object> values = new ArrayList<>();
        List<Boolean> masked = new ArrayList<>();
        List<Object> secrets = new ArrayList<>();
        for (int i = 0; i < source.getParameterCount(); i++) {
            Object raw = source.getParameters().get(i);
            var policy = invalidPolicies || invalidFlags || policies.isEmpty() ? SqlParameterLogPolicy.UNKNOWN : policies.get(i);
            boolean forced = legacyCredential || policy == SqlParameterLogPolicy.CREDENTIAL || hasCredentials(raw);
            boolean alreadyMasked = source.getParameterMasked().size() == source.getParameterCount()
                    && source.getParameterMasked().get(i);
            boolean hide = forced || invalidPolicies || alreadyMasked || policy == SqlParameterLogPolicy.UNKNOWN
                    || (!allow && policy != SqlParameterLogPolicy.PLAIN);
            masked.add(hide);
            if (hide) {
                secrets.add(raw);
                values.add(raw == null ? null : !forced && policy == SqlParameterLogPolicy.MASKED
                        && (raw instanceof CharSequence || raw instanceof Number || raw instanceof Boolean
                            || raw instanceof java.time.temporal.Temporal)
                        ? TeaQLRuntime.maskAuditValue(raw.toString()) : REDACTED);
            } else values.add(copyValue(raw));
        }
        if (source.getIntentRedactions() != null) source.getIntentRedactions().appendTo(secrets, allow);
        ExecutionMetadata safe = new ExecutionMetadata();
        safe.setBackend(source.getBackend()); safe.setOperation(source.getOperation());
        safe.setExecutionOutcome(source.getExecutionOutcome());
        safe.setBatchOutcome(source.getBatchOutcome());
        safe.setStartedAt(source.getStartedAt()); safe.setEndedAt(source.getEndedAt());
        safe.setElapsedUs(source.getElapsedUs()); safe.setAffectedRows(source.getAffectedRows());
        safe.setResultCount(source.getResultCount());
        safe.setResultSummary(source.getResultCount() != null ? source.getResultCount() + " rows returned"
                : source.getAffectedRows() != null ? source.getAffectedRows() + " rows affected"
                : scrub(source.getResultSummary(), secrets));
        safe.setBackendRequestId(scrub(source.getBackendRequestId(), secrets));
        safe.setComment(scrub(source.getComment(), secrets)); safe.setPurpose(scrub(source.getPurpose(), secrets));
        safe.setAuditReason(scrub(source.getAuditReason(), secrets)); safe.setTraceChain(trace(source.getTraceChain(), secrets));
        safe.setMutationLineage(trace(source.getMutationLineage(), secrets));
        safe.setStatementOperation(source.getStatementOperation());
        if (orphanedDebug) {
            safe.setComment(hideIntent(source.getComment())); safe.setPurpose(hideIntent(source.getPurpose()));
            safe.setAuditReason(hideIntent(source.getAuditReason()));
            safe.setBackendRequestId(hideIntent(source.getBackendRequestId()));
            if (source.getResultCount() == null && source.getAffectedRows() == null)
                safe.setResultSummary(hideIntent(source.getResultSummary()));
            safe.setTraceChain(source.getTraceChain() == null ? List.of() : source.getTraceChain().stream()
                    .map(node -> new TraceNode(node.getKind(), hideIntent(node.getName()), node.getEntityId(), hideIntent(node.getComment()))).toList());
            safe.setMutationLineage(source.getMutationLineage().stream()
                    .map(node -> new TraceNode(node.getKind(), hideIntent(node.getName()), node.getEntityId(), hideIntent(node.getComment()))).toList());
        }
        String sql = source.getParameterizedQuery();
        safe.setParameterizedQuery(sql);
        safe.setParameters(values); safe.setParameterMasked(masked);
        safe.setParameterLogPolicies(policies); safe.setGeneratedSql(source.isGeneratedSql());
        boolean anyMasked = masked.contains(true);
        safe.setLogMode(allow ? anyMasked ? "DEBUG PARTIALLY MASKED" : "DEBUG PLAINTEXT" : anyMasked ? "MASKED" : "PLAIN");
        boolean unsafeInline = !source.isGeneratedSql() && sql != null
                && (sql.matches("(?s).*[0-9'\"`$].*") || sql.contains("--") || sql.contains("/*"));
        String omission = sql == null ? "missing_sql" : invalidPolicies ? "policy_count_mismatch"
                : invalidFlags ? "mask_count_mismatch"
                : unsafeInline && (!allow || credential(sql)) ? "untrusted_inline_sql" : null;
        if (source.getSqlOmissionReason() != null) {
            omission = Set.of("missing_sql", "policy_count_mismatch", "mask_count_mismatch", "untrusted_inline_sql",
                    "unsupported_literal_or_binding_mismatch").contains(source.getSqlOmissionReason())
                    ? source.getSqlOmissionReason() : "unavailable_sql";
        }
        String rendered = null;
        if (omission == null) {
            try {
                rendered = SqlLogRenderer.render(sql, values.size(), i ->
                        SqlLogRenderer.literal(values.get(i), source.getBackend())
                                + (masked.get(i) ? " /* masked */" : ""), source.getBackend());
            } catch (IllegalArgumentException failure) {
                omission = "unsupported_literal_or_binding_mismatch";
            }
        }
        if (omission != null) {
            safe.setLogMode("OMITTED");
            safe.setSqlOmissionReason(omission);
            safe.setParameterizedQuery("[REDACTED SQL; NOT REPLAYABLE]");
            safe.setDebugQuery("-- TeaQL SQL OMITTED; NOT REPLAYABLE; reason=" + omission);
        } else {
            String header = allow ? DEBUG_LABEL : "-- TeaQL " + safe.getLogMode() + "\n";
            if (anyMasked) header += (allow ? "-- DEBUG PARTIALLY MASKED; " : "-- ") + "NOT REPLAYABLE; masked_parameters="
                    + masked.stream().filter(Boolean::booleanValue).count() + "\n";
            safe.setDebugQuery(header + rendered);
        }
        if (allow && safe.getLogMode().startsWith("DEBUG"))
            SqlLogProjectionCache.remember(safe, sql(source, false));
        return safe;
    }

    private static String hideIntent(String value) { return value == null || value.isEmpty() ? value : REDACTED; }

    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key, copyValue(item)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof Iterable<?> items) {
            List<Object> copy = new ArrayList<>();
            items.forEach(item -> copy.add(copyValue(item)));
            return Collections.unmodifiableList(copy);
        }
        if (value != null && value.getClass().isArray()) {
            int count = java.lang.reflect.Array.getLength(value);
            Object copy = java.lang.reflect.Array.newInstance(value.getClass().getComponentType(), count);
            for (int i = 0; i < count; i++) java.lang.reflect.Array.set(copy, i, copyValue(java.lang.reflect.Array.get(value, i)));
            return copy;
        }
        if (value instanceof java.util.Date date) return date.clone();
        return value;
    }

    public static RawAuditEvent audit(RawAuditEvent source, boolean allow) {
        return audit(source, allow, null);
    }

    public static RawAuditEvent audit(RawAuditEvent source, boolean allow, io.teaql.core.SqlIntentRedactions redactions) {
        List<Object> secrets = new ArrayList<>();
        if (redactions != null) redactions.appendTo(secrets, allow);
        List<AuditFieldChange> changes = source.changes().stream().map(change -> {
            boolean mask = !allow || credential(change.field()) || hasCredentials(change.oldValue()) || hasCredentials(change.newValue());
            if (!mask) return change;
            secrets.add(change.oldValue()); secrets.add(change.newValue());
            return new AuditFieldChange(change.field(), change.oldValue() == null ? null : REDACTED,
                    change.newValue() == null ? null : REDACTED);
        }).toList();
        List<Object> intentValues = new ArrayList<>(secrets);
        if (source.entityId() != null) intentValues.add(source.entityId());
        return new RawAuditEvent(source.kind(), source.entityType(), source.entityId(), changes,
                trace(source.traceChain(), intentValues), scrub(source.actor(), intentValues), source.category(),
                scrub(source.reason(), intentValues), source.resultingVersion(), source.occurredAt(),
                source.governance());
    }
}
