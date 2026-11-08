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

    /** Creates a new aggregate with an explicit ID; never changes an existing entity. */
    public T newEntityWithId(Long id, UserContext context) {
        if (context == null) throw new IllegalArgumentException("UserContext is required for entity creation");
        if (id == null || id <= 0) throw new IllegalArgumentException("Creation ID must be positive");
        T entity = request.internalNewEntity();
        if (!(entity instanceof BaseEntity base)) {
            throw new IllegalArgumentException("Explicit-ID creation requires a generated BaseEntity");
        }
        InternalIdGenerationService ids = context.capability(InternalIdGenerationService.class);
        if (ids == null) {
            throw new IllegalStateException("Explicit-ID creation requires an InternalIdGenerationService");
        }
        ids.ensureFloor(base.typeName(), id);
        base.__internalInitializeNewEntityId(id);
        return context.initializeEntity(request.getTypeName(), entity);
    }

    public SmartList<T> executeForList(UserContext context) {
        return context.executeForList(this);
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
