package io.teaql.opentelemetry;

import static org.junit.Assert.*;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
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
        OpenTelemetryRuntimeTelemetry telemetry = new OpenTelemetryRuntimeTelemetry(
                tracerProvider.get("io.teaql.runtime"),
                meterProvider.get("io.teaql.runtime"));

        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", "School.list", Map.of(
                        "teaql.entity.type", "School",
                        "teaql.entity.id", 42L)));
        scope.success(Map.of("teaql.result.cardinality", 1));

        assertEquals(1, spans.getFinishedSpanItems().size());
        var span = spans.getFinishedSpanItems().get(0);
        assertEquals("teaql.query", span.getName());
        assertEquals("School", span.getAttributes().get(AttributeKey.stringKey("teaql.entity.type")));
        assertNull(span.getAttributes().get(AttributeKey.longKey("teaql.entity.id")));
        assertEquals(Long.valueOf(1),
                span.getAttributes().get(AttributeKey.longKey("teaql.result.cardinality")));
        assertTrue(metrics.collectAllMetrics().stream().anyMatch(metric ->
                "teaql.runtime.operation.duration".equals(metric.getName())));
        assertTrue(metrics.collectAllMetrics().stream().anyMatch(metric ->
                "teaql.runtime.operation.count".equals(metric.getName())));

        tracerProvider.close();
        meterProvider.close();
    }
}
