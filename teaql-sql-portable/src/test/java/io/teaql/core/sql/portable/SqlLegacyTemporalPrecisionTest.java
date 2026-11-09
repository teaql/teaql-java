package io.teaql.core.sql.portable;

import static org.junit.Assert.*;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.reflect.Proxy;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Legacy SQL carriers must never become a successful lossy read. */
public class SqlLegacyTemporalPrecisionTest {
    private static final LocalDateTime LEGACY = LocalDateTime.of(2026, 11, 10, 12, 34, 56, 123456789);
    private static final LocalDateTime ALIGNED = LEGACY.withNano(123000000);

    public static final class TemporalRow extends BaseEntity {
        static {
            FieldLayout.installGenerated(FieldLayout.generated(TemporalRow.class, "legacy-sql-time-v1",
                    Map.of("id", 0, "version", 1, "happened_at", 2),
                    Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                            "happened_at", List.of("happenedAt", "event_time")), Set.of()));
        }
        LocalDateTime happenedAt;
        @Override public String typeName() { return "TemporalRow"; }
        @Override public Object __internalGet(String name) {
            return "happenedAt".equals(name) ? happenedAt : super.__internalGet(name);
        }
        @Override public void __internalSet(String name, Object value) {
            if ("happenedAt".equals(name)) { happenedAt = (LocalDateTime) value; markPropertyLoaded(name); }
            else super.__internalSet(name, value);
        }
    }

    private static final class Request extends BaseRequest<TemporalRow> {
        Request() {
            super(TemporalRow.class);
            selectProperty("id"); selectProperty("version"); selectProperty("happenedAt");
            offset(0, 1);
            internalComment("read the legacy SQL precision boundary");
            internalPurpose("reject incompatible stored precision without rewriting data");
        }
        @Override public String getTypeName() { return "TemporalRow"; }
    }

    private record Fixture(PortableSQLRepository<TemporalRow> repository, DefaultUserContext context) {
        SmartList<TemporalRow> load() { return repository.loadInternal(context, new Request()); }
    }

    private Fixture fixture(TeaQLDatabase database) {
        new TemporalRow();
        var descriptor = new SQLEntityDescriptor();
        descriptor.setType("TemporalRow"); descriptor.setTargetType(TemporalRow.class);
        descriptor.setEntitySupplier(TemporalRow::new); descriptor.setDataService("legacy-time-test");
        descriptor.with("table_name", "temporal_data"); descriptor.setAuditMaskFields(List.of());
        for (String name : List.of("id", "version", "happenedAt")) {
            var property = (GenericSQLProperty) descriptor.addSimpleProperty(name,
                    name.equals("happenedAt") ? LocalDateTime.class : Long.class);
            property.setColumnName(name.equals("happenedAt") ? "event_time" : name);
            property.setColumnType(name.equals("happenedAt") ? "TIMESTAMP" : "BIGINT");
        }
        var metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
        var runtime = TeaQLRuntime.builder().metadata(metadata).queryExecutionLogging(false).build();
        return new Fixture(new PortableSQLRepository<>(descriptor, database, null, metadata), new DefaultUserContext(runtime));
    }

    private Fixture fixture(Object value, boolean compiled, AtomicInteger mapped) {
        TeaQLDatabase database = (TeaQLDatabase) Proxy.newProxyInstance(TeaQLDatabase.class.getClassLoader(),
                new Class<?>[]{TeaQLDatabase.class}, (proxy, method, args) -> {
                    if (method.getName().equals("supportsCompiledRowMapping")) return compiled;
                    if (method.getName().equals("query")) {
                        mapped.incrementAndGet();
                        if (args.length == 5 && args[3] instanceof CompiledRowMapper<?> mapper) {
                            return List.of(mapper.map(new DataRow() {
                                @Override public Object get(int index) { return index < 3 ? 1L : value; }
                                @Override public <V> V get(int index, Class<V> type) { return type.cast(get(index)); }
                            }));
                        }
                        var row = new LinkedHashMap<String, Object>();
                        row.put("id", 1L); row.put("version", 1L); row.put("happenedAt", value);
                        return List.of(row);
                    }
                    throw new AssertionError("unexpected provider access: " + method);
                });
        return fixture(database);
    }

    private RuntimeException rejected(org.junit.function.ThrowingRunnable action, boolean fieldKnown) throws Exception {
        var error = assertThrows(TeaQLRuntimeException.class, action);
        assertEquals("SQL_DATETIME_PRECISION_MISMATCH", error.getClass().getMethod("getCode").invoke(error));
        assertEquals("milliseconds", error.getClass().getMethod("getExpectedPrecision").invoke(error));
        assertEquals(true, error.getClass().getMethod("isMigrationRequired").invoke(error));
        var remedy = (String) error.getClass().getMethod("getSuggestedFix").invoke(error);
        assertTrue(remedy.toLowerCase(Locale.ROOT).contains("migrat"));
        assertFalse(error.toString().contains("2026-11-10"));
        assertNull("the rejected private value must not survive in a cause", error.getCause());
        if (fieldKnown) {
            assertEquals("TemporalRow", error.getClass().getMethod("getEntityType").invoke(error));
            assertEquals("happenedAt", error.getClass().getMethod("getField").invoke(error));
            assertEquals("/happenedAt", error.getClass().getMethod("getPath").invoke(error));
        }
        return error;
    }

    @Test public void nativeSubMillisecondSqlValuesAreRejectedWithoutChangingThem() throws Exception {
        for (var time : List.of(LEGACY, LEGACY.withNano(1), LEGACY.withNano(999999999),
                LocalDateTime.of(1960, 2, 29, 23, 59, 59, 999999999))) {
            int before = time.getNano();
            rejected(() -> PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, time), false);
            assertEquals(before, time.getNano());
        }
    }
    @Test public void timestampSubMillisecondSqlValuesAreRejectedWithoutChangingThem() throws Exception {
        var timestamp = java.sql.Timestamp.valueOf(LEGACY);
        rejected(() -> PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, timestamp), false);
        assertEquals(123456789, timestamp.getNanos());
    }
    @Test public void textualAndVendorSqlValuesAreRejectedWithoutEmbeddingPrivateValues() throws Exception {
        for (Object carrier : List.of("2026-11-10 12:34:56.123456789", "2026-11-10T12:34:56.000000001",
                new Object() { @Override public String toString() { return "2026-11-10 12:34:56.999999999"; } })) {
            rejected(() -> PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, carrier), false);
        }
    }
    @Test public void compiledMapperReportsTheModeledFieldOnColdAndCachedPlans() throws Exception {
        var mapped = new AtomicInteger(); var fixture = fixture(LEGACY, true, mapped);
        rejected(fixture::load, true); rejected(fixture::load, true);
        assertEquals(2, mapped.get());
    }
    @Test public void fallbackMapperReportsTheModeledFieldInsteadOfReturningLoadedNull() throws Exception {
        var mapped = new AtomicInteger(); var fixture = fixture("2026-11-10 12:34:56.123456789", false, mapped);
        rejected(fixture::load, true); assertEquals(1, mapped.get());
    }
    @Test public void authoritativeReadbackReportsTheSameField() throws Exception {
        var mapped = new AtomicInteger(); var fixture = fixture(LEGACY, false, mapped);
        rejected(() -> fixture.repository.loadPersistedById(fixture.context, 1L), true);
        assertEquals(1, mapped.get());
    }
    @Test public void alignedAndNullSqlValuesKeepTheirSuccessfulLoadedStates() {
        for (boolean compiled : List.of(false, true)) {
            var row = fixture(ALIGNED, compiled, new AtomicInteger()).load().get(0);
            assertSame(ALIGNED, row.happenedAt); assertTrue(row.isPropertyLoaded("happenedAt"));
            var empty = fixture(null, compiled, new AtomicInteger()).load().get(0);
            assertNull(empty.happenedAt); assertTrue(empty.isPropertyLoaded("happenedAt"));
        }
        assertSame(ALIGNED, PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, ALIGNED));
        assertNull(PortableSQLRepository.convertTemporalColumnValue(LocalDateTime.class, null));
    }
    @Test public void realLegacySqliteRowRemainsUnchangedAfterRejectedQueryAndReadback() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE temporal_data(id INTEGER PRIMARY KEY, version INTEGER, event_time TEXT)");
                // Deliberately incompatible legacy fixture, not application bootstrap data.
                statement.execute("INSERT INTO temporal_data VALUES(1,1,'2026-11-10 12:34:56.123456789')");
            }
            var queries = new AtomicInteger();
            TeaQLDatabase database = (TeaQLDatabase) Proxy.newProxyInstance(TeaQLDatabase.class.getClassLoader(),
                    new Class<?>[]{TeaQLDatabase.class}, (proxy, method, args) -> {
                        if (method.getName().equals("supportsCompiledRowMapping")) return false;
                        if (!method.getName().equals("query")) throw new AssertionError("no writes allowed: " + method);
                        queries.incrementAndGet();
                        try (var statement = connection.prepareStatement((String) args[1])) {
                            var parameters = (Object[]) args[2];
                            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
                            try (var rows = statement.executeQuery()) {
                                var result = new ArrayList<Map<String, Object>>();
                                while (rows.next()) {
                                    var row = new LinkedHashMap<String, Object>();
                                    for (int i = 1; i <= rows.getMetaData().getColumnCount(); i++)
                                        row.put(rows.getMetaData().getColumnLabel(i), rows.getObject(i));
                                    result.add(row);
                                }
                                return result;
                            }
                        }
                    });
            var fixture = fixture(database);
            rejected(fixture::load, true);
            rejected(() -> fixture.repository.loadPersistedById(fixture.context, 1L), true);
            assertEquals(2, queries.get());
            try (var statement = connection.createStatement();
                    var rows = statement.executeQuery("SELECT event_time, total_changes() FROM temporal_data")) {
                assertTrue(rows.next()); assertEquals("2026-11-10 12:34:56.123456789", rows.getString(1));
                assertEquals(1, rows.getInt(2)); assertFalse(rows.next());
            }
        }
    }
}
