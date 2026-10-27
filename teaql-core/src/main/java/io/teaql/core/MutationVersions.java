package io.teaql.core;

import java.util.Objects;

/** Checked signed-64-bit versions shared by planning, providers and readback. */
public final class MutationVersions {
    private MutationVersions() {}

    public static long next(MutationOperationKind operation, Long originalVersion) {
        Objects.requireNonNull(operation, "operation");
        if (operation == MutationOperationKind.CREATE) return 1L;
        if (originalVersion == null || (operation == MutationOperationKind.RECOVER
                ? originalVersion >= 0 : originalVersion <= 0)) {
            throw new MutationVersionException(MutationVersionException.Code.INVALID_VERSION_STATE, operation, originalVersion);
        }
        long version = originalVersion;
        return switch (operation) {
            case UPDATE -> {
                if (version == Long.MAX_VALUE) throw exhausted(operation, version);
                yield version + 1;
            }
            // Avoid even an intermediate MAX+1 overflow. Deleting MAX is legal.
            case DELETE -> -version - 1;
            case RECOVER -> {
                if (version < Long.MIN_VALUE + 2) throw exhausted(operation, version);
                yield -version + 1;
            }
            case CREATE -> 1L;
        };
    }

    private static MutationVersionException exhausted(MutationOperationKind operation, long version) {
        return new MutationVersionException(MutationVersionException.Code.VERSION_EXHAUSTED, operation, version);
    }
}
