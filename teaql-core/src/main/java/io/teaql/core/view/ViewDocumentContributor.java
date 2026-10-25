package io.teaql.core.view;

import io.teaql.core.UserContext;
import java.util.*;

/** Prepares references from a frozen graph; may not load or expand the authorized surface. */
public interface ViewDocumentContributor {
    String CAPABILITY="view.governed.v1";
    record Preparation(AuthorizedResponseGraph graph,DocumentContext context,Map<String,String> credentials) {
        public Preparation { Objects.requireNonNull(graph); Objects.requireNonNull(context); credentials=Map.copyOf(credentials); }
        @Override public String toString() { return "ViewDocumentPreparation[redacted]"; }
    }
    Preparation prepare(UserContext context,AuthorizedResponseGraph graph,ViewOptions options);
    /** Rechecks current document/purpose authority without issuing a new token. Required for governed output. */
    default void validateCurrent(UserContext context,AuthorizedResponseGraph graph,DocumentContext document,ViewOptions options) {
        throw new ViewPreparationException(ViewPreparationException.Code.VIEW_CAPABILITY_MISSING,options.businessCompletion());
    }
}
