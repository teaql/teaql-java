package io.teaql.core;

public interface QueryExecutor extends DataServiceExecutor {
    QueryResult query(UserContext context, QueryRequest request);
}
