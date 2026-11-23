package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.checker.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import org.sqlite.Function;
import static org.junit.Assert.*;

/** Ordinary generated Q/E/mutation calls; raw-ledger writes below are explicit negative fault injection. */
public class GeneratedCanonicalNameTest {
    static final class Context extends DefaultUserContext implements ScalarTypeConformanceServiceUserContext {
        Context(TeaQLRuntime runtime) { super(runtime); }
    }
    static final class ObservedDataSource implements DataSource {
        final String url;
        long calls;
        final List<Long> written = new ArrayList<>(), rejected = new ArrayList<>();
        ObservedDataSource(String url) { this.url = url; }
        public Connection getConnection() throws SQLException {
            calls++;
            var connection = DriverManager.getConnection(url);
            Function.create(connection, "name93_written", new Function() {
                protected void xFunc() throws SQLException { written.add(value_long(0)); result(0); }
            });
            Function.create(connection, "name93_rejected", new Function() {
                protected void xFunc() throws SQLException { rejected.add(value_long(0)); result(0); }
            });
            return connection;
        }
        public Connection getConnection(String user, String password) throws SQLException { return getConnection(); }
        public PrintWriter getLogWriter() { return null; }
        public void setLogWriter(PrintWriter value) {}
        public void setLoginTimeout(int value) {}
        public int getLoginTimeout() { return 0; }
        public Logger getParentLogger() { return Logger.getGlobal(); }
        public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException("Not a wrapper"); }
        public boolean isWrapperFor(Class<?> type) { return false; }
    }
    static final class Fixture {
        final ObservedDataSource data;
        final JdbcSqlExecutor sql;
        final List<RawAuditEvent> audits = new ArrayList<>();
        final List<MutationPlan> policies = new ArrayList<>();
        final List<Set<String>> checked = new ArrayList<>();
        final Context context;
        Fixture(String url, int round) {
            this(url, round, null);
        }
        Fixture(String url, int round, java.util.function.Consumer<ScalarTypeProbe> customerFix) {
            data = new ObservedDataSource(url);
            sql = new JdbcSqlExecutor(data);
            var service = new SqliteDataServiceExecutor("sqlite", sql, data);
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("name93", "1", "sha256:name93"); }
                public MutationDecision review(UserContext caller, MutationPlan plan) { policies.add(plan); return MutationDecision.allow(); }
            };
            var ids = new AtomicLong(100L * round);
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(new InternalIdGenerationService() {
                        public Long generateId(UserContext caller, Entity entity) { return ids.incrementAndGet(); }
                        public void ensureFloor(String type, long floor) { ids.accumulateAndGet(floor, Math::max); }
                    })
                    .executionLogging(false).mutationPolicyRegistry(key -> Optional.of(policy))
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller, ExecutionMetadata event) {}
                        public void writeAuditEvent(UserContext caller, RawAuditEvent event) { audits.add(event); }
                    }).build();
            var module = GeneratedRuntimeModule.module(); runtime.install(module);
            for (var original : module.checkers()) if (original.type().equals("ScalarTypeProbe")) {
                @SuppressWarnings("unchecked") var delegate = (Checker<ScalarTypeProbe>) original;
                runtime.install(RuntimeModule.of().withCheckers(new Checker<ScalarTypeProbe>() {
                    public String type() { return "ScalarTypeProbe"; }
                    public void checkAndFix(UserContext caller, ScalarTypeProbe entity, ObjectLocation location) {
                        var keys = new HashSet<>(entity.getUpdatedProperties());
                        assertFalse(keys.contains("type_platform_id")); assertFalse(keys.contains("requiredString"));
                        checked.add(Set.copyOf(keys)); delegate.checkAndFix(caller, entity, location);
                        if (customerFix != null) customerFix.accept(entity);
                    }
                }));
            }
            context = new Context(runtime); context.ensureSchema();
        }
        TypePlatform root(String name) {
            var value = Q.typePlatforms().comment("Initialize the name graph root")
                    .purpose("Verify generated root mutations").newEntity(context);
            value.updateName(name); value.auditAs("Create the name graph root").save(context); return value;
        }
        ScalarTypeProbe probe(TypePlatform root, String name) {
            var value = Q.scalarTypeProbes().comment("Initialize a typed scalar probe")
                    .purpose("Verify generated canonical mutation fields").newEntity(context);
            value.updateTypePlatform(root); value.updateRequiredString(name); value.updateOptionalString(null);
            value.updateRequiredBoolean(true); value.updateRequiredInteger(7); value.updateRequiredLong(8L);
            value.updateRequiredDecimal(new BigDecimal("12.50")); value.updateRequiredFloat(1.25f); value.updateRequiredDouble(2.5d);
            value.updateRequiredDate(LocalDate.of(2032, 2, 29)); value.updateRequiredTime(LocalTime.of(12, 30));
            value.updateRequiredDatetime(LocalDateTime.of(2032, 2, 29, 12, 30)); return value;
        }
        ScalarTypeProbe load(long id) {
            return Q.scalarTypeProbes().withIdIs(id).limit(1).comment("Reload the complete name probe")
                    .purpose("Verify persisted identity, version and fields").executeForOne(context);
        }
        void oracle(long id, String name, long version, long root) throws Exception {
            try (var connection = DriverManager.getConnection(data.url);
                 var statement = connection.prepareStatement("SELECT required_string,version,type_platform FROM scalar_type_probe_data WHERE id=?")) {
                statement.setLong(1, id);
                try (var rows = statement.executeQuery()) {
                    assertTrue(rows.next()); assertEquals(name, rows.getString(1)); assertEquals(version, rows.getLong(2));
                    assertEquals(root, rows.getLong(3)); assertFalse(rows.next());
                }
            }
        }
    }
    @Test public void generatedCanonicalNameFlow() throws Exception {
        var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for (var type : List.of(UserContext.class, TeaQLRuntime.class, SqliteDataServiceExecutor.class, TeaQLModule.class))
            assertTrue(type.getName(), Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        int round = Integer.parseInt(System.getenv("TEAQL_NAME_ROUND"));
        var f = new Fixture("jdbc:sqlite:" + Path.of(System.getenv("TEAQL_NAME_DB")).toAbsolutePath(), round);
        var cases = new ArrayList<String>();
        var root = f.root("Name93 root " + round);
        var first = f.probe(root, "Name93 first " + round);
        assertTrue(first.getUpdatedProperties().containsAll(List.of("required_string", "type_platform")));
        first.auditAs("Create the canonical name probe").save(f.context);
        assertEquals(Long.valueOf(1), first.getVersion());
        assertTrue(f.checked.stream().anyMatch(keys -> keys.containsAll(List.of("required_string", "type_platform"))));
        assertTrue(f.policies.stream().flatMap(plan -> plan.operations().stream())
                .anyMatch(operation -> operation.changedValues().keySet().containsAll(List.of("required_string", "type_platform"))));
        var created = f.audits.get(f.audits.size() - 1);
        assertTrue(created.changes().stream().anyMatch(change -> change.field().equals("type_platform")));
        assertTrue(created.changes().stream().anyMatch(change -> change.field().equals("required_string")));
        f.oracle(first.getId(), "Name93 first " + round, 1, root.getId()); cases.add("canonical-create");

        var loaded = Q.scalarTypeProbes().withIdIs(first.getId()).limit(1)
                .selectTypePlatformWith(Q.typePlatformsWithMinimalFields().selectName())
                .comment("Load canonical relation details").purpose("Verify generated Q and E").executeForOne(f.context);
        assertEquals(root.getId(), E.scalarTypeProbe(loaded).getTypePlatform().getId().eval());
        assertEquals(root.getName(), E.scalarTypeProbe(loaded).getTypePlatform().getName().eval());
        assertNull(E.scalarTypeProbe(loaded).getOptionalString().eval());
        var partial = Q.scalarTypeProbesWithMinimalFields().withIdIs(first.getId()).selectRequiredString().limit(1)
                .comment("Load a minimal name projection").purpose("Distinguish missing fields from loaded null").executeForOne(f.context);
        assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(partial).getOptionalString().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.scalarTypeProbe(partial).getTypePlatform().getName().eval());
        cases.add("q-e-load-state");

        var secondRoot = f.root("Name93 destination " + round);
        var update = f.load(first.getId()); update.updateTypePlatform(secondRoot); update.updateRequiredString("Name93 moved " + round);
        var keys = update.getEntityMutationLedger().changedFieldNames(new EntityKey("ScalarTypeProbe", update.getId()));
        assertTrue(keys.containsAll(List.of("type_platform", "required_string"))); assertFalse(keys.contains("typePlatform"));
        update.auditAs("Move the canonical relation").save(f.context);
        f.oracle(first.getId(), "Name93 moved " + round, 2, secondRoot.getId()); cases.add("canonical-update");

        int malformed = 0;
        for (boolean existing : List.of(false, true)) for (String field : List.of("type_platform_id", "typePlatform", "requiredString", "unknown_field", "equal", "unequal")) {
            var bad = existing ? f.load(first.getId()) : f.probe(root, "Name93 invalid");
            var ledger = bad.getEntityMutationLedger();
            var key = new EntityKey("ScalarTypeProbe", bad.getId());
            if (field.equals("equal") || field.equals("unequal")) {
                ledger.set(key, "type_platform", root.getId());
                ledger.set(key, "type_platform_id", field.equals("equal") ? root.getId() : secondRoot.getId());
            } else ledger.set(key, field, field.equals("requiredString") ? "PRIVATE_NAME_CANARY" : 1L);
            long calls = f.data.calls; int checks = f.checked.size(), policies = f.policies.size(), audits = f.audits.size();
            var error = assertThrows(CheckException.class, () -> bad.auditAs("Reject malformed model fields").save(f.context));
            assertTrue(error.getViolates().stream().allMatch(value -> value.getRuleId() == CheckResult.RuleId.UNKNOWN_FIELD));
            assertFalse(error.getMessage().contains("PRIVATE_NAME_CANARY"));
            assertEquals(calls, f.data.calls); assertEquals(checks, f.checked.size()); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
            malformed++;
        }
        cases.add("malformed-zero-provider");
        for (boolean existing : List.of(false, true)) {
            var bad = existing ? f.load(first.getId()) : f.probe(root, "Name93 required"); bad.updateRequiredString(null);
            long calls = f.data.calls; int policies = f.policies.size(), audits = f.audits.size();
            var error = assertThrows(CheckException.class, () -> bad.auditAs("Reject a missing required string").save(f.context));
            assertTrue(error.getViolates().stream().anyMatch(value -> value.getLocation().modelPath().equals("required_string")));
            assertEquals(calls, f.data.calls); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        }
        cases.add("required-zero-provider");

        var mapper = new ObjectMapper().findAndRegisterModules().registerModule(new TeaQLModule(f.context));
        var json = mapper.valueToTree(loaded);
        assertTrue(json.has("typePlatform")); assertTrue(json.has("requiredString")); assertFalse(json.has("type_platform"));
        assertTrue(json.has("optionalString")); assertTrue(json.get("optionalString").isNull());
        var decoded = mapper.treeToValue(json, ScalarTypeProbe.class);
        assertEquals(root.getId(), E.scalarTypeProbe(decoded).getTypePlatform().getId().eval());
        assertEquals(E.scalarTypeProbe(loaded).getRequiredString().eval(), E.scalarTypeProbe(decoded).getRequiredString().eval());
        assertTrue(decoded.getUpdatedProperties().isEmpty()); assertFalse(decoded.__internalHasMutationLedger());
        var duplicate = mapper.createObjectNode().put("requiredString", "same").put("required_string", "same");
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> mapper.treeToValue(duplicate, ScalarTypeProbe.class));
        cases.add("typed-json-default-profile");

        var a = f.probe(root, "Name93 graph alpha"); var b = f.probe(root, "Name93 graph beta");
        a.auditAs("Seed first graph member").save(f.context); b.auditAs("Seed second graph member").save(f.context);
        var graph = Q.typePlatforms().withIdIs(root.getId()).limit(1)
                .selectScalarTypeProbeListWith(Q.scalarTypeProbes().orderByIdAscending().limit(10))
                .comment("Load the complete two-member graph").purpose("Verify atomic graph rollback and retry").executeForOne(f.context);
        var children = graph.getScalarTypeProbeList();
        var childA = children.stream().filter(row -> row.getId().equals(a.getId())).findFirst().orElseThrow();
        var childB = children.stream().filter(row -> row.getId().equals(b.getId())).findFirst().orElseThrow();
        childA.updateRequiredString("Name93 graph revised"); childB.updateRequiredString("Name93 fail child");
        try (var connection = DriverManager.getConnection(f.data.url); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER name93_observe AFTER UPDATE ON scalar_type_probe_data BEGIN SELECT name93_written(NEW.id); END");
            statement.execute("CREATE TRIGGER name93_fail BEFORE UPDATE ON scalar_type_probe_data WHEN NEW.required_string='Name93 fail child' BEGIN SELECT name93_rejected(NEW.id); SELECT RAISE(ABORT,'NAME93_SECOND_UPDATE'); END");
        }
        f.data.written.clear(); f.data.rejected.clear(); f.audits.clear();
        assertThrows(RuntimeException.class, () -> graph.auditAs("Revise two name graph members").save(f.context));
        assertEquals(List.of(a.getId()), f.data.written); assertEquals(List.of(b.getId()), f.data.rejected); assertTrue(f.audits.isEmpty());
        f.oracle(a.getId(), "Name93 graph alpha", 1, root.getId()); f.oracle(b.getId(), "Name93 graph beta", 1, root.getId());
        assertEquals(Long.valueOf(1), childA.getVersion()); assertEquals(Long.valueOf(1), childB.getVersion());
        assertTrue(childA.getUpdatedProperties().contains("required_string")); assertTrue(childB.getUpdatedProperties().contains("required_string"));
        cases.add("real-second-update-rollback");
        try (var connection = DriverManager.getConnection(f.data.url); var statement = connection.createStatement()) { statement.execute("DROP TRIGGER name93_fail"); }
        f.data.written.clear(); graph.auditAs("Revise two name graph members").save(f.context);
        assertEquals(List.of(a.getId(), b.getId()), f.data.written);
        assertEquals(2, f.audits.size()); assertEquals(Set.of(a.getId(), b.getId()), new HashSet<>(f.audits.stream().map(RawAuditEvent::entityId).toList()));
        for (var event : f.audits) {
            assertEquals("ScalarTypeProbe", event.entityType()); assertEquals(Long.valueOf(2), event.resultingVersion());
            assertTrue(event.changes().stream().anyMatch(change -> change.field().equals("required_string")));
            assertEquals("Revise two name graph members", event.reason());
        }
        f.oracle(a.getId(), "Name93 graph revised", 2, root.getId()); f.oracle(b.getId(), "Name93 fail child", 2, root.getId());
        try (var connection = DriverManager.getConnection(f.data.url); var statement = connection.createStatement()) { statement.execute("DROP TRIGGER name93_observe"); }
        cases.add("same-graph-retry");
        System.out.println("JAVA_CANONICAL_NAME=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "round", round, "cases", cases, "malformedRejections", malformed,
                "requiredRejections", 2, "rolledBackAfterWrites", 1, "retryAuditEntities", 2,
                "ids", List.of(first.getId(), a.getId(), b.getId()), "versions", List.of(2, 2, 2), "wireProfiles", List.of("camelCase"))));
    }
}
