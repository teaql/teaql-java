package io.teaql.examples.security;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.*;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Real SQLite graph loading; handwritten runtime fixture, not generated source. */
@RunWith(Parameterized.class)
public class SqlRelationMaskingTest {
    @Parameterized.Parameters(name = "{0}-failure={1}-transaction={2}-sensitiveSink={3}")
    public static Collection<Object[]> cases() {
        var cases = new ArrayList<Object[]>();
        for (String shape : List.of("probe", "window", "forward", "nested", "idset", "aggregate", "facet"))
            for (boolean fail : List.of(false, true))
                for (boolean tx : List.of(false, true))
                    for (boolean sensitive : List.of(false, true)) cases.add(new Object[]{shape, fail, tx, sensitive});
        return cases;
    }
    private final String shape;
    private final boolean fail;
    private final boolean transaction;
    private final boolean sensitiveSink;
    public SqlRelationMaskingTest(String shape, boolean fail, boolean transaction, boolean sensitiveSink) {
        this.shape = shape; this.fail = fail; this.transaction = transaction;
        this.sensitiveSink = sensitiveSink;
    }
    public static abstract class Row extends BaseEntity {
        private final Map<String, Object> fields = new HashMap<>();
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("id") || field.equals("version")) super.__internalSet(field, value);
            else { fields.put(field, value); markPropertyLoaded(field); }
        }
        @Override public Object __internalGet(String field) {
            return field.equals("id") || field.equals("version") ? super.__internalGet(field) : fields.get(field);
        }
    }
    public static class Parent extends Row { @Override public String typeName() { return "MaskParent"; } }
    public static class Child extends Row { @Override public String typeName() { return "MaskChild"; } }
    public static class Leaf extends Row { @Override public String typeName() { return "MaskLeaf"; } }
    public static class Request<T extends Entity> extends BaseRequest<T> {
        private final String type;
        Request(Class<T> cls, String type) {
            super(cls); this.type = type; setSize(10);
            for (String field : List.of("id", "version", "name", "password")) selectProperty(field);
            if (type.equals("MaskChild")) selectProperty("parent");
            if (type.equals("MaskLeaf")) selectProperty("child");
        }
        @Override public String getTypeName() { return type; }
        Request<T> equal(String field, Object value) {
            appendSearchCriteria(createBasicSearchCriteria(field, Operator.EQUAL, value)); return this;
        }
        Request<T> comment(String value) { internalComment(value); return this; }
    }
    private static SQLEntityDescriptor descriptor(String type, Class<? extends Row> cls,
            java.util.function.Supplier<? extends Row> factory, String table) {
        var d = new SQLEntityDescriptor(); d.setType(type); d.setTargetType(cls);
        d.setEntitySupplier(factory); d.setDataService("sqlite"); d.with("table_name", table);
        d.addSimpleProperty("id", Long.class); d.addSimpleProperty("version", Long.class);
        d.addSimpleProperty("name", String.class); d.addSimpleProperty("password", String.class);
        d.setAuditMaskFields(List.of("name")); return d;
    }
    @Test public void inheritedIntentIsSafeAndGraphAndSqlFactsRemainIntact() throws Exception {
        var metadata = new SimpleEntityMetaFactory();
        var parent = descriptor("MaskParent", Parent.class, Parent::new, "mask_parent_data");
        var child = descriptor("MaskChild", Child.class, Child::new, "mask_child_data");
        metadata.register(parent);
        var relation = (GenericSQLRelation) child.addObjectProperty(metadata, "parent", "MaskParent",
                "children", Parent.class, GenericSQLRelation::new);
        relation.setTableName("mask_child_data"); relation.setColumnName("parent"); relation.setColumnType("BIGINT");
        metadata.register(child);
        var leaf = descriptor("MaskLeaf", Leaf.class, Leaf::new, "mask_leaf_data");
        var leafRelation = (GenericSQLRelation) leaf.addObjectProperty(metadata, "child", "MaskChild",
                "leaves", Child.class, GenericSQLRelation::new);
        leafRelation.setTableName("mask_leaf_data"); leafRelation.setColumnName("child"); leafRelation.setColumnType("BIGINT");
        metadata.register(leaf);
        var ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-java-relation-mask-", ".db"));
        var provider = new SqliteDataServiceExecutor("sqlite", new JdbcSqlExecutor(ds), ds);
        var logs = new ArrayList<ExecutionMetadata>();
        var ids = new AtomicLong(10);
        var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite", provider)
                .idGenerationService((c,e) -> ids.getAndIncrement()).logSink(new RuntimeLogSink() {
                    @Override public void writeExecutionLog(UserContext caller, ExecutionMetadata entry) { logs.add(entry); }
                    @Override public boolean requiresSensitiveSqlData() { return sensitiveSink; }
                }).build());
        context.ensureSchema();
        var p = new Parent(); p.updateProperty("name", "Riverside"); p.updateProperty("password", "RELATION-PASSWORD-CANARY");
        p.auditAs("seed relation fixture").save(context);
        var c = new Child(); c.updateProperty("name", "Riverside"); c.updateProperty("password", "RELATION-PASSWORD-CANARY");
        c.updateProperty("parent", p); c.auditAs("seed relation child").save(context);
        var l = new Leaf(); l.updateProperty("name", "leaf payload"); l.updateProperty("child", c);
        l.auditAs("seed nested leaf").save(context);
        boolean forward = shape.equals("forward") || shape.equals("facet");
        var root = forward ? new Request<>(Child.class, "MaskChild") : new Request<>(Parent.class, "MaskParent");
        root.equal("name", "Riverside").equal("password", "RELATION-PASSWORD-CANARY");
        var nested = forward ? new Request<>(Parent.class, "MaskParent") : new Request<>(Child.class, "MaskChild");
        nested.topNProbeParentThreshold(shape.equals("window") ? 0 : 32);
        if (shape.equals("nested")) nested.enhanceRelation("leaves", new Request<>(Leaf.class, "MaskLeaf"));
        if (shape.equals("facet")) root.addFacet("parentFacet", "parent", nested, true);
        else root.enhanceRelation(forward ? "parent" : "children", nested);
        if (shape.equals("aggregate")) {
            root.enhanceRelations().clear();
            nested.count("count"); nested.setPartitionProperty("parent");
            root.getDynamicAggregateAttributes().add(new SimpleAggregation("childCount", nested, true));
        }
        if (shape.equals("idset")) {
            root.optimizePaginationWithIdSet();
            root.comment("warm retained IDs").purpose("prime current request cache").executeForList(context);
        }
        if (fail) {
            // Fault injection only: seed data above uses the mutation API.
            try (var conn = ds.getConnection(); var statement = conn.createStatement()) {
                statement.execute("ALTER TABLE " + (forward ? "mask_parent_data" : "mask_child_data") + " RENAME TO missing_relation_data");
            }
        }
        logs.clear();
        Runnable execute = () -> {
            var rows = root.comment("load Riverside RELATION-PASSWORD-CANARY graph")
                    .purpose("inspect Riverside RELATION-PASSWORD-CANARY relations").executeForList(context);
            assertEquals(1, rows.size());
            assertEquals("Riverside", rows.get(0).getProperty("name"));
            if (shape.equals("aggregate")) assertEquals(1, ((Number) rows.get(0).getProperty("childCount")).intValue());
            else if (shape.equals("facet")) {
                SmartList<Entity> parents = rows.getFacet("parentFacet");
                assertEquals(1, parents.size());
                assertEquals("Riverside", parents.get(0).getProperty("name"));
            }
            else if (forward) {
                Entity loadedParent = rows.get(0).getProperty("parent");
                assertEquals("Riverside", loadedParent.getProperty("name"));
            }
            else {
                SmartList<Entity> children = rows.get(0).getProperty("children");
                assertEquals(1, children.size());
                if (shape.equals("nested")) {
                    SmartList<Entity> leaves = children.get(0).getProperty("leaves");
                    assertEquals(1, leaves.size());
                    assertEquals("leaf payload", leaves.get(0).getProperty("name"));
                }
            }
        };
        Runnable run = transaction ? () -> provider.executeInTransaction(context, () -> { execute.run(); return null; }) : execute;
        if (fail) assertThrows(RuntimeException.class, run::run); else run.run();
        // A facet first logs its grouped query, then loads the related rows. The injected
        // failure is in that final related-row query, so all three attempts are visible.
        assertEquals(shape.equals("facet") || (shape.equals("nested") && !fail) ? 3 : 2, logs.size());
        boolean debug = sensitiveSink && LogPrivacy.plaintextEnabled();
        String name = debug ? "Riverside" : "[REDACTED]";
        for (var entry : logs) {
            assertEquals("load " + name + " [REDACTED] graph", entry.getComment());
            assertEquals("inspect " + name + " [REDACTED] relations", entry.getPurpose());
            assertNull(entry.getIntentRedactions());
            assertFalse(entry.getDebugQuery().contains("RELATION-PASSWORD-CANARY"));
            if (debug) assertTrue(entry.getDebugQuery().contains("EXPLICIT OPT-IN"));
        }
        assertEquals(fail ? "failure" : "success", logs.get(logs.size() - 1).getExecutionOutcome());
        if (fail) assertNull(logs.get(logs.size() - 1).getResultCount());
        var output = Files.createTempFile("teaql-java-relation-mask-", ".log");
        try (var out = new java.io.PrintStream(Files.newOutputStream(output))) {
            var sink = new DefaultTextRuntimeLogSink(out);
            for (var entry : logs) sink.writeExecutionLog(context, entry);
        }
        var text = Files.readString(output);
        assertFalse(text.contains("Riverside")); assertFalse(text.contains("RELATION-PASSWORD-CANARY"));
        assertTrue(text.contains("SELECT")); assertTrue(text.contains("inspect"));
        if (debug) {
            var debugFile = Files.createTempFile("teaql-java-debug-relation-", ".log");
            try (var out = new java.io.PrintStream(Files.newOutputStream(debugFile))) {
                var sink = new SensitiveDiagnosticTextRuntimeLogSink(out);
                for (var entry : logs) sink.writeExecutionLog(context, entry);
            }
            String debugText = Files.readString(debugFile);
            assertTrue(debugText.contains("Riverside")); assertTrue(debugText.contains("EXPLICIT OPT-IN"));
            assertFalse(debugText.contains("RELATION-PASSWORD-CANARY"));
        }
        logs.clear();
        // Reuse both context and repository; previous query provenance must not bleed into a new request.
        var independent = forward ? new Request<>(Child.class, "MaskChild") : new Request<>(Parent.class, "MaskParent");
        independent.comment("independent Riverside").purpose("no inherited bindings").executeForList(context);
        assertEquals("independent Riverside", logs.get(0).getComment());
    }
}
