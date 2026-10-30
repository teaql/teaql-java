package io.teaql.core.blob;

import io.teaql.core.FrameworkInternal;
import java.util.Objects;

/** Synchronous framework lease. Retains no actor or command payload and closes before commit delivery. */
public final class BlobCommandScope implements AutoCloseable {
    private static final ThreadLocal<BlobCommandScope> CURRENT=new ThreadLocal<>();
    private final Object resource,owner;
    private boolean closed;
    private BlobCommandScope(Object resource,Object owner) { this.resource=resource; this.owner=owner; }
    @FrameworkInternal("Command owner resource constraint; not a workspace credential")
    public static void requireAbsent() { if (CURRENT.get()!=null) throw mismatch(); }
    @FrameworkInternal("Command owner resource constraint; not a workspace credential")
    public static BlobCommandScope open(Object resource,Object owner) {
        requireAbsent(); var value=new BlobCommandScope(Objects.requireNonNull(resource),Objects.requireNonNull(owner)); CURRENT.set(value); return value;
    }
    @FrameworkInternal("Command owner resource constraint; not a workspace credential")
    public static void requireResource(Object resource) { var value=CURRENT.get(); if (value!=null && value.resource!=resource) throw mismatch(); }
    @FrameworkInternal("Command owner resource constraint; not a workspace credential")
    public static void requireOwner(Object resource,Object owner) {
        var value=CURRENT.get(); if (value!=null && (value.resource!=resource || value.owner!=owner)) throw mismatch();
    }
    @FrameworkInternal("Close only on the synchronous owning thread")
    @Override public void close() {
        if (closed) return;
        if (CURRENT.get()!=this) throw mismatch();
        CURRENT.remove(); closed=true;
    }
    private static BlobException mismatch() { return new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED); }
}
