package io.teaql.dataservice.sql;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.*;
import io.teaql.runtime.*;
import java.lang.reflect.*;
import java.sql.BatchUpdateException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlStreamBatchMaskingTest {
    private static final String SQL = "SELECT * FROM customer WHERE name = ? AND address = ? AND password = ?";
    private static final Object[] ARGS = {"Riverside", "1 Runtime Road", "PASSWORD-CANARY"};
    private static final SqlLogBindings BINDINGS = new SqlLogBindings(List.of(
            SqlParameterLogPolicy.MASKED, SqlParameterLogPolicy.PLAIN, SqlParameterLogPolicy.CREDENTIAL), true);
    private final List<ExecutionMetadata> logs = new ArrayList<>();
    private final AtomicInteger closed = new AtomicInteger();
    private final RuntimeException error = new IllegalStateException("PASSWORD-CANARY Riverside");
    private boolean enabled = true;
    private SqlLogBindings streamBindings = BINDINGS;
    private boolean brokenSink;
    private final DefaultUserContext context = new DefaultUserContext(TeaQLRuntime.builder()
            .metadata(new SimpleEntityMetaFactory()).logSink((caller, metadata) -> {
                logs.add(metadata);
                if (brokenSink) throw new IllegalStateException("sink failed");
            }).build()) {
        @Override public boolean isQueryExecutionLoggingEnabled() { return enabled; }
        @Override public boolean isMutationExecutionLoggingEnabled() { return enabled; }
    };

    private TeaQLDatabase database(InvocationHandler action) throws Exception {
        var adapter = (SqlExecutionAdapter) Proxy.newProxyInstance(SqlExecutionAdapter.class.getClassLoader(),
                new Class<?>[]{SqlExecutionAdapter.class}, action);
        var executor = new SqlDataServiceExecutor("sql", adapter);
        var factory = SqlDataServiceExecutor.class.getDeclaredMethod("createPortableService", io.teaql.core.meta.EntityMetaFactory.class);
        factory.setAccessible(true);
        var service = factory.invoke(executor, new SimpleEntityMetaFactory());
        var field = PortableSQLDataService.class.getDeclaredField("database");
        field.setAccessible(true);
        return (TeaQLDatabase) field.get(service);
    }

    private Stream<Map<String, Object>> stream(String mode) throws Exception {
        context.pushTrace(TraceKind.COMMENT, "Customer", "read Riverside");
        context.pushTrace(TraceKind.PURPOSE, "Customer", "verify cursor lifecycle");
        var db = database((proxy, method, args) -> {
            if (mode.equals("open")) throw error;
            Stream<Integer> input = mode.equals("empty") ? Stream.empty() : Stream.of(1, 2, 3);
            return input.map(index -> {
                if (index == 2 && mode.equals("read")) throw error;
                if (index == 2 && mode.equals("cancel")) throw new CancellationException("PASSWORD-CANARY");
                return Map.<String, Object>of("id", index);
            }).onClose(() -> {
                closed.incrementAndGet();
                if (mode.equals("close")) throw error;
            });
        });
        try {
            return db.queryForStream(context, SQL, ARGS, streamBindings);
        } finally {
            context.popTrace(); context.popTrace();
        }
    }

    private void safe(ExecutionMetadata log) {
        assertTrue(log.getDebugQuery(), log.getDebugQuery().contains("Ri*****de"));
        assertTrue(log.getDebugQuery().contains("1 Runtime Road"));
        assertFalse(log.getDebugQuery().contains("PASSWORD-CANARY"));
        assertFalse(log.getDebugQuery().contains("Riverside"));
        assertTrue(log.getDebugQuery().contains("NOT REPLAYABLE"));
        assertArrayEquals(new Object[]{"Riverside", "1 Runtime Road", "PASSWORD-CANARY"}, ARGS);
    }

    private void terminal(String outcome, Integer delivered) {
        assertEquals(1, logs.size());
        var log = logs.get(0);
        safe(log);
        assertEquals(outcome, log.getExecutionOutcome());
        assertEquals(delivered, log.getResultCount());
        assertEquals("verify cursor lifecycle", log.getPurpose());
        assertTrue(log.getComment().startsWith("read "));
        assertFalse(log.getComment().contains("Riverside"));
        assertEquals(1, closed.get());
    }

    @Test public void exhaustedCursorClosesAndLogsOnce() throws Exception {
        try (var rows = stream("ok")) {
            assertEquals(3, rows.toList().size());
            assertEquals(1, closed.get());
            assertEquals(1, logs.size());
        }
        terminal("success", 3);
    }
    @Test public void emptyCursorCompletesWithKnownZeroRows() throws Exception {
        try (var rows = stream("empty")) {
            assertEquals(0, rows.count());
            assertEquals(1, closed.get());
        }
        terminal("success", 0);
    }
    @Test public void countCannotSkipDeliveryOrDiagnostics() throws Exception {
        try (var rows = stream("ok")) { assertEquals(3L, rows.count()); }
        terminal("success", 3);
    }
    @Test public void earlyCloseCountsOnlyDeliveredRows() throws Exception {
        try (var rows = stream("ok")) { assertEquals(1, rows.limit(1).toList().size()); }
        terminal("cancelled", 1);
    }
    @Test public void unopenedConsumptionClosesWithZeroRows() throws Exception {
        stream("ok").close(); terminal("cancelled", 0);
    }
    @Test public void readFailureKeepsDeliveredCountAndOriginalError() throws Exception {
        try (var rows = stream("read")) { assertSame(error, assertThrows(RuntimeException.class, rows::toList)); }
        terminal("failure", 1);
    }
    @Test public void cancellationKeepsDeliveredCount() throws Exception {
        try (var rows = stream("cancel")) { assertThrows(CancellationException.class, rows::toList); }
        terminal("cancelled", 1);
    }
    @Test public void downstreamFailureClosesCursor() throws Exception {
        try (var rows = stream("ok")) {
            assertSame(error, assertThrows(RuntimeException.class, () -> rows.forEach(row -> { throw error; })));
        }
        terminal("failure", 1);
    }
    @Test public void downstreamErrorAlsoClosesCursor() throws Exception {
        var fatal = new AssertionError("consumer failed");
        try (var rows = stream("ok")) {
            assertSame(fatal, assertThrows(AssertionError.class, () -> rows.forEach(row -> { throw fatal; })));
        }
        terminal("failure", 1);
    }
    @Test public void closeFailureIsNotReportedAsSuccess() throws Exception {
        var rows = stream("close");
        assertSame(error, assertThrows(RuntimeException.class, rows::toList));
        rows.close(); terminal("failure", 3);
    }
    @Test public void lateConsumptionRetainsOriginalIntent() throws Exception {
        var rows = stream("ok");
        context.pushTrace(TraceKind.PURPOSE, "Other", "unrelated later request");
        try (rows) { rows.toList(); }
        terminal("success", 3);
    }
    @Test public void openFailureHasUnknownCount() throws Exception {
        assertSame(error, assertThrows(RuntimeException.class, () -> stream("open")));
        assertEquals(1, logs.size()); safe(logs.get(0));
        assertEquals("failure", logs.get(0).getExecutionOutcome());
        assertNull(logs.get(0).getResultCount()); assertEquals(0, closed.get());
    }
    @Test public void sinkFailureDoesNotReplaceReadFailure() throws Exception {
        brokenSink = true;
        try (var rows = stream("read")) { assertSame(error, assertThrows(RuntimeException.class, rows::toList)); }
        terminal("failure", 1);
    }
    @Test public void disabledLogsStillCloseCursor() throws Exception {
        enabled = false;
        try (var rows = stream("ok")) { rows.limit(1).toList(); }
        assertEquals(0, logs.size()); assertEquals(1, closed.get());
    }

    @Test public void cursorCollectsTerminalEvidenceIndependentlyOfLogging() throws Exception {
        for (boolean logging : List.of(false, true)) {
            for (String mode : List.of("ok", "empty", "early", "unused", "read", "cancel", "close", "consumer")) {
                var fixture = new SqlStreamBatchMaskingTest();
                fixture.enabled = logging;
                var facts = new ArrayList<ExecutionMetadata>();
                var request = new BaseRequest<>(BaseEntity.class, BaseEntity::new) {
                    { internalComment("read Riverside"); internalPurpose("verify cursor lifecycle"); }
                    @Override public String getTypeName() { return "Customer"; }
                };
                fixture.streamBindings = BINDINGS.withTrace(SqlExecutionTrace.query(request).collecting(facts::add));
                var rows = fixture.stream(mode);
                assertTrue("open is not completion", facts.isEmpty());
                try (rows) {
                    switch (mode) {
                        case "early" -> assertEquals(1, rows.limit(1).toList().size());
                        case "unused" -> { }
                        case "read", "close" -> assertSame(fixture.error, assertThrows(RuntimeException.class, rows::toList));
                        case "cancel" -> assertThrows(CancellationException.class, rows::toList);
                        case "consumer" -> assertSame(fixture.error, assertThrows(RuntimeException.class,
                                () -> rows.forEach(row -> { throw fixture.error; })));
                        default -> rows.toList();
                    }
                }
                rows.close();
                assertEquals(mode + " logging=" + logging, 1, facts.size());
                assertEquals(1, fixture.closed.get());
                var fact = facts.get(0);
                String outcome = switch (mode) {
                    case "early", "unused", "cancel" -> "cancelled";
                    case "read", "close", "consumer" -> "failure";
                    default -> "success";
                };
                int count = switch (mode) { case "empty", "unused" -> 0; case "ok", "close" -> 3; default -> 1; };
                assertEquals(outcome, fact.getExecutionOutcome());
                assertEquals(Integer.valueOf(count), fact.getResultCount());
                assertEquals("read Riverside", fact.getComment());
                assertEquals("select", fact.getTraceChain().get(fact.getTraceChain().size() - 1).getName());
                assertEquals(logging ? 1 : 0, fixture.logs.size());
                fixture.safe(LogPrivacy.sql(fact, false));
            }
        }
    }

    private void batch(int[] result, RuntimeException failure) throws Exception {
        var db = database((proxy, method, args) -> {
            if (failure != null) throw failure;
            return result;
        });
        var rows = List.of(ARGS, ARGS.clone(), ARGS.clone());
        if (failure == null) assertSame(result, db.batchUpdate(context, SQL, rows, BINDINGS));
        else assertSame(failure, assertThrows(RuntimeException.class, () -> db.batchUpdate(context, SQL, rows, BINDINGS)));
        assertEquals(enabled ? 3 : 0, logs.size());
        for (var log : logs) { safe(log); assertNull(log.getResultCount()); }
    }
    @Test public void batchSuccessAndSuccessNoInfo() throws Exception {
        batch(new int[]{1, 0, -2}, null);
        assertEquals(List.of("success", "success", "success"), logs.stream().map(ExecutionMetadata::getExecutionOutcome).toList());
        assertTrue(logs.stream().allMatch(log -> "success".equals(log.getBatchOutcome())));
        assertEquals(Long.valueOf(1), logs.get(0).getAffectedRows());
        assertEquals(Long.valueOf(0), logs.get(1).getAffectedRows());
        assertNull(logs.get(2).getAffectedRows());
    }
    @Test public void partialBatchDoesNotInventUnexecutedOutcomes() throws Exception {
        batch(null, new RuntimeException(new BatchUpdateException("PASSWORD-CANARY", new int[]{1, -3})));
        assertEquals(List.of("success", "failure", "unknown"), logs.stream().map(ExecutionMetadata::getExecutionOutcome).toList());
        assertTrue(logs.stream().allMatch(log -> "failure".equals(log.getBatchOutcome())));
        assertEquals(Long.valueOf(1), logs.get(0).getAffectedRows());
        assertNull(logs.get(1).getAffectedRows()); assertNull(logs.get(2).getAffectedRows());
    }
    @Test public void genericBatchFailureDoesNotInventProgress() throws Exception {
        batch(null, error);
        assertTrue(logs.stream().allMatch(log -> "unknown".equals(log.getExecutionOutcome()) && log.getAffectedRows() == null));
        assertTrue(logs.stream().allMatch(log -> log.getResultSummary().contains("Batch failure")));
        assertTrue(logs.stream().allMatch(log -> "failure".equals(log.getBatchOutcome())));
    }
    @Test public void brokenSinkDoesNotReplaceBatchFailure() throws Exception {
        brokenSink = true; batch(null, error);
    }
    @Test public void disabledBatchLogsPreserveOriginalError() throws Exception {
        enabled = false; batch(null, error);
    }

    private SqlExecutionTrace itemTrace(long id, String reason) {
        var entity = new BaseEntity();
        entity.__internalSet("id", id);
        return SqlExecutionTrace.mutation(entity,
                List.of(new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", id, reason)), "insert");
    }

    @Test public void partialPreparedBatchKeepsPerRowTraceForSuccessFailureAndUnknown() throws Exception {
        var traces = List.of(itemTrace(201, "entry alpha"), itemTrace(202, "entry beta"), itemTrace(203, "entry gamma"));
        var supplied = new ArrayList<>(traces);
        var bindings = BINDINGS.withBatchTraces(supplied);
        supplied.clear();
        var failure = new RuntimeException(new BatchUpdateException("PASSWORD-CANARY", new int[]{1, -3}));
        var db = database((proxy, method, args) -> { throw failure; });
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> db.batchUpdate(context, SQL, List.of(ARGS, ARGS.clone(), ARGS.clone()), bindings)));
        assertEquals(List.of("success", "failure", "unknown"), logs.stream().map(ExecutionMetadata::getExecutionOutcome).toList());
        for (int index = 0; index < logs.size(); index++) {
            assertEquals(traces.get(index).mutationLineage(), logs.get(index).getMutationLineage());
            safe(logs.get(index));
        }
        assertThrows(UnsupportedOperationException.class, () -> bindings.batchTraces().clear());
    }

    @Test public void malformedTraceRowCountRejectsBeforeDriverEvenWithLoggingDisabled() throws Exception {
        enabled = false;
        var driverCalls = new AtomicInteger();
        var db = database((proxy, method, args) -> { driverCalls.incrementAndGet(); return new int[]{1, 1}; });
        assertThrows(IllegalArgumentException.class, () -> db.batchUpdate(context, SQL, List.of(ARGS, ARGS.clone()),
                BINDINGS.withBatchTraces(List.of(itemTrace(201, "only one trace")))));
        assertEquals(0, driverCalls.get());
        assertTrue(logs.isEmpty());
    }
}
