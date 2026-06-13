package io.teaql.coreservice;

import io.teaql.core.UserContext;

public interface TransactionExecutor extends DataServiceExecutor {
    <T> T executeInTransaction(UserContext ctx, TransactionCallback<T> action);
}
