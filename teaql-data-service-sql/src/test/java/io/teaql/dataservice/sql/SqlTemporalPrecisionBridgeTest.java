package io.teaql.dataservice.sql;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.portable.*;
import io.teaql.runtime.*;
import java.lang.reflect.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Modeled SQL precision, not a change to native objects or JSON codecs. */
public class SqlTemporalPrecisionBridgeTest {
    private static final String SQL = "SELECT happened_at FROM temporal WHERE happened_at = ? LIMIT 1";
    private static final LocalDateTime INPUT = LocalDateTime.of(2026, 11, 10, 12, 34, 56, 123456789);
    private static final LocalDateTime EXPECTED = INPUT.truncatedTo(ChronoUnit.MILLIS);
    private static final SqlLogBindings BINDINGS = new SqlLogBindings(List.of(SqlParameterLogPolicy.PLAIN), true);
    private final List<ExecutionMetadata> logs = new ArrayList<>();
    private final DefaultUserContext context = new DefaultUserContext(TeaQLRuntime.builder()
            .metadata(new SimpleEntityMetaFactory())
            .logSink((caller, value) -> logs.add(value)).build());

    private TeaQLDatabase database(InvocationHandler handler) throws Exception {
        var adapter = (SqlExecutionAdapter) Proxy.newProxyInstance(SqlExecutionAdapter.class.getClassLoader(),
                new Class<?>[]{SqlExecutionAdapter.class}, handler);
        var factory = SqlDataServiceExecutor.class.getDeclaredMethod("createPortableService", EntityMetaFactory.class);
        factory.setAccessible(true);
        var service = factory.invoke(new SqlDataServiceExecutor("sql", adapter), new SimpleEntityMetaFactory());
        var field = PortableSQLDataService.class.getDeclaredField("database");
        field.setAccessible(true);
        return (TeaQLDatabase) field.get(service);
    }

    private TeaQLDatabase checking() throws Exception {
        return database((proxy, method, args) -> {
            if (method.getName().equals("supportsCompiledStreamMapping")) return true;
            Object value = args[1];
            if (value instanceof List<?> batch) {
                assertEquals(EXPECTED, ((Object[]) batch.get(0))[0]);
                return new int[]{1};
            }
            assertEquals(EXPECTED, ((Object[]) value)[0]);
            return switch (method.getName()) {
                case "update" -> 1;
                case "queryForStream" -> Stream.empty();
                default -> List.of();
            };
        });
    }

    private interface Call { void run(TeaQLDatabase db, Object[] args) throws Exception; }
    private void check(Call call) throws Exception {
        Object[] original = {INPUT};
        call.run(checking(), original);
        assertSame(INPUT, original[0]);
        assertEquals(123456789, INPUT.getNano());
    }

    @Test public void directQueryBindsMilliseconds() throws Exception {
        check((db,args) -> db.query(SQL,args));
    }
    @Test public void observedMapQueryBindsMillisecondsAndLogsEffectiveValues() throws Exception {
        check((db,args) -> db.query(context,SQL,args,BINDINGS));
        assertEquals(EXPECTED, logs.get(0).getParameters().get(0));
    }
    @Test public void observedCompiledQueryBindsMilliseconds() throws Exception {
        check((db,args) -> db.query(context,SQL,args,row -> new BaseEntity(),BINDINGS));
    }
    @Test public void mapStreamBindsMilliseconds() throws Exception {
        check((db,args) -> { try (var stream=db.queryForStream(context,SQL,args,BINDINGS)) { assertEquals(0,stream.count()); } });
    }
    @Test public void compiledStreamBindsMilliseconds() throws Exception {
        check((db,args) -> { try (var stream=db.queryForStream(context,SQL,args,row -> new BaseEntity(),BINDINGS)) { assertEquals(0,stream.count()); } });
    }
    @Test public void directUpdateBindsMilliseconds() throws Exception {
        check((db,args) -> assertEquals(1,db.executeUpdate("UPDATE temporal SET happened_at = ?",args)));
    }
    @Test public void observedUpdateBindsMilliseconds() throws Exception {
        check((db,args) -> assertEquals(1,db.executeUpdate(context,"UPDATE temporal SET happened_at = ?",args,BINDINGS)));
        assertEquals(EXPECTED,logs.get(0).getParameters().get(0));
    }
    @Test public void directBatchUsesDetachedMillisecondRows() throws Exception {
        check((db,args) -> assertArrayEquals(new int[]{1},db.batchUpdate("UPDATE temporal SET happened_at = ?",Collections.singletonList(args))));
    }
    @Test public void observedBatchUsesTheSameValuesForDriverAndLog() throws Exception {
        check((db,args) -> assertArrayEquals(new int[]{1},db.batchUpdate(context,"UPDATE temporal SET happened_at = ?",Collections.singletonList(args),BINDINGS)));
        assertEquals(EXPECTED,logs.get(0).getParameters().get(0));
    }
    @Test public void alignedAndNullArgumentsKeepTheExistingFastPath() throws Exception {
        Object[] aligned={EXPECTED,null,"unchanged",17L};
        var db=database((proxy,method,args) -> { assertSame(aligned,args[1]);return List.of(); });
        db.query(SQL,aligned);
    }
    @Test public void truncationDoesNotRoundAcrossASecondOrChangePreEpochWallTime() throws Exception {
        for (var value:List.of(LocalDateTime.of(1960,2,29,23,59,59,999999999),
                LocalDateTime.of(2024,3,10,2,30,0,999999),LocalDateTime.of(2026,12,31,23,59,59,999999999))) {
            var db=database((proxy,method,args) -> { assertEquals(value.truncatedTo(ChronoUnit.MILLIS),((Object[])args[1])[0]);return List.of(); });
            Object[] original={value};db.query(SQL,original);assertSame(value,original[0]);
        }
    }
    @Test public void failureRetainsTheDriverErrorAndCanonicalSqlMetadata() throws Exception {
        var failure=new IllegalStateException("controlled driver failure");
        var db=database((proxy,method,args) -> { assertEquals(EXPECTED,((Object[])args[1])[0]);throw failure; });
        assertSame(failure,assertThrows(IllegalStateException.class,()->db.query(context,SQL,new Object[]{INPUT},BINDINGS)));
        assertEquals(EXPECTED,logs.get(0).getParameters().get(0));
    }
    private Object read(Object value) throws Exception {
        var method=PortableSQLRepository.class.getDeclaredMethod("convertTemporalColumnValue",Class.class,Object.class);
        method.setAccessible(true);return method.invoke(null,LocalDateTime.class,value);
    }
    @Test public void nativeSqlDateTimeReadUsesMillisecondsWithoutMutatingTheNativeInput() throws Exception {
        assertEquals(EXPECTED,read(INPUT));assertEquals(123456789,INPUT.getNano());
    }
    @Test public void jdbcTimestampReadUsesTheSameMillisecondPolicy() throws Exception {
        var timestamp=java.sql.Timestamp.valueOf(INPUT);
        assertEquals(EXPECTED,read(timestamp));assertEquals(123456789,timestamp.getNanos());
    }
    @Test public void textualSqlFallbackReadUsesTheSameMillisecondPolicy() throws Exception {
        assertEquals(EXPECTED,read("2026-11-10 12:34:56.123456789"));
    }
    @Test public void copyPasteDebugSqlRepresentsTheEffectiveMillisecondBinding() {
        assertEquals("SELECT '2026-11-10 12:34:56.123'",SqlDataServiceExecutor.debugSql("SELECT ?",new Object[]{INPUT}));
        assertEquals(123456789,INPUT.getNano());
    }
}
