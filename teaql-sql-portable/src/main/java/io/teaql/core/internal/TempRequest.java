package io.teaql.core.internal;

import io.teaql.core.BaseRequest;
import io.teaql.core.OrderBys;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;
import io.teaql.core.QueryIntent;

public class TempRequest extends io.teaql.core.QueryRequestSnapshot {
    String type;
    private QueryIntent rootIntent;
    private SearchRequest original;

    public TempRequest(SearchRequest request) {
        this(request, request.inheritedQueryIntent());
    }

    public TempRequest(SearchRequest request, QueryIntent rootIntent) {
        super(request, request.requestScope());
        this.original = request;
        this.rootIntent = rootIntent;
        type = request.getTypeName();
        this.comment = rootIntent == null ? request.comment() : rootIntent.comment();
        this.purpose = rootIntent == null ? request.purpose() : rootIntent.purpose();
    }

    @Override public QueryIntent inheritedQueryIntent() { return rootIntent; }
    @Override public java.util.List<io.teaql.core.TraceNode> sqlTraceSource() {
        return original == null ? java.util.List.of() : original.sqlTraceSource();
    }
    @Override public io.teaql.core.Entity internalNewEntity() {
        return original == null ? super.internalNewEntity() : original.internalNewEntity();
    }

    public TempRequest(Class returnType, String typeName) {
        super(returnType, typeName);
        type = typeName;
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
