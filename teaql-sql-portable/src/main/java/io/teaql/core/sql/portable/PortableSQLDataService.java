package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.runtime.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortableSQLDataService implements DataServiceExecutor, QueryExecutor, MutationExecutor, TransactionExecutor {

    private final String name;
    private final DataServiceCapabilities capabilities;
    private final TeaQLDatabase database;
    private final EntityMetaFactory metadata;
    private final Map<String, PortableSQLRepository<?>> repositories = new ConcurrentHashMap<>();
    private final PortableSQLRepository.PortableSQLRepositoryResolver resolver = this::getRepository;
    private io.teaql.core.sql.dialect.SqlDialect dialect;

    public PortableSQLDataService(String name, TeaQLDatabase database, EntityMetaFactory metadata) {
        this.name = name;
        this.database = database;
        this.metadata = metadata;
        this.capabilities = new DataServiceCapabilities();
        this.capabilities.setQuery(true);
        this.capabilities.setMutation(true);
        this.capabilities.setTransaction(true);
    }

    @Override
    public String name() {
        return name;
    }

    public void setDialect(io.teaql.core.sql.dialect.SqlDialect dialect) {
        this.dialect = dialect;
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
            PortableSQLRepository<?> repo = new PortableSQLRepository<>(descriptor, database, resolver);
            if (this.dialect != null) {
                repo.setDialect(this.dialect);
            }
            return (PortableSQLRepository<T>) repo;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public QueryResult query(UserContext ctx, QueryRequest request) {
        if (!(request instanceof DefaultQueryRequest)) {
            throw new TeaQLRuntimeException("Unsupported QueryRequest in PortableSQLDataService");
        }
        SearchRequest<?> searchRequest = ((DefaultQueryRequest) request).getSearchRequest();
        String typeName = searchRequest.getTypeName();
        PortableSQLRepository<?> repository = getRepository(typeName);
        SmartList<?> result = repository.loadInternal(ctx, (SearchRequest) searchRequest);
        
        if (searchRequest.enhanceRelations() != null && !searchRequest.enhanceRelations().isEmpty()) {
            enhanceRelations(ctx, (SmartList<Entity>) result, searchRequest);
        }
        
        return new DefaultQueryResult((SmartList<Entity>) result);
    }
    
    private void enhanceRelations(
            UserContext userContext, SmartList<Entity> dataSet, SearchRequest<?> request) {
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
                        enhanceParent(userContext, dataSet, (Relation) property, r);
                        return;
                    }
                    collectChildren(userContext, dataSet, (Relation) property, r);
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
            SearchRequest parentRequest) {
        List<Entity> parents =
                results.stream()
                        .map(e -> e.getProperty(relation.getName()))
                        .filter(p -> p instanceof Entity)
                        .map(e -> (Entity) e)
                        .distinct()
                        .toList();
        if (io.teaql.core.utils.ObjectUtil.isEmpty(parents)) return;

        io.teaql.core.internal.TempRequest parentTemp = new io.teaql.core.internal.TempRequest(parentRequest);
        parentTemp.appendSearchCriteria(parentTemp.createBasicSearchCriteria(BaseEntity.ID_PROPERTY, io.teaql.core.criteria.Operator.IN, parents));

        QueryResult res = query(userContext, new DefaultQueryRequest(parentTemp));
        SmartList<Entity> parentItems = (SmartList<Entity>) ((DefaultQueryResult)res).getResult();

        Map<Long, Entity> map = parentItems.mapById();
        for (Entity result : results) {
            Object oldValue = result.getProperty(relation.getName());
            if (oldValue instanceof Entity) {
                Entity value = map.get(((Entity) oldValue).getId());
                if (value == null) continue;
                result.addRelation(relation.getName(), value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectChildren(
            UserContext userContext,
            SmartList<Entity> dataSet,
            Relation relation,
            SearchRequest childRequest) {
        io.teaql.core.internal.TempRequest childTempRequest = new io.teaql.core.internal.TempRequest(childRequest);
        PropertyDescriptor reverseProperty = relation.getReverseProperty();
        childTempRequest.selectProperty(reverseProperty.getName());
        if (childTempRequest.getSlice() != null) {
            childTempRequest.setPartitionProperty(reverseProperty.getName());
        }
        childTempRequest.appendSearchCriteria(
                childTempRequest.createBasicSearchCriteria(
                        reverseProperty.getName(), io.teaql.core.criteria.Operator.IN, dataSet));

        QueryResult res = query(userContext, new DefaultQueryRequest(childTempRequest));
        SmartList<Entity> children = (SmartList<Entity>) ((DefaultQueryResult)res).getResult();

        Map<Long, Entity> longTMap = dataSet.mapById();
        for (Entity childEntity : children) {
            Object parent = childEntity.getProperty(reverseProperty.getName());
            if (parent instanceof Entity) {
                Entity parentEntity = longTMap.get(((Entity) parent).getId());
                if (parentEntity != null) {
                    parentEntity.addRelation(relation.getName(), childEntity);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public MutationResult mutate(UserContext ctx, MutationRequest request) {
        if (!(request instanceof DefaultMutationRequest)) {
            throw new TeaQLRuntimeException("Unsupported MutationRequest in PortableSQLDataService");
        }
        DefaultMutationRequest mutation = (DefaultMutationRequest) request;
        Entity entity = mutation.getEntity();
        String typeName = entity.typeName();
        PortableSQLRepository repository = getRepository(typeName);

        if (mutation.getAction() == DefaultMutationRequest.Action.SAVE) {
            if (entity.getId() == null) {
                Long newId = repository.prepareId(ctx, entity);
                ((BaseEntity) entity).internalSet("id", newId);
            }
            if (entity.newItem()) {
                ((BaseEntity) entity).internalSet("version", 1L);
                repository.createInternal(ctx, Collections.singletonList(entity));
            } else if (entity.updateItem()) {
                repository.updateInternal(ctx, Collections.singletonList(entity));
                ((BaseEntity) entity).internalSet("version", entity.getVersion() + 1);
            } else if (entity.recoverItem()) {
                repository.recoverInternal(ctx, Collections.singletonList(entity));
                ((BaseEntity) entity).internalSet("version", -entity.getVersion() + 1);
            }
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
            }
        } else if (mutation.getAction() == DefaultMutationRequest.Action.DELETE) {
            repository.deleteInternal(ctx, Collections.singletonList(entity));
            ((BaseEntity) entity).internalSet("version", -(entity.getVersion() + 1));
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
            }
        }

        return new MutationResult() {};
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeInTransaction(UserContext ctx, TransactionCallback<T> action) {
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

    public void ensureSchema(UserContext ctx, String typeName) {
        PortableSQLRepository<?> repository = getRepository(typeName);
        repository.ensureSchema(ctx);
    }
}
