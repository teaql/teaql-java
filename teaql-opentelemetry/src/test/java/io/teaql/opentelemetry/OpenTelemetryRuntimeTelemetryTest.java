package io.teaql.opentelemetry;

import static org.junit.Assert.*;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.teaql.runtime.RuntimeTelemetry;
import java.util.Map;
import org.junit.Test;

public class OpenTelemetryRuntimeTelemetryTest {
    @Test
    public void exportsSafeSpanAndMetricsThroughOfficialSdk() {
        InMemorySpanExporter spans = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spans))
                .build();
        InMemoryMetricReader metrics = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metrics)
                .build();
        InMemoryLogRecordExporter logs = InMemoryLogRecordExporter.create();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(logs))
                .build();
        OpenTelemetryRuntimeTelemetry telemetry = new OpenTelemetryRuntimeTelemetry(
                tracerProvider.get("io.teaql.runtime"),
                meterProvider.get("io.teaql.runtime"),
                loggerProvider.get("io.teaql.runtime"));

        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", "School.list", Map.of(
                        "teaql.entity.type", "School",
                        "teaql.entity.id", 42L)));
        RuntimeTelemetry.Scope providerScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("provider", "sqlite.query", Map.of(
                        "teaql.provider.kind", "sqlite",
                        "teaql.provider.operation", "query")));
        providerScope.success();
        scope.success(Map.of("teaql.result.cardinality", 1));

        assertEquals(2, spans.getFinishedSpanItems().size());
        var span = spans.getFinishedSpanItems().stream()
                .filter(item -> "teaql.query".equals(item.getName())).findFirst().orElseThrow();
        var providerSpan = spans.getFinishedSpanItems().stream()
                .filter(item -> "teaql.provider".equals(item.getName())).findFirst().orElseThrow();
        assertEquals("teaql.query", span.getName());
        assertEquals("School", span.getAttributes().get(AttributeKey.stringKey("teaql.entity.type")));
        assertNull(span.getAttributes().get(AttributeKey.longKey("teaql.entity.id")));
        assertEquals(Long.valueOf(1),
                span.getAttributes().get(AttributeKey.longKey("teaql.result.cardinality")));
        assertEquals(span.getSpanId(), providerSpan.getParentSpanId());
        assertTrue(metrics.collectAllMetrics().stream().anyMatch(metric ->
                "teaql.runtime.operation.duration".equals(metric.getName())));
        assertTrue(metrics.collectAllMetrics().stream().anyMatch(metric ->
                "teaql.runtime.operation.count".equals(metric.getName())));
        assertEquals(2, logs.getFinishedLogRecordItems().size());
        var log = logs.getFinishedLogRecordItems().stream()
                .filter(item -> "query".equals(item.getAttributes().get(
                        AttributeKey.stringKey("teaql.operation.family"))))
                .findFirst().orElseThrow();
        assertEquals("TeaQL runtime operation completed", log.getBody().asString());
        assertEquals("School.list", log.getAttributes().get(
                AttributeKey.stringKey("teaql.operation.name")));
        assertNull(log.getAttributes().get(AttributeKey.longKey("teaql.entity.id")));
        assertEquals(span.getTraceId(), log.getSpanContext().getTraceId());
        assertEquals(span.getSpanId(), log.getSpanContext().getSpanId());

        tracerProvider.close();
        meterProvider.close();
        loggerProvider.close();
    }
}
