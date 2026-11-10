package io.teaql.jackson;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.teaql.core.SmartList;

public class SmartListAsListSerializer extends StdSerializer<SmartList> {

    protected SmartListAsListSerializer(Class<SmartList> type) {
        super(type);
    }

    @Override
    public void serialize(SmartList value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        List<?> data = value.getData();
        // Reuse the active provider's cycle path and scalar serializers even
        // when the generator is a TokenBuffer (ObjectMapper.valueToTree).
        provider.defaultSerializeValue(data, gen);
    }
}
