package io.teaql.core.jackson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;

public class ListAsSmartListDeserializer<T extends BaseEntity>
        extends StdDeserializer<SmartList<T>> {
    protected ListAsSmartListDeserializer(JavaType valueType) {
        super(valueType);
    }

    @Override
    public SmartList deserialize(JsonParser p, DeserializationContext ctx)
            throws IOException, JacksonException {
        SmartList list = new SmartList();
        CollectionLikeType type =
                ctx.getTypeFactory()
                        .constructCollectionLikeType(ArrayList.class, _valueType.containedType(0));
        list.setData(
                p.readValueAs(
                        new TypeReference<>() {
                            @Override
                            public Type getType() {
                                return type;
                            }
                        }));
        return list;
    }
}
