package io.teaql.core.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;

public class Convert {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static void registerModule(Module module) {
        OBJECT_MAPPER.registerModule(module);
    }

    public static <T> T convert(io.teaql.core.utils.TypeReference<T> p0, java.lang.Object p1) {
        if (p1 == null) {
            return null;
        }
        return convert(p0.getType(), p1);
    }

    public static <T> T convert(java.lang.Class<T> p0, java.lang.Object p1) {
        if (p1 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(p1, p0);
        } catch (Exception e) {
            throw new RuntimeException("Convert failed", e);
        }
    }

    public static <T> T convert(java.lang.Class<T> p0, java.lang.Object p1, T p2) {
        if (p1 == null) {
            return p2;
        }
        try {
            return OBJECT_MAPPER.convertValue(p1, p0);
        } catch (Exception e) {
            return p2;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(java.lang.reflect.Type p0, java.lang.Object p1) {
        if (p1 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(p1, OBJECT_MAPPER.getTypeFactory().constructType(p0));
        } catch (Exception e) {
            throw new RuntimeException("Convert failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(java.lang.reflect.Type p0, java.lang.Object p1, T p2) {
        if (p1 == null) {
            return p2;
        }
        try {
            return OBJECT_MAPPER.convertValue(p1, OBJECT_MAPPER.getTypeFactory().constructType(p0));
        } catch (Exception e) {
            return p2;
        }
    }

}
