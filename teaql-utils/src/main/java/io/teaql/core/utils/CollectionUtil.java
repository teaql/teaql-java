package io.teaql.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectionUtil {

    public static boolean contains(java.util.Collection<?> p0, java.lang.Object p1) {
        return p0 != null && p0.contains(p1);
    }

    public static <T> boolean contains(java.util.Collection<T> p0, java.util.function.Predicate<? super T> p1) {
        if (p0 == null || p1 == null) return false;
        for (T item : p0) {
            if (p1.test(item)) return true;
        }
        return false;
    }

    public static boolean isEmpty(java.lang.Iterable<?> p0) {
        return p0 == null || !p0.iterator().hasNext();
    }

    public static boolean isEmpty(java.util.Collection<?> p0) {
        return p0 == null || p0.isEmpty();
    }

    public static boolean isEmpty(java.util.Enumeration<?> p0) {
        return p0 == null || !p0.hasMoreElements();
    }

    public static boolean isEmpty(java.util.Iterator<?> p0) {
        return p0 == null || !p0.hasNext();
    }

    public static boolean isEmpty(java.util.Map<?, ?> p0) {
        return p0 == null || p0.isEmpty();
    }

    public static int size(java.lang.Object p0) {
        if (p0 == null) return 0;
        if (p0 instanceof java.util.Collection) return ((java.util.Collection<?>) p0).size();
        if (p0 instanceof java.util.Map) return ((java.util.Map<?, ?>) p0).size();
        if (p0 instanceof java.lang.Iterable) {
            int count = 0;
            for (Object x : (java.lang.Iterable<?>) p0) count++;
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
        if (p0.getClass().isArray()) return java.lang.reflect.Array.getLength(p0);
        return 0;
    }

    public static <T> T findOne(java.lang.Iterable<T> p0, io.teaql.core.utils.Filter<T> p1) {
        if (p0 == null) return null;
        if (p1 == null) throw new NullPointerException("Filter cannot be null");
        for (T item : p0) {
            if (p1.accept(item)) {
                return item;
            }
        }
        return null;
    }

    public static <T> T get(java.util.Collection<T> p0, int p1) {
        return CollUtil.get(p0, p1);
    }

    public static <T> T getFirst(java.lang.Iterable<T> p0) {
        return CollUtil.getFirst(p0);
    }

    public static <T> T getFirst(java.util.Iterator<T> p0) {
        return CollUtil.getFirst(p0);
    }

    public static <T> T getLast(java.util.Collection<T> p0) {
        if (p0 == null || p0.isEmpty()) return null;
        if (p0 instanceof java.util.List) {
            return ((java.util.List<T>) p0).get(p0.size() - 1);
        }
        T last = null;
        for (T item : p0) {
            last = item;
        }
        return last;
    }

    public static <T> java.lang.String join(java.lang.Iterable<T> p0, java.lang.CharSequence p1) {
        if (p0 == null) return null;
        return join(p0.iterator(), p1);
    }

    public static <T> java.lang.String join(java.lang.Iterable<T> p0, java.lang.CharSequence p1, java.lang.String p2, java.lang.String p3) {
        if (p0 == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(p2);
        boolean first = true;
        for (T item : p0) {
            if (!first) {
                sb.append(p1);
            }
            sb.append(item);
            first = false;
        }
        sb.append(p3);
        return sb.toString();
    }

    public static <T> java.lang.String join(java.lang.Iterable<T> p0, java.lang.CharSequence p1, java.util.function.Function<T, ? extends java.lang.CharSequence> p2) {
        if (p0 == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T item : p0) {
            if (!first) {
                sb.append(p1);
            }
            sb.append(p2.apply(item));
            first = false;
        }
        return sb.toString();
    }

    public static <T> java.lang.String join(java.util.Iterator<T> p0, java.lang.CharSequence p1) {
        if (p0 == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (p0.hasNext()) {
            if (!first) {
                sb.append(p1);
            }
            sb.append(p0.next());
            first = false;
        }
        return sb.toString();
    }

    public static <T> java.util.Collection<T> subtract(java.util.Collection<T> p0, java.util.Collection<T> p1) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.List<T> res = new java.util.ArrayList<>(p0);
        if (p1 != null) {
            res.removeAll(p1);
        }
        return res;
    }

    public static <T, R> java.util.List<R> map(java.lang.Iterable<T> p0, java.util.function.Function<? super T, ? extends R> p1, boolean p2) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.List<R> res = new java.util.ArrayList<>();
        for (T item : p0) {
            if (item == null && p2) continue;
            R mapped = p1.apply(item);
            if (mapped == null && p2) continue;
            res.add(mapped);
        }
        return res;
    }

    public static <T> java.util.List<T> sub(java.util.Collection<T> p0, int p1, int p2) {
        return sub(p0, p1, p2, 1);
    }

    public static <T> java.util.List<T> sub(java.util.Collection<T> p0, int p1, int p2, int p3) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.List<T> list = new java.util.ArrayList<>(p0);
        return sub(list, p1, p2, p3);
    }

    public static <T> java.util.List<T> sub(java.util.List<T> p0, int p1, int p2) {
        return sub(p0, p1, p2, 1);
    }

    public static <T> java.util.List<T> sub(java.util.List<T> p0, int p1, int p2, int p3) {
        if (p0 == null) return new java.util.ArrayList<>();
        int len = p0.size();
        if (p1 < 0) p1 += len;
        if (p2 < 0) p2 += len;
        if (p1 < 0) p1 = 0;
        if (p1 > len) p1 = len;
        if (p2 < 0) p2 = 0;
        if (p2 > len) p2 = len;
        if (p1 > p2) {
            int tmp = p1;
            p1 = p2;
            p2 = tmp;
        }
        if (p3 <= 0) p3 = 1;
        java.util.List<T> res = new java.util.ArrayList<>();
        for (int i = p1; i < p2; i += p3) {
            res.add(p0.get(i));
        }
        return res;
    }

}
