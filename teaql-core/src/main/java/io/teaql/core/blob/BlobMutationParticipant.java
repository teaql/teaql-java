package io.teaql.core.blob;

import io.teaql.core.EntityKey;
import io.teaql.core.FrameworkInternal;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.MutationPlan;
import io.teaql.core.UserContext;
import java.util.List;
import java.util.Objects;

/** Optional binding participant. Every invocation receives its current trusted context. */
public interface BlobMutationParticipant {
    /** Pure planning after mutation approval. Never mutate entities, perform I/O or cache an actor. */
    @FrameworkInternal("Runtime save integration; share the business transaction")
    Plan plan(UserContext context, MutationPlan approvedPlan);

    interface Plan {
        /** Pure identity/capability check before opening the transaction or writing business data. */
        void requireTransaction(Object transactionResource);

        /** Lock and recheck authoritative bindings on the already active business transaction. */
        Prepared prepare(UserContext context, Object transactionResource);
    }

    interface Prepared {
        /** Apply bindings before commit, using actual persisted IDs and revisions. Never commit. */
        void apply(UserContext context, Object transactionResource, List<PersistedEntity> entities);
    }

    /** Immutable identity mapping; no mutable business entity escapes into catalog state. */
    record PersistedEntity(EntityKey plannedEntity, EntityKey persistedEntity, Long version,
                           MutationOperationKind kind) {
        public PersistedEntity {
            Objects.requireNonNull(plannedEntity, "plannedEntity");
            Objects.requireNonNull(persistedEntity, "persistedEntity");
            Objects.requireNonNull(kind, "kind");
            if (persistedEntity.id() == null || persistedEntity.id() <= 0 || version == null || version == 0) {
                throw new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED);
            }
            if (!plannedEntity.entity().equals(persistedEntity.entity())) {
                throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
            }
        }
    }
}
