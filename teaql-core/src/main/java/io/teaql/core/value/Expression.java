package io.teaql.core.value;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Expression<E, T> {
    T eval(E e);

    default <U> Expression<E, U> apply(Function<T, U> function) {
        return new ExpressionAdaptor(this, function);
    }

    default E $getRoot() {
        return null;
    }

    default T eval() {
        return eval($getRoot());
    }

    default T resolve() {
        return eval();
    }

    default T orElse(T defaultValue) {
        T value = resolve();
        if(io.teaql.core.utils.ObjectUtil.isEmpty(value)){
            return defaultValue;
        }
        return value;
    }
    default T orElseThrow() {
        T value = resolve();
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    default <X> T orElseThrow(Supplier<? extends Throwable> exceptionSupplier)
            throws Throwable{

        T value = resolve();
        if(io.teaql.core.utils.ObjectUtil.isEmpty(value)){
            throw exceptionSupplier.get();
        }
        return value;

    }

    default boolean isNull() {
        return null == resolve();
    }

    default boolean isNotNull() {
        return null != resolve();
    }

    default boolean isEmpty() {
        return io.teaql.core.utils.ObjectUtil.isEmpty(resolve());
    }

    default boolean isNotEmpty() {
        return io.teaql.core.utils.ObjectUtil.isNotEmpty(resolve());
    }

    default void whenIsNull(Runnable function) {
        if (isNull() && function != null) {
            function.run();
        }
    }

    default void whenIsNotNull(Runnable function) {
        if (isNotNull() && function != null) {
            function.run();
        }
    }

    default void whenIsNotNull(Consumer<T> consumer) {
        if (isNotNull() && consumer != null) {
            consumer.accept(resolve());
        }
    }

    default void whenIsEmpty(Runnable function) {
        if (isEmpty() && function != null) {
            function.run();
        }
    }

    default void whenNotEmpty(Consumer<T> consumer) {
        if (isNotEmpty() && consumer != null) {
            consumer.accept(resolve());
        }
    }

    default void whenNotEmpty(Runnable function) {
        if (isNotEmpty() && function != null) {
            function.run();
        }
    }
}
