package io.teaql.core;

import java.util.stream.Stream;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;

public interface UserContext extends OptNullBasicTypeFromObjectGetter<String> {

    // Business-facing API
    <T extends Entity> T executeForOne(SearchRequest<T> searchRequest);

    <T extends Entity> SmartList<T> executeForList(SearchRequest searchRequest);

    <T extends Entity> Stream<T> executeForStream(SearchRequest searchRequest);

    <T extends Entity> Stream<T> executeForStream(SearchRequest searchRequest, int enhanceBatchSize);

    <T extends Entity> AggregationResult aggregation(SearchRequest request);

    void saveGraph(Object items);

    void saveGraph(Entity entity);

    void delete(Entity pEntity);

    void put(String key, Object value);
}
