package io.teaql.core.value;

import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.checker.JsonFieldNamingProfile;
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
        return $evaluate("eval()");
    }

    default T resolve() {
        return $evaluate("resolve()");
    }

    /** Resolves only already-loaded values; diagnostic rendering performs no I/O. */
    default T $evaluate(String terminal) {
        try { return eval($getRoot()); }
        catch (TeaQLNotLoadedException error) { throw error.withAttempt($trace().operation(terminal)); }
    }

    default ExpressionTrace $trace() { return ExpressionTrace.root("E.entity(<root>)"); }

    default String $path() { return ""; }

    /** Canonical model location; null denotes the entity root. */
    default ObjectLocation $location() { return null; }

    default JsonFieldNamingProfile $jsonFieldNaming() { return JsonFieldNamingProfile.CAMEL_CASE; }

    default T orIfNull(T defaultValue) {
        T value = $evaluate("orIfNull(<fallback>)");
        if(value == null){
            return defaultValue;
        }
        return value;
    }
    default T orElseThrow() {
        T value = $evaluate("orElseThrow()");
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    default <X> T orElseThrow(Supplier<? extends Throwable> exceptionSupplier)
            throws Throwable{

        T value = $evaluate("orElseThrow(<supplier>)");
        if(value == null){
            throw exceptionSupplier.get();
        }
        return value;

    }

    default boolean isNull() {
        return null == $evaluate("isNull()");
    }

    default boolean isNotNull() {
        return null != $evaluate("isNotNull()");
    }

    default boolean isEmpty() {
        return io.teaql.core.utils.ObjectUtil.isEmpty($evaluate("isEmpty()"));
    }

    default boolean isNotEmpty() {
        return io.teaql.core.utils.ObjectUtil.isNotEmpty($evaluate("isNotEmpty()"));
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
