package io.teaql.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Provider-neutral, fail-open lifecycle telemetry for TeaQL runtime operations. */
public interface RuntimeTelemetry {
    Set<String> FORBIDDEN_ATTRIBUTES = Set.of(
            "teaql.entity.id",
            "teaql.user.id",
            "teaql.tenant.id",
            "teaql.query.parameters",
            "teaql.field.values",
            "teaql.audit.reason",
            "db.query.parameter_values",
            "http.request.body",
            "url.full");

    Scope start(Operation operation);

    default void inject(Map<String, String> carrier) {}

    default PropagationScope extractAndActivate(Map<String, String> carrier) {
        return NoopPropagationScope.INSTANCE;
    }

    default void flush() {}

    default void shutdown() {}

    /** Stable category derived from a native error type, never its message. */
    static String errorCategory(Throwable error) {
        return errorCategory(error == null ? "unknown" : error.getClass().getSimpleName());
    }

    static String errorCategory(String errorType) {
        String type = errorType == null ? "unknown" : errorType.toLowerCase(java.util.Locale.ROOT);
        if (containsAny(type, "timeout", "deadline")) return "timeout";
        if (containsAny(type, "authentication", "authorization", "unauthorized", "forbidden", "permission")) return "authorization";
        if (containsAny(type, "validation", "invalidargument", "valueerror", "parse", "format")) return "validation";
        if (containsAny(type, "conflict", "optimistic", "version", "duplicate", "alreadyexists")) return "conflict";
        if (containsAny(type, "transport", "network", "connection", "socket", "http", "ioexception")) return "transport";
        if (containsAny(type, "provider", "sql", "database", "jdbc")) return "provider";
        return "internal";
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    record Operation(String family, String name, Map<String, Object> attributes) {
        public Operation {
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("teaql.operation.family", family);
            safe.put("teaql.operation.name", name);
            if (attributes != null) {
                attributes.forEach((key, value) -> {
                    if (!FORBIDDEN_ATTRIBUTES.contains(key) && isSafeValue(value)) {
                        safe.put(key, value);
                    }
                });
            }
            attributes = Collections.unmodifiableMap(safe);
        }

        private static boolean isSafeValue(Object value) {
            return value instanceof String || value instanceof Number || value instanceof Boolean;
        }
    }

    interface Scope {
        void success(Map<String, Object> attributes);

        void failure(Throwable error);

        default void success() {
            success(Collections.emptyMap());
        }
    }

    RuntimeTelemetry NOOP = operation -> NoopScope.INSTANCE;

    static Scope startSafely(RuntimeTelemetry telemetry, Operation operation) {
        if (telemetry == null) return NoopScope.INSTANCE;
        try {
            Scope delegate = telemetry.start(operation);
            if (delegate == null) return NoopScope.INSTANCE;
            return new FailOpenScope(delegate);
        } catch (Throwable ignored) {
            return NoopScope.INSTANCE;
        }
    }

    static void injectSafely(RuntimeTelemetry telemetry, Map<String, String> carrier) {
        if (telemetry == null || carrier == null) return;
        try {
            telemetry.inject(carrier);
        } catch (Throwable ignored) {
            // Runtime telemetry must not affect transport behavior.
        }
    }

    static PropagationScope activateSafely(
            RuntimeTelemetry telemetry, Map<String, String> carrier) {
        if (telemetry == null || carrier == null) return NoopPropagationScope.INSTANCE;
        try {
            PropagationScope scope = telemetry.extractAndActivate(carrier);
            return scope == null ? NoopPropagationScope.INSTANCE : new FailOpenPropagationScope(scope);
        } catch (Throwable ignored) {
            return NoopPropagationScope.INSTANCE;
        }
    }

    interface PropagationScope extends AutoCloseable {
        @Override
        void close();
    }

    enum NoopPropagationScope implements PropagationScope {
        INSTANCE;
        @Override public void close() {}
    }

    final class FailOpenPropagationScope implements PropagationScope {
        private final PropagationScope delegate;
        private boolean closed;

        private FailOpenPropagationScope(PropagationScope delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try {
                delegate.close();
            } catch (Throwable ignored) {
                // Runtime telemetry must not affect transport behavior.
            }
        }
    }

    enum NoopScope implements Scope {
        INSTANCE;

        @Override
        public void success(Map<String, Object> attributes) {}

        @Override
        public void failure(Throwable error) {}
    }

    final class FailOpenScope implements Scope {
        private final Scope delegate;
        private boolean ended;

        private FailOpenScope(Scope delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void success(Map<String, Object> attributes) {
            if (ended) return;
            ended = true;
            try {
                delegate.success(attributes == null ? Collections.emptyMap() : attributes);
            } catch (Throwable ignored) {
                // Runtime telemetry must not affect application behavior.
            }
        }

        @Override
        public synchronized void failure(Throwable error) {
            if (ended) return;
            ended = true;
            try {
                delegate.failure(error);
            } catch (Throwable ignored) {
                // Runtime telemetry must not affect application behavior.
            }
        }
    }
}
