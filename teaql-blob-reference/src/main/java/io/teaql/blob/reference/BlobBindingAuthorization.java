package io.teaql.blob.reference;

import io.teaql.core.MutationOperationKind;
import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import java.util.Objects;

/** Required host field permission intersected with a separate, additional Blob binding policy. */
public final class BlobBindingAuthorization {
    @FunctionalInterface public interface ScopeProvider { BlobScope scope(UserContext context); }
    @FunctionalInterface public interface FieldPermission {
        boolean canWrite(UserContext context, BlobFieldTarget target, MutationOperationKind kind);
    }
    @FunctionalInterface public interface AccessPolicy {
        boolean canBind(UserContext context, BlobFieldTarget target, BlobCatalogRecord blob);
    }
    private final ScopeProvider scopes;
    private final FieldPermission fields;
    private final AccessPolicy policy;
    public BlobBindingAuthorization(ScopeProvider scopes, FieldPermission fields, AccessPolicy policy) {
        this.scopes = Objects.requireNonNull(scopes); this.fields = Objects.requireNonNull(fields);
        this.policy = Objects.requireNonNull(policy);
    }
    public BlobScope scope(UserContext context) {
        BlobScope scope = scopes.scope(Objects.requireNonNull(context));
        if (scope == null) throw new BlobException(BlobException.Code.BLOB_BIND_DENIED);
        return scope;
    }
    public void requireWrite(UserContext context, BlobFieldTarget target, MutationOperationKind kind) {
        if (!fields.canWrite(context, target, kind)) throw new BlobException(BlobException.Code.BLOB_BIND_DENIED);
    }
    public void requireBind(UserContext context, BlobFieldTarget target, BlobCatalogRecord blob) {
        if (!scope(context).equals(blob.scope()) || !policy.canBind(context, target, blob)) {
            throw new BlobException(BlobException.Code.BLOB_BIND_DENIED);
        }
    }
}
