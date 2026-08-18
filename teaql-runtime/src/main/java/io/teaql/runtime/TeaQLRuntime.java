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
    private final RuntimeTelemetry telemetry;

    private TeaQLRuntime(Builder builder) {
        this.metadata = builder.metadata;
        this.registry = builder.registry != null ? builder.registry : new DefaultDataServiceRegistry();
        this.requestPolicy = builder.requestPolicy;
        this.idGenerationService = builder.idGenerationService;
        this.logSink = builder.logSink;
        this.telemetry = builder.telemetry != null ? builder.telemetry : RuntimeTelemetry.NOOP;
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

    public RuntimeTelemetry getTelemetry() {
        return telemetry;
    }

    /** Installs a passive generated manifest. Database schemas remain unchanged. */
    public TeaQLRuntime install(RuntimeModule module) {
        module.install(metadata);
        return this;
    }

    public void recordExecutionMetadata(UserContext context, ExecutionMetadata metadata) {
        if (logSink != null) {
            logSink.writeExecutionLog(context, metadata);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForList(UserContext context, SearchRequest<T> request) {
        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", request.getTypeName() + ".list",
                        Map.of("teaql.entity.type", request.getTypeName())));
        try {
        if (request.purpose() == null || request.purpose().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on query execution.");
        }
        enforceMaterializedLimit(request, request.hardLimit());
        if (requestPolicy != null) {
            requestPolicy.enforceSelect(context, request);
        }
        boolean pushedComment = false;
        boolean pushedPurpose = false;
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            context.pushTrace(request.comment());
            pushedComment = true;
        }
        if (request.purpose() != null && !request.purpose().trim().isEmpty()) {
            context.pushTrace(request.purpose());
            pushedPurpose = true;
        }
        try {
            SmartList<T> result = executeForListResolved(context, request);
            telemetryScope.success(Map.of("teaql.result.cardinality", result.size()));
            return result;
        } finally {
            if (pushedPurpose) context.popTrace();
            if (pushedComment) context.popTrace();
        }
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForPage(
            UserContext context, SearchRequest<T> request, int offset, int limit) {
        if (!(request instanceof BaseRequest<?> baseRequest)) {
            throw new TeaQLRuntimeException("Paged execution requires a generated BaseRequest");
        }
        baseRequest.offset(offset, limit);
        SmartList<T> rows = executeForList(context, request);
        SearchRequest<?> countRequest = baseRequest.internalCountRequest();
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) route = "default";
        QueryExecutor executor = registry.resolveQueryExecutor(route);
        QueryResult countResult = executor.query(context, new DefaultQueryRequest(countRequest));
        if (!(countResult instanceof DefaultQueryResult result)
                || result.getAggregationResult() == null) {
            throw new TeaQLRuntimeException("Exact page count is not supported for route: " + route);
        }
        rows.addAggregationResult(context, result.getAggregationResult());
        return rows;
    }

    /**
     * Executes a framework-owned nested query under the trace established by its
     * already-authorized root request. Nested relation requests are generated as
     * query expressions and deliberately do not carry a second business purpose.
     */
    public <T extends Entity> SmartList<T> internalExecuteForList(
            UserContext context, SearchRequest<T> request) {
        if (context.getTraceChain() == null || context.getTraceChain().isEmpty()) {
            throw new TeaQLRuntimeException(
                    "[INTERNAL QUERY CONTEXT REQUIRED] Nested query has no authorized root trace.");
        }
        enforceMaterializedLimit(request, SearchRequest.DEFAULT_HARD_LIMIT);
        if (requestPolicy != null) {
            requestPolicy.enforceSelect(context, request);
        }
        return executeForListResolved(context, request);
    }

    private static void enforceMaterializedLimit(SearchRequest<?> request, int hardLimit) {
        Slice slice = request.getSlice();
        if (slice == null) {
            throw new TeaQLRuntimeException("[QUERY HARD LIMIT] An unlimited materialized query is not allowed");
        }
        int requested = slice.getSize();
        if (requested <= 0) {
            slice.setSize(hardLimit);
        } else if (requested > hardLimit) {
            throw new TeaQLRuntimeException("[QUERY HARD LIMIT] Requested limit " + requested
                    + " exceeds hard limit " + hardLimit);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> SmartList<T> executeForListResolved(
            UserContext context, SearchRequest<T> request) {
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
        RuntimeTelemetry.Scope providerScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("provider", route + ".query", Map.of(
                        "teaql.provider.kind", route,
                        "teaql.provider.operation", "query")));
        QueryResult queryResult;
        try {
            queryResult = queryExecutor.query(context, queryRequest);
            providerScope.success();
        } catch (RuntimeException | Error error) {
            providerScope.failure(error);
            throw error;
        }
        if (queryResult instanceof DefaultQueryResult) {
            return (SmartList<T>) ((DefaultQueryResult) queryResult).getResult();
        }
        throw new TeaQLRuntimeException(
                "Unsupported QueryResult type: " + queryResult.getClass().getName());
    }

    public <T extends Entity> AggregationResult aggregation(UserContext context, SearchRequest<T> request) {
        if (request.purpose() == null || request.purpose().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[PURPOSE REQUIRED] Missing .purpose() on aggregation.");
        }
        if (requestPolicy != null) {
            requestPolicy.enforceSelect(context, request);
        }
        boolean pushedComment = false;
        boolean pushedPurpose = false;
        if (request.comment() != null && !request.comment().trim().isEmpty()) {
            context.pushTrace(request.comment());
            pushedComment = true;
        }
        if (request.purpose() != null && !request.purpose().trim().isEmpty()) {
            context.pushTrace(request.purpose());
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
            QueryResult queryResult = queryExecutor.query(context, queryRequest);
            if (queryResult instanceof DefaultQueryResult) {
                return ((DefaultQueryResult) queryResult).getAggregationResult();
            }
            throw new TeaQLRuntimeException("Unsupported QueryResult type: " + queryResult.getClass().getName());
        } finally {
            if (pushedPurpose) context.popTrace();
            if (pushedComment) context.popTrace();
        }
    }

    public void saveGraph(UserContext context, Object items) {
        if (items instanceof Entity) {
            saveGraph(context, (Entity) items);
        } else if (items instanceof Collection) {
            for (Object item : (Collection<?>) items) {
                saveGraph(context, item);
            }
        }
    }

    private static final String SAVE_GRAPH_ACTIVE_ROUTE_KEY = "__teaql_save_graph_route__";

    public void saveGraph(UserContext context, Entity entity) {
        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("mutation", entity.typeName() + ".save", Map.of(
                        "teaql.entity.type", entity.typeName(),
                        "teaql.mutation.kind", "save")));
        try {
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[AUDIT REQUIRED] Missing .auditAs() or .setComment() before saveGraph().");
        }
        boolean pushed = false;
        if (entity.getComment() != null && !entity.getComment().trim().isEmpty()) {
            context.pushTrace(entity.getComment());
            pushed = true;
        }
        try {
            // Get entity's own EntityRoot
            EntityRoot entityRoot = ((BaseEntity) entity).getEntityRoot();

            // Allocate identifiers before roots are merged. New children commonly
            // receive their field updates while their id is still null, so those
            // updates cannot yet have been recorded in an EntityRoot ledger.
            assignMissingGraphIds(
                    context, entity, Collections.newSetFromMap(new IdentityHashMap<>()));

            // Merge related entities' EntityRoots into this one
            mergeRelatedEntityRoots(
                    entity,
                    entityRoot,
                    Collections.newSetFromMap(new IdentityHashMap<>()));
            recordGraphChanges(
                    entity,
                    entityRoot,
                    Collections.newSetFromMap(new IdentityHashMap<>()));

            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
            }

            Object activeRoute = context.extension(SAVE_GRAPH_ACTIVE_ROUTE_KEY);
            if (activeRoute == null) {
                context.putAttribute(SAVE_GRAPH_ACTIVE_ROUTE_KEY, route);
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

            Map<io.teaql.core.EntityKey, io.teaql.core.BaseEntity> realEntities = new java.util.HashMap<>();
            collectRealEntities(
                    entity,
                    realEntities,
                    Collections.newSetFromMap(new IdentityHashMap<>()));
            if (mutationExecutor instanceof TransactionExecutor transactionExecutor) {
                transactionExecutor.executeInTransaction(context, () -> {
                    executeLedgerPlan(context, entityRoot, mutationExecutor, realEntities);
                    return null;
                });
            } else {
                executeLedgerPlan(context, entityRoot, mutationExecutor, realEntities);
            }
            entityRoot.clearCurrentChangeSet();
            telemetryScope.success();
        } finally {
            if (pushed) context.popTrace();
        }
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    /**
     * Merge related entities' EntityRoots into the main entity's EntityRoot.
     * This ensures that when saving an Order, its OrderItems' changes are also saved.
     */
    private void assignMissingGraphIds(
            UserContext context, Entity entity, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity baseEntity) || !visited.add(entity)) {
            return;
        }

        if (entity.getId() == null && idGenerationService != null) {
            Long newId = idGenerationService.generateId(context, entity);
            baseEntity.__internalSet("id", newId);
            baseEntity.getEntityRoot().markAsNew(new EntityKey(entity.typeName(), newId));
        }

        visitRelatedEntities(
                entity, related -> assignMissingGraphIds(context, related, visited));
    }

    private void mergeRelatedEntityRoots(
            Entity entity, EntityRoot targetRoot, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity) || !visited.add(entity)) {
            return;
        }

        visitRelatedEntities(entity, related -> {
            BaseEntity relatedBase = (BaseEntity) related;
            EntityRoot relatedRoot = relatedBase.getEntityRoot();
            if (relatedRoot != null && relatedRoot != targetRoot) {
                targetRoot.mergeFrom(relatedRoot);
                relatedBase.setEntityRoot(targetRoot);
            }
            mergeRelatedEntityRoots(related, targetRoot, visited);
        });
    }

    private void recordGraphChanges(
            Entity entity, EntityRoot targetRoot, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity baseEntity) || !visited.add(entity)) {
            return;
        }
        if (baseEntity.getId() != null) {
            EntityKey key = new EntityKey(baseEntity.typeName(), baseEntity.getId());
            for (String property : baseEntity.getUpdatedProperties()) {
                targetRoot.set(key, property, baseEntity.__internalGet(property));
            }
        }
        visitRelatedEntities(
                entity, related -> recordGraphChanges(related, targetRoot, visited));
    }

    private void visitRelatedEntities(
            Entity entity, java.util.function.Consumer<Entity> visitor) {

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        if (descriptor == null) return;

        for (PropertyDescriptor prop : descriptor.getProperties()) {
            if (!(prop instanceof io.teaql.core.meta.Relation)) continue;
            Object value = entity.getProperty(prop.getName());
            if (value instanceof Entity relEntity) {
                visitor.accept(relEntity);
            } else if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Entity relEntity) {
                        visitor.accept(relEntity);
                    }
                }
            } else if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item instanceof Entity relEntity) {
                        visitor.accept(relEntity);
                    }
                }
            }
        }
    }




    private void collectRealEntities(
            Entity entity,
            Map<EntityKey, BaseEntity> realEntities,
            Set<Entity> visited) {
        if (!(entity instanceof BaseEntity baseEntity) || !visited.add(entity)) return;
        if (baseEntity.getId() != null) {
            realEntities.put(new EntityKey(baseEntity.typeName(), baseEntity.getId()), baseEntity);
        }
        visitRelatedEntities(
                entity,
                related -> collectRealEntities(related, realEntities, visited));
    }

    private void executeLedgerPlan(UserContext context, EntityRoot root, MutationExecutor mutationExecutor, Map<EntityKey, BaseEntity> realEntities) {
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
            BaseEntity deleteEntity = realEntities.get(key);
            if (deleteEntity == null) {
                deleteEntity = (BaseEntity) descriptor.createEntity();
                deleteEntity.__internalSet("id", key.id());
                deleteEntity.set$status(io.teaql.core.EntityStatus.PERSISTED);
            }
            deleteEntity.markToRemove();
            if (root.getComment() != null) deleteEntity.setComment(root.getComment());

            DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                deleteEntity, DefaultMutationRequest.Action.DELETE);
            MutationResult result = mutateWithTelemetry(context, mutationExecutor, mutationRequest,
                    key.entity(), "delete");
            applyPersistedEntity(descriptor, deleteEntity, result);
            emitAuditEvent(context, deleteEntity, MutationAuditKind.DELETED, Collections.emptyMap());
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
                BaseEntity entity = realEntities.get(key);
                if (entity == null) {
                    entity = (BaseEntity) descriptor.createEntity();
                    entity.__internalSet("id", key.id());
                }
                Long version = root.getOriginalVersion(key);
                if (version != null) {
                    entity.__internalSet("version", version);
                }
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.updateProperty(change.getKey(), change.getValue());
                }
                if (root.getComment() != null) entity.setComment(root.getComment());

                DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                    entity, DefaultMutationRequest.Action.SAVE);
                MutationResult result = mutateWithTelemetry(context, mutationExecutor, mutationRequest,
                        entityName, "save");
                applyPersistedEntity(descriptor, entity, result);
                emitAuditEvent(context, entity, MutationAuditKind.CREATED, changes);
                entity.clearUpdatedProperties();
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
                BaseEntity entity = realEntities.get(key);
                if (entity == null) {
                    entity = (BaseEntity) descriptor.createEntity();
                    entity.__internalSet("id", key.id());
                }
                Long version = root.getOriginalVersion(key);
                if (version != null) {
                    entity.__internalSet("version", version);
                }
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.updateProperty(change.getKey(), change.getValue());
                }
                entity.set$status(io.teaql.core.EntityStatus.UPDATED);
                if (root.getComment() != null) entity.setComment(root.getComment());

                DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                    entity, DefaultMutationRequest.Action.SAVE);
                MutationAuditKind auditKind = entity.recoverItem()
                        ? MutationAuditKind.RECOVERED
                        : MutationAuditKind.UPDATED;
                MutationResult result = mutateWithTelemetry(context, mutationExecutor, mutationRequest,
                        entityName, auditKind.name().toLowerCase(Locale.ROOT));
                applyPersistedEntity(descriptor, entity, result);
                emitAuditEvent(context, entity, auditKind, changes);
                entity.clearUpdatedProperties();
            }
        }
    }

    private void applyPersistedEntity(
            EntityDescriptor descriptor, BaseEntity target, MutationResult result) {
        if (result == null || result.persistedEntity() == null) {
            throw new TeaQLRuntimeException(
                    "Mutation did not return the authoritative persisted entity for "
                            + descriptor.getType());
        }
        BaseEntity persisted = (BaseEntity) result.persistedEntity();
        for (PropertyDescriptor property : descriptor.getProperties()) {
            if (!persisted.isPropertyLoaded(property.getName())) continue;
            target.__internalSet(property.getName(), persisted.getProperty(property.getName()));
        }
        target.set$status(((BaseEntity) persisted).get$status());
        target.clearUpdatedProperties();
    }

    private MutationResult mutateWithTelemetry(
            UserContext context,
            MutationExecutor executor,
            MutationRequest request,
            String entityType,
            String operation) {
        String provider = executor.getClass().getSimpleName();
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("provider", provider + ".mutation", Map.of(
                        "teaql.provider.kind", provider,
                        "teaql.provider.operation", operation,
                        "teaql.entity.type", entityType)));
        try {
            MutationResult result = executor.mutate(context, request);
            scope.success();
            return result;
        } catch (RuntimeException | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    public void delete(UserContext context, Entity entity) {
        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("mutation", entity.typeName() + ".delete", Map.of(
                        "teaql.entity.type", entity.typeName(),
                        "teaql.mutation.kind", "delete")));
        try {
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            throw new TeaQLRuntimeException("[AUDIT REQUIRED] Missing .auditAs() or .setComment() before delete().");
        }
        boolean pushed = false;
        if (entity.getComment() != null && !entity.getComment().trim().isEmpty()) {
            context.pushTrace(entity.getComment());
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

            if (entity instanceof BaseEntity baseEntity && entity.getId() != null) {
                EntityRoot entityRoot = baseEntity.getEntityRoot();
                entityRoot.markAsDelete(new EntityKey(entity.typeName(), entity.getId()));
            }

            DefaultMutationRequest.Action action = DefaultMutationRequest.Action.DELETE;
            MutationRequest mutationRequest = new DefaultMutationRequest(entity, action);
            mutateWithTelemetry(context, mutationExecutor, mutationRequest,
                    entity.typeName(), "delete");
            emitAuditEvent(context, entity, MutationAuditKind.DELETED, Collections.emptyMap());
            telemetryScope.success();
        } finally {
            if (pushed) context.popTrace();
        }
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    private void emitAuditEvent(
            UserContext context,
            Entity entity,
            MutationAuditKind kind,
            Map<String, Object> changedValues) {
        List<AuditFieldChange> changes = new ArrayList<>();
        if (changedValues != null) {
            for (Map.Entry<String, Object> entry : changedValues.entrySet()) {
                if (entry.getKey() == null || entry.getKey().startsWith("_")) continue;
                changes.add(new AuditFieldChange(entry.getKey(), null, entry.getValue()));
            }
        }
        changes.sort(Comparator.comparing(AuditFieldChange::field));
        RawAuditEvent rawEvent = new RawAuditEvent(
                kind, entity.typeName(), entity.getId(), changes, context.getTraceChain());

        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("audit", entity.typeName() + ".audit", Map.of(
                        "teaql.entity.type", entity.typeName(),
                        "teaql.mutation.kind", kind.name().toLowerCase(Locale.ROOT),
                        "teaql.audit.changed_field_count", changes.size())));
        try {

        // The standard sink is server-owned by TeaQLRuntime and cannot be replaced by
        // dynamic input or an application capability registered on UserContext.
        if (logSink != null) {
            logSink.writeAuditEvent(context, rawEvent);
        }

        AppAuditEventSink appSink = context.capability(AppAuditEventSink.class);
        if (appSink != null) {
            appSink.onAuditEvent(context, buildSafeAuditEvent(rawEvent));
        }
        telemetryScope.success();
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    private SafeAuditEvent buildSafeAuditEvent(RawAuditEvent event) {
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(event.entityType());
        Set<String> maskFields = descriptor == null
                ? Collections.emptySet()
                : new HashSet<>(descriptor.getAuditMaskFields());
        Integer maxLength = descriptor == null ? null : descriptor.getAuditValueMaxLength();
        List<SafeAuditField> fields = new ArrayList<>();
        for (AuditFieldChange change : event.changes()) {
            Object value = change.newValue() != null ? change.newValue() : change.oldValue();
            String raw = value == null ? null : String.valueOf(value);
            boolean masked = raw != null && maskFields.contains(change.field());
            String safe = masked ? maskAuditValue(raw) : raw;
            int rawLength = raw == null ? 0 : raw.length();
            boolean truncated = safe != null && maxLength != null && safe.length() > maxLength;
            if (truncated) safe = limitAuditValue(safe, maxLength);
            fields.add(new SafeAuditField(
                    change.field(), safe, masked, truncated,
                    raw == null ? null : rawLength,
                    safe == null ? null : safe.length()));
        }
        return new SafeAuditEvent(
                event.kind(), event.entityType(), event.entityId(), fields, event.traceChain());
    }

    static String maskAuditValue(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.chars().allMatch(Character::isDigit)) return "*".repeat(value.length());
        if (value.length() < 8) return "*".repeat(value.length());
        return value.substring(0, 2)
                + "*".repeat(value.length() - 4)
                + value.substring(value.length() - 2);
    }

    static String limitAuditValue(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        if (maxLength <= 3) return "*".repeat(maxLength);
        int remaining = maxLength - 3;
        int head = remaining / 2;
        int tail = remaining - head;
        return value.substring(0, head) + "..." + value.substring(value.length() - tail);
    }

    public static class Builder {
        private EntityMetaFactory metadata;
        private DataServiceRegistry registry = new DefaultDataServiceRegistry();
        private RequestPolicy requestPolicy;
        private InternalIdGenerationService idGenerationService;
        private RuntimeLogSink logSink;
        private RuntimeTelemetry telemetry = RuntimeTelemetry.NOOP;

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

        public Builder telemetry(RuntimeTelemetry telemetry) {
            this.telemetry = telemetry;
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
