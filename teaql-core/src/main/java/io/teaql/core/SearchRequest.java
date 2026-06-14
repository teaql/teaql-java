package io.teaql.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.teaql.core.utils.StrUtil;

public interface SearchRequest<T extends Entity> {
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

    Class<? extends T> returnType();


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

    default List<String> dataProperties(UserContext ctx) {
        Set<String> allRelationProperties = new HashSet<>();
        List<SimpleNamedExpression> projections = getProjections();
        if (projections != null) {
            for (SimpleNamedExpression projection : projections) {
                allRelationProperties.addAll(projection.properties(ctx));
            }
        }

        List<SimpleNamedExpression> simpleDynamicProperties = getSimpleDynamicProperties();
        if (simpleDynamicProperties != null) {
            for (SimpleNamedExpression dynamicProperty : simpleDynamicProperties) {
                allRelationProperties.addAll(dynamicProperty.properties(ctx));
            }
        }

        SearchCriteria searchCriteria = getSearchCriteria();
        if (searchCriteria != null) {
            allRelationProperties.addAll(searchCriteria.properties(ctx));
        }

        String partitionProperty = getPartitionProperty();
        if (partitionProperty != null && getSlice().getSize() != 0) {
            allRelationProperties.add(partitionProperty);
        }

        OrderBys orderBy = getOrderBy();
        if (orderBy != null) {
            allRelationProperties.addAll(orderBy.properties(ctx));
        }

        return new ArrayList<>(allRelationProperties);
    }

    default List<String> aggregationProperties(UserContext ctx) {
        Set<String> allRelationProperties = new HashSet<>();
        List<SimpleNamedExpression> all = getAggregations().getSelectedExpressions();
        for (SimpleNamedExpression simpleNamedExpression : all) {
            allRelationProperties.addAll(simpleNamedExpression.properties(ctx));
        }
        SearchCriteria searchCriteria = getSearchCriteria();
        if (searchCriteria != null) {
            allRelationProperties.addAll(searchCriteria.properties(ctx));
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
