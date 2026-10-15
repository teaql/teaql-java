package io.teaql.core.http;

/** Optional Context capability for a safe HTTP operation lifecycle. */
public interface HttpObservation {
    Scope start(Operation operation);

    /** Contains no URL, headers, body, entity ID, or provider error text. */
    record Operation(String profile, HttpRequestService.Method method, String comment,
                     String intent, String traceChain) {
        @Override public String toString() {
            return "HttpOperation[profile=" + profile + ", method=" + method + ", details=masked]";
        }
    }

    interface Scope {
        void finish(String outcome, int attempts, long elapsedMillis);
    }
}
