package io.teaql.core;

import java.math.BigInteger;
import org.junit.Test;
import static io.teaql.core.MutationOperationKind.*;
import static org.junit.Assert.*;

public class VersionLifecycleCoreTest {
    @Test public void exactLiveEndpointsAndDeleteAtMax() {
        assertEquals(2L, MutationVersions.next(UPDATE, 1L));
        assertEquals(Long.MAX_VALUE, MutationVersions.next(UPDATE, Long.MAX_VALUE - 1));
        assertEquals(-2L, MutationVersions.next(DELETE, 1L));
        assertEquals(Long.MIN_VALUE, MutationVersions.next(DELETE, Long.MAX_VALUE));
    }
    @Test public void exactRecoveryEndpoints() {
        assertEquals(2L, MutationVersions.next(RECOVER, -1L));
        assertEquals(Long.MAX_VALUE, MutationVersions.next(RECOVER, Long.MIN_VALUE + 2));
    }
    @Test public void explicitCreatesDoNotTreatClientVersionAsPersistedBaseline() {
        for (Long input : new Long[]{null, 0L, -2L, Long.MAX_VALUE}) assertEquals(1L, MutationVersions.next(CREATE, input));
    }
    @Test public void exhaustedTransitionsHaveTypedOriginalAndOperation() {
        exhausted(UPDATE, Long.MAX_VALUE);
        exhausted(RECOVER, Long.MIN_VALUE); exhausted(RECOVER, Long.MIN_VALUE + 1);
    }
    private static void exhausted(MutationOperationKind operation, long original) {
        var error = assertThrows(MutationVersionException.class, () -> MutationVersions.next(operation, original));
        assertEquals(MutationVersionException.Code.VERSION_EXHAUSTED, error.code());
        assertEquals(operation, error.operation()); assertEquals(Long.valueOf(original), error.originalVersion());
    }
    @Test public void missingZeroAndWrongSignsCannotChangeLifecycle() {
        for (var operation : new MutationOperationKind[]{UPDATE, DELETE, RECOVER}) {
            Long wrongSign = operation == RECOVER ? 1L : -1L;
            for (Long original : new Long[]{null, 0L, wrongSign}) {
                var error = assertThrows(MutationVersionException.class, () -> MutationVersions.next(operation, original));
                assertEquals(MutationVersionException.Code.INVALID_VERSION_STATE, error.code());
            }
        }
    }
    @Test public void deterministicDomainSampleMatchesUnboundedIntegerArithmetic() {
        var random = new java.util.Random(236);
        for (int i = 0; i < 2048; i++) {
            long original = random.nextLong() & Long.MAX_VALUE;
            if (original == 0 || original == Long.MAX_VALUE) continue;
            var value = BigInteger.valueOf(original);
            assertEquals(value.add(BigInteger.ONE).longValueExact(), MutationVersions.next(UPDATE, original));
            long tombstone = MutationVersions.next(DELETE, original);
            assertEquals(value.negate().subtract(BigInteger.ONE).longValueExact(), tombstone);
            assertEquals(value.add(BigInteger.TWO).longValueExact(), MutationVersions.next(RECOVER, tombstone));
        }
    }
}
