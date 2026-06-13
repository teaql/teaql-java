package io.teaql.core.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Base64 {

    public static byte[] decode(byte[] p0) {
        if (p0 == null) return null;
        return java.util.Base64.getDecoder().decode(p0);
    }

    public static byte[] decode(java.lang.CharSequence p0) {
        if (p0 == null) return null;
        return java.util.Base64.getDecoder().decode(p0.toString());
    }

    public static byte[] encode(byte[] p0, boolean p1) {
        if (p0 == null) throw new NullPointerException("bytes cannot be null");
        return p1 ? java.util.Base64.getMimeEncoder().encode(p0) : java.util.Base64.getEncoder().encode(p0);
    }

    public static byte[] encode(byte[] p0, boolean p1, boolean p2) {
        if (p0 == null) throw new NullPointerException("bytes cannot be null");
        if (p2) {
            return java.util.Base64.getUrlEncoder().encode(p0);
        }
        return p1 ? java.util.Base64.getMimeEncoder().encode(p0) : java.util.Base64.getEncoder().encode(p0);
    }

    public static java.lang.String decodeStr(java.lang.CharSequence p0) {
        if (p0 == null) return null;
        return new String(decode(p0), StandardCharsets.UTF_8);
    }

    public static java.lang.String decodeStr(java.lang.CharSequence p0, java.lang.String p1) {
        if (p0 == null) return null;
        try {
            return new String(decode(p0), p1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static java.lang.String decodeStr(java.lang.CharSequence p0, java.nio.charset.Charset p1) {
        if (p0 == null) return null;
        return new String(decode(p0), p1);
    }

    public static java.lang.String encode(byte[] p0) {
        if (p0 == null) throw new NullPointerException("bytes cannot be null");
        return java.util.Base64.getEncoder().encodeToString(p0);
    }

    public static java.lang.String encode(java.io.File p0) {
        if (p0 == null) throw new NullPointerException("file cannot be null");
        try {
            byte[] bytes = Files.readAllBytes(p0.toPath());
            return encode(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static java.lang.String encode(java.io.InputStream p0) {
        if (p0 == null) throw new NullPointerException("stream cannot be null");
        try {
            byte[] bytes = IoUtil.readBytes(p0);
            return encode(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static java.lang.String encode(java.lang.CharSequence p0) {
        if (p0 == null) throw new NullPointerException("string cannot be null");
        return encode(p0.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static java.lang.String encode(java.lang.CharSequence p0, java.lang.String p1) {
        if (p0 == null) throw new NullPointerException("string cannot be null");
        try {
            return encode(p0.toString().getBytes(p1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static java.lang.String encode(java.lang.CharSequence p0, java.nio.charset.Charset p1) {
        if (p0 == null) throw new NullPointerException("string cannot be null");
        return encode(p0.toString().getBytes(p1));
    }

}
