package io.teaql.coreservice;

import io.teaql.core.UserContext;

public interface SchemaExecutor extends DataServiceExecutor {
    void ensureSchema(UserContext ctx);
}
