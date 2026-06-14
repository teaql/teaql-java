package io.teaql.core.utils;

import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.function.Function;

public class StrUtil {

    public static boolean contains(CharSequence p0, char p1) {
        return p0 != null && p0.toString().indexOf(p1) >= 0;
    }

    public static boolean contains(CharSequence p0, CharSequence p1) {
        return p0 != null && p1 != null && p0.toString().contains(p1);
    }

    public static boolean endWith(CharSequence p0, char p1) {
        return p0 != null && p0.length() > 0 && p0.charAt(p0.length() - 1) == p1;
    }

    public static boolean endWith(CharSequence p0, CharSequence p1) {
        return p0 != null && p1 != null && p0.toString().endsWith(p1.toString());
    }

    public static boolean endWith(CharSequence p0, CharSequence p1, boolean p2) {
        if (p0 == null || p1 == null) {
            return false;
        }
        if (p2) {
            return p0.toString().toLowerCase().endsWith(p1.toString().toLowerCase());
        }
        return endWith(p0, p1);
    }

    public static boolean endWith(CharSequence p0, CharSequence p1, boolean p2, boolean p3) {
        return endWith(p0, p1, p2);
    }

    public static boolean isEmpty(CharSequence p0) {
        return p0 == null || p0.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence p0) {
        return !isEmpty(p0);
    }

    public static boolean startWith(CharSequence p0, char p1) {
        return p0 != null && p0.length() > 0 && p0.charAt(0) == p1;
    }

    public static boolean startWith(CharSequence p0, CharSequence p1) {
        return p0 != null && p1 != null && p0.toString().startsWith(p1.toString());
    }

    public static boolean startWith(CharSequence p0, CharSequence p1, boolean p2) {
        if (p0 == null || p1 == null) {
            return false;
        }
        if (p2) {
            return p0.toString().toLowerCase().startsWith(p1.toString().toLowerCase());
        }
        return startWith(p0, p1);
    }

    public static boolean startWith(CharSequence p0, CharSequence p1, boolean p2, boolean p3) {
        return startWith(p0, p1, p2);
    }

    public static boolean startWithIgnoreCase(CharSequence p0, CharSequence p1) {
        return startWith(p0, p1, true);
    }

    public static StrBuilder strBuilder(CharSequence... p0) {
        StrBuilder sb = new StrBuilder();
        if (p0 != null) {
            for (CharSequence s : p0) {
                sb.append(s);
            }
        }
        return sb;
    }

    public static StrBuilder strBuilder() {
        return new StrBuilder();
    }

    public static StrBuilder strBuilder(int p0) {
        return new StrBuilder(p0);
    }

    public static int length(CharSequence p0) {
        return p0 == null ? 0 : p0.length();
    }

    public static String format(CharSequence p0, Object... p1) {
        if (p0 == null) {
            return "null";
        }
        String str = p0.toString();
        if (p1 == null || p1.length == 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 50);
        int cursor = 0;
        for (Object param : p1) {
            int placeholderIdx = str.indexOf("{}", cursor);
            if (placeholderIdx == -1) {
                break;
            }
            sb.append(str, cursor, placeholderIdx);
            sb.append(param != null ? param.toString() : "null");
            cursor = placeholderIdx + 2;
        }
        sb.append(str, cursor, str.length());
        return sb.toString();
    }

    public static <T> String join(CharSequence p0, Iterable<T> p1) {
        if (p1 == null) {
            return null;
        }
        String conj = p0 != null ? p0.toString() : "";
        StringBuilder sb = new StringBuilder();
        Iterator<T> it = p1.iterator();
        while (it.hasNext()) {
            T item = it.next();
            if (item != null) {
                sb.append(item);
            }
            if (it.hasNext()) {
                sb.append(conj);
            }
        }
        return sb.toString();
    }

    public static String join(CharSequence p0, Object... p1) {
        if (p1 == null) {
            return null;
        }
        return join(p0, Arrays.asList(p1));
    }

    public static String removePrefix(CharSequence p0, CharSequence p1) {
        if (p0 == null) return null;
        if (p1 == null) return p0.toString();
        String s = p0.toString();
        String p = p1.toString();
        if (s.startsWith(p)) {
            return s.substring(p.length());
        }
        return s;
    }

    public static String removeSuffix(CharSequence p0, CharSequence p1) {
        if (p0 == null) return null;
        if (p1 == null) return p0.toString();
        String s = p0.toString();
        String suf = p1.toString();
        if (s.endsWith(suf)) {
            return s.substring(0, s.length() - suf.length());
        }
        return s;
    }

    public static String repeatAndJoin(CharSequence p0, int p1, CharSequence p2) {
        if (p0 == null || p1 <= 0) return "";
        List<String> list = new ArrayList<>(p1);
        for (int i = 0; i < p1; i++) {
            list.add(p0.toString());
        }
        return String.join(p2 != null ? p2.toString() : "", list);
    }

    public static String sub(CharSequence p0, int p1, int p2) {
        if (p0 == null) return null;
        int len = p0.length();
        if (p1 < 0) p1 += len;
        if (p2 < 0) p2 += len;
        if (p1 < 0) p1 = 0;
        if (p2 < 0) p2 = 0;
        if (p1 > len) p1 = len;
        if (p2 > len) p2 = len;
        if (p1 > p2) {
            int tmp = p1;
            p1 = p2;
            p2 = tmp;
        }
        return p0.toString().substring(p1, p2);
    }

    public static String subSuf(CharSequence p0, int p1) {
        if (p0 == null) return null;
        return sub(p0, p1, p0.length());
    }

    public static String unWrap(CharSequence p0, char p1) {
        return unWrap(p0, p1, p1);
    }

    public static String unWrap(CharSequence p0, char p1, char p2) {
        if (p0 == null || p0.length() < 2) return p0 != null ? p0.toString() : null;
        String s = p0.toString();
        if (s.charAt(0) == p1 && s.charAt(s.length() - 1) == p2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static String unWrap(CharSequence p0, String p1, String p2) {
        if (p0 == null) return null;
        String s = p0.toString();
        if (p1 != null && p2 != null && s.startsWith(p1) && s.endsWith(p2)) {
            return s.substring(p1.length(), s.length() - p2.length());
        }
        return s;
    }

    public static String upperFirst(CharSequence p0) {
        if (p0 == null || p0.length() == 0) return p0 != null ? p0.toString() : null;
        char c = p0.charAt(0);
        if (Character.isLowerCase(c)) {
            return Character.toUpperCase(c) + p0.toString().substring(1);
        }
        return p0.toString();
    }

    public static String upperFirstAndAddPre(CharSequence p0, String p1) {
        if (p0 == null) return null;
        return (p1 != null ? p1 : "") + upperFirst(p0);
    }

    public static String wrap(CharSequence p0, CharSequence p1) {
        return wrap(p0, p1, p1);
    }

    public static String wrap(CharSequence p0, CharSequence p1, CharSequence p2) {
        if (p0 == null) return null;
        return (p1 != null ? p1.toString() : "") + p0.toString() + (p2 != null ? p2.toString() : "");
    }

    public static String wrapIfMissing(CharSequence p0, CharSequence p1, CharSequence p2) {
        if (p0 == null) return null;
        String s = p0.toString();
        String p = p1 != null ? p1.toString() : "";
        String suf = p2 != null ? p2.toString() : "";
        if (!s.startsWith(p)) {
            s = p + s;
        }
        if (!s.endsWith(suf)) {
            s = s + suf;
        }
        return s;
    }

    public static String format(CharSequence p0, Map<?, ?> p1) {
        return format(p0, p1, true);
    }

    public static String format(CharSequence p0, Map<?, ?> p1, boolean p2) {
        if (p0 == null) {
            return "null";
        }
        String str = p0.toString();
        if (p1 == null || p1.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 50);
        int cursor = 0;
        int len = str.length();
        while (cursor < len) {
            int start = str.indexOf('{', cursor);
            if (start == -1) {
                sb.append(str, cursor, len);
                break;
            }
            int end = str.indexOf('}', start);
            if (end == -1) {
                sb.append(str, cursor, len);
                break;
            }
            sb.append(str, cursor, start);
            String key = str.substring(start + 1, end);
            Object val = p1.get(key);
            if (val != null) {
                sb.append(val);
            } else if (!p2) {
                sb.append('{').append(key).append('}');
            }
            cursor = end + 1;
        }
        return sb.toString();
    }

    public static String[] split(CharSequence p0, int p1) {
        if (p0 == null) return null;
        if (p1 <= 0) return new String[]{p0.toString()};
        String str = p0.toString();
        int len = str.length();
        int chunks = (len + p1 - 1) / p1;
        String[] result = new String[chunks];
        for (int i = 0; i < chunks; i++) {
            int start = i * p1;
            int end = Math.min(start + p1, len);
            result[i] = str.substring(start, end);
        }
        return result;
    }

    public static List<String> split(CharSequence p0, char p1) {
        if (p0 == null) return Collections.emptyList();
        return split(p0, p1, 0, false, false);
    }

    public static List<String> split(CharSequence p0, char p1, boolean p2, boolean p3) {
        return split(p0, p1, 0, p2, p3);
    }

    public static List<String> split(CharSequence p0, char p1, int p2) {
        return split(p0, p1, p2, false, false);
    }

    public static List<String> split(CharSequence p0, char p1, int p2, boolean p3, boolean p4) {
        if (p0 == null) {
            return Collections.emptyList();
        }
        String str = p0.toString();
        String regex = String.valueOf(p1);
        if (".*+?^${}()|[]\\".indexOf(p1) >= 0) {
            regex = "\\" + p1;
        }
        String[] parts = str.split(regex, p2 > 0 ? p2 : -1);
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            if (p3) {
                part = part.trim();
            }
            if (p4 && part.isEmpty()) {
                continue;
            }
            list.add(part);
        }
        return list;
    }

    public static <R> List<R> split(CharSequence p0, char p1, int p2, boolean p3, Function<String, R> p4) {
        List<String> raw = split(p0, p1, p2, p3, false);
        List<R> list = new ArrayList<>();
        for (String s : raw) {
            list.add(p4.apply(s));
        }
        return list;
    }

    public static List<String> split(CharSequence p0, CharSequence p1) {
        if (p0 == null) return Collections.emptyList();
        String str = p0.toString();
        String sep = p1 != null ? p1.toString() : "";
        if (sep.isEmpty()) {
            return Collections.singletonList(str);
        }
        return split(p0, sep.charAt(0));
    }

    public static List<String> split(CharSequence p0, CharSequence p1, boolean p2, boolean p3) {
        if (p0 == null) return Collections.emptyList();
        String sep = p1 != null ? p1.toString() : "";
        if (sep.isEmpty()) {
            return Collections.singletonList(p0.toString());
        }
        return split(p0, sep.charAt(0), 0, p2, p3);
    }

    public static List<String> split(CharSequence p0, CharSequence p1, int p2, boolean p3, boolean p4) {
        if (p0 == null) return Collections.emptyList();
        String sep = p1 != null ? p1.toString() : "";
        if (sep.isEmpty()) {
            return Collections.singletonList(p0.toString());
        }
        return split(p0, sep.charAt(0), p2, p3, p4);
    }
}
