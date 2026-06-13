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
    private final ExecutionLogSink logSink;

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

    public ExecutionLogSink getLogSink() {
        return logSink;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForList(UserContext ctx, SearchRequest<T> request) {
        SearchRequest<T> checkedRequest = request;
        if (requestPolicy != null) {
            // Can enforce select policy
        }

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(checkedRequest.getTypeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) {
            route = "sql";
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
    }

    public <T extends Entity> AggregationResult aggregation(UserContext ctx, SearchRequest<T> request) {
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) {
            route = "sql";
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
        if (entity.getId() == null && idGenerationService != null) {
            Long newId = idGenerationService.generateId(ctx, entity);
            entity.setId(newId);
        }

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) {
            route = "sql";
        }

        MutationExecutor mutationExecutor = registry.resolveMutationExecutor(route);
        if (mutationExecutor == null) {
            throw new TeaQLRuntimeException("No MutationExecutor registered for route: " + route);
        }

        DefaultMutationRequest.Action action = entity.deleteItem() ? DefaultMutationRequest.Action.DELETE : DefaultMutationRequest.Action.SAVE;
        DefaultMutationRequest mutationRequest = new DefaultMutationRequest(entity, action);
        mutationExecutor.mutate(ctx, mutationRequest);
    }

    public void delete(UserContext ctx, Entity entity) {
        if (entity instanceof BaseEntity) {
            BaseEntity base = (BaseEntity) entity;
            if (!base.deleteItem()) {
                base.markToRemove();
            }
        }

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) {
            route = "sql";
        }

        MutationExecutor mutationExecutor = registry.resolveMutationExecutor(route);
        if (mutationExecutor == null) {
            throw new TeaQLRuntimeException("No MutationExecutor registered for route: " + route);
        }

        DefaultMutationRequest mutationRequest = new DefaultMutationRequest(entity, DefaultMutationRequest.Action.DELETE);
        mutationExecutor.mutate(ctx, mutationRequest);
    }

    public static class Builder {
        private EntityMetaFactory metadata;
        private DataServiceRegistry registry = new DefaultDataServiceRegistry();
        private RequestPolicy requestPolicy;
        private InternalIdGenerationService idGenerationService;
        private ExecutionLogSink logSink;

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

        public Builder logSink(ExecutionLogSink logSink) {
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
