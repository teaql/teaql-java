package io.teaql.core.checker;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.checker.internal.CheckerInvocation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class StandaloneCheckerInvocationTest {
    private static final LocalDateTime T0 = LocalDateTime.of(2032, 2, 29, 10, 15, 30);
    private static final String[] KEYS = {Checker.TEAQL_FIX_TIME, Checker.TEAQL_DATA_CHECKED_ITEMS,
            Checker.TEAQL_DATA_CHECK_RESULT, UserContext.TEAQL_FIX_EVIDENCE_CURRENT};

    private static final class Entity extends BaseEntity {
        @Override public String typeName() { return "ClockNode"; }
    }
    @FunctionalInterface private interface Work { void run(UserContext context, Entity entity); }
    private static Checker<Entity> checker(Work work) {
        return new Checker<>() {
            public String type() { return "ClockNode"; }
            public void checkAndFix(UserContext context, Entity entity, ObjectLocation location) { work.run(context, entity); }
        };
    }
    private static final class Context {
        final Map<String,Object> attributes = new ConcurrentHashMap<>();
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger next;
        volatile RuntimeException clockFailure;
        final UserContext value;
        Context() { this(new AtomicInteger()); }
        Context(AtomicInteger next) {
            this.next = next;
            value = (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(), new Class[]{UserContext.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("businessTime")) {
                            calls.incrementAndGet();
                            if (clockFailure != null) throw clockFailure;
                            return T0.plusNanos(next.getAndIncrement() * 1_000_000L);
                        }
                        if (method.getName().equals("getAttribute") && args.length == 1) return attributes.get(args[0]);
                        if (method.getName().equals("putAttribute")) {
                            if (args[1] == null) attributes.remove(args[0]); else attributes.put((String) args[0], args[1]);
                            return null;
                        }
                        if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "ClockContext";
                        };
                        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                        throw new AssertionError("unexpected capability/provider operation: " + method.getName());
                    });
        }
    }
    private static LocalDateTime time(UserContext context) {
        return (LocalDateTime) CheckerInvocation.attribute(context, Checker.TEAQL_FIX_TIME);
    }
    private static void evidence(UserContext context, String label) {
        context.recordFixEvidence(new FixEvidence("ClockNode", "create_time", FixEvidence.Source.CLOCK, label));
    }
    private static void released(Context context) {
        assertNull(CheckerInvocation.current(context.value));
        for (String key : KEYS) assertNull(key, context.attributes.get(key));
    }

    @Test public void standaloneCapturesOnceAndPublishesDetachedReceipts() {
        var context = new Context(); var times = new ArrayList<LocalDateTime>();
        var checker = checker((ctx, entity) -> { times.add(time(ctx)); evidence(ctx, "first"); times.add(time(ctx)); });
        checker.checkAndFix(context.value, new Entity());
        List<FixEvidence> first = context.value.lastFixEvidence();
        assertEquals(1, context.calls.get()); assertEquals(List.of(T0,T0), times); released(context);
        assertThrows(UnsupportedOperationException.class, () -> first.add(first.get(0)));
        checker.checkAndFix(context.value, new Entity());
        assertEquals(List.of(T0,T0,T0.plusNanos(1_000_000),T0.plusNanos(1_000_000)), times);
        assertEquals(2, context.calls.get()); assertEquals(1, first.size()); assertEquals(1, context.value.lastFixEvidence().size()); released(context);
    }

    @Test public void aLaterInvocationChecksTheSameEntityAgain() {
        var context = new Context(); var entity = new Entity(); var visited = new AtomicInteger();
        Checker<Entity>[] holder = new Checker[1];
        holder[0] = checker((ctx, item) -> {
            if (!holder[0].needCheck(ctx, item)) return;
            holder[0].markAsChecked(ctx, item); visited.incrementAndGet();
        });
        holder[0].checkAndFix(context.value, entity); holder[0].checkAndFix(context.value, entity);
        assertEquals(2, visited.get()); assertEquals(2, context.calls.get()); released(context);
    }

    @Test public void recursivePublicChecksShareTheClockAndCycleGuard() {
        var context = new Context(); var first = new Entity(); var second = new Entity();
        var seen = new ArrayList<LocalDateTime>(); Checker<Entity>[] holder = new Checker[1];
        holder[0] = checker((ctx, item) -> {
            if (!holder[0].needCheck(ctx, item)) return;
            holder[0].markAsChecked(ctx, item); seen.add(time(ctx));
            holder[0].checkAndFix(ctx, item == first ? second : first);
        });
        holder[0].checkAndFix(context.value, first);
        assertEquals(List.of(T0,T0), seen); assertEquals(1, context.calls.get()); released(context);
    }

    @Test public void anExistingMutationInvocationIsJoinedWithoutResettingItsState() {
        var context = new Context();
        try (var outer = CheckerInvocation.openMutation(context.value)) {
            Object guard = outer.attribute(Checker.TEAQL_DATA_CHECKED_ITEMS);
            checker((ctx, entity) -> {
                assertSame(outer, CheckerInvocation.current(ctx)); assertTrue(outer.mutationOnly());
                assertSame(guard, CheckerInvocation.attribute(ctx, Checker.TEAQL_DATA_CHECKED_ITEMS)); evidence(ctx, "joined");
            }).checkAndFix(context.value, new Entity());
            assertSame(outer, CheckerInvocation.current(context.value)); assertEquals(1, context.calls.get());
        }
        assertEquals(1, context.value.lastFixEvidence().size()); released(context);
    }

    @Test public void aDifferentContextGetsItsOwnClockAndRestoresTheOuterContext() {
        var counter = new AtomicInteger(); var first = new Context(counter); var second = new Context(counter);
        checker((ctx, entity) -> {
            var outer = CheckerInvocation.current(ctx); assertEquals(T0, time(ctx));
            checker((other, ignored) -> { assertNotSame(outer, CheckerInvocation.current(other)); assertEquals(T0.plusNanos(1_000_000),time(other)); })
                    .checkAndFix(second.value, new Entity());
            assertSame(outer, CheckerInvocation.current(ctx)); assertEquals(T0,time(ctx));
        }).checkAndFix(first.value, new Entity());
        assertEquals(1, first.calls.get()); assertEquals(1, second.calls.get()); released(first); released(second);
    }

    @Test public void failureRetainsItsOwnReceiptAndRetryCapturesFreshTime() {
        var context = new Context(); var error = new IllegalStateException("checker failed");
        assertSame(error, assertThrows(IllegalStateException.class, () -> checker((ctx, item) -> {
            evidence(ctx,"failed"); throw error;
        }).checkAndFix(context.value,new Entity())));
        var failed = context.value.lastFixEvidence(); assertEquals(1,failed.size()); released(context);
        checker((ctx, item) -> { assertEquals(T0.plusNanos(1_000_000),time(ctx)); evidence(ctx,"retry"); }).checkAndFix(context.value,new Entity());
        assertEquals(1,failed.size()); assertEquals(1,context.value.lastFixEvidence().size()); assertEquals(2,context.calls.get()); released(context);
    }

    @Test public void synchronousCancellationUnwindsWithoutReplacingItsExactError() {
        var context = new Context(); var error = new CancellationException("cancelled");
        assertSame(error,assertThrows(CancellationException.class,()->checker((ctx,item)->{ evidence(ctx,"cancelled"); throw error; }).checkAndFix(context.value,new Entity())));
        released(context);
        checker((ctx,item)->assertEquals(T0.plusNanos(1_000_000),time(ctx))).checkAndFix(context.value,new Entity());
        assertEquals(2,context.calls.get()); released(context);
    }

    @Test public void clockFailureCannotBindOrLeakAnInvocation() {
        var context = new Context(); var error = new IllegalStateException("clock failed"); context.clockFailure = error;
        assertSame(error,assertThrows(IllegalStateException.class,()->checker((ctx,item)->fail("callback must not run")).checkAndFix(context.value,new Entity())));
        released(context); context.clockFailure = null;
        checker((ctx,item)->assertEquals(T0,time(ctx))).checkAndFix(context.value,new Entity());
        assertEquals(2,context.calls.get()); released(context);
    }

    @Test public void diagnosticCloseFailureIsSuppressedUnderTheOriginalCallbackError() {
        var context = new Context(); var error = new IllegalStateException("primary checker failure");
        assertSame(error,assertThrows(IllegalStateException.class,()->checker((ctx,item)->{
            CheckerInvocation.current(ctx).attribute(UserContext.TEAQL_FIX_EVIDENCE_LAST,"invalid receipt"); throw error;
        }).checkAndFix(context.value,new Entity())));
        assertEquals(1,error.getSuppressed().length); assertTrue(error.getSuppressed()[0] instanceof ClassCastException); released(context);
    }

    @Test public void anotherThreadCannotBorrowTheEnclosingContextInvocation() throws Exception {
        var context = new Context(); var workers = Executors.newSingleThreadExecutor();
        try (var outer = CheckerInvocation.open(context.value)) {
            workers.submit(()->{
                assertNull(CheckerInvocation.current(context.value));
                checker((ctx,item)->{ assertEquals(T0.plusNanos(1_000_000),time(ctx)); evidence(ctx,"worker"); }).checkAndFix(context.value,new Entity());
                assertNull(CheckerInvocation.current(context.value)); assertEquals(1,context.value.lastFixEvidence().size());
            }).get(10,TimeUnit.SECONDS);
            assertSame(outer,CheckerInvocation.current(context.value)); assertEquals(T0,time(context.value));
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS)); }
        assertEquals(2,context.calls.get()); released(context);
    }

    @Test public void standaloneValidationFailuresSurviveClosingAsARealCheckException() {
        var context = new Context();
        var expected = CheckResult.required(ObjectLocation.hashRoot("contact_phone"));
        var error = assertThrows(CheckException.class, () -> checker((ctx, item) -> {
            evidence(ctx,"invalid");
            CheckerInvocation.current(ctx).attribute(Checker.TEAQL_DATA_CHECK_RESULT,new ArrayList<>(List.of(expected)));
        }).checkAndFix(context.value,new Entity()));
        assertEquals(1,error.getViolates().size()); assertSame(expected,error.getViolates().get(0));
        assertEquals(1,context.calls.get()); assertEquals(1,context.value.lastFixEvidence().size()); released(context);
    }

    @Test public void joinedGraphChecksLeaveValidationFailuresForTheGraphOwner() {
        var context = new Context(); var expected = CheckResult.required(ObjectLocation.hashRoot("contact_phone"));
        try (var outer = CheckerInvocation.openMutation(context.value)) {
            var violations = new ArrayList<CheckResult>();
            outer.attribute(Checker.TEAQL_DATA_CHECK_RESULT,violations);
            checker((ctx,item)->violations.add(expected)).checkAndFix(context.value,new Entity());
            assertSame(outer,CheckerInvocation.current(context.value));
            assertSame(violations,outer.attribute(Checker.TEAQL_DATA_CHECK_RESULT));
            assertEquals(List.of(expected),violations); assertEquals(1,context.calls.get());
        }
        released(context);
    }
}
