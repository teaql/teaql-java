package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.teaql.core.BaseEntity;

import java.io.IOException;
import java.util.Map;

public class BaseEntityJsonSerializer extends JsonSerializer<BaseEntity> {
    private static final Object ACTIVE_PATH = new Object();

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
            var state = value.__internalLoadState();
            var layout = state.layout();
            if (value.getId() != null) {
                serializers.defaultSerializeField(layout.isGenerated() ? layout.jsonFieldName(BaseEntity.ID_PROPERTY) : BaseEntity.ID_PROPERTY, value.getId(), gen);
            }
            if (value.getVersion() != null) {
                serializers.defaultSerializeField(layout.isGenerated() ? layout.jsonFieldName(BaseEntity.VERSION_PROPERTY) : BaseEntity.VERSION_PROPERTY, value.getVersion(), gen);
            }
            // A cycle retains the existing identity-only reference shape, without
            // recursively reserializing the same object's business fields.
            if (!added) {
                gen.writeEndObject();
                return;
            }
            if (layout.isGenerated()) {
                for (String field : layout.indexes().keySet()) {
                    if (BaseEntity.ID_PROPERTY.equals(field) || BaseEntity.VERSION_PROPERTY.equals(field)) continue;
                    if (!state.isLoaded(field)) continue;
                    String member = layout.jsonFieldName(field);
                    // Preserve this provider's graph path, configured serializers
                    // and per-call attributes during nested serialization.
                    // The selected wire spelling is not the native/member or model key used by
                    // generated entity access and the mutation ledger.
                    serializers.defaultSerializeField(member, value.__internalGet(field), gen);
                }
                for (String relation : layout.relationNames()) {
                    if (layout.findIndex(relation) != null || !state.isLoaded(relation)) continue;
                    serializers.defaultSerializeField(layout.jsonFieldName(relation), value.__internalGet(relation), gen);
                }
            }
            for (Map.Entry<String, Object> entry : value.getAdditionalInfo().entrySet()) {
                if (BaseEntity.ID_PROPERTY.equals(entry.getKey()) || BaseEntity.VERSION_PROPERTY.equals(entry.getKey())
                        || layout.findIndex(entry.getKey()) != null
                        || layout.canonicalRelationName(entry.getKey()) != null) continue;
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
