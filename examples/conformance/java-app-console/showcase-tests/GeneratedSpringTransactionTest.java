package com.teaql.runtimeexampleconformanceservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.runtimeexampleconformanceservice.platform.Platform;
import com.teaql.runtimeexampleconformanceservice.workitem.WorkItem;
import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.provider.springjdbc.SpringJdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.Function;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Application-owned business calls. SQL below is an independent oracle/fault injector only. */
public class GeneratedSpringTransactionTest {
    static final class Context extends DefaultUserContext implements RuntimeExampleConformanceServiceUserContext {
        Context(TeaQLRuntime runtime) { super(runtime); }
    }

    static final class Source extends SQLiteDataSource {
        long connections;
        final List<Long> written = new ArrayList<>(), rejected = new ArrayList<>();
        @Override public Connection getConnection() throws SQLException {
            connections++;
            var connection = super.getConnection();
            Function.create(connection, "spring_written", new Function() {
                protected void xFunc() throws SQLException { written.add(value_long(0)); result(0); }
            });
            Function.create(connection, "spring_rejected", new Function() {
                protected void xFunc() throws SQLException { rejected.add(value_long(0)); result(0); }
            });
            return connection;
        }
    }

    static final class Fixture {
        final int round = Integer.parseInt(System.getenv("TEAQL_SPRING_ROUND"));
        final String kind, url;
        final Source source = new Source();
        final SpringJdbcSqlExecutor sql;
        final DataSourceTransactionManager manager;
        final List<RawAuditEvent> audits = new ArrayList<>();
        final List<MutationPlan> policies = new ArrayList<>();
        final Context context;
        final Platform root;
        boolean failDelivery;

        Fixture(String kind) throws Exception {
            this.kind = kind;
            url = "jdbc:sqlite:" + Path.of(System.getenv("TEAQL_SPRING_DB_ROOT"), kind + ".sqlite").toAbsolutePath();
            source.setUrl(url);
            sql = new SpringJdbcSqlExecutor(new NamedParameterJdbcTemplate(source));
            manager = new DataSourceTransactionManager(source);
            var service = new SqliteDataServiceExecutor("sqlite", sql, source);
            var ids = new AtomicLong(round * 1000L);
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("spring-example", "1", "sha256:spring-example"); }
                public MutationDecision review(UserContext caller, MutationPlan plan) { policies.add(plan); return MutationDecision.allow(); }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(new InternalIdGenerationService() {
                        public Long generateId(UserContext caller, Entity entity) { return ids.incrementAndGet(); }
                        public void ensureFloor(String type, long floor) { ids.accumulateAndGet(floor, Math::max); }
                    })
                    .executionLogging(false).mutationPolicyRegistry(key -> Optional.of(policy))
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller, ExecutionMetadata event) {}
                        public void writeAuditEvent(UserContext caller, RawAuditEvent event) {
                            assertFalse("audit cannot reuse the completed physical owner", sql.hasActiveTransaction());
                            // A separate connection must observe committed data before delivery.
                            if (event.entityType().equals("WorkItem")) {
                                try { assertTrue(version((Long) event.entityId()) >= event.resultingVersion()); }
                                catch (SQLException error) { throw new IllegalStateException(error); }
                            }
                            audits.add(event);
                            if (failDelivery) throw new IllegalStateException("EXPECTED_AUDIT_DELIVERY_FAILURE");
                        }
                    }).build().install(GeneratedRuntimeModule.module());
            context = new Context(runtime);
            context.ensureSchema();
            root = Q.platforms().withIdIs(1L).limit(1).comment("Load the system-provided root")
                    .purpose("Attach the Spring transaction example to its domain root").executeForOne(context);
            assertNotNull(root);
            var expected = Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
            for (var type : List.of(BaseEntity.class, TeaQLRuntime.class, SpringJdbcSqlExecutor.class, SqliteDataServiceExecutor.class)) {
                var origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
                assertTrue("source-paired runtime origin: " + type.getName(), origin.startsWith(expected));
            }
        }

        TransactionTemplate transaction(int propagation) {
            var template = new TransactionTemplate(manager); template.setPropagationBehavior(propagation); return template;
        }
        TransactionTemplate transaction() { return transaction(TransactionDefinition.PROPAGATION_REQUIRED); }
        WorkItem create(String title) {
            return Q.workItems().comment("Prepare a Spring work item").purpose("Verify audited business persistence")
                    .newEntity(context).updatePlatform(root).updateTitle(title).updateDescription(null)
                    .auditAs("Seed the Spring transaction example").save(context);
        }
        WorkItem load(long id) {
            return Q.workItems().withIdIs(id).limit(1).comment("Load every scalar field before mutation")
                    .purpose("Keep the original optimistic baseline").executeForOne(context);
        }
        long version(long id) throws SQLException {
            try (var connection = DriverManager.getConnection(url);
                    var query = connection.prepareStatement("SELECT version FROM work_item_data WHERE id=?")) {
                query.setLong(1, id); try (var rows = query.executeQuery()) { assertTrue(rows.next()); return rows.getLong(1); }
            }
        }
        void oracle(long id, String title, String description, long version) throws SQLException {
            try (var connection = DriverManager.getConnection(url);
                    var query = connection.prepareStatement("SELECT title,description,version,platform FROM work_item_data WHERE id=?")) {
                query.setLong(1, id);
                try (var rows = query.executeQuery()) {
                    assertTrue(rows.next()); assertEquals(title, rows.getString(1)); assertEquals(description, rows.getString(2));
                    assertEquals(version, rows.getLong(3)); assertEquals(root.getId().longValue(), rows.getLong(4)); assertFalse(rows.next());
                }
            }
        }
        void emit(Map<String, Object> observations) throws Exception {
            var value = new LinkedHashMap<String, Object>(); value.put("passed", true); value.put("case", kind);
            value.put("round", round); value.put("observations", observations);
            var stored = new ArrayList<Map<String, Object>>();
            try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement();
                    var rows = statement.executeQuery("SELECT id,title,description,version,platform FROM work_item_data ORDER BY id")) {
                while (rows.next()) {
                    var row = new LinkedHashMap<String, Object>(); row.put("id", rows.getLong(1)); row.put("title", rows.getString(2));
                    row.put("description", rows.getString(3)); row.put("version", rows.getLong(4)); row.put("platform", rows.getLong(5)); stored.add(row);
                }
            }
            value.put("stored", stored);
            System.out.println("JAVA_SPRING_BUSINESS=" + new ObjectMapper().writeValueAsString(value));
        }
    }

    @Test public void outerCommitKeepsSparseNoopAndDefersAudit() throws Exception {
        var f = new Fixture("outer-commit-noop"); var clean = f.create("Unchanged"); var row = f.create("Original");
        var view = Q.workItemsWithMinimalFields().withIdIs(clean.getId()).selectTitle().limit(1)
                .selectPlatformWith(Q.platformsWithMinimalFields().selectName())
                .comment("Load a sparse display view").purpose("Preserve NotLoaded through an unchanged save").executeForOne(f.context);
        var state = view.__internalLoadState(); int audits = f.audits.size(), policies = f.policies.size();
        f.transaction().executeWithoutResult(status -> {
            long calls = f.source.connections; view.updateTitle("Temporary").updateTitle("Unchanged");
            view.auditAs("Keep a sparse no-op inside the caller transaction").save(f.context);
            assertEquals(calls, f.source.connections); assertSame(state, view.__internalLoadState());
            assertThrows(TeaQLNotLoadedException.class, () -> E.workItem(view).getDescription().eval());
            row.updateTitle("Committed").auditAs("Commit the actual business edit").save(f.context);
            assertEquals("audit waits for physical commit", audits, f.audits.size()); assertTrue(f.sql.hasActiveTransaction());
        });
        assertEquals(audits + 1, f.audits.size()); assertEquals(policies + 1, f.policies.size());
        assertEquals("Committed", E.workItem(f.load(row.getId())).getTitle().eval());
        assertSame(state, view.__internalLoadState()); assertEquals(Long.valueOf(1), view.getVersion());
        f.oracle(row.getId(), "Committed", null, 2); f.oracle(clean.getId(), "Unchanged", null, 1);
        f.emit(Map.of("auditDelta", 1, "policyDelta", 1, "id", row.getId(), "unchangedId", clean.getId()));
    }

    @Test public void outerRollbackRetainsLatestIntentForOneRetry() throws Exception {
        var f = new Fixture("outer-rollback-retry"); var row = f.load(f.create("Original").getId()); int audits = f.audits.size();
        var error = assertThrows(IllegalStateException.class, () -> f.transaction().executeWithoutResult(status -> {
            row.updateTitle("First").auditAs("Stage the first edit").save(f.context);
            row.updateTitle("Latest").auditAs("Stage the latest edit").save(f.context);
            row.updateDescription("Unsaved later input"); assertEquals(audits, f.audits.size());
            throw new IllegalStateException("EXPECTED_OUTER_ROLLBACK");
        }));
        assertEquals("EXPECTED_OUTER_ROLLBACK", error.getMessage()); assertEquals(audits, f.audits.size());
        assertEquals(Long.valueOf(1), row.getVersion()); assertEquals("Latest", E.workItem(row).getTitle().eval());
        assertEquals("Unsaved later input", E.workItem(row).getDescription().eval());
        assertTrue(row.getUpdatedProperties().containsAll(List.of("title", "description")));
        f.oracle(row.getId(), "Original", null, 1);
        row.auditAs("Retry final business intent once").save(f.context);
        assertEquals(audits + 1, f.audits.size()); assertEquals(Long.valueOf(2), row.getVersion());
        f.oracle(row.getId(), "Latest", "Unsaved later input", 2);
        f.emit(Map.of("auditDelta", 1, "id", row.getId(), "finalVersion", row.getVersion()));
    }

    @Test public void nestedRollbackRestoresOnlyInnerBusinessMutation() throws Exception {
        var f = new Fixture("nested-rollback"); var outer = f.create("Outer original"); var inner = f.create("Inner original");
        int audits = f.audits.size();
        f.transaction().executeWithoutResult(status -> {
            outer.updateTitle("Outer committed").auditAs("Commit only the outer business object").save(f.context);
            f.transaction(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(nested -> {
                inner.updateTitle("Inner retry").auditAs("Abandon the nested business attempt").save(f.context); nested.setRollbackOnly();
            });
            assertEquals(audits, f.audits.size()); assertEquals(Long.valueOf(1), inner.getVersion());
            assertTrue(inner.getUpdatedProperties().contains("title")); assertEquals("Inner retry", E.workItem(inner).getTitle().eval());
            assertEquals("Inner original", E.workItem(f.load(inner.getId())).getTitle().eval());
        });
        assertEquals(audits + 1, f.audits.size()); assertEquals(outer.getId(), f.audits.get(f.audits.size() - 1).entityId());
        f.oracle(outer.getId(), "Outer committed", null, 2); f.oracle(inner.getId(), "Inner original", null, 1);
        inner.auditAs("Retry the independently rolled-back inner object").save(f.context);
        assertEquals(audits + 2, f.audits.size()); f.oracle(inner.getId(), "Inner retry", null, 2);
        f.emit(Map.of("auditDelta", 2, "outerId", outer.getId(), "innerId", inner.getId()));
    }

    @Test public void equalAndRepeatedSavepointsRetainPrecedingBusinessCommit() throws Exception {
        var f = new Fixture("savepoint-boundaries"); var outer = f.create("Outer original"); var inner = f.create("Inner original");
        int audits = f.audits.size();
        f.transaction().executeWithoutResult(status -> {
            outer.updateTitle("Outer committed").auditAs("Preserve the preceding physical work").save(f.context);
            var first = status.createSavepoint(); var second = status.createSavepoint();
            inner.updateTitle("Inner first").auditAs("First abandoned nested intent").save(f.context); status.rollbackToSavepoint(second);
            assertEquals(Long.valueOf(1), inner.getVersion());
            inner.updateTitle("Inner second").auditAs("Second abandoned nested intent").save(f.context); status.rollbackToSavepoint(first);
            assertEquals(Long.valueOf(1), inner.getVersion());
            inner.updateTitle("Inner final").auditAs("Third abandoned nested intent").save(f.context); status.rollbackToSavepoint(first);
            assertEquals(Long.valueOf(1), inner.getVersion()); assertEquals(audits, f.audits.size());
        });
        assertEquals(audits + 1, f.audits.size()); assertEquals(outer.getId(), f.audits.get(f.audits.size() - 1).entityId());
        f.oracle(outer.getId(), "Outer committed", null, 2); f.oracle(inner.getId(), "Inner original", null, 1);
        inner.auditAs("Retry the final intended inner value").save(f.context); f.oracle(inner.getId(), "Inner final", null, 2);
        assertEquals(audits + 2, f.audits.size()); f.emit(Map.of("auditDelta", 2, "outerId", outer.getId(), "innerId", inner.getId()));
    }

    @Test public void requiresNewCommitsWithoutAbsorbingOuterIntent() throws Exception {
        var f = new Fixture("requires-new"); var outer = f.create("Outer original"); var inner = f.create("Inner original"); int audits = f.audits.size();
        assertThrows(IllegalStateException.class, () -> f.transaction().executeWithoutResult(status -> {
            // SQLite permits only one writer. The outer owner has not written or established a read snapshot yet.
            f.transaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(independent ->
                    inner.updateTitle("Inner committed").auditAs("Commit an independent physical transaction").save(f.context));
            assertEquals(audits + 1, f.audits.size()); assertEquals(inner.getId(), f.audits.get(f.audits.size() - 1).entityId());
            outer.updateTitle("Outer retry").auditAs("Roll back only the outer physical transaction").save(f.context);
            throw new IllegalStateException("EXPECTED_OUTER_ROLLBACK");
        }));
        assertEquals(audits + 1, f.audits.size()); assertEquals(Long.valueOf(1), outer.getVersion());
        f.oracle(outer.getId(), "Outer original", null, 1); f.oracle(inner.getId(), "Inner committed", null, 2);
        outer.auditAs("Retry the outer object independently").save(f.context); f.oracle(outer.getId(), "Outer retry", null, 2);
        assertEquals(audits + 2, f.audits.size()); f.emit(Map.of("auditDelta", 2, "outerId", outer.getId(), "innerId", inner.getId()));
    }

    @Test public void failedPostCommitDeliveryCannotInventRollback() throws Exception {
        var f = new Fixture("audit-delivery-failure"); var row = f.create("Original"); int audits = f.audits.size(); f.failDelivery = true;
        var error = assertThrows(RuntimeException.class, () -> f.transaction().executeWithoutResult(status -> {
            row.updateTitle("Committed despite delivery failure").auditAs("Commit before failing the observer").save(f.context);
            assertEquals(audits, f.audits.size());
        }));
        assertTrue(error.toString().contains("EXPECTED_AUDIT_DELIVERY_FAILURE")); f.failDelivery = false;
        assertEquals(audits + 1, f.audits.size()); assertEquals(Long.valueOf(2), row.getVersion()); assertTrue(row.getUpdatedProperties().isEmpty());
        f.oracle(row.getId(), "Committed despite delivery failure", null, 2);
        row.updateDescription("Later successful edit").auditAs("Continue after a delivery-only failure").save(f.context);
        assertEquals(audits + 2, f.audits.size()); f.oracle(row.getId(), "Committed despite delivery failure", "Later successful edit", 3);
        f.emit(Map.of("auditDelta", 2, "id", row.getId(), "finalVersion", row.getVersion()));
    }

    @Test public void checkerAndIntentRejectionNeverEnterSpringProvider() throws Exception {
        var f = new Fixture("admission"); var invalid = Q.workItems().comment("Construct an incomplete work item")
                .purpose("Check before acquiring a database transaction").newEntity(f.context).updatePlatform(f.root);
        long calls = f.source.connections; int policies = f.policies.size(), audits = f.audits.size();
        assertThrows(CheckException.class, () -> invalid.auditAs("Reject the missing required title").save(f.context));
        assertThrows(RuntimeException.class, () -> Q.workItems().limit(1).purpose("Reject a missing query comment").executeForList(f.context));
        assertEquals(calls, f.source.connections); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        var valid = f.create("Valid control"); calls = f.source.connections; audits = f.audits.size(); policies = f.policies.size();
        valid.updateTitle("Rejected intent");
        assertThrows(RuntimeException.class, () -> valid.auditAs(" ").save(f.context));
        assertEquals(calls, f.source.connections); assertEquals(policies, f.policies.size()); assertEquals(audits, f.audits.size());
        f.oracle(valid.getId(), "Valid control", null, 1); f.emit(Map.of("auditDelta", 0, "id", valid.getId(), "rejections", 3));
    }

    @Test public void actualSecondGraphWriteFailureRollsBackAndRetries() throws Exception {
        var f = new Fixture("graph-write-failure"); var first = f.create("First original"); var second = f.create("Second original");
        var graph = Q.platforms().withIdIs(1L).limit(1)
                .selectWorkItemListWith(Q.workItems().orderByIdAscending().limit(10))
                .comment("Load a bounded complete two-member graph").purpose("Verify atomic generated graph mutation").executeForOne(f.context);
        var children = graph.getWorkItemList();
        var a = children.stream().filter(row -> row.getId().equals(first.getId())).findFirst().orElseThrow();
        var b = children.stream().filter(row -> row.getId().equals(second.getId())).findFirst().orElseThrow();
        a.updateTitle("First committed"); b.updateTitle("Second committed");
        try (var connection = DriverManager.getConnection(f.url); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER spring_observe AFTER UPDATE ON work_item_data BEGIN SELECT spring_written(NEW.id); END");
            statement.execute("CREATE TRIGGER spring_fail BEFORE UPDATE ON work_item_data WHEN NEW.title='Second committed' BEGIN SELECT spring_rejected(NEW.id); SELECT RAISE(ABORT,'EXPECTED_SECOND_GRAPH_WRITE'); END");
        }
        int audits = f.audits.size(); f.source.written.clear(); f.source.rejected.clear();
        assertThrows(RuntimeException.class, () -> f.transaction().executeWithoutResult(status ->
                graph.auditAs("Attempt both graph writes in the caller transaction").save(f.context)));
        assertEquals(List.of(first.getId()), f.source.written); assertEquals(List.of(second.getId()), f.source.rejected); assertEquals(audits, f.audits.size());
        assertEquals(Long.valueOf(1), a.getVersion()); assertEquals(Long.valueOf(1), b.getVersion());
        assertTrue(a.getUpdatedProperties().contains("title")); assertTrue(b.getUpdatedProperties().contains("title"));
        f.oracle(first.getId(), "First original", null, 1); f.oracle(second.getId(), "Second original", null, 1);
        try (var connection = DriverManager.getConnection(f.url); var statement = connection.createStatement()) { statement.execute("DROP TRIGGER spring_fail"); }
        f.source.written.clear(); graph.auditAs("Retry the same generated object graph").save(f.context);
        assertEquals(List.of(first.getId(), second.getId()), f.source.written); assertEquals(audits + 2, f.audits.size());
        f.oracle(first.getId(), "First committed", null, 2); f.oracle(second.getId(), "Second committed", null, 2);
        try (var connection = DriverManager.getConnection(f.url); var statement = connection.createStatement()) { statement.execute("DROP TRIGGER spring_observe"); }
        f.emit(Map.of("auditDelta", 2, "firstId", first.getId(), "secondId", second.getId(), "failedAfterWrites", 1));
    }
}
