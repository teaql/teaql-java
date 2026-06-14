package io.teaql.core.utils;

public class NamingCase {

    public static java.lang.String toCamelCase(java.lang.CharSequence p0) {
        return toCamelCase(p0, '_');
    }

    public static java.lang.String toCamelCase(java.lang.CharSequence p0, char p1) {
        if (p0 == null) {
            return null;
        }
        String str = p0.toString();
        if (str.isEmpty()) {
            return "";
        }
        if (str.indexOf(p1) == -1) {
            if (Character.isUpperCase(str.charAt(0))) {
                return Character.toLowerCase(str.charAt(0)) + str.substring(1);
            }
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        boolean upper = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == p1) {
                upper = true;
            } else {
                if (upper) {
                    sb.append(Character.toUpperCase(c));
                    upper = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        if (sb.length() > 0) {
            char first = sb.charAt(0);
            sb.setCharAt(0, Character.toLowerCase(first));
        }
        return sb.toString();
    }

    public static java.lang.String toPascalCase(java.lang.CharSequence p0) {
        if (p0 == null) {
            return null;
        }
        String camel = toCamelCase(p0, '_');
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    public static java.lang.String toUnderlineCase(java.lang.CharSequence p0) {
        if (p0 == null) {
            return null;
        }
        String str = p0.toString();
        if (str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() + 4);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && str.charAt(i - 1) != '_') {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
