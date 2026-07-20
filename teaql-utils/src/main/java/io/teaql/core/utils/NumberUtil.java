package io.teaql.core.utils;

import java.math.BigDecimal;

public class NumberUtil {

    public static boolean isGreater(BigDecimal p0, BigDecimal p1) {
        if (p0 == null || p1 == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        return p0.compareTo(p1) > 0;
    }

    public static boolean isLess(BigDecimal p0, BigDecimal p1) {
        if (p0 == null || p1 == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        return p0.compareTo(p1) < 0;
    }

    public static double add(double p0, double p1) {
        return BigDecimal.valueOf(p0).add(BigDecimal.valueOf(p1)).doubleValue();
    }

    public static double add(double p0, float p1) {
        return BigDecimal.valueOf(p0).add(BigDecimal.valueOf(p1)).doubleValue();
    }

    public static double add(float p0, double p1) {
        return BigDecimal.valueOf(p0).add(BigDecimal.valueOf(p1)).doubleValue();
    }

    public static double add(float p0, float p1) {
        return BigDecimal.valueOf(p0).add(BigDecimal.valueOf(p1)).doubleValue();
    }

    public static double add(Double p0, Double p1) {
        return add(p0 == null ? 0.0 : p0.doubleValue(), p1 == null ? 0.0 : p1.doubleValue());
    }

    public static Number parseNumber(String p0) {
        if (p0 == null) {
            throw new IllegalArgumentException("Number string cannot be null");
        }
        String s = p0.trim();
        if (s.isEmpty()) {
            throw new NumberFormatException("Empty number string");
        }
        if (s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
            return Double.valueOf(s);
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException e2) {
                return new BigDecimal(s);
            }
        }
    }

    public static BigDecimal add(Number p0, Number p1) {
        BigDecimal b0 = toBigDecimal(p0);
        BigDecimal b1 = toBigDecimal(p1);
        return b0.add(b1);
    }

    public static BigDecimal add(Number... p0) {
        if (p0 == null || p0.length == 0) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (Number num : p0) {
            sum = sum.add(toBigDecimal(num));
        }
        return sum;
    }

    public static BigDecimal add(String... p0) {
        if (p0 == null || p0.length == 0) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (String s : p0) {
            sum = sum.add(toBigDecimal(s));
        }
        return sum;
    }

    public static BigDecimal add(BigDecimal... p0) {
        if (p0 == null || p0.length == 0) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal b : p0) {
            if (b != null) {
                sum = sum.add(b);
            }
        }
        return sum;
    }

    public static BigDecimal toBigDecimal(Number p0) {
        if (p0 == null) {
            return BigDecimal.ZERO;
        }
        if (p0 instanceof BigDecimal) {
            return (BigDecimal) p0;
        }
        if (p0 instanceof Long || p0 instanceof Integer || p0 instanceof Short || p0 instanceof Byte) {
            return BigDecimal.valueOf(p0.longValue());
        }
        return BigDecimal.valueOf(p0.doubleValue());
    }

    public static BigDecimal toBigDecimal(String p0) {
        if (p0 == null || p0.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(p0.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid BigDecimal string: " + p0, e);
        }
    }

}
