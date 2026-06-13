package io.teaql.coreservice;

import io.teaql.core.UserContext;

public interface QueryExecutor extends DataServiceExecutor {
    QueryResult query(UserContext ctx, QueryRequest request);
}
