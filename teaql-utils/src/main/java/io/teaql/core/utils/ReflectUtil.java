package io.teaql.core.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {

    public static java.lang.Object getStaticFieldValue(java.lang.reflect.Field p0) {
        if (p0 == null) return null;
        try {
            p0.setAccessible(true);
            return p0.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Get static field value failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(java.lang.Object p0, java.lang.String p1, java.lang.Object... p2) {
        if (p0 == null) {
            throw new RuntimeException("Target object cannot be null");
        }
        Class<?> clazz = p0.getClass();
        Method method = getMethodByName(clazz, p1);
        if (method == null) {
            throw new RuntimeException("Method not found: " + p1);
        }
        return invoke(p0, method, p2);
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(java.lang.Object p0, java.lang.reflect.Method p1, java.lang.Object... p2) {
        if (p1 == null) {
            throw new RuntimeException("Method cannot be null");
        }
        try {
            p1.setAccessible(true);
            return (T) p1.invoke(p0, p2);
        } catch (Exception e) {
            throw new RuntimeException("Invoke failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeStatic(java.lang.reflect.Method p0, java.lang.Object... p1) {
        return invoke(null, p0, p1);
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(java.lang.Class<T> p0, java.lang.Object... p1) {
        if (p0 == null) {
            throw new RuntimeException("Class cannot be null");
        }
        try {
            if (p1 == null || p1.length == 0) {
                Constructor<T> ctor = p0.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            }
            Class<?>[] parameterTypes = new Class<?>[p1.length];
            for (int i = 0; i < p1.length; i++) {
                parameterTypes[i] = p1[i] != null ? p1[i].getClass() : Object.class;
            }
            for (Constructor<?> c : p0.getDeclaredConstructors()) {
                if (c.getParameterCount() == p1.length) {
                    try {
                        c.setAccessible(true);
                        return (T) c.newInstance(p1);
                    } catch (Exception ignored) {}
                }
            }
            throw new RuntimeException("Constructor not found");
        } catch (Exception e) {
            throw new RuntimeException("Create new instance failed", e);
        }
    }

    public static <T> T newInstance(java.lang.String p0) {
        Class<T> clazz = ClassUtil.loadClass(p0);
        return newInstance(clazz);
    }

    public static <T> T newInstanceIfPossible(java.lang.Class<T> p0) {
        try {
            return newInstance(p0);
        } catch (Exception e) {
            return null;
        }
    }

    private static final java.util.Map<String, java.lang.reflect.Field> fieldCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, java.lang.reflect.Method> methodCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.lang.reflect.Field NULL_FIELD;
    private static final java.lang.reflect.Method NULL_METHOD;

    static {
        java.lang.reflect.Field tempField = null;
        try {
            tempField = ReflectUtil.class.getDeclaredField("NULL_FIELD");
        } catch (Exception e) {
        }
        NULL_FIELD = tempField;

        java.lang.reflect.Method tempMethod = null;
        try {
            tempMethod = ReflectUtil.class.getDeclaredMethod("getMethodByName", java.lang.Class.class, boolean.class, java.lang.String.class);
        } catch (Exception e) {
        }
        NULL_METHOD = tempMethod;
    }

    public static java.lang.reflect.Field getField(java.lang.Class<?> p0, java.lang.String p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (p1 == null) {
            return null;
        }
        String cacheKey = p0.getName() + ":" + p1;
        java.lang.reflect.Field cached = fieldCache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_FIELD ? null : cached;
        }

        Class<?> current = p0;
        java.lang.reflect.Field found = null;
        while (current != null) {
            try {
                found = current.getDeclaredField(p1);
                break;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        if (found != null) {
            fieldCache.put(cacheKey, found);
        } else {
            fieldCache.put(cacheKey, NULL_FIELD);
        }
        return found;
    }

    public static java.lang.reflect.Method getMethodByName(java.lang.Class<?> p0, boolean p1, java.lang.String p2) {
        if (p0 == null || p2 == null) return null;
        String cacheKey = p0.getName() + ":" + p1 + ":" + p2;
        java.lang.reflect.Method cached = methodCache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_METHOD ? null : cached;
        }

        java.lang.reflect.Method found = null;
        for (Method m : p0.getMethods()) {
            if (p1 ? m.getName().equalsIgnoreCase(p2) : m.getName().equals(p2)) {
                found = m;
                break;
            }
        }
        if (found == null) {
            for (Method m : p0.getDeclaredMethods()) {
                if (p1 ? m.getName().equalsIgnoreCase(p2) : m.getName().equals(p2)) {
                    found = m;
                    break;
                }
            }
        }

        if (found != null) {
            methodCache.put(cacheKey, found);
        } else {
            methodCache.put(cacheKey, NULL_METHOD);
        }
        return found;
    }

    public static java.lang.reflect.Method getMethodByName(java.lang.Class<?> p0, java.lang.String p1) {
        return getMethodByName(p0, false, p1);
    }

    public static java.lang.reflect.Method getMethodOfObj(java.lang.Object p0, java.lang.String p1, java.lang.Object... p2) {
        if (p0 == null) return null;
        return getMethodByName(p0.getClass(), p1);
    }

    public static java.lang.reflect.Method getPublicMethod(java.lang.Class<?> p0, java.lang.String p1, java.lang.Class<?>... p2) {
        if (p0 == null || p1 == null) return null;
        try {
            return p0.getMethod(p1, p2);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}
