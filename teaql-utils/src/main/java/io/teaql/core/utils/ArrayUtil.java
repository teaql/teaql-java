package io.teaql.core.utils;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ArrayUtil {

    private static int[] getStartEnd(int length, int start, int end) {
        if (start < 0) {
            start += length;
        }
        if (end < 0) {
            end += length;
        }
        if (start < 0) start = 0;
        if (start > length) start = length;
        if (end < 0) end = 0;
        if (end > length) end = length;
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        return new int[]{start, end};
    }

    public static <T> boolean contains(T[] p0, T p1) {
        if (p0 == null) return false;
        for (T item : p0) {
            if (Objects.equals(item, p1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isArray(java.lang.Object p0) {
        return p0 != null && p0.getClass().isArray();
    }

    public static boolean contains(boolean[] p0, boolean p1) {
        if (p0 == null) return false;
        for (boolean b : p0) {
            if (b == p1) return true;
        }
        return false;
    }

    public static boolean contains(byte[] p0, byte p1) {
        if (p0 == null) return false;
        for (byte b : p0) {
            if (b == p1) return true;
        }
        return false;
    }

    public static boolean contains(char[] p0, char p1) {
        if (p0 == null) return false;
        for (char b : p0) {
            if (b == p1) return true;
        }
        return false;
    }

    public static boolean contains(double[] p0, double p1) {
        if (p0 == null) return false;
        for (double b : p0) {
            if (Double.doubleToLongBits(b) == Double.doubleToLongBits(p1)) return true;
        }
        return false;
    }

    public static boolean contains(float[] p0, float p1) {
        if (p0 == null) return false;
        for (float b : p0) {
            if (Float.floatToIntBits(b) == Float.floatToIntBits(p1)) return true;
        }
        return false;
    }

    public static boolean contains(int[] p0, int p1) {
        if (p0 == null) return false;
        for (int b : p0) {
            if (b == p1) return true;
        }
        return false;
    }

    public static boolean contains(long[] p0, long p1) {
        if (p0 == null) return false;
        for (long b : p0) {
            if (b == p1) return true;
        }
        return false;
    }

    public static boolean contains(short[] p0, short p1) {
        if (p0 == null) return false;
        for (short b : p0) {
            if (b == p1) return true;
        }
        return false;
    }

    public static boolean[] sub(boolean[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        boolean[] res = new boolean[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    public static byte[] toArray(java.nio.ByteBuffer p0) {
        if (p0 == null) return null;
        if (p0.hasArray()) {
            return p0.array();
        }
        byte[] bytes = new byte[p0.remaining()];
        p0.get(bytes);
        return bytes;
    }

    public static byte[] sub(byte[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        byte[] res = new byte[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    public static char[] sub(char[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        char[] res = new char[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    public static double[] sub(double[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        double[] res = new double[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    public static float[] sub(float[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        float[] res = new float[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    public static int length(java.lang.Object p0) {
        if (p0 == null) return 0;
        if (p0.getClass().isArray()) {
            return Array.getLength(p0);
        }
        return 0;
    }

    public static int[] sub(int[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        int[] res = new int[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(java.lang.Object p0, int p1) {
        if (p0 == null) {
            return null;
        }
        if (!p0.getClass().isArray()) {
            return null;
        }
        int len = Array.getLength(p0);
        if (p1 < 0) {
            p1 += len;
        }
        if (p1 < 0 || p1 >= len) {
            return null;
        }
        return (T) Array.get(p0, p1);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeNull(T[] p0) {
        if (p0 == null) return null;
        java.util.List<T> list = new java.util.ArrayList<>();
        for (T item : p0) {
            if (item != null) {
                list.add(item);
            }
        }
        T[] res = (T[]) java.lang.reflect.Array.newInstance(p0.getClass().getComponentType(), list.size());
        return list.toArray(res);
    }

    public static java.lang.Object[] sub(java.lang.Object p0, int p1, int p2) {
        return sub(p0, p1, p2, 1);
    }

    public static java.lang.Object[] sub(java.lang.Object p0, int p1, int p2, int p3) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        if (!p0.getClass().isArray()) {
            throw new IllegalArgumentException("Object must be an array");
        }
        int length = Array.getLength(p0);
        int[] range = getStartEnd(length, p1, p2);
        int start = range[0];
        int end = range[1];
        if (p3 <= 0) p3 = 1;
        int len = (end - start + p3 - 1) / p3;
        Object[] res = new Object[len];
        int idx = 0;
        for (int i = start; i < end; i += p3) {
            res[idx++] = Array.get(p0, i);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] sub(T[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        T[] res = (T[]) java.lang.reflect.Array.newInstance(p0.getClass().getComponentType(), len);
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(java.lang.Iterable<T> p0, java.lang.Class<T> p1) {
        if (p0 == null) return (T[]) java.lang.reflect.Array.newInstance(p1, 0);
        java.util.List<T> list = new java.util.ArrayList<>();
        for (T item : p0) {
            list.add(item);
        }
        T[] res = (T[]) java.lang.reflect.Array.newInstance(p1, list.size());
        return list.toArray(res);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(java.util.Collection<T> p0, java.lang.Class<T> p1) {
        if (p0 == null) return (T[]) java.lang.reflect.Array.newInstance(p1, 0);
        T[] res = (T[]) java.lang.reflect.Array.newInstance(p1, p0.size());
        return p0.toArray(res);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(java.util.Iterator<T> p0, java.lang.Class<T> p1) {
        if (p0 == null) return (T[]) java.lang.reflect.Array.newInstance(p1, 0);
        java.util.List<T> list = new java.util.ArrayList<>();
        while (p0.hasNext()) {
            list.add(p0.next());
        }
        T[] res = (T[]) java.lang.reflect.Array.newInstance(p1, list.size());
        return list.toArray(res);
    }

    public static long[] sub(long[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        long[] res = new long[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

    public static short[] sub(short[] p0, int p1, int p2) {
        if (p0 == null) throw new NullPointerException("Array cannot be null");
        int[] range = getStartEnd(p0.length, p1, p2);
        int len = range[1] - range[0];
        short[] res = new short[len];
        System.arraycopy(p0, range[0], res, 0, len);
        return res;
    }

}
