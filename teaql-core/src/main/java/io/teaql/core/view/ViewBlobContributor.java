package io.teaql.core.view;

import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobRuntime;
import java.util.*;

/** Only the already authorized graph is available. No entity traversal, business writes or document issuance. */
public interface ViewBlobContributor {
    @FunctionalInterface interface CurrentValidation {
        /** A read-only current-authority check. Never retains the original actor or signs another URL. */
        void requireCurrent(UserContext context,ViewOptions options,Optional<DocumentContext> document);
    }
    record Projection(List<BlobRuntime.Reads.Read> reads,CurrentValidation validation) {
        public Projection { reads=List.copyOf(reads); Objects.requireNonNull(validation); }
        @Override public String toString() { return "ViewBlobProjection[redacted]"; }
    }
    Projection projectBatch(UserContext context,AuthorizedResponseGraph graph,ViewOptions options,Optional<DocumentContext> document);
}
