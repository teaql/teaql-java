package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import java.util.Objects;

/** Both current host visibility and the additional Blob policy must allow each target. */
public final class BlobReadAuthorization {
    @FunctionalInterface public interface FieldPermission { boolean canRead(UserContext context,BlobFieldTarget target); }
    @FunctionalInterface public interface AccessPolicy { boolean canRead(UserContext context,BlobFieldTarget target,BlobCatalogRecord blob); }
    private final BlobBindingAuthorization.ScopeProvider scopes;
    private final FieldPermission fields;
    private final AccessPolicy policy;
    public BlobReadAuthorization(BlobBindingAuthorization.ScopeProvider scopes,FieldPermission fields,AccessPolicy policy) {
        this.scopes=Objects.requireNonNull(scopes); this.fields=Objects.requireNonNull(fields); this.policy=Objects.requireNonNull(policy);
    }
    public BlobScope scope(UserContext context) {
        BlobScope scope=scopes.scope(Objects.requireNonNull(context));
        if (scope==null) throw new BlobException(BlobException.Code.BLOB_READ_DENIED);
        return scope;
    }
    public void requireField(UserContext context,BlobFieldTarget target) {
        if (!fields.canRead(context,target)) throw new BlobException(BlobException.Code.BLOB_READ_DENIED);
    }
    public void requireRead(UserContext context,BlobFieldTarget target,BlobCatalogRecord blob) {
        requireField(context,target);
        if (!scope(context).equals(blob.scope()) || !policy.canRead(context,target,blob)) throw new BlobException(BlobException.Code.BLOB_READ_DENIED);
    }
}
