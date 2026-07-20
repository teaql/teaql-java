package io.teaql.core;

public interface SchemaExecutor extends DataServiceExecutor {
    void ensureSchema(UserContext ctx);
}
