package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;
import io.teaql.core.meta.EntityDescriptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import io.teaql.core.log.TraceNode;
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
    public <T extends Entity> T executeForOne(SearchRequest<T> searchRequest) {
        SmartList<T> list = executeForList(searchRequest);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public <T extends Entity> SmartList<T> executeForList(SearchRequest searchRequest) {
        return runtime.executeForList(this, searchRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> executeForStream(SearchRequest searchRequest) {
        return (Stream<T>) (Stream<?>) executeForList(searchRequest).stream();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> executeForStream(SearchRequest searchRequest, int enhanceBatchSize) {
        return (Stream<T>) (Stream<?>) executeForList(searchRequest).stream();
    }

    @Override
    public <T extends Entity> AggregationResult aggregation(SearchRequest request) {
        return runtime.aggregation(this, request);
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
    public void pushTrace(String comment) {
        traceChain.add(new TraceNode(comment));
    }

    @Override
    public List<TraceNode> getTraceChain() {
        return Collections.unmodifiableList(traceChain);
    }

    @Override
    public void logSql(String sql, long elapsedUs, String message) {
        io.teaql.runtime.log.LogManager.getInstance().writeSqlLog(
                this.traceChain, 
                new io.teaql.runtime.log.SqlLogEntry(sql, elapsedUs, message)
        );
    }
}
