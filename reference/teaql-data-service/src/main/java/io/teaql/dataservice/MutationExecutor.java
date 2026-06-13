package io.teaql.coreservice;

import io.teaql.core.UserContext;

public interface MutationExecutor extends DataServiceExecutor {
    MutationResult mutate(UserContext ctx, MutationRequest request);
}
