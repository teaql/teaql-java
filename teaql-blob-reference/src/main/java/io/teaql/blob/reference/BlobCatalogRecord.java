package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import java.time.Instant;
import java.util.Objects;

/** Infrastructure-only immutable catalog snapshot. Never use this as a prepared response. */
public record BlobCatalogRecord(BlobRef reference, BlobScope scope, BlobState state,
        String uploadId, String originalName, String extension, String contentType, Long sizeBytes,
        String sha256, String contentVersion, String storageLocation, BlobFieldTarget uploadTarget,
        BlobPolicySnapshot policy, Instant createdAt, Instant uploadExpiresAt, Instant completedAt,
        Instant firstBoundAt, Instant unreferencedAt, Instant retentionFloorUntil,
        long effectiveUnreferencedSeconds, boolean singleBinding, Instant deletedAt) {
    public BlobCatalogRecord {
        Objects.requireNonNull(reference); Objects.requireNonNull(scope); Objects.requireNonNull(state);
        Objects.requireNonNull(policy); Objects.requireNonNull(uploadTarget);
        Objects.requireNonNull(createdAt); Objects.requireNonNull(uploadExpiresAt);
        if (effectiveUnreferencedSeconds < 0) throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
    }
    @Override public String toString() { return "BlobCatalogRecord[redacted,state=" + state + "]"; }
}
