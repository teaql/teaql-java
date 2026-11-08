package io.teaql.runtime.mutation;

import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.*;
import io.teaql.core.utils.NamingCase;
import java.util.*;

/** Inspect the entire unexecuted graph before any customer callback or I/O. */
public final class MutationVersionAdmission {
    private MutationVersionAdmission() {}

    @FrameworkInternal("Mutation planning admission only, not post-write live-ledger replay")
    public static void validateGraph(EntityMetaFactory metadata, Entity root) {
        var validation = new Validation(metadata);
        validation.visit(root, null);
        for (var ledger : validation.ledgers) {
            var changes = ledger.currentChangeSet();
            Set<EntityKey> keys = new TreeSet<>(changes.changes().keySet());
            keys.addAll(changes.dynamicChanges().keySet());
            keys.addAll(ledger.deletedKeys()); keys.addAll(ledger.recoveredKeys());
            for (EntityKey key : keys) {
                if (ledger.isNew(key) && ledger.isMarkedAsDelete(key)) continue;
                if (validation.represented.contains(key)) continue;
                var kind = ledger.isMarkedAsDelete(key) ? MutationOperationKind.DELETE
                        : ledger.isNew(key) ? MutationOperationKind.CREATE
                        : ledger.recoveredKeys().contains(key) ? MutationOperationKind.RECOVER : MutationOperationKind.UPDATE;
                validation.check(kind, ledger.getOriginalVersion(key), key.entity(), null);
            }
        }
        validation.finish();
    }

    @FrameworkInternal("Admission of the complete, still-unexecuted mutation plan")
    public static void validatePlan(MutationPlan plan) {
        var validation = new Validation(null);
        for (var operation : plan.operations())
            validation.check(operation.kind(), operation.originalVersion(), operation.entity().entity(), null);
        validation.finish();
    }

    private static final class Validation {
        final EntityMetaFactory metadata;
        final Set<Entity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<EntityMutationLedger> ledgers = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<EntityKey> represented = new HashSet<>();
        final List<CheckResult> violations = new ArrayList<>();
        List<BaseEntity> cancelledNewBranches;
        MutationVersionException firstCause;
        Validation(EntityMetaFactory metadata) { this.metadata = metadata; }

        void check(MutationOperationKind kind, Long version, String type, ObjectLocation parent) {
            try { MutationVersions.next(kind, version); }
            catch (MutationVersionException cause) {
                var result = new CheckResult();
                result.setRuleId(CheckResult.RuleId.valueOf(cause.code().name()));
                result.setRootType(type);
                result.setLocation(parent == null ? ObjectLocation.hashRoot("version") : parent.member("version"));
                result.setNaturalLanguageStatement(cause.getMessage());
                violations.add(result);
                if (firstCause == null) firstCause = cause;
            }
        }

        void visit(Entity entity, ObjectLocation location) {
            if (!(entity instanceof BaseEntity base) || !visited.add(entity)) return;
            if (base.isNew() && base.isMarkedAsDelete()) {
                if (cancelledNewBranches == null) cancelledNewBranches = new ArrayList<>();
                cancelledNewBranches.add(base);
                return;
            }
            var ledger = base.getEntityMutationLedger(); ledgers.add(ledger);
            var key = new EntityKey(entity.typeName(), entity.getId());
            if (base.get$status() != EntityStatus.REFER) {
                represented.add(key);
                var changes = ledger.currentChangeSet();
                MutationOperationKind kind = base.deleteItem() || ledger.isMarkedAsDelete(key) ? MutationOperationKind.DELETE
                        : base.newItem() || ledger.isNew(key) ? MutationOperationKind.CREATE
                        : base.recoverItem() || ledger.recoveredKeys().contains(key) ? MutationOperationKind.RECOVER
                        : base.updateItem() || !base.getUpdatedProperties().isEmpty()
                            || changes.changes().containsKey(key) || changes.dynamicChanges().containsKey(key) ? MutationOperationKind.UPDATE : null;
                if (kind != null) {
                    Long version = base.getOriginalVersion();
                    check(kind, version == null ? base.getVersion() : version, entity.typeName(), location);
                }
            }
            for (var descriptor = metadata.resolveEntityDescriptor(entity.typeName()); descriptor != null; descriptor = descriptor.getParent()) {
                for (var property : descriptor.getProperties()) {
                    if (!(property instanceof Relation)) continue;
                    String name = NamingCase.toUnderlineCase(property.getName());
                    var childLocation = location == null ? ObjectLocation.hashRoot(name) : location.member(name);
                    Object value = entity.getProperty(property.getName());
                    if (value instanceof Entity child) visit(child, childLocation);
                    else if (value instanceof Iterable<?> children) {
                        int index = 0;
                        for (Object child : children) { if (child instanceof Entity related) visit(related, childLocation.element(index)); index++; }
                    }
                }
            }
        }

        void finish() {
            if (!violations.isEmpty()) {
                var error = new CheckException(violations); error.initCause(firstCause); throw error;
            }
            if (cancelledNewBranches != null) {
                // Do not guess whether to discard pending descendant intent or
                // cascade cancellation. An ambiguous branch must never write
                // orphan records. Explicitly reached live objects remain live;
                // a different same-ID view/reference does not authorize adoption.
                Set<Entity> active = Collections.newSetFromMap(new IdentityHashMap<>());
                active.addAll(visited);
                for (var branch : cancelledNewBranches) active.remove(branch);
                Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
                for (var branch : cancelledNewBranches) inspectCancelledBranch(branch, active, seen);
            }
        }

        private void inspectCancelledBranch(Entity entity, Set<Entity> active, Set<Entity> seen) {
            if (!(entity instanceof BaseEntity base) || active.contains(entity) || !seen.add(entity)) return;
            if (!(base.isNew() && base.isMarkedAsDelete())
                    && (base.needPersist() || !base.getUpdatedProperties().isEmpty())) {
                throw new TeaQLRuntimeException("CANCELLED_NEW_BRANCH_HAS_PENDING_DESCENDANTS: Explicitly cancel or reattach pending descendants before saving");
            }
            for (var descriptor = metadata.resolveEntityDescriptor(entity.typeName()); descriptor != null; descriptor = descriptor.getParent()) {
                for (var property : descriptor.getProperties()) {
                    if (!(property instanceof Relation)) continue;
                    Object value = entity.getProperty(property.getName());
                    if (value instanceof Entity child) inspectCancelledBranch(child, active, seen);
                    else if (value instanceof Iterable<?> children)
                        for (Object child : children) if (child instanceof Entity related) inspectCancelledBranch(related, active, seen);
                }
            }
        }
    }
}
