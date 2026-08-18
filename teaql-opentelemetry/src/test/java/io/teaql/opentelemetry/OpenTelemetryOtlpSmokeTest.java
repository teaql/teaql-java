package io.teaql.opentelemetry;

import static org.junit.Assert.assertTrue;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
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
        String runId = serviceName.substring(serviceName.lastIndexOf('-') + 1);
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.builder()
                .put("service.name", serviceName)
                .put("service.instance.id", runId)
                .put("teaql.runtime.language", "java")
                .put("teaql.conformance.run_id", runId)
                .build()));
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(SimpleSpanProcessor.create(OtlpHttpSpanExporter.builder()
                        .setEndpoint(endpoint + "/v1/traces").build()))
                .build();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(OtlpHttpMetricExporter.builder()
                                .setEndpoint(endpoint + "/v1/metrics").build())
                        .setInterval(Duration.ofSeconds(1)).build())
                .build();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(
                        OtlpHttpLogRecordExporter.builder()
                                .setEndpoint(endpoint + "/v1/logs").build()))
                .build();
        OpenTelemetryRuntimeTelemetry telemetry = new OpenTelemetryRuntimeTelemetry(
                tracerProvider.get("io.teaql.runtime"),
                meterProvider.get("io.teaql.runtime"),
                loggerProvider.get("io.teaql.runtime"));

        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", "ConformanceProbe.list", Map.of(
                        "teaql.entity.type", "ConformanceProbe",
                        "teaql.entity.id", "must-not-export")));
        scope.success(Map.of("teaql.result.cardinality", 1));

        assertTrue(tracerProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess());
        assertTrue(meterProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess());
        assertTrue(loggerProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess());
        tracerProvider.close();
        meterProvider.close();
        loggerProvider.close();
    }
}
