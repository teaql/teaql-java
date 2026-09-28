package io.teaql.dataservice.sql;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.*;
import io.teaql.runtime.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Test the actual adapter installed by SqlDataServiceExecutor, without exposing a workspace API. */
public class SqlFailureMaskingTest {
    private static final String SQL = "SELECT * FROM customer WHERE name = ? AND address = ? AND password = ?";
    private static final Object[] ARGS = {"Riverside", "1 Runtime Road", "PASSWORD-CANARY"};
    private static final SqlLogBindings BINDINGS = new SqlLogBindings(List.of(
            SqlParameterLogPolicy.MASKED, SqlParameterLogPolicy.PLAIN, SqlParameterLogPolicy.CREDENTIAL), true);

    private TeaQLDatabase database(RuntimeException error) throws Exception {
        SqlExecutionAdapter adapter = (SqlExecutionAdapter) Proxy.newProxyInstance(
                SqlExecutionAdapter.class.getClassLoader(), new Class<?>[]{SqlExecutionAdapter.class},
                (proxy, method, args) -> { throw error; });
        var executor = new SqlDataServiceExecutor("sql", adapter);
        var factory = SqlDataServiceExecutor.class.getDeclaredMethod("createPortableService", io.teaql.core.meta.EntityMetaFactory.class);
        factory.setAccessible(true);
        var service = factory.invoke(executor, new SimpleEntityMetaFactory());
        var field = PortableSQLDataService.class.getDeclaredField("database");
        field.setAccessible(true);
        return (TeaQLDatabase) field.get(service);
    }

    private void failure(String operation, boolean enabled, boolean brokenSink) throws Exception {
        failure(operation, enabled, brokenSink, false);
    }

    private void failure(String operation, boolean enabled, boolean brokenSink, boolean cancelled) throws Exception {
        RuntimeException driverError = cancelled
                ? new java.util.concurrent.CancellationException("driver exposes PASSWORD-CANARY Riverside")
                : new IllegalStateException("driver exposes PASSWORD-CANARY Riverside");
        var logs = new ArrayList<ExecutionMetadata>();
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .logSink((caller, metadata) -> {
                    logs.add(metadata);
                    if (brokenSink) throw new IllegalArgumentException("sink unavailable");
                }).build()) {
            @Override public boolean isQueryExecutionLoggingEnabled() { return enabled; }
            @Override public boolean isMutationExecutionLoggingEnabled() { return enabled; }
        };
        context.pushTrace(TraceKind.COMMENT, "Customer", "what: run diagnostic fixture");
        context.pushTrace(TraceKind.PURPOSE, "Customer", "why: verify failed SQL");
        context.pushTrace(TraceKind.AUDIT_REASON, "Customer", "test error handling");
        var db = database(driverError);
        var actual = assertThrows(RuntimeException.class, () -> {
            switch (operation) {
                case "query" -> db.query(context, SQL, ARGS, BINDINGS);
                case "typed" -> db.query(context, SQL, ARGS, (CompiledRowMapper<BaseEntity>) null, BINDINGS);
                case "update" -> db.executeUpdate(context, SQL, ARGS, BINDINGS);
                case "ddl" -> db.execute(context, "CREATE TABLE broken (name TEXT)");
                default -> throw new AssertionError(operation);
            }
        });
        assertSame(driverError, actual);
        assertEquals(enabled ? 1 : 0, logs.size());
        if (enabled) {
            var safe = logs.get(0);
            assertEquals(cancelled ? "cancelled" : "failure", safe.getExecutionOutcome());
            assertEquals("what: run diagnostic fixture", safe.getComment());
            assertEquals("why: verify failed SQL", safe.getPurpose());
            assertEquals("test error handling", safe.getAuditReason());
            assertTrue(safe.getTraceChain().stream().anyMatch(node -> node.getKind() == TraceKind.SQL));
            assertNull(safe.getResultCount());
            assertNull(safe.getAffectedRows());
            assertFalse(String.valueOf(safe.getResultSummary()).contains("PASSWORD-CANARY"));
            assertFalse(safe.getDebugQuery().contains("PASSWORD-CANARY"));
            if (!operation.equals("ddl")) {
                assertTrue(safe.getDebugQuery(), safe.getDebugQuery().contains("Ri*****de"));
                assertTrue(safe.getDebugQuery().contains("1 Runtime Road"));
                assertTrue(safe.getDebugQuery().contains("NOT REPLAYABLE"));
                assertFalse(safe.getDebugQuery().contains("Riverside"));
            }
        }
        assertArrayEquals(new Object[]{"Riverside", "1 Runtime Road", "PASSWORD-CANARY"}, ARGS);
    }

    @Test public void queryFailure() throws Exception { failure("query", true, false); }
    @Test public void typedFailure() throws Exception { failure("typed", true, false); }
    @Test public void updateFailure() throws Exception { failure("update", true, false); }
    @Test public void ddlFailure() throws Exception { failure("ddl", true, false); }
    @Test public void queryDisabled() throws Exception { failure("query", false, false); }
    @Test public void updateDisabled() throws Exception { failure("update", false, false); }
    @Test public void queryBrokenSink() throws Exception { failure("query", true, true); }
    @Test public void updateBrokenSink() throws Exception { failure("update", true, true); }
    @Test public void queryCancelled() throws Exception { failure("query", true, false, true); }
    @Test public void updateCancelled() throws Exception { failure("update", true, false, true); }

    @Test public void inheritedReadbackFailure() throws Exception { inheritedReadback(false, true); }
    @Test public void inheritedReadbackCancelled() throws Exception { inheritedReadback(true, true); }
    @Test public void inheritedReadbackDisabled() throws Exception { inheritedReadback(false, false); }

    private void inheritedReadback(boolean cancelled, boolean enabled) throws Exception {
        RuntimeException error = cancelled ? new java.util.concurrent.CancellationException("DRIVER-CANARY")
                : new IllegalStateException("DRIVER-CANARY");
        var logs = new ArrayList<ExecutionMetadata>();
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .logSink((caller, metadata) -> logs.add(metadata)).build()) {
            @Override public boolean isQueryExecutionLoggingEnabled() { return enabled; }
        };
        context.pushTrace(TraceKind.AUDIT_REASON, "Customer", "read Riverside PASSWORD-CANARY snapshot");
        var provenance = new SqlIntentRedactions();
        provenance.capture(BINDINGS.policies(), ARGS);
        var bindings = new SqlLogBindings(List.of(SqlParameterLogPolicy.PLAIN), true, null, provenance);
        var db = database(error);
        assertSame(error, assertThrows(RuntimeException.class,
                () -> db.query(context, "SELECT * FROM customer WHERE id = ?", new Object[]{1L}, bindings)));
        assertEquals(enabled ? 1 : 0, logs.size());
        if (enabled) {
            var entry = logs.get(0);
            assertEquals("read [REDACTED] [REDACTED] snapshot", entry.getAuditReason());
            assertEquals(cancelled ? "cancelled" : "failure", entry.getExecutionOutcome());
            assertNull(entry.getResultCount());
            assertNull(entry.getIntentRedactions());
            assertTrue(entry.getDebugQuery().contains("id = 1"));
        }
    }
}
