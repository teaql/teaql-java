package io.teaql.core.view;

import io.teaql.core.reference.InternalEntityIdentity;
import java.util.List;
import java.util.Objects;

/** Committed business result whose outbound presentation may still fail. */
public record AcceptedDocumentView(DocumentView document, long previousRevision,
        long newRevision, List<InternalEntityIdentity> changedEntities) {
    public AcceptedDocumentView {
        Objects.requireNonNull(document);
        changedEntities = List.copyOf(changedEntities);
    }
    @Override public String toString() { return "AcceptedDocumentView[redacted]"; }
}
