package io.teaql.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.teaql.runtime.RuntimeTelemetry;
import java.util.Map;

/** OpenTelemetry bridge backed by application-owned Tracer and Meter providers. */
public final class OpenTelemetryRuntimeTelemetry implements RuntimeTelemetry {
    private final Tracer tracer;
    private final DoubleHistogram duration;
    private final LongCounter operations;
    private final Runnable flush;
    private final Runnable shutdown;

    public OpenTelemetryRuntimeTelemetry(Tracer tracer, Meter meter) {
        this(tracer, meter, () -> {}, () -> {});
    }

    public OpenTelemetryRuntimeTelemetry(
            Tracer tracer, Meter meter, Runnable flush, Runnable shutdown) {
        this.tracer = tracer;
        this.duration = meter.histogramBuilder("teaql.runtime.operation.duration")
                .setDescription("TeaQL runtime operation duration")
                .setUnit("ms")
                .build();
        this.operations = meter.counterBuilder("teaql.runtime.operation.count")
                .setDescription("Completed TeaQL runtime operations")
                .setUnit("{operation}")
                .build();
        this.flush = flush == null ? () -> {} : flush;
        this.shutdown = shutdown == null ? () -> {} : shutdown;
    }

    @Override
    public Scope start(Operation operation) {
        long startedAt = System.nanoTime();
        Span span = tracer.spanBuilder("teaql." + operation.family())
                .setAllAttributes(attributes(operation.attributes()))
                .startSpan();
        io.opentelemetry.context.Scope activation = span.makeCurrent();
        return new Scope() {
            private boolean ended;

            @Override
            public synchronized void success(Map<String, Object> completion) {
                if (ended) return;
                if (completion != null) {
                    completion.forEach((key, value) -> {
                        if ("teaql.result.cardinality".equals(key)
                                || "teaql.cache.result".equals(key)) {
                            setAttribute(span, key, value);
                        }
                    });
                }
                span.setStatus(StatusCode.OK);
                finish("success");
            }

            @Override
            public synchronized void failure(Throwable error) {
                if (ended) return;
                span.setAttribute("teaql.error.type",
                        error == null ? "unknown" : error.getClass().getSimpleName());
                span.setStatus(StatusCode.ERROR);
                finish("failure");
            }

            private void finish(String outcome) {
                ended = true;
                Attributes dimensions = Attributes.builder()
                        .put("teaql.operation.family", operation.family())
                        .put("teaql.operation.outcome", outcome)
                        .build();
                duration.record(Math.max(0d, (System.nanoTime() - startedAt) / 1_000_000d), dimensions);
                operations.add(1, dimensions);
                activation.close();
                span.end();
            }
        };
    }

    @Override
    public void flush() {
        flush.run();
    }

    @Override
    public void shutdown() {
        shutdown.run();
    }

    private static Attributes attributes(Map<String, Object> values) {
        AttributesBuilder builder = Attributes.builder();
        if (values != null) values.forEach((key, value) -> setAttribute(builder, key, value));
        return builder.build();
    }

    private static void setAttribute(AttributesBuilder builder, String key, Object value) {
        if (value instanceof String string) builder.put(key, string);
        else if (value instanceof Long number) builder.put(key, number);
        else if (value instanceof Integer number) builder.put(key, number.longValue());
        else if (value instanceof Double number) builder.put(key, number);
        else if (value instanceof Float number) builder.put(key, number.doubleValue());
        else if (value instanceof Boolean bool) builder.put(key, bool);
        else if (value instanceof Number number) builder.put(key, number.doubleValue());
    }

    private static void setAttribute(Span span, String key, Object value) {
        if (value instanceof String string) span.setAttribute(key, string);
        else if (value instanceof Long number) span.setAttribute(key, number);
        else if (value instanceof Integer number) span.setAttribute(key, number.longValue());
        else if (value instanceof Double number) span.setAttribute(key, number);
        else if (value instanceof Float number) span.setAttribute(key, number.doubleValue());
        else if (value instanceof Boolean bool) span.setAttribute(key, bool);
        else if (value instanceof Number number) span.setAttribute(key, number.doubleValue());
    }
}
