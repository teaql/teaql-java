package io.teaql.core.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.teaql.core.utils.Cache;
import io.teaql.core.utils.CacheUtil;
import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CollUtil;
import io.teaql.core.utils.CompareUtil;
import io.teaql.core.utils.NumberUtil;
import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.AggrFunction;
import io.teaql.core.AggregationItem;
import io.teaql.core.AggregationResult;
import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.EntityAction;
import io.teaql.core.Expression;
import io.teaql.core.FacetRequest;
import io.teaql.core.FunctionApply;
import io.teaql.core.PropertyFunction;
import io.teaql.core.PropertyReference;
import io.teaql.core.Repository;
import io.teaql.core.RepositoryException;
import io.teaql.core.SearchRequest;
import io.teaql.core.SimpleAggregation;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.SmartList;
import io.teaql.core.SubQuerySearchCriteria;
import io.teaql.core.internal.RequestAggregationCacheKey;
import io.teaql.core.internal.TempRequest;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.Operator;
import io.teaql.core.event.EntityCreatedEvent;
import io.teaql.core.event.EntityDeletedEvent;
import io.teaql.core.event.EntityRecoverEvent;
import io.teaql.core.event.EntityUpdatedEvent;
import io.teaql.core.log.Markers;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;

public abstract class AbstractRepository<T extends Entity> implements Repository<T> {

    public static final String VERSION = "version";
    public static final String ID = "id";

    private Cache<RequestAggregationCacheKey, AggregationResult> aggregateCache =
            CacheUtil.newLRUCache(1000, 60000);

    protected abstract void updateInternal(UserContext ctx, Collection<T> items);

    protected abstract void createInternal(UserContext ctx, Collection<T> items);

    protected abstract void deleteInternal(UserContext userContext, Collection<T> deleteItems);

    protected abstract void recoverInternal(UserContext userContext, Collection<T> recoverItems);

    protected abstract SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request);

    protected AggregationResult aggregateInternal(UserContext userContext, SearchRequest<T> request) {
        if (request.tryCacheAggregation()) {
            RequestAggregationCacheKey requestAggregationCacheKey =
                    new RequestAggregationCacheKey(request);
            AggregationResult aggregationResult = aggregateCache.get(requestAggregationCacheKey, false);
            if (aggregationResult == null) {
                long now = System.currentTimeMillis();
                aggregationResult = doAggregateInternal(userContext, request);
                long cost = System.currentTimeMillis() - now;
                long cacheTime = request.getAggregateCacheTime();
                if (cacheTime <= 0) {
                    cacheTime = cost * 10;
                }
                aggregateCache.put(requestAggregationCacheKey, aggregationResult, cacheTime);
            }
            return aggregationResult;
        }
        return doAggregateInternal(userContext, request);
    }

    protected abstract AggregationResult doAggregateInternal(
            UserContext userContext, SearchRequest<T> request);

    @Override
    public Collection<T> save(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) {
            return entities;
        }
        Collection<T> newItems = CollUtil.filterNew(entities, Entity::newItem);
        if (ObjectUtil.isNotEmpty(newItems)) {
            for (T newItem : newItems) {
                userContext.info("AbstractRepository.save: BEFORE createInternal " + newItem.typeName() + " id=" + newItem.getId() + " hash=" + System.identityHashCode(newItem));
                if (newItem.typeName().equals("Task")) {
                    try {
                        Object status = io.teaql.utils.reflect.ReflectUtil.invoke(newItem, "getStatus");
                        userContext.info("AbstractRepository.save: Task.getStatus()=" + status);
                    } catch (Exception e) {}
                }
                if (newItem instanceof io.teaql.core.Entity) {
                    userContext.info("AbstractRepository.save: Property status=" + ((io.teaql.core.Entity)newItem).getProperty("status"));
                }
            }
            for (T newItem : newItems) {
                userContext.info("AbstractRepository.save: Creating newItem " + newItem.typeName() + " id=" + newItem.getId() + " status=" + ((BaseEntity)newItem).get$status());
                setIdAndVersionForInsert(userContext, newItem);
            }
            beforeCreate(userContext, newItems);
            createInternal(userContext, newItems);
            for (T newItem : newItems) {
                if (newItem instanceof BaseEntity item) {
                    item.gotoNextStatus(EntityAction.PERSIST);
                    sendEvent(userContext, new EntityCreatedEvent(item));
                    afterPersist(userContext, item);
                }
            }
        }
        Collection<T> updatedItems = CollUtil.filterNew(entities, Entity::updateItem);
        if (ObjectUtil.isNotEmpty(updatedItems)) {
            beforeUpdate(userContext, updatedItems);
            updateInternal(userContext, updatedItems);
            for (T updateItem : updatedItems) {
                updateItem.setVersion(updateItem.getVersion() + 1);
                if (updateItem instanceof BaseEntity item) {
                    sendEvent(userContext, new EntityUpdatedEvent(item));
                    item.gotoNextStatus(EntityAction.PERSIST);
                    afterPersist(userContext, item);
                }
            }
        }
        Collection<T> deleteItems = CollUtil.filterNew(entities, Entity::deleteItem);
        if (ObjectUtil.isNotEmpty(deleteItems)) {
            beforeDelete(userContext, deleteItems);
            deleteInternal(userContext, deleteItems);
            for (T deleteItem : deleteItems) {
                deleteItem.setVersion(-(deleteItem.getVersion() + 1));
                if (deleteItem instanceof BaseEntity item) {
                    item.gotoNextStatus(EntityAction.PERSIST);
                    sendEvent(userContext, new EntityDeletedEvent(item));
                    afterPersist(userContext, item);
                }
            }
        }

        Collection<T> recoverItems = CollUtil.filterNew(entities, Entity::recoverItem);
        if (ObjectUtil.isNotEmpty(recoverItems)) {
            beforeRecover(userContext, recoverItems);
            recoverInternal(userContext, recoverItems);
            for (T recoverItem : recoverItems) {
                recoverItem.setVersion(-recoverItem.getVersion() + 1);
                if (recoverItem instanceof BaseEntity item) {
                    item.gotoNextStatus(EntityAction.PERSIST);
                    sendEvent(userContext, new EntityRecoverEvent(item));
                    afterPersist(userContext, item);
                }
            }
        }
        return entities;
    }

    private void beforeRecover(UserContext userContext, Collection<T> toBeRecoverItems) {
        for (T toBeRecoverItem : toBeRecoverItems) {
            beforeRecover(userContext, getEntityDescriptor(), toBeRecoverItem);
        }
    }

    private void beforeDelete(UserContext userContext, Collection<T> toBeDeleted) {
        for (T item : toBeDeleted) {
            beforeDelete(userContext, getEntityDescriptor(), item);
        }
    }

    private void beforeUpdate(UserContext userContext, Collection<T> toBeUpdatedItems) {
        for (T item : toBeUpdatedItems) {
            beforeUpdate(userContext, getEntityDescriptor(), item);
        }
    }

    protected void beforeCreate(UserContext userContext, Collection<T> toBeCreatedItems) {
        for (T item : toBeCreatedItems) {
            beforeCreate(userContext, getEntityDescriptor(), item);
        }
    }

    private void sendEvent(UserContext ctx, Object event) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.sendEvent(event);
        }
    }

    private void afterPersist(UserContext ctx, BaseEntity item) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.afterPersist(item);
        }
    }

    private void beforeRecover(UserContext ctx, EntityDescriptor desc, T item) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.beforeRecover(desc, item);
        }
    }

    private void beforeDelete(UserContext ctx, EntityDescriptor desc, T item) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.beforeDelete(desc, item);
        }
    }

    private void beforeUpdate(UserContext ctx, EntityDescriptor desc, T item) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.beforeUpdate(desc, item);
        }
    }

    private void beforeCreate(UserContext ctx, EntityDescriptor desc, T item) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.beforeCreate(desc, item);
        }
    }

    private void afterLoad(UserContext ctx, EntityDescriptor desc, T item) {
        if (ctx instanceof DefaultUserContext dctx) {
            dctx.afterLoad(desc, item);
        }
    }

    private void setIdAndVersionForInsert(UserContext userContext, Entity entity) {
        Long id = prepareId(userContext, (T) entity);
        entity.setId(id);
        entity.setVersion(1L);
    }

    protected boolean ensureTableEnabled(UserContext ctx) {
        if (ctx instanceof DefaultUserContext dctx) {
            return dctx.config() != null && dctx.config().isEnsureTable();
        }
        return false;
    }

    /**
     * check if current relation is handled by this repository
     *
     * @param relation relation
     * @return true if current relation is handled(save/query) by this repository
     */
    public boolean shouldHandle(Relation relation) {
        if (relation == null) {
            throw new IllegalArgumentException("relation is null");
        }
        EntityDescriptor relationKeeper = relation.getRelationKeeper();
        EntityDescriptor entityDescriptor = getEntityDescriptor();
        while (entityDescriptor != null) {
            if (entityDescriptor == relationKeeper) {
                return true;
            }
            entityDescriptor = entityDescriptor.getParent();
        }
        return false;
    }

    @Override
    public SmartList<T> executeForList(UserContext userContext, SearchRequest<T> request) {
        String comment = request.comment();
        if (ObjectUtil.isNotEmpty(comment)) {
            org.slf4j.MDC.put("comment", comment);
            userContext.info(Markers.SEARCH_REQUEST_START, "start execute request: {}", comment);
        }
        try {
            SmartList<T> smartList = loadInternal(userContext, request);
            enhanceChildren(userContext, smartList, request);
            enhanceRelations(userContext, smartList, request);
            enhanceWithAggregation(userContext, smartList, request);
            addDynamicAggregations(userContext, smartList, request);
            addFacets(userContext, smartList, request);
            for (T t : smartList) {
                afterLoad(userContext, getEntityDescriptor(), t);
            }
            if (ObjectUtil.isNotEmpty(comment)) {
                userContext.info(Markers.SEARCH_REQUEST_END, "end execute request: {}", comment);
            }
            return smartList;
        } finally {
            if (ObjectUtil.isNotEmpty(comment)) {
                org.slf4j.MDC.remove("comment");
            }
        }
    }

    private void addFacets(UserContext userContext, SmartList<T> smartList, SearchRequest<T> request) {
        List<FacetRequest> facetRequests = request.getFacetRequests();
        if (ObjectUtil.isEmpty(facetRequests)) {
            return;
        }
        for (FacetRequest facetRequest : facetRequests) {
            String facetName = facetRequest.getFacetName();
            SearchRequest facetSearchRequest = facetRequest.getRequest();
            SmartList facet = facetSearchRequest.executeForList(userContext);
            smartList.addFacet(facetName, facet);
        }
    }

    public Stream<T> executeForStream(
            UserContext userContext, SearchRequest<T> request, int enhanceBatch) {
        String comment = request.comment();
        if (ObjectUtil.isNotEmpty(comment)) {
            org.slf4j.MDC.put("comment", comment);
        }
        try {
            SmartList<T> smartList = loadInternal(userContext, request);
            enhanceChildren(userContext, smartList, request);
            enhanceRelations(userContext, smartList, request);
            return smartList.stream()
                    .map(
                            item -> {
                                afterLoad(userContext, getEntityDescriptor(), item);
                                return item;
                            });
        } finally {
            if (ObjectUtil.isNotEmpty(comment)) {
                org.slf4j.MDC.remove("comment");
            }
        }
    }

    public void enhanceChildren(
            UserContext userContext, SmartList<T> dataSet, SearchRequest<T> request) {
        if (dataSet == null || dataSet.isEmpty()) {
            return;
        }
        Map<String, SearchRequest> childrenRequest = request.enhanceChildren();
        if (ObjectUtil.isEmpty(childrenRequest)) {
            return;
        }
        Map<Long, Integer> itemLocation = new HashMap<>();
        int i = 0;
        for (T t : dataSet) {
            itemLocation.put(t.getId(), i++);
        }
        childrenRequest.forEach(
                (type, childRequest) -> {
                    TempRequest tempRequest = new TempRequest(childRequest);
                    tempRequest.appendSearchCriteria(
                            tempRequest.createBasicSearchCriteria(BaseEntity.ID_PROPERTY, Operator.IN, dataSet));
                    SmartList childrenItems = tempRequest.executeForList(userContext);
                    for (Object item : childrenItems) {
                        T subItem = (T) item;
                        Long id = subItem.getId();
                        Integer location = itemLocation.get(id);
                        // this is for custom extensions in child request
                        if (location == null) {
                            continue;
                        }
                        T oldItem = dataSet.get(location);
                        copyProperties(subItem, oldItem);
                        dataSet.set(location, subItem);
                    }
                });
    }

    protected void copyProperties(T subItem, T parentItem) {
        EntityDescriptor entityDescriptor = getEntityDescriptor();
        while (entityDescriptor != null) {
            List<PropertyDescriptor> properties = entityDescriptor.getProperties();
            for (PropertyDescriptor property : properties) {
                String name = property.getName();
                subItem.setProperty(name, parentItem.getProperty(name));
            }
            entityDescriptor = entityDescriptor.getParent();
        }
    }

    protected void enhanceWithAggregation(
            UserContext userContext, SmartList<T> dataSet, SearchRequest<T> request) {
        List<SearchRequest> aggregationRequests = findAggregations(userContext, request);
        for (SearchRequest aggregationRequest : aggregationRequests) {
            AggregationResult aggregation = aggregationRequest.aggregation(userContext);
            dataSet.addAggregationResult(userContext, aggregation);
        }
    }

    public void enhanceRelations(
            UserContext userContext, SmartList<T> dataSet, SearchRequest<T> request) {
        if (dataSet == null || dataSet.isEmpty()) {
            return;
        }
        Map<String, SearchRequest> enhanceProperties = request.enhanceRelations();
        enhanceProperties.forEach(
                (p, r) -> {
                    PropertyDescriptor property = findProperty(p);
                    if (property == null) {
                        return;
                    }

                    if (!(property instanceof Relation)) {
                        return;
                    }

                    if (shouldHandle((Relation) property)) {
                        enhanceParent(userContext, dataSet, (Relation) property, r);
                    }
                    else {
                        collectChildren(userContext, dataSet, (Relation) property, r);
                    }
                });
    }

    private void enhanceParent(
            UserContext userContext,
            SmartList<T> results,
            Relation relation,
            SearchRequest parentRequest) {
        if (ObjectUtil.isEmpty(results)) {
            return;
        }
        List<Entity> parents =
                results.stream()
                        .map(e -> e.getProperty(relation.getName()))
                        .filter(p -> p instanceof Entity)
                        .map(e -> (Entity) e)
                        .distinct()
                        .toList();
        if (ObjectUtil.isEmpty(parents)) {
            return;
        }

        // parent request add id criteria
        TempRequest parentTemp = new TempRequest(parentRequest);
        parentTemp.appendSearchCriteria(parentTemp.createBasicSearchCriteria(ID, Operator.IN, parents));
        Repository repository = userContext.resolveRepository(parentTemp.getTypeName());
        SmartList parentItems = repository.executeForList(userContext, parentTemp);

        Map map = parentItems.mapById();
        for (T result : results) {
            Object oldValue = result.getProperty(relation.getName());
            if (oldValue instanceof Entity) {
                Entity value = (Entity) map.get(((Entity) oldValue).getId());
                // this is for custom extensions in enhance parent
                if (value == null) {
                    continue;
                }
                result.addRelation(relation.getName(), value);
            }
        }
    }

    private void collectChildren(
            UserContext userContext,
            SmartList<T> dataSet,
            Relation relation,
            SearchRequest childRequest) {
        if (dataSet == null || dataSet.isEmpty()) {
            return;
        }
        TempRequest childTempRequest = new TempRequest(childRequest);
        String typeName = childTempRequest.getTypeName();
        Repository repository = userContext.resolveRepository(typeName);
        PropertyDescriptor reverseProperty = relation.getReverseProperty();
        // always select the parent property,  keep the children maintained in the parent
        childTempRequest.selectProperty(reverseProperty.getName());
        if (childTempRequest.getSlice() != null) {
            childTempRequest.setPartitionProperty(reverseProperty.getName());
        }
        childTempRequest.appendSearchCriteria(
                childTempRequest.createBasicSearchCriteria(
                        reverseProperty.getName(), Operator.IN, dataSet));
        SmartList children = repository.executeForList(userContext, childTempRequest);

        Map<Long, T> longTMap = dataSet.mapById();
        for (Object child : children) {
            Entity childEntity = (Entity) child;
            Object parent = childEntity.getProperty(reverseProperty.getName());
            if (parent instanceof Entity) {
                T parentEntity = longTMap.get(((Entity) parent).getId());
                if (parentEntity != null) {
                    parentEntity.addRelation(relation.getName(), childEntity);
                }
            }
        }
    }

    public PropertyDescriptor findProperty(String propertyName) {
        EntityDescriptor entityDescriptor = getEntityDescriptor();
        while (entityDescriptor != null) {
            PropertyDescriptor propertyDescriptor = entityDescriptor.findProperty(propertyName);
            if (propertyDescriptor != null) {
                return propertyDescriptor;
            }
            entityDescriptor = entityDescriptor.getParent();
        }
        throw new RepositoryException("Property: " + propertyName + " not defined");
    }

    public void addDynamicAggregations(
            UserContext userContext, SmartList<T> dataSet, SearchRequest<T> request) {
        if (dataSet == null || dataSet.isEmpty()) {
            return;
        }

        List<SimpleAggregation> dynamicAggregateAttributes = request.getDynamicAggregateAttributes();
        if (ObjectUtil.isEmpty(dynamicAggregateAttributes)) {
            return;
        }

        Map<Long, T> idEntityMap = dataSet.mapById();
        Set<Long> ids = idEntityMap.keySet();
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }

        for (SimpleAggregation dynamicAggregateAttribute : dynamicAggregateAttributes) {
            SearchRequest aggregateRequest = dynamicAggregateAttribute.getAggregateRequest();
            String property = aggregateRequest.getPartitionProperty();
            TempRequest t = new TempRequest(aggregateRequest);
            t.groupBy(property);
            if (ids.size() < preferIdInCount()) {
                t.appendSearchCriteria(t.createBasicSearchCriteria(property, Operator.IN, ids));
            }
            else {
                t.appendSearchCriteria(new SubQuerySearchCriteria(property, request, ID));
            }
            List<SearchRequest> aggregations = findAggregations(userContext, t);
            SearchRequest aggregatePoint = aggregations.get(0);
            AggregationResult aggregation = aggregatePoint.aggregation(userContext);
            if (dynamicAggregateAttribute.isSingleNumber()) {
                saveSingleDynamicValue(idEntityMap, dynamicAggregateAttribute, aggregation);
            }
            else {
                List<Map<String, Object>> dynamicAttributes = aggregation.toList();
                for (Map<String, Object> dynamicAttribute : dynamicAttributes) {
                    saveMultiDynamicValue(idEntityMap, dynamicAggregateAttribute, property, dynamicAttribute);
                }
            }
        }
    }

    protected int preferIdInCount() {
        return 1000;
    }

    public List<SearchRequest> findAggregations(UserContext userContext, SearchRequest request) {
        List<SearchRequest> ret = new ArrayList<>();
        if (request.hasSimpleAgg()) {
            ret.add(request);
        }
        Map<String, SearchRequest> propagateAggregations = request.getPropagateAggregations();
        propagateAggregations.forEach(
                (property, subRequest) -> {
                    TempRequest t = new TempRequest(subRequest);
                    PropertyDescriptor propertyDescriptor = findProperty(property);
                    if (shouldHandle((Relation) propertyDescriptor)) {
                        t.appendSearchCriteria(new SubQuerySearchCriteria(ID, request, property));
                    }
                    else {
                        PropertyDescriptor reverseProperty =
                                ((Relation) propertyDescriptor).getReverseProperty();
                        t.appendSearchCriteria(
                                new SubQuerySearchCriteria(reverseProperty.getName(), request, ID));
                    }
                    List<SearchRequest> aggregations = findAggregations(userContext, t);
                    if (aggregations != null) {
                        ret.addAll(aggregations);
                    }
                });
        return ret;
    }

    private void saveMultiDynamicValue(
            Map<Long, T> idEntityMap,
            SimpleAggregation dynamicAggregateAttribute,
            String property,
            Map<String, Object> dynamicAttribute) {
        Long parentID = ((Number) dynamicAttribute.remove(property)).longValue();
        T parent = idEntityMap.get(parentID);
        parent.appendDynamicProperty(dynamicAggregateAttribute.getName(), dynamicAttribute);
    }

    private void saveSingleDynamicValue(
            Map<Long, T> idEntityMap,
            SimpleAggregation dynamicAggregateAttribute,
            AggregationResult aggregation) {
        Map<Object, Number> simpleMap = aggregation.toSimpleMap();
        simpleMap.forEach(
                (parentId, value) -> {
                    if (parentId instanceof Number numberParentId) {
                        T parent = idEntityMap.get(numberParentId.longValue());
                        parent.addDynamicProperty(dynamicAggregateAttribute.getName(), value);
                        return;
                    }
                    T parent = idEntityMap.get(parentId);
                    if (parent == null) {
                        throw new IllegalArgumentException(
                                "Not able to find parent object from idEntityMap by key: "
                                        + parentId
                                        + ", with class"
                                        + parentId.getClass().getSimpleName());
                    }
                    parent.addDynamicProperty(dynamicAggregateAttribute.getName(), value);
                });
    }

    public void advanceGroupBy(
            UserContext userContext, AggregationResult result, SearchRequest<T> request) {
        Map<String, SearchRequest> propagateDimensions = request.getPropagateDimensions();
        List<SimpleNamedExpression> allDimensions = request.getAggregations().getDimensions();
        propagateDimensions.forEach(
                (property, dimensionRequest) -> {
                    SimpleNamedExpression toBeEnhancedDimension =
                            findCurrentDimension(allDimensions, property);

                    List propagateDimensionValues = result.getPropagateDimensionValues(property);
                    TempRequest t =
                            new TempRequest(dimensionRequest.returnType(), dimensionRequest.getTypeName());

                    t.appendSearchCriteria(dimensionRequest.getSearchCriteria());
                    t.addSimpleDynamicProperty(property, new PropertyReference(ID));

                    Map<String, SearchRequest> subPropagateDimensions =
                            dimensionRequest.getPropagateDimensions();
                    subPropagateDimensions.forEach(
                            (k, v) -> {
                                t.groupBy(k, v);
                            });

                    List<SimpleNamedExpression> dimensions =
                            dimensionRequest.getAggregations().getDimensions();
                    for (SimpleNamedExpression dimension : dimensions) {
                        t.addSimpleDynamicProperty(dimension.name(), dimension.getExpression());
                    }
                    t.appendSearchCriteria(
                            t.createBasicSearchCriteria(ID, Operator.IN, propagateDimensionValues));
                    SmartList orderByResults = t.executeForList(userContext);
                    appendResult(userContext, result, t, toBeEnhancedDimension, orderByResults);
                });
    }

    private SimpleNamedExpression findCurrentDimension(
            List<SimpleNamedExpression> allDimensions, String property) {
        for (SimpleNamedExpression dimension : allDimensions) {
            if (dimension.name().equals(property)) {
                return dimension;
            }
        }
        return null;
    }

    private void appendResult(
            UserContext userContext,
            AggregationResult result,
            SearchRequest dimensionRequest,
            SimpleNamedExpression toBeRefinedDimension,
            SmartList<Entity> dimensionResult) {
        List<SimpleNamedExpression> simpleDynamicProperties =
                dimensionRequest.getSimpleDynamicProperties();

        Map<Object, Map<SimpleNamedExpression, Object>> refinedDimensions =
                CollStreamUtil.toMap(
                        dimensionResult.getData(),
                        e -> e.getDynamicProperty(toBeRefinedDimension.name()),
                        e -> {
                            Map<SimpleNamedExpression, Object> refinedDimension = new HashMap<>();
                            for (SimpleNamedExpression simpleDynamicProperty : simpleDynamicProperties) {
                                if (simpleDynamicProperty.name().equals(toBeRefinedDimension.name())) {
                                    continue;
                                }
                                refinedDimension.put(
                                        simpleDynamicProperty, e.getDynamicProperty(simpleDynamicProperty.name()));
                            }
                            return refinedDimension;
                        });

        List<AggregationItem> data = result.getData();

        for (AggregationItem datum : data) {
            Map<SimpleNamedExpression, Object> dimensions = datum.getDimensions();
            Object currentValue = remove(dimensions, toBeRefinedDimension);
            if (currentValue == null) {
                continue;
            }
            Map<SimpleNamedExpression, Object> replacements = refinedDimensions.get(currentValue);
            if (replacements != null) {
                dimensions.putAll(replacements);
            }
        }

        // merge
        Map<Map<SimpleNamedExpression, Object>, AggregationItem> collect =
                data.stream()
                        .collect(
                                Collectors.toMap(
                                        item -> item.getDimensions(),
                                        item -> item,
                                        (pre, current) -> {
                                            Map<SimpleNamedExpression, Object> preValues = pre.getValues();
                                            Map<SimpleNamedExpression, Object> currentValues = current.getValues();
                                            Set<SimpleNamedExpression> simpleNamedExpressions = preValues.keySet();
                                            for (SimpleNamedExpression simpleNamedExpression : simpleNamedExpressions) {
                                                preValues.put(
                                                        simpleNamedExpression,
                                                        merge(
                                                                simpleNamedExpression,
                                                                preValues.get(simpleNamedExpression),
                                                                currentValues.get(simpleNamedExpression)));
                                            }
                                            return pre;
                                        }));

        advanceGroupBy(userContext, result, dimensionRequest);
    }

    private Object remove(
            Map<SimpleNamedExpression, Object> dimensions, SimpleNamedExpression toBeRefinedDimension) {
        Set<Map.Entry<SimpleNamedExpression, Object>> entries = dimensions.entrySet();
        Iterator<Map.Entry<SimpleNamedExpression, Object>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<SimpleNamedExpression, Object> next = iterator.next();
            SimpleNamedExpression key = next.getKey();
            Object value = next.getValue();
            if (key.name().equals(toBeRefinedDimension.name())) {
                iterator.remove();
                return value;
            }
        }
        return null;
    }

    private Object merge(SimpleNamedExpression aggregation, Object p0, Object p1) {
        Expression expression = aggregation.getExpression();
        if (!(expression instanceof FunctionApply)) {
            throw new RepositoryException("FunctionApply expression only for aggregation");
        }

        PropertyFunction operator = ((FunctionApply) expression).getOperator();
        if (!(operator instanceof AggrFunction)) {
            throw new RepositoryException("Operator expression only for aggregation");
        }
        AggrFunction aggr = (AggrFunction) operator;
        if (aggr == AggrFunction.COUNT || aggr == AggrFunction.SUM) {
            return NumberUtil.add((Number) p0, (Number) p1);
        }

        if (aggr == AggrFunction.MIN) {
            return CompareUtil.compare((Comparable) p0, (Comparable) p1) < 0 ? p0 : p1;
        }

        if (aggr == AggrFunction.MAX) {
            return CompareUtil.compare((Comparable) p0, (Comparable) p1) < 0 ? p1 : p0;
        }

        throw new RepositoryException("un mergeable AggrFunction" + aggr);
    }

    @Override
    public AggregationResult aggregation(UserContext userContext, SearchRequest<T> request) {
        AggregationResult result = aggregateInternal(userContext, request);
        if (result == null) {
            return null;
        }
        advanceGroupBy(userContext, result, request);
        return result;
    }
}
