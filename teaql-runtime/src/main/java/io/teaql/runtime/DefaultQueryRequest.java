package io.teaql.runtime;

import io.teaql.core.QueryRequest;
import io.teaql.core.SearchRequest;

public class DefaultQueryRequest implements QueryRequest {
    private final SearchRequest<?> searchRequest;

    public DefaultQueryRequest(SearchRequest<?> searchRequest) {
        this.searchRequest = searchRequest;
    }

    public SearchRequest<?> getSearchRequest() {
        return searchRequest;
    }
}
