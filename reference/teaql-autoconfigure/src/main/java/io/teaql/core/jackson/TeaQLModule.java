package io.teaql.core.jackson;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.teaql.core.utils.Convert;
import io.teaql.core.utils.DateUtil;
import io.teaql.core.utils.TemporalAccessorUtil;

import io.teaql.core.SmartList;

public class TeaQLModule extends SimpleModule {
    public static final TeaQLModule INSTANCE = new TeaQLModule();

    private TeaQLModule() {
        super("TeaQL");
        addSerializer(SmartList.class, new SmartListAsListSerializer(SmartList.class));

        setDeserializerModifier(
                new BeanDeserializerModifier() {
                    @Override
                    public JsonDeserializer<?> modifyDeserializer(
                            DeserializationConfig config,
                            BeanDescription beanDesc,
                            JsonDeserializer<?> deserializer) {
                        Class<?> beanClass = beanDesc.getBeanClass();
                        if (beanClass.equals(SmartList.class)) {
                            return new ListAsSmartListDeserializer<>(beanDesc.getType());
                        }
                        if (io.teaql.core.Entity.class.isAssignableFrom(beanClass)) {
                            return new RemoteInputCheckingDeserializer((JsonDeserializer<Object>) deserializer, beanClass);
                        }
                        return super.modifyDeserializer(config, beanDesc, deserializer);
                    }
                });
        addSerializer(
                TemporalAccessor.class,
                new JsonSerializer<>() {
                    @Override
                    public void serialize(
                            TemporalAccessor value, JsonGenerator gen, SerializerProvider provider)
                            throws IOException {
                        if (value == null) {
                            gen.writeNull();
                            return;
                        }
                        long epochMilli = TemporalAccessorUtil.toEpochMilli(value);
                        gen.writeNumber(epochMilli);
                    }
                });

        addDeserializer(
                LocalDateTime.class,
                new JsonDeserializer<>() {
                    @Override
                    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx)
                            throws IOException, JacksonException {
                        return TeaQLModule.deserialize(p, LocalDateTime.class);
                    }
                });

        addDeserializer(
                LocalDate.class,
                new JsonDeserializer<>() {
                    @Override
                    public LocalDate deserialize(JsonParser p, DeserializationContext ctx)
                            throws IOException, JacksonException {
                        return TeaQLModule.deserialize(p, LocalDate.class);
                    }
                });
    }

    public static <T extends TemporalAccessor> T deserialize(JsonParser p, Class<T> type)
            throws IOException, JacksonException {
        Number number = p.getNumberValue();
        if (number == null) {
            return null;
        }
        LocalDateTime localDateTime = DateUtil.toLocalDateTime(new Date(number.longValue()));
        return Convert.convert(type, localDateTime);
    }
}
