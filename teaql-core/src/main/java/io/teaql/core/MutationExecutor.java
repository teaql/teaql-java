package io.teaql.core;

public interface MutationExecutor extends DataServiceExecutor {
    MutationResult mutate(UserContext context, MutationRequest request);
}
