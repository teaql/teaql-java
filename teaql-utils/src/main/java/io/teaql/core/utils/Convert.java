package io.teaql.core.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Convert {

    @Deprecated
    public static void registerModule(Object module) {
        // JSON modules are handled by teaql-utils-json / teaql-jackson.
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
        return convertToClass(p0, p1);
    }

    public static <T> T convert(java.lang.Class<T> p0, java.lang.Object p1, T p2) {
        if (p1 == null) {
            return p2;
        }
        try {
            return convertToClass(p0, p1);
        } catch (Exception e) {
            return p2;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(java.lang.reflect.Type p0, java.lang.Object p1) {
        if (p1 == null) {
            return null;
        }
        if (p0 instanceof Class<?>) {
            return (T) convert((Class<?>) p0, p1);
        }
        if (p0 instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?>) {
            return (T) convert((Class<?>) parameterizedType.getRawType(), p1);
        }
        throw new RuntimeException("Convert failed: unsupported target type " + p0);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(java.lang.reflect.Type p0, java.lang.Object p1, T p2) {
        if (p1 == null) {
            return p2;
        }
        try {
            return convert(p0, p1);
        } catch (Exception e) {
            return p2;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertToClass(Class<T> targetType, Object value) {
        if (targetType == null) {
            throw new RuntimeException("Target type cannot be null");
        }
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return (T) value;
        }
        if (targetType == Object.class) {
            return (T) value;
        }
        if (targetType == String.class) {
            return (T) String.valueOf(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(BooleanUtil.toBoolean(String.valueOf(value)));
        }
        if (targetType == Character.class || targetType == char.class) {
            String str = String.valueOf(value);
            if (str.isEmpty()) {
                throw new RuntimeException("Cannot convert empty string to character");
            }
            return (T) Character.valueOf(str.charAt(0));
        }
        if (Number.class.isAssignableFrom(wrap(targetType)) || targetType.isPrimitive()) {
            return (T) convertNumber(wrap(targetType), value);
        }
        if (targetType.isEnum()) {
            return (T) Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), String.valueOf(value));
        }
        if (targetType == LocalDateTime.class) {
            return (T) LocalDateTime.parse(String.valueOf(value));
        }
        if (targetType == LocalDate.class) {
            return (T) LocalDate.parse(String.valueOf(value));
        }
        if (targetType == LocalTime.class) {
            return (T) LocalTime.parse(String.valueOf(value));
        }
        throw new RuntimeException("Convert failed: unsupported target type " + targetType.getName());
    }

    private static Object convertNumber(Class<?> targetType, Object value) {
        if (targetType == Byte.class) {
            return number(value).byteValue();
        }
        if (targetType == Short.class) {
            return number(value).shortValue();
        }
        if (targetType == Integer.class) {
            return number(value).intValue();
        }
        if (targetType == Long.class) {
            return number(value).longValue();
        }
        if (targetType == Float.class) {
            return number(value).floatValue();
        }
        if (targetType == Double.class) {
            return number(value).doubleValue();
        }
        if (targetType == BigInteger.class) {
            return new BigDecimal(String.valueOf(value)).toBigInteger();
        }
        if (targetType == BigDecimal.class || targetType == Number.class) {
            return number(value);
        }
        throw new RuntimeException("Convert failed: unsupported numeric target type " + targetType.getName());
    }

    private static BigDecimal number(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
