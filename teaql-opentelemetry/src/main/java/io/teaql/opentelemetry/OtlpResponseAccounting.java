package io.teaql.opentelemetry;

import com.google.protobuf.CodedInputStream;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.teaql.runtime.TelemetryStartup;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Receiver-reported rejection is separate from local failed batch-entry units. */
final class OtlpResponseAccounting {
    static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    enum State { PARTIAL_SUCCESS, WARNING, INVALID_RESPONSE }
    private final AtomicLong[] rejected = { new AtomicLong(), new AtomicLong(), new AtomicLong() };
    private final AtomicLong[][] responses = new AtomicLong[3][State.values().length];
    private final AtomicBoolean[][] announced = new AtomicBoolean[3][State.values().length];

    OtlpResponseAccounting() {
        for (var signal : ExportLossAccounting.Signal.values()) for (var state : State.values()) {
            responses[signal.ordinal()][state.ordinal()] = new AtomicLong();
            announced[signal.ordinal()][state.ordinal()] = new AtomicBoolean();
        }
    }

    long rejected(ExportLossAccounting.Signal signal) { return rejected[signal.ordinal()].get(); }
    long responses(ExportLossAccounting.Signal signal, State state) { return responses[signal.ordinal()][state.ordinal()].get(); }

    boolean receive(ExportLossAccounting.Signal signal, byte[] body) {
        try {
            // An empty protobuf response has no encoded fields; some senders
            // represent that valid empty body as null.
            if (body == null) body = new byte[0];
            if (body.length > MAX_RESPONSE_BYTES) throw new IOException();
            CodedInputStream input = CodedInputStream.newInstance(body);
            input.setSizeLimit(MAX_RESPONSE_BYTES); input.setRecursionLimit(16);
            boolean partial; boolean warning; long count;
            switch (signal) {
                case TRACES -> {
                    var reply = ExportTraceServiceResponse.parseFrom(input);
                    partial = reply.hasPartialSuccess(); count = reply.getPartialSuccess().getRejectedSpans();
                    warning = !reply.getPartialSuccess().getErrorMessageBytes().isEmpty();
                }
                case LOGS -> {
                    var reply = ExportLogsServiceResponse.parseFrom(input);
                    partial = reply.hasPartialSuccess(); count = reply.getPartialSuccess().getRejectedLogRecords();
                    warning = !reply.getPartialSuccess().getErrorMessageBytes().isEmpty();
                }
                case METRICS -> {
                    var reply = ExportMetricsServiceResponse.parseFrom(input);
                    partial = reply.hasPartialSuccess(); count = reply.getPartialSuccess().getRejectedDataPoints();
                    warning = !reply.getPartialSuccess().getErrorMessageBytes().isEmpty();
                }
                default -> throw new IOException();
            }
            if (!input.isAtEnd() || count < 0) throw new IOException();
            // Collector 0.159 encodes a default empty partial_success (0a00)
            // on full acceptance. Only actual rejection or diagnostic text is
            // actionable; never manufacture a warning from an empty wrapper.
            if (partial && (count > 0 || warning)) {
                if (count > 0) rejected[signal.ordinal()].updateAndGet(value ->
                        value > Long.MAX_VALUE - count ? Long.MAX_VALUE : value + count);
                record(signal, count > 0 ? State.PARTIAL_SUCCESS : State.WARNING);
            }
            return true;
        } catch (Throwable ignored) {
            record(signal, State.INVALID_RESPONSE);
            return false;
        }
    }

    void invalid(ExportLossAccounting.Signal signal) { record(signal, State.INVALID_RESPONSE); }

    private void record(ExportLossAccounting.Signal signal, State state) {
        responses[signal.ordinal()][state.ordinal()].incrementAndGet();
        if (announced[signal.ordinal()][state.ordinal()].compareAndSet(false, true)) TelemetryStartup.notice(
                "TeaQL telemetry: receiver response; signal=" + signal.name().toLowerCase(java.util.Locale.ROOT)
                        + "; state=" + state.name().toLowerCase(java.util.Locale.ROOT)
                        + "; remote details masked; see teaql.telemetry.export.rejected and teaql.telemetry.export.response; business outcome unchanged.");
    }

    void bind(Meter meter) {
        meter.counterBuilder("teaql.telemetry.export.rejected")
                .setDescription("Receiver-reported rejected spans, log records or metric data points; not failed MetricData objects")
                .setUnit("{item}").buildWithCallback(observer -> {
                    for (var signal : ExportLossAccounting.Signal.values()) observer.record(rejected(signal),
                            Attributes.builder().put("teaql.telemetry.signal", signal.name().toLowerCase(java.util.Locale.ROOT))
                                    .put("teaql.telemetry.rejected.unit", signal == ExportLossAccounting.Signal.METRICS
                                            ? "data_point" : signal == ExportLossAccounting.Signal.TRACES ? "span" : "log_record").build());
                });
        meter.counterBuilder("teaql.telemetry.export.response").setUnit("{response}")
                .setDescription("Non-full OTLP acknowledgements; no remote body or message labels")
                .buildWithCallback(observer -> {
                    for (var signal : ExportLossAccounting.Signal.values()) for (var state : State.values())
                        observer.record(responses(signal, state), Attributes.builder()
                                .put("teaql.telemetry.signal", signal.name().toLowerCase(java.util.Locale.ROOT))
                                .put("teaql.telemetry.response.state", state.name().toLowerCase(java.util.Locale.ROOT)).build());
                });
    }
}
