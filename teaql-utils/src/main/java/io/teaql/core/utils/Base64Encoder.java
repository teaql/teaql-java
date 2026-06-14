package io.teaql.core.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Base64Encoder {

    public static byte[] encodeUrlSafe(byte[] p0, boolean p1) {
        if (p0 == null) return null;
        return java.util.Base64.getUrlEncoder().withoutPadding().encode(p0);
    }

    public static java.lang.String encodeUrlSafe(byte[] p0) {
        if (p0 == null) return null;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(p0);
    }

    public static java.lang.String encodeUrlSafe(java.lang.CharSequence p0) {
        if (p0 == null) return null;
        return encodeUrlSafe(p0.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static java.lang.String encodeUrlSafe(java.lang.CharSequence p0, java.nio.charset.Charset p1) {
        if (p0 == null) return null;
        return encodeUrlSafe(p0.toString().getBytes(p1));
    }

}
