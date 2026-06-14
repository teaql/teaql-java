package io.teaql.core.utils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class URLEncodeUtil {

    public static java.lang.String encode(java.lang.String p0) {
        return encode(p0, StandardCharsets.UTF_8);
    }

    public static java.lang.String encode(java.lang.String p0, java.nio.charset.Charset p1) {
        if (p0 == null) {
            return null;
        }
        return URLEncoder.encode(p0, p1);
    }

}
