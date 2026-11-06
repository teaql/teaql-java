package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** ML-09/ML-11: one generated graph, three operation kinds, actual SQLite rollback. */
public class MixedGraphExampleTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2032, 2, 29, 10, 15, 30);

    private static final class Fixture {
        final String scenario, database;
        final SQLiteDataSource source = new SQLiteDataSource();
        final ContextRootProbe.ObservedSql sql;
        final UserContext context;
        final PurchaseOrder order;
        final OrderItem removed, added;
        final List<Map<String, Object>> operands = new ArrayList<>();
        final List<Map<String, Object>> beforeCommit = new ArrayList<>();
        boolean armed, failSecond;
        int policies, begins, commits, rollbacks, audits, attempts;

        Fixture(String scenario) {
            this.scenario = scenario;
            String base = System.getenv("TEAQL_FIX_E_DB");
            assertNotNull("the runner owns and retains the actual SQLite files", base);
            database = base + ".mixed-graph-" + scenario + ".sqlite";
            source.setUrl("jdbc:sqlite:" + database);
            if (scenario.equals("commit-failure")) source.setEnforceForeignKeys(true);
            sql = new ContextRootProbe.ObservedSql(source);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql));
            ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source) {
                @Override public <T> T executeInTransaction(UserContext caller, TransactionCallback<T> action) {
                    if (armed) begins++;
                    try {
                        T result = super.executeInTransaction(caller, () -> {
                            T value = action.doInTransaction();
                            if (armed && scenario.equals("commit-failure")) {
                                // The callback has completed, but JdbcSqlExecutor
                                // has not called Connection.commit() yet. These
                                // reads use its real transaction-bound connection.
                                var parent = sql.queryForList("SELECT version FROM purchase_order_data WHERE id=?",
                                        new Object[]{order.getId()}).getFirst();
                                var old = sql.queryForList("SELECT version FROM order_item_data WHERE id=?",
                                        new Object[]{removed.getId()}).getFirst();
                                var fresh = sql.queryForList("SELECT version,purchase_order FROM order_item_data WHERE id=?",
                                        new Object[]{added.getId()}).getFirst();
                                int violations = sql.queryForList("PRAGMA foreign_key_check", new Object[0]).size();
                                assertEquals(3, attempts - beforeCommit.size() * 3);
                                assertEquals(0, audits);
                                assertEquals(2L, ((Number) parent.get("version")).longValue());
                                assertEquals(-2L, ((Number) old.get("version")).longValue());
                                assertEquals(1L, ((Number) fresh.get("version")).longValue());
                                assertEquals(order.getId().longValue(), ((Number) fresh.get("purchase_order")).longValue());
                                beforeCommit.add(Map.of("writes", 3, "audits", audits, "violations", violations,
                                        "parentVersion", 2L, "oldVersion", -2L, "newVersion", 1L));
                            }
                            return value;
                        });
                        if (armed) commits++;
                        return result;
                    } catch (RuntimeException | Error error) {
                        if (armed) rollbacks++;
                        throw error;
                    }
                }
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation request) {
                    if (armed) {
                        attempts++;
                        if (failSecond && attempts == 2)
                            throw new TeaQLRuntimeException("MIXED_GRAPH_INJECTED_SECOND_WRITE_FAILURE");
                    }
                    var mutation = (EntityPersistenceMutation) request;
                    var target = mutation.getEntity();
                    var operand = new LinkedHashMap<String, Object>();
                    operand.put("entity", target.typeName()); operand.put("id", target.getId());
                    operand.put("action", mutation.getAction().name()); operand.put("originalVersion", target.getVersion());
                    MutationResult result = super.mutate(caller, request);
                    if (armed) operands.add(operand);
                    return result;
                }
            };
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return NOW.toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext caller) { return NOW; }
            };
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() {
                    return new MutationPolicyIdentity("mixed-graph-example", "1", "sha256:mixed-graph-example");
                }
                public MutationDecision review(UserContext caller, MutationPlan plan) {
                    if (armed) {
                        policies++;
                        assertEquals(3, plan.operations().size());
                        var kinds = EnumSet.noneOf(MutationOperationKind.class);
                        for (var operation : plan.operations()) {
                            assertTrue("exactly one operation of each kind", kinds.add(operation.kind()));
                            switch (operation.kind()) {
                                case CREATE -> {
                                    assertEquals(new EntityKey("OrderItem", added.getId()), operation.entity());
                                    assertNull(operation.originalVersion());
                                }
                                case UPDATE -> {
                                    assertEquals(new EntityKey("PurchaseOrder", order.getId()), operation.entity());
                                    assertEquals(Long.valueOf(1), operation.originalVersion());
                                    assertEquals("updated", operation.changedValues().get("contact_phone"));
                                }
                                case DELETE -> {
                                    assertEquals(new EntityKey("OrderItem", removed.getId()), operation.entity());
                                    assertEquals(Long.valueOf(1), operation.originalVersion());
                                }
                                default -> fail("unexpected mutation kind");
                            }
                        }
                        assertEquals(EnumSet.of(MutationOperationKind.CREATE, MutationOperationKind.UPDATE,
                                MutationOperationKind.DELETE), kinds);
                    }
                    return MutationDecision.allow();
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(ids).businessClock(clock).executionLogging(false)
                    .mutationPolicyRegistry(key -> Optional.of(policy))
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller, ExecutionMetadata event) {}
                    }).build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(),
                    (AppAuditEventSink) (caller, event) -> { if (armed) audits++; });
            context.ensureSchema();
            order = Q.purchaseOrders().comment("Prepare the mixed graph parent")
                    .purpose("Verify atomic create update and deletion through generated APIs").newEntity(context);
            order.updateContactPhone("initial"); order.updateInternalNote("mixed graph control");
            removed = Q.orderItems().comment("Prepare an existing child for later deletion")
                    .purpose("Verify pending deletion joins the same audited graph save").newEntity(context);
            removed.updateProductName("Removed item"); removed.updateUnitPrice(BigDecimal.ONE);
            order.addOrderItem(removed);
            order.auditAs("Persist the existing parent and child before the mixed mutation").save(context);
            assertEquals(Long.valueOf(1), order.getVersion()); assertEquals(Long.valueOf(1), removed.getVersion());

            added = Q.orderItems().comment("Prepare a newly identified replacement child")
                    .purpose("Verify insertion joins the same graph as update and deletion").newEntity(context);
            added.updateProductName("Added item"); added.updateUnitPrice(new BigDecimal("2"));
            order.addOrderItem(added);
            assertTrue(added.getId() > 0); assertNull(added.getVersion());
            assertNotEquals(added.getId(), removed.getId());
            order.updateContactPhone("updated");
            int readsBefore = sql.queries, writesBefore = sql.writes;
            removed.markForDeletion();
            assertEquals("markForDeletion cannot read the provider", readsBefore, sql.queries);
            assertEquals("markForDeletion cannot write the provider", writesBefore, sql.writes);
            sql.writes = 0; sql.transactions = 0; armed = true;
        }

        void save() { order.auditAs("Apply the reviewed insertion update and deletion as one graph").save(context); }

        void storage(boolean committed) throws Exception {
            var parent = Q.purchaseOrders().withIdIs(order.getId()).limit(1)
                    .comment("Read the mixed graph parent after a save attempt")
                    .purpose("Compare generated E values with the atomic commit outcome").executeForOne(context);
            assertEquals(committed ? "updated" : "initial", E.purchaseOrder(parent).getContactPhone().eval());
            assertEquals(Long.valueOf(committed ? 2 : 1), E.purchaseOrder(parent).getVersion().eval());
            var child = Q.orderItems().withIdIs(added.getId()).limit(1)
                    .comment("Read the replacement child after a save attempt")
                    .purpose("Verify a rolled back insertion is absent and a committed insertion is readable").executeForOne(context);
            var old = Q.orderItems().withIdIs(removed.getId()).limit(1)
                    .comment("Read the deleted child through the ordinary generated query")
                    .purpose("Verify tombstones are excluded from the normal live graph").executeForOne(context);
            if (committed) {
                assertNotNull(child); assertNull(old);
                assertEquals("Added item", E.orderItem(child).getProductName().eval());
                assertEquals(0, new BigDecimal("2").compareTo(E.orderItem(child).getUnitPrice().eval()));
                assertEquals(Long.valueOf(1), E.orderItem(child).getVersion().eval());
            } else { assertNull(child); assertNotNull(old); }
            // Independent, read-only SQL oracle, not an alternative business write path.
            try (var connection = source.getConnection(); var statement = connection.prepareStatement(
                    "SELECT version,purchase_order,product_name FROM order_item_data WHERE id=?")) {
                statement.setLong(1, removed.getId());
                try (var row = statement.executeQuery()) {
                    assertTrue(row.next()); assertEquals(committed ? -2L : 1L, row.getLong(1));
                    assertEquals(order.getId().longValue(), row.getLong(2));
                    assertEquals("Removed item", row.getString(3)); assertFalse(row.next());
                }
                statement.setLong(1, added.getId());
                try (var row = statement.executeQuery()) {
                    assertEquals(committed, row.next());
                    if (committed) {
                        assertEquals(1L, row.getLong(1)); assertEquals(order.getId().longValue(), row.getLong(2));
                        assertEquals("Added item", row.getString(3)); assertFalse(row.next());
                    }
                }
            }
        }

        Map<String, Integer> effects() {
            return Map.of("policies", policies, "providerAttempts", attempts, "providerWrites", operands.size(),
                    "sqlWrites", sql.writes, "begins", begins, "commits", commits, "rollbacks", rollbacks, "audits", audits);
        }

        void committed(Map<String, Integer> rejected) throws Exception {
            assertEquals(Long.valueOf(2), order.getVersion());
            assertEquals(Long.valueOf(-2), removed.getVersion());
            assertEquals(Long.valueOf(1), added.getVersion());
            assertTrue(added.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
            storage(true);
            var beforeNoOp = effects();
            save();
            assertEquals("a repeated save cannot write or audit the committed graph again", beforeNoOp, effects());
            storage(true);
            var result = new LinkedHashMap<String, Object>();
            result.put("case", scenario); result.put("round", Integer.parseInt(System.getenv("TEAQL_FIX_E_ROUND")));
            result.put("passed", true); result.put("orderId", order.getId()); result.put("removedId", removed.getId());
            result.put("addedId", added.getId()); result.put("database", java.nio.file.Path.of(database).getFileName().toString());
            result.put("rejected", rejected); result.put("final", effects()); result.put("operands", operands);
            result.put("generatedQAndEReadback", true); result.put("readOnlySqlOracle", true);
            result.put("repeatedSaveIsNoOp", true);
            if (scenario.equals("commit-failure")) result.put("beforeCommit", beforeCommit);
            System.out.println("JAVA_MIXED_GRAPH_CASE=" + new ObjectMapper().writeValueAsString(result));
        }
    }

    @Test public void oneReviewedGraphCreatesUpdatesAndDeletesAtomically() throws Exception {
        var f = new Fixture("commit"); f.save();
        assertEquals(1, f.policies); assertEquals(3, f.attempts); assertEquals(3, f.operands.size());
        assertEquals(3, f.sql.writes); assertEquals(1, f.begins); assertEquals(1, f.commits);
        assertEquals(0, f.rollbacks); assertEquals(3, f.audits);
        f.committed(Map.of());
    }

    @Test public void secondProviderFailureRollsBackAllKindsAndOriginalGraphRetries() throws Exception {
        var f = new Fixture("rollback");
        long orderId = f.order.getId(), oldId = f.removed.getId(), addedId = f.added.getId();
        f.failSecond = true;
        var error = assertThrows(TeaQLRuntimeException.class, f::save);
        assertTrue(error.getMessage(), error.getMessage().contains("MIXED_GRAPH_INJECTED_SECOND_WRITE_FAILURE"));
        assertEquals(1, f.policies); assertEquals(2, f.attempts); assertEquals(1, f.operands.size());
        assertEquals(1, f.sql.writes); assertEquals(1, f.begins); assertEquals(0, f.commits);
        assertEquals(1, f.rollbacks); assertEquals(0, f.audits);
        assertEquals(Long.valueOf(1), f.order.getVersion()); assertEquals(Long.valueOf(1), f.removed.getVersion());
        assertNull(f.added.getVersion()); assertTrue(f.added.newItem());
        assertFalse(f.added.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        assertEquals("updated", E.purchaseOrder(f.order).getContactPhone().eval());
        f.storage(false);
        var rejected = f.effects();
        f.failSecond = false; f.save();
        assertEquals(orderId, f.order.getId().longValue()); assertEquals(oldId, f.removed.getId().longValue());
        assertEquals(addedId, f.added.getId().longValue());
        assertEquals(2, f.policies); assertEquals(5, f.attempts); assertEquals(4, f.operands.size());
        assertEquals(4, f.sql.writes); assertEquals(2, f.begins); assertEquals(1, f.commits);
        assertEquals(1, f.rollbacks); assertEquals(3, f.audits);
        f.committed(rejected);
    }

    @Test public void deferredForeignKeyRejectsRealCommitAfterAllWritesAndOriginalGraphRetries() throws Exception {
        var f = new Fixture("commit-failure");
        long orderId = f.order.getId(), oldId = f.removed.getId(), addedId = f.added.getId();
        // Test-only auxiliary schema injects a deferred FK violation. Every
        // business row is still created and changed through generated APIs.
        try (var connection = f.source.getConnection(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("PRAGMA foreign_keys")) {
                assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
            }
            statement.execute("CREATE TABLE IF NOT EXISTS core236_commit_parent(id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE IF NOT EXISTS core236_commit_guard(id INTEGER PRIMARY KEY, missing_parent INTEGER NOT NULL, " +
                    "FOREIGN KEY(missing_parent) REFERENCES core236_commit_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            statement.execute("CREATE TRIGGER core236_commit_fault AFTER UPDATE ON purchase_order_data WHEN NEW.id=" + orderId +
                    " BEGIN INSERT INTO core236_commit_guard(missing_parent) VALUES(1); END");
        }
        var error = assertThrows(TeaQLRuntimeException.class, f::save);
        boolean foreignKey = false;
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            foreignKey |= String.valueOf(cause.getMessage()).toLowerCase(Locale.ROOT).contains("foreign key constraint failed");
        assertTrue("the real deferred SQLite constraint must reject commit", foreignKey);
        assertEquals(1, f.beforeCommit.size());
        assertEquals(1, f.beforeCommit.getFirst().get("violations"));
        assertEquals(1, f.policies); assertEquals(3, f.attempts); assertEquals(3, f.operands.size());
        assertEquals(3, f.sql.writes); assertEquals(1, f.begins); assertEquals(0, f.commits);
        assertEquals(1, f.rollbacks); assertEquals(0, f.audits);
        assertEquals(Long.valueOf(1), f.order.getVersion()); assertEquals(Long.valueOf(1), f.removed.getVersion());
        assertNull(f.added.getVersion()); assertTrue(f.added.newItem());
        assertFalse(f.added.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        f.storage(false);
        var rejected = f.effects();
        try (var connection = f.source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER core236_commit_fault");
            try (var rows = statement.executeQuery("SELECT count(*) FROM core236_commit_guard")) {
                assertTrue(rows.next()); assertEquals(0L, rows.getLong(1));
            }
        }
        f.save();
        assertEquals(orderId, f.order.getId().longValue()); assertEquals(oldId, f.removed.getId().longValue());
        assertEquals(addedId, f.added.getId().longValue());
        assertEquals(2, f.beforeCommit.size()); assertEquals(0, f.beforeCommit.get(1).get("violations"));
        assertEquals(2, f.policies); assertEquals(6, f.attempts); assertEquals(6, f.operands.size());
        assertEquals(6, f.sql.writes); assertEquals(2, f.begins); assertEquals(1, f.commits);
        assertEquals(1, f.rollbacks); assertEquals(3, f.audits);
        f.committed(rejected);
        assertEquals("unchanged save cannot reach commit again", 2, f.beforeCommit.size());
    }
}
