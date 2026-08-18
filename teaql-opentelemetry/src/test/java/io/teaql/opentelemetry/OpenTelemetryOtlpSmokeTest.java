package io.teaql.opentelemetry;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.teaql.runtime.RuntimeTelemetry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Assume;
import org.junit.Test;

public class OpenTelemetryOtlpSmokeTest {
    @Test
    public void exportsQueryTraceMetricAndLogThroughOtlpHttp() {
        String serviceName = System.getenv("TEAQL_OTLP_SERVICE_NAME");
        Assume.assumeNotNull(serviceName);
        String endpoint = System.getenv().getOrDefault(
                "OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318");
        boolean expectExportFailure = "1".equals(System.getenv("TEAQL_EXPECT_EXPORT_FAILURE"));
        String runId = serviceName.substring(serviceName.lastIndexOf('-') + 1);
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.builder()
                .put("service.name", serviceName)
                .put("service.instance.id", runId)
                .put("teaql.runtime.language", "java")
                .put("teaql.conformance.run_id", runId)
                .build()));
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                        .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpHttpSpanExporter.builder()
                                .setEndpoint(endpoint + "/v1/traces")
                                .setTimeout(Duration.ofSeconds(1)).build())
                        .setMaxQueueSize(64).setMaxExportBatchSize(16)
                        .setExporterTimeout(Duration.ofSeconds(2)).build())
                .build();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                        .registerMetricReader(PeriodicMetricReader.builder(OtlpHttpMetricExporter.builder()
                                .setEndpoint(endpoint + "/v1/metrics")
                                .setTimeout(Duration.ofSeconds(1)).build())
                        .setInterval(Duration.ofSeconds(1)).build())
                .build();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                                OtlpHttpLogRecordExporter.builder()
                                        .setEndpoint(endpoint + "/v1/logs")
                                        .setTimeout(Duration.ofSeconds(1)).build())
                        .setMaxQueueSize(64).setMaxExportBatchSize(16)
                        .setExporterTimeout(Duration.ofSeconds(2)).build())
                .build();
        OpenTelemetryRuntimeTelemetry telemetry = new OpenTelemetryRuntimeTelemetry(
                tracerProvider.get("io.teaql.runtime"),
                meterProvider.get("io.teaql.runtime"),
                loggerProvider.get("io.teaql.runtime"));

        complete(telemetry, "query", "ConformanceProbe.list", Map.of(
                "teaql.entity.type", "ConformanceProbe"));
        complete(telemetry, "mutation", "ConformanceProbe.update", Map.of(
                "teaql.entity.type", "ConformanceProbe", "teaql.mutation.kind", "update"));
        complete(telemetry, "relation_load", "ConformanceProbe.children", Map.of(
                "teaql.entity.type", "ConformanceProbe", "teaql.relation.name", "children"));
        complete(telemetry, "provider", "sqlite.query", Map.of(
                "teaql.provider.kind", "sqlite", "teaql.provider.operation", "query"));
        complete(telemetry, "cache", "local.get", Map.of("teaql.cache.operation", "get"));
        complete(telemetry, "tfp", "server.query", Map.of("teaql.tfp.role", "server"));
        complete(telemetry, "audit", "ConformanceProbe.audit", Map.of(
                "teaql.entity.type", "ConformanceProbe",
                "teaql.mutation.kind", "update",
                "teaql.audit.changed_field_count", 1));

        boolean traceFlushed = tracerProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess();
        boolean metricFlushed = meterProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess();
        boolean logFlushed = loggerProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess();
        if (expectExportFailure) {
            assertFalse(traceFlushed && metricFlushed && logFlushed);
        } else {
            assertTrue(traceFlushed && metricFlushed && logFlushed);
        }
        tracerProvider.close();
        meterProvider.close();
        loggerProvider.close();
    }

    private static void complete(OpenTelemetryRuntimeTelemetry telemetry, String family,
            String name, Map<String, Object> attributes) {
        Map<String, Object> probeAttributes = new java.util.HashMap<>(attributes);
        probeAttributes.put("teaql.entity.id", "must-not-export");
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation(family, name, probeAttributes));
        Map<String, Object> completion = new java.util.HashMap<>();
        completion.put("teaql.result.cardinality", 1);
        if ("cache".equals(family)) {
            completion.put("teaql.cache.result", "hit");
        }
        scope.success(completion);
    }
}
