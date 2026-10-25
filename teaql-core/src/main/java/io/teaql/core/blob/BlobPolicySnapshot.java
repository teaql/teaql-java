package io.teaql.core.blob;

import java.time.Duration;
import io.teaql.core.blob.BlobPolicyProfile.*;

/** Resolved upload/binding policy; later configuration changes cannot shorten accepted promises. */
public record BlobPolicySnapshot(String profile, long version, BlobFieldSpec field,
        long maxSizeBytes, boolean allowEmpty, Duration uploadSessionTtl, Duration uploadUrlTtl,
        Duration unboundRetention, Duration unreferencedRetention, Duration minimumRetention,
        Duration readUrlTtl, UploadMode uploadMode, Sharing sharing, Disposition disposition,
        ResponseShape responseShape, String cachePolicy) {
    public BlobPolicySnapshot {
        if (profile == null || field == null || uploadMode == null || sharing == null
                || disposition == null || responseShape == null || profile.isBlank()
                || version <= 0 || maxSizeBytes <= 0 || cachePolicy == null) {
            throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
        }
        positive(uploadSessionTtl); positive(uploadUrlTtl); positive(readUrlTtl);
        nonnegative(unboundRetention); nonnegative(unreferencedRetention); nonnegative(minimumRetention);
        if (!cachePolicy.equals("private, no-store")) {
            if (!cachePolicy.matches("private, max-age=[0-9]+")) throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
            try {
                if (Long.parseLong(cachePolicy.substring("private, max-age=".length())) > readUrlTtl.getSeconds()) {
                    throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
                }
            } catch (NumberFormatException invalid) { throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID); }
        }
    }
    private static void positive(Duration value) {
        nonnegative(value);
        if (value.isZero()) throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
    }
    private static void nonnegative(Duration value) {
        if (value == null || value.isNegative() || value.getNano() != 0) throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
        try { value.toMillis(); }
        catch (ArithmeticException invalid) { throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID); }
    }
}
