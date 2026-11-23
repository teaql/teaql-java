package io.teaql.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/** Local logging preserves the independently installed adapter and its capabilities. */
final class RuntimeLoggingTelemetry implements RuntimeTelemetry {
    // Framework-created policy errors retain their exact existing class and
    // message. A weak identity hint supplies local severity without retaining
    // payloads or changing the error seen by application/SDK consumers.
    private static final Map<io.teaql.core.TeaQLRuntimeException, Boolean> POLICY_REJECTIONS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    static io.teaql.core.TeaQLRuntimeException policyRejection(String message) {
        var error = new io.teaql.core.TeaQLRuntimeException(message);
        POLICY_REJECTIONS.put(error, Boolean.TRUE);
        return error;
    }
    private final RuntimeTelemetry delegate;
    private final RuntimeLogger logger;
    RuntimeLoggingTelemetry(RuntimeTelemetry delegate, RuntimeLogger logger) { this.delegate = delegate; this.logger = logger; }
    @Override public Scope start(Operation operation) {
        var original = RuntimeTelemetry.startSafely(delegate, operation);
        return new Scope() {
            private boolean ended;
            private boolean log(Throwable error) {
                if (ended) return false; ended = true;
                String category = error == null ? null : RuntimeTelemetry.errorCategory(error);
                if (error instanceof io.teaql.core.checker.CheckException || error instanceof io.teaql.core.RequestIntentException) category = "validation";
                if (error != null && error.getClass() == io.teaql.core.TeaQLRuntimeException.class && POLICY_REJECTIONS.containsKey(error)) category = "authorization";
                String level = error == null ? "INFO" : java.util.Set.of("validation", "authorization", "conflict").contains(category) ? "WARN" : "ERROR";
                String outcome = error == null ? "success" : error instanceof java.util.concurrent.CancellationException ? "cancelled" : "failure";
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("category", operation.family().toUpperCase(java.util.Locale.ROOT)); fields.put("outcome", outcome);
                if (category != null) fields.put("error_code", category);
                var correlation = original.correlation(); if (correlation != null) fields.put("trace_id", correlation.traceId());
                logger.write(level, "operation." + outcome, "TeaQL operation " + outcome, fields);
                return true;
            }
            @Override public synchronized void success(Map<String, Object> attributes) { if (log(null)) original.success(attributes); }
            @Override public synchronized void failure(Throwable error) { if (log(error)) original.failure(error); }
            @Override public synchronized void failureWithAttributes(Throwable error, Map<String, Object> attributes) { if (log(error)) original.failureWithAttributes(error, attributes); }
            @Override public io.teaql.core.SqlExecutionCorrelation correlation() { return original.correlation(); }
            @Override public void detach() { original.detach(); }
            @Override public PropagationScope activate() { return original.activate(); }
        };
    }
    @Override public boolean recordsSqlDiagnostics() { return delegate.recordsSqlDiagnostics(); }
    @Override public void recordDiagnostic(Diagnostic event) { delegate.recordDiagnostic(event); }
    @Override public void inject(Map<String, String> carrier) { delegate.inject(carrier); }
    @Override public PropagationScope extractAndActivate(Map<String, String> carrier) { return delegate.extractAndActivate(carrier); }
    @Override public void flush() { delegate.flush(); }
    @Override public void shutdown() { delegate.shutdown(); }
}
