package io.teaql.core;

import java.util.stream.Stream;

/** Executes a validated query with resources owned by the returned closeable Stream. */
public interface StreamingQueryExecutor extends DataServiceExecutor {
    /** The envelope owns the captured root intent, just as for materialized queries. */
    <T extends Entity> Stream<T> queryForStream(UserContext context, QueryRequest request);

    /** Optional provider contract for returned lifecycle evidence, without changing the Stream API. */
    default <T extends Entity> QueryCursor<T> queryForCursor(UserContext context, QueryRequest request) {
        throw new UnsupportedOperationException("This provider does not support returned query cursor evidence");
    }
}
