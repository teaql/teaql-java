package io.teaql.core.utils;

@FunctionalInterface
public interface Filter<T> {
    boolean accept(T t);
}
