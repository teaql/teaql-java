package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.core.BaseEntity;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class BaseEntityJsonDeserializer extends JsonDeserializer<BaseEntity> {

    @Override
    public BaseEntity deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        BaseEntity entity = new BaseEntity();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode value = field.getValue();
            if (BaseEntity.ID_PROPERTY.equals(name)) {
                entity.internalSet(BaseEntity.ID_PROPERTY, value.isNull() ? null : value.longValue());
            } else if (BaseEntity.VERSION_PROPERTY.equals(name)) {
                entity.internalSet(BaseEntity.VERSION_PROPERTY, value.isNull() ? null : value.longValue());
            } else {
                entity.putAdditional(name, parser.getCodec().treeToValue(value, Object.class));
            }
        }
        return entity;
    }
}
