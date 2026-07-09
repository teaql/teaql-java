package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import java.util.Collection;

public class TeaQLRuntime {
    private final EntityMetaFactory metadata;
    private final DataServiceRegistry registry;
    private final RequestPolicy requestPolicy;
    private final InternalIdGenerationService idGenerationService;
    private final RuntimeLogSink logSink;

    private TeaQLRuntime(Builder builder) {
        this.metadata = builder.metadata;
        this.registry = builder.registry != null ? builder.registry : new DefaultDataServiceRegistry();
        this.requestPolicy = builder.requestPolicy;
        this.idGenerationService = builder.idGenerationService;
        this.logSink = builder.logSink;
    }

    public static Builder builder() {
        return new Builder();
    }

    public EntityMetaFactory getMetadata() {
        return metadata;
    }

    public DataServiceRegistry getRegistry() {
        return registry;
    }

    public RequestPolicy getRequestPolicy() {
        return requestPolicy;
    }

    public InternalIdGenerationService getIdGenerationService() {
        return idGenerationService;
    }

    public RuntimeLogSink getLogSink() {
        return logSink;
    }

    public void recordExecutionMetadata(UserContext ctx, ExecutionMetadata metadata) {
        if (logSink != null) {
            logSink.writeExecutionLog(ctx, metadata);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForList(UserContext ctx, SearchRequest<T> request) {
        if (request.purpose() == null || request.purpose().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on query execution. You must not call executeForList directly without purpose.");
        }
        if (requestPolicy != null) {
            requestPolicy.enforceSelect(ctx, request);
        }
        boolean pushedComment = false;
        boolean pushedPurpose = false;
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            ctx.pushTrace(request.comment());
            pushedComment = true;
        }
        if (request.purpose() != null && !request.purpose().trim().isEmpty()) {
            ctx.pushTrace(request.purpose());
            pushedPurpose = true;
        }
        try {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }

            QueryExecutor queryExecutor = registry.resolveQueryExecutor(route);
            if (queryExecutor == null) {
                throw new TeaQLRuntimeException("No QueryExecutor registered for route: " + route);
            }

            QueryRequest queryRequest = new DefaultQueryRequest(request);
            QueryResult queryResult = queryExecutor.query(ctx, queryRequest);

            if (queryResult instanceof DefaultQueryResult) {
                return (SmartList<T>) ((DefaultQueryResult) queryResult).getResult();
            }
            throw new TeaQLRuntimeException(
                "Unsupported QueryResult type '" + queryResult.getClass().getName()
                + "' returned by QueryExecutor for route: " + route
                + ". Executor must return DefaultQueryResult or a subclass.");
        } finally {
            if (pushedPurpose) {
                ctx.popTrace();
            }
            if (pushedComment) {
                ctx.popTrace();
            }
        }
    }

    public <T extends Entity> AggregationResult aggregation(UserContext ctx, SearchRequest<T> request) {
        if (request.purpose() == null || request.purpose().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on query execution. You must not call aggregation directly without purpose.");
        }
        if (requestPolicy != null) {
            requestPolicy.enforceSelect(ctx, request);
        }
        boolean pushedComment = false;
        boolean pushedPurpose = false;
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            ctx.pushTrace(request.comment());
            pushedComment = true;
        }
        if (request.purpose() != null && !request.purpose().trim().isEmpty()) {
            ctx.pushTrace(request.purpose());
            pushedPurpose = true;
        }
        try {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }
            QueryExecutor queryExecutor = registry.resolveQueryExecutor(route);
            if (queryExecutor == null) {
                throw new TeaQLRuntimeException("No QueryExecutor registered for route: " + route);
            }
            QueryRequest queryRequest = new DefaultQueryRequest(request);
            QueryResult queryResult = queryExecutor.query(ctx, queryRequest);
            if (queryResult instanceof DefaultQueryResult) {
                return ((DefaultQueryResult) queryResult).getAggregationResult();
            }
            throw new TeaQLRuntimeException(
                "Unsupported QueryResult type '" + queryResult.getClass().getName()
                + "' returned by QueryExecutor for route: " + route
                + ". Executor must return DefaultQueryResult or a subclass.");
        } finally {
            if (pushedPurpose) {
                ctx.popTrace();
            }
            if (pushedComment) {
                ctx.popTrace();
            }
        }
    }

    public void saveGraph(UserContext ctx, Object items) {
        if (items instanceof Entity) {
            saveGraph(ctx, (Entity) items);
        } else if (items instanceof Collection) {
            for (Object item : (Collection<?>) items) {
                saveGraph(ctx, item);
            }
        }
    }

    /** Context key used to track the first data-service route written to in a saveGraph chain. */
    private static final String SAVE_GRAPH_ACTIVE_ROUTE_KEY = "__teaql_save_graph_route__";

    public void saveGraph(UserContext ctx, Entity entity) {
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[AUDIT REQUIRED] Missing .auditAs() or .setComment() before saveGraph().");
        }
        boolean pushed = false;
        if (entity.getComment() != null && !entity.getComment().trim().isEmpty()) {
            ctx.pushTrace(entity.getComment());
            pushed = true;
        }
        try {
            if (entity.getId() == null && idGenerationService != null) {
                Long newId = idGenerationService.generateId(ctx, entity);
                ((BaseEntity) entity).__internalSet("id", newId);
            }

            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }

            // Cross-provider mutation guard: two different routes in the same
            // saveGraph call have NO atomicity guarantee. Fail fast rather than
            // silently losing consistency.
            Object activeRoute = ctx.extension(SAVE_GRAPH_ACTIVE_ROUTE_KEY);
            if (activeRoute == null) {
                ctx.put(SAVE_GRAPH_ACTIVE_ROUTE_KEY, route);
            } else if (!activeRoute.equals(route)) {
                throw new TeaQLRuntimeException(
                    "[CROSS-PROVIDER MUTATION] saveGraph attempted to write entity '"
                    + entity.typeName() + "' to route '" + route
                    + "' while the current saveGraph chain is already writing to route '"
                    + activeRoute + "'. Cross-provider mutations have no atomicity guarantee. "
                    + "Use separate UserContext instances and an explicit outbox or saga pattern.");
            }

            MutationExecutor mutationExecutor = registry.resolveMutationExecutor(route);
            if (mutationExecutor == null) {
                throw new TeaQLRuntimeException("No MutationExecutor registered for route: " + route);
            }

            DefaultMutationRequest.Action action = entity.deleteItem() ? DefaultMutationRequest.Action.DELETE : DefaultMutationRequest.Action.SAVE;
            MutationRequest mutationRequest = new DefaultMutationRequest(entity, action);

            mutationExecutor.mutate(ctx, mutationRequest);
        } finally {
            if (pushed) {
                ctx.popTrace();
            }
        }
    }

    public void delete(UserContext ctx, Entity entity) {
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[AUDIT REQUIRED] Missing .auditAs() or .setComment() before delete().");
        }
        boolean pushed = false;
        if (entity.getComment() != null && !entity.getComment().trim().isEmpty()) {
            ctx.pushTrace(entity.getComment());
            pushed = true;
        }
        try {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }

            MutationExecutor mutationExecutor = registry.resolveMutationExecutor(route);
            if (mutationExecutor == null) {
                throw new TeaQLRuntimeException("No MutationExecutor registered for route: " + route);
            }

            DefaultMutationRequest.Action action = DefaultMutationRequest.Action.DELETE;
            MutationRequest mutationRequest = new DefaultMutationRequest(entity, action);

            mutationExecutor.mutate(ctx, mutationRequest);
        } finally {
            if (pushed) {
                ctx.popTrace();
            }
        }
    }

    public static class Builder {
        private EntityMetaFactory metadata;
        private DataServiceRegistry registry = new DefaultDataServiceRegistry();
        private RequestPolicy requestPolicy;
        private InternalIdGenerationService idGenerationService;
        private RuntimeLogSink logSink;

        public Builder metadata(EntityMetaFactory metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder registry(DataServiceRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder dataService(String name, DataServiceExecutor executor) {
            if (!(this.registry instanceof DefaultDataServiceRegistry dsr)) {
                throw new IllegalStateException("Cannot register data service on custom registry");
            }
            dsr.register(name, executor);
            return this;
        }

        public Builder requestPolicy(RequestPolicy requestPolicy) {
            this.requestPolicy = requestPolicy;
            return this;
        }

        public Builder idGenerationService(InternalIdGenerationService idGenerationService) {
            this.idGenerationService = idGenerationService;
            return this;
        }

        public Builder logSink(RuntimeLogSink logSink) {
            this.logSink = logSink;
            return this;
        }

        public TeaQLRuntime build() {
            if (metadata == null) {
                throw new IllegalStateException("EntityMetaFactory metadata is required");
            }
            return new TeaQLRuntime(this);
        }
    }
}
