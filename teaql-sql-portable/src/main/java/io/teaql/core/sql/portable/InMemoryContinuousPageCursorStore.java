package io.teaql.core.sql.portable;

import io.teaql.core.ContinuousPageCursor;
import io.teaql.core.ContinuousPageCursorStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded process-local default. Applications can inject a distributed store. */
public final class InMemoryContinuousPageCursorStore implements ContinuousPageCursorStore {
    private static final int DEFAULT_MAX_ENTRIES = 4096;
    private final ConcurrentHashMap<String, ContinuousPageCursor> cursors = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryContinuousPageCursorStore() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public InMemoryContinuousPageCursorStore(int maxEntries) {
        if (maxEntries <= 0) throw new IllegalArgumentException("maxEntries must be positive");
        this.maxEntries = maxEntries;
    }

    @Override
    public Optional<ContinuousPageCursor> get(String queryKey, long targetOffset) {
        String key = checkpointKey(queryKey, targetOffset);
        ContinuousPageCursor cursor = cursors.get(key);
        if (cursor != null && cursor.expired(Instant.now())) {
            cursors.remove(key, cursor);
            return Optional.empty();
        }
        return Optional.ofNullable(cursor);
    }

    @Override
    public void put(String queryKey, ContinuousPageCursor cursor) {
        if (cursors.size() >= maxEntries) {
            cursors.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().lastUsedAt()))
                    .ifPresent(e -> cursors.remove(e.getKey(), e.getValue()));
        }
        cursors.put(checkpointKey(queryKey, cursor.nextOffset()), cursor);
    }

    @Override
    public void invalidate(String queryKey) {
        String prefix = queryKey + ':';
        cursors.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static String checkpointKey(String queryKey, long offset) {
        return queryKey + ':' + offset;
    }
}
