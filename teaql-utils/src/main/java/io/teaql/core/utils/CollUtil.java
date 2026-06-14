package io.teaql.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CollUtil {

    public static <T> T get(java.util.Collection<T> p0, int p1) {
        if (p0 == null || p0.isEmpty()) return null;
        int size = p0.size();
        if (p1 < 0) {
            p1 += size;
        }
        if (p1 < 0 || p1 >= size) {
            return null;
        }
        if (p0 instanceof java.util.List) {
            return ((java.util.List<T>) p0).get(p1);
        }
        int i = 0;
        for (T item : p0) {
            if (i == p1) {
                return item;
            }
            i++;
        }
        return null;
    }

    public static <T> T getFirst(java.lang.Iterable<T> p0) {
        if (p0 == null) return null;
        java.util.Iterator<T> iterator = p0.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static <T> T getFirst(java.util.Iterator<T> p0) {
        if (p0 == null) return null;
        return p0.hasNext() ? p0.next() : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> java.util.Collection<T> filterNew(java.util.Collection<T> p0, io.teaql.core.utils.Filter<T> p1) {
        if (p0 == null) return null;
        java.util.Collection<T> result;
        try {
            result = p0.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            result = new java.util.ArrayList<>();
        }
        if (p1 != null) {
            for (T item : p0) {
                if (p1.accept(item)) {
                    result.add(item);
                }
            }
        } else {
            result.addAll(p0);
        }
        return result;
    }

}
