package io.teaql.examples.security;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Runtime-owned fixture, not a hand-edited generated library. */
final class SqlMaskingLifecycleExample {
    static void verify() throws Exception {
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new SQLEntityDescriptor();
        descriptor.setType("MaskFixture"); descriptor.setTargetType(MaskFixture.class);
        descriptor.setEntitySupplier(MaskFixture::new); descriptor.setDataService("sqlite");
        descriptor.addSimpleProperty("id", Long.class);
        descriptor.addSimpleProperty("version", Long.class);
        descriptor.addSimpleProperty("name", String.class);
        descriptor.setAuditMaskFields(List.of("name"));
        descriptor.with("table_name", "mask_fixture_data"); metadata.register(descriptor);
        var ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-java-mask-example-", ".db"));
        var provider = new SqliteDataServiceExecutor("sqlite", new JdbcSqlExecutor(ds), ds);
        var logs = new ArrayList<ExecutionMetadata>();
        var sequence = new AtomicLong(10);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("sqlite", provider).idGenerationService((caller, entity) -> sequence.getAndIncrement())
                .logSink((caller, entry) -> logs.add(entry)).build());

        assertThrows(RuntimeException.class, () -> query().executeForStream(context));
        assertEquals(1, logs.size());
        assertEquals("failure", logs.get(0).getExecutionOutcome());
        assertNull(logs.get(0).getResultCount());
        context.ensureSchema();
        for (int i = 0; i < 3; i++) {
            logs.clear();
            new MaskFixture().updateName("Riverside").auditAs("seed masking example Riverside").save(context);
            var readback = logs.stream().filter(e -> e.getOperation() == DataServiceOperation.QUERY
                    && String.valueOf(e.getParameterizedQuery()).contains("SELECT *")).findFirst().orElseThrow();
            assertEquals("seed masking example [REDACTED]", readback.getAuditReason());
            assertFalse(readback.getDebugQuery().contains("Riverside"));
            assertEquals(Integer.valueOf(1), readback.getResultCount());
        }
        MaskFixture changed;
        try (var rows = query().executeForStream(context)) { changed = rows.findFirst().orElseThrow(); }
        for (String name : List.of("Lakeside", "Riverside")) {
            logs.clear();
            changed.updateName(name).auditAs("update masked fixture " + name).save(context);
            var readback = logs.stream().filter(e -> e.getOperation() == DataServiceOperation.QUERY
                    && String.valueOf(e.getParameterizedQuery()).contains("SELECT *")).findFirst().orElseThrow();
            assertEquals("update masked fixture [REDACTED]", readback.getAuditReason());
            assertNull(readback.getIntentRedactions());
        }
        // Test-only fault injection, not application persistence SQL.
        try (var conn = ds.getConnection(); var statement = conn.createStatement()) {
            statement.execute("CREATE TRIGGER disappear_mask_fixture AFTER INSERT ON mask_fixture_data WHEN NEW.id >= 13 BEGIN DELETE FROM mask_fixture_data WHERE id = NEW.id; END");
        }
        try {
            logs.clear();
            assertThrows(RuntimeException.class, () -> new MaskFixture().updateName("READBACK-CANARY")
                    .auditAs("verify missing READBACK-CANARY snapshot").save(context));
            var readback = logs.stream().filter(e -> e.getOperation() == DataServiceOperation.QUERY
                    && String.valueOf(e.getParameterizedQuery()).contains("SELECT *")).findFirst().orElseThrow();
            assertEquals("success", readback.getExecutionOutcome());
            assertEquals(Integer.valueOf(0), readback.getResultCount());
            assertEquals("verify missing [REDACTED] snapshot", readback.getAuditReason());
            assertTrue(logs.stream().anyMatch(e -> e.getOperation() == DataServiceOperation.MUTATION
                    && "success".equals(e.getExecutionOutcome()) && Long.valueOf(1).equals(e.getAffectedRows())));
        } finally {
            try (var conn = ds.getConnection(); var statement = conn.createStatement()) {
                statement.execute("DROP TRIGGER disappear_mask_fixture");
            }
        }
        System.out.println("PASS Java readback inherits sensitive intent for create/update and empty SQLite snapshot");
        logs.clear();
        var duplicateContext = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("sqlite", provider).idGenerationService((caller, entity) -> 10L)
                .logSink((caller, entry) -> logs.add(entry)).build());
        assertThrows(RuntimeException.class, () -> new MaskFixture().updateName("MASKED-DUPLICATE-CANARY")
                .auditAs("verify failed duplicate insert").save(duplicateContext));
        assertTrue(logs.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION));
        assertTrue(logs.stream().noneMatch(entry -> String.valueOf(entry.getDebugQuery()).contains("MASKED-DUPLICATE-CANARY")));
        logs.clear();
        try (var rows = query().executeForStream(context)) {
            assertEquals(1, rows.limit(1).count());
        }
        assertEquals(1, logs.size());
        assertEquals("cancelled", logs.get(0).getExecutionOutcome());
        assertEquals(Integer.valueOf(1), logs.get(0).getResultCount());
        assertEquals("verify cursor diagnostics", logs.get(0).getPurpose());
        assertTrue(logs.get(0).getDebugQuery().contains("Ri*****de"));
        assertFalse(logs.get(0).getDebugQuery().contains("Riverside"));
        logs.clear();
        try (var rows = query().executeForStream(context)) {
            assertEquals(List.of("Riverside", "Riverside", "Riverside"), rows.map(MaskFixture::getName).toList());
        }
        assertEquals(1, logs.size());
        assertEquals("success", logs.get(0).getExecutionOutcome());
        assertEquals(Integer.valueOf(3), logs.get(0).getResultCount());
        // Re-emitting a stored safe record must never upgrade it to debug plaintext.
        var retained = logs.get(0);
        var replay = LogPrivacy.sql(retained, true);
        assertEquals(retained.getDebugQuery(), replay.getDebugQuery());
        var output = Files.createTempFile("teaql-java-masked-replay-", ".log");
        try (var out = new java.io.PrintStream(Files.newOutputStream(output))) {
            new DefaultTextRuntimeLogSink(out).writeExecutionLog(context, replay);
        }
        String text = Files.readString(output);
        assertTrue(text.contains("Ri*****de"));
        assertFalse(text.contains("Riverside"));
        assertFalse(text.contains("DEBUG PLAINTEXT"));
        assertTrue(text.contains("verify cursor diagnostics"));
        System.out.println("PASS Java retained SQL projection: no plaintext upgrade, masked file output, intent preserved");
        System.out.println("PASS Java SQLite masking lifecycle: failed open/duplicate insert, early close, completion, intent and original data");
    }

    private static ExecutableRequest<MaskFixture> query() {
        var request = new FixtureRequest();
        request.setSize(3);
        request.appendSearchCriteria(request.createBasicSearchCriteria("name", Operator.EQUAL, "Riverside"));
        return request.comment("read masked fixture").purpose("verify cursor diagnostics");
    }

    public static final class FixtureRequest extends BaseRequest<MaskFixture> {
        FixtureRequest() { super(MaskFixture.class); }
        @Override public String getTypeName() { return "MaskFixture"; }
        public FixtureRequest comment(String text) { internalComment(text); return this; }
    }

    public static final class MaskFixture extends BaseEntity {
        private String name;
        @Override public String typeName() { return "MaskFixture"; }
        public String getName() { return name; }
        public MaskFixture updateName(String value) { handleUpdate("name", name, value); name = value; return this; }
        @Override public void __internalSet(String property, Object value) {
            if (property.equals("name")) name = (String) value; else super.__internalSet(property, value);
        }
        @Override public Object __internalGet(String property) {
            return property.equals("name") ? name : super.__internalGet(property);
        }
    }
}
