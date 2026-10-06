package io.teaql.runtime;

import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.internal.CheckerInvocation;
import io.teaql.core.checker.FixEvidence;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Lifecycle tests, complementary to the actual generated Checker/SQLite acceptance. */
public class CheckerInvocationTest {
    private static FixEvidence evidence(String path) {
        return new FixEvidence("CustomerOrder", path, FixEvidence.Source.CLOCK, "graphClock");
    }

    @Test public void reservedAttributesAndClockRestoreAfterNestedInvocations() {
        var clocks = new AtomicInteger();
        var context = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() {
                return LocalDateTime.of(2026, 10, 2, 0, 0).plusDays(clocks.getAndIncrement());
            }
        };
        context.putAttribute("application.config", "preserved");
        try (var outer = CheckerInvocation.open(context)) {
            assertSame(outer, CheckerInvocation.current(context));
            Object outerResults = context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT);
            assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), context.evaluate("now"));
            context.recordFixEvidence(evidence("outer_clock"));
            try (var inner = CheckerInvocation.open(context)) {
                assertSame(inner, CheckerInvocation.current(context));
                assertNotSame(outerResults, context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
                assertEquals(LocalDateTime.of(2026, 10, 3, 0, 0), context.evaluate("now"));
                context.recordFixEvidence(evidence("inner_clock"));
                context.finishFixEvidence();
            }
            assertEquals(List.of(evidence("inner_clock")), context.lastFixEvidence());
            assertSame(outerResults, context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
            assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), context.evaluate("now"));
            context.finishFixEvidence();
        }
        assertEquals(2, clocks.get());
        assertNull(CheckerInvocation.current(context));
        assertNull(context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        assertNull(context.getAttribute(Checker.TEAQL_DATA_CHECKED_ITEMS));
        assertNull(context.getAttribute(Checker.TEAQL_FIX_TIME));
        assertNull(context.getAttribute(UserContext.TEAQL_FIX_EVIDENCE_CURRENT));
        assertNull("completed receipt is not a shared Context attribute", context.getAttribute(UserContext.TEAQL_FIX_EVIDENCE_LAST));
        assertEquals(List.of(evidence("outer_clock")), context.lastFixEvidence());
        assertEquals("preserved", context.getAttribute("application.config"));
    }

    @Test public void closeOnWrongThreadFailsWithoutDestroyingTheOwningInvocation() throws Exception {
        var context = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() { return LocalDateTime.of(2026, 10, 2, 0, 0); }
        };
        var workers = Executors.newSingleThreadExecutor();
        try (var invocation = CheckerInvocation.open(context)) {
            var error = workers.submit(() -> {
                assertNull(CheckerInvocation.current(context));
                assertTrue(context.lastFixEvidence().isEmpty());
                return assertThrows(IllegalStateException.class, invocation::close);
            }).get(10, TimeUnit.SECONDS);
            assertTrue(error.getMessage().contains("owning thread"));
            assertSame(invocation, CheckerInvocation.current(context));
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertNull(CheckerInvocation.current(context));
    }

    @Test public void failedClockCaptureCannotReplaceTheOuterInvocation() {
        var calls = new AtomicInteger();
        var context = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() {
                if (calls.incrementAndGet() == 2) throw new IllegalStateException("clock unavailable");
                return LocalDateTime.of(2026, 10, 2, 0, 0);
            }
        };
        try (var outer = CheckerInvocation.open(context)) {
            assertThrows(IllegalStateException.class, () -> CheckerInvocation.open(context));
            assertSame(outer, CheckerInvocation.current(context));
            assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), context.evaluate("now"));
        }
        assertNull(CheckerInvocation.current(context));
    }

    @Test public void explicitEvidenceSessionSupersedesThePreviousCompletedReceipt() {
        var context = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() { return LocalDateTime.of(2026, 10, 2, 0, 0); }
        };
        try (var ignored = CheckerInvocation.open(context)) {
            context.recordFixEvidence(evidence("checked_clock"));
            context.finishFixEvidence();
        }
        assertEquals(List.of(evidence("checked_clock")), context.lastFixEvidence());
        context.beginFixEvidence();
        context.recordFixEvidence(evidence("explicit_clock"));
        context.finishFixEvidence();
        assertEquals(List.of(evidence("explicit_clock")), context.lastFixEvidence());
    }

    @Test public void contextsAreMatchedByIdentityAndClosingMustBeLifo() {
        var first = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() { return LocalDateTime.of(2026, 10, 2, 0, 0); }
        };
        var second = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() { return LocalDateTime.of(2026, 10, 3, 0, 0); }
        };
        try (var outer = CheckerInvocation.open(first)) {
            try (var inner = CheckerInvocation.open(second)) {
                assertSame(outer, CheckerInvocation.current(first));
                assertSame(inner, CheckerInvocation.current(second));
                assertNotSame(CheckerInvocation.attribute(first, Checker.TEAQL_DATA_CHECK_RESULT),
                        CheckerInvocation.attribute(second, Checker.TEAQL_DATA_CHECK_RESULT));
                assertThrows(IllegalStateException.class, outer::close);
                assertSame(inner, CheckerInvocation.current(second));
            }
            assertNull(CheckerInvocation.current(second));
            assertSame(outer, CheckerInvocation.current(first));
        }
        assertNull(CheckerInvocation.current(first));
    }

    @Test public void evidenceFailureCannotLeaveTheInnerInvocationBound() {
        var context = new DefaultUserContext(null) {
            @Override public LocalDateTime businessTime() { return LocalDateTime.of(2026, 10, 2, 0, 0); }
        };
        try (var outer = CheckerInvocation.open(context)) {
            var inner = CheckerInvocation.open(context);
            // A broken custom diagnostic hook must not poison subsequent saves.
            inner.attribute(UserContext.TEAQL_FIX_EVIDENCE_LAST, "invalid evidence receipt");
            try {
                assertThrows(ClassCastException.class, inner::close);
                assertSame("failure still restores the outer invocation", outer, CheckerInvocation.current(context));
            } finally {
                // Ensure the intentional red run does not contaminate other tests.
                if (CheckerInvocation.current(context) == inner) {
                    inner.attribute(UserContext.TEAQL_FIX_EVIDENCE_LAST, List.of());
                    inner.close();
                }
            }
        }
        assertNull(CheckerInvocation.current(context));
    }
}
