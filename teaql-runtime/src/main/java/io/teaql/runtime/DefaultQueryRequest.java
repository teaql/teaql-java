package io.teaql.runtime;

import io.teaql.core.QueryRequest;
import io.teaql.core.SearchRequest;
import io.teaql.core.QueryIntent;
import java.util.Objects;

public final class DefaultQueryRequest implements QueryRequest {
    private final SearchRequest<?> searchRequest;
    private final QueryIntent intent;
    private final io.teaql.core.QueryRequestScope scope;

    public DefaultQueryRequest(SearchRequest<?> searchRequest) {
        this(searchRequest, searchRequest.inheritedQueryIntent() == null
                ? QueryIntent.of(searchRequest.comment(), searchRequest.purpose())
                : searchRequest.inheritedQueryIntent());
    }

    /** Framework-derived requests inherit an already validated root intent, never ambient trace. */
    public DefaultQueryRequest(SearchRequest<?> searchRequest, QueryIntent intent) {
        this.intent = Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(searchRequest, "searchRequest");
        this.scope = searchRequest.requestScope() == null
                ? io.teaql.core.QueryRequestScope.root(searchRequest.getTypeName(), intent) : searchRequest.requestScope();
        this.searchRequest = new io.teaql.core.QueryRequestSnapshot(searchRequest, scope);
    }

    @Override public io.teaql.core.QueryRequestScope requestScope() { return scope; }

    public DefaultQueryRequest withQuery(SearchRequest<?> query) {
        if (query.requestScope() != null && query.requestScope().rootRequest() != scope.rootRequest())
            throw new IllegalArgumentException("QUERY_SCOPE_OWNER_MISMATCH: replacement belongs to another query invocation");
        return new DefaultQueryRequest(new io.teaql.core.QueryRequestSnapshot(query, scope), intent);
    }

    public DefaultQueryRequest deriveOperation(SearchRequest<?> query, io.teaql.core.QueryOperation operation) {
        return new DefaultQueryRequest(new io.teaql.core.QueryRequestSnapshot(query,
                scope.deriveOperation(query.getTypeName(), operation)), intent);
    }

    public DefaultQueryRequest derive(SearchRequest<?> child, io.teaql.core.QueryOperation operation) {
        var parent = scope.deriveOperation(searchRequest.getTypeName(), operation);
        String comment = child.requestScope() == null ? child.comment() : child.requestScope().comment();
        return new DefaultQueryRequest(new io.teaql.core.QueryRequestSnapshot(child,
                parent.deriveChild(child.getTypeName(), comment)), intent);
    }

    @Override public QueryIntent intent() { return intent; }

    public SearchRequest<?> getSearchRequest() {
        return searchRequest;
    }
}
