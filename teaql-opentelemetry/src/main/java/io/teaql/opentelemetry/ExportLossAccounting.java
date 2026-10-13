package io.teaql.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.teaql.runtime.TelemetryStartup;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Counts failed batch entries, not guessed delivered records or retained retries. */
final class ExportLossAccounting {
    enum Signal { TRACES, LOGS, METRICS }
    enum Reason { EXPORT_FAILED, QUEUE_OVERFLOW, SHUTDOWN }
    private final AtomicLong[][] lost = new AtomicLong[Signal.values().length][Reason.values().length];
    private final AtomicBoolean[][] announced = new AtomicBoolean[Signal.values().length][Reason.values().length];
    private final OtlpResponseAccounting responseAccounting = new OtlpResponseAccounting();

    OtlpResponseAccounting responses() { return responseAccounting; }

    ExportLossAccounting() {
        for (Signal signal : Signal.values()) for (Reason reason : Reason.values()) {
            lost[signal.ordinal()][reason.ordinal()] = new AtomicLong();
            announced[signal.ordinal()][reason.ordinal()] = new AtomicBoolean();
        }
    }

    long lost(Signal signal) {
        long total = 0;
        for (Reason reason : Reason.values()) total += lost(signal, reason);
        return total;
    }

    long lost(Signal signal, Reason reason) { return lost[signal.ordinal()][reason.ordinal()].get(); }

    void lost(Signal signal, Reason reason, int size) {
        if (size <= 0) return;
        lost[signal.ordinal()][reason.ordinal()].addAndGet(size);
        if (announced[signal.ordinal()][reason.ordinal()].compareAndSet(false, true)) TelemetryStartup.notice(
                "TeaQL telemetry: export entries lost; signal=" + signal.name().toLowerCase(java.util.Locale.ROOT)
                        + "; reason=" + reason.name().toLowerCase(java.util.Locale.ROOT)
                        + "; see teaql.telemetry.export.lost; business outcome unchanged.");
    }

    void bind(Meter meter) {
        responseAccounting.bind(meter);
        meter.counterBuilder("teaql.telemetry.export.lost")
                .setDescription("Failed export entries; metrics count MetricData objects, not points")
                .setUnit("{entry}").buildWithCallback(observation -> {
                    for (Signal signal : Signal.values()) for (Reason reason : Reason.values()) {
                        // Metrics collect cumulative snapshots, not a span/log event queue.
                        if (signal == Signal.METRICS && reason != Reason.EXPORT_FAILED) continue;
                        observation.record(lost(signal, reason), Attributes.builder()
                                .put("teaql.telemetry.signal", signal.name().toLowerCase(java.util.Locale.ROOT))
                                .put("teaql.telemetry.loss.reason", reason.name().toLowerCase(java.util.Locale.ROOT)).build());
                    }
                });
    }

    private CompletableResultCode watch(Signal signal, int size, Supplier<CompletableResultCode> export,
            BoundedSignalAdmission admission) {
        CompletableResultCode result;
        try { result = export.get(); }
        catch (Throwable ignored) { result = CompletableResultCode.ofFailure(); }
        if (result == null) result = CompletableResultCode.ofFailure();
        CompletableResultCode outcome = result;
        outcome.whenComplete(() -> {
            if (admission != null) admission.release(size);
            if (!outcome.isSuccess()) lost(signal, Reason.EXPORT_FAILED, size);
        });
        return outcome;
    }

    SpanExporter spans(SpanExporter delegate) {
        return spans(delegate, null);
    }

    SpanExporter spans(SpanExporter delegate, BoundedSignalAdmission admission) {
        return new SpanExporter() {
            @Override public CompletableResultCode export(Collection<SpanData> values) {
                return watch(Signal.TRACES, values.size(), () -> delegate.export(values), admission);
            }
            @Override public CompletableResultCode flush() { return delegate.flush(); }
            @Override public CompletableResultCode shutdown() { return delegate.shutdown(); }
        };
    }

    LogRecordExporter logs(LogRecordExporter delegate) {
        return logs(delegate, null);
    }

    LogRecordExporter logs(LogRecordExporter delegate, BoundedSignalAdmission admission) {
        return new LogRecordExporter() {
            @Override public CompletableResultCode export(Collection<LogRecordData> values) {
                return watch(Signal.LOGS, values.size(), () -> delegate.export(values), admission);
            }
            @Override public CompletableResultCode flush() { return delegate.flush(); }
            @Override public CompletableResultCode shutdown() { return delegate.shutdown(); }
        };
    }

    MetricExporter metrics(MetricExporter delegate) {
        return new MetricExporter() {
            @Override public CompletableResultCode export(Collection<MetricData> values) {
                return watch(Signal.METRICS, values.size(), () -> delegate.export(values), null);
            }
            @Override public AggregationTemporality getAggregationTemporality(InstrumentType type) { return delegate.getAggregationTemporality(type); }
            @Override public Aggregation getDefaultAggregation(InstrumentType type) { return delegate.getDefaultAggregation(type); }
            @Override public MemoryMode getMemoryMode() { return delegate.getMemoryMode(); }
            @Override public CompletableResultCode flush() { return delegate.flush(); }
            @Override public CompletableResultCode shutdown() { return delegate.shutdown(); }
        };
    }
}
