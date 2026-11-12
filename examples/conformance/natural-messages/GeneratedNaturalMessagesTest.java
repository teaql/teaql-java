package io.teaql.examples.naturalmessages;

import com.example.checkernaturalmessageservice.*;
import com.example.checkernaturalmessageservice.entry.*;
import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.i18n.CheckerMessageDefaults;
import io.teaql.core.i18n.Locale;
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

/** App-owned customization; generated domain source remains byte-for-byte unchanged. */
public class GeneratedNaturalMessagesTest {
    private static final LocalDateTime A = LocalDateTime.of(2026, 11, 12, 12, 0), B = A.plusHours(1);
    private static final String PRIVATE_NOTE = "密钥🔑abc";

    static final class EntryRules extends EntryChecker {
        @Override public void doCheck(UserContext context, Entry row, ObjectLocation location) {
            super.doCheck(context, row, location);
            Integer quantity = E.entry(row).getQuantity().eval();
            if (quantity != null) minNumberCheck(context, newLocation(location, "quantity"), 0, quantity);
            String description = E.entry(row).getDescription().eval();
            if (description != null) maxStringCheck(context, newLocation(location, "description"), 8, description);
            String note = E.entry(row).getProtectedNote().eval();
            if (note != null) maxStringCheck(context, newLocation(location, "protected_note"), 4, note);
        }
    }

    private static Map<Locale, String> labels(String english, String chinese) {
        return Map.of(Locale.ENGLISH, english, Locale.CHINESE_SIMPLIFIED, chinese);
    }
    private static CheckerPresentation.Field field(String name, String english, String chinese) {
        return new CheckerPresentation.Field(name, null, labels(english, chinese), Map.of(), Set.of());
    }
    private static CheckerPresentation.Model presentation() {
        return new CheckerPresentation.Model("java-example-v1", Map.of(
                "Catalog", new CheckerPresentation.Entity(labels("catalog", "目录"), List.of(field("name", "name", "名称"),
                        new CheckerPresentation.Field("entry_list", "Entry", labels("entry", "明细"), Map.of(Locale.CHINESE_SIMPLIFIED, "条"), Set.of("entryList")))),
                "Entry", new CheckerPresentation.Entity(labels("entry", "明细"), List.of(field("name", "name", "名称"),
                        field("quantity", "quantity", "数量"), field("description", "description", "描述"),
                        new CheckerPresentation.Field("protected_note", null, labels("private note", "私密备注"), Map.of(), Set.of("protectedNote"))))),
                CheckerMessageDefaults.currentObjectLabels());
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
        runtime.install(GeneratedRuntimeModule.module().withCheckerPresentation(presentation()));
        runtime.install(RuntimeModule.of().withCheckers(new EntryRules()));
        var context = new DefaultUserContext(runtime); context.putAttribute("businessNow", A);
        context.ensureSchema();
        var catalogs = Q.catalogs().limit(1).comment("Find the provisioned validation root")
                .purpose("Verify generated schema bootstrap before graph validation").executeForList(context);
        assertEquals("ensureSchema provisions the root", 1, catalogs.size()); var catalog = catalogs.get(0);
        context.withActiveRoot(new ContextEntityRef("Catalog", catalog.getId()));
        for (String name : List.of("first", "second")) {
            var entry = Q.entries().comment("Prepare a validation example row").purpose("Create a complete typed test graph").newEntity(context);
            entry.updateCatalog(catalog); entry.updateName(name); entry.updateQuantity(10);
            entry.updateDescription("short"); entry.updateProtectedNote("safe");
            entry.auditAs("Create an example entry through the generated mutation API").save(context);
        }
        var graph = Q.catalogs().withIdIs(catalog.getId()).limit(1)
                .selectEntryListWith(Q.entries().orderByIdDescending().limit(2).comment("Load the latest two validation entries"))
                .comment("Load the parent and its bounded entry graph")
                .purpose("Verify indexed Checker failures through generated Q and E").executeForOne(context);
        assertEquals(2, graph.getEntryList().size()); var entry = graph.getEntryList().get(1);
        assertEquals("first", E.entry(entry).getName().eval()); assertEquals(Integer.valueOf(10), E.entry(entry).getQuantity().eval());
        LocalDateTime previousUpdate = E.entry(entry).getUpdateTime().eval();
        context.putAttribute("businessNow", B);
        entry.updateName(null); entry.updateQuantity(-1); entry.updateDescription("123456789"); entry.updateProtectedNote(PRIVATE_NOTE);
        var audits = new AtomicInteger(); context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits.incrementAndGet());
        int before = connections.calls.get();
        var english = assertThrows(CheckException.class, () -> graph.auditAs("Reject invalid entry changes before persistence").save(context));
        assertEquals(4, english.getViolates().size()); assertEquals(before, connections.calls.get()); assertEquals(0, audits.get());
        assertEquals(previousUpdate, E.entry(entry).getUpdateTime().eval());
        Map<String, CheckResult> results = new HashMap<>();
        for (var result : english.getViolates()) {
            results.put(result.getLocation().modelPath(), result); assertEquals("Entry", result.getRootType());
            assertTrue(result.getLocation().instancePath().startsWith("/entryList/1/"));
            assertFalse(result.getNaturalLanguageStatement().contains(PRIVATE_NOTE));
        }
        assertEquals("The name of the 2nd entry is required.", results.get("entry_list[1].name").getNaturalLanguageStatement());
        assertEquals("The quantity of the 2nd entry must be at least 0.", results.get("entry_list[1].quantity").getNaturalLanguageStatement());
        assertEquals("The description of the 2nd entry must not exceed 8 UTF-16 code units; its current length is 9.",
                results.get("entry_list[1].description").getNaturalLanguageStatement());
        assertEquals("The private note of the 2nd entry must not exceed 4 UTF-16 code units; its current length is 7.",
                results.get("entry_list[1].protected_note").getNaturalLanguageStatement());
        assertNotEquals(PRIVATE_NOTE, results.get("entry_list[1].protected_note").getInputValue());
        String response = new ObjectMapper().writeValueAsString(Map.of("errors", english.getViolates().stream()
                .map(result -> result.toWire(JsonFieldNamingProfile.CAMEL_CASE)).toList()));
        assertFalse(response.contains(PRIVATE_NOTE)); assertTrue(response.contains("/entryList/1/protectedNote"));
        System.out.println("JAVA_NATURAL_CHECKER_RESPONSE=" + response);
        context.setLocaleCode("zh-CN");
        var chinese = assertThrows(CheckException.class, () -> graph.auditAs("Repeat validation with Chinese messages").save(context));
        assertEquals(4, chinese.getViolates().size());
        assertTrue(chinese.getViolates().stream().anyMatch(result -> "请填写第2条明细的名称。".equals(result.getNaturalLanguageStatement())));
        assertEquals(before, connections.calls.get()); assertEquals(0, audits.get());
        entry.updateName("first"); entry.updateQuantity(10); entry.updateDescription("short"); entry.updateProtectedNote("safe");
        graph.auditAs("Cancel invalid caller edits without writing automatic Context values").save(context);
        assertEquals(before, connections.calls.get()); assertEquals(0, audits.get());
        entry.updateName("accepted"); graph.auditAs("Persist the valid corrected entry").save(context);
        assertEquals(1, audits.get());
        var stored = Q.entries().withIdIs(entry.getId()).limit(1).comment("Read the corrected entry")
                .purpose("Prove readable diagnostics preserve successful audited mutation").executeForOne(context);
        assertEquals("accepted", E.entry(stored).getName().eval()); assertEquals(B, E.entry(stored).getUpdateTime().eval());
        System.out.println("JAVA_NATURAL_CHECKER_SQLITE=passed typedQ=true typedE=true indexed=true required=true range=true length=true masked=true noFailedWrites=true chinese=true correctedSave=true");
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
