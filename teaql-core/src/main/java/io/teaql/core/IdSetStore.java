package io.teaql.core;

import java.util.Optional;

/** Modular retention boundary; failures must degrade to ordinary pagination. */
public interface IdSetStore {
    Optional<RetainedIdSet> get(String queryKey);
    void put(RetainedIdSet idSet);
    void invalidate(String queryKey);
}
