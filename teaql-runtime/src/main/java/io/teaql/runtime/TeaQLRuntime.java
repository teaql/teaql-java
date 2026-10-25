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
import io.teaql.core.blob.BlobException;
import io.teaql.core.blob.BlobMutationParticipant;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.blob.BlobRuntime;
import io.teaql.runtime.businessid.SystemBusinessClock;
import java.util.*;
import java.util.stream.Stream;

public class TeaQLRuntime implements AutoCloseable {
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
    private final boolean ownsTelemetry;
    private boolean telemetryClosed;
    private final SchemaExecutor schemaExecutor;
    private final BusinessIdService businessIdService;
    private final BusinessIdSchemaContributor businessIdSchemaContributor;
    private final BusinessIdKeyProvider businessIdKeyProvider;
    private final RoundTripReferenceCodec roundTripReferenceCodec;
    private final BlobMutationParticipant blobMutationParticipant;
    private final BlobRuntime blobRuntime;
    private final BusinessClock businessClock;
    private final Map<String, Checker<?>> checkers = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<GeneratedSchemaBootstrap> generatedBootstraps =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Set<String> emittedMutationGovernanceWarnings =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> requiredCapabilities = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        this.ownsTelemetry = builder.telemetry == null;
        this.telemetry = builder.telemetry != null ? builder.telemetry
                : TelemetryStartup.initialize(System.getenv(), TeaQLRuntime.class.getClassLoader());
        this.schemaExecutor = builder.schemaExecutor;
        this.businessIdService = builder.businessIdService;
        this.businessIdSchemaContributor = builder.businessIdSchemaContributor;
        this.businessIdKeyProvider = builder.businessIdKeyProvider;
        this.roundTripReferenceCodec = builder.roundTripReferenceProvider == null
                ? null
                : new RoundTripReferenceCodec(builder.roundTripReferenceProvider);
        this.businessClock = builder.businessClock;
        this.blobRuntime = builder.blobRuntime;
        this.blobMutationParticipant = builder.blobMutationParticipant;
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

    @Override
    public synchronized void close() {
        if (!ownsTelemetry || telemetryClosed) return;
        telemetryClosed = true;
        try { telemetry.shutdown(); }
        catch (Throwable ignored) { TelemetryStartup.notice("TeaQL telemetry: owned shutdown failed; business outcome unchanged."); }
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

    BlobRuntime getBlobRuntime() { return blobRuntime; }

    BlobMutationParticipant getBlobMutationParticipant() {
        return blobMutationParticipant;
    }

    /** Installs a passive generated manifest. Database schemas remain unchanged. */
    public TeaQLRuntime install(RuntimeModule module) {
        module.install(metadata);
        module.checkers().forEach(checker -> checkers.put(checker.type(), checker));
        generatedBootstraps.addAll(module.bootstraps());
        requiredCapabilities.addAll(module.requiredCapabilities());
        return this;
    }

    /** Pure required-capability check; infrastructure readiness is a separate explicit operation. */
    public void validateCapabilities() {
        for (String capability : requiredCapabilities) {
            if ("secure_document.v1".equals(capability) && roundTripReferenceCodec != null) continue;
            if ("blob.v1".equals(capability) && blobRuntime != null) {
                blobRuntime.validateConfiguration(metadata,registry);
                continue;
            }
            String code = "blob.v1".equals(capability) ? "BLOB_CAPABILITY_MISSING" : "VIEW_CAPABILITY_MISSING";
            throw new TeaQLRuntimeException(code + ": required capability " + capability + " is not installed");
        }
    }

    /** Explicit startup probe after all modules/routes are installed. Never migrates or starts workers. */
    public void validateReadiness() {
        validateCapabilities();
        boolean hasBlob=false;
        for (EntityDescriptor root:metadata.allEntityDescriptors()) {
            for (EntityDescriptor descriptor=root;descriptor!=null;descriptor=descriptor.getParent()) {
                for (PropertyDescriptor property:descriptor.getProperties()) {
                    if (property.blobFieldSpec()!=null || property.getType()!=null && property.getType().javaType()==BlobRef.class) hasBlob=true;
                }
            }
        }
        if (hasBlob && blobRuntime==null) throw new BlobException(BlobException.Code.BLOB_CAPABILITY_MISSING);
        if (blobRuntime!=null) {
            blobRuntime.validateConfiguration(metadata,registry);
            blobRuntime.validateReadiness();
        }
    }

    GeneratedSchemaBootstrap getGeneratedSchemaBootstrap() {
        if (generatedBootstraps.isEmpty()) return null;
        return context -> generatedBootstraps.forEach(bootstrap -> bootstrap.ensure(context));
    }

    public void recordExecutionMetadata(UserContext context, ExecutionMetadata metadata) {
        if (metadata.getSqlExecutionCorrelation() != null) {
            try {
                telemetry.recordDiagnostic(SqlDiagnosticEvent.project(metadata));
            } catch (Throwable ignored) {
                // No unsafe fallback: exporting diagnostics never changes business behavior.
            }
        }
        boolean logging = metadata.getOperation() == DataServiceOperation.QUERY
                ? isQueryExecutionLoggingEnabled() : isMutationExecutionLoggingEnabled();
        if (logging && logSink != null) {
            try {
                logSink.writeExecutionLog(context,
                        LogPrivacy.sql(metadata, requiresSensitiveSqlLogData()));
            } catch (RuntimeException ignored) {
                // Diagnostics are fail-open. Never print the exception: a custom
                // sink failure may itself contain the unmasked SQL or values.
            }
        }
    }

    io.teaql.core.SqlStatementObservation startSqlStatement(String backend, DataServiceOperation operation) {
        try { if (!telemetry.recordsSqlDiagnostics()) return io.teaql.core.SqlStatementObservation.NOOP; }
        catch (Throwable ignored) { return io.teaql.core.SqlStatementObservation.NOOP; }
        String family = operation == null ? "unknown" : operation.name().toLowerCase(java.util.Locale.ROOT);
        var scope = RuntimeTelemetry.startSafely(telemetry, new RuntimeTelemetry.Operation("provider", backend + ".statement",
                Map.of("teaql.provider.kind", backend, "teaql.provider.operation", family,
                        "teaql.provider.execution", "statement")));
        return new io.teaql.core.SqlStatementObservation() {
            @Override public boolean enabled() { return true; }
            @Override public io.teaql.core.SqlExecutionCorrelation correlation() { return scope.correlation(); }
            @Override public void success() { scope.success(); }
            @Override public void failure(Throwable failure) { scope.failure(failure); }
            @Override public void detach() { scope.detach(); }
            @Override public Activation activate() { var activation = scope.activate(); return activation::close; }
        };
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> SmartList<T> executeForList(UserContext context, SearchRequest<T> request) {
        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", request.getTypeName() + ".list",
                        Map.of("teaql.entity.type", request.getTypeName())));
        try {
        QueryIntent intent = QueryIntent.of(request.comment(), request.purpose());
        var requestScope = QueryRequestScope.capture(request);
        enforceMaterializedLimit(request, request.hardLimit());
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        // Existing customer policies may use generated typed request methods.
        // Freeze their result only after that hook, while the intent header was
        // captured beforehand and cannot be overwritten by the policy.
        request = new QueryRequestSnapshot<>(request, requestScope);
        QueryOperation countOperation = requestedListCount(request);
        SearchRequest<?> countRequest = null;
        String countAlias = null;
        if (countOperation != null) {
            countAlias = countOperation.alias() == null ? TeaQLConstants.ROOT_LIST_PARAMETER_NAME : countOperation.alias();
            BaseRequest<?> snapshot = (BaseRequest<?>) request;
            countRequest = new QueryRequestSnapshot(snapshot.internalCountRequest(countAlias),
                    requestScope.deriveOperation(request.getTypeName(), countOperation), request);
            String alias = countAlias;
            snapshot.getAggregations().getAggregates().removeIf(aggregate -> alias.equals(aggregate.name()));
        }
        SmartList<T> result = executeForListResolved(context, request, intent);
        if (countRequest != null && !(result.aggregationNumberProperties().get(countAlias) instanceof Number)) {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(request.getTypeName());
            String route = descriptor.getDataService();
            if (route == null || route.isEmpty()) route = "default";
            QueryResult count = registry.resolveQueryExecutor(route).query(context, new DefaultQueryRequest(countRequest, intent));
            if (!(count instanceof DefaultQueryResult counted) || counted.getAggregationResult() == null) {
                throw new TeaQLRuntimeException("Requested list count is not supported for route: " + route);
            }
            if (result.isSharedEmpty()) result = new SmartList<>();
            result.addAggregationResult(context, counted.getAggregationResult());
        }
        if (countRequest != null) result.__internalSetPageCountAlias(countAlias);
        telemetryScope.success(Map.of("teaql.result.cardinality", result.size()));
        return result;
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    /** A declared ungrouped count accompanies rows through the normal list entry.
     * Grouped/mixed aggregate requests retain their existing aggregate semantics. */
    private static QueryOperation requestedListCount(SearchRequest<?> request) {
        QueryOperation operation = request.pageCountOperation();
        if (operation == null || !(request instanceof BaseRequest<?>)) return null;
        var aggregations = request.getAggregations();
        if (aggregations == null || !aggregations.getSimpleDimensions().isEmpty()
                || !aggregations.getComplexDimensions().isEmpty() || aggregations.getAggregates().size() != 1) return null;
        String alias = operation.alias() == null ? TeaQLConstants.ROOT_LIST_PARAMETER_NAME : operation.alias();
        var aggregate = aggregations.getAggregates().get(0);
        return alias.equals(aggregate.name()) && aggregate.getExpression() instanceof AggrExpression expression
                && expression.getOperator() == AggrFunction.COUNT ? operation : null;
    }

    /** Executes a business-facing streaming query after the same policy gate as list queries. */
    public <T extends Entity> Stream<T> executeForStream(
            UserContext context, SearchRequest<T> request) {
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", request.getTypeName() + ".stream",
                        Map.of("teaql.entity.type", request.getTypeName())));
        try {
        QueryIntent intent = QueryIntent.of(request.comment(), request.purpose());
        var requestScope = QueryRequestScope.capture(request);
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        request = new QueryRequestSnapshot<>(request, requestScope);
        return RuntimeQueryStream.wrap(executeForStreamResolved(context, request, intent), scope);
        } catch (RuntimeException | Error failure) {
            scope.failure(failure); throw failure;
        }
    }

    /**
     * Executes a framework-owned nested stream under an already-authorized root query. The
     * nested request still passes through QueryPolicy before reaching the provider.
     */
    public <T extends Entity> Stream<T> internalExecuteForStream(
            UserContext context, SearchRequest<T> request) {
        QueryIntent intent = requireInheritedQueryIntent(request);
        var requestScope = QueryRequestScope.capture(request);
        if (queryPolicy != null) {
            queryPolicy.enforceSelect(context, request);
        }
        request = new QueryRequestSnapshot<>(request, requestScope);
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
        if (slice.getOffset() < 0) {
            throw new IllegalArgumentException("offset must not be negative");
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
        SaveCompletion[] completion={null};
        try {
            MutationIntent intent = MutationIntent.of(entity.getComment());
            validateCapabilities();
            if (entity instanceof BaseEntity base && base.__internalHasMutationLedger() && base.getEntityMutationLedger().hasUnknownTransactionOutcome()) {
                throw new TransactionCompletionException(TransactionCompletion.Outcome.UNKNOWN,null);
            }
            checkAndFix(context, entity);
            // Get entity's own EntityMutationLedger
            EntityMutationLedger entityMutationLedger = ((BaseEntity) entity).getEntityMutationLedger();
            if (entityMutationLedger.hasUnknownTransactionOutcome()) {
                throw new TransactionCompletionException(TransactionCompletion.Outcome.UNKNOWN,null);
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
            entityMutationLedger.requireSubmissionOwner(mutationExecutor instanceof TransactionExecutor transaction
                    ?transaction.activeTransactionIdentity():null);

            Object graphOwner=mutationExecutor instanceof TransactionExecutor owner?owner.activeTransactionIdentity():null;

            // Allocate identifiers before roots are merged. New children commonly
            // receive their field updates while their id is still null, so those
            // updates cannot yet have been recorded in an EntityMutationLedger ledger.
            assignMissingGraphIds(
                    context, entity, Collections.newSetFromMap(new IdentityHashMap<>()));

            // Merge related entities' EntityMutationLedgers into this one
            mergeRelatedEntityMutationLedgers(
                    entity,
                    entityMutationLedger,
                    Collections.newSetFromMap(new IdentityHashMap<>()),graphOwner);
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

            Map<io.teaql.core.EntityKey, io.teaql.core.BaseEntity> realEntities = new java.util.HashMap<>();
            collectRealEntities(
                    entity,
                    realEntities,
                    Collections.newSetFromMap(new IdentityHashMap<>()));
            Map<BaseEntity, PersistenceState> persistenceStates = new IdentityHashMap<>();
            Map<BaseEntity,BaseEntity.PendingChanges> previousChanges=new IdentityHashMap<>();
            realEntities.values().forEach(value -> persistenceStates.put(
                    value,
                    new PersistenceState(
                            value.getVersion(), value.get$status(),
                            value.isPropertyLoaded(BaseEntity.VERSION_PROPERTY),entityMutationLedger.isNew(new EntityKey(value.typeName(),value.getId())))));
            realEntities.values().forEach(value -> previousChanges.put(value,value.__internalSnapshotChanges()));
            MutationPlan mutationPlan = buildMutationPlan(entity, entityMutationLedger, realEntities, intent);
            requireSingleMutationRoute(mutationPlan, route);
            MutationGovernanceSnapshot governance = reviewMutationPlan(context, mutationPlan);
            BlobSavePlan blobPlan = planBlobMutations(context, mutationPlan, mutationExecutor);
            var dynamicChanges = entityMutationLedger.currentChangeSet().dynamicChanges();
            var dynamicRequests = new ArrayList<io.teaql.data.dynamic.DynamicGraphMutation>();
            dynamicChanges.forEach((key, mutations) -> {
                if (!entityMutationLedger.deletedKeys().contains(key)) mutations.values().forEach(mutation -> dynamicRequests.add(
                        new io.teaql.data.dynamic.DynamicGraphMutation(io.teaql.data.dynamic.DynamicOwnerRef.of(key.entity(), key.id()), mutation,
                                realEntities.get(key).dynamicFields().metadata())));
            });
            Map<EntityKey, io.teaql.data.dynamic.DynamicFieldValues> dynamicReadbacks = dynamicRequests.isEmpty() ? Collections.emptyMap() : new HashMap<>();
            List<PendingMutation> completed;
            try {
                if (mutationExecutor instanceof TransactionExecutor transactionExecutor) {
                    completed = transactionExecutor.executeInTransaction(context, () -> {
                        Object physicalOwner=transactionExecutor.activeTransactionIdentity();
                        if (transactionExecutor.supportsTransactionCompletion() && physicalOwner==null) {
                            throw new TeaQLRuntimeException("Physical transaction identity is required for graph completion");
                        }
                        entityMutationLedger.requireSubmissionOwner(physicalOwner);
                        if (transactionExecutor.supportsTransactionCompletion()) {
                            SaveCompletion attempt=new SaveCompletion(context,entityMutationLedger,physicalOwner,persistenceStates,previousChanges,telemetryScope);
                            try { transactionExecutor.afterTransaction(attempt); }
                            catch (RuntimeException | Error registrationFailure) { attempt.beforeNotifications(TransactionCompletion.Outcome.ROLLED_BACK); throw registrationFailure; }
                            completion[0]=attempt;
                        }
                        BlobMutationParticipant.Prepared blobPrepared = blobPlan == null ? null
                                : blobPlan.plan().prepare(context, blobPlan.resource());
                        if (blobPlan != null && blobPrepared == null) {
                            throw new BlobException(BlobException.Code.BLOB_CAPABILITY_MISSING);
                        }
                        io.teaql.data.dynamic.DynamicFieldsFacade dynamic = null;
                        if (!dynamicRequests.isEmpty()) {
                            dynamic = context.dynamicFields().comment(intent.comment()).purpose("runtime: persist audited graph extensions");
                            dynamic.prepareGraphMutations(transactionExecutor.transactionResource(), dynamicRequests);
                        }
                        List<PendingMutation> results = executeLedgerPlan(context, entityMutationLedger, mutationExecutor,
                                realEntities, governance, intent, traceScopes, graphScope);
                        if (dynamic != null) {
                            dynamic.applyGraphMutations(transactionExecutor.transactionResource(), dynamicRequests);
                            for (var entry : dynamicChanges.entrySet()) {
                                if (entityMutationLedger.deletedKeys().contains(entry.getKey())) continue;
                                BaseEntity target = realEntities.get(entry.getKey());
                                if (target == null) throw new TeaQLRuntimeException("Dynamic mutation has no materialized target");
                                var selection = new io.teaql.data.dynamic.DynamicFieldSelection();
                                Set<String> codes = new TreeSet<>(target.dynamicFields().selectedCodes());
                                codes.addAll(entry.getValue().keySet());
                                for (String code : codes) selection.select(code, target.dynamicFields().metadata().requireType(code));
                                var value = dynamic.owner(entry.getKey().entity(), entry.getKey().id()).readAll(selection);
                                if (value == null) throw new TeaQLRuntimeException("Dynamic mutation readback omitted owner");
                                dynamicReadbacks.put(entry.getKey(), value);
                            }
                        }
                        if (blobPrepared != null) {
                            blobPrepared.apply(context, blobPlan.resource(), persistedBlobEntities(results));
                        }
                        if (transactionExecutor.supportsTransactionCompletion()) {
                            completion[0].stage(results);
                            for (PendingMutation mutation:results) {
                                applyPersistedEntity(mutation.descriptor(),mutation.target(),mutation.result());
                                mutation.target().__internalStartUnsubmittedChanges();
                            }
                            dynamicReadbacks.forEach((key,values) -> {
                                BaseEntity target=realEntities.get(key);
                                target.__internalHydrateDynamicFields(values,target.__internalLoadState().withDynamicSelection(values.selectedCodes()));
                            });
                        }
                        return results;
                    });
                } else {
                    if (!dynamicRequests.isEmpty()) throw new TeaQLRuntimeException("DYNAMIC_FIELD_TRANSACTION_BINDING_REQUIRED: native graph executor has no transaction");
                    completed = executeLedgerPlan(
                            context, entityMutationLedger, mutationExecutor, realEntities, governance, intent, traceScopes, graphScope);
                }
            } catch (RuntimeException | Error failure) {
                if (completion[0]==null) restoreGraphPersistenceState(entity,entityMutationLedger,persistenceStates,Collections.newSetFromMap(new IdentityHashMap<>()));
                throw failure;
            }
            if (completion[0]!=null) return;
            dynamicReadbacks.forEach((key, values) -> {
                BaseEntity target = realEntities.get(key);
                target.__internalHydrateDynamicFields(values, target.__internalLoadState().withDynamicSelection(values.selectedCodes()));
            });
            completeLedgerPlan(context, completed);
            entityMutationLedger.clearCurrentChangeSet();
            telemetryScope.success();
        } catch (RuntimeException | Error error) {
            if (completion[0]==null || !completion[0].telemetryFinished) {
                telemetryScope.failure(error); if (completion[0]!=null) completion[0].telemetryFinished=true;
            }
            throw error;
        }
    }

    private BlobSavePlan planBlobMutations(
            UserContext context, MutationPlan plan, MutationExecutor executor) {
        boolean touchesBlob = false;
        for (MutationOperation operation : plan.operations()) {
            for (EntityDescriptor descriptor = metadata.resolveEntityDescriptor(operation.entity().entity());
                    descriptor != null; descriptor = descriptor.getParent()) {
                for (PropertyDescriptor property : descriptor.getProperties()) {
                    if (property.blobFieldSpec() == null
                            && (property.getType() == null || property.getType().javaType() != BlobRef.class)) continue;
                    if (operation.kind() != MutationOperationKind.UPDATE
                            || operation.changedValues().containsKey(property.getName())) {
                        touchesBlob = true;
                    }
                }
            }
        }
        if (!touchesBlob) return null;
        BlobMutationParticipant participant = context.capability(BlobMutationParticipant.class);
        if (participant == null) throw new BlobException(BlobException.Code.BLOB_CAPABILITY_MISSING);
        if (!(executor instanceof TransactionExecutor transaction)) {
            throw new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED);
        }
        if (!transaction.supportsTransactionCompletion()) throw new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED);
        Object resource = transaction.transactionResource();
        if (resource == null) throw new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED);
        BlobMutationParticipant.Plan result = participant.plan(context, plan);
        if (result == null) throw new BlobException(BlobException.Code.BLOB_CAPABILITY_MISSING);
        result.requireTransaction(resource);
        return new BlobSavePlan(result, resource);
    }

    private record BlobSavePlan(BlobMutationParticipant.Plan plan, Object resource) {}

    private List<BlobMutationParticipant.PersistedEntity> persistedBlobEntities(List<PendingMutation> results) {
        List<BlobMutationParticipant.PersistedEntity> identities = new ArrayList<>();
        for (PendingMutation result : results) {
            Entity persisted = result.result() == null ? null : result.result().persistedEntity();
            if (persisted == null) throw new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED);
            MutationOperationKind kind = switch (result.auditKind()) {
                case CREATED -> MutationOperationKind.CREATE;
                case UPDATED -> MutationOperationKind.UPDATE;
                case DELETED -> MutationOperationKind.DELETE;
                case RECOVERED -> MutationOperationKind.RECOVER;
            };
            identities.add(new BlobMutationParticipant.PersistedEntity(result.plannedEntity(),
                    new EntityKey(persisted.typeName(), persisted.getId()), persisted.getVersion(), kind));
        }
        return List.copyOf(identities);
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
        if (checkers.isEmpty()) return;
        try (var invocation = io.teaql.core.checker.internal.CheckerInvocation.openMutation(context)) {
            context.beginFixEvidence();
            try {
                checkGraphMutations(context, entity, null,
                        Collections.newSetFromMap(new IdentityHashMap<>()));
                List<CheckResult> violations = (List<CheckResult>) invocation.attribute(Checker.TEAQL_DATA_CHECK_RESULT);
                if (violations != null && !violations.isEmpty()) {
                    throw new CheckException(new ArrayList<>(violations));
                }
            } finally {
                context.finishFixEvidence();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void checkGraphMutations(UserContext context, Entity entity,
            io.teaql.core.checker.ObjectLocation location, Set<Entity> visited) {
        if (!(entity instanceof BaseEntity base) || !visited.add(entity)) return;
        Checker checker = checkers.get(entity.runtimeType());
        if (checker == null) checker = checkers.get(entity.typeName());
        if (checker != null && checker.needCheck(context, base)) {
            checker.checkAndFix(context, base, location);
        }
        // An untouched partial parent must not hide a dirty descendant. Generated
        // Checkers may stop at that parent, so persistence walks the graph itself.
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        for (EntityDescriptor current = descriptor; current != null; current = current.getParent()) {
            for (PropertyDescriptor property : current.getProperties()) {
                if (!(property instanceof io.teaql.core.meta.Relation)) continue;
                String member = io.teaql.core.utils.NamingCase.toUnderlineCase(property.getName());
                var childLocation = location == null
                        ? io.teaql.core.checker.ObjectLocation.hashRoot(member) : location.member(member);
                Object value = entity.getProperty(property.getName());
                if (value instanceof Entity child) {
                    checkGraphMutations(context, child, childLocation, visited);
                } else if (value instanceof Iterable<?> children) {
                    int index = 0;
                    for (Object child : children) {
                        if (child instanceof Entity related) {
                            checkGraphMutations(context, related, childLocation.element(index), visited);
                        }
                        index++;
                    }
                }
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
            Entity entity, EntityMutationLedger targetRoot, Set<Entity> visited,Object owner) {
        if (!(entity instanceof BaseEntity) || !visited.add(entity)) {
            return;
        }

        visitRelatedEntities(entity, related -> {
            if (visited.contains(related)) return;
            BaseEntity relatedBase = (BaseEntity) related;
            EntityMutationLedger relatedRoot = relatedBase.getEntityMutationLedger();
            EntityKey relatedKey = new EntityKey(related.typeName(), related.getId());
            if (relatedBase.get$status() != EntityStatus.REFER
                    && targetRoot.moveEntityFrom(relatedRoot, relatedKey,owner)) {
                relatedBase.setEntityMutationLedger(targetRoot);
            }
            mergeRelatedEntityMutationLedgers(related, targetRoot, visited,owner);
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
            if (targetRoot.isNew(key)) {
                for (EntityDescriptor descriptor=metadata.resolveEntityDescriptor(entity.typeName());descriptor!=null;descriptor=descriptor.getParent()) {
                    for (PropertyDescriptor property:descriptor.getProperties()) {
                        String field=property.getName();
                        if (!field.equals("id") && !field.equals("version") && baseEntity.isPropertyLoaded(field)) targetRoot.set(key,field,baseEntity.__internalGet(field));
                    }
                }
            }
            for (String property : baseEntity.getUpdatedProperties()) {
                targetRoot.set(key, property, baseEntity.__internalGet(property));
            }
            if (!targetRoot.isMarkedAsDelete(key)) baseEntity.__internalDynamicMutations().values().forEach(mutation -> targetRoot.setDynamic(key, mutation));
        }
        visitRelatedEntities(
                entity, related -> recordGraphChanges(related, targetRoot, visited));
    }

    private void visitRelatedEntities(
            Entity entity, java.util.function.Consumer<Entity> visitor) {

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        if (descriptor == null) return;

        for (EntityDescriptor current = descriptor; current != null; current = current.getParent()) {
            for (PropertyDescriptor prop : current.getProperties()) {
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
        keys.addAll(changeSet.dynamicChanges().keySet());
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
                changes = changesWithDynamic(changeSet, key);
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
        var graphRedactions = new SqlIntentRedactions();
        realEntities.values().forEach(value -> graphRedactions.captureEntity(
                value, metadata.resolveEntityDescriptor(value.typeName())));
        realEntities.values().forEach(entity -> {
            var descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            if (entity.getDynamicFieldValues() != null) entity.getDynamicFieldValues().toMap().forEach((field, value) ->
                    graphRedactions.capture(List.of(SqlFieldLogPolicy.resolve(descriptor, "#" + field)), new Object[]{value.value()}));
            entity.__internalDynamicOriginalValues().forEach((field, value) -> graphRedactions.capture(
                    List.of(SqlFieldLogPolicy.resolve(descriptor, "#" + field)), new Object[]{value.value()}));
        });
        changeSet.changes().forEach((key, values) -> {
            var descriptor = metadata.resolveEntityDescriptor(key.entity());
            values.forEach((field, value) -> graphRedactions.capture(
                    List.of(SqlFieldLogPolicy.resolve(descriptor, field)), new Object[]{value}));
        });
        // Extension intents are not reflected in the predefined-field descriptor.
        // Capture their actual operands, not the mutation's deliberately safe toString.
        changeSet.dynamicChanges().forEach((key, values) -> {
            var descriptor = metadata.resolveEntityDescriptor(key.entity());
            values.forEach((field, mutation) -> graphRedactions.capture(
                    List.of(SqlFieldLogPolicy.resolve(descriptor, "#" + field)), new Object[]{mutation.value()}));
        });

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
                    deleteEntity, EntityPersistenceMutation.Action.DELETE, intent, mutationTrace(root, key, traceScopes, graphScope), target, graphRedactions);
                requests.add(mutationRequest);
                targets.add(target == null ? deleteEntity : target);
            }
            List<MutationResult> results = mutateBatchWithTelemetry(
                    context, mutationExecutor, intent, requests, batch.getKey(), "delete");
            for (int index = 0; index < requests.size(); index++) {
                completed.add(new PendingMutation(batch.getValue().get(index), descriptor, targets.get(index), results.get(index),
                        MutationAuditKind.DELETED, Collections.emptyMap(), governance, intent, requests.get(index).getTraceChain(), graphRedactions));
            }
        }

        // 2. Group changes
        Map<String, List<EntityKey>> insertBatches = new TreeMap<>();
        Map<String, List<EntityKey>> updateBatches = new TreeMap<>();

        Set<EntityKey> changedKeys = new TreeSet<>(changeSet.changes().keySet());
        changedKeys.addAll(changeSet.dynamicChanges().keySet());
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
                Map<String, Object> changes = changeSet.changes().getOrDefault(key, Map.of());
                BaseEntity target = realEntities.get(key);
                BaseEntity entity = mutationEntity(descriptor, target);
                entity.__internalSet("id", key.id());
                Long version = root.getOriginalVersion(key);
                if (version != null) {
                    entity.__internalSet("version", version);
                }
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.__internalApplyRecordedMutation(change.getKey(), change.getValue());
                }
                if (root.getComment() != null) entity.setComment(root.getComment());

                EntityPersistenceMutation mutationRequest = new EntityPersistenceMutation(
                    entity, EntityPersistenceMutation.Action.SAVE, intent, mutationTrace(root, key, traceScopes, graphScope), target, graphRedactions);
                requests.add(mutationRequest);
                targets.add(target == null ? entity : target);
                snapshots.add(snapshotChanges(changesWithDynamic(changeSet, key)));
            }
            List<MutationResult> results = mutateBatchWithTelemetry(
                    context, mutationExecutor, intent, requests, entityName, "save");
            for (int index = 0; index < requests.size(); index++) {
                completed.add(new PendingMutation(
                        keys.get(index), descriptor, targets.get(index), results.get(index),
                        MutationAuditKind.CREATED, snapshots.get(index), governance, intent, requests.get(index).getTraceChain(), graphRedactions));
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
                List<EntityKey> requestKeys = new ArrayList<>();
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
                        entity.__internalApplyRecordedMutation(change.getKey(), change.getValue());
                    }
                    if (changeSet.dynamicChanges().containsKey(key)) entity.__internalRequireVersionUpdate();
                    entity.set$status(recovering ? io.teaql.core.EntityStatus.UPDATED_RECOVER : io.teaql.core.EntityStatus.UPDATED);
                    if (root.getComment() != null) entity.setComment(root.getComment());

                    EntityPersistenceMutation mutationRequest = new EntityPersistenceMutation(
                        entity, EntityPersistenceMutation.Action.SAVE, intent, mutationTrace(root, key, traceScopes, graphScope), target, graphRedactions);
                    requests.add(mutationRequest);
                    requestKeys.add(key);
                    targets.add(target == null ? entity : target);
                    snapshots.add(snapshotChanges(changesWithDynamic(changeSet, key)));
                }
                MutationAuditKind auditKind = recovering ? MutationAuditKind.RECOVERED : MutationAuditKind.UPDATED;
                List<MutationResult> results = mutateBatchWithTelemetry(
                        context, mutationExecutor, intent, requests, entityName, auditKind.name().toLowerCase(Locale.ROOT));
                for (int index = 0; index < requests.size(); index++) {
                    completed.add(new PendingMutation(requestKeys.get(index), descriptor, targets.get(index), results.get(index),
                            auditKind, snapshots.get(index), governance, intent, requests.get(index).getTraceChain(), graphRedactions));
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
                    mutation.governance(), mutation.intent(), mutation.traceChain(), mutation.redactions());
            mutation.target().clearUpdatedProperties();
            // A committed delete supersedes any staged extension edits too.
            mutation.target().__internalClearDynamicMutations();
        }
    }

    private Map<String, Object> snapshotChanges(Map<String, Object> changes) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(changes));
    }

    private Map<String, Object> changesWithDynamic(EntityChangeSet changes, EntityKey key) {
        Map<String, Object> fields = new LinkedHashMap<>(changes.changes().getOrDefault(key, Map.of()));
        changes.dynamicChanges().getOrDefault(key, Map.of()).forEach((code, mutation) -> fields.put("#" + code, mutation));
        return fields;
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
            EntityKey plannedEntity,
            EntityDescriptor descriptor,
            BaseEntity target,
            MutationResult result,
            MutationAuditKind auditKind,
            Map<String, Object> changedValues,
            MutationGovernanceSnapshot governance, MutationIntent intent, List<TraceNode> traceChain,
            SqlIntentRedactions redactions) {}

    private record PersistenceState(
            Long version, io.teaql.core.EntityStatus status, boolean versionLoaded,boolean created) {}

    private record PreparedAudit(RawAuditEvent event,SqlIntentRedactions redactions) {}

    private final class SaveCompletion implements TransactionCompletion {
        private final UserContext context;
        private final EntityMutationLedger.Submission submission;
        private final Map<BaseEntity,PersistenceState> states;
        private final Map<BaseEntity,BaseEntity.PendingChanges> previousChanges;
        private List<PreparedAudit> audits=List.of();
        private final RuntimeTelemetry.Scope telemetryScope;
        private Outcome outcome;
        private boolean telemetryFinished;
        private boolean notified;
        SaveCompletion(UserContext context,EntityMutationLedger ledger,Object owner,
                Map<BaseEntity,PersistenceState> states,Map<BaseEntity,BaseEntity.PendingChanges> previousChanges,RuntimeTelemetry.Scope scope) {
            this.context=context; this.states=states; this.previousChanges=previousChanges; telemetryScope=scope;
            submission=ledger.beginSubmission(owner);
        }
        void stage(List<PendingMutation> results) {
            audits=results.stream().map(mutation -> new PreparedAudit(prepareAuditEvent(context,mutation.target(),mutation.result().persistedEntity(),
                    mutation.auditKind(),mutation.changedValues(),mutation.governance(),mutation.intent(),mutation.traceChain()),mutation.redactions())).toList();
            submission.stage();
        }
        @Override public void beforeNotifications(Outcome value) {
            if (outcome!=null) return;
            outcome=value; submission.complete(value);
            if (value==Outcome.ROLLED_BACK) {
                states.forEach((target,state) -> {
                    EntityMutationLedger ledger=target.getEntityMutationLedger();
                    EntityKey key=new EntityKey(target.typeName(),target.getId());
                    boolean cancelled=ledger.rebaseAfterRollback(key,state.version(),state.created());
                    if (cancelled) target.__internalStartUnsubmittedChanges();
                    else target.__internalRestoreChanges(previousChanges.get(target));
                    EntityStatus status=state.status();
                    if (state.created()) status=EntityStatus.NEW;
                    else if (ledger.isMarkedAsDelete(key)) status=EntityStatus.UPDATED_DELETED;
                    else if (ledger.recoveredKeys().contains(key)) status=EntityStatus.UPDATED_RECOVER;
                    else if (!ledger.currentChangeSet().fieldNames(key).isEmpty() && state.version()!=null && state.version()>0) status=EntityStatus.UPDATED;
                    else if (state.version()!=null && state.version()<0) status=EntityStatus.PERSISTED_DELETED;
                    else if (state.version()!=null && state.version()>0 && state.status()!=EntityStatus.REFER) status=EntityStatus.PERSISTED;
                    target.__internalRestorePersistenceState(state.created()?null:state.version(),status,state.versionLoaded());
                });
            }
            if (!telemetryFinished) {
                telemetryFinished=true;
                if (value==Outcome.COMMITTED) telemetryScope.success();
                else telemetryScope.failure(new TransactionCompletionException(value,null));
            }
        }
        @Override public void afterCompletion(Outcome value) {
            beforeNotifications(value);
            if (notified || value!=Outcome.COMMITTED) return;
            notified=true;
            Throwable failure=null;
            for (PreparedAudit audit:audits) {
                try { emitPreparedAuditEvent(context,audit.event(),audit.redactions()); }
                catch (RuntimeException | Error error) {
                    if (failure==null) failure=error;
                    else if (error instanceof Error && !(failure instanceof Error)) { error.addSuppressed(failure); failure=error; }
                    else if (failure!=error) failure.addSuppressed(error);
                }
            }
            if (failure instanceof Error fatal) throw fatal;
            if (failure instanceof RuntimeException failed) throw failed;
        }
    }

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
        for (EntityDescriptor current = descriptor; current != null; current = current.getParent()) {
            for (PropertyDescriptor property : current.getProperties()) {
                if (!persisted.isPropertyLoaded(property.getName())) continue;
                target.__internalSet(property.getName(), persisted.getProperty(property.getName()));
            }
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
            MutationGovernanceSnapshot governance, MutationIntent intent, List<TraceNode> traceChain,
            SqlIntentRedactions redactions) {
        emitPreparedAuditEvent(context,prepareAuditEvent(context,entity,entity,kind,changedValues,governance,intent,traceChain),redactions);
    }

    private RawAuditEvent prepareAuditEvent(UserContext context,Entity entity,Entity persisted,MutationAuditKind kind,
            Map<String,Object> changedValues,MutationGovernanceSnapshot governance,MutationIntent intent,List<TraceNode> traceChain) {
        List<AuditFieldChange> changes = new ArrayList<>();
        if (changedValues != null) {
            for (Map.Entry<String, Object> entry : changedValues.entrySet()) {
                if (entry.getKey() == null || entry.getKey().startsWith("_")) continue;
                Object value = entry.getValue();
                Object previous = null;
                if (value instanceof io.teaql.data.dynamic.DynamicFieldMutation mutation) {
                    if (entity instanceof BaseEntity base) {
                        var original = base.__internalDynamicOriginalValues().get(mutation.code());
                        if (original != null && original.isLoaded()) previous = original.value();
                    }
                    var details = new LinkedHashMap<String, Object>();
                    details.put("operation", mutation.kind().name());
                    if (mutation.kind() == io.teaql.data.dynamic.DynamicFieldMutation.Kind.SET) details.put("value", mutation.value());
                    value = Collections.unmodifiableMap(details);
                }
                changes.add(new AuditFieldChange(entry.getKey(), snapshotAuditValue(previous), snapshotAuditValue(value)));
            }
        }
        changes.sort(Comparator.comparing(AuditFieldChange::field));
        return new RawAuditEvent(
                kind,
                persisted.typeName(),
                persisted.getId(),
                changes,
                traceChain,
                context.getAttribute(GeneratedSchemaBootstrap.AUDIT_ACTOR_ATTRIBUTE, String.class),
                context.getAttribute(GeneratedSchemaBootstrap.AUDIT_CATEGORY_ATTRIBUTE, String.class),
                intent.auditReason(),
                persisted.getVersion(),
                java.time.Instant.now(),
                governance);
    }

    Object snapshotAuditValue(Object value) {
        return snapshotAuditValue(value,Collections.newSetFromMap(new IdentityHashMap<>()),0,new int[]{0});
    }
    private Object snapshotAuditValue(Object value,Set<Object> visited,int depth,int[] nodes) {
        if (++nodes[0]>10000 || depth>32) throw new TeaQLRuntimeException("Audit value exceeds snapshot bounds");
        if (value instanceof Entity entity) return new EntityKey(entity.typeName(),entity.getId());
        if (value instanceof byte[] bytes) return bytes.clone();
        if (value instanceof java.util.Date date) return new java.util.Date(date.getTime());
        if (value instanceof com.fasterxml.jackson.databind.JsonNode json) return json.deepCopy();
        if (!(value instanceof Map<?,?>) && !(value instanceof Collection<?>) && !(value instanceof Object[])) return value;
        if (!visited.add(value)) throw new TeaQLRuntimeException("Cyclic audit value");
        try {
            if (value instanceof Map<?,?> map) {
                Map<Object,Object> copy=new LinkedHashMap<>();
                map.forEach((key,entry) -> copy.put(key,snapshotAuditValue(entry,visited,depth+1,nodes))); return Collections.unmodifiableMap(copy);
            }
            Collection<?> entries=value instanceof Collection<?> collection?collection:Arrays.asList((Object[])value);
            List<Object> copy=new ArrayList<>(entries.size());
            for (Object entry:entries) copy.add(snapshotAuditValue(entry,visited,depth+1,nodes));
            return Collections.unmodifiableList(copy);
        } finally { visited.remove(value); }
    }

    private void emitPreparedAuditEvent(UserContext context,RawAuditEvent prepared,SqlIntentRedactions redactions) {
        RawAuditEvent rawEvent=new RawAuditEvent(prepared.kind(),prepared.entityType(),prepared.entityId(),prepared.changes(),prepared.traceChain(),
                prepared.actor(),prepared.category(),prepared.reason(),prepared.resultingVersion(),java.time.Instant.now(),prepared.governance());

        RuntimeTelemetry.Scope telemetryScope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("audit", rawEvent.entityType() + ".audit", Map.of(
                        "teaql.entity.type", rawEvent.entityType(),
                        "teaql.mutation.kind", rawEvent.kind().name().toLowerCase(Locale.ROOT),
                        "teaql.audit.changed_field_count", rawEvent.changes().size())));
        try {

        // The standard sink is server-owned by TeaQLRuntime and cannot be replaced by
        // dynamic input or an application capability registered on UserContext.
        if (logSink != null) {
            logSink.writeAuditEvent(context, LogPrivacy.audit(rawEvent, LogPrivacy.plaintextEnabled(), redactions));
        }

        AppAuditEventSink appSink = context.capability(AppAuditEventSink.class);
        if (appSink != null) {
            appSink.onAuditEvent(context, buildSafeAuditEvent(rawEvent, redactions));
        }
        telemetryScope.success();
        } catch (RuntimeException | Error error) {
            telemetryScope.failure(error);
            throw error;
        }
    }

    private SafeAuditEvent buildSafeAuditEvent(RawAuditEvent event, SqlIntentRedactions redactions) {
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(event.entityType());
        Set<String> maskFields = descriptor == null
                ? Collections.emptySet()
                : new HashSet<>(descriptor.getAuditMaskFields());
        Integer maxLength = descriptor == null ? null : descriptor.getAuditValueMaxLength();
        List<SafeAuditField> fields = new ArrayList<>();
        List<Object> sensitiveValues = new ArrayList<>();
        boolean allowPlaintext = LogPrivacy.plaintextEnabled();
        redactions.appendTo(sensitiveValues, allowPlaintext);
        for (AuditFieldChange change : event.changes()) {
            if ((!allowPlaintext && (maskFields.contains(change.field()) || change.field().startsWith("#"))) || LogPrivacy.credential(change.field())
                    || LogPrivacy.hasCredentials(change.oldValue()) || LogPrivacy.hasCredentials(change.newValue())) {
                sensitiveValues.add(change.oldValue());
                sensitiveValues.add(change.newValue());
            }
        }
        for (AuditFieldChange change : event.changes()) {
            Object value = change.newValue() != null ? change.newValue() : change.oldValue();
            String raw = value == null ? null : String.valueOf(value);
            boolean masked = raw != null && ((!allowPlaintext && (maskFields.contains(change.field()) || change.field().startsWith("#")))
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
        private RuntimeTelemetry telemetry;
        private SchemaExecutor schemaExecutor;
        private BusinessIdService businessIdService;
        private BusinessIdSchemaContributor businessIdSchemaContributor;
        private BusinessIdKeyProvider businessIdKeyProvider;
        private RoundTripReferenceProvider roundTripReferenceProvider;
        private BlobMutationParticipant blobMutationParticipant;
        private BlobRuntime blobRuntime;
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
            if (this.roundTripReferenceProvider != null) {
                throw new IllegalStateException("Secure document reference capability is already installed");
            }
            this.roundTripReferenceProvider = java.util.Objects.requireNonNull(provider, "provider");
            return this;
        }

        /** Installs the optional transaction participant. This alone does not advertise full blob.v1 readiness. */
        public Builder blobMutationParticipant(BlobMutationParticipant participant) {
            if (this.blobMutationParticipant != null || this.blobRuntime != null) {
                throw new IllegalStateException("Blob mutation participant is already installed");
            }
            this.blobMutationParticipant = Objects.requireNonNull(participant, "participant");
            return this;
        }

        /** Passive typed installation. Duplicate/replacement installation must be resolved before building. */
        public Builder blobRuntime(BlobRuntime services) {
            if (this.blobRuntime!=null || this.blobMutationParticipant!=null) throw new IllegalStateException("Blob capability is already installed");
            Objects.requireNonNull(services,"services");
            BlobMutationParticipant participant=Objects.requireNonNull(services.mutation(),"mutation");
            Objects.requireNonNull(services.upload(),"upload"); Objects.requireNonNull(services.reads(),"reads"); Objects.requireNonNull(services.maintenance(),"maintenance");
            this.blobRuntime=services; this.blobMutationParticipant=participant;
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
