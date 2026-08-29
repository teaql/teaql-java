package io.teaql.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.teaql.core.utils.StrUtil;
import io.teaql.data.dynamic.DynamicFieldSelection;

public interface SearchRequest<T extends Entity> {
    int DEFAULT_HARD_LIMIT = 10_000;
    default String getTypeName() {
        String simpleName = this.getClass().getSimpleName();
        return StrUtil.removeSuffix(simpleName, "Request");
    }

    default java.util.Map<String, Object> getExtensions() {
        return null;
    }

    default Object getExtension(String key) {
        java.util.Map<String, Object> extensions = getExtensions();
        return extensions == null ? null : extensions.get(key);
    }

    default String getSearchForText() {
        return null;
    }

    /**
     * Returns the dynamic field selection for this request, or null if none.
     * When non-null, the runtime will post-load the specified dynamic fields
     * after the main query completes.
     */
    default DynamicFieldSelection getDynamicFieldSelection() {
        return null;
    }

    Class<? extends T> returnType();

    /** Native-image-safe entity factory supplied by generated request code. */
    default T internalNewEntity() {
        throw new TeaQLRuntimeException(
                "Request does not provide a native-image-safe entity factory for " + getTypeName());
    }


    String comment();

    /**
     * Returns the declared purpose of this query.
     * Purpose describes WHY this query is being executed (business intent).
     * When Triple-Intent enforcement is enabled, queries without a purpose will be rejected.
     */
    default String purpose() {
        return null;
    }

    String getPartitionProperty();

    void setPartitionProperty(String propertyName);

    List<SimpleNamedExpression> getProjections();

    List<SimpleNamedExpression> getSimpleDynamicProperties();

    SearchCriteria getSearchCriteria();

    Aggregations getAggregations();

    Map<String, SearchRequest> getPropagateAggregations();

    Map<String, SearchRequest> getPropagateDimensions();

    OrderBys getOrderBy();

    Slice getSlice();

    /** Local runtime policy; deliberately not a bean getter or federation field. */
    default int hardLimit() { return DEFAULT_HARD_LIMIT; }

    /** Local runtime hint; deliberately excluded from federation input. */
    default ContinuousPageFetchOptions continuousPageFetchOptions() { return null; }

    /** Local runtime hint; deliberately excluded from federation input. */
    default IdSetPaginationOptions idSetPaginationOptions() { return null; }

    /**
     * Local per-parent Top-N planner override. Null uses the provider default,
     * zero forces the window plan, and a positive value permits bounded probes
     * only when the already-loaded parent count is at or below the value.
     */
    default Integer topNProbeParentThreshold() { return null; }

    Map<String, SearchRequest> enhanceRelations();

    Map<String, SearchRequest> enhanceChildren();

    List<SimpleAggregation> getDynamicAggregateAttributes();

    SearchRequest<T> appendSearchCriteria(SearchCriteria searchCriteria);

    List<FacetRequest> getFacetRequests();



    default boolean hasSimpleAgg() {
        Aggregations aggregations = getAggregations();
        if (aggregations == null) {
            return false;
        }
        return !aggregations.getAggregates().isEmpty();
    }

    default List<String> dataProperties(UserContext context) {
        Set<String> allRelationProperties = new HashSet<>();
        List<SimpleNamedExpression> projections = getProjections();
        if (projections != null) {
            for (SimpleNamedExpression projection : projections) {
                allRelationProperties.addAll(projection.properties(context));
            }
        }

        List<SimpleNamedExpression> simpleDynamicProperties = getSimpleDynamicProperties();
        if (simpleDynamicProperties != null) {
            for (SimpleNamedExpression dynamicProperty : simpleDynamicProperties) {
                allRelationProperties.addAll(dynamicProperty.properties(context));
            }
        }

        SearchCriteria searchCriteria = getSearchCriteria();
        if (searchCriteria != null) {
            allRelationProperties.addAll(searchCriteria.properties(context));
        }

        String partitionProperty = getPartitionProperty();
        if (partitionProperty != null && getSlice().getSize() != 0) {
            allRelationProperties.add(partitionProperty);
        }

        OrderBys orderBy = getOrderBy();
        if (orderBy != null) {
            allRelationProperties.addAll(orderBy.properties(context));
        }

        return new ArrayList<>(allRelationProperties);
    }

    default List<String> aggregationProperties(UserContext context) {
        Set<String> allRelationProperties = new HashSet<>();
        List<SimpleNamedExpression> all = getAggregations().getSelectedExpressions();
        for (SimpleNamedExpression simpleNamedExpression : all) {
            allRelationProperties.addAll(simpleNamedExpression.properties(context));
        }
        SearchCriteria searchCriteria = getSearchCriteria();
        if (searchCriteria != null) {
            allRelationProperties.addAll(searchCriteria.properties(context));
        }
        return new ArrayList<>(allRelationProperties);
    }


    default boolean tryUseSubQuery() {
        return true;
    }

    default boolean tryCacheAggregation() {
        return false;
    }

    default long getAggregateCacheTime() {
        return 0l;
    }
}
