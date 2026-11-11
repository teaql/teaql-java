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
import java.time.*;
import java.sql.*;
import java.util.concurrent.atomic.*;
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
        var data = new SqliteDataServiceExecutor("sqlite", new JdbcSqlExecutor(ds), ds);
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
