package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;
import io.teaql.core.meta.EntityDescriptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return runtime.executeForList(this, searchRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest) {
        return (Stream<T>) (Stream<?>) internalExecuteForList(searchRequest).stream();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest, int enhanceBatchSize) {
        return (Stream<T>) (Stream<?>) internalExecuteForList(searchRequest).stream();
    }

    @Override
    public <T extends Entity> AggregationResult internalAggregation(SearchRequest request) {
        return runtime.aggregation(this, request);
    }

    @Override
    public <T extends Entity> T executeForOne(ExecutableRequest<T> request) {
        return internalExecuteForOne(request.request());
    }

    @Override
    public <T extends Entity> SmartList<T> executeForList(ExecutableRequest<T> request) {
        return internalExecuteForList(request.request());
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
    public void delete(Entity pEntity) {
        runtime.delete(this, pEntity);
    }

    @Override
    public void put(String key, Object value) {
        if (value == null) {
            storage.remove(key);
        } else {
            storage.put(key, value);
        }
    }

    @Override
    public Object getObj(String key) {
        return storage.get(key);
    }

    @Override
    public Object getObj(String key, Object defaultValue) {
        Object val = storage.get(key);
        return val != null ? val : defaultValue;
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
        if (value == null) {
            return null;
        }
        if (!capabilityType.isInstance(value)) {
            return null;
        }
        return (T) value;
    }

    @Override
    public void pushTrace(String comment) {
        traceChain.add(new TraceNode(comment));
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
    public void recordExecutionMetadata(io.teaql.core.ExecutionMetadata metadata) {
        if (metadata.getTraceChain() == null || metadata.getTraceChain().isEmpty()) {
            metadata.setTraceChain(getTraceChain());
        }
        if (runtime != null) {
            runtime.recordExecutionMetadata(this, metadata);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(String expression, Object... args) {
        if ("now".equalsIgnoreCase(expression)) {
            return (T) java.time.LocalDateTime.now();
        }
        return null;
    }
}
