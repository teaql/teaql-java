package io.teaql.examples.naturalmessages;

import com.example.checkernaturalmessageservice.*;
import com.example.checkernaturalmessageservice.catalog.Catalog;
import com.example.checkernaturalmessageservice.entry.*;
import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import static org.junit.Assert.*;

/** Automatic generated metadata and native Checkers through normal Q/E/Save. */
public class GeneratedNaturalMessagesTest {
    private static final LocalDateTime A = LocalDateTime.of(2026, 11, 12, 12, 0), B = A.plusHours(1);
    private static final String PRIVATE_NAME = "密钥🔑" + "x".repeat(98); // 101 scalars, 102 UTF-16 units.

    private static CheckException reject(Catalog graph, UserContext context, Source connections,
                                         AtomicInteger audits, String expected) throws Exception {
        int before = connections.calls.get();
        var failure = assertThrows(CheckException.class,
                () -> graph.auditAs("Reject the second child before database or audit activity").save(context));
        assertEquals(before, connections.calls.get()); assertEquals(0, audits.get());
        assertEquals(1, failure.getViolates().size());
        var result = failure.getViolates().get(0);
        assertEquals("Entry", result.getRootType());
        assertEquals("entry_list[1].name", result.getLocation().modelPath());
        assertEquals("/entryList/1/name", result.getLocation().instancePath());
        assertEquals(expected, result.getNaturalLanguageStatement());
        String response = new ObjectMapper().writeValueAsString(Map.of("errors", failure.getViolates().stream()
                .map(value -> value.toWire(JsonFieldNamingProfile.CAMEL_CASE)).toList()));
        assertFalse(response.contains(PRIVATE_NAME)); assertFalse(failure.toString().contains(PRIVATE_NAME));
        System.out.println("JAVA_NATURAL_CHECKER_RESPONSE=" + response);
        return failure;
    }

    @Test public void generatedGraphProducesSafeNaturalMessagesAndStillSavesValidChanges() throws Exception {
        String database = System.getenv("TEAQL_NATURAL_MESSAGES_DB"); assertNotNull("use a persistent owned SQLite path", database);
        String source = System.getProperty("teaql.expected.runtime.root");
        assertNotNull(source);
        assertTrue(java.nio.file.Path.of(TeaQLRuntime.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .startsWith(java.nio.file.Path.of(source)));
        var connections = new Source("jdbc:sqlite:" + database);
        var sql = new JdbcSqlExecutor(connections);
        var sqlite = new SqliteDataServiceExecutor("sqlite", sql, connections);
        var nextId = new AtomicLong(System.currentTimeMillis());
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).dataService("sqlite", sqlite)
                .dataService("default", sqlite).executionLogging(false)
                .idGenerationService(new InternalIdGenerationService() {
                    public Long generateId(UserContext context, Entity entity) { return nextId.incrementAndGet(); }
                    public void ensureFloor(String type, long floor) { nextId.accumulateAndGet(floor, Math::max); }
                }).businessClock(new BusinessClock() {
                    public LocalDate businessDate(UserContext context) { return businessDateTime(context).toLocalDate(); }
                    public LocalDateTime businessDateTime(UserContext context) { return context.getAttribute("businessNow", LocalDateTime.class); }
                }).build();
        runtime.install(GeneratedRuntimeModule.module());
        var context = new DefaultUserContext(runtime); context.putAttribute("businessNow", A);
        context.ensureSchema();
        context.ensureSchema();
        var catalogs = Q.catalogs().limit(1).comment("Find the provisioned validation root")
                .purpose("Verify generated schema bootstrap before graph validation").executeForList(context);
        assertEquals("ensureSchema provisions the root", 1, catalogs.size()); var catalog = catalogs.get(0);
        context.withActiveRoot(new ContextEntityRef("Catalog", catalog.getId()));
        for (String name : List.of("first", "second")) {
            var entry = Q.entries().comment("Prepare a validation example row").purpose("Create a complete typed test graph").newEntity(context);
            entry.updateCatalog(catalog); entry.updateName(name);
            entry.auditAs("Create an example entry through the generated mutation API").save(context);
        }
        var graph = Q.catalogs().withIdIs(catalog.getId()).limit(1)
                .selectEntryListWith(Q.entries().orderByIdDescending().limit(2).comment("Load the latest two validation entries"))
                .comment("Load the parent and its bounded entry graph")
                .purpose("Verify indexed Checker failures through generated Q and E").executeForOne(context);
        assertEquals(2, graph.getEntryList().size()); var entry = graph.getEntryList().get(1);
        var other = graph.getEntryList().get(0);
        assertEquals("first", E.entry(entry).getName().eval());
        assertEquals("second", E.entry(other).getName().eval());
        long parentVersion = graph.getVersion(), previousVersion = entry.getVersion();
        LocalDateTime previousUpdate = E.entry(entry).getUpdateTime().eval();
        context.putAttribute("businessNow", B);
        other.updateName("changed"); entry.updateName(null);
        var audits = new AtomicInteger(); context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits.incrementAndGet());
        int before = connections.calls.get();
        var english = reject(graph, context, connections, audits, "The name of the 2nd entry is required.");
        assertEquals(CheckResult.RuleId.REQUIRED, english.getViolates().get(0).getRuleId());
        assertEquals(previousUpdate, E.entry(entry).getUpdateTime().eval());
        context.setLocaleCode("zh-CN");
        reject(graph, context, connections, audits, "请填写第2项明细的名称。");
        context.setLocaleCode("en");
        entry.updateName(PRIVATE_NAME);
        var length = reject(graph, context, connections, audits,
                "The name of the 2nd entry must not exceed 100 UTF-16 code units; its current length is 102.");
        assertNotEquals(PRIVATE_NAME, length.getViolates().get(0).getInputValue());
        var rendererCalls = new AtomicInteger();
        context.installCheckerMessageRenderer(request -> {
            rendererCalls.incrementAndGet();
            assertEquals("密钥" + "*".repeat(97) + "xx", request.arguments().get("input"));
            assertEquals(102, ((Number) request.arguments().get("input_len")).intValue());
            return "Private " + PRIVATE_NAME;
        });
        reject(graph, context, connections, audits, "Private [REDACTED]");
        assertEquals(1, rendererCalls.get());
        context.installCheckerMessageRenderer(CheckerMessageRenderer.STANDARD);
        assertEquals(before, connections.calls.get()); assertEquals(0, audits.get());
        assertEquals(previousUpdate, E.entry(entry).getUpdateTime().eval());
        assertEquals(previousVersion, (long) entry.getVersion());
        entry.updateName("first"); other.updateName("second");
        graph.auditAs("Cancel invalid caller edits without writing automatic Context values").save(context);
        assertEquals(before, connections.calls.get()); assertEquals(0, audits.get());
        entry.updateName("accepted"); graph.auditAs("Persist the valid corrected entry").save(context);
        assertEquals(1, audits.get());
        var stored = Q.entries().withIdIs(entry.getId()).limit(1).comment("Read the corrected entry")
                .purpose("Prove readable diagnostics preserve successful audited mutation").executeForOne(context);
        assertEquals("accepted", E.entry(stored).getName().eval()); assertEquals(B, E.entry(stored).getUpdateTime().eval());
        assertEquals(previousVersion + 1, (long) stored.getVersion());
        assertEquals(parentVersion, (long) graph.getVersion());
        System.out.println("JAVA_NATURAL_CHECKER_SQLITE=" + new ObjectMapper().writeValueAsString(Map.ofEntries(
                Map.entry("passed", true), Map.entry("typedQ", true), Map.entry("typedE", true),
                Map.entry("automaticMetadata", true), Map.entry("originalGeneratedChecker", true), Map.entry("responses", 4),
                Map.entry("providerCallsOnRejection", 0), Map.entry("transactionCallsOnRejection", 0),
                Map.entry("updatedAudits", audits.get()), Map.entry("customerRendererCalls", rendererCalls.get()),
                Map.entry("unchangedParent", graph.getVersion() == parentVersion), Map.entry("version", stored.getVersion()))));
    }

    static final class Source implements DataSource {
        final String url; final AtomicInteger calls = new AtomicInteger(); Source(String url) { this.url = url; }
        public Connection getConnection() throws SQLException { calls.incrementAndGet(); return DriverManager.getConnection(url); }
        public Connection getConnection(String user, String password) throws SQLException { return getConnection(); }
        public PrintWriter getLogWriter() { return null; } public void setLogWriter(PrintWriter writer) {}
        public void setLoginTimeout(int timeout) {} public int getLoginTimeout() { return 0; }
        public Logger getParentLogger() { return Logger.getGlobal(); }
        public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException("not a wrapper"); }
        public boolean isWrapperFor(Class<?> type) { return false; }
    }
}
