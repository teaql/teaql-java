package io.teaql.runtime;

import io.teaql.core.TeaQLRuntimeException;

/** The provider write completed, but the in-memory graph requires a fresh load. */
public final class CommittedStateReconciliationException extends TeaQLRuntimeException {
    public CommittedStateReconciliationException() {
        super("COMMITTED_STATE_RELOAD_REQUIRED: provider write completed but entity reconciliation failed; "
                + "reload the graph and do not retry this instance");
    }
}
