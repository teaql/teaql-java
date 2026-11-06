package io.teaql.dataservice.sql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/** Shared provider boundary, not a numeric rounding policy or application API. */
public final class SqlNumericConversions {
    private SqlNumericConversions() {}

    public static boolean supports(Class<?> target) {
        return target == Long.class || target == Integer.class || target == Short.class || target == Byte.class
                || target == Float.class || target == Double.class || target == BigDecimal.class;
    }

    public static Object convert(Class<?> target, Number number) {
        if (target == Double.class) {
            double value = number.doubleValue();
            if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite JDBC Double");
            return number instanceof Double ? number : Double.valueOf(value);
        }
        if (target == Float.class) {
            float value = number.floatValue();
            if (!Float.isFinite(value)) throw new ArithmeticException("Nonfinite JDBC Float");
            return number instanceof Float ? number : Float.valueOf(value);
        }
        if (target == BigDecimal.class) {
            if (number instanceof BigDecimal) return number;
            if (number instanceof BigInteger integer) return new BigDecimal(integer);
            // Floating input is already approximate. Exact storage uses
            // BigDecimal or text; this conversion cannot recover lost digits.
            if (number instanceof Float || number instanceof Double) return BigDecimal.valueOf(number.doubleValue());
            if (integralCarrier(number)) return BigDecimal.valueOf(number.longValue());
            return new BigDecimal(number.toString());
        }
        long value = exactLong(number);
        if (target == Long.class) return value;
        if (target == Integer.class) return Math.toIntExact(value);
        if (target == Short.class && value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) return (short) value;
        if (target == Byte.class && value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) return (byte) value;
        throw new ArithmeticException("JDBC integral value outside target range");
    }

    /** Deliberately excludes the untrusted value and original exception cause. */
    public static IllegalArgumentException invalidColumn(int index, String label, Class<?> target) {
        String safe = label != null && label.matches("[A-Za-z_][A-Za-z0-9_]*")
                ? " (" + label.toLowerCase(Locale.ROOT) + ")" : "";
        return new IllegalArgumentException("Invalid JDBC column " + index + safe + " as " + target.getName());
    }

    private static boolean integralCarrier(Number number) {
        return number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long
                || number instanceof java.util.concurrent.atomic.AtomicInteger || number instanceof java.util.concurrent.atomic.AtomicLong;
    }

    private static long exactLong(Number number) {
        if (integralCarrier(number)) return number.longValue();
        if (number instanceof BigInteger integer) return integer.longValueExact();
        if (number instanceof BigDecimal decimal) return decimal.longValueExact();
        if (number instanceof Float || number instanceof Double) {
            double value = number.doubleValue();
            // 2^63 is exclusive: the double representation of Long.MAX_VALUE
            // rounds there, so a Java saturating cast is not valid admission.
            if (!Double.isFinite(value) || value != Math.rint(value) || value < -0x1.0p63 || value >= 0x1.0p63) {
                throw new ArithmeticException("JDBC number is not an exact signed 64-bit integer");
            }
            return (long) value;
        }
        return new BigDecimal(number.toString()).longValueExact();
    }
}
