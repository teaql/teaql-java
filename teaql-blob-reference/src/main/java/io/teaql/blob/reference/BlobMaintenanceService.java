package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import java.time.Clock;
import java.util.*;
import static io.teaql.core.blob.BlobException.Code.*;

/** Explicit host-scheduled collection. No installation I/O, cached actor, timer or worker. */
public final class BlobMaintenanceService {
    @FunctionalInterface public interface Permission {
        boolean canMaintain(UserContext context,BlobScope scope);
    }
    public record Cursor(BlobScope scope,BlobRef after) {
        public Cursor { Objects.requireNonNull(scope); Objects.requireNonNull(after); }
        @Override public String toString() { return "BlobCollectionCursor[redacted]"; }
    }
    public enum Status { DELETED, ALREADY_MISSING, NOT_ELIGIBLE, VERSION_CONFLICT, FAILED }
    public record Outcome(BlobRef reference,Status status,BlobException.Code error) {
        public Outcome { Objects.requireNonNull(reference); Objects.requireNonNull(status); }
        @Override public String toString() { return "BlobCollectionOutcome[status="+status+",error="+error+"]"; }
    }
    public record Batch(List<Outcome> outcomes,Cursor next) {
        public Batch { outcomes=List.copyOf(outcomes); }
    }
    private final PostgresBlobCatalog catalog;
    private final BlobStore store;
    private final BlobBindingAuthorization.ScopeProvider scopes;
    private final Permission permission;
    private final Clock clock;
    private final int batchSize;
    public BlobMaintenanceService(PostgresBlobCatalog catalog,BlobStore store,
            BlobBindingAuthorization.ScopeProvider scopes,Permission permission,Clock clock) {
        this(catalog,store,scopes,permission,clock,100);
    }
    public BlobMaintenanceService(PostgresBlobCatalog catalog,BlobStore store,
            BlobBindingAuthorization.ScopeProvider scopes,Permission permission,Clock clock,int batchSize) {
        this.catalog=Objects.requireNonNull(catalog); this.store=Objects.requireNonNull(store);
        this.scopes=Objects.requireNonNull(scopes); this.permission=Objects.requireNonNull(permission); this.clock=Objects.requireNonNull(clock);
        if (batchSize<1 || batchSize>1000) throw new BlobException(BLOB_POLICY_INVALID);
        this.batchSize=batchSize;
    }
    public Batch runBatch(UserContext context) { return runBatch(context,null); }
    public Batch runBatch(UserContext context,Cursor cursor) {
        BlobScope scope=authorize(context,null);
        if (cursor!=null && !cursor.scope().equals(scope)) throw new BlobException(BLOB_MAINTENANCE_DENIED);
        if (!store.capabilities().versionConditionalDelete()) throw new BlobException(BLOB_CAPABILITY_MISSING);
        var candidates=catalog.deletionCandidates(scope,cursor==null?null:cursor.after(),clock.instant(),batchSize);
        List<Outcome> outcomes=new ArrayList<>(candidates.size());
        for (BlobCatalogRecord candidate:candidates) {
            try { outcomes.add(collect(context,scope,candidate)); }
            catch (BlobException failed) {
                if (failed.code()==BLOB_MAINTENANCE_DENIED || failed.code()==BLOB_TRANSACTION_REQUIRED) throw failed;
                outcomes.add(new Outcome(candidate.reference(),failed.code()==BLOB_IDEMPOTENCY_CONFLICT?Status.VERSION_CONFLICT:Status.FAILED,failed.code()));
            } catch (RuntimeException unavailable) {
                outcomes.add(new Outcome(candidate.reference(),Status.FAILED,BLOB_STORE_FAILED));
            }
        }
        Cursor next=candidates.size()==batchSize?new Cursor(scope,candidates.get(candidates.size()-1).reference()):null;
        return new Batch(outcomes,next);
    }
    private Outcome collect(UserContext context,BlobScope scope,BlobCatalogRecord candidate) {
        authorize(context,scope);
        // Direct-upload providers need their own proven quiescence/fencing contract before collection.
        if (candidate.policy().uploadMode()!=BlobPolicyProfile.UploadMode.SERVER_STREAM) throw new BlobException(BLOB_CAPABILITY_MISSING);
        var claim=catalog.claimDeletion(scope,candidate.reference(),clock.instant());
        if (claim.isEmpty()) return new Outcome(candidate.reference(),Status.NOT_ELIGIBLE,null);
        BlobCatalogRecord blob=claim.orElseThrow();
        authorize(context,scope);
        var observed=store.sealForDeletion(context,blob.storageLocation(),blob.contentVersion());
        authorize(context,scope);
        if (blob.contentVersion()==null) {
            if (observed.isEmpty()) {
                catalog.finishDeletion(scope,blob.reference(),null,clock.instant());
                return new Outcome(blob.reference(),Status.ALREADY_MISSING,null);
            }
            blob=catalog.captureDeletionContent(scope,blob.reference(),observed.orElseThrow());
            if (blob.state()==BlobState.DELETED) return new Outcome(blob.reference(),Status.NOT_ELIGIBLE,null);
        }
        authorize(context,scope);
        var deleted=store.deleteIfVersion(context,blob.storageLocation(),blob.contentVersion());
        if (deleted==BlobStore.DeleteResult.VERSION_MISMATCH) return new Outcome(blob.reference(),Status.VERSION_CONFLICT,BLOB_IDEMPOTENCY_CONFLICT);
        if (deleted==null) throw new BlobException(BLOB_STORE_FAILED);
        authorize(context,scope);
        catalog.finishDeletion(scope,blob.reference(),blob.contentVersion(),clock.instant());
        return new Outcome(blob.reference(),deleted==BlobStore.DeleteResult.DELETED?Status.DELETED:Status.ALREADY_MISSING,null);
    }
    private BlobScope authorize(UserContext context,BlobScope expected) {
        Objects.requireNonNull(context); BlobScope scope;
        try {
            scope=scopes.scope(context);
            if (scope==null || expected!=null && !expected.equals(scope) || !permission.canMaintain(context,scope)) {
                throw new BlobException(BLOB_MAINTENANCE_DENIED);
            }
        } catch (RuntimeException denied) { throw new BlobException(BLOB_MAINTENANCE_DENIED); }
        catalog.requireNoActiveTransaction();
        return scope;
    }
}
