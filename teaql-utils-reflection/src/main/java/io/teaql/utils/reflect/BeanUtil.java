package io.teaql.utils.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BeanUtil {

    @SuppressWarnings("unchecked")
    public static <T> T getProperty(java.lang.Object p0, java.lang.String p1) {
        if (p0 == null || p1 == null) {
            return null;
        }
        try {
            return (T) getPropertyManual(p0, p1);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getPropertyManual(Object obj, String path) {
        if (obj == null || path == null) return null;
        String[] parts = path.split("\\.");
        Object current = obj;
        for (String part : parts) {
            if (current == null) return null;
            current = getSimpleProperty(current, part);
        }
        return current;
    }

    private static Object getSimpleProperty(Object obj, String part) {
        if (obj == null) return null;
        int openBracket = part.indexOf('[');
        String propName = openBracket >= 0 ? part.substring(0, openBracket) : part;
        Object val = obj;
        if (!propName.isEmpty()) {
            if (obj instanceof Map) {
                val = ((Map<?, ?>) obj).get(propName);
            } else {
                try {
                    val = ReflectUtil.invoke(obj, "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1));
                } catch (Exception e) {
                    try {
                        Field f = ReflectUtil.getField(obj.getClass(), propName);
                        if (f != null) {
                            f.setAccessible(true);
                            val = f.get(obj);
                        } else {
                            val = null;
                        }
                    } catch (Exception ex) {
                        val = null;
                    }
                }
            }
        }
        if (openBracket >= 0) {
            int closeBracket = part.indexOf(']');
            if (closeBracket > openBracket) {
                String indexStr = part.substring(openBracket + 1, closeBracket);
                int index = Integer.parseInt(indexStr);
                if (val instanceof List) {
                    List<?> list = (List<?>) val;
                    val = (index >= 0 && index < list.size()) ? list.get(index) : null;
                } else if (val != null && val.getClass().isArray()) {
                    int len = Array.getLength(val);
                    val = (index >= 0 && index < len) ? Array.get(val, index) : null;
                } else {
                    val = null;
                }
            }
        }
        return val;
    }

    public static <T> T toBean(java.lang.Object p0, java.lang.Class<T> p1) {
        if (p0 == null || p1 == null) {
            return null;
        }
        if (p1.isInstance(p0)) {
            return p1.cast(p0);
        }
        T bean = ReflectUtil.newInstance(p1);
        if (p0 instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    setProperty(bean, String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return bean;
        }
        Map<String, Object> map = beanToMap(p0);
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                setProperty(bean, entry.getKey(), entry.getValue());
            }
        }
        return bean;
    }

    public static java.util.Map<java.lang.String, java.lang.Object> beanToMap(java.lang.Object p0) {
        if (p0 == null) {
            return null;
        }
        if (p0 instanceof Map<?, ?> source) {
            Map<String, Object> ret = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (entry.getKey() != null) {
                    ret.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return ret;
        }
        Map<String, Object> ret = new LinkedHashMap<>();
        for (Method method : p0.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
                continue;
            }
            String name = propertyName(method);
            if (name == null || "class".equals(name)) {
                continue;
            }
            try {
                ret.put(name, method.invoke(p0));
            } catch (Exception ignored) {
            }
        }
        if (!ret.isEmpty()) {
            return ret;
        }
        for (Field field : p0.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                ret.put(field.getName(), field.get(p0));
            } catch (Exception ignored) {
            }
        }
        return ret;
    }

    public static java.util.Map<java.lang.String, java.lang.Object> beanToMap(java.lang.Object p0, boolean p1, boolean p2) {
        return beanToMap(p0);
    }

    public static java.util.Map<java.lang.String, java.lang.Object> beanToMap(java.lang.Object p0, java.lang.String... p1) {
        java.util.Map<String, Object> map = beanToMap(p0);
        if (map == null || p1 == null || p1.length == 0) {
            return map;
        }
        java.util.Set<String> keys = new java.util.HashSet<>(java.util.Arrays.asList(p1));
        map.keySet().retainAll(keys);
        return map;
    }

    public static java.util.Map<java.lang.String, java.lang.Object> beanToMap(java.lang.Object p0, java.util.Map<java.lang.String, java.lang.Object> p1, boolean p2, boolean p3) {
        java.util.Map<String, Object> map = beanToMap(p0);
        if (map != null && p1 != null) {
            p1.putAll(map);
            return p1;
        }
        return map;
    }

    public static void setProperty(java.lang.Object p0, java.lang.String p1, java.lang.Object p2) {
        if (p0 == null) {
            throw new RuntimeException("Bean cannot be null");
        }
        if (p1 == null) {
            throw new RuntimeException("Property path cannot be null");
        }
        try {
            setPropertyManual(p0, p1, p2);
        } catch (Exception e) {
            throw new RuntimeException("Set property failed: " + p1, e);
        }
    }

    private static void setPropertyManual(Object obj, String path, Object value) throws Exception {
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            String parentPath = path.substring(0, lastDot);
            String propName = path.substring(lastDot + 1);
            Object parent = getProperty(obj, parentPath);
            if (parent == null) {
                throw new RuntimeException("Parent property is null in path: " + path);
            }
            setSimpleProperty(parent, propName, value);
        } else {
            setSimpleProperty(obj, path, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setSimpleProperty(Object obj, String part, Object value) throws Exception {
        int openBracket = part.indexOf('[');
        String propName = openBracket >= 0 ? part.substring(0, openBracket) : part;
        if (openBracket >= 0) {
            Object listObj = getSimpleProperty(obj, propName);
            int closeBracket = part.indexOf(']');
            int index = Integer.parseInt(part.substring(openBracket + 1, closeBracket));
            if (listObj instanceof List) {
                List<Object> list = (List<Object>) listObj;
                while (list.size() <= index) {
                    list.add(null);
                }
                list.set(index, value);
            } else if (listObj != null && listObj.getClass().isArray()) {
                Array.set(listObj, index, value);
            } else {
                throw new RuntimeException("Property " + propName + " is not a list or array");
            }
        } else {
            if (obj instanceof Map) {
                ((Map<Object, Object>) obj).put(propName, value);
            } else {
                String setterName = "set" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
                try {
                    ReflectUtil.invoke(obj, setterName, value);
                } catch (Exception e) {
                    Field f = ReflectUtil.getField(obj.getClass(), propName);
                    if (f != null) {
                        f.setAccessible(true);
                        f.set(obj, value);
                    } else {
                        throw new NoSuchFieldException("No field " + propName + " on " + obj.getClass());
                    }
                }
            }
        }
    }

    private static String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == Boolean.class || method.getReturnType() == boolean.class)) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }
}
