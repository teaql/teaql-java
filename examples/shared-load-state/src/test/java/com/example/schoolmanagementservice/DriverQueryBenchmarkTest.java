package com.example.schoolmanagementservice;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sqlite.SQLiteDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in matched JDBC track. Both lanes open/close one connection per query. */
@EnabledIfSystemProperty(named = "teaql.driverBenchmark", matches = "true")
class DriverQueryBenchmarkTest {
    record Row(Long id, Long version, String name, String note) {}
    record StatementInput(String sql, Object[] params) {}
    record Sample<T>(T value, long bytes, long nanos) {}
    static volatile Object consumed;
    static final class Capture extends JdbcSqlExecutor {
        final List<StatementInput> inputs = new ArrayList<>();
        Capture(SQLiteDataSource source) { super(source); }
        @Override public <T extends Entity> List<T> query(String sql, Object[] params, CompiledRowMapper<T> mapper) {
            inputs.add(new StatementInput(sql, params.clone()));
            return super.query(sql, params, mapper);
        }
    }
    static <T> Sample<T> measure(com.sun.management.ThreadMXBean bean, Supplier<T> action) {
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread), start = System.nanoTime();
        T value = action.get();
        long nanos = System.nanoTime() - start;
        consumed = value;
        return new Sample<>(value, bean.getThreadAllocatedBytes(thread)-before, nanos);
    }
    static List<Row> nativeQuery(SQLiteDataSource source, StatementInput input, boolean full) {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(input.sql())) {
            for (int i=0; i<input.params().length; i++) statement.setObject(i+1, input.params()[i]);
            try (ResultSet cursor = statement.executeQuery()) {
                List<Row> result = new ArrayList<>();
                while (cursor.next()) result.add(new Row(cursor.getLong(1), cursor.getLong(2),
                        cursor.getString(3), full ? cursor.getString(4) : null));
                return result;
            }
        } catch (SQLException error) { throw new IllegalStateException(error); }
    }
    static void report(String lane, boolean full, int count, List<? extends Sample<?>> samples) {
        for (int i=0; i<samples.size(); i++) {
            var sample=samples.get(i);
            System.out.printf("SAMPLE,%s,%s,%d,%d,%d,%d%n",lane,full,count,i,sample.nanos(),sample.bytes());
        }
        long[] times = samples.stream().mapToLong(Sample::nanos).sorted().toArray();
        long[] bytes = samples.stream().mapToLong(Sample::bytes).sorted().toArray();
        long total = Arrays.stream(times).sum();
        System.out.printf(Locale.ROOT,"%s,%s,%d,%d,%d,%d,%.2f,%d%n", lane,
                full ? "full" : "sparse",count,samples.size(),times[times.length/2],
                times[(int)Math.ceil(times.length*0.95)-1],samples.size()*1e9/total,bytes[bytes.length/2]);
    }
    @Test void matchedTypedDriverQueries() throws Exception {
        assertNotNull(HydrationAllocationTest.Probe.LAYOUT);
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new SQLEntityDescriptor(); descriptor.setType("Probe");
        descriptor.setTargetType(HydrationAllocationTest.Probe.class);
        descriptor.setEntitySupplier(HydrationAllocationTest.Probe::new);
        for (String field : List.of("id","version","name","note")) {
            var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                    field.equals("id") || field.equals("version") ? Long.class : String.class);
            property.setColumnType(field.equals("id") || field.equals("version") ? "BIGINT" : "VARCHAR(255)");
        }
        metadata.register(descriptor);
        var source = new SQLiteDataSource();
        var database = Files.createTempDirectory("teaql-java-driver-").resolve("probe.sqlite");
        source.setUrl("jdbc:sqlite:"+database);
        System.out.println("DATABASE "+database);
        var sql = new JdbcSqlExecutor(source);
        var service = new SqliteDataServiceExecutor("sqlite",sql,source);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("default",service).queryExecutionLogging(false).build());
        context.ensureSchema();
        // Synthetic fixture only. Business/bootstrap save alternatives are not
        // under test, and raw fixture setup is excluded from all measurements.
        try (Connection connection = source.getConnection()) {
            System.out.println("DRIVER_VERSION," + connection.getMetaData().getDriverName() + "," + connection.getMetaData().getDriverVersion());
            try (Statement version = connection.createStatement(); ResultSet row = version.executeQuery("SELECT sqlite_version()")) {
                assertTrue(row.next()); System.out.println("ENGINE_VERSION,sqlite," + row.getString(1));
            }
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO probe_data(id,version,name,note) VALUES(?,?,?,?)")) {
                for (long id=1; id<=10_000; id++) {
                    insert.setLong(1,id); insert.setLong(2,1); insert.setString(3,"row-"+id);
                    insert.setString(4,id%2==0 ? null : "nullable note"); insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
        }
        var capture = new Capture(source);
        var captureService = new SqliteDataServiceExecutor("sqlite",capture,source);
        var observed = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("default",captureService).queryExecutionLogging(false).build());
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported()); bean.setThreadAllocatedMemoryEnabled(true);
        var warm = new HydrationAllocationTest.Request(metadata,false,1);
        warm.addOrderByAscending("id");
        var warmQuery = warm.purpose("why: warm JIT and runtime caches");
        for (int i=0; i<4_000; i++) consumed = warmQuery.executeForList(context);
        System.out.println("case,projection,rows,samples,p50_ns,p95_ns,queries_per_second,allocated_bytes");
        for (boolean full : List.of(false,true)) for (int count : new int[]{1,100,10_000}) {
            var request = new HydrationAllocationTest.Request(metadata,full,count);
            request.addOrderByAscending("id");
            var executable = request.purpose("why: isolate governed runtime query overhead");
            capture.inputs.clear();
            assertEquals(count,executable.executeForList(observed).size());
            assertEquals(1,capture.inputs.size());
            var input = capture.inputs.get(0);
            System.out.println("SQL_MATCH "+input.sql()+" "+Arrays.toString(input.params()));
            Supplier<List<Row>> nativeAction = () -> nativeQuery(source,input,full);
            Supplier<SmartList<HydrationAllocationTest.Probe>> runtimeAction = () -> executable.executeForList(context);
            for (int i=0; i<20; i++) { consumed=nativeAction.get(); consumed=runtimeAction.get(); }
            // Keep metrics, not all entity graphs: retaining 31 result lists
            // would distort heap/GC pressure compared with a normal consumer.
            List<Sample<Void>> nativeSamples = new ArrayList<>();
            List<Sample<Void>> runtimeSamples = new ArrayList<>();
            for (int i=0; i<31; i++) {
                Sample<List<Row>> raw;
                Sample<SmartList<HydrationAllocationTest.Probe>> typed;
                if (i%2==0) { raw=measure(bean,nativeAction); typed=measure(bean,runtimeAction); }
                else { typed=measure(bean,runtimeAction); raw=measure(bean,nativeAction); }
                assertEquals(count,raw.value().size()); assertEquals(count,typed.value().size());
                LoadState state = typed.value().get(0).__internalLoadState();
                for (int row=0; row<count; row++) {
                    var entity=typed.value().get(row); var nativeRow=raw.value().get(row);
                    assertEquals(nativeRow,new Row(entity.getId(),entity.getVersion(),entity.name,entity.note));
                    assertEquals(full,entity.isPropertyLoaded("note"));
                    assertSame(state,entity.__internalLoadState());
                    assertFalse(entity.__internalHasMutationLedger());
                }
                nativeSamples.add(new Sample<>(null,raw.bytes(),raw.nanos()));
                runtimeSamples.add(new Sample<>(null,typed.bytes(),typed.nanos()));
            }
            report("jdbc",full,count,nativeSamples); report("teaql",full,count,runtimeSamples);
        }
        System.out.println("PASS matched JDBC typed results and shared snapshots; log-off; per-query connections");
    }
}
