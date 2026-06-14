package io.teaql.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

public class JSONUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static JSONObject parseObj(Object p0) {
        if (p0 == null) {
            return new JSONObject();
        }
        try {
            if (p0 instanceof String) {
                return parseObj((String) p0);
            }
            Map<String, Object> map = OBJECT_MAPPER.convertValue(p0, new TypeReference<Map<String, Object>>() {});
            return new JSONObject(map);
        } catch (Exception e) {
            throw new RuntimeException("Parse JSON failed", e);
        }
    }

    public static JSONObject parseObj(Object p0, boolean p1) {
        return parseObj(p0);
    }

    public static JSONObject parseObj(Object p0, boolean p1, boolean p2) {
        return parseObj(p0);
    }

    public static JSONObject parseObj(Object p0, Object p1) {
        return parseObj(p0);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject parseObj(String p0) {
        if (p0 == null || p0.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(p0, Map.class);
            return new JSONObject(map);
        } catch (Exception e) {
            throw new RuntimeException("Parse JSON failed", e);
        }
    }

    public static <T> T toBean(Object p0, io.teaql.core.utils.TypeReference<T> p1, boolean p2) {
        if (p0 == null || p1 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(p0, OBJECT_MAPPER.getTypeFactory().constructType(p1.getType()));
        } catch (Exception e) {
            throw new RuntimeException("Convert JSON to bean failed", e);
        }
    }

    public static <T> T toBean(Object p0, Type p1, boolean p2) {
        if (p0 == null || p1 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(p0, OBJECT_MAPPER.getTypeFactory().constructType(p1));
        } catch (Exception e) {
            throw new RuntimeException("Convert JSON to bean failed", e);
        }
    }

    public static <T> T toBean(JSONObject p0, Class<T> p1) {
        if (p0 == null || p1 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(p0, p1);
        } catch (Exception e) {
            throw new RuntimeException("Convert JSON to bean failed", e);
        }
    }

    public static <T> T toBean(String p0, io.teaql.core.utils.TypeReference<T> p1, boolean p2) {
        if (p1 == null) {
            return null;
        }
        if (p0 == null) {
            try {
                java.lang.reflect.Type type = p1.getType();
                if (type instanceof Class) {
                    return ((Class<T>) type).getDeclaredConstructor().newInstance();
                }
                throw new RuntimeException("Cannot instantiate generic type from null string: " + type);
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate generic type from null string", e);
            }
        }
        try {
            return OBJECT_MAPPER.readValue(p0, OBJECT_MAPPER.getTypeFactory().constructType(p1.getType()));
        } catch (Exception e) {
            throw new RuntimeException("Convert JSON to bean failed", e);
        }
    }

    public static <T> T toBean(String p0, Object p1, Class<T> p2) {
        return toBean(p0, p2);
    }

    public static <T> T toBean(String p0, Class<T> p1) {
        if (p0 == null || p1 == null) {
            if (p1 != null) {
                try {
                    return p1.getDeclaredConstructor().newInstance();
                } catch (Exception ignored) {}
            }
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(p0, p1);
        } catch (Exception e) {
            throw new RuntimeException("Convert JSON to bean failed", e);
        }
    }

    public static <T> T toBean(String p0, Type p1, boolean p2) {
        if (p0 == null || p1 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(p0, OBJECT_MAPPER.getTypeFactory().constructType(p1));
        } catch (Exception e) {
            throw new RuntimeException("Convert JSON to bean failed", e);
        }
    }

    public static String toJsonStr(Object p0) {
        if (p0 == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(p0);
        } catch (Exception e) {
            throw new RuntimeException("Serialize JSON failed", e);
        }
    }

    public static String toJsonStr(Object p0, int p1) {
        return toJsonStr(p0);
    }

    public static String toJsonStr(Object p0, Object p1) {
        return toJsonStr(p0);
    }

    public static void toJsonStr(Object p0, Writer p1) {
        if (p0 == null || p1 == null) {
            return;
        }
        try {
            OBJECT_MAPPER.writeValue(p1, p0);
        } catch (Exception e) {
            throw new RuntimeException("Serialize JSON failed", e);
        }
    }
}
