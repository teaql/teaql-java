package io.teaql.core.blob;

import io.teaql.core.DataServiceRegistry;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Shared services/configuration only. Every operation obtains identity from its current context. */
public interface BlobRuntime {
    Uploads upload();
    BlobMutationParticipant mutation();
    Reads reads();
    Maintenance maintenance();
    default java.util.Optional<BlobCommandService> commands() { return java.util.Optional.empty(); }

    /** Pure configuration/route checks, safe during a caller-owned transaction. */
    void validateConfiguration(EntityMetaFactory metadata, DataServiceRegistry registry);
    /** Explicit read-only infrastructure check. Never provisions schema, buckets or workers. */
    void validateReadiness();

    interface Uploads {
        /** Declared facts are hints; confirmation must verify actual bytes. */
        record FileInfo(String fileName, Long declaredSize, String declaredContentType) {
            public FileInfo {
                if (fileName==null || fileName.isBlank() || fileName.indexOf('\0')>=0 || declaredSize!=null && declaredSize<0
                        || declaredContentType!=null && declaredContentType.length()>160) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
            }
            @Override public String toString() { return "BlobFileInfo[redacted]"; }
        }
        record Session(String uploadId, BlobRef reference, Instant uploadExpiresAt, URI directUploadUrl, Instant directUrlExpiresAt) {
            @Override public String toString() { return "BlobUploadSession[redacted]"; }
        }
        record WriteReceipt(String uploadId, long sizeBytes) {
            @Override public String toString() { return "BlobWriteReceipt[sizeBytes="+sizeBytes+"]"; }
        }
        Session begin(UserContext context, BlobFieldTarget target, FileInfo info, String idempotencyKey);
        WriteReceipt write(UserContext context, String uploadId, InputStream source);
        BlobRef complete(UserContext context, String uploadId);
    }

    interface Reads {
        int MAX_BATCH=256;
        /** A currently authorized, visible and loaded field, not a bearer Key lookup. */
        record Request(BlobFieldTarget target, BlobRef reference) {
            public Request {
                Objects.requireNonNull(target); Objects.requireNonNull(reference);
                if (target.entityId()==null) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
            }
            @Override public String toString() { return "BlobReadRequest[redacted]"; }
        }
        record Options(Duration lifetime, Instant documentExpiresAt, boolean requireContextCheckedRead) {
            public Options {
                if (lifetime!=null && (lifetime.isZero() || lifetime.isNegative() || lifetime.getNano()!=0)) {
                    throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
                }
            }
            public static Options defaults() { return new Options(null,null,false); }
        }
        record Read(BlobRef reference, URI url, Instant expiresAt, String originalName, String contentType,
                    long sizeBytes, String cachePolicy, BlobPolicyProfile.ResponseShape responseShape) {
            public Read {
                Objects.requireNonNull(reference); Objects.requireNonNull(expiresAt);
                new BlobStore.SignedRead(url,expiresAt);
                Objects.requireNonNull(originalName); Objects.requireNonNull(contentType); Objects.requireNonNull(cachePolicy); Objects.requireNonNull(responseShape);
                if (sizeBytes<0) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
            }
            @Override public String toString() { return "BlobRead[redacted]"; }
        }
        /** Preserves input order. No entity/ledger mutation or business-write replay. */
        List<Read> readBatch(UserContext context, List<Request> requests, Options options);
    }

    interface Maintenance {
        record Cursor(BlobScope scope, BlobRef after) {
            public Cursor { Objects.requireNonNull(scope); Objects.requireNonNull(after); }
            @Override public String toString() { return "BlobCollectionCursor[redacted]"; }
        }
        enum Status { DELETED, ALREADY_MISSING, NOT_ELIGIBLE, VERSION_CONFLICT, FAILED }
        record Outcome(BlobRef reference, Status status, BlobException.Code error) {
            public Outcome { Objects.requireNonNull(reference); Objects.requireNonNull(status); }
            @Override public String toString() { return "BlobCollectionOutcome[status="+status+",error="+error+"]"; }
        }
        record Batch(List<Outcome> outcomes, Cursor next) {
            public Batch { outcomes=List.copyOf(outcomes); }
        }
        default Batch runBatch(UserContext context) { return runBatch(context,null); }
        Batch runBatch(UserContext context, Cursor cursor);
    }
}
