package io.teaql.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.*;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlDiagnosticCorrelationTest {
    @Test public void lateSqlLogUsesCarriedIdentityAndMaskProjectionNotActiveSpanOrDebugMode() {
        var spans = InMemorySpanExporter.create();
        var logs = InMemoryLogRecordExporter.create();
        try (var traces = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spans)).build();
             var meters = SdkMeterProvider.builder().build();
             var logProvider = SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(logs)).build()) {
            var telemetry = new OpenTelemetryRuntimeTelemetry(traces.get("test"), meters.get("test"), logProvider.get("test"));
            var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .telemetry(telemetry).queryExecutionLogging(false).mutationExecutionLogging(false).build());
            var root = telemetry.start(new RuntimeTelemetry.Operation("query", "School.list", Map.of()));
            var first = telemetry.start(new RuntimeTelemetry.Operation("provider", "sqlite.statement", Map.of()));
            var firstIdentity = first.correlation();
            first.success();
            var second = telemetry.start(new RuntimeTelemetry.Operation("provider", "sqlite.statement", Map.of()));
            var secondIdentity = second.correlation();
            second.success();
            root.success();
            var unrelated = telemetry.start(new RuntimeTelemetry.Operation("query", "Other.list", Map.of()));
            String unrelatedId = Span.current().getSpanContext().getSpanId();
            context.recordExecutionMetadata(metadata(secondIdentity));
            var plaintextDebug = LogPrivacy.sql(metadata(firstIdentity), true);
            assertTrue(plaintextDebug.getDebugQuery().contains("PRIVATE-SQL-CANARY"));
            plaintextDebug.setSqlExecutionCorrelation(firstIdentity);
            context.recordExecutionMetadata(plaintextDebug);
            assertEquals(unrelatedId, Span.current().getSpanContext().getSpanId());
            unrelated.success();

            var sqlLogs = logs.getFinishedLogRecordItems().stream()
                    .filter(log -> log.getBodyValue().asString().equals("TeaQL SQL execution")).toList();
            assertEquals(2, sqlLogs.size());
            assertEquals(secondIdentity.spanId(), sqlLogs.get(0).getSpanContext().getSpanId());
            assertEquals(firstIdentity.spanId(), sqlLogs.get(1).getSpanContext().getSpanId());
            assertEquals(firstIdentity.traceId(), sqlLogs.get(1).getSpanContext().getTraceId());
            assertNotEquals(unrelatedId, sqlLogs.get(1).getSpanContext().getSpanId());
            for (var log : sqlLogs) {
                assertFalse(log.getAttributes().toString().contains("PRIVATE-SQL-CANARY"));
                assertFalse(log.getAttributes().toString().contains("9988776655"));
                assertFalse(log.getAttributes().toString().contains("DEBUG PLAINTEXT"));
                assertTrue(log.getAttributes().get(AttributeKey.stringKey("teaql.sql.debug")).contains("/* masked */"));
                assertTrue(spans.getFinishedSpanItems().stream().anyMatch(span -> span.getSpanId().equals(log.getSpanContext().getSpanId())));
            }
            assertEquals("show school details", sqlLogs.get(0).getAttributes().get(AttributeKey.stringKey("teaql.intent.purpose")));
            // Context canonicalization changes a debug projection's fingerprint. The existing
            // privacy policy conservatively redacts orphaned intent instead of trusting it.
            assertEquals("[REDACTED]", sqlLogs.get(1).getAttributes().get(AttributeKey.stringKey("teaql.intent.purpose")));
            var query = spans.getFinishedSpanItems().stream().filter(span -> "School.list".equals(
                    span.getAttributes().get(AttributeKey.stringKey("teaql.operation.name")))).findFirst().orElseThrow();
            assertEquals(2, spans.getFinishedSpanItems().stream().filter(span -> span.getParentSpanId().equals(query.getSpanId())).count());
        }
    }

    private static ExecutionMetadata metadata(SqlExecutionCorrelation identity) {
        var metadata = new ExecutionMetadata();
        metadata.setBackend("sqlite"); metadata.setOperation(DataServiceOperation.QUERY);
        metadata.setParameterizedQuery("SELECT id FROM school WHERE name = ?");
        metadata.setParameters(List.of("PRIVATE-SQL-CANARY"));
        metadata.setParameterLogPolicies(List.of(SqlParameterLogPolicy.MASKED));
        metadata.setGeneratedSql(true); metadata.setExecutionOutcome("success"); metadata.setResultCount(1);
        metadata.setPurpose("show school details"); metadata.setComment("load PRIVATE-SQL-CANARY");
        metadata.setTraceChain(List.of(new TraceNode(TraceKind.COMMENT, "School", "load PRIVATE-SQL-CANARY"),
                new TraceNode(TraceKind.PURPOSE, "School", "show school details"),
                new TraceNode(TraceKind.ENTITY, "School", 9988776655L, "")));
        metadata.setRequestTraceChain(List.of(new TraceNode(TraceKind.COMMENT, "School", "load PRIVATE-SQL-CANARY")));
        metadata.setSqlExecutionCorrelation(identity);
        return metadata;
    }

    @Test public void detachedSpanLogsKeepIdentityAndDoNotLeakThreadActivation() {
        var logs = InMemoryLogRecordExporter.create();
        try (var traces = SdkTracerProvider.builder().build(); var meters = SdkMeterProvider.builder().build();
             var logProvider = SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(logs)).build()) {
            var telemetry = new OpenTelemetryRuntimeTelemetry(traces.get("test"), meters.get("test"), logProvider.get("test"));
            var span = telemetry.start(new RuntimeTelemetry.Operation("provider", "cursor", Map.of()));
            var identity = span.correlation();
            span.detach();
            assertFalse(Span.current().getSpanContext().isValid());
            try (var active = span.activate()) { assertEquals(identity.spanId(), Span.current().getSpanContext().getSpanId()); }
            assertFalse(Span.current().getSpanContext().isValid());
            span.success();
            assertEquals(identity.spanId(), logs.getFinishedLogRecordItems().get(0).getSpanContext().getSpanId());
            assertFalse(Span.current().getSpanContext().isValid());
        }
    }
}
