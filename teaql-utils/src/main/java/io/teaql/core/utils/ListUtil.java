package io.teaql.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListUtil {

    public static <T> java.util.ArrayList<T> toList(java.lang.Iterable<T> p0) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        for (T item : p0) {
            list.add(item);
        }
        return list;
    }

    public static <T> java.util.ArrayList<T> toList(T... p0) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.ArrayList<T> list = new java.util.ArrayList<>(p0.length);
        for (T item : p0) {
            list.add(item);
        }
        return list;
    }

    public static <T> java.util.ArrayList<T> toList(java.util.Collection<T> p0) {
        if (p0 == null) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(p0);
    }

    public static <T> java.util.ArrayList<T> toList(java.util.Enumeration<T> p0) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        while (p0.hasMoreElements()) {
            list.add(p0.nextElement());
        }
        return list;
    }

    public static <T> java.util.ArrayList<T> toList(java.util.Iterator<T> p0) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        while (p0.hasNext()) {
            list.add(p0.next());
        }
        return list;
    }

    public static <T> java.util.List<T> empty() {
        return java.util.Collections.emptyList();
    }

    public static <T> java.util.List<T> list(boolean p0) {
        return p0 ? new java.util.concurrent.CopyOnWriteArrayList<>() : new java.util.ArrayList<>();
    }

    public static <T> java.util.List<T> list(boolean p0, java.lang.Iterable<T> p1) {
        java.util.List<T> list = list(p0);
        if (p1 != null) {
            for (T item : p1) {
                list.add(item);
            }
        }
        return list;
    }

    public static <T> java.util.List<T> list(boolean p0, T... p1) {
        java.util.List<T> list = list(p0);
        if (p1 != null) {
            for (T item : p1) {
                list.add(item);
            }
        }
        return list;
    }

    public static <T> java.util.List<T> list(boolean p0, java.util.Collection<T> p1) {
        java.util.List<T> list = list(p0);
        if (p1 != null) {
            list.addAll(p1);
        }
        return list;
    }

    public static <T> java.util.List<T> list(boolean p0, java.util.Enumeration<T> p1) {
        java.util.List<T> list = list(p0);
        if (p1 != null) {
            while (p1.hasMoreElements()) {
                list.add(p1.nextElement());
            }
        }
        return list;
    }

    public static <T> java.util.List<T> list(boolean p0, java.util.Iterator<T> p1) {
        java.util.List<T> list = list(p0);
        if (p1 != null) {
            while (p1.hasNext()) {
                list.add(p1.next());
            }
        }
        return list;
    }

    public static <T> java.util.List<T> of(T... p0) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.ArrayList<T> list = new java.util.ArrayList<>(p0.length);
        for (T item : p0) {
            list.add(item);
        }
        return list;
    }

}
