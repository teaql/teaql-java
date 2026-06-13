package io.teaql.core;

import java.util.stream.Stream;

/**
 * A query that has declared comment and purpose, ready to execute.
 * Can only be created via BaseRequest.build(), enforcing comment + purpose.
 *
 * Design goal: prevent queries without declared intent from executing.
 *
 * Usage:
 *   // Compiles
 *   Q.tasks()
 *       .filterByName("xxx")
 *       .comment("Load tasks")
 *       .purpose("Display kanban board")
 *       .build()              // returns ExecutableRequest
 *       .executeForList(ctx); // only ExecutableRequest can execute
 *
 *   // Compile error: no build(), cannot get ExecutableRequest
 *   Q.tasks().executeForList(ctx);
 */
public class ExecutableRequest<T extends Entity> {
    private final SearchRequest<T> request;

    ExecutableRequest(SearchRequest<T> request) {
        this.request = request;
    }

    public SmartList<T> executeForList(UserContext ctx) {
        return request.executeForList(ctx);
    }

    public T executeForOne(UserContext ctx) {
        return request.executeForOne(ctx);
    }

    public Stream<T> executeForStream(UserContext ctx) {
        return request.executeForStream(ctx);
    }

    public AggregationResult aggregation(UserContext ctx) {
        return request.aggregation(ctx);
    }

    public SearchRequest<T> request() {
        return request;
    }
}
