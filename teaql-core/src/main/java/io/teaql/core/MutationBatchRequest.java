package io.teaql.core;

import java.util.List;
import java.util.Objects;

/**
 * Immutable envelope for commands already planned under one root mutation intent.
 * Local reasons belong to the individual command's lineage, not another root intent.
 */
public final class MutationBatchRequest implements MutationRequest {
    private final MutationIntent intent;
    private final List<PersistenceMutation> items;

    public MutationBatchRequest(String comment, List<? extends PersistenceMutation> items) {
        this(MutationIntent.of(comment), items);
    }

    public MutationBatchRequest(MutationIntent intent, List<? extends PersistenceMutation> items) {
        this.intent = Objects.requireNonNull(intent, "intent");
        this.items = List.copyOf(items);
        for (PersistenceMutation item : this.items) {
            if (!intent.comment().equals(item.intent().comment())) {
                throw new IllegalArgumentException("Batch member must own the batch root intent; retain local reasons in its lineage");
            }
        }
    }

    @Override public MutationIntent intent() { return intent; }
    public List<PersistenceMutation> items() { return items; }
    @Override public String toString() { return "MutationBatchRequest[items=" + items.size() + ", validated]"; }
}
