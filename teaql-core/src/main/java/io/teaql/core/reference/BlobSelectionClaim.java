package io.teaql.core.reference;

import io.teaql.core.blob.BlobRef;
import java.util.Objects;

/** Authenticated tqs1 payload; its stable Key is never a download or direct write credential. */
public record BlobSelectionClaim(BlobSelectionTarget target, BlobRef reference) {
    public BlobSelectionClaim {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(reference, "reference");
    }
    @Override public String toString() { return "BlobSelectionClaim[redacted]"; }
}
