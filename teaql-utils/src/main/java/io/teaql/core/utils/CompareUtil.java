package io.teaql.core.utils;

import java.util.Comparator;

public class CompareUtil {

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> int compare(T p0, T p1, boolean p2) {
        if (p0 == p1) {
            return 0;
        }
        if (p0 == null) {
            return p2 ? 1 : -1;
        }
        if (p1 == null) {
            return p2 ? -1 : 1;
        }
        if (p0 instanceof Comparable) {
            return ((Comparable) p0).compareTo(p1);
        }
        return p0.toString().compareTo(p1.toString());
    }

    public static <T> int compare(T p0, T p1, java.util.Comparator<T> p2) {
        if (p2 != null) {
            return p2.compare(p0, p1);
        }
        return compare(p0, p1, false);
    }

}
