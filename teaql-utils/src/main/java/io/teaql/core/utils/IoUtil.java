package io.teaql.core.utils;

import java.io.IOException;
import java.io.InputStream;

public class IoUtil {

    public static byte[] readBytes(java.io.InputStream p0) {
        return readBytes(p0, false);
    }

    public static byte[] readBytes(java.io.InputStream p0, boolean p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = p0.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Read bytes failed", e);
        } finally {
            if (p1) {
                try {
                    p0.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public static byte[] readBytes(java.io.InputStream p0, int p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        try {
            return p0.readNBytes(p1);
        } catch (IOException e) {
            throw new RuntimeException("Read bytes failed", e);
        }
    }

}
