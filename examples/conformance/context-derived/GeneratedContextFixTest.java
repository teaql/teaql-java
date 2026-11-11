package io.teaql.examples.contextfix;

import com.example.contextfixservice.*;
import com.example.contextfixservice.entry.*;
import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import java.time.*;
import java.sql.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import static org.junit.Assert.*;

/** Application-owned customization of an unmodified generated library. */
public class GeneratedContextFixTest {
    static final LocalDateTime A = LocalDateTime.of(2026, 11, 11, 10, 0), B = A.plusHours(1);
    static class Context extends DefaultUserContext {
        Context(TeaQLRuntime runtime) { super(runtime); }
        @Override @SuppressWarnings("unchecked") protected <T> T evaluateExpression(String expression, Object... args) {
            if ("uniStr".equals(expression)) {
                if (Boolean.TRUE.equals(getAttribute("brokenContext")))
                    throw new IllegalStateException("PRIVATE_CONTEXT_PROVIDER_CANARY");
                return (T) "fixture-token";
            }
            return null;
        }
    }
    static class ContextChecker extends EntryChecker {
        @Override public void doCheck(UserContext context, Entry row, ObjectLocation location) {
            // Freeze eligibility before the first automatic assignment.
            boolean eligible = needContextFix(context, row);
            if (eligible) {
                if (row.isNew()) {
                    row.updateSourceIp(context.getAttribute("clientIp", String.class));
                    row.updateOpenedBy(context.getAttribute("actor", String.class));
                }
                row.updateLastIp(context.getAttribute("clientIp", String.class));
                row.updateChangedBy(context.getAttribute("actor", String.class));
            }
            super.doCheck(context, row, location);
            if ("NORMALIZE".equals(row.getName())) row.updateName("entry");
            if ("INVALID".equals(row.getName())) invalidTypeCheck(context, newLocation(location, "name"), "fixture rejection");
        }
    }

    @Test public void generatedQAndEAuditedSaveUsesContextOnlyForRealChanges() throws Exception {
        String source = System.getProperty("teaql.expected.runtime.root");
        if (source != null) assertTrue(java.nio.file.Path.of(TeaQLRuntime.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .startsWith(java.nio.file.Path.of(source)));
        String database = System.getenv("TEAQL_CONTEXT_FIX_DB");
        assertNotNull("use an owned persistent SQLite path", database);
        var ds = new Source("jdbc:sqlite:" + database);
        var metadata = new SimpleEntityMetaFactory();
        var sql = new JdbcSqlExecutor(ds);
        var data = new SqliteDataServiceExecutor("sqlite", sql, ds);
        var ids = new AtomicLong(System.currentTimeMillis());
        var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("sqlite", data)
                .dataService("default", data).idGenerationService(new InternalIdGenerationService() {
                    public Long generateId(UserContext ctx, Entity row) { return ids.incrementAndGet(); }
                    public void ensureFloor(String type, long floor) { ids.accumulateAndGet(floor, Math::max); }
                })
                .businessClock(new BusinessClock() {
                    public LocalDate businessDate(UserContext ctx) { return businessDateTime(ctx).toLocalDate(); }
                    public LocalDateTime businessDateTime(UserContext ctx) { return ctx.getAttribute("businessNow", LocalDateTime.class); }
                }).executionLogging(false).build();
        runtime.install(GeneratedRuntimeModule.module());
        var descriptor = metadata.resolveEntityDescriptor("Entry");
        // Supported model-aware customization; never patch the emitted library.
        descriptor.findProperty("source_ip").with("createFunction", "clientIp");
        descriptor.findProperty("opened_by").with("createFunction", "actor");
        descriptor.findProperty("last_ip").with("updateFunction", "clientIp");
        descriptor.findProperty("changed_by").with("updateFunction", "actor");
        runtime.install(RuntimeModule.of().withCheckers(new ContextChecker()));
        var context = new Context(runtime);
        context.putAttribute("businessNow", A); context.putAttribute("clientIp", "192.0.2.10"); context.putAttribute("actor", "alice");
        context.ensureSchema();
        var roots = Q.catalogs().limit(1).comment("find fixture root").purpose("verify Context save").executeForList(context);
        var catalog = roots.isEmpty() ? Q.catalogs().comment("create fixture root").purpose("verify Context save").newEntity(context) : roots.get(0);
        if (catalog.isNew()) { catalog.updateName("Catalog"); catalog.auditAs("create Context fixture root").save(context); }
        context.withActiveRoot(new ContextEntityRef("Catalog", catalog.getId()));
        var failedCreate = Q.entries().comment("create with broken Context supplier").purpose("verify Fix location").newEntity(context);
        failedCreate.updateName("failed"); failedCreate.updateCatalog(catalog);
        context.putAttribute("brokenContext", true);
        int beforeFixFailure = ds.calls.get();
        var fixError = assertThrows(CheckException.class,
                () -> failedCreate.auditAs("reject broken Context supplier").save(context));
        assertEquals("Entry", fixError.getViolates().get(0).getRootType());
        assertEquals("/guardedToken", fixError.getViolates().get(0).getLocation().instancePath());
        assertTrue(fixError.getMessage().contains("guarded_token"));
        assertFalse(fixError.getMessage().contains("PRIVATE_CONTEXT_PROVIDER_CANARY"));
        assertEquals(beforeFixFailure, ds.calls.get());
        assertNull(failedCreate.getBornAt()); assertNull(failedCreate.getLastIp());
        assertEquals("failed", failedCreate.getName());
        context.putAttribute("brokenContext", false);
        var row = Q.entries().comment("create Context fixture").purpose("verify generated Context injection").newEntity(context);
        row.updateName("entry"); row.updateCatalog(catalog); row.auditAs("create Context fixture").save(context);
        long id = row.getId();
        assertEquals(A, row.getBornAt()); assertEquals("alice", row.getOpenedBy());

        var audit = new AtomicInteger();
        context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (ctx, event) -> audit.incrementAndGet());
        context.putAttribute("businessNow", B); context.putAttribute("clientIp", "192.0.2.20"); context.putAttribute("actor", "bob");
        row = Q.entries().withIdIs(id).limit(1).comment("reload full fixture").purpose("verify no-op").executeForList(context).get(0);
        long version = row.getVersion(); int connections = ds.calls.get();
        row.auditAs("first unchanged save under Context B").save(context);
        row.auditAs("second unchanged save under Context B").save(context);
        assertEquals(connections, ds.calls.get()); assertEquals(0, audit.get());
        assertEquals(Long.valueOf(version), row.getVersion()); assertEquals(A, row.getTouchedAt());
        assertEquals("192.0.2.10", row.getLastIp()); assertEquals("alice", row.getChangedBy());

        row.updateName("NORMALIZE");
        row.auditAs("normalize business edit back to its persisted value").save(context);
        assertEquals("a repaired no-op must not acquire a connection", connections, ds.calls.get());
        assertEquals(0, audit.get()); assertEquals(Long.valueOf(version), row.getVersion());
        assertEquals("entry", row.getName()); assertEquals(A, row.getTouchedAt());
        assertEquals("192.0.2.10", row.getLastIp()); assertEquals("alice", row.getChangedBy());

        row.updateName("INVALID"); Entry invalid = row;
        connections = ds.calls.get();
        var error = assertThrows(CheckException.class, () -> invalid.auditAs("reject Context fixture").save(context));
        assertEquals("Entry", error.getViolates().get(0).getRootType());
        assertEquals("/name", error.getViolates().get(0).getLocation().instancePath());
        assertEquals(connections, ds.calls.get()); assertEquals(0, audit.get());
        assertEquals(A, row.getTouchedAt()); assertEquals("192.0.2.10", row.getLastIp());
        assertEquals("INVALID", row.getName());
        row.updateName("entry"); row.auditAs("cancel failed caller edit").save(context);
        assertEquals(connections, ds.calls.get());

        context.putAttribute("clientIp", "x".repeat(101)); row.updateName("blocked"); Entry invalidContext = row;
        var contextError = assertThrows(CheckException.class,
                () -> invalidContext.auditAs("reject invalid Context IP").save(context));
        var ipError = contextError.getViolates().stream()
                .filter(item -> "last_ip".equals(item.getLocation().modelPath())).findFirst().orElseThrow();
        assertEquals("Entry", ipError.getRootType());
        assertEquals("/lastIp", ipError.getLocation().instancePath());
        assertEquals(connections, ds.calls.get()); assertEquals(0, audit.get());
        assertEquals("192.0.2.10", row.getLastIp());
        context.putAttribute("clientIp", "192.0.2.20");
        row.updateName("updated"); row.auditAs("real business update under Context B").save(context);
        assertEquals(1, audit.get());
        var persisted = Q.entries().withIdIs(id).limit(1).comment("read back real update").purpose("verify SQLite persistence").executeForList(context).get(0);
        assertEquals("updated", E.entry(persisted).getName().eval());
        assertEquals(A, persisted.getBornAt()); assertEquals("192.0.2.10", persisted.getSourceIp());
        assertEquals("alice", persisted.getOpenedBy()); assertEquals(B, persisted.getTouchedAt());
        assertEquals("192.0.2.20", persisted.getLastIp()); assertEquals("bob", persisted.getChangedBy());
        assertEquals(Long.valueOf(version + 1), persisted.getVersion());
        // A computed result belongs to the query view, not the mutation ledger.
        var readonlyRequest = Q.entries().withIdIs(id).limit(1);
        readonlyRequest.setDynamicPropertyMetadata(new DynamicPropertyMetadata(Map.of("_name_copy", String.class)));
        readonlyRequest.addSimpleDynamicProperty("_name_copy", new PropertyReference("name"));
        var readonly = readonlyRequest.comment("Load a typed readonly query result")
                .purpose("Prove view materialization is not a business edit").executeForOne(context);
        assertEquals(persisted.getName(), readonly.getDynamicProperty("_name_copy"));
        assertTrue(readonly.getUpdatedProperties().isEmpty());
        context.putAttribute("businessNow", B.plusHours(1));
        context.putAttribute("clientIp", "192.0.2.30"); context.putAttribute("actor", "carol");
        connections = ds.calls.get(); int auditsBefore = audit.get();
        for (int attempt = 0; attempt < 2; attempt++)
            readonly.auditAs("Retain readonly properties without Context-only writes").save(context);
        assertEquals(connections, ds.calls.get()); assertEquals(auditsBefore, audit.get());
        assertEquals(Long.valueOf(version + 1), readonly.getVersion()); assertEquals(B, readonly.getTouchedAt());
        assertEquals(persisted.getName(), readonly.getDynamicProperty("_name_copy"));

        // Persist extensions through the same audited graph boundary and exact JDBC executor.
        var provider = new JdbcDynamicFieldsProvider(sql); provider.ensureSchema();
        DynamicFieldContext definitionContext = new DynamicFieldContext() {
            public String scopeType() { return "GLOBAL"; }
            public String scopeId() { return "default"; }
            public String userId() { return "context-example"; }
            public String purpose() { return "Verify Context Fix for persistent extensions"; }
            public String comment() { return "Register a controlled extension definition"; }
            public boolean strictIntent() { return true; }
            public long nextId(String type) { return ids.incrementAndGet(); }
        };
        if (provider.listFieldDefs(definitionContext, "Entry").stream().noneMatch(def -> "context_note".equals(def.getCode()))) {
            var def = new DynamicFieldDef(); def.setScope(DynamicFieldScope.global()); def.setOwnerType("Entry");
            def.setCode("context_note"); def.setName("Context note"); def.setDataType(DynamicDataType.STRING);
            provider.registerFieldDef(definitionContext, def);
        }
        context.putAttribute(DynamicFieldsFacade.class.getName(),
                new DefaultDynamicFieldsFacade(provider).withIdGenerator(type -> ids.incrementAndGet()));
        var dynamicRequest = Q.entries().withIdIs(id).limit(1);
        dynamicRequest.selectDynamicFieldsWith(new DynamicFieldSelection().selectString("context_note"));
        var dynamic = dynamicRequest.comment("Load typed extension definitions and values")
                .purpose("Update only a persistent dynamic field").executeForOne(context);
        assertEquals(DynamicFieldValue.State.NOT_LOADED, dynamic.dynamicFields().field("context_note").state());
        dynamic.updateDynamicField("context_note", "dynamic-only");
        assertTrue(dynamic.getUpdatedProperties().isEmpty());
        auditsBefore = audit.get();
        dynamic.auditAs("Persist a dynamic-only business edit with Context defaults").save(context);
        assertEquals(auditsBefore + 1, audit.get());
        assertEquals(Long.valueOf(version + 2), dynamic.getVersion());
        assertEquals(B.plusHours(1), dynamic.getTouchedAt()); assertEquals("carol", dynamic.getChangedBy());
        assertEquals(A, dynamic.getBornAt()); assertEquals("alice", dynamic.getOpenedBy());
        var checkRequest = Q.entries().withIdIs(id).limit(1);
        checkRequest.selectDynamicFieldsWith(new DynamicFieldSelection().selectString("context_note"));
        var storedDynamic = checkRequest.comment("Read the committed extension and owner")
                .purpose("Compare typed E and persistent extension readback").executeForOne(context);
        assertEquals("dynamic-only", storedDynamic.dynamicFields().field("context_note").value());
        assertEquals(B.plusHours(1), E.entry(storedDynamic).getTouchedAt().eval());
        context.putAttribute("businessNow", B.plusHours(2)); context.putAttribute("actor", "dora");
        context.putAttribute("clientIp", "192.0.2.40");
        storedDynamic.updateDynamicField("context_note", "dynamic-only");
        connections = ds.calls.get(); auditsBefore = audit.get();
        storedDynamic.auditAs("An equal persistent extension is not a Context update").save(context);
        assertEquals(connections, ds.calls.get()); assertEquals(auditsBefore, audit.get());
        assertEquals(Long.valueOf(version + 2), storedDynamic.getVersion());
        assertEquals("carol", storedDynamic.getChangedBy());
        // Repeat indexed failure/undo using real generated relationships, not a test-only carrier.
        var graphRoot = Q.catalogs().comment("Create a separate generated graph owner")
                .purpose("Verify indexed Checker locations before graph writes").newEntity(context);
        graphRoot.updateName("graph-root"); graphRoot.auditAs("Create an isolated graph root").save(context);
        context.withActiveRoot(new ContextEntityRef("Catalog", graphRoot.getId()));
        for (String name : java.util.List.of("graph-first", "graph-second")) {
            var child = Q.entries().comment("Prepare one generated graph child")
                    .purpose("Verify all children before persistence").newEntity(context);
            child.updateName(name); child.updateCatalog(graphRoot); child.auditAs("Create a graph child").save(context);
        }
        var parent = Q.catalogs().withIdIs(graphRoot.getId()).limit(1)
                .selectEntryListWith(Q.entries().orderByIdAscending().limit(10))
                .comment("Load the generated parent and both children")
                .purpose("Verify exact child index and rollback of automatic Context values").executeForOne(context);
        assertEquals(2, parent.getEntryList().size());
        var first = parent.getEntryList().get(0); var second = parent.getEntryList().get(1);
        long parentVersion = parent.getVersion(), secondVersion = second.getVersion();
        LocalDateTime graphBefore = first.getTouchedAt();
        context.putAttribute("businessNow", B.plusHours(3)); context.putAttribute("actor", "eve");
        context.putAttribute("clientIp", "192.0.2.50");
        first.updateName("changed-child"); second.updateName("INVALID");
        connections = ds.calls.get(); auditsBefore = audit.get();
        var childError = assertThrows(CheckException.class,
                () -> parent.auditAs("Reject a later invalid child before any graph write").save(context));
        assertTrue(childError.getViolates().stream().anyMatch(v -> "/entryList/1/name".equals(v.getLocation().instancePath())));
        assertEquals(connections, ds.calls.get()); assertEquals(auditsBefore, audit.get());
        assertEquals(graphBefore, first.getTouchedAt()); assertEquals("dora", first.getChangedBy());
        first.updateName("graph-first"); second.updateName("graph-second");
        parent.auditAs("Undo rejected graph edits without retaining automatic Context changes").save(context);
        assertEquals(connections, ds.calls.get()); assertEquals(auditsBefore, audit.get());
        first.updateName("accepted-child"); parent.auditAs("Save only the genuinely changed generated child").save(context);
        assertEquals(auditsBefore + 1, audit.get()); assertEquals(Long.valueOf(parentVersion), parent.getVersion());
        assertEquals(Long.valueOf(secondVersion), second.getVersion());
        var storedChild = Q.entries().withIdIs(first.getId()).limit(1)
                .comment("Read the accepted graph child").purpose("Verify typed E after indexed failure and retry").executeForOne(context);
        assertEquals("accepted-child", E.entry(storedChild).getName().eval()); assertEquals("eve", storedChild.getChangedBy());
        assertEquals(B.plusHours(3), E.entry(storedChild).getTouchedAt().eval());
        System.out.println("JAVA_CONTEXT_FIX_GRAPH=passed indexedLocation=true automaticUndo=true childOnly=true");
        System.out.println("JAVA_CONTEXT_FIX_EXTENSIONS=passed dynamicOnly=true readonlyNoop=true equalDynamicNoop=true");
        System.out.println("JAVA_CONTEXT_FIX_SQLITE=passed typedQ=true typedE=true noOpProviderCalls=0 failedCheckProviderCalls=0 updateAudits=1 objectLocation=true");
    }

    static final class Source implements DataSource {
        final String url; final AtomicInteger calls = new AtomicInteger(); Source(String url) { this.url = url; }
        public Connection getConnection() throws SQLException { calls.incrementAndGet(); return DriverManager.getConnection(url); }
        public Connection getConnection(String u, String p) throws SQLException { return getConnection(); }
        public PrintWriter getLogWriter() { return null; } public void setLogWriter(PrintWriter writer) {}
        public void setLoginTimeout(int timeout) {} public int getLoginTimeout() { return 0; }
        public Logger getParentLogger() { return Logger.getGlobal(); }
        public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException("not a wrapper"); }
        public boolean isWrapperFor(Class<?> type) { return false; }
    }
}
