package io.teaql.core.utils;

import java.util.Objects;

public class ObjectUtil {

    public static boolean equals(java.lang.Object p0, java.lang.Object p1) {
        return Objects.equals(p0, p1);
    }

    public static boolean isEmpty(java.lang.Object p0) {
        return ObjUtil.isEmpty(p0);
    }

    public static boolean isNotEmpty(java.lang.Object p0) {
        return !isEmpty(p0);
    }

    public static boolean isNotNull(java.lang.Object p0) {
        return p0 != null;
    }

    public static boolean isNull(java.lang.Object p0) {
        return p0 == null;
    }

    public static <T extends java.lang.Comparable<? super T>> int compare(T p0, T p1) {
        return compare(p0, p1, false);
    }

    public static <T extends java.lang.Comparable<? super T>> int compare(T p0, T p1, boolean p2) {
        if (p0 == p1) {
            return 0;
        }
        if (p0 == null) {
            return p2 ? 1 : -1;
        }
        if (p1 == null) {
            return p2 ? -1 : 1;
        }
        return p0.compareTo(p1);
    }

    public static int length(java.lang.Object p0) {
        if (p0 == null) {
            return 0;
        }
        if (p0 instanceof CharSequence) {
            return ((CharSequence) p0).length();
        }
        if (p0 instanceof java.util.Collection) {
            return ((java.util.Collection<?>) p0).size();
        }
        if (p0 instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) p0).size();
        }
        if (p0.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(p0);
        }
        if (p0 instanceof java.lang.Iterable) {
            int count = 0;
            for (Object item : (java.lang.Iterable<?>) p0) {
                count++;
            }
            return count;
        }
        if (p0 instanceof java.util.Iterator) {
            int count = 0;
            java.util.Iterator<?> it = (java.util.Iterator<?>) p0;
            while (it.hasNext()) {
                it.next();
                count++;
            }
            return count;
        }
        if (p0 instanceof java.util.Enumeration) {
            int count = 0;
            java.util.Enumeration<?> en = (java.util.Enumeration<?>) p0;
            while (en.hasMoreElements()) {
                en.nextElement();
                count++;
            }
            return count;
        }
        return -1;
    }

}
