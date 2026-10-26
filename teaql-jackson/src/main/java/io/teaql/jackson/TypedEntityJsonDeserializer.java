package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.EntityStatus;
import io.teaql.core.FieldLayout;
import io.teaql.core.LoadState;
import io.teaql.core.SmartList;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.Relation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Model-bound JSON hydration; no bean setters, reflection, database calls or mutation APIs. */
final class TypedEntityJsonDeserializer extends JsonDeserializer<BaseEntity> {
    // Same wire-state boundary as the Rust context-owned reader. These are not
    // readonly business properties and must reject before the supplier is used.
    private static final java.util.Set<String> FORBIDDEN_RUNTIME_KEYS = java.util.Set.of(
            "_comment", "_dirty_fields", "_original_values", "_is_new", "_is_deleted",
            "__load_state", "__teaql_runtime_state");
    private record Binding(String member, Class<?> type, int index, Class<? extends Entity> element) {}
    private final Class<?> target;
    private final java.util.function.Supplier<? extends Entity> supplier;
    private final FieldLayout layout;
    private final Map<Integer, Binding> fixed;
    private final Map<String, Binding> relations;
    // Bounded, value-free geometry, owned by one mapper/model reader, not a process-global cache.
    private final Map<List<String>, LoadState> shapes = new LinkedHashMap<>(16, .75f, true);

    TypedEntityJsonDeserializer(EntityDescriptor descriptor) {
        target = descriptor.getTargetType();
        supplier = Objects.requireNonNull(descriptor.getEntitySupplier(), "Typed JSON requires an installed entity supplier");
        layout = FieldLayout.forType(target);
        if (!layout.isGenerated()) throw new IllegalArgumentException("Typed JSON requires generated field indexes: " + target.getName());
        Map<Integer, Binding> fields = new HashMap<>();
        Map<String, Binding> named = new HashMap<>();
        for (EntityDescriptor current = descriptor; current != null; current = current.getParent()) {
            for (var property : current.getProperties()) {
                Integer index = layout.findIndex(property.getName());
                Class<?> type = property.getType().javaType();
                Class<? extends Entity> element = null;
                if (SmartList.class.isAssignableFrom(type)) {
                    if (!(property instanceof Relation relation) || relation.getReverseProperty() == null) {
                        throw new IllegalArgumentException("Typed JSON list requires relation metadata: " + property.getName());
                    }
                    element = relation.getReverseProperty().getOwner().getTargetType();
                    Objects.requireNonNull(element, "Typed JSON relation element type");
                }
                Binding binding = new Binding(property.getName(), type, index == null ? -1 : index, element);
                if (index != null) fields.putIfAbsent(index, binding);
                else if (layout.relationNames().contains(property.getName())) named.putIfAbsent(property.getName(), binding);
                else throw new IllegalArgumentException("Metadata is outside the installed field layout: " + property.getName());
            }
        }
        if (fields.size() != layout.indexes().size()) throw new IllegalArgumentException("Incomplete typed JSON field metadata: " + target.getName());
        fixed = Map.copyOf(fields); relations = Map.copyOf(named);
    }

    private Binding binding(String name) {
        String canonical = layout.canonicalJsonField(name);
        if (canonical == null) return null;
        Integer index = layout.findIndex(canonical);
        if (index != null) return fixed.get(index);
        String relation = layout.canonicalRelationName(canonical);
        return relation == null ? null : relations.get(relation);
    }

    @Override public BaseEntity deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        boolean strictDuplicates = parser.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        JsonNode node;
        parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        try { node = ModelScalarJson.readTree(parser); }
        finally { if (!strictDuplicates) parser.disable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION); }
        if (!node.isObject()) return context.reportInputMismatch(target, "Expected a model entity object");
        List<String> members = new ArrayList<>();
        var names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (name.startsWith("_") && name.length() > 1) {
                if (FORBIDDEN_RUNTIME_KEYS.contains(name)) {
                    return context.reportInputMismatch(target, "Incoming runtime state is forbidden: %s", name);
                }
                continue; // readonly derived data, not load-state metadata
            }
            Binding field = binding(name);
            if (field == null) return context.reportInputMismatch(target, "Unknown or unsupported model field: %s", name);
            members.add(field.member());
        }
        members.sort(String::compareTo);
        for (int index = 1; index < members.size(); index++) {
            if (members.get(index).equals(members.get(index - 1))) {
                return context.reportInputMismatch(target, "Duplicate aliases for model field: %s", members.get(index));
            }
        }
        LoadState shape;
        synchronized (shapes) {
            shape = shapes.get(members);
            if (shape == null) {
                shape = LoadState.projection(layout, members);
                shapes.put(List.copyOf(members), shape);
                if (shapes.size() > 128) shapes.remove(shapes.keySet().iterator().next());
            }
        }
        Entity created = supplier.get();
        if (created == null || created.getClass() != target || !(created instanceof BaseEntity)) {
            return context.reportInputMismatch(target, "Installed model supplier returned an incompatible entity");
        }
        BaseEntity entity = (BaseEntity) created;
        LoadState initial = entity.__internalLoadState();
        if (initial.bits() != 0L || !initial.overflow().isEmpty() || !initial.selectedNames().isEmpty()
                || entity.__internalHasMutationLedger()) {
            return context.reportInputMismatch(target, "Installed model supplier must return a fresh entity");
        }
        entity.__internalUseLoadState(shape);
        var fields = node.fields();
        while (fields.hasNext()) {
            var field = fields.next(); String name = field.getKey(); JsonNode value = field.getValue();
            if (name.startsWith("_") && name.length() > 1) {
                entity.addDynamicProperty(name.substring(1), parser.getCodec().treeToValue(value, Object.class));
                continue;
            }
            Binding binding = binding(name);
            Object converted;
            if (binding.element() != null) {
                if (value.isNull()) converted = null;
                else {
                    if (!value.isArray()) return context.reportInputMismatch(target, "Expected relation array: %s", name);
                    List<Entity> children = new ArrayList<>(value.size());
                    for (JsonNode child : value) {
                        if (child.isNull()) return context.reportInputMismatch(target, "Null relation-list element: %s", name);
                        try { children.add(parser.getCodec().treeToValue(child, binding.element())); }
                        catch (JsonMappingException error) {
                            error.prependPath(children, children.size()); error.prependPath(target, name); throw error;
                        }
                    }
                    converted = SmartList.takeOwnership(children);
                }
            } else if (ModelScalarJson.supports(binding.type())) converted = ModelScalarJson.read(value, binding.type(), target, name);
            else {
                try { converted = parser.getCodec().treeToValue(value, binding.type()); }
                catch (JsonMappingException error) { error.prependPath(target, name); throw error; }
            }
            if (binding.index() >= 0) entity.__internalHydrate(binding.member(), converted, binding.index());
            else entity.__internalSet(binding.member(), converted);
        }
        // Persistence state is derived from the hydrated identity, never accepted
        // as wire metadata or invented as mutation intent. Hydrate all members
        // first so JSON order cannot change the lifecycle or original version.
        if (entity.getId() != null && entity.getVersion() != null && entity.getVersion() != 0) {
            entity.set$status(entity.getVersion() > 0 ? EntityStatus.PERSISTED : EntityStatus.PERSISTED_DELETED);
        } else if (entity.getId() != null && entity.getVersion() == null
                && members.stream().allMatch(name -> BaseEntity.ID_PROPERTY.equals(name) || BaseEntity.VERSION_PROPERTY.equals(name))) {
            // Native FK stubs carry identity but no editable snapshot/version.
            // Retain that reference-only contract across JSON without loading
            // target details, validating them as a new entity, or permitting edits.
            entity.set$status(EntityStatus.REFER);
        }
        return entity;
    }
}
