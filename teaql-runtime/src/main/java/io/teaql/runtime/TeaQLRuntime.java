package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.Checker;
import io.teaql.core.businessid.BusinessIdAllocator;
import io.teaql.core.businessid.BusinessIdKeyProvider;
import io.teaql.core.businessid.BusinessIdSchemaContributor;
import io.teaql.core.businessid.BusinessIdService;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.runtime.businessid.DefaultBusinessIdService;
import io.teaql.core.reference.RoundTripReferenceCodec;
import io.teaql.core.reference.RoundTripReferenceProvider;
import io.teaql.runtime.businessid.SystemBusinessClock;
import java.util.*;
import java.util.stream.Stream;

public class TeaQLRuntime {
    private final EntityMetaFactory metadata;
    private final DataServiceRegistry registry;
    private final QueryPolicy queryPolicy;
    private final MutationPolicyRegistry mutationPolicyRegistry;
    private final MutationPolicyApprovalProvider mutationPolicyApprovalProvider;
    private final InternalIdGenerationService idGenerationService;
    private final RuntimeLogSink logSink;
    private final boolean queryExecutionLoggingEnabled;
    private final boolean mutationExecutionLoggingEnabled;
    private final RuntimeTelemetry telemetry;
    private final SchemaExecutor schemaExecutor;
    private final BusinessIdService businessIdService;
    private final BusinessIdSchemaContributor businessIdSchemaContributor;
    private final BusinessIdKeyProvider businessIdKeyProvider;
    private final RoundTripReferenceCodec roundTripReferenceCodec;
    private final BusinessClock businessClock;
    private final Map<String, Checker<?>> checkers = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<GeneratedSchemaBootstrap> generatedBootstraps =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Set<String> emittedMutationGovernanceWarnings =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private TeaQLRuntime(Builder builder) {
        this.metadata = builder.metadata;
        this.registry = builder.registry != null ? builder.registry : new DefaultDataServiceRegistry();
        this.queryPolicy = builder.queryPolicy;
        this.mutationPolicyRegistry = builder.mutationPolicyRegistry;
        this.mutationPolicyApprovalProvider = builder.mutationPolicyApprovalProvider;
        this.idGenerationService = builder.idGenerationService;
        this.logSink = builder.logSink;
        this.queryExecutionLoggingEnabled = builder.queryExecutionLoggingEnabled;
        this.mutationExecutionLoggingEnabled = builder.mutationExecutionLoggingEnabled;
        this.telemetry = builder.telemetry != null ? builder.telemetry : RuntimeTelemetry.NOOP;
        this.schemaExecutor = builder.schemaExecutor;
        this.businessIdService = builder.businessIdService;
        this.businessIdSchemaContributor = builder.businessIdSchemaContributor;
        this.businessIdKeyProvider = builder.businessIdKeyProvider;
        this.roundTripReferenceCodec = builder.roundTripReferenceProvider == null
                ? null
                : new RoundTripReferenceCodec(builder.roundTripReferenceProvider);
        this.businessClock = builder.businessClock;
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

    public QueryPolicy getQueryPolicy() {
        return queryPolicy;
    }

    public MutationPolicyRegistry getMutationPolicyRegistry() {
        return mutationPolicyRegistry;
    }

    public MutationPolicyApprovalProvider getMutationPolicyApprovalProvider() {
        return mutationPolicyApprovalProvider;
    }

    public InternalIdGenerationService getIdGenerationService() {
        return idGenerationService;
    }

    public RuntimeLogSink getLogSink() {
        return logSink;
    }

    public boolean isExecutionLoggingEnabled() {
        return queryExecutionLoggingEnabled || mutationExecutionLoggingEnabled;
    }

    public boolean isQueryExecutionLoggingEnabled() { return queryExecutionLoggingEnabled; }

    public boolean isMutationExecutionLoggingEnabled() { return mutationExecutionLoggingEnabled; }

    public boolean requiresSensitiveSqlLogData() {
        return logSink != null && logSink.requiresSensitiveSqlData() && LogPrivacy.plaintextEnabled();
    }

    public RuntimeTelemetry getTelemetry() {
        return telemetry;
    }

    SchemaExecutor getSchemaExecutor() {
        return schemaExecutor;
    }

    BusinessIdService getBusinessIdService() {
        return businessIdService;
    }

    BusinessClock getBusinessClock() {
        return businessClock;
    }

    BusinessIdSchemaContributor getBusinessIdSchemaContributor() {
        return businessIdSchemaContributor;
    }

    BusinessIdKeyProvider getBusinessIdKeyProvider() {
        return businessIdKeyProvider;
    }

    RoundTripReferenceCodec getRoundTripReferenceCodec() {
        return roundTripReferenceCodec;
    }

    /** Installs a passive generated manifest. Database schemas remain unchanged. */
    public TeaQLRuntime install(RuntimeModule module) {
        module.install(metadata);
        module.checkers().forEach(checker -> checkers.put(checker.type(), checker));
        generatedBootstraps.addAll(module.bootstraps());
        return this;
    }

    GeneratedSchemaBootstrap getGeneratedSchemaBootstrap() {
        if (generatedBootstraps.isEmpty()) return null;
        return context -> generatedBootstraps.forEach(bootstrap -> bootstrap.ensure(context));
    }

    public void recordExecutionMetadata(UserContext context, ExecutionMetadata metadata) {
        if (logSink != null) {
            try {
                logSink.writeExecutionLog(context,
                        LogPrivacy.sql(metadata, requiresSensitiveSqlLogData()));
            } catch (RuntimeException ignored) {
                // Diagnostics are fail-open. Never print the exception: a custom
                // sink failure may itself contain the unmasked SQL or values.
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForList(UserContext context, SearchRequest<T> request) {
        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", request.getTypeName() + ".list",
                        Map.of("teaql.entity.type", request.getTypeName())));
        try {
        QueryIntent intent = QueryIntent.of(request.comment(), request.purpose());
        enforceMaterializedLimit(request, request.hardLimit());
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        SmartList<T> result = executeForListResolved(context, request, intent);
        telemetryScope.success(Map.of("teaql.result.cardinality", result.size()));
        return result;
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForPage(
            UserContext context, SearchRequest<T> request, int offset, int limit) {
        QueryIntent intent = QueryIntent.of(request.comment(), request.purpose());
        if (!(request instanceof BaseRequest<?> baseRequest)) {
            throw new TeaQLRuntimeException("Paged execution requires a generated BaseRequest");
        }
        baseRequest.offset(offset, limit);
        SmartList<T> rows = executeForList(context, request);
        Object idSetAccuracy = context.getAttribute("teaql.idSet.countAccuracy");
        Object idSetCount = context.getAttribute("teaql.idSet.count");
        if ("EXACT".equals(idSetAccuracy) && idSetCount instanceof Number exactCount) {
            if (rows.isSharedEmpty()) rows = new SmartList<>();
            AggregationItem item = new AggregationItem();
            item.setValues(Map.of(
                    new SimpleNamedExpression(TeaQLConstants.ROOT_LIST_PARAMETER_NAME),
                    exactCount.longValue()));
            AggregationResult total = new AggregationResult();
            total.setData(List.of(item));
            rows.addAggregationResult(context, total);
            return rows;
        }
        SearchRequest<?> countRequest = baseRequest.internalCountRequest();
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) route = "default";
        QueryExecutor executor = registry.resolveQueryExecutor(route);
        QueryResult countResult = executor.query(context, new DefaultQueryRequest(countRequest, intent));
        if (!(countResult instanceof DefaultQueryResult result)
                || result.getAggregationResult() == null) {
            throw new TeaQLRuntimeException("Exact page count is not supported for route: " + route);
        }
        if (rows.isSharedEmpty()) {
            rows = new SmartList<>();
        }
        rows.addAggregationResult(context, result.getAggregationResult());
        return rows;
    }

    /** Executes a business-facing streaming query after the same policy gate as list queries. */
    public <T extends Entity> Stream<T> executeForStream(
            UserContext context, SearchRequest<T> request) {
        QueryIntent intent = QueryIntent.of(request.comment(), request.purpose());
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        return executeForStreamResolved(context, request, intent);
    }

    /**
     * Executes a framework-owned nested stream under an already-authorized root query. The
     * nested request still passes through QueryPolicy before reaching the provider.
     */
    public <T extends Entity> Stream<T> internalExecuteForStream(
            UserContext context, SearchRequest<T> request) {
        QueryIntent intent = requireInheritedQueryIntent(request);
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        return executeForStreamResolved(context, request, intent);
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> Stream<T> executeForStreamResolved(
            UserContext context, SearchRequest<T> request, QueryIntent intent) {
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
        String route = descriptor != null ? descriptor.getDataService() : null;
        if (route == null || route.isEmpty()) {
            route = "default";
        }
        DataServiceExecutor executor = registry.resolve(route);
        if (executor instanceof StreamingQueryExecutor streamingQueryExecutor) {
            return streamingQueryExecutor.queryForStream(context, new DefaultQueryRequest(request, intent));
        }
        throw new TeaQLRuntimeException("Streaming query is not supported for route: " + route);
    }

    /**
     * Executes a framework-owned nested query with the explicit provenance of its
     * already-authorized root request, never a Context trace stack. Nested relation
     * requests do not require a second caller-supplied business purpose.
     */
    public <T extends Entity> SmartList<T> internalExecuteForList(
            UserContext context, SearchRequest<T> request) {
        Map<String, Object> relationAttributes = new java.util.LinkedHashMap<>();
        relationAttributes.put("teaql.entity.type", request.getTypeName());
        copyRelationPlanAttribute(request, relationAttributes,
                "teaql.internal.top_n.parent_count", "teaql.relation.parent_count");
        copyRelationPlanAttribute(request, relationAttributes,
                "teaql.internal.top_n.per_parent_limit", "teaql.relation.per_parent_limit");
        copyRelationPlanAttribute(request, relationAttributes,
                "teaql.internal.top_n.probe_threshold", "teaql.relation.configured_probe_threshold");
        copyRelationPlanAttribute(request, relationAttributes,
                "teaql.internal.top_n.selected_plan", "teaql.relation.selected_plan");
        copyRelationPlanAttribute(request, relationAttributes,
                "teaql.internal.top_n.probe_count", "teaql.relation.probe_count");
        RuntimeTelemetry.Scope relationScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("relation_load", request.getTypeName() + ".relation",
                        relationAttributes));
        try {
        QueryIntent intent = requireInheritedQueryIntent(request);
        enforceMaterializedLimit(request, SearchRequest.DEFAULT_HARD_LIMIT);
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        SmartList<T> result = executeForListResolved(context, request, intent);
        relationScope.success(Map.of("teaql.result.cardinality", result.size()));
        return result;
        } catch (RuntimeException | Error error) {
            relationScope.failure(error);
            throw error;
        }
    }

    private static void copyRelationPlanAttribute(
            SearchRequest<?> request, Map<String, Object> attributes, String extension, String attribute) {
        Object value = request.getExtension(extension);
        if (value != null) attributes.put(attribute, value);
    }

    private static QueryIntent requireInheritedQueryIntent(SearchRequest<?> request) {
        QueryIntent intent = request.inheritedQueryIntent();
        if (intent == null) {
            throw new TeaQLRuntimeException(
                    "[INTERNAL QUERY CONTEXT REQUIRED] Nested query requires an explicit validated root request intent.");
        }
        return intent;
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
            UserContext context, SearchRequest<T> request, QueryIntent intent) {
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) {
            route = "default";
        }
        QueryExecutor queryExecutor = registry.resolveQueryExecutor(route);
        if (queryExecutor == null) {
            throw new TeaQLRuntimeException("No QueryExecutor registered for route: " + route);
        }
        QueryRequest queryRequest = new DefaultQueryRequest(request, intent);
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
        QueryIntent intent = QueryIntent.of(request.comment(), request.purpose());
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
        String route = descriptor.getDataService();
        if (route == null || route.isEmpty()) {
            route = "default";
        }
        QueryExecutor queryExecutor = registry.resolveQueryExecutor(route);
        if (queryExecutor == null) {
            throw new TeaQLRuntimeException("No QueryExecutor registered for route: " + route);
        }
        QueryRequest queryRequest = new DefaultQueryRequest(request, intent);
        QueryResult queryResult = queryExecutor.query(context, queryRequest);
        if (queryResult instanceof DefaultQueryResult) {
            return ((DefaultQueryResult) queryResult).getAggregationResult();
        }
        throw new TeaQLRuntimeException("Unsupported QueryResult type: " + queryResult.getClass().getName());
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

    public void saveGraph(UserContext context, Entity entity) {
        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("mutation", entity.typeName() + ".save", Map.of(
                        "teaql.entity.type", entity.typeName(),
                        "teaql.mutation.kind", "save")));
        try {
            MutationIntent intent = MutationIntent.of(entity.getComment());
            checkAndFix(context, entity);
            // Get entity's own EntityMutationLedger
            EntityMutationLedger entityMutationLedger = ((BaseEntity) entity).getEntityMutationLedger();

            // Allocate identifiers before roots are merged. New children commonly
            // receive their field updates while their id is still null, so those
            // updates cannot yet have been recorded in an EntityMutationLedger ledger.
            assignMissingGraphIds(
                    context, entity, Collections.newSetFromMap(new IdentityHashMap<>()));

            // Merge related entities' EntityMutationLedgers into this one
            mergeRelatedEntityMutationLedgers(
                    entity,
                    entityMutationLedger,
                    Collections.newSetFromMap(new IdentityHashMap<>()));
            recordGraphChanges(
                    entity,
                    entityMutationLedger,
                    Collections.newSetFromMap(new IdentityHashMap<>()));

            Map<EntityKey, MutationTraceScope> traceScopes = new HashMap<>();
            MutationTraceScope graphScope = MutationTraceScope.append(
                    null, entity.typeName(), entity.getId(), intent.comment());
            traceScopes.put(new EntityKey(entity.typeName(), entity.getId()), graphScope);
            Set<Entity> traceVisited = Collections.newSetFromMap(new IdentityHashMap<>());
            traceVisited.add(entity);
            visitRelatedEntities(entity, child -> collectMutationTraceScopes(child, graphScope, traceScopes, traceVisited));

            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) {
                route = "default";
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
            Map<BaseEntity, PersistenceState> persistenceStates = new IdentityHashMap<>();
            realEntities.values().forEach(value -> persistenceStates.put(
                    value,
                    new PersistenceState(
                            value.getVersion(), value.get$status(),
                            value.isPropertyLoaded(BaseEntity.VERSION_PROPERTY))));
            MutationPlan mutationPlan = buildMutationPlan(entity, entityMutationLedger, realEntities, intent);
            requireSingleMutationRoute(mutationPlan, route);
            MutationGovernanceSnapshot governance = reviewMutationPlan(context, mutationPlan);
            List<PendingMutation> completed;
            try {
                if (mutationExecutor instanceof TransactionExecutor transactionExecutor) {
                    completed = transactionExecutor.executeInTransaction(context, () ->
                            executeLedgerPlan(context, entityMutationLedger, mutationExecutor,
                                    realEntities, governance, intent, traceScopes, graphScope));
                } else {
                    completed = executeLedgerPlan(
                            context, entityMutationLedger, mutationExecutor, realEntities, governance, intent, traceScopes, graphScope);
                }
            } catch (RuntimeException | Error failure) {
                restoreGraphPersistenceState(
                        entity,
                        entityMutationLedger,
                        persistenceStates,
                        Collections.newSetFromMap(new IdentityHashMap<>()));
                throw failure;
            }
            completeLedgerPlan(context, completed);
            entityMutationLedger.clearCurrentChangeSet();
            telemetryScope.success();
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    /** One atomic plan cannot borrow a root provider for a different entity route. */
    private void requireSingleMutationRoute(MutationPlan plan, String rootRoute) {
        for (MutationOperation operation : plan.operations()) {
            String type = operation.entity().entity();
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(type);
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) route = "default";
            if (!rootRoute.equals(route)) {
                throw new TeaQLRuntimeException(
                        "[CROSS-PROVIDER MUTATION] Atomic mutation plan contains entity '"
                                + type + "' on route '" + route
                                + "' outside its root route '" + rootRoute
                                + "'. Use independently audited saves or explicit orchestration.");
            }
        }
    }

    private void collectMutationTraceScopes(Entity entity, MutationTraceScope parent,
            Map<EntityKey, MutationTraceScope> scopes, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity baseEntity) || !visited.add(entity)) return;
        MutationTraceScope active = MutationTraceScope.append(
                parent, entity.typeName(), entity.getId(), entity.getComment());
        EntityKey key = new EntityKey(entity.typeName(), entity.getId());
        // A reference cannot replace the lineage of its materialized counterpart.
        if (baseEntity.get$status() != io.teaql.core.EntityStatus.REFER) scopes.put(key, active);
        visitRelatedEntities(entity, child -> collectMutationTraceScopes(child, active, scopes, visited));
    }

    private List<TraceNode> mutationTrace(EntityMutationLedger ledger, EntityKey key,
            Map<EntityKey, MutationTraceScope> scopes, MutationTraceScope graphScope) {
        List<TraceNode> specific = ledger.getTraceChain(key);
        if (specific != null && !specific.isEmpty()) return specific;
        MutationTraceScope scope = scopes.getOrDefault(key, graphScope);
        return scope.recover();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void checkAndFix(UserContext context, Entity entity) {
        Checker checker = checkers.get(entity.runtimeType());
        if (checker == null) {
            checker = checkers.get(entity.typeName());
        }
        if (checker == null) {
            return;
        }
        try (var invocation = io.teaql.core.checker.internal.CheckerInvocation.open(context)) {
            context.beginFixEvidence();
            try {
                checker.checkAndFix(context, (BaseEntity) entity);
                List<CheckResult> violations = (List<CheckResult>) invocation.attribute(Checker.TEAQL_DATA_CHECK_RESULT);
                if (violations != null && !violations.isEmpty()) {
                    throw new CheckException(new ArrayList<>(violations));
                }
            } finally {
                context.finishFixEvidence();
            }
        }
    }

    /**
     * Merge related entities' EntityMutationLedgers into the main entity's EntityMutationLedger.
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
            baseEntity.getEntityMutationLedger().markAsNew(new EntityKey(entity.typeName(), newId));
        }

        visitRelatedEntities(
                entity, related -> assignMissingGraphIds(context, related, visited));
    }

    private void mergeRelatedEntityMutationLedgers(
            Entity entity, EntityMutationLedger targetRoot, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity) || !visited.add(entity)) {
            return;
        }

        visitRelatedEntities(entity, related -> {
            if (visited.contains(related)) return;
            BaseEntity relatedBase = (BaseEntity) related;
            EntityMutationLedger relatedRoot = relatedBase.getEntityMutationLedger();
            EntityKey relatedKey = new EntityKey(related.typeName(), related.getId());
            if (relatedBase.get$status() != EntityStatus.REFER
                    && targetRoot.mergeEntityFrom(relatedRoot, relatedKey)) {
                relatedBase.setEntityMutationLedger(targetRoot);
            }
            mergeRelatedEntityMutationLedgers(related, targetRoot, visited);
        });
    }

    private void recordGraphChanges(
            Entity entity, EntityMutationLedger targetRoot, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity baseEntity) || !visited.add(entity)) {
            return;
        }
        // A relation reference may share the materialized entity's ledger after
        // graph composition. It has no authoritative field snapshot of its own;
        // replaying shared dirty names through the reference would overwrite
        // real values with its local null placeholders.
        if (baseEntity.getId() != null
                && baseEntity.get$status() != io.teaql.core.EntityStatus.REFER) {
            EntityKey key = new EntityKey(baseEntity.typeName(), baseEntity.getId());
            if (baseEntity.recoverItem()) targetRoot.markAsRecover(key);
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
            realEntities.merge(
                    new EntityKey(baseEntity.typeName(), baseEntity.getId()),
                    baseEntity,
                    TeaQLRuntime::preferMaterializedEntity);
        }
        visitRelatedEntities(
                entity,
                related -> collectRealEntities(related, realEntities, visited));
    }

    /**
     * A hydrated graph can contain both a materialized entity and lightweight
     * relation references with the same logical key. Persistence must keep the
     * materialized instance: a later reference must not erase its loaded
     * version, fields, or mutation state merely because graph traversal reaches
     * the reference last.
     */
    private static BaseEntity preferMaterializedEntity(
            BaseEntity existing, BaseEntity candidate) {
        if (existing.get$status() == io.teaql.core.EntityStatus.REFER
                && candidate.get$status() != io.teaql.core.EntityStatus.REFER) {
            return candidate;
        }
        if (candidate.get$status() == io.teaql.core.EntityStatus.REFER
                && existing.get$status() != io.teaql.core.EntityStatus.REFER) {
            return existing;
        }
        if (existing.getVersion() == null && candidate.getVersion() != null) {
            return candidate;
        }
        return existing;
    }

    private MutationPlan buildMutationPlan(
            Entity rootEntity,
            EntityMutationLedger ledger,
            Map<EntityKey, BaseEntity> realEntities, MutationIntent intent) {
        EntityChangeSet changeSet = ledger.currentChangeSet();
        Set<EntityKey> deleted = ledger.deletedKeys();
        Set<EntityKey> created = ledger.newKeys();
        Set<EntityKey> keys = new TreeSet<>();
        keys.addAll(changeSet.changes().keySet());
        keys.addAll(deleted);
        keys.addAll(ledger.recoveredKeys());

        List<MutationOperation> operations = new ArrayList<>();
        for (EntityKey key : keys) {
            BaseEntity target = realEntities.get(key);
            MutationOperationKind kind;
            Map<String, Object> changes;
            if (deleted.contains(key)) {
                kind = MutationOperationKind.DELETE;
                changes = Map.of();
            } else {
                changes = changeSet.changes().getOrDefault(key, Map.of());
                if (created.contains(key) || key.id() == null) {
                    kind = MutationOperationKind.CREATE;
                } else if (ledger.recoveredKeys().contains(key)) {
                    kind = MutationOperationKind.RECOVER;
                } else {
                    kind = MutationOperationKind.UPDATE;
                }
            }
            Long originalVersion = ledger.getOriginalVersion(key);
            if (originalVersion == null && target != null) originalVersion = target.getVersion();
            operations.add(new MutationOperation(kind, key, originalVersion, changes));
        }
        return new MutationPlan(
                UUID.randomUUID().toString(),
                rootEntity.typeName() + ".saveGraph",
                rootEntity.typeName(),
                intent.auditReason(),
                operations);
    }

    private MutationGovernanceSnapshot reviewMutationPlan(
            UserContext context, MutationPlan plan) {
        Optional<MutationPolicy> resolved = mutationPolicyRegistry.resolve(plan.requestKey());
        MutationPolicySource source;
        MutationPolicyIdentity identity = null;
        MutationPolicyApprovalStatus approvalStatus;
        List<String> warnings = new ArrayList<>();

        if (resolved.isEmpty()) {
            source = MutationPolicySource.GENERATED_DEFAULT;
            approvalStatus = MutationPolicyApprovalStatus.NOT_APPLICABLE;
            warnings.add("MUTATION-POLICY-001");
        } else {
            source = MutationPolicySource.CUSTOMER;
            MutationPolicy policy = resolved.get();
            identity = Objects.requireNonNull(policy.identity(), "MutationPolicy.identity()");
            MutationDecision decision = Objects.requireNonNull(
                    policy.review(context, plan), "MutationPolicy.review()");
            if (!decision.allowed()) {
                throw new TeaQLRuntimeException(
                        "[MUTATION POLICY DENIED] " + decision.code() + ": "
                                + (decision.message() == null ? "mutation rejected" : decision.message()));
            }
            boolean approved = mutationPolicyApprovalProvider.findApproval(identity)
                    .map(MutationPolicyApproval::policy)
                    .filter(identity::equals)
                    .isPresent();
            approvalStatus = approved
                    ? MutationPolicyApprovalStatus.APPROVED
                    : MutationPolicyApprovalStatus.MISSING;
            if (!approved) warnings.add("MUTATION-POLICY-002");
        }

        List<MutationOperationSummary> operationSummaries = plan.operations().stream()
                .map(operation -> new MutationOperationSummary(
                        operation.kind(),
                        operation.entity(),
                        new ArrayList<>(operation.changedValues().keySet())))
                .toList();
        MutationGovernanceSnapshot snapshot = new MutationGovernanceSnapshot(
                plan.executionId(), plan.requestKey(), source, identity, approvalStatus,
                warnings, operationSummaries);
        for (String warning : warnings) {
            emitMutationGovernanceWarning(context, snapshot, warning);
        }
        return snapshot;
    }

    private void emitMutationGovernanceWarning(
            UserContext context,
            MutationGovernanceSnapshot snapshot,
            String warningCode) {
        if (logSink == null) return;
        String identity = snapshot.policy() == null
                ? "none"
                : snapshot.policy().id() + ":" + snapshot.policy().version()
                        + ":" + snapshot.policy().fingerprint();
        boolean first = emittedMutationGovernanceWarnings.add(
                snapshot.requestKey() + "|" + identity + "|" + warningCode);
        try {
            logSink.writeMutationGovernanceEvent(
                    context, new MutationGovernanceEvent(snapshot, warningCode, first));
        } catch (RuntimeException | Error ignored) {
            // Governance-warning delivery is fail-open. The retained audit
            // event still contains the complete snapshot.
        }
    }

    private List<PendingMutation> executeLedgerPlan(
            UserContext context,
            EntityMutationLedger root,
            MutationExecutor mutationExecutor,
            Map<EntityKey, BaseEntity> realEntities,
            MutationGovernanceSnapshot governance,
            MutationIntent intent, Map<EntityKey, MutationTraceScope> traceScopes, MutationTraceScope graphScope) {
        List<PendingMutation> completed = new ArrayList<>();
        EntityChangeSet changeSet = root.currentChangeSet();
        Set<EntityKey> deletedKeys = root.deletedKeys();
        Set<EntityKey> newKeys = root.newKeys();

        // 1. Execute Deletes
        List<EntityKey> sortedDeletedKeys = new ArrayList<>(deletedKeys);
        Collections.sort(sortedDeletedKeys);
        Map<String, List<EntityKey>> deleteBatches = new TreeMap<>();
        for (EntityKey key : sortedDeletedKeys) {
            deleteBatches.computeIfAbsent(key.entity(), ignored -> new ArrayList<>()).add(key);
        }
        for (var batch : deleteBatches.entrySet()) {
            List<EntityPersistenceMutation> requests = new ArrayList<>();
            List<BaseEntity> targets = new ArrayList<>();
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(batch.getKey());
            if (descriptor == null) throw new TeaQLRuntimeException("No entity descriptor for: " + batch.getKey());
            for (EntityKey key : batch.getValue()) {
                BaseEntity target = realEntities.get(key);
                BaseEntity deleteEntity = mutationEntity(descriptor, target);
                deleteEntity.__internalSet("id", key.id());
                Long originalVersion = root.getOriginalVersion(key);
                if (originalVersion == null && target != null) originalVersion = target.getVersion();
                if (originalVersion != null) deleteEntity.__internalSet("version", originalVersion);
                deleteEntity.set$status(io.teaql.core.EntityStatus.PERSISTED);
                deleteEntity.markForDeletion();
                if (root.getComment() != null) deleteEntity.setComment(root.getComment());

                EntityPersistenceMutation mutationRequest = new EntityPersistenceMutation(
                    deleteEntity, EntityPersistenceMutation.Action.DELETE, intent, mutationTrace(root, key, traceScopes, graphScope));
                requests.add(mutationRequest);
                targets.add(target == null ? deleteEntity : target);
            }
            List<MutationResult> results = mutateBatchWithTelemetry(
                    context, mutationExecutor, intent, requests, batch.getKey(), "delete");
            for (int index = 0; index < requests.size(); index++) {
                completed.add(new PendingMutation(descriptor, targets.get(index), results.get(index),
                        MutationAuditKind.DELETED, Collections.emptyMap(), governance, intent, requests.get(index).getTraceChain()));
            }
        }

        // 2. Group changes
        Map<String, List<EntityKey>> insertBatches = new TreeMap<>();
        Map<String, List<EntityKey>> updateBatches = new TreeMap<>();

        Set<EntityKey> changedKeys = new TreeSet<>(changeSet.changes().keySet());
        changedKeys.addAll(root.recoveredKeys());
        for (EntityKey key : changedKeys) {
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
            List<EntityPersistenceMutation> requests = new ArrayList<>();
            List<BaseEntity> targets = new ArrayList<>();
            List<Map<String, Object>> snapshots = new ArrayList<>();
            Collections.sort(keys);
            for (EntityKey key : keys) {
                Map<String, Object> changes = changeSet.changes().get(key);
                if (changes == null) continue;
                BaseEntity target = realEntities.get(key);
                BaseEntity entity = mutationEntity(descriptor, target);
                entity.__internalSet("id", key.id());
                Long version = root.getOriginalVersion(key);
                if (version != null) {
                    entity.__internalSet("version", version);
                }
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.updateProperty(change.getKey(), change.getValue());
                }
                if (root.getComment() != null) entity.setComment(root.getComment());

                EntityPersistenceMutation mutationRequest = new EntityPersistenceMutation(
                    entity, EntityPersistenceMutation.Action.SAVE, intent, mutationTrace(root, key, traceScopes, graphScope));
                requests.add(mutationRequest);
                targets.add(target == null ? entity : target);
                snapshots.add(snapshotChanges(changes));
            }
            List<MutationResult> results = mutateBatchWithTelemetry(
                    context, mutationExecutor, intent, requests, entityName, "save");
            for (int index = 0; index < requests.size(); index++) {
                completed.add(new PendingMutation(
                        descriptor, targets.get(index), results.get(index),
                        MutationAuditKind.CREATED, snapshots.get(index), governance, intent, requests.get(index).getTraceChain()));
            }
        }

        // 4. Execute Updates and Recoveries
        for (Map.Entry<String, List<EntityKey>> entry : updateBatches.entrySet()) {
            String entityName = entry.getKey();
            List<EntityKey> keys = entry.getValue();
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entityName);
            if (descriptor == null) {
                throw new TeaQLRuntimeException("No entity descriptor for: " + entityName);
            }
            // Separate recover from update: they use different version transitions.
            Collections.sort(keys);
            for (boolean recovering : List.of(false, true)) {
                List<EntityPersistenceMutation> requests = new ArrayList<>();
                List<BaseEntity> targets = new ArrayList<>();
                List<Map<String, Object>> snapshots = new ArrayList<>();
                for (EntityKey key : keys) {
                    if (root.recoveredKeys().contains(key) != recovering) continue;
                    BaseEntity target = realEntities.get(key);
                    Map<String, Object> changes = changeSet.changes().getOrDefault(key, Map.of());
                    BaseEntity entity = mutationEntity(descriptor, target);
                    entity.__internalSet("id", key.id());
                    Long version = root.getOriginalVersion(key);
                    if (version == null && target != null) version = target.getVersion();
                    if (version != null) {
                        entity.__internalSet("version", version);
                    }
                    for (Map.Entry<String, Object> change : changes.entrySet()) {
                        entity.updateProperty(change.getKey(), change.getValue());
                    }
                    entity.set$status(recovering ? io.teaql.core.EntityStatus.UPDATED_RECOVER : io.teaql.core.EntityStatus.UPDATED);
                    if (root.getComment() != null) entity.setComment(root.getComment());

                    EntityPersistenceMutation mutationRequest = new EntityPersistenceMutation(
                        entity, EntityPersistenceMutation.Action.SAVE, intent, mutationTrace(root, key, traceScopes, graphScope));
                    requests.add(mutationRequest);
                    targets.add(target == null ? entity : target);
                    snapshots.add(snapshotChanges(changes));
                }
                MutationAuditKind auditKind = recovering ? MutationAuditKind.RECOVERED : MutationAuditKind.UPDATED;
                List<MutationResult> results = mutateBatchWithTelemetry(
                        context, mutationExecutor, intent, requests, entityName, auditKind.name().toLowerCase(Locale.ROOT));
                for (int index = 0; index < requests.size(); index++) {
                    completed.add(new PendingMutation(descriptor, targets.get(index), results.get(index),
                            auditKind, snapshots.get(index), governance, intent, requests.get(index).getTraceChain()));
                }
            }
        }
        return completed;
    }

    private void completeLedgerPlan(UserContext context, List<PendingMutation> completed) {
        for (PendingMutation mutation : completed) {
            applyPersistedEntity(mutation.descriptor(), mutation.target(), mutation.result());
            emitAuditEvent(
                    context, mutation.target(), mutation.auditKind(), mutation.changedValues(),
                    mutation.governance(), mutation.intent(), mutation.traceChain());
            mutation.target().clearUpdatedProperties();
        }
    }

    private Map<String, Object> snapshotChanges(Map<String, Object> changes) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(changes));
    }

    private BaseEntity mutationEntity(EntityDescriptor descriptor, BaseEntity target) {
        try {
            return (BaseEntity) descriptor.createEntity();
        } catch (IllegalStateException missingSupplier) {
            if (target != null) return target;
            throw missingSupplier;
        }
    }

    private record PendingMutation(
            EntityDescriptor descriptor,
            BaseEntity target,
            MutationResult result,
            MutationAuditKind auditKind,
            Map<String, Object> changedValues,
            MutationGovernanceSnapshot governance, MutationIntent intent, List<TraceNode> traceChain) {}

    private record PersistenceState(
            Long version, io.teaql.core.EntityStatus status, boolean versionLoaded) {}

    private void restoreGraphPersistenceState(
            Entity entity,
            EntityMutationLedger ledger,
            Map<BaseEntity, PersistenceState> states,
            Set<Entity> visited) {
        if (!(entity instanceof BaseEntity baseEntity) || !visited.add(entity)) return;
        EntityKey key = new EntityKey(baseEntity.typeName(), baseEntity.getId());
        PersistenceState state = states.get(baseEntity);
        if (ledger.isNew(key)) {
            baseEntity.__internalRestorePersistenceState(
                    null, io.teaql.core.EntityStatus.NEW, false);
        } else if (state != null) {
            baseEntity.__internalRestorePersistenceState(
                    state.version(), state.status(), state.versionLoaded());
        }
        visitRelatedEntities(entity, related ->
                restoreGraphPersistenceState(related, ledger, states, visited));
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
            PersistenceMutation mutation,
            String entityType,
            String operation) {
        String provider = executor.getClass().getSimpleName();
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("provider", provider + ".mutation", Map.of(
                        "teaql.provider.kind", provider,
                        "teaql.provider.operation", operation,
                        "teaql.entity.type", entityType)));
        try {
            MutationResult result = executor.mutate(context, mutation);
            scope.success();
            return result;
        } catch (RuntimeException | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    private List<MutationResult> mutateBatchWithTelemetry(
            UserContext context, MutationExecutor executor, MutationIntent intent,
            List<EntityPersistenceMutation> requests, String entityType, String operation) {
        if (requests.size() < 2 || !(executor instanceof io.teaql.core.BatchMutationExecutor batchExecutor)) {
            List<MutationResult> results = new ArrayList<>();
            for (EntityPersistenceMutation request : requests) {
                results.add(mutateWithTelemetry(context, executor, request, entityType, operation));
            }
            return results;
        }
        var request = new io.teaql.core.MutationBatchRequest(intent, requests);
        String provider = executor.getClass().getSimpleName();
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("provider", provider + ".mutation.batch", Map.of(
                        "teaql.provider.kind", provider,
                        "teaql.provider.operation", operation,
                        "teaql.entity.type", entityType,
                        "teaql.batch.size", requests.size())));
        try {
            List<MutationResult> results = batchExecutor.mutateBatch(context, request);
            if (results == null || results.size() != requests.size()) {
                throw new TeaQLRuntimeException("Batch mutation must return one ordered result per item");
            }
            for (int index = 0; index < results.size(); index++) {
                MutationResult result = results.get(index);
                if (result == null || result.persistedEntity() == null) {
                    throw new TeaQLRuntimeException("Batch mutation did not return an authoritative persisted entity");
                }
                Entity expected = requests.get(index).getEntity();
                Entity persisted = result.persistedEntity();
                if (!expected.typeName().equals(persisted.typeName())
                        || !Objects.equals(expected.getId(), persisted.getId())) {
                    throw new TeaQLRuntimeException("Batch mutation result identity does not match its ordered command");
                }
            }
            scope.success();
            return results;
        } catch (RuntimeException | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    private void emitAuditEvent(
            UserContext context,
            Entity entity,
            MutationAuditKind kind,
            Map<String, Object> changedValues,
            MutationGovernanceSnapshot governance, MutationIntent intent, List<TraceNode> traceChain) {
        List<AuditFieldChange> changes = new ArrayList<>();
        if (changedValues != null) {
            for (Map.Entry<String, Object> entry : changedValues.entrySet()) {
                if (entry.getKey() == null || entry.getKey().startsWith("_")) continue;
                changes.add(new AuditFieldChange(entry.getKey(), null, entry.getValue()));
            }
        }
        changes.sort(Comparator.comparing(AuditFieldChange::field));
        RawAuditEvent rawEvent = new RawAuditEvent(
                kind,
                entity.typeName(),
                entity.getId(),
                changes,
                traceChain,
                context.getAttribute(GeneratedSchemaBootstrap.AUDIT_ACTOR_ATTRIBUTE, String.class),
                context.getAttribute(GeneratedSchemaBootstrap.AUDIT_CATEGORY_ATTRIBUTE, String.class),
                intent.auditReason(),
                entity.getVersion(),
                java.time.Instant.now(),
                governance);

        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("audit", entity.typeName() + ".audit", Map.of(
                        "teaql.entity.type", entity.typeName(),
                        "teaql.mutation.kind", kind.name().toLowerCase(Locale.ROOT),
                        "teaql.audit.changed_field_count", changes.size())));
        try {

        // The standard sink is server-owned by TeaQLRuntime and cannot be replaced by
        // dynamic input or an application capability registered on UserContext.
        if (logSink != null) {
            logSink.writeAuditEvent(context, LogPrivacy.audit(rawEvent, LogPrivacy.plaintextEnabled()));
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
        List<Object> sensitiveValues = new ArrayList<>();
        boolean allowPlaintext = LogPrivacy.plaintextEnabled();
        for (AuditFieldChange change : event.changes()) {
            if ((!allowPlaintext && maskFields.contains(change.field())) || LogPrivacy.credential(change.field())
                    || LogPrivacy.hasCredentials(change.oldValue()) || LogPrivacy.hasCredentials(change.newValue())) {
                sensitiveValues.add(change.oldValue());
                sensitiveValues.add(change.newValue());
            }
        }
        for (AuditFieldChange change : event.changes()) {
            Object value = change.newValue() != null ? change.newValue() : change.oldValue();
            String raw = value == null ? null : String.valueOf(value);
            boolean masked = raw != null && ((!allowPlaintext && maskFields.contains(change.field()))
                    || LogPrivacy.credential(change.field()) || LogPrivacy.hasCredentials(change.oldValue())
                    || LogPrivacy.hasCredentials(change.newValue()));
            boolean credential = LogPrivacy.credential(change.field())
                    || LogPrivacy.hasCredentials(change.oldValue()) || LogPrivacy.hasCredentials(change.newValue());
            String safe = masked ? (credential ? LogPrivacy.REDACTED : maskAuditValue(raw))
                    : LogPrivacy.scrub(raw, sensitiveValues);
            int rawLength = raw == null ? 0 : raw.length();
            boolean truncated = safe != null && maxLength != null && safe.length() > maxLength;
            if (truncated) safe = limitAuditValue(safe, maxLength);
            fields.add(new SafeAuditField(
                    change.field(), safe, masked, truncated,
                    raw == null ? null : rawLength,
                    safe == null ? null : safe.length()));
        }
        List<Object> intentValues = new ArrayList<>(sensitiveValues);
        if (event.entityId() != null) intentValues.add(event.entityId());
        return new SafeAuditEvent(
                event.kind(), event.entityType(), event.entityId(), fields,
                LogPrivacy.trace(event.traceChain(), intentValues), event.governance());
    }

    static String maskAuditValue(String value) {
        if (value == null || value.isEmpty()) return value;
        int length = value.codePointCount(0, value.length());
        if (length < 8 || value.codePoints().allMatch(c -> c >= '0' && c <= '9')) return "*".repeat(length);
        return value.substring(0, value.offsetByCodePoints(0, 2))
                + "*".repeat(length - 4)
                + value.substring(value.offsetByCodePoints(0, length - 2));
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
        private QueryPolicy queryPolicy;
        private MutationPolicyRegistry mutationPolicyRegistry = MutationPolicyRegistry.empty();
        private MutationPolicyApprovalProvider mutationPolicyApprovalProvider =
                MutationPolicyApprovalProvider.none();
        private InternalIdGenerationService idGenerationService;
        private RuntimeLogSink logSink = new DefaultTextRuntimeLogSink();
        // Operation logging is default-on, while the default sink deliberately
        // excludes bind values and rendered Debug SQL.
        private boolean queryExecutionLoggingEnabled = true;
        private boolean mutationExecutionLoggingEnabled = true;
        private RuntimeTelemetry telemetry = RuntimeTelemetry.NOOP;
        private SchemaExecutor schemaExecutor;
        private BusinessIdService businessIdService;
        private BusinessIdSchemaContributor businessIdSchemaContributor;
        private BusinessIdKeyProvider businessIdKeyProvider;
        private RoundTripReferenceProvider roundTripReferenceProvider;
        private BusinessClock businessClock = SystemBusinessClock.INSTANCE;

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
            if (schemaExecutor == null && executor instanceof SchemaExecutor schema) {
                schemaExecutor = schema;
            }
            return this;
        }

        public Builder queryPolicy(QueryPolicy queryPolicy) {
            this.queryPolicy = queryPolicy;
            return this;
        }

        public Builder mutationPolicyRegistry(MutationPolicyRegistry mutationPolicyRegistry) {
            this.mutationPolicyRegistry = Objects.requireNonNull(mutationPolicyRegistry);
            return this;
        }

        public Builder mutationPolicyApprovalProvider(
                MutationPolicyApprovalProvider mutationPolicyApprovalProvider) {
            this.mutationPolicyApprovalProvider =
                    Objects.requireNonNull(mutationPolicyApprovalProvider);
            return this;
        }

        public Builder idGenerationService(InternalIdGenerationService idGenerationService) {
            this.idGenerationService = idGenerationService;
            return this;
        }

        /** Installs a Business ID service without an infrastructure schema contribution. */
        public Builder businessIdService(BusinessIdService businessIdService) {
            this.businessIdService = businessIdService;
            return this;
        }

        /** Installs the time source visible to domain behavior in each context. */
        public Builder businessClock(BusinessClock businessClock) {
            this.businessClock = java.util.Objects.requireNonNull(businessClock, "businessClock");
            return this;
        }

        /**
         * Installs one provider-backed allocator for generated Fix execution and
         * the explicit {@code context.ensureSchema()} lifecycle.
         */
        public <A extends BusinessIdAllocator & BusinessIdSchemaContributor>
                Builder businessIdInfrastructure(A allocator) {
            java.util.Objects.requireNonNull(allocator, "allocator");
            this.businessIdService = new DefaultBusinessIdService(allocator);
            this.businessIdSchemaContributor = allocator;
            return this;
        }

        /** Installs application-owned versioned key material for the default profile. */
        public Builder businessIdKeyProvider(BusinessIdKeyProvider keyProvider) {
            this.businessIdKeyProvider =
                    java.util.Objects.requireNonNull(keyProvider, "keyProvider");
            return this;
        }

        /** Installs context-bound identity serialization without coupling it to a Web framework. */
        public Builder roundTripReferenceProvider(RoundTripReferenceProvider provider) {
            this.roundTripReferenceProvider = java.util.Objects.requireNonNull(provider, "provider");
            return this;
        }

        public Builder logSink(RuntimeLogSink logSink) {
            this.logSink = logSink;
            return this;
        }

        /**
         * Controls construction and delivery of execution-log metadata.
         * Defaults to true. Disabling this does not disable audit events or
         * runtime telemetry, which have independent lifecycle and sampling.
         */
        public Builder executionLogging(boolean enabled) {
            this.queryExecutionLoggingEnabled = enabled;
            this.mutationExecutionLoggingEnabled = enabled;
            return this;
        }

        public Builder queryExecutionLogging(boolean enabled) {
            this.queryExecutionLoggingEnabled = enabled;
            return this;
        }

        public Builder mutationExecutionLogging(boolean enabled) {
            this.mutationExecutionLoggingEnabled = enabled;
            return this;
        }

        /**
         * Selects the built-in value-bearing, copy-paste SQL destination.
         * This does not change the independent Query and Mutation logging
         * controls. Ordinary RuntimeTelemetry and audit delivery are also
         * independent.
         */
        public Builder diagnosticSqlLogging(boolean enabled) {
            this.logSink = enabled
                    ? new SensitiveDiagnosticTextRuntimeLogSink()
                    : new DefaultTextRuntimeLogSink();
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
