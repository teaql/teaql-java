package io.teaql.core.internal;

import io.teaql.core.BaseRequest;
import io.teaql.core.OrderBys;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;

public class TempRequest extends BaseRequest {
    String type;

    public TempRequest(SearchRequest request) {
        super(request.returnType());
        type = request.getTypeName();
        copy(request);
    }

    public TempRequest(Class returnType, String typeName) {
        super(returnType);
        type = typeName;
    }

    private void copy(SearchRequest pRequest) {
        projections.addAll(pRequest.getProjections());
        simpleDynamicProperties.addAll(pRequest.getSimpleDynamicProperties());
        searchCriteria = pRequest.getSearchCriteria();
        orderBys = pRequest.getOrderBy();
        slice = pRequest.getSlice();
        enhanceRelations = pRequest.enhanceRelations();
        partitionProperty = pRequest.getPartitionProperty();
        aggregations = pRequest.getAggregations();
        propagateAggregations = pRequest.getPropagateAggregations();
        propagateDimensions = pRequest.getPropagateDimensions();
        dynamicAggregateAttributes = pRequest.getDynamicAggregateAttributes();
        enhanceChildren = pRequest.enhanceChildren();
        cacheAggregation = pRequest.tryCacheAggregation();
        aggregateCacheTime = pRequest.getAggregateCacheTime();
        continuousPageFetchOptions = pRequest.continuousPageFetchOptions();
        idSetPaginationOptions = pRequest.idSetPaginationOptions();
        topNProbeParentThreshold = pRequest.topNProbeParentThreshold();
        if (pRequest.getExtensions() != null) {
            this.extensions.putAll(pRequest.getExtensions());
        }
        facetRequests = pRequest.getFacetRequests();
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public BaseRequest appendSearchCriteria(SearchCriteria searchCriteria) {
        if (searchCriteria == null) {
            return this;
        }
        if (this.searchCriteria == null) {
            this.searchCriteria = searchCriteria;
        }
        else {
            this.searchCriteria = SearchCriteria.and(this.searchCriteria, searchCriteria);
        }
        return this;
    }

    public void setOrderBy(OrderBys orderBy) {
        orderBys = orderBy;
    }
}
