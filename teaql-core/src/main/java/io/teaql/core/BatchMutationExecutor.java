package io.teaql.core;

import java.util.List;

/** Optional provider capability; ordered results correspond to the request's items. */
public interface BatchMutationExecutor extends MutationExecutor {
    List<MutationResult> mutateBatch(UserContext context, MutationBatchRequest request);
}
