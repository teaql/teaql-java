package io.teaql.runtime;

import io.teaql.core.http.HttpObservation;
import io.teaql.core.http.HttpRequestFailure;
import java.util.Map;

/** Bridges the safe core HTTP operation to the runtime's telemetry module. */
final class RuntimeHttpObservation implements HttpObservation {
    private final RuntimeTelemetry telemetry;

    RuntimeHttpObservation(RuntimeTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override public Scope start(Operation operation) {
        var scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("http", operation.profile(), Map.of(
                        "teaql.http.method", operation.method().name(),
                        "teaql.http.comment", operation.comment(),
                        "teaql.http.intent", operation.intent(),
                        "teaql.http.trace_chain", operation.traceChain())));
        return (outcome, attempts, elapsedMillis) -> {
            var completion = Map.<String, Object>of("teaql.http.outcome", outcome,
                    "teaql.http.attempts", attempts,
                    "teaql.http.elapsed_ms", elapsedMillis);
            if ("success".equals(outcome)) {
                scope.success(completion);
            } else {
                scope.failureWithAttributes(
                        new HttpRequestFailure(HttpRequestFailure.Code.UPSTREAM_FAILURE), completion);
            }
        };
    }
}
