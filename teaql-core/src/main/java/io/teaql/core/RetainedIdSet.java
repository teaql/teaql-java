package io.teaql.core;

import java.time.Instant;

/** Compact immutable snapshot retained by an {@link IdSetStore}. */
public record RetainedIdSet(String queryKey, long[] ids, Instant expiresAt) {
    public RetainedIdSet {
        if (queryKey == null || queryKey.isBlank()) throw new IllegalArgumentException("queryKey is required");
        ids = ids == null ? new long[0] : ids.clone();
        if (expiresAt == null) throw new IllegalArgumentException("expiresAt is required");
    }

    @Override public long[] ids() { return ids.clone(); }
    public int size() { return ids.length; }
    public long byteSize() { return (long) ids.length * Long.BYTES; }
    public boolean expired(Instant now) { return !expiresAt.isAfter(now); }
}
