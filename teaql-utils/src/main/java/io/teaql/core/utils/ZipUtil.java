package io.teaql.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtil {

    public static byte[] gzip(byte[] p0) {
        if (p0 == null) throw new NullPointerException("bytes cannot be null");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(p0.length);
            try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
                gzos.write(p0);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Gzip failed", e);
        }
    }

    public static byte[] gzip(java.io.File p0) {
        if (p0 == null) throw new NullPointerException("file cannot be null");
        try {
            return gzip(Files.readAllBytes(p0.toPath()));
        } catch (Exception e) {
            throw new RuntimeException("Gzip file failed", e);
        }
    }

    public static byte[] gzip(java.io.InputStream p0) {
        if (p0 == null) throw new NullPointerException("stream cannot be null");
        try {
            return gzip(IoUtil.readBytes(p0));
        } catch (Exception e) {
            throw new RuntimeException("Gzip stream failed", e);
        }
    }

    public static byte[] gzip(java.io.InputStream p0, int p1) {
        return gzip(p0);
    }

    public static byte[] gzip(java.lang.String p0, java.lang.String p1) {
        if (p0 == null) throw new NullPointerException("content cannot be null");
        try {
            return gzip(p0.getBytes(p1));
        } catch (Exception e) {
            throw new RuntimeException("Gzip string failed", e);
        }
    }

    public static byte[] unGzip(byte[] p0) {
        if (p0 == null) throw new NullPointerException("bytes cannot be null");
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(p0);
            return unGzip(bis);
        } catch (Exception e) {
            throw new RuntimeException("Ungzip failed", e);
        }
    }

    public static byte[] unGzip(java.io.InputStream p0) {
        if (p0 == null) throw new NullPointerException("stream cannot be null");
        try (GZIPInputStream gzis = new GZIPInputStream(p0)) {
            return IoUtil.readBytes(gzis);
        } catch (Exception e) {
            throw new RuntimeException("Ungzip failed", e);
        }
    }

    public static byte[] unGzip(java.io.InputStream p0, int p1) {
        return unGzip(p0);
    }

    public static java.lang.String unGzip(byte[] p0, java.lang.String p1) {
        if (p0 == null) throw new NullPointerException("bytes cannot be null");
        try {
            return new java.lang.String(unGzip(p0), p1);
        } catch (Exception e) {
            throw new RuntimeException("Ungzip failed", e);
        }
    }

}
