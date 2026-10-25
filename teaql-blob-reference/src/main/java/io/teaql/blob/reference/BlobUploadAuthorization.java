package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import java.util.Objects;

/** Current host field permission intersected with an additional upload policy at every stage. */
public final class BlobUploadAuthorization {
    public enum Action { BEGIN, WRITE, CONFIRM }
    @FunctionalInterface public interface FieldPermission { boolean canUpload(UserContext context,BlobFieldTarget target); }
    @FunctionalInterface public interface AccessPolicy {
        boolean canUpload(UserContext context,BlobFieldTarget target,Action action,BlobUploadService.FileInfo facts);
    }
    private final BlobBindingAuthorization.ScopeProvider scopes;
    private final FieldPermission fields;
    private final AccessPolicy policy;
    public BlobUploadAuthorization(BlobBindingAuthorization.ScopeProvider scopes,FieldPermission fields,AccessPolicy policy) {
        this.scopes=Objects.requireNonNull(scopes); this.fields=Objects.requireNonNull(fields); this.policy=Objects.requireNonNull(policy);
    }
    public BlobScope scope(UserContext context) {
        BlobScope scope=scopes.scope(Objects.requireNonNull(context)); if (scope==null) throw new BlobException(BlobException.Code.BLOB_BIND_DENIED); return scope;
    }
    public void require(UserContext context,BlobFieldTarget target,Action action,BlobUploadService.FileInfo facts) {
        if (!fields.canUpload(context,target) || !policy.canUpload(context,target,action,facts)) throw new BlobException(BlobException.Code.BLOB_BIND_DENIED);
    }
}
