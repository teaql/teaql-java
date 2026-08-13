package io.teaql.core;

import java.util.Optional;

/**
 * Application-replaceable store for continuous-page checkpoints. Implementations
 * must enforce TTL and bounded capacity; a miss must be safe and inexpensive.
 */
public interface ContinuousPageCursorStore {
    Optional<ContinuousPageCursor> get(String queryKey, long targetOffset);

    void put(String queryKey, ContinuousPageCursor cursor);

    void invalidate(String queryKey);
}
