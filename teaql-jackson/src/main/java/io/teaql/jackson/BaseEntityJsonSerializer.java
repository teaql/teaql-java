package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.teaql.core.BaseEntity;

import java.io.IOException;
import java.util.Map;

public class BaseEntityJsonSerializer extends JsonSerializer<BaseEntity> {
    private static final Object ACTIVE_PATH = new Object();
    private final Map<String, String> accessorKeys;
    private final Map<String, String> relationWireNames;

    public BaseEntityJsonSerializer() {
        accessorKeys = Map.of();
        relationWireNames = Map.of();
    }

    /** Bind value-free accessor geometry to the installed model, not to row values. */
    BaseEntityJsonSerializer(io.teaql.core.meta.EntityDescriptor descriptor) {
        var layout = io.teaql.core.FieldLayout.forType(descriptor.getTargetType());
        var byIndex = new java.util.HashMap<Integer, String>();
        for (var current = descriptor; current != null; current = current.getParent()) {
            for (var property : current.getProperties()) {
                Integer index = layout.findIndex(property.getName());
                if (index != null) byIndex.putIfAbsent(index, property.getName());
            }
        }
        var keys = new java.util.HashMap<String, String>();
        layout.indexes().forEach((field, index) -> {
            String accessor = byIndex.get(index);
            if (accessor == null) throw new IllegalArgumentException("Incomplete typed JSON field metadata: " + field);
            keys.put(field, accessor);
        });
        accessorKeys = Map.copyOf(keys);
        var wireNames = new java.util.HashMap<String, String>();
        for (String relation : layout.relationNames()) {
            wireNames.put(relation, io.teaql.core.utils.NamingCase.toCamelCase(relation));
        }
        relationWireNames = Map.copyOf(wireNames);
    }

    @Override
    public void serialize(BaseEntity value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        @SuppressWarnings("unchecked")
        var path = (java.util.Set<BaseEntity>) serializers.getAttribute(ACTIVE_PATH);
        if (path == null) {
            path = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            serializers.setAttribute(ACTIVE_PATH, path);
        }
        boolean added = path.add(value);
        try {
            gen.writeStartObject();
            if (value.getId() != null) {
                serializers.defaultSerializeField(BaseEntity.ID_PROPERTY, value.getId(), gen);
            }
            if (value.getVersion() != null) {
                serializers.defaultSerializeField(BaseEntity.VERSION_PROPERTY, value.getVersion(), gen);
            }
            // A cycle retains the existing identity-only reference shape, without
            // recursively reserializing the same object's business fields.
            if (!added) {
                gen.writeEndObject();
                return;
            }
            var state = value.__internalLoadState();
            var layout = state.layout();
            if (layout.isGenerated()) {
                for (String field : layout.indexes().keySet()) {
                    if (BaseEntity.ID_PROPERTY.equals(field) || BaseEntity.VERSION_PROPERTY.equals(field)) continue;
                    if (!state.isLoaded(field)) continue;
                    String member = layout.memberName(field);
                    // Preserve this provider's graph path, configured serializers
                    // and per-call attributes during nested serialization.
                    serializers.defaultSerializeField(member,
                            value.__internalGet(accessorKeys.getOrDefault(field, member)), gen);
                }
                for (String relation : layout.relationNames()) {
                    if (layout.findIndex(relation) != null || !state.isLoaded(relation)) continue;
                    serializers.defaultSerializeField(relationWireNames.getOrDefault(relation, relation),
                            value.__internalGet(relation), gen);
                }
            }
            for (Map.Entry<String, Object> entry : value.getAdditionalInfo().entrySet()) {
                if (BaseEntity.ID_PROPERTY.equals(entry.getKey()) || BaseEntity.VERSION_PROPERTY.equals(entry.getKey())
                        || layout.findIndex(entry.getKey()) != null
                        || layout.relationNames().contains(entry.getKey())) continue;
                serializers.defaultSerializeField(entry.getKey(), entry.getValue(), gen);
            }
            gen.writeEndObject();
        } finally {
            if (added) path.remove(value);
            // Retain an empty, value-free guard in this serialization provider,
            // so list rows reuse it rather than allocating one set per entity.
        }
    }

    @Override
    public Class<BaseEntity> handledType() {
        return BaseEntity.class;
    }
}
