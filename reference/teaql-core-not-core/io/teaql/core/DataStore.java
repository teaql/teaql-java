package io.teaql.core;

import java.util.function.Supplier;

/**
 * data store across context take redis as an example
 */
public interface DataStore {
    void put(String key, Object object);

    /**
     * @param timeout timeout in seconds
     */
    void put(String key, Object object, long timeout);

    <T> T get(String key);

    <T> T getAndRemove(String key);

    <T> T get(String key, Supplier<T> supplier);

    void remove(String key);

    boolean containsKey(String key);
}
