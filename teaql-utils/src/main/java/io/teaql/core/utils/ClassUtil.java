package io.teaql.core.utils;

import java.lang.reflect.Modifier;

public class ClassUtil {

    public static boolean isAbstract(java.lang.Class<?> p0) {
        return p0 != null && Modifier.isAbstract(p0.getModifiers());
    }

    public static boolean isAssignable(java.lang.Class<?> p0, java.lang.Class<?> p1) {
        if (p0 == null || p1 == null) {
            return false;
        }
        return p0.isAssignableFrom(p1);
    }

    public static boolean isInterface(java.lang.Class<?> p0) {
        if (p0 == null) {
            throw new NullPointerException("Class cannot be null");
        }
        return p0.isInterface();
    }

    public static boolean isSimpleValueType(java.lang.Class<?> p0) {
        if (p0 == null) {
            throw new NullPointerException("Class cannot be null");
        }
        return p0.isPrimitive()
            || p0 == String.class
            || p0 == Boolean.class
            || p0 == Character.class
            || p0 == Byte.class
            || p0 == Short.class
            || p0 == Integer.class
            || p0 == Long.class
            || p0 == Float.class
            || p0 == Double.class
            || p0 == Void.class
            || CharSequence.class.isAssignableFrom(p0)
            || Number.class.isAssignableFrom(p0)
            || java.util.Date.class.isAssignableFrom(p0)
            || java.time.temporal.Temporal.class.isAssignableFrom(p0)
            || p0.isEnum()
            || p0 == Class.class
            || p0 == java.net.URI.class
            || p0 == java.net.URL.class;
    }

    public static <T> java.lang.Class<T> loadClass(java.lang.String p0) {
        return loadClass(p0, true);
    }

    @SuppressWarnings("unchecked")
    public static <T> java.lang.Class<T> loadClass(java.lang.String p0, boolean p1) {
        if (p0 == null) {
            throw new RuntimeException("Class name cannot be null");
        }
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ClassUtil.class.getClassLoader();
            }
            return (Class<T>) Class.forName(p0, p1, cl);
        } catch (Exception e) {
            throw new RuntimeException("Load class failed: " + p0, e);
        }
    }

    public static java.lang.reflect.Method[] getPublicMethods(java.lang.Class<?> p0) {
        if (p0 == null) {
            return new java.lang.reflect.Method[0];
        }
        return p0.getMethods();
    }

    public static java.util.List<java.lang.reflect.Method> getPublicMethods(java.lang.Class<?> p0, io.teaql.core.utils.Filter<java.lang.reflect.Method> p1) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.List<java.lang.reflect.Method> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Method m : p0.getMethods()) {
            if (p1 == null || p1.accept(m)) {
                list.add(m);
            }
        }
        return list;
    }

    public static java.util.List<java.lang.reflect.Method> getPublicMethods(java.lang.Class<?> p0, java.lang.String... p1) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.Set<String> names = new java.util.HashSet<>(java.util.Arrays.asList(p1));
        java.util.List<java.lang.reflect.Method> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Method m : p0.getMethods()) {
            if (p1 == null || p1.length == 0 || names.contains(m.getName())) {
                list.add(m);
            }
        }
        return list;
    }

    public static java.util.List<java.lang.reflect.Method> getPublicMethods(java.lang.Class<?> p0, java.lang.reflect.Method... p1) {
        if (p0 == null) return new java.util.ArrayList<>();
        java.util.Set<java.lang.reflect.Method> methods = new java.util.HashSet<>(java.util.Arrays.asList(p1));
        java.util.List<java.lang.reflect.Method> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Method m : p0.getMethods()) {
            if (p1 == null || p1.length == 0 || methods.contains(m)) {
                list.add(m);
            }
        }
        return list;
    }

}
