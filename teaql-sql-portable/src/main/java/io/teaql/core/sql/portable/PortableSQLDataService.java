package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.runtime.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortableSQLDataService implements DataServiceExecutor, QueryExecutor, StreamingQueryExecutor, BatchMutationExecutor, TransactionExecutor {

    private final String name;
    private final DataServiceCapabilities capabilities;
    private final TeaQLDatabase database;
    private final EntityMetaFactory metadata;
    private final Map<String, PortableSQLRepository<?>> repositories = new ConcurrentHashMap<>();
    private final PortableSQLRepository.PortableSQLRepositoryResolver resolver = this::getRepository;
    private io.teaql.core.sql.dialect.SqlDialect dialect;
    private TopNRelationPlanPolicy topNRelationPlanPolicy = TopNRelationPlanPolicy.WINDOW;

    static final String TOP_N_PARENT_COUNT = "teaql.internal.top_n.parent_count";
    static final String TOP_N_PER_PARENT_LIMIT = "teaql.internal.top_n.per_parent_limit";
    static final String TOP_N_PROBE_THRESHOLD = "teaql.internal.top_n.probe_threshold";
    static final String TOP_N_SELECTED_PLAN = "teaql.internal.top_n.selected_plan";
    static final String TOP_N_PROBE_COUNT = "teaql.internal.top_n.probe_count";

    public PortableSQLDataService(String name, TeaQLDatabase database, EntityMetaFactory metadata) {
        this.name = name;
        this.database = database;
        this.metadata = metadata;
        this.capabilities = new DataServiceCapabilities();
        this.capabilities.setQuery(true);
        this.capabilities.setMutation(true);
        this.capabilities.setBatchMutation(true);
        this.capabilities.setTransaction(true);
        this.capabilities.setStreamingQuery(true);
    }

    @Override
    public String name() {
        return name;
    }

    public void setDialect(io.teaql.core.sql.dialect.SqlDialect dialect) {
        this.dialect = dialect;
    }

    public void setTopNRelationPlanPolicy(TopNRelationPlanPolicy policy) {
        this.topNRelationPlanPolicy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public DataServiceCapabilities capabilities() {
        return capabilities;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> PortableSQLRepository<T> getRepository(String typeName) {
        return (PortableSQLRepository<T>) repositories.computeIfAbsent(typeName, t -> {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(t);
            if (descriptor == null) {
                throw new TeaQLRuntimeException("Entity descriptor not found for type: " + t);
            }
            PortableSQLRepository<?> repo =
                    new PortableSQLRepository<>(descriptor, database, resolver, metadata);
            if (this.dialect != null) {
                repo.setDialect(this.dialect);
            }
            return (PortableSQLRepository<T>) repo;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public QueryResult query(UserContext context, QueryRequest request) {
        if (!(request instanceof DefaultQueryRequest)) {
            throw new TeaQLRuntimeException("Unsupported QueryRequest in PortableSQLDataService");
        }
        SearchRequest<?> searchRequest = ((DefaultQueryRequest) request).getSearchRequest();
        SqlIntentRedactions intent = SqlDiagnosticRequest.source(context, searchRequest);
        if (intent == null) intent = new SqlIntentRedactions();
        var statements = new ArrayList<ExecutionMetadata>();
        searchRequest = SqlDiagnosticRequest.collecting(searchRequest, intent, request.intent(), statements::add);
        String typeName = searchRequest.getTypeName();
        PortableSQLRepository<?> repository = getRepository(typeName);
        if (searchRequest.hasSimpleAgg()) {
            AggregationResult aggregation =
                    repository.doAggregateInternal(context, (SearchRequest) searchRequest, intent);
            return new DefaultQueryResult(new SmartList<>(), aggregation, statements);
        }
        SmartList<?> result = repository.loadInternal(context, (SearchRequest) searchRequest, intent);
        
        if (searchRequest.enhanceRelations() != null && !searchRequest.enhanceRelations().isEmpty()) {
            enhanceRelations(context, (SmartList<Entity>) result, searchRequest, intent);
        }
        attachDynamicAggregations(context, (SmartList<Entity>) result, searchRequest, intent);
        
        return new DefaultQueryResult((SmartList<Entity>) result, null, statements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> java.util.stream.Stream<T> queryForStream(UserContext context, QueryRequest request) {
        if (!(request instanceof DefaultQueryRequest query)) {
            throw new TeaQLRuntimeException("Unsupported QueryRequest in PortableSQLDataService");
        }
        SearchRequest<T> searchRequest = (SearchRequest<T>) query.getSearchRequest();
        SqlIntentRedactions source = SqlDiagnosticRequest.source(context, searchRequest);
        SearchRequest<T> scoped = SqlDiagnosticRequest.forExecution(searchRequest, source, request.intent());
        if (scoped.hasSimpleAgg() || !scoped.enhanceRelations().isEmpty() || !scoped.enhanceChildren().isEmpty()) {
            throw new TeaQLRuntimeException("Streaming aggregation/relation enhancement is not supported; stream root rows only");
        }
        return this.<T>getRepository(scoped.getTypeName()).streamInternal(context, scoped);
    }

    private void attachDynamicAggregations(
            UserContext userContext,
            SmartList<Entity> results,
            SearchRequest<?> parentRequest, SqlIntentRedactions intent) {
        List<SimpleAggregation> attributes = parentRequest.getDynamicAggregateAttributes();
        if (results == null || results.isEmpty() || attributes == null || attributes.isEmpty()) {
            return;
        }

        Map<Long, Entity> parentsById = results.mapById();
        if (parentsById.isEmpty()) return;

        for (SimpleAggregation attribute : attributes) {
            SearchRequest<?> aggregateRequest = attribute.getAggregateRequest();
            String partitionProperty = aggregateRequest.getPartitionProperty();
            if (partitionProperty == null || partitionProperty.isBlank()) {
                throw new TeaQLRuntimeException(
                        "Dynamic aggregate '"
                                + attribute.getName()
                                + "' is missing its relation partition property");
            }

            io.teaql.core.internal.TempRequest request =
                    dynamicAggregateRequest(aggregateRequest, partitionProperty, intent, parentRequest);
            request.groupBy(partitionProperty);
            request.appendSearchCriteria(
                    request.createBasicSearchCriteria(
                            partitionProperty, io.teaql.core.criteria.Operator.IN, parentsById.keySet()));

            PortableSQLRepository<?> aggregateRepository =
                    getRepository(aggregateRequest.getTypeName());
            AggregationResult aggregation =
                    aggregateRepository.doAggregateInternal(userContext, request, intent == null ? null : intent.copy());
            if (attribute.isSingleNumber()) {
                for (Entity parent : parentsById.values()) {
                    parent.addDynamicProperty(attribute.getName(), 0);
                }
                aggregation
                        .toSimpleMap()
                        .forEach(
                                (parentId, value) -> {
                                    Entity parent = parentByAggregationKey(parentsById, parentId);
                                    if (parent != null) {
                                        parent.addDynamicProperty(attribute.getName(), value);
                                    }
                                });
                continue;
            }

            for (Map<String, Object> values : aggregation.toList()) {
                Object parentId = values.remove(partitionProperty);
                Entity parent = parentByAggregationKey(parentsById, parentId);
                if (parent != null) {
                    parent.appendDynamicProperty(attribute.getName(), values);
                }
            }
        }
    }

    private SqlDiagnosticRequest dynamicAggregateRequest(SearchRequest<?> aggregateRequest,
            String partitionProperty, SqlIntentRedactions intent, SearchRequest<?> parentRequest) {
        EntityDescriptor aggregateDescriptor = metadata.resolveEntityDescriptor(aggregateRequest.getTypeName());
        PropertyDescriptor partition = findProperty(aggregateDescriptor, partitionProperty);
        if (partition instanceof Relation relation && shouldHandle(aggregateDescriptor, relation)) {
            PropertyDescriptor reverse = relation.getReverseProperty();
            EntityDescriptor parentDescriptor = metadata.resolveEntityDescriptor(parentRequest.getTypeName());
            while (reverse != null && parentDescriptor != null) {
                if (reverse.getOwner() == parentDescriptor) {
                    return SqlDiagnosticRequest.forRelation(
                            aggregateRequest, intent, parentRequest, reverse.getName());
                }
                parentDescriptor = parentDescriptor.getParent();
            }
        }
        // Arbitrary partitions still inherit their request origin, but cannot claim a model edge.
        return SqlDiagnosticRequest.forDerived(aggregateRequest, intent, parentRequest);
    }

    private Entity parentByAggregationKey(Map<Long, Entity> parentsById, Object parentId) {
        if (parentId instanceof Number number) {
            return parentsById.get(number.longValue());
        }
        if (parentId == null) return null;
        try {
            return parentsById.get(Long.valueOf(String.valueOf(parentId)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
    
    private void enhanceRelations(
            UserContext userContext, SmartList<Entity> dataSet, SearchRequest<?> request, SqlIntentRedactions intent) {
        if (dataSet == null || dataSet.isEmpty()) {
            return;
        }
        Map<String, SearchRequest> enhanceProperties = request.enhanceRelations();
        if (enhanceProperties == null || enhanceProperties.isEmpty()) return;

        EntityDescriptor entityDescriptor = metadata.resolveEntityDescriptor(request.getTypeName());

        enhanceProperties.forEach(
                (p, r) -> {
                    PropertyDescriptor property = findProperty(entityDescriptor, p);
                    if (property == null) return;
                    if (!(property instanceof Relation)) return;

                    if (shouldHandle(entityDescriptor, (Relation) property)) {
                        enhanceParent(userContext, dataSet, (Relation) property, r, intent, request);
                        return;
                    }
                    collectChildren(userContext, dataSet, (Relation) property, r, intent, request);
                });
    }

    private boolean shouldHandle(EntityDescriptor entityDescriptor, Relation relation) {
        if (relation == null) return false;
        EntityDescriptor relationKeeper = relation.getRelationKeeper();
        while (entityDescriptor != null) {
            if (entityDescriptor == relationKeeper) {
                return true;
            }
            entityDescriptor = entityDescriptor.getParent();
        }
        return false;
    }

    private PropertyDescriptor findProperty(EntityDescriptor entityDescriptor, String propertyName) {
        while (entityDescriptor != null) {
            PropertyDescriptor propertyDescriptor = entityDescriptor.findProperty(propertyName);
            if (propertyDescriptor != null) {
                return propertyDescriptor;
            }
            entityDescriptor = entityDescriptor.getParent();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void enhanceParent(
            UserContext userContext,
            SmartList<Entity> results,
            Relation relation,
            SearchRequest parentRequest, SqlIntentRedactions intent, SearchRequest<?> origin) {
        List<Entity> parents =
                results.stream()
                        .map(e -> e.getProperty(relation.getName()))
                        .filter(p -> p instanceof Entity)
                        .map(e -> (Entity) e)
                        .distinct()
                        .toList();
        if (io.teaql.core.utils.ObjectUtil.isEmpty(parents)) return;

        io.teaql.core.internal.TempRequest parentTemp = SqlDiagnosticRequest.forRelation(
                parentRequest, intent, origin, relation.getName());
        parentTemp.appendSearchCriteria(parentTemp.createBasicSearchCriteria(BaseEntity.ID_PROPERTY, io.teaql.core.criteria.Operator.IN, parents));
        // This is a framework-owned lookup over the already materialized child page.
        // A caller may project the parent without specifying a separate page size, but
        // the distinct referenced IDs give this internal query an exact upper bound.
        if (parentTemp.getSlice() == null) parentTemp.setSize(parents.size());

        SmartList<Entity> parentItems = userContext.internalExecuteForList(parentTemp);

        Map<Long, Entity> map = parentItems.mapById();
        for (Entity result : results) {
            Object oldValue = result.getProperty(relation.getName());
            if (oldValue instanceof Entity) {
                Entity value = map.get(((Entity) oldValue).getId());
                if (value == null) continue;
                attachRelation(result, relation, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectChildren(
            UserContext userContext,
            SmartList<Entity> dataSet,
            Relation relation,
            SearchRequest childRequest, SqlIntentRedactions intent, SearchRequest<?> origin) {
        io.teaql.core.internal.TempRequest childTempRequest = SqlDiagnosticRequest.forRelation(
                childRequest, intent, origin, relation.getName());
        PropertyDescriptor reverseProperty = relation.getReverseProperty();
        childTempRequest.selectProperty(reverseProperty.getName());
        Slice slice = childTempRequest.getSlice();
        boolean boundedTopN = slice != null && slice.getSize() > 0;
        if (boundedTopN) ensureStableEntityIdOrder(childTempRequest);
        Integer configuredThreshold = childTempRequest.topNProbeParentThreshold();
        boolean probe = boundedTopN && shouldProbe(dataSet.size(), configuredThreshold);
        SmartList<Entity> children = new SmartList<>();

        if (probe) {
            addTopNTelemetry(childTempRequest, dataSet.size(), slice.getSize(), configuredThreshold,
                    "probe", dataSet.size());
            for (Entity parent : dataSet) {
                io.teaql.core.internal.TempRequest probeRequest =
                        SqlDiagnosticRequest.forRelation(childRequest, intent, origin, relation.getName());
                probeRequest.selectProperty(reverseProperty.getName());
                probeRequest.setPartitionProperty(null);
                ensureStableEntityIdOrder(probeRequest);
                probeRequest.appendSearchCriteria(
                        probeRequest.createBasicSearchCriteria(
                                reverseProperty.getName(), io.teaql.core.criteria.Operator.EQUAL, parent));
                addTopNTelemetry(probeRequest, dataSet.size(), slice.getSize(), configuredThreshold,
                        "probe", dataSet.size());
                userContext.internalExecuteForList(probeRequest).forEach(children::add);
            }
        } else {
            if (boundedTopN) {
                childTempRequest.setPartitionProperty(reverseProperty.getName());
                addTopNTelemetry(childTempRequest, dataSet.size(), slice.getSize(), configuredThreshold,
                        "window", 0);
            }
            childTempRequest.appendSearchCriteria(
                    childTempRequest.createBasicSearchCriteria(
                            reverseProperty.getName(), io.teaql.core.criteria.Operator.IN, dataSet));
            userContext.internalExecuteForList(childTempRequest).forEach(children::add);
        }

        Map<Long, Entity> longTMap = dataSet.mapById();
        for (Entity childEntity : children) {
            Object parent = childEntity.getProperty(reverseProperty.getName());
            if (parent instanceof Entity) {
                Entity parentEntity = longTMap.get(((Entity) parent).getId());
                if (parentEntity != null) {
                    attachRelation(parentEntity, relation, childEntity);
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void attachRelation(Entity target, PropertyDescriptor relation, Entity value) {
        if (relation == null || relation.getType() == null) return;
        Class<?> relationType = relation.getType().javaType();
        if (SmartList.class.isAssignableFrom(relationType)) {
            SmartList existing = target.getProperty(relation.getName());
            if (existing == null) {
                existing = new SmartList<>();
                target.setProperty(relation.getName(), existing);
            }
            existing.add(value);
            return;
        }
        if (Entity.class.isAssignableFrom(relationType)) {
            target.setProperty(relation.getName(), value);
        }
    }

    private void ensureStableEntityIdOrder(BaseRequest<?> request) {
        boolean hasId = request.getOrderBy().properties(null).stream()
                .anyMatch(BaseEntity.ID_PROPERTY::equals);
        if (!hasId) request.getOrderBy().addOrderBy(new OrderBy(BaseEntity.ID_PROPERTY));
    }

    private boolean shouldProbe(int parentCount, Integer configuredThreshold) {
        if (configuredThreshold != null) {
            return configuredThreshold > 0 && parentCount <= configuredThreshold;
        }
        return topNRelationPlanPolicy == TopNRelationPlanPolicy.ALWAYS_PROBE;
    }

    private void addTopNTelemetry(
            BaseRequest<?> request,
            int parentCount,
            int perParentLimit,
            Integer configuredThreshold,
            String selectedPlan,
            int probeCount) {
        request.putExtension(TOP_N_PARENT_COUNT, parentCount);
        request.putExtension(TOP_N_PER_PARENT_LIMIT, perParentLimit);
        request.putExtension(TOP_N_PROBE_THRESHOLD,
                configuredThreshold == null ? "provider-default" : configuredThreshold);
        request.putExtension(TOP_N_SELECTED_PLAN, selectedPlan);
        request.putExtension(TOP_N_PROBE_COUNT, probeCount);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MutationResult mutate(UserContext context, PersistenceMutation request) {
        if (!(request instanceof EntityPersistenceMutation)) {
            throw new TeaQLRuntimeException("Unsupported PersistenceMutation in PortableSQLDataService");
        }
        EntityPersistenceMutation mutation = (EntityPersistenceMutation) request;
        Entity entity = mutation.getEntity();
        String typeName = entity.typeName();
        PortableSQLRepository repository = getRepository(typeName);
        // Local to this mutation, never stored on context or a shared repository.
        var readbackIntent = mutation.diagnosticRedactions();
        repository.captureMutationIntent(entity, readbackIntent);
        if (mutation.diagnosticSource() != entity)
            repository.captureMutationIntent(mutation.diagnosticSource(), readbackIntent);
        var statements = new ArrayList<io.teaql.core.ExecutionMetadata>();

        String operation = mutation.getAction() == EntityPersistenceMutation.Action.DELETE ? "delete"
                : entity.newItem() ? "insert" : entity.recoverItem() ? "recover" : "update";
        var trace = io.teaql.core.SqlExecutionTrace.mutation(entity, mutation.getTraceChain(), operation)
                .collecting(statements::add);

        if (mutation.getAction() == EntityPersistenceMutation.Action.SAVE) {
            if (entity.getId() == null) {
                Long newId = repository.prepareId(context, entity);
                ((BaseEntity) entity).__internalSet("id", newId);
            }
            if (entity.newItem()) {
                ((BaseEntity) entity).__internalSet("version", 1L);
                repository.createInternal(context, Collections.singletonList(entity), readbackIntent, trace);
            } else if (entity.updateItem()) {
                repository.updateInternal(context, Collections.singletonList(entity), readbackIntent, trace);
                ((BaseEntity) entity).__internalSet("version", entity.getVersion() + 1);
            } else if (entity.recoverItem()) {
                repository.recoverInternal(context, Collections.singletonList(entity), readbackIntent, trace);
                ((BaseEntity) entity).__internalSet("version", -entity.getVersion() + 1);
            }
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
            }
        } else if (mutation.getAction() == EntityPersistenceMutation.Action.DELETE) {
            repository.deleteInternal(context, Collections.singletonList(entity), readbackIntent, trace);
            ((BaseEntity) entity).__internalSet("version", -(entity.getVersion() + 1));
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
            }
        }

        Entity persisted = null;
        if (entity.getId() != null
                && (mutation.getAction() == EntityPersistenceMutation.Action.SAVE
                    || mutation.getAction() == EntityPersistenceMutation.Action.DELETE)) {
            persisted = repository.loadPersistedById(context, entity.getId(), readbackIntent, trace.readback(mutation.intent()));
        }
        return new io.teaql.core.DefaultMutationResult(persisted, statements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MutationResult> mutateBatch(UserContext context, MutationBatchRequest request) {
        Objects.requireNonNull(request, "request");
        List<EntityPersistenceMutation> items = new ArrayList<>();
        for (PersistenceMutation item : request.items()) {
            if (!(item instanceof EntityPersistenceMutation mutation)) {
                throw new TeaQLRuntimeException("Unsupported batch member in PortableSQLDataService");
            }
            items.add(mutation);
        }
        if (items.isEmpty()) return List.of();
        String type = items.get(0).getEntity().typeName();
        String operation = batchOperation(items.get(0));
        if (items.stream().anyMatch(item -> !item.getEntity().typeName().equals(type)
                || !batchOperation(item).equals(operation))) {
            throw new TeaQLRuntimeException("Portable SQL prepared batch requires one entity type and mutation operation");
        }
        return executeInTransaction(context, () -> {
            PortableSQLRepository repository = getRepository(type);
            var redactions = new SqlIntentRedactions();
            for (var item : items) {
                redactions.include(item.diagnosticRedactions());
                repository.captureMutationIntent(item.getEntity(), redactions);
                if (item.diagnosticSource() != item.getEntity())
                    repository.captureMutationIntent(item.diagnosticSource(), redactions);
            }
            List<Entity> entities = new ArrayList<>();
            List<SqlExecutionTrace> traces = new ArrayList<>();
            List<List<io.teaql.core.ExecutionMetadata>> statements = new ArrayList<>();
            for (EntityPersistenceMutation item : items) {
                var entity = (BaseEntity) item.getEntity();
                if (operation.equals("insert")) {
                    if (entity.getId() == null) entity.__internalSet("id", repository.prepareId(context, entity));
                    entity.__internalSet("version", 1L);
                } else if (entity.getId() == null || entity.getVersion() == null) {
                    throw new TeaQLRuntimeException("Prepared persisted mutation requires identity and optimistic version");
                }
                entities.add(entity);
                var memberStatements = new ArrayList<io.teaql.core.ExecutionMetadata>();
                statements.add(memberStatements);
                traces.add(SqlExecutionTrace.mutation(entity, item.getTraceChain(), operation)
                        .collecting(memberStatements::add));
            }
            switch (operation) {
                case "insert" -> repository.createBatchInternal(context, entities, redactions, traces);
                case "update" -> repository.updateBatchInternal(context, entities, redactions, traces);
                case "delete" -> repository.deleteBatchInternal(context, entities, redactions, traces);
                case "recover" -> repository.recoverBatchInternal(context, entities, redactions, traces);
                default -> throw new TeaQLRuntimeException("Unsupported prepared mutation operation");
            }
            List<MutationResult> results = new ArrayList<>();
            for (int index = 0; index < items.size(); index++) {
                var item = items.get(index);
                var entity = (BaseEntity) item.getEntity();
                if (!operation.equals("insert")) {
                    long version = entity.getVersion();
                    entity.__internalSet("version", operation.equals("delete") ? -(version + 1)
                            : operation.equals("recover") ? -version + 1 : version + 1);
                }
                entity.gotoNextStatus(EntityAction.PERSIST);
                Entity persisted = repository.loadPersistedById(context, entity.getId(), redactions,
                        traces.get(index).readback(item.intent()));
                if (persisted == null) throw new TeaQLRuntimeException("Batch mutation readback returned no entity");
                results.add(new DefaultMutationResult(persisted, statements.get(index)));
            }
            return List.copyOf(results);
        });
    }

    private String batchOperation(EntityPersistenceMutation item) {
        Entity entity = item.getEntity();
        if (item.getAction() == EntityPersistenceMutation.Action.DELETE && entity.deleteItem()) return "delete";
        if (item.getAction() == EntityPersistenceMutation.Action.SAVE) {
            if (entity.newItem()) return "insert";
            if (entity.updateItem()) return "update";
            if (entity.recoverItem()) return "recover";
        }
        throw new TeaQLRuntimeException("Prepared mutation member has no executable persistence state");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeInTransaction(UserContext context, TransactionCallback<T> action) {
        final Object[] resultHolder = new Object[1];
        final Exception[] exceptionHolder = new Exception[1];
        database.executeInTransaction(() -> {
            try {
                resultHolder[0] = action.doInTransaction();
            } catch (Exception e) {
                exceptionHolder[0] = e;
                throw new TeaQLRuntimeException("Transaction failed", e);
            }
        });
        if (exceptionHolder[0] != null) {
            if (exceptionHolder[0] instanceof RuntimeException) {
                throw (RuntimeException) exceptionHolder[0];
            }
            throw new TeaQLRuntimeException("Transaction failed", exceptionHolder[0]);
        }
        return (T) resultHolder[0];
    }

    public void ensureSchema(UserContext context, String typeName) {
        PortableSQLRepository<?> repository = getRepository(typeName);
        repository.ensureSchema(context);
    }
}
