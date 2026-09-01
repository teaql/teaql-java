package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;
import io.teaql.core.meta.EntityDescriptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DefaultUserContext implements UserContext, OptNullBasicTypeFromObjectGetter<String> {

    private final TeaQLRuntime runtime;
    private final Map<String, Object> storage = new ConcurrentHashMap<>();
    private final List<TraceNode> traceChain = new ArrayList<>();

    public DefaultUserContext(TeaQLRuntime runtime) {
        this.runtime = runtime;
    }

    public TeaQLRuntime getRuntime() {
        return runtime;
    }

    @Override
    public <T extends Entity> T internalExecuteForOne(SearchRequest searchRequest) {
        SmartList<T> list = internalExecuteForList(searchRequest);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public <T extends Entity> SmartList<T> internalExecuteForList(SearchRequest searchRequest) {
        return runtime.internalExecuteForList(this, searchRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest) {
        return internalExecuteForStream(searchRequest, 200);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest, int batchSize) {
        // Check whether the backing executor declares streaming capability.
        // If it does, delegate to a lazy batch-pull iterator.
        // If not, fall back to a fully-materialized list (safe but not lazy).
        EntityDescriptor descriptor = runtime.getMetadata().resolveEntityDescriptor(searchRequest.getTypeName());
        String route = descriptor != null ? descriptor.getDataService() : null;
        if (route == null || route.isEmpty()) {
            route = "default";
        }
        DataServiceExecutor executor = runtime.getRegistry().resolve(route);
        if (executor instanceof StreamingQueryExecutor) {
            return ((StreamingQueryExecutor) executor).queryForStream(this, searchRequest);
        }
        throw new TeaQLRuntimeException("Streaming query is not supported for route: " + route);
    }

    @Override
    public <T extends Entity> AggregationResult internalAggregation(SearchRequest request) {
        return runtime.aggregation(this, request);
    }

    @Override
    public <T extends Entity> T executeForOne(ExecutableRequest<T> request) {
        SmartList<T> list = runtime.executeForList(this, request.request());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public <T extends Entity> SmartList<T> executeForList(ExecutableRequest<T> request) {
        return runtime.executeForList(this, request.request());
    }

    @Override
    public <T extends Entity> SmartList<T> executeForPage(
            ExecutableRequest<T> request, int offset, int limit) {
        return runtime.executeForPage(this, request.request(), offset, limit);
    }

    @Override
    public <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request) {
        return internalExecuteForStream(request.request());
    }

    @Override
    public <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request, int enhanceBatchSize) {
        return internalExecuteForStream(request.request(), enhanceBatchSize);
    }

    @Override
    public <T extends Entity> AggregationResult aggregation(ExecutableRequest<T> request) {
        return internalAggregation(request.request());
    }

    @Override
    public void saveGraph(Object items) {
        runtime.saveGraph(this, items);
    }

    @Override
    public void saveGraph(Entity entity) {
        runtime.saveGraph(this, entity);
    }

    @Override
    public void putAttribute(String key, Object value) {
        if (value == null) {
            storage.remove(key);
        } else {
            storage.put(key, value);
        }
    }

    @Override
    public Object getAttribute(String key) {
        return storage.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> clazz) {
        Object val = storage.get(key);
        if (clazz != null && clazz.isInstance(val)) {
            return (T) val;
        }
        return null;
    }

    @Override
    public Object extension(String name) {
        return getObj(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T capability(Class<T> capabilityType) {
        if (capabilityType == null) {
            return null;
        }
        Object value = getObj(capabilityType.getName());
        if (value == null && capabilityType == SchemaExecutor.class) {
            value = runtime.getSchemaExecutor();
            if (value == null) {
                value = runtime.getRegistry().resolve("default");
            }
        }
        if (value == null && capabilityType == GeneratedSchemaBootstrap.class) {
            value = runtime.getGeneratedSchemaBootstrap();
        }
        if (value == null && capabilityType == InternalIdGenerationService.class) {
            value = runtime.getIdGenerationService();
        }
        if (value == null) {
            return null;
        }
        if (!capabilityType.isInstance(value)) {
            return null;
        }
        return (T) value;
    }

    @Override
    public void putToLocalCache(String key, Object value, int timeToLiveInSeconds) {
        observeCache("local.put", "put", () -> {
            UserContext.super.putToLocalCache(key, value, timeToLiveInSeconds);
            return null;
        }, ignored -> "stored");
    }

    @Override
    public <T> T getFromLocalCache(String key, Class<T> clazz) {
        return observeCache("local.get", "get",
                () -> UserContext.super.getFromLocalCache(key, clazz),
                value -> value == null ? "miss" : "hit");
    }

    @Override
    public void removeFromLocalCache(String key) {
        observeCache("local.remove", "remove", () -> {
            UserContext.super.removeFromLocalCache(key);
            return null;
        }, ignored -> "removed");
    }

    @Override
    public void putToRemoteCache(String key, Object value, int timeToLiveInSeconds) {
        observeCache("remote.put", "put", () -> {
            UserContext.super.putToRemoteCache(key, value, timeToLiveInSeconds);
            return null;
        }, ignored -> "stored");
    }

    @Override
    public <T> T getFromRemoteCache(String key, Class<T> clazz) {
        return observeCache("remote.get", "get",
                () -> UserContext.super.getFromRemoteCache(key, clazz),
                value -> value == null ? "miss" : "hit");
    }

    @Override
    public void removeFromRemoteCache(String key) {
        observeCache("remote.remove", "remove", () -> {
            UserContext.super.removeFromRemoteCache(key);
            return null;
        }, ignored -> "removed");
    }

    private <T> T observeCache(String name, String operation, Supplier<T> work,
            java.util.function.Function<T, String> result) {
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(runtime.getTelemetry(),
                new RuntimeTelemetry.Operation("cache", name,
                        Map.of("teaql.cache.operation", operation)));
        try {
            T value = work.get();
            scope.success(Map.of("teaql.cache.result", result.apply(value)));
            return value;
        } catch (RuntimeException | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    @Override
    public void pushTrace(String comment) {
        traceChain.add(new TraceNode(comment));
    }

    @Override
    public void pushTrace(TraceKind kind, String name, String value) {
        traceChain.add(new TraceNode(kind, name, value));
    }

    @Override
    public void popTrace() {
        if (!traceChain.isEmpty()) {
            traceChain.remove(traceChain.size() - 1);
        }
    }

    @Override
    public List<TraceNode> getTraceChain() {
        return Collections.unmodifiableList(new ArrayList<>(traceChain));
    }

    @Override
    public boolean isExecutionLoggingEnabled() {
        return runtime == null || runtime.isExecutionLoggingEnabled();
    }

    @Override
    public boolean isQueryExecutionLoggingEnabled() {
        return runtime == null || runtime.isQueryExecutionLoggingEnabled();
    }

    @Override
    public boolean isMutationExecutionLoggingEnabled() {
        return runtime == null || runtime.isMutationExecutionLoggingEnabled();
    }

    @Override
    public void recordExecutionMetadata(io.teaql.core.ExecutionMetadata metadata) {
        if (metadata.getTraceChain() == null || metadata.getTraceChain().isEmpty()) {
            metadata.setTraceChain(getTraceChain());
        }
        if (metadata.getTraceChain() != null) {
            for (TraceNode node : metadata.getTraceChain()) {
                if (node.getKind() == TraceKind.COMMENT) metadata.setComment(node.getComment());
                if (node.getKind() == TraceKind.PURPOSE) metadata.setPurpose(node.getComment());
                if (node.getKind() == TraceKind.AUDIT_REASON) metadata.setAuditReason(node.getComment());
            }
        }
        java.util.List<TraceNode> canonical = new java.util.ArrayList<>();
        if (metadata.getTraceChain() != null) {
            metadata.getTraceChain().stream()
                    .filter(node -> node.getKind() != TraceKind.COMMENT
                            && node.getKind() != TraceKind.PURPOSE
                            && node.getKind() != TraceKind.AUDIT_REASON
                            && node.getKind() != TraceKind.PROVIDER
                            && node.getKind() != TraceKind.SQL)
                    .forEach(canonical::add);
        }
        String backend = metadata.getBackend() == null ? "unknown" : metadata.getBackend();
        canonical.add(new TraceNode(TraceKind.PROVIDER, backend, backend));
        String sqlOperation = sqlOperation(metadata);
        canonical.add(new TraceNode(TraceKind.SQL, sqlOperation, sqlOperation));
        metadata.setTraceChain(canonical);
        if (runtime != null) {
            runtime.recordExecutionMetadata(this, metadata);
        }
    }

    private static String sqlOperation(io.teaql.core.ExecutionMetadata metadata) {
        String sql = metadata.getParameterizedQuery();
        if (sql != null) {
            String normalized = sql.stripLeading();
            while (normalized.startsWith("/*")) {
                int end = normalized.indexOf("*/");
                if (end < 0) break;
                normalized = normalized.substring(end + 2).stripLeading();
            }
            int boundary = normalized.indexOf(' ');
            String keyword = (boundary < 0 ? normalized : normalized.substring(0, boundary))
                    .replaceAll("[^A-Za-z]", "").toLowerCase(java.util.Locale.ROOT);
            if (!keyword.isEmpty()) return keyword;
        }
        return metadata.getOperation() == null
                ? "sql"
                : metadata.getOperation().name().toLowerCase(java.util.Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T evaluate(String expression, Object... args) {
        // Built-in: "now" returns the current local date-time.
        if ("now".equalsIgnoreCase(expression)) {
            Object captured = getAttribute(io.teaql.core.checker.Checker.TEAQL_FIX_TIME);
            return (T) (captured != null ? captured : java.time.LocalDateTime.now());
        }
        // Delegate to subclass or extension for application-defined expressions.
        return evaluateExpression(expression, args);
    }

    /**
     * Extension point for application-defined expression evaluation.
     * Override in a subclass of {@link DefaultUserContext} to handle
     * custom expressions without modifying framework code.
     *
     * <p>Return {@code null} when the expression is not recognised.
     *
     * @param expression the expression name
     * @param args       optional arguments
     * @param <T>        the expected return type
     * @return the evaluated value, or {@code null} if unrecognised
     */
    @SuppressWarnings("unchecked")
    protected <T> T evaluateExpression(String expression, Object... args) {
        return null;
    }
}
