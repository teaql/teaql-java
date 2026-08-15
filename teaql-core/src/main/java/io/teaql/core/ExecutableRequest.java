package io.teaql.core;

import java.util.stream.Stream;

/**
 * A query that has declared comment and purpose, ready to execute.
 * Can only be created via BaseRequest.purpose(), after a comment has been set.
 *
 * Design goal: prevent queries without declared intent from executing.
 *
 * Usage:
 *   // Compiles
 *   Q.tasks()
 *       .filterByName("xxx")
 *       .comment("Load tasks")
 *       .purpose("Display kanban board") // returns ExecutableRequest
 *       .executeForList(ctx);             // only ExecutableRequest can execute
 *
 *   // Compile error: purpose() has not produced an ExecutableRequest
 *   Q.tasks().executeForList(ctx);
 */
public class ExecutableRequest<T extends Entity> {
    private final SearchRequest<T> request;

    ExecutableRequest(SearchRequest<T> request) {
        this.request = request;
    }

    /** Creates a generated entity only after Comment and Purpose are declared. */
    public T newEntity(UserContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("UserContext is required for entity creation");
        }
        return ctx.initializeEntity(request.getTypeName(), request.internalNewEntity());
    }

    public SmartList<T> executeForList(UserContext ctx) {
        return ctx.executeForList(this);
    }

    public T executeForOne(UserContext ctx) {
        return ctx.executeForOne(this);
    }

    public Stream<T> executeForStream(UserContext ctx) {
        return ctx.executeForStream(this);
    }

    public AggregationResult aggregation(UserContext ctx) {
        return ctx.aggregation(this);
    }

    public SearchRequest<T> request() {
        return request;
    }
}
