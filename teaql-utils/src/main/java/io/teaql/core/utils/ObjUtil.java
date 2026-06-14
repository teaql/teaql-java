package io.teaql.core.utils;

public class ObjUtil {

    public static boolean isEmpty(java.lang.Object p0) {
        if (p0 == null) {
            return true;
        }
        if (p0 instanceof CharSequence) {
            return ((CharSequence) p0).length() == 0;
        }
        if (p0 instanceof java.util.Collection) {
            return ((java.util.Collection<?>) p0).isEmpty();
        }
        if (p0 instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) p0).isEmpty();
        }
        if (p0 instanceof java.lang.Iterable) {
            return !((java.lang.Iterable<?>) p0).iterator().hasNext();
        }
        if (p0 instanceof java.util.Iterator) {
            return !((java.util.Iterator<?>) p0).hasNext();
        }
        if (p0 instanceof java.util.Enumeration) {
            return !((java.util.Enumeration<?>) p0).hasMoreElements();
        }
        if (p0.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(p0) == 0;
        }
        return false;
    }

}
