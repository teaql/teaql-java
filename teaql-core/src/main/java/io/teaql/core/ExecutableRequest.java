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
 *       .executeForList(context);             // only ExecutableRequest can execute
 *
 *   // Compile error: purpose() has not produced an ExecutableRequest
 *   Q.tasks().executeForList(context);
 */
public class ExecutableRequest<T extends Entity> {
    private final SearchRequest<T> request;

    ExecutableRequest(SearchRequest<T> request) {
        this.request = request;
    }

    /** Creates a generated entity only after Comment and Purpose are declared. */
    public T newEntity(UserContext context) {
        if (context == null) {
            throw new IllegalArgumentException("UserContext is required for entity creation");
        }
        return context.initializeEntity(request.getTypeName(), request.internalNewEntity());
    }

    public SmartList<T> executeForList(UserContext context) {
        return context.executeForList(this);
    }

    /**
     * Executes a stable offset page and attaches the exact filtered total to
     * the returned SmartList. Generated request filters and trusted runtime
     * policy are shared by the rows and count aggregation.
     */
    public SmartList<T> executeForPage(UserContext context, int offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > SearchRequest.DEFAULT_HARD_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + SearchRequest.DEFAULT_HARD_LIMIT);
        }
        return context.executeForPage(this, offset, limit);
    }

    public T executeForOne(UserContext context) {
        return context.executeForOne(this);
    }

    public Stream<T> executeForStream(UserContext context) {
        return context.executeForStream(this);
    }

    public AggregationResult aggregation(UserContext context) {
        return context.aggregation(this);
    }

    public SearchRequest<T> request() {
        return request;
    }
}
