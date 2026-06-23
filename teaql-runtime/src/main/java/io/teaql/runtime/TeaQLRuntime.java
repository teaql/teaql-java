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
        boolean pushedPurpose = false;
        boolean pushedComment = false;
        if (request.purpose() != null && !request.purpose().trim().isEmpty()) {
            ctx.pushTrace(request.purpose());
            pushedPurpose = true;
        }
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            ctx.pushTrace(request.comment());
            pushedComment = true;
        }
        try {
            SearchRequest<T> checkedRequest = request;
            if (requestPolicy != null) {
                // Can enforce select policy
            }

            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(checkedRequest.getTypeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }

            QueryExecutor queryExecutor = registry.resolveQueryExecutor(route);
            if (queryExecutor == null) {
                throw new TeaQLRuntimeException("No QueryExecutor registered for route: " + route);
            }

            QueryRequest queryRequest = new DefaultQueryRequest(checkedRequest);
            QueryResult queryResult = queryExecutor.query(ctx, queryRequest);

            if (queryResult instanceof DefaultQueryResult) {
                return (SmartList<T>) ((DefaultQueryResult) queryResult).getResult();
            }
            throw new TeaQLRuntimeException("Unsupported QueryResult type from query executor: " + route);
        } finally {
            if (pushedComment) {
                ctx.popTrace();
            }
            if (pushedPurpose) {
                ctx.popTrace();
            }
        }
    }

    public <T extends Entity> AggregationResult aggregation(UserContext ctx, SearchRequest<T> request) {
        if (request.purpose() == null || request.purpose().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on query execution. You must not call aggregation directly without purpose.");
        }
        boolean pushedPurpose = false;
        boolean pushedComment = false;
        if (request.purpose() != null && !request.purpose().trim().isEmpty()) {
            ctx.pushTrace(request.purpose());
            pushedPurpose = true;
        }
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            ctx.pushTrace(request.comment());
            pushedComment = true;
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
            return null;
        } finally {
            if (pushedComment) {
                ctx.popTrace();
            }
            if (pushedPurpose) {
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
                ((BaseEntity) entity).internalSet("id", newId);
            }

            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
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
            if (this.registry instanceof DefaultDataServiceRegistry) {
                ((DefaultDataServiceRegistry) this.registry).register(name, executor);
            } else {
                throw new IllegalStateException("Cannot register data service on custom registry");
            }
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
