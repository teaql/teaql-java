package io.teaql.core.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {

    public static java.util.stream.Stream<java.lang.String> of(java.io.File p0) {
        return of(p0, StandardCharsets.UTF_8);
    }

    public static java.util.stream.Stream<java.lang.String> of(java.io.File p0, java.nio.charset.Charset p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        return of(p0.toPath(), p1);
    }

    public static <T> java.util.stream.Stream<T> of(java.lang.Iterable<T> p0) {
        return of(p0, false);
    }

    public static <T> java.util.stream.Stream<T> of(java.lang.Iterable<T> p0, boolean p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("Iterable cannot be null");
        }
        return StreamSupport.stream(p0.spliterator(), p1);
    }

    public static <T> java.util.stream.Stream<T> of(T p0, java.util.function.UnaryOperator<T> p1, int p2) {
        return Stream.iterate(p0, p1).limit(p2);
    }

    public static <T> java.util.stream.Stream<T> of(T... p0) {
        if (p0 == null) {
            return Stream.empty();
        }
        return Stream.of(p0);
    }

    public static java.util.stream.Stream<java.lang.String> of(java.nio.file.Path p0) {
        return of(p0, StandardCharsets.UTF_8);
    }

    public static java.util.stream.Stream<java.lang.String> of(java.nio.file.Path p0, java.nio.charset.Charset p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        try {
            return Files.lines(p0, p1);
        } catch (IOException e) {
            throw new RuntimeException("Read lines failed", e);
        }
    }

    public static <T> java.util.stream.Stream<T> of(java.util.Iterator<T> p0) {
        return of(p0, false);
    }

    public static <T> java.util.stream.Stream<T> of(java.util.Iterator<T> p0, boolean p1) {
        if (p0 == null) {
            throw new IllegalArgumentException("Iterator cannot be null");
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(p0, Spliterator.ORDERED), p1);
    }

}
