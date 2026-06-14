package io.teaql.core.utils;

public class BooleanUtil {

    public static boolean toBoolean(java.lang.String p0) {
        if (p0 == null) {
            return false;
        }
        String s = p0.trim().toLowerCase();
        return "true".equals(s) || "yes".equals(s) || "y".equals(s) || "1".equals(s) || "ok".equals(s);
    }

}
