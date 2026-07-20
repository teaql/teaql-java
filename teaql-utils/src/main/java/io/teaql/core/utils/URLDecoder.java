package io.teaql.core.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class URLDecoder {

    public static byte[] decode(byte[] p0) {
        return decode(p0, false);
    }

    public static byte[] decode(byte[] p0, boolean p1) {
        if (p0 == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(p0.length);
        for (int i = 0; i < p0.length; i++) {
            int c = p0[i];
            if (c == '+') {
                out.write(p1 ? ' ' : '+');
                continue;
            }
            if (c == '%') {
                if (i + 2 >= p0.length) {
                    out.write(c);
                    continue;
                }
                int d1 = Character.digit((char) p0[i + 1], 16);
                int d2 = Character.digit((char) p0[i + 2], 16);
                if (d1 < 0 || d2 < 0) {
                    out.write(c);
                    continue;
                }
                out.write((d1 << 4) + d2);
                i += 2;
                continue;
            }
            out.write(c);
        }
        return out.toByteArray();
    }

    public static java.lang.String decode(java.lang.String p0, java.nio.charset.Charset p1) {
        return decode(p0, p1, false);
    }

    public static java.lang.String decode(java.lang.String p0, java.nio.charset.Charset p1, boolean p2) {
        if (p0 == null) {
            return null;
        }
        try {
            String input = p2 ? p0.replace("+", "%2B") : p0;
            return java.net.URLDecoder.decode(input, p1);
        } catch (Exception e) {
            return p0;
        }
    }

}
