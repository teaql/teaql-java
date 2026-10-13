package io.teaql.dataservice.sql;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.*;
import io.teaql.runtime.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlStatementTelemetryTest {
    private static final String SQL = "SELECT id FROM school WHERE name = ?";
    private static final SqlLogBindings BINDINGS = new SqlLogBindings(List.of(SqlParameterLogPolicy.MASKED), true);
    private final Recording telemetry = new Recording();
    private final AtomicInteger ordinaryLogs = new AtomicInteger();
    private final DefaultUserContext context = new DefaultUserContext(TeaQLRuntime.builder()
            .metadata(new SimpleEntityMetaFactory()).telemetry(telemetry)
            .queryExecutionLogging(false).mutationExecutionLogging(false)
            .logSink((caller, metadata) -> ordinaryLogs.incrementAndGet()).build());

    private TeaQLDatabase database(InvocationHandler action) throws Exception {
        var adapter = (SqlExecutionAdapter) Proxy.newProxyInstance(SqlExecutionAdapter.class.getClassLoader(),
                new Class<?>[]{SqlExecutionAdapter.class}, action);
        var factory = SqlDataServiceExecutor.class.getDeclaredMethod("createPortableService", io.teaql.core.meta.EntityMetaFactory.class);
        factory.setAccessible(true);
        var service = factory.invoke(new SqlDataServiceExecutor("sql", adapter), new SimpleEntityMetaFactory());
        var field = PortableSQLDataService.class.getDeclaredField("database");
        field.setAccessible(true);
        return (TeaQLDatabase) field.get(service);
    }

    @Test public void outOfOrderExecutionsKeepStatementOwnedIdentityWithLoggingDisabled() throws Exception {
        var firstInDriver = new CountDownLatch(1);
        var secondCompleted = new CountDownLatch(1);
        var identitiesAtDriver = new ConcurrentHashMap<String, String>();
        var db = database((proxy, method, args) -> {
            String value = (String) ((Object[]) args[1])[0];
            identitiesAtDriver.put(value, telemetry.active.get());
            assertNotNull("Statement span must already exist at driver entry", telemetry.active.get());
            if (value.equals("FIRST-CANARY")) {
                firstInDriver.countDown();
                assertTrue(secondCompleted.await(5, TimeUnit.SECONDS));
            }
            return List.of(Map.of("id", 1L));
        });
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> db.query(context, SQL, new Object[]{"FIRST-CANARY"}, BINDINGS));
            assertTrue(firstInDriver.await(5, TimeUnit.SECONDS));
            var second = workers.submit(() -> {
                var rows = db.query(context, SQL, new Object[]{"SECOND-CANARY"}, BINDINGS);
                secondCompleted.countDown();
                return rows;
            });
            assertEquals(1, first.get(8, TimeUnit.SECONDS).size());
            assertEquals(1, second.get(8, TimeUnit.SECONDS).size());
        } finally { workers.shutdownNow(); }
        assertEquals(2, telemetry.events.size());
        assertEquals(identitiesAtDriver.get("SECOND-CANARY"), telemetry.events.get(0).correlation().spanId());
        assertEquals(identitiesAtDriver.get("FIRST-CANARY"), telemetry.events.get(1).correlation().spanId());
        assertNotEquals(telemetry.events.get(0).correlation(), telemetry.events.get(1).correlation());
        assertEquals(0, ordinaryLogs.get());
        assertFalse(telemetry.events.toString().contains("FIRST-CANARY"));
        assertFalse(telemetry.events.toString().contains("SECOND-CANARY"));
        assertEquals(2, telemetry.completed.size());
    }

    @Test public void writeAndReadbackUseDistinctExecutionsAndRetainLineageWithoutEntityIds() throws Exception {
        var db = database((proxy, method, args) -> method.getName().equals("update") ? 1 : List.of(Map.of("id", 17L)));
        var entity = new BaseEntity();
        entity.updateId(17L);
        var intent = MutationIntent.of("save school");
        var lineage = List.of(new TraceNode(TraceKind.AUDIT_REASON, "School", 99887766L, "save school"));
        var trace = SqlExecutionTrace.mutation(entity, lineage, "update", intent);
        db.executeUpdate(context, "UPDATE school SET name = ?", new Object[]{"PRIVATE-CANARY"}, BINDINGS.withTrace(trace));
        db.query(context, SQL, new Object[]{"PRIVATE-CANARY"}, BINDINGS.withTrace(trace.readback(intent)));
        assertEquals(2, telemetry.events.size());
        assertNotEquals(telemetry.events.get(0).correlation().spanId(), telemetry.events.get(1).correlation().spanId());
        assertEquals("save school", telemetry.events.get(0).attributes().get("teaql.intent.audit_reason"));
        assertEquals("save school", telemetry.events.get(1).attributes().get("teaql.intent.audit_reason"));
        assertFalse(telemetry.events.toString().contains("99887766"));
        assertFalse(telemetry.events.toString().contains("PRIVATE-CANARY"));
        assertTrue(telemetry.events.get(0).attributes().get("teaql.mutation_lineage").toString().contains("save school"));
        assertEquals(0, ordinaryLogs.get());
    }

    @Test public void lazyCursorDetachesAndKeepsItsOwnCorrelationUnderUnrelatedOperation() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        var db = database((proxy, method, args) -> Stream.of(Map.<String, Object>of("id", 1L), Map.<String, Object>of("id", 2L))
                .peek(row -> assertNotNull(telemetry.active.get())).onClose(closed::incrementAndGet));
        var cursor = db.queryForStream(context, SQL, new Object[]{"CURSOR-CANARY"}, BINDINGS);
        assertNull("Opening a cursor must not leave its span active", telemetry.active.get());
        var unrelated = telemetry.start(new RuntimeTelemetry.Operation("query", "unrelated", Map.of()));
        String unrelatedId = telemetry.active.get();
        try (cursor) { assertEquals(1, cursor.limit(1).count()); }
        assertEquals(unrelatedId, telemetry.active.get());
        assertEquals(1, closed.get());
        assertEquals(1, telemetry.events.size());
        assertNotEquals(unrelatedId, telemetry.events.get(0).correlation().spanId());
        assertEquals("cancelled", telemetry.events.get(0).attributes().get("teaql.sql.outcome"));
        assertEquals(1, telemetry.completed.size());
        unrelated.success();
        assertNull(telemetry.active.get());
    }

    @Test public void driverFailureIsNotReplacedAndUnknownCountsAreNotInvented() throws Exception {
        var failure = new IllegalStateException("DRIVER-PRIVATE-CANARY");
        var db = database((proxy, method, args) -> { throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> db.query(context, SQL, new Object[]{"DRIVER-PRIVATE-CANARY"}, BINDINGS)));
        assertEquals(1, telemetry.events.size());
        assertEquals("failure", telemetry.events.get(0).attributes().get("teaql.sql.outcome"));
        assertFalse(telemetry.events.get(0).attributes().containsKey("teaql.sql.result_count"));
        assertFalse(telemetry.events.toString().contains("DRIVER-PRIVATE-CANARY"));
        assertEquals(List.of("failure"), telemetry.completed);
        assertNull(telemetry.active.get());
    }

    @Test public void onePhysicalBatchHasOneIdentityAndSeparateMemberOutcomes() throws Exception {
        var db = database((proxy, method, args) -> new int[]{1, java.sql.Statement.SUCCESS_NO_INFO});
        assertArrayEquals(new int[]{1, -2}, db.batchUpdate(context, "UPDATE school SET name = ?",
                List.of(new Object[]{"BATCH-A-CANARY"}, new Object[]{"BATCH-B-CANARY"}), BINDINGS));
        assertEquals(2, telemetry.events.size());
        assertEquals(telemetry.events.get(0).correlation(), telemetry.events.get(1).correlation());
        assertEquals("success", telemetry.events.get(1).attributes().get("teaql.sql.outcome"));
        assertFalse(telemetry.events.get(1).attributes().containsKey("teaql.sql.affected_rows"));
        assertEquals(1, telemetry.completed.size());
        assertEquals(0, ordinaryLogs.get());
    }

    private static final class Recording implements RuntimeTelemetry {
        private final AtomicInteger next = new AtomicInteger();
        private final ThreadLocal<String> active = new ThreadLocal<>();
        private final List<Diagnostic> events = new CopyOnWriteArrayList<>();
        private final List<String> completed = new CopyOnWriteArrayList<>();
        @Override public boolean recordsSqlDiagnostics() { return true; }
        @Override public void recordDiagnostic(Diagnostic event) { events.add(event); }
        @Override public Scope start(Operation operation) {
            String id = String.format("%016x", next.incrementAndGet());
            String previous = active.get();
            active.set(id);
            return new Scope() {
                private boolean ended;
                private boolean detached;
                @Override public SqlExecutionCorrelation correlation() { return new SqlExecutionCorrelation("00000000000000000000000000000001", id); }
                @Override public void success(Map<String, Object> attrs) { finish("success"); }
                @Override public void failure(Throwable error) { finish("failure"); }
                @Override public void detach() { if (!detached) { active.set(previous); detached = true; } }
                @Override public PropagationScope activate() {
                    String before = active.get(); active.set(id); return () -> active.set(before);
                }
                private void finish(String result) { if (!ended) { ended = true; completed.add(result); detach(); } }
            };
        }
    }
}
