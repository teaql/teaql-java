package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.*;
import io.teaql.data.dynamic.DynamicFieldMutation;
import java.util.*;

/** Fixed model vocabulary at admission; never a native/wire alias converter. */
final class CanonicalMutationFields {
    private CanonicalMutationFields() {}

    static void validateGraph(UserContext context, EntityMetaFactory metadata, Entity root) {
        var validation = new Validation(context, metadata);
        validation.visit(root, null);
        // Raw detached changes must not disappear when the runtime materializes
        // commands. Reached typed keys already received their precise graph path.
        for (var ledger : validation.ledgers) {
            for (var entry : ledger.currentChangeSet().changes().entrySet()) {
                if (ledger.isNew(entry.getKey()) && ledger.isMarkedAsDelete(entry.getKey())) continue;
                if (!validation.representedKeys.contains(entry.getKey()))
                    validation.fields(entry.getKey().entity(), entry.getValue().keySet(), null);
            }
        }
        validation.finish();
    }

    static void validatePlan(UserContext context, EntityMetaFactory metadata, MutationPlan plan) {
        var validation = new Validation(context, metadata);
        for (var operation : plan.operations()) {
            for (var entry : operation.changedValues().entrySet()) {
                // Persisted dynamic fields have a separate typed ledger channel.
                // The normalized policy view names those intents with '#'.
                if (entry.getValue() instanceof DynamicFieldMutation mutation
                        && entry.getKey().equals("#" + mutation.code())) continue;
                validation.fields(operation.entity().entity(), List.of(entry.getKey()), null);
            }
        }
        validation.finish();
    }

    private static final class Validation {
        private final UserContext context;
        private final EntityMetaFactory metadata;
        // Per-type metadata, not a field-name set allocated on every row.
        private final Map<EntityDescriptor, Set<String>> modelFields = new IdentityHashMap<>();
        private final Set<Entity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<EntityMutationLedger> ledgers = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<EntityKey> representedKeys = new HashSet<>();
        private List<CheckResult> violations;

        private Validation(UserContext context, EntityMetaFactory metadata) { this.context = context; this.metadata = metadata; }
        private Set<String> names(String type) {
            var descriptor = metadata.resolveEntityDescriptor(type);
            if (descriptor == null) return Set.of();
            return modelFields.computeIfAbsent(descriptor, value -> {
                Set<String> names = new HashSet<>();
                for (var current = value; current != null; current = current.getParent())
                    for (var property : current.getProperties()) names.add(property.getName());
                if (!names.isEmpty()) { names.add(BaseEntity.ID_PROPERTY); names.add(BaseEntity.VERSION_PROPERTY); }
                return names;
            });
        }
        private void fields(String type, Iterable<String> fields, ObjectLocation parent) {
            Set<String> names = names(type);
            // Preserve manual legacy metadata that declares no field contract.
            if (names.isEmpty()) return;
            for (String field : fields) if (!names.contains(field)) {
                var location = parent == null ? ObjectLocation.hashRoot(field) : parent.member(field);
                var result = CheckResult.unknownField(location); result.setRootType(type);
                context.translateCheckResult(result);
                if (violations == null) violations = new ArrayList<>();
                violations.add(result);
            }
        }
        private void visit(Entity entity, ObjectLocation location) {
            if (!(entity instanceof BaseEntity base) || !visited.add(entity)) return;
            if (base.isNew() && base.isMarkedAsDelete()) return;
            var pending = base.getUpdatedProperties();
            fields(entity.typeName(), pending, location);
            var ledger = base.getEntityMutationLedger(); ledgers.add(ledger);
            var key = new EntityKey(entity.typeName(), entity.getId()); representedKeys.add(key);
            var raw = ledger.currentChangeSet().changes().get(key);
            if (raw != null) for (String field : raw.keySet())
                if (!pending.contains(field)) fields(entity.typeName(), List.of(field), location);
            var descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            for (var current = descriptor; current != null; current = current.getParent()) {
                for (var property : current.getProperties()) {
                    if (!(property instanceof Relation)) continue;
                    var childLocation = location == null ? ObjectLocation.hashRoot(property.getName()) : location.member(property.getName());
                    Object value = entity.getProperty(property.getName());
                    if (value instanceof Entity child) visit(child, childLocation);
                    else if (value instanceof Iterable<?> children) {
                        int index = 0;
                        for (Object child : children) { if (child instanceof Entity related) visit(related, childLocation.element(index)); index++; }
                    }
                }
            }
        }
        private void finish() { if (violations != null) throw new CheckException(violations); }
    }
}
