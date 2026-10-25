package io.teaql.core.blob;

import java.io.InputStream;

/** Actual byte recognition/validation; filename and declared MIME never supply the detected type. */
public interface BlobContentVerifier {
    record Verified(String contentType, long sizeBytes, String sha256) {
        public Verified {
            if (contentType==null || !contentType.matches("[a-z0-9.+-]+/[a-z0-9.+-]+") || sizeBytes<0 || sha256==null || !sha256.matches("[0-9a-f]{64}")) {
                throw new BlobException(BlobException.Code.BLOB_REFERENCE_INVALID);
            }
        }
        @Override public String toString() { return "VerifiedBlobContent[sizeBytes="+sizeBytes+"]"; }
    }
    /** Explicit format attestation for module readiness; legacy implementations remain unadvertised. */
    default java.util.Set<String> supportedExtensions() { return java.util.Set.of(); }
    /** Source stays caller-owned. The expected extension is a restriction, never a detection hint. */
    Verified verify(InputStream source, String expectedExtension, long maximumBytes, boolean allowEmpty);
}
