package io.teaql.runtime.log;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import io.teaql.core.UserContext;
import io.teaql.runtime.LogPrivacy;
import io.teaql.runtime.RawAuditEvent;
import io.teaql.runtime.RuntimeLogSink;
import io.teaql.runtime.RuntimeLogger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Optional formatting/custom-sink adapter using the runtime-owned JSONL destination. */
public class LogManager implements RuntimeLogSink {
    private static final LogManager INSTANCE = new LogManager();
    private LogManager() { }
    public static LogManager getInstance() { return INSTANCE; }

    @Override public boolean requiresSensitiveSqlData() {
        return LogConfig.getInstance().includesSensitiveSqlData();
    }

    private void custom(UserContext context, String content, RuntimeLogger logger) {
        try {
            CustomLogSink sink = resolveCustomSink(context);
            if (sink != null) sink.onLog(content);
        } catch (RuntimeException unavailable) {
            RuntimeLogger.failure(logger.logName(), "custom_log_sink_failed");
        }
    }

    @Override public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {
        if (!LogConfig.getInstance().shouldLogSql(metadata.getParameterizedQuery())) return;
        var logger = RuntimeLogger.forContext(context);
        var safe = LogPrivacy.sql(metadata, requiresSensitiveSqlData() && LogPrivacy.plaintextEnabled());
        try { custom(context, LogFormatterFactory.getFormatter().formatExecutionLog(safe), logger); }
        catch (RuntimeException unavailable) { RuntimeLogger.failure(logger.logName(), "log_projection_failed"); }
        logger.writeSql(safe, requiresSensitiveSqlData());
    }

    @Override public void writeAuditEvent(UserContext context, RawAuditEvent event) {
        List<FieldChange> changes = event.changes().stream()
                .map(change -> new FieldChange(change.field(), change.oldValue(), change.newValue())).toList();
        writeAuditLog(context, event.traceChain(), new AuditEvent(event.entityType(), event.entityId(), event.kind().name(), changes));
    }

    public void writeAuditLog(UserContext context, List<TraceNode> traceChain, AuditEvent event) {
        if (!LogConfig.getInstance().shouldLogAudit(event.getEntityType())) return;
        boolean allow = LogPrivacy.plaintextEnabled();
        List<Object> secrets = new ArrayList<>();
        List<FieldChange> safeChanges = event.getChanges().stream().map(change -> {
            if (allow && !LogPrivacy.credential(change.getField())
                    && !LogPrivacy.hasCredentials(change.getOldValue()) && !LogPrivacy.hasCredentials(change.getNewValue())) return change;
            secrets.add(change.getOldValue()); secrets.add(change.getNewValue());
            return new FieldChange(change.getField(), change.getOldValue() == null ? null : LogPrivacy.REDACTED,
                    change.getNewValue() == null ? null : LogPrivacy.REDACTED);
        }).toList();
        var safe = new AuditEvent(event.getEntityType(), event.getEntityId(), event.getMutationKind(), safeChanges);
        var safeTrace = LogPrivacy.trace(traceChain, secrets);
        var logger = RuntimeLogger.forContext(context);
        try { custom(context, LogFormatterFactory.getFormatter().formatAuditLog(safeTrace, safe), logger); }
        catch (RuntimeException unavailable) { RuntimeLogger.failure(logger.logName(), "log_projection_failed"); }
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("category", "AUDIT"); fields.put("outcome", "success"); fields.put("entity", safe.getEntityType());
        fields.put("entity_id", safe.getEntityId()); fields.put("fields", safeChanges); fields.put("trace_chain", safeTrace);
        logger.write("INFO", "audit.event", "TeaQL audit event", fields);
    }

    protected String readHeaderOrDefault(java.util.Scanner scanner) { return scanner.hasNext() ? scanner.next() : ""; }
    protected CustomLogSink resolveCustomSink(UserContext context) {
        return context == null ? null : context.capability(CustomLogSink.class);
    }
}
