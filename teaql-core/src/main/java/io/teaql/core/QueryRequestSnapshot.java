package io.teaql.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Framework query-body copy with an immutable header; never mutates a reusable builder. */
@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryRequestSnapshot<T extends Entity> extends BaseRequest<T> {
    private final transient QueryRequestScope scope;
    private final transient SearchRequest<T> original;
    private final String typeName;
    private final transient SearchRequest<?> diagnosticSource;
    protected QueryRequestSnapshot(Class<T> returnType, String typeName) {
        super(returnType);
        this.typeName = typeName; original = null; scope = null; diagnosticSource = null;
    }
    public QueryRequestSnapshot(SearchRequest<T> request, QueryRequestScope scope) {
        this(request, scope, request.diagnosticSource());
    }
    public QueryRequestSnapshot(SearchRequest<T> request, QueryRequestScope scope, SearchRequest<?> diagnosticSource) {
        super((Class<T>) request.returnType(), request::internalNewEntity);
        original = request;
        typeName = request.getTypeName();
        this.scope = scope;
        this.diagnosticSource = diagnosticSource;
        if (request instanceof BaseRequest<?> base) base.propagateMetadata(this);
        comment = scope == null ? request.comment() : scope.intent().comment();
        purpose = scope == null ? request.purpose() : scope.purpose();
        projections = new ArrayList<>(request.getProjections());
        simpleDynamicProperties = new ArrayList<>(request.getSimpleDynamicProperties());
        dynamicPropertyMetadata = request.getDynamicPropertyMetadata();
        searchCriteria = request.getSearchCriteria();
        orderBys = new OrderBys();
        if (request.getOrderBy() != null) for (OrderBy source : request.getOrderBy().getOrderBys()) {
            OrderBy order = new OrderBy("id"); order.setExpression(source.getExpression()); order.setDirection(source.getDirection());
            orderBys.addOrderBy(order);
        }
        if (request.getSlice() == null) slice = null;
        else { slice = new Slice(); slice.setOffset(request.getSlice().getOffset()); slice.setSize(request.getSlice().getSize()); }
        partitionProperty = request.getPartitionProperty();
        aggregations = copyAggregations(request.getAggregations());
        enhanceRelations = copyRequests(request.enhanceRelations());
        enhanceChildren = copyRequests(request.enhanceChildren());
        propagateAggregations = copyRequests(request.getPropagateAggregations());
        propagateDimensions = copyRequests(request.getPropagateDimensions());
        dynamicAggregateAttributes = new ArrayList<>();
        for (SimpleAggregation source : request.getDynamicAggregateAttributes()) {
            var aggregate = new SimpleAggregation(source.getName(), new QueryRequestSnapshot(source.getAggregateRequest(), source.getAggregateRequest().requestScope()), source.isSingleNumber());
            aggregate.setOperation(source.getOperation());
            dynamicAggregateAttributes.add(aggregate);
        }
        facetRequests = new ArrayList<>();
        for (FacetRequest source : request.getFacetRequests()) {
            var facet = new FacetRequest();
            facet.setFacetName(source.getFacetName()); facet.setRelationName(source.getRelationName());
            facet.setMergeCriteria(source.isMergeCriteria()); facet.setOperation(source.getOperation());
            if (source.getRequest() != null) facet.setRequest(new QueryRequestSnapshot(source.getRequest(), source.getRequest().requestScope()));
            facetRequests.add(facet);
        }
        queryOperations = new HashMap<>(request.queryOperations());
        pageCountOperation = request.pageCountOperation();
        cacheAggregation = request.tryCacheAggregation(); aggregateCacheTime = request.getAggregateCacheTime();
        hardLimit = request.hardLimit(); continuousPageFetchOptions = request.continuousPageFetchOptions();
        idSetPaginationOptions = request.idSetPaginationOptions(); topNProbeParentThreshold = request.topNProbeParentThreshold();
        if (request.getExtensions() != null) extensions.putAll(request.getExtensions());
        searchForText = request.getSearchForText(); dynamicFieldSelection = request.getDynamicFieldSelection();
    }
    private static java.util.Map<String, SearchRequest> copyRequests(java.util.Map<String, SearchRequest> requests) {
        var result = new HashMap<String, SearchRequest>();
        if (requests != null) requests.forEach((name, query) -> result.put(name, new QueryRequestSnapshot(query, query.requestScope())));
        return result;
    }
    private static Aggregations copyAggregations(Aggregations source) {
        Aggregations result = new Aggregations();
        if (source != null) {
            result.setName(source.getName()); result.setAggregates(new ArrayList<>(source.getAggregates()));
            result.setSimpleDimensions(new ArrayList<>(source.getSimpleDimensions()));
            result.setComplexDimensions(new ArrayList<>(source.getComplexDimensions()));
        }
        return result;
    }
    @Override public String getTypeName() { return typeName; }
    @Override public QueryIntent inheritedQueryIntent() { return scope != null ? scope.intent() : original == null ? null : original.inheritedQueryIntent(); }
    @Override public QueryRequestScope requestScope() { return scope; }
    @Override public SearchRequest<?> diagnosticSource() { return diagnosticSource; }
    @Override public java.util.function.Consumer<ExecutionMetadata> statementObserver() { return original == null ? null : original.statementObserver(); }
    @Override public SqlIntentRedactions sqlIntentSource() { return original == null ? null : original.sqlIntentSource(); }
    @Override public List<TraceNode> sqlTraceSource() { return original == null ? List.of() : original.sqlTraceSource(); }
    @Override public boolean tryUseSubQuery() { return original == null || original.tryUseSubQuery(); }
}
