package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.util.*;

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
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on query execution.");
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
                SmartList<T> results = (SmartList<T>) ((DefaultQueryResult) queryResult).getResult();
                // Set entityRoot for all loaded entities
                EntityRoot entityRoot = getOrCreateEntityRoot(ctx);
                for (T entity : results) {
                    setupEntityRoot(entity, entityRoot);
                }
                return results;
            }
            throw new TeaQLRuntimeException("Unsupported QueryResult type: " + queryResult.getClass().getName());
        } finally {
            if (pushedPurpose) ctx.popTrace();
            if (pushedComment) ctx.popTrace();
        }
    }

    public <T extends Entity> AggregationResult aggregation(UserContext ctx, SearchRequest<T> request) {
        if (request.purpose() == null || request.purpose().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on aggregation.");
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
            throw new TeaQLRuntimeException("Unsupported QueryResult type: " + queryResult.getClass().getName());
        } finally {
            if (pushedPurpose) ctx.popTrace();
            if (pushedComment) ctx.popTrace();
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

    private static final String SAVE_GRAPH_ACTIVE_ROUTE_KEY = "__teaql_save_graph_route__";
    private static final String ENTITY_ROOT_KEY = "__teaql_entity_root__";

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
            EntityRoot entityRoot = getOrCreateEntityRoot(ctx);
            setupEntityRoot(entity, entityRoot);

            if (entity.getId() == null && idGenerationService != null) {
                Long newId = idGenerationService.generateId(ctx, entity);
                ((BaseEntity) entity).__internalSet("id", newId);
                entityRoot.markAsNew(new EntityKey(entity.typeName(), newId));
            }

            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }

            Object activeRoute = ctx.extension(SAVE_GRAPH_ACTIVE_ROUTE_KEY);
            if (activeRoute == null) {
                ctx.put(SAVE_GRAPH_ACTIVE_ROUTE_KEY, route);
            } else if (!activeRoute.equals(route)) {
                throw new TeaQLRuntimeException(
                    "[CROSS-PROVIDER MUTATION] saveGraph attempted to write entity '"
                    + entity.typeName() + "' to route '" + route
                    + "' while the current saveGraph chain is already writing to route '"
                    + activeRoute + "'.");
            }

            MutationExecutor mutationExecutor = registry.resolveMutationExecutor(route);
            if (mutationExecutor == null) {
                throw new TeaQLRuntimeException("No MutationExecutor registered for route: " + route);
            }

            executeLedgerPlan(ctx, entityRoot, mutationExecutor);
            entityRoot.clearCurrentChangeSet();
        } finally {
            if (pushed) ctx.popTrace();
        }
    }

    private EntityRoot getOrCreateEntityRoot(UserContext ctx) {
        EntityRoot root = (EntityRoot) ctx.extension(ENTITY_ROOT_KEY);
        if (root == null) {
            root = new EntityRoot();
            ctx.put(ENTITY_ROOT_KEY, root);
        }
        return root;
    }

    private void setupEntityRoot(Entity entity, EntityRoot root) {
        if (!(entity instanceof BaseEntity baseEntity)) {
            return;
        }
        baseEntity.setEntityRoot(root);

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        if (descriptor == null) return;

        for (PropertyDescriptor prop : descriptor.getProperties()) {
            if (!(prop instanceof io.teaql.core.meta.Relation)) continue;
            Object value = entity.getProperty(prop.getName());
            if (value instanceof Entity relEntity) {
                setupEntityRoot(relEntity, root);
            } else if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Entity relEntity) {
                        setupEntityRoot(relEntity, root);
                    }
                }
            }
        }
    }

    private void executeLedgerPlan(UserContext ctx, EntityRoot root, MutationExecutor mutationExecutor) {
        EntityChangeSet changeSet = root.currentChangeSet();
        Set<EntityKey> deletedKeys = root.deletedKeys();
        Set<EntityKey> newKeys = root.newKeys();

        // 1. Execute Deletes
        List<EntityKey> sortedDeletedKeys = new ArrayList<>(deletedKeys);
        Collections.sort(sortedDeletedKeys);
        for (EntityKey key : sortedDeletedKeys) {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(key.entity());
            if (descriptor == null) {
                throw new TeaQLRuntimeException("No entity descriptor for: " + key.entity());
            }
            BaseEntity deleteEntity = (BaseEntity) descriptor.createEntity();
            deleteEntity.__internalSet("id", key.id());
            deleteEntity.markToRemove();
            if (root.getComment() != null) deleteEntity.setComment(root.getComment());

            DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                deleteEntity, DefaultMutationRequest.Action.DELETE);
            mutationExecutor.mutate(ctx, mutationRequest);
        }

        // 2. Group changes
        Map<String, List<EntityKey>> insertBatches = new TreeMap<>();
        Map<String, List<EntityKey>> updateBatches = new TreeMap<>();

        for (Map.Entry<EntityKey, Map<String, Object>> entry : changeSet.changes().entrySet()) {
            EntityKey key = entry.getKey();
            if (deletedKeys.contains(key)) continue;

            boolean isNew = newKeys.contains(key) || key.id() == null;
            if (isNew) {
                insertBatches.computeIfAbsent(key.entity(), k -> new ArrayList<>()).add(key);
            } else {
                updateBatches.computeIfAbsent(key.entity(), k -> new ArrayList<>()).add(key);
            }
        }

        // 3. Execute Inserts
        for (Map.Entry<String, List<EntityKey>> entry : insertBatches.entrySet()) {
            String entityName = entry.getKey();
            List<EntityKey> keys = entry.getValue();
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entityName);
            if (descriptor == null) {
                throw new TeaQLRuntimeException("No entity descriptor for: " + entityName);
            }
            for (EntityKey key : keys) {
                Map<String, Object> changes = changeSet.changes().get(key);
                if (changes == null) continue;
                BaseEntity entity = (BaseEntity) descriptor.createEntity();
                entity.__internalSet("id", key.id());
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.setProperty(change.getKey(), change.getValue());
                }
                if (root.getComment() != null) entity.setComment(root.getComment());

                DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                    entity, DefaultMutationRequest.Action.SAVE);
                mutationExecutor.mutate(ctx, mutationRequest);
            }
        }

        // 4. Execute Updates
        for (Map.Entry<String, List<EntityKey>> entry : updateBatches.entrySet()) {
            String entityName = entry.getKey();
            List<EntityKey> keys = entry.getValue();
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entityName);
            if (descriptor == null) {
                throw new TeaQLRuntimeException("No entity descriptor for: " + entityName);
            }
            for (EntityKey key : keys) {
                Map<String, Object> changes = changeSet.changes().get(key);
                if (changes == null) continue;
                BaseEntity entity = (BaseEntity) descriptor.createEntity();
                entity.__internalSet("id", key.id());
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.setProperty(change.getKey(), change.getValue());
                }
                entity.gotoNextStatus(EntityAction.UPDATE);
                if (root.getComment() != null) entity.setComment(root.getComment());

                DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                    entity, DefaultMutationRequest.Action.SAVE);
                mutationExecutor.mutate(ctx, mutationRequest);
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

            EntityRoot entityRoot = getOrCreateEntityRoot(ctx);
            if (entity instanceof BaseEntity baseEntity && entity.getId() != null) {
                baseEntity.setEntityRoot(entityRoot);
                entityRoot.markAsDelete(new EntityKey(entity.typeName(), entity.getId()));
            }

            DefaultMutationRequest.Action action = DefaultMutationRequest.Action.DELETE;
            MutationRequest mutationRequest = new DefaultMutationRequest(entity, action);
            mutationExecutor.mutate(ctx, mutationRequest);
        } finally {
            if (pushed) ctx.popTrace();
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
