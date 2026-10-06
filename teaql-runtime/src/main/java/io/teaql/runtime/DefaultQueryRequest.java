package io.teaql.runtime;

import io.teaql.core.QueryRequest;
import io.teaql.core.SearchRequest;
import io.teaql.core.QueryIntent;
import java.util.Objects;

public final class DefaultQueryRequest implements QueryRequest {
    private final SearchRequest<?> searchRequest;
    private final QueryIntent intent;

    public DefaultQueryRequest(SearchRequest<?> searchRequest) {
        this(searchRequest, searchRequest.inheritedQueryIntent() == null
                ? QueryIntent.of(searchRequest.comment(), searchRequest.purpose())
                : searchRequest.inheritedQueryIntent());
    }

    /** Framework-derived requests inherit an already validated root intent, never ambient trace. */
    public DefaultQueryRequest(SearchRequest<?> searchRequest, QueryIntent intent) {
        this.intent = Objects.requireNonNull(intent, "intent");
        this.searchRequest = Objects.requireNonNull(searchRequest, "searchRequest");
    }

    @Override public QueryIntent intent() { return intent; }

    public SearchRequest<?> getSearchRequest() {
        return searchRequest;
    }
}
