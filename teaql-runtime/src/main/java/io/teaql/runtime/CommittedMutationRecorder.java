package io.teaql.runtime;

import io.teaql.core.MutationIntent;
import io.teaql.core.UserContext;
import java.util.List;

/**
 * Optional, trusted source-side Outbox capability. Called exactly once for a
 * changed graph while the graph's provider transaction is still open. A
 * failure must abort that transaction; delivery is never a post-commit hook.
 */
@FunctionalInterface
public interface CommittedMutationRecorder {
    void record(UserContext context, Object transactionResource,
            MutationIntent intent, List<CommittedMutationFact> facts);
}
