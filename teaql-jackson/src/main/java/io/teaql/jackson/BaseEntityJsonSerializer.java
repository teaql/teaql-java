package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.teaql.core.BaseEntity;

import java.io.IOException;
import java.util.Map;

public class BaseEntityJsonSerializer extends JsonSerializer<BaseEntity> {

    @Override
    public void serialize(BaseEntity value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        if (value.getId() != null) {
            gen.writeObjectField(BaseEntity.ID_PROPERTY, value.getId());
        }
        if (value.getVersion() != null) {
            gen.writeObjectField(BaseEntity.VERSION_PROPERTY, value.getVersion());
        }
        for (Map.Entry<String, Object> entry : value.getAdditionalInfo().entrySet()) {
            gen.writeObjectField(entry.getKey(), entry.getValue());
        }
        gen.writeEndObject();
    }

    @Override
    public Class<BaseEntity> handledType() {
        return BaseEntity.class;
    }
}
