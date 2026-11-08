package io.teaql.core.checker.internal;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.EntityKey;
import io.teaql.core.EntityMutationLedger;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.Relation;
import java.util.*;

/** Framework-owned final-value checks, after customer Fix and before any provider call. */
public final class FiniteScalarAdmission {
    private FiniteScalarAdmission() {}

    public static boolean nonFinite(Object value) {
        return value instanceof Double doubleValue && !Double.isFinite(doubleValue)
                || value instanceof Float floatValue && !Float.isFinite(floatValue);
    }

    /** Standalone Checkers use entity-owned geometry, never a context/provider lookup. */
    public static List<CheckResult> collect(Entity root) {
        return collect(null, root, false);
    }

    public static List<CheckResult> collect(EntityMetaFactory metadata, Entity root, boolean modifiedOnly) {
        var validation = new Validation(metadata, modifiedOnly);
        validation.visit(root, null);
        for (var ledger : validation.ledgers) {
            for (var entry : ledger.currentChangeSet().changes().entrySet()) {
                if (modifiedOnly && ledger.isNew(entry.getKey()) && ledger.isMarkedAsDelete(entry.getKey())) continue;
                if (!validation.represented.contains(entry.getKey()))
                    validation.values(entry.getKey().entity(), entry.getValue(), null);
            }
        }
        return validation.violations == null ? List.of() : validation.violations;
    }

    /** Detached plan channels get the same final-value policy, without allocating a graph. */
    public static List<CheckResult> collect(String type, Map<String, Object> values) {
        var validation = new Validation(null, true);
        validation.values(type, values, null);
        return validation.violations == null ? List.of() : validation.violations;
    }

    private static final class Validation {
        final EntityMetaFactory metadata;
        final boolean modifiedOnly;
        final Set<Entity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<EntityMutationLedger> ledgers = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<EntityKey> represented = new HashSet<>();
        List<CheckResult> violations;

        Validation(EntityMetaFactory metadata, boolean modifiedOnly) {
            this.metadata = metadata; this.modifiedOnly = modifiedOnly;
        }
        void value(String type, String field, Object value, ObjectLocation parent) {
            if (!nonFinite(value)) return;
            var location = parent == null ? ObjectLocation.hashRoot(field) : parent.member(field);
            var result = CheckResult.invalidType(location, "Expected a finite floating-point number");
            result.setRootType(type);
            // NaN/Infinity have no portable JSON number representation.
            // Leave the optional diagnostic input absent; never stringify it.
            if (violations == null) violations = new ArrayList<>();
            violations.add(result);
        }
        void values(String type, Map<String, Object> values, ObjectLocation location) {
            values.forEach((field, value) -> value(type, field, value, location));
        }
        void visit(Entity entity, ObjectLocation location) {
            if (!(entity instanceof BaseEntity base) || !visited.add(entity)) return;
            if (modifiedOnly && base.isNew() && base.isMarkedAsDelete()) return;
            var key = new EntityKey(entity.typeName(), entity.getId());
            represented.add(key);
            Map<String, Object> pending = Map.of();
            if (base.__internalHasMutationLedger()) {
                var ledger = base.getEntityMutationLedger(); ledgers.add(ledger);
                var raw = ledger.currentChangeSet().changes().get(key);
                if (raw != null) pending = raw;
            }
            boolean inspect = !modifiedOnly || base.needPersist() || !pending.isEmpty();
            var descriptor = metadata == null ? null : metadata.resolveEntityDescriptor(entity.typeName());
            if (descriptor == null) {
                // The generated layout and availability snapshot are already
                // attached to the row. Reuse them for standalone checks and
                // unindexed adapters; do not introduce a runtime dependency.
                var state = base.__internalLoadState();
                var layout = state.layout();
                for (String field : layout.indexes().keySet())
                    if (state.isLoaded(field)) available(base, field, pending, inspect, location);
                for (String field : layout.relationNames())
                    if (!layout.indexes().containsKey(field) && state.isLoaded(field))
                        available(base, field, pending, inspect, location);
                for (String field : state.selectedNames())
                    if (!field.startsWith("_") && !field.startsWith("#")
                            && !layout.indexes().containsKey(field) && !layout.relationNames().contains(field))
                        available(base, field, pending, inspect, location);
                if (inspect) values(entity.typeName(), pending, location);
                return;
            }
            // Geometry belongs to metadata. No per-record field-name HashSet
            // or merged copy of all values is required for scalar admission.
            if (inspect) {
                for (var current = descriptor; current != null; current = current.getParent()) {
                    for (var property : current.getProperties()) {
                        if (property instanceof Relation) continue;
                        String field = property.getName();
                        if (!pending.containsKey(field)) value(entity.typeName(), field, entity.getProperty(field), location);
                    }
                }
                values(entity.typeName(), pending, location);
            }
            for (var current = descriptor; current != null; current = current.getParent()) {
                for (var property : current.getProperties()) {
                    if (!(property instanceof Relation)) continue;
                    String field = property.getName();
                    related(entity.getProperty(field), field, location);
                }
            }
        }

        void available(BaseEntity entity, String field, Map<String, Object> pending,
                boolean inspect, ObjectLocation location) {
            Object current = entity.getProperty(field);
            related(current, field, location);
            if (inspect && !pending.containsKey(field)) value(entity.typeName(), field, current, location);
        }

        void related(Object value, String field, ObjectLocation location) {
            var childLocation = location == null ? ObjectLocation.hashRoot(field) : location.member(field);
            if (value instanceof Entity child) visit(child, childLocation);
            else if (value instanceof Iterable<?> children) {
                int index = 0;
                for (Object child : children) {
                    if (child instanceof Entity item) visit(item, childLocation.element(index));
                    index++;
                }
            }
        }
    }
}
