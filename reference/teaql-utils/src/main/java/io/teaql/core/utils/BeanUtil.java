package io.teaql.core.utils;



import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (p0 == null) {
            return null;
        }
        return JSONUtil.toBean(JSONUtil.toJsonStr(p0), p1);
    }

    public static java.util.Map<java.lang.String, java.lang.Object> beanToMap(java.lang.Object p0) {
        if (p0 == null) {
            return null;
        }
        return JSONUtil.toBean(JSONUtil.toJsonStr(p0), new TypeReference<java.util.Map<java.lang.String, java.lang.Object>>() {}, true);
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

}
