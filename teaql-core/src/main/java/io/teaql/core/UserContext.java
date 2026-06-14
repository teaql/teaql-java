package io.teaql.core;

import java.util.stream.Stream;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;
import java.util.List;
import io.teaql.core.log.TraceNode;

public interface UserContext extends OptNullBasicTypeFromObjectGetter<String> {

    void pushTrace(String comment);

    List<TraceNode> getTraceChain();

    void popTrace();
    void recordExecutionMetadata(ExecutionMetadata metadata);

    void registerCustomSink(io.teaql.core.log.CustomLogSink sink);
    io.teaql.core.log.CustomLogSink getCustomSink();

    // Business-facing API
    <T extends Entity> T executeForOne(ExecutableRequest<T> request);

    <T extends Entity> SmartList<T> executeForList(ExecutableRequest<T> request);

    <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request);

    <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request, int enhanceBatchSize);

    <T extends Entity> AggregationResult aggregation(ExecutableRequest<T> request);

    // Internal framework API (do not use in business logic)
    <T extends Entity> SmartList<T> internalExecuteForList(SearchRequest searchRequest);
    <T extends Entity> T internalExecuteForOne(SearchRequest searchRequest);
    <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest);
    <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest, int enhanceBatchSize);
    <T extends Entity> AggregationResult internalAggregation(SearchRequest request);

    void saveGraph(Object items);

    void saveGraph(Entity entity);

    void delete(Entity pEntity);

    void put(String key, Object value);

    <T> T evaluate(String expression, Object... args);
}
