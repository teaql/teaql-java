package io.teaql.core;

public interface QueryExecutor extends DataServiceExecutor {
    QueryResult query(UserContext ctx, QueryRequest request);
}
