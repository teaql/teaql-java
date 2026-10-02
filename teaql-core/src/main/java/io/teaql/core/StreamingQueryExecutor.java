package io.teaql.core;

import java.util.stream.Stream;

/** Executes a validated query with resources owned by the returned closeable Stream. */
public interface StreamingQueryExecutor extends DataServiceExecutor {
    /** The envelope owns the captured root intent, just as for materialized queries. */
    <T extends Entity> Stream<T> queryForStream(UserContext context, QueryRequest request);
}
