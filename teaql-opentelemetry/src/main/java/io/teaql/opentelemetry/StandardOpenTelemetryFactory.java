package io.teaql.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
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
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.StandardTelemetryFactory;
import io.teaql.runtime.TelemetryStartup;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Builds an owned pipeline without registering a GlobalOpenTelemetry instance. */
public final class StandardOpenTelemetryFactory implements StandardTelemetryFactory {
    @Override
    public RuntimeTelemetry create(String serviceName, URI endpoint) {
        return create(serviceName, endpoint, new ExportLossAccounting());
    }

    RuntimeTelemetry create(String serviceName, URI endpoint, ExportLossAccounting losses) {
        return create(serviceName, endpoint, losses, 2048);
    }

    RuntimeTelemetry create(String serviceName, URI endpoint, ExportLossAccounting losses, int queueCapacity) {
        String base = endpoint.toString().replaceAll("/$", "");
        var components = new OwnedHttpComponents(StandardOpenTelemetryFactory.class.getClassLoader(), losses.responses());
        // SDK 1.65 uses null to disable its HTTP retry policy; verify with actual
        // failing HTTP requests rather than accepting the default retry behavior.
        Resource resource = Resource.create(Attributes.builder().put("service.name", serviceName)
                .put("teaql.runtime.language", "java").build());
        var traceAdmission = new BoundedSignalAdmission(queueCapacity, losses, ExportLossAccounting.Signal.TRACES);
        var logAdmission = new BoundedSignalAdmission(queueCapacity, losses, ExportLossAccounting.Signal.LOGS);
        SdkTracerProvider traces = SdkTracerProvider.builder().setResource(resource).setSampler(Sampler.alwaysOn())
                .addSpanProcessor(traceAdmission.spans(BatchSpanProcessor.builder(losses.spans(OtlpHttpSpanExporter.builder()
                        .setEndpoint(base + "/v1/traces").setComponentLoader(components).setRetryPolicy(null)
                        .setConnectTimeout(Duration.ofSeconds(3)).setTimeout(Duration.ofSeconds(3)).build(), traceAdmission))
                        .setMaxQueueSize(queueCapacity).setMaxExportBatchSize(Math.min(128, queueCapacity))
                        .setExporterTimeout(Duration.ofSeconds(5)).build())).build();
        SdkMeterProvider metrics = SdkMeterProvider.builder().setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(losses.metrics(OtlpHttpMetricExporter.builder()
                        .setEndpoint(base + "/v1/metrics").setComponentLoader(components).setRetryPolicy(null)
                        .setConnectTimeout(Duration.ofSeconds(3)).setTimeout(Duration.ofSeconds(3)).build()))
                        .setInterval(Duration.ofSeconds(10)).build()).build();
        losses.bind(metrics.get("io.teaql.runtime"));
        SdkLoggerProvider logs = SdkLoggerProvider.builder().setResource(resource)
                .addLogRecordProcessor(logAdmission.logs(BatchLogRecordProcessor.builder(losses.logs(OtlpHttpLogRecordExporter.builder()
                        .setEndpoint(base + "/v1/logs").setComponentLoader(components).setRetryPolicy(null)
                        .setConnectTimeout(Duration.ofSeconds(3)).setTimeout(Duration.ofSeconds(3)).build(), logAdmission))
                        .setMaxQueueSize(queueCapacity).setMaxExportBatchSize(Math.min(128, queueCapacity))
                        .setExporterTimeout(Duration.ofSeconds(5)).build())).build();
        AtomicBoolean closed = new AtomicBoolean();
        Runnable flush = () -> {
            var result = io.opentelemetry.sdk.common.CompletableResultCode.ofAll(java.util.List.of(
                    traces.forceFlush(), logs.forceFlush(), metrics.forceFlush()));
            if (!result.join(6, TimeUnit.SECONDS).isSuccess()) TelemetryStartup.notice(
                    "TeaQL telemetry: export incomplete; business outcome unchanged.");
        };
        Runnable shutdown = () -> {
            if (!closed.compareAndSet(false, true)) return;
            var result = io.opentelemetry.sdk.common.CompletableResultCode.ofAll(java.util.List.of(
                    traces.shutdown(), logs.shutdown(), metrics.shutdown()));
            if (!result.join(6, TimeUnit.SECONDS).isSuccess()) TelemetryStartup.notice(
                    "TeaQL telemetry: owned shutdown incomplete; business outcome unchanged.");
        };
        return new OpenTelemetryRuntimeTelemetry(traces.get("io.teaql.runtime"),
                metrics.get("io.teaql.runtime"), logs.get("io.teaql.runtime"), flush, shutdown,
                ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
    }
}
