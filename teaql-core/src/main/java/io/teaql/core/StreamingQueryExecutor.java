package io.teaql.core;

import java.util.stream.Stream;

/** Executes a query with resources owned by the returned closeable Stream. */
public interface StreamingQueryExecutor extends DataServiceExecutor {
    <T extends Entity> Stream<T> queryForStream(UserContext ctx, SearchRequest<T> request);
}
