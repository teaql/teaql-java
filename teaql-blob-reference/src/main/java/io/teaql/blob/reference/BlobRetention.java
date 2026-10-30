package io.teaql.blob.reference;

import io.teaql.core.blob.BlobException;
import io.teaql.core.blob.BlobState;
import java.time.Duration;
import java.time.Instant;
import static io.teaql.core.blob.BlobException.Code.*;

/** Pure deadline rules. Catalog operations must lock/recheck before applying any transition. */
public final class BlobRetention {
    private BlobRetention() {}
    public static Duration effectiveRetention(Iterable<Duration> accepted, Duration proposed) {
        requireRetention(proposed);
        Duration result = proposed;
        for (Duration value : accepted) { requireRetention(value); if (value.compareTo(result) > 0) result = value; }
        return result;
    }
    public static Instant unreferencedEligibleAt(Instant lastUnbound, Duration retention, Instant floor) {
        Instant result = plus(lastUnbound, retention);
        return floor != null && floor.isAfter(result) ? floor : result;
    }
    public static void requireBindable(BlobState state, Instant now, Instant completedAt,
                                       Duration unboundRetention, Instant unreferencedEligibleAt) {
        if (state == null || now == null) throw new BlobException(BLOB_REFERENCE_INVALID);
        if (state == BlobState.INITIATED) throw new BlobException(BLOB_UPLOAD_INCOMPLETE);
        if (state == BlobState.DELETING || state == BlobState.DELETED) throw new BlobException(BLOB_REFERENCE_EXPIRED);
        if (state == BlobState.UPLOADED_UNBOUND) {
            if (completedAt == null) throw new BlobException(BLOB_REFERENCE_INVALID);
            if (!now.isBefore(plus(completedAt, unboundRetention))) throw new BlobException(BLOB_REFERENCE_EXPIRED);
        }
        if (state == BlobState.AVAILABLE && unreferencedEligibleAt != null && !now.isBefore(unreferencedEligibleAt)) {
            throw new BlobException(BLOB_REFERENCE_EXPIRED);
        }
    }
    public static boolean collectable(BlobState state, long bindingCount, Instant now,
            Instant uploadExpiresAt, Instant completedAt, Duration unboundRetention, Instant unreferencedEligibleAt) {
        if (bindingCount < 0 || state == null || now == null) throw new BlobException(BLOB_REFERENCE_INVALID);
        if (bindingCount > 0 || state == BlobState.DELETED) return false;
        return switch (state) {
            case INITIATED -> { if (uploadExpiresAt == null) throw new BlobException(BLOB_REFERENCE_INVALID); yield !now.isBefore(uploadExpiresAt); }
            case UPLOADED_UNBOUND -> !now.isBefore(plus(completedAt, unboundRetention));
            case AVAILABLE -> unreferencedEligibleAt != null && !now.isBefore(unreferencedEligibleAt);
            case DELETING -> true;
            case DELETED -> false;
        };
    }

    private static void requireRetention(Duration value) {
        if (value == null || value.isNegative() || value.getNano() != 0) throw new BlobException(BLOB_POLICY_INVALID);
    }
    private static Instant plus(Instant base, Duration duration) {
        if (base == null) throw new BlobException(BLOB_REFERENCE_INVALID);
        requireRetention(duration);
        try { return base.plus(duration); }
        catch (java.time.DateTimeException | ArithmeticException invalid) { throw new BlobException(BLOB_POLICY_INVALID); }
    }
}
