package io.teaql.blob.reference;

import io.teaql.core.EntityKey;
import io.teaql.core.MutationOperation;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.MutationPlan;
import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobException;
import io.teaql.core.blob.BlobFieldSpec;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Pure, immutable extraction from an approved mutation plan and trusted current metadata. */
public record BlobBindingIntents(String executionId, List<Mutation> mutations) {
    public BlobBindingIntents {
        Objects.requireNonNull(executionId, "executionId");
        mutations = List.copyOf(mutations);
    }

    /** No entity traversal, old client Key, catalog read or authorization side effect. */
    public static BlobBindingIntents from(UserContext context, MutationPlan approvedPlan) {
        return from(EntityMetaFactory.requireFrom(context), approvedPlan);
    }

    static BlobBindingIntents from(EntityMetaFactory metadata, MutationPlan approvedPlan) {
        List<Mutation> mutations = new ArrayList<>();
        for (MutationOperation operation : approvedPlan.operations()) {
            Map<String, BlobFieldSpec> fields = new TreeMap<>();
            for (EntityDescriptor descriptor = metadata.resolveEntityDescriptor(operation.entity().entity());
                    descriptor != null; descriptor = descriptor.getParent()) {
                for (PropertyDescriptor property : descriptor.getProperties()) {
                    BlobFieldSpec spec = property.blobFieldSpec();
                    boolean blobType = property.getType() != null && property.getType().javaType() == BlobRef.class;
                    if (!blobType && spec == null) continue;
                    if (!blobType || spec == null || !spec.field().equals(property.getName())) {
                        throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
                    }
                    if (operation.kind() != MutationOperationKind.UPDATE
                            || operation.changedValues().containsKey(property.getName())) fields.putIfAbsent(property.getName(), spec);
                }
            }
            if (fields.isEmpty()) continue;
            Map<String, BlobRef> assignments = new LinkedHashMap<>();
            if (operation.kind() != MutationOperationKind.DELETE) {
                for (var field : fields.entrySet()) {
                    boolean explicit = operation.changedValues().containsKey(field.getKey());
                    if (!explicit && operation.kind() != MutationOperationKind.CREATE) continue;
                    Object value = operation.changedValues().get(field.getKey());
                    if (value != null && !(value instanceof BlobRef)) {
                        throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
                    }
                    if (value == null && !field.getValue().nullable()) {
                        throw new BlobException(BlobException.Code.BLOB_BIND_DENIED);
                    }
                    assignments.put(field.getKey(), (BlobRef) value);
                }
            }
            mutations.add(new Mutation(operation.kind(), operation.entity(), operation.originalVersion(), fields, assignments));
        }
        return new BlobBindingIntents(approvedPlan.executionId(), mutations);
    }

    /**
     * UPDATE contains changed Blob fields only. CREATE includes absent nullable initial values.
     * DELETE/RECOVER carry all model fields so the catalog can resolve authoritative old state.
     * A present null assignment means explicit clearing; absence on recovery preserves stored data.
     */
    public record Mutation(MutationOperationKind kind, EntityKey entity, Long originalVersion,
                           Map<String, BlobFieldSpec> fields, Map<String, BlobRef> assignments) {
        public Mutation {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(entity, "entity");
            fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
            // Map.copyOf rejects explicit clearing, whose null value is meaningful here.
            assignments = Collections.unmodifiableMap(new LinkedHashMap<>(assignments));
        }
    }
}
