package io.teaql.core.blob;

import io.teaql.core.UserContext;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Transport only. Blob services authorize the current context before calling this SPI. */
public interface BlobStore {
    record Capabilities(boolean directUpload, boolean range, boolean signedReads,
                        boolean contextCheckedReads, boolean versionConditionalDelete, Duration maximumReadTtl) {
        public Capabilities {
            Objects.requireNonNull(maximumReadTtl);
            if (maximumReadTtl.isNegative() || maximumReadTtl.getNano()!=0 || signedReads && maximumReadTtl.isZero()) {
                throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
            }
        }
    }
    record UploadTarget(String uploadId, String location, URI directUrl, Instant directExpiresAt) {
        public UploadTarget {
            text(uploadId); text(location);
            if ((directUrl==null)!=(directExpiresAt==null)) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
        }
        @Override public String toString() { return "UploadTarget[redacted]"; }
    }
    record StoredContent(String location, String contentVersion, long sizeBytes, String sha256) {
        public StoredContent {
            text(location); text(contentVersion);
            if (sizeBytes<0 || sha256!=null && !sha256.matches("[0-9a-f]{64}")) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
        }
        @Override public String toString() { return "StoredContent[redacted,size="+sizeBytes+"]"; }
    }
    record RangeContent(StoredContent content, long offset, long length, InputStream stream) implements AutoCloseable {
        public RangeContent {
            Objects.requireNonNull(content); Objects.requireNonNull(stream);
            if (offset<0 || length<0 || offset>content.sizeBytes() || length>content.sizeBytes()-offset) {
                throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
            }
        }
        @Override public void close() throws java.io.IOException { stream.close(); }
        @Override public String toString() { return "RangeContent[redacted]"; }
    }
    record ReadRequest(String location, String contentVersion, String contentType, String fileName,
                       BlobPolicyProfile.Disposition disposition, Duration lifetime, String cachePolicy) {
        public ReadRequest {
            text(location); text(contentVersion); text(fileName); Objects.requireNonNull(lifetime);
            if (contentType==null || !contentType.matches("[a-z0-9.+-]+/[a-z0-9.+-]+")
                    || disposition==null || disposition==BlobPolicyProfile.Disposition.AUTO
                    || lifetime.isZero() || lifetime.isNegative() || lifetime.getNano()!=0
                    || cachePolicy==null || !(cachePolicy.equals("private, no-store") || cachePolicy.matches("private, max-age=[0-9]+"))
                    || fileName.codePoints().anyMatch(Character::isISOControl)) throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
            try { lifetime.toMillis(); if (!cachePolicy.equals("private, no-store")) Long.parseLong(cachePolicy.substring("private, max-age=".length())); }
            catch (ArithmeticException | NumberFormatException invalid) { throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID); }
        }
        @Override public String toString() { return "ReadRequest[redacted]"; }
    }
    record SignedRead(URI url, Instant expiresAt) {
        public SignedRead {
            Objects.requireNonNull(url); Objects.requireNonNull(expiresAt);
            if (!url.isAbsolute() || url.getHost()==null || url.getRawUserInfo()!=null || url.getFragment()!=null
                    || !(url.getScheme().equals("https") || url.getScheme().equals("http"))) throw new BlobException(BlobException.Code.BLOB_SIGN_FAILED);
        }
        @Override public String toString() { return "SignedRead[redacted]"; }
    }
    enum DeleteResult { DELETED, ALREADY_MISSING, VERSION_MISMATCH }

    /** Read-only explicit readiness; unsupported adapters fail closed. */
    default void validateReady() { throw new BlobException(BlobException.Code.BLOB_CAPABILITY_MISSING); }
    /** Largest single immutable write supported by this adapter, independently of deployment policy. */
    default long maximumWriteBytes() { return 0; }
    Capabilities capabilities();
    UploadTarget createUpload(UserContext context, String uploadId);
    /** Bounded immutable write. A repeated identical write may return the original generation. */
    StoredContent write(UserContext context, UploadTarget target, InputStream source, long maxBytes);
    Optional<StoredContent> stat(UserContext context, String location);
    /**
     * Called only after a committed catalog deleting claim. Remote publishers must close the
     * namespace against delayed immutable writes before absence can acknowledge deletion.
     * The default is for synchronous providers already fenced by the catalog transaction.
     */
    default Optional<StoredContent> sealForDeletion(UserContext context,String location,String expectedVersion) {
        Optional<StoredContent> content=stat(context,location);
        if (expectedVersion!=null && content.isPresent() && !expectedVersion.equals(content.orElseThrow().contentVersion())) {
            throw new BlobException(BlobException.Code.BLOB_IDEMPOTENCY_CONFLICT);
        }
        return content;
    }
    RangeContent openRange(UserContext context, String location, String expectedVersion, long offset, long length);
    List<SignedRead> signReadBatch(UserContext context, List<ReadRequest> requests);
    /** Atomic precondition. A stat followed by an unguarded delete does not implement this method. */
    DeleteResult deleteIfVersion(UserContext context, String location, String expectedVersion);

    private static void text(String value) {
        if (value==null || value.isBlank() || value.length()>2048 || value.indexOf('\0')>=0) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
    }
}
