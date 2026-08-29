package io.teaql.core.sql.portable;

import io.teaql.core.IdSetStore;
import io.teaql.core.RetainedIdSet;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdSetStore implements IdSetStore {
    private final ConcurrentHashMap<String, RetainedIdSet> sets = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final long maxBytes;

    public InMemoryIdSetStore() { this(64, 256L * 1024 * 1024); }
    public InMemoryIdSetStore(int maxEntries, long maxBytes) {
        if (maxEntries <= 0 || maxBytes <= 0) throw new IllegalArgumentException("store limits must be positive");
        this.maxEntries = maxEntries;
        this.maxBytes = maxBytes;
    }

    @Override public Optional<RetainedIdSet> get(String queryKey) {
        RetainedIdSet value = sets.get(queryKey);
        if (value != null && value.expired(Instant.now())) {
            sets.remove(queryKey, value); value = null;
        }
        return Optional.ofNullable(value);
    }

    @Override public synchronized void put(RetainedIdSet idSet) {
        if (idSet.byteSize() > maxBytes) throw new IllegalArgumentException("ID set exceeds memory ceiling");
        sets.entrySet().removeIf(entry -> entry.getValue().expired(Instant.now()));
        while (sets.size() >= maxEntries || retainedBytes() + idSet.byteSize() > maxBytes) {
            var oldest = sets.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().expiresAt()));
            if (oldest.isEmpty()) break;
            sets.remove(oldest.get().getKey());
        }
        sets.put(idSet.queryKey(), idSet);
    }

    private long retainedBytes() {
        return sets.values().stream().mapToLong(RetainedIdSet::byteSize).sum();
    }

    @Override public void invalidate(String queryKey) { sets.remove(queryKey); }
}
