package io.teaql.core.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    public static java.lang.String readUtf8Str(java.lang.String p0) {
        if (p0 == null) {
            throw new RuntimeException("Resource path cannot be null");
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ResourceUtil.class.getClassLoader();
        }
        try (InputStream in = classLoader.getResourceAsStream(p0)) {
            if (in == null) {
                throw new RuntimeException("Resource not found: " + p0);
            }
            return new String(IoUtil.readBytes(in), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Read resource failed: " + p0, e);
        }
    }

}
