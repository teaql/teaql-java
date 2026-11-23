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

/** Real two-object SQLite saves through unchanged generated Q/E and audited APIs. */
public class ReviewedPlanExampleTest {
    enum Phase { NONE, POLICY, APPROVAL, WARNING, TRANSACTION, PROVIDER }
    private static final LocalDateTime NOW = LocalDateTime.of(2032, 2, 29, 10, 15, 30);

    private static final class Fixture {
        final Phase phase;
        final String database;
        final ContextRootProbe.ObservedSql sql;
        final UserContext context;
        final PurchaseOrder order;
        final OrderItem item;
        final List<Map<String, Object>> writeOperands = new ArrayList<>();
        boolean armed, tamper = true;
        int policies, begins, commits, rollbacks, audits;
        String reviewedPhone;
        BigDecimal reviewedPrice;

        Fixture(Phase phase) {
            this.phase = phase;
            String base = System.getenv("TEAQL_FIX_E_DB");
            assertNotNull("the runner owns and retains the actual SQLite files", base);
            database = base + ".reviewed-plan-" + phase.name().toLowerCase(Locale.ROOT) + ".sqlite";
            var source = new SQLiteDataSource(); source.setUrl("jdbc:sqlite:" + database);
            sql = new ContextRootProbe.ObservedSql(source);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql));
            ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source) {
                @Override public <T> T executeInTransaction(UserContext caller, TransactionCallback<T> action) {
                    if (armed) begins++;
                    try {
                        T result = super.executeInTransaction(caller, () -> {
                            edit(Phase.TRANSACTION);
                            return action.doInTransaction();
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
                        var target = (BaseEntity) ((EntityPersistenceMutation) request).getEntity();
                        String field = target.typeName().equals("PurchaseOrder") ? "contact_phone" : "unit_price";
                        Object expected = field.equals("contact_phone") ? reviewedPhone : reviewedPrice;
                        assertEquals("the provider must consume the value actually reviewed", expected,
                                target.__internalGet(field));
                        writeOperands.add(Map.of("entity", target.typeName(), "field", field,
                                "value", target.__internalGet(field).toString(), "originalVersion", target.getVersion()));
                    }
                    MutationResult result = super.mutate(caller, request);
                    // Both actual writes have happened, but the transaction has
                    // not committed. A late edit must roll back the whole graph.
                    if (armed && writeOperands.size() == 2) edit(Phase.PROVIDER);
                    return result;
                }
            };
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return NOW.toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext caller) { return NOW; }
            };
            var policy = new MutationPolicy() {
                public MutationPolicyIdentity identity() {
                    return new MutationPolicyIdentity("reviewed-sqlite-example", "1", "sha256:reviewed-sqlite-example");
                }
                public MutationDecision review(UserContext caller, MutationPlan plan) {
                    if (armed) {
                        policies++;
                        assertEquals(2, plan.operations().size());
                        for (var operation : plan.operations()) {
                            assertEquals(MutationOperationKind.UPDATE, operation.kind());
                            assertEquals(Long.valueOf(1), operation.originalVersion());
                            if (operation.entity().entity().equals("PurchaseOrder"))
                                reviewedPhone = (String) operation.changedValues().get("contact_phone");
                            else if (operation.entity().entity().equals("OrderItem"))
                                reviewedPrice = (BigDecimal) operation.changedValues().get("unit_price");
                            else fail("unexpected entity in the reviewed graph");
                        }
                        assertNotNull(reviewedPhone); assertNotNull(reviewedPrice);
                        edit(Phase.POLICY);
                    }
                    return MutationDecision.allow();
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(ids).businessClock(clock).executionLogging(false)
                    .mutationPolicyRegistry(key -> Optional.of(policy))
                    .mutationPolicyApprovalProvider(identity -> { edit(Phase.APPROVAL); return Optional.empty(); })
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller, ExecutionMetadata event) {}
                        public void writeMutationGovernanceEvent(UserContext caller, MutationGovernanceEvent event) {
                            edit(Phase.WARNING);
                        }
                    }).build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(),
                    (AppAuditEventSink) (caller, event) -> { if (armed) audits++; });
            context.ensureSchema();
            order = Q.purchaseOrders().comment("Prepare a reviewed parent mutation")
                    .purpose("Verify reviewed graph values against actual SQLite storage").newEntity(context);
            order.updateContactPhone("initial"); order.updateInternalNote("reviewed graph control");
            item = Q.orderItems().comment("Prepare the reviewed child mutation")
                    .purpose("Verify that parent and child commit or roll back together").newEntity(context);
            item.updatePurchaseOrder(order); item.updateProductName("Reviewed item"); item.updateUnitPrice(BigDecimal.ONE);
            item.auditAs("Persist the unchanged generated graph control").save(context);
            assertEquals(Long.valueOf(1), order.getVersion()); assertEquals(Long.valueOf(1), item.getVersion());
            sql.writes = 0; sql.transactions = 0;
            order.updateContactPhone("reviewed"); item.updateUnitPrice(new BigDecimal("2"));
            armed = true;
        }

        void edit(Phase current) {
            if (armed && tamper && phase == current) order.updateContactPhone("not-reviewed");
        }
        void save() { item.auditAs("Persist exactly the reviewed parent and child plan").save(context); }
        void storage(String phone, BigDecimal price, long version) {
            var parent = Q.purchaseOrders().withIdIs(order.getId()).limit(1)
                    .comment("Read the authoritative reviewed parent")
                    .purpose("Verify that rejected or accepted graph values match storage").executeForOne(context);
            var child = Q.orderItems().withIdIs(item.getId()).limit(1)
                    .comment("Read the authoritative reviewed child")
                    .purpose("Verify that the two graph rows share the expected commit outcome").executeForOne(context);
            assertEquals(phone, E.purchaseOrder(parent).getContactPhone().eval());
            assertEquals(0, price.compareTo(E.orderItem(child).getUnitPrice().eval()));
            assertEquals(Long.valueOf(version), parent.getVersion());
            assertEquals(Long.valueOf(version), child.getVersion());
        }
        Map<String, Object> effects() {
            return Map.of("policies", policies, "providerWrites", writeOperands.size(), "sqlWrites", sql.writes,
                    "begins", begins, "commits", commits, "rollbacks", rollbacks, "audits", audits);
        }
        void receipt(Map<String, Object> rejected) throws Exception {
            var result = new LinkedHashMap<String, Object>();
            result.put("phase", phase.name()); result.put("round", Integer.parseInt(System.getenv("TEAQL_FIX_E_ROUND")));
            result.put("passed", true); result.put("orderId", order.getId()); result.put("itemId", item.getId());
            result.put("database", java.nio.file.Path.of(database).getFileName().toString());
            result.put("rejected", rejected); result.put("final", effects()); result.put("writeOperands", writeOperands);
            result.put("storedPhone", phase == Phase.NONE ? "reviewed" : "not-reviewed");
            result.put("storedPrice", "2"); result.put("storedVersion", 2);
            result.put("generatedQAndEReadback", true);
            System.out.println("JAVA_REVIEWED_SQLITE_CASE=" + new ObjectMapper().writeValueAsString(result));
        }
    }

    @Test public void stableReviewedGraphCommitsBothRows() throws Exception {
        var f = new Fixture(Phase.NONE); f.save();
        assertEquals(1, f.policies); assertEquals(2, f.writeOperands.size());
        assertEquals(1, f.begins); assertEquals(1, f.commits); assertEquals(0, f.rollbacks); assertEquals(2, f.audits);
        assertEquals(2, f.sql.writes);
        f.storage("reviewed", new BigDecimal("2"), 2);
        assertTrue(f.item.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        f.receipt(Map.of());
    }
    @Test public void policyEditRejectsBeforeWriteAndRetryRechecks() throws Exception { rejects(Phase.POLICY); }
    @Test public void approvalEditRejectsBeforeWriteAndRetryRechecks() throws Exception { rejects(Phase.APPROVAL); }
    @Test public void warningEditRejectsBeforeWriteAndRetryRechecks() throws Exception { rejects(Phase.WARNING); }
    @Test public void transactionEditRollsBackBeforeWriteAndRetryRechecks() throws Exception { rejects(Phase.TRANSACTION); }
    @Test public void lateProviderEditRollsBackBothWritesAndRetryRechecks() throws Exception { rejects(Phase.PROVIDER); }

    private static void rejects(Phase phase) throws Exception {
        var f = new Fixture(phase);
        var error = assertThrows(TeaQLRuntimeException.class, f::save);
        assertTrue(error.getMessage(), error.getMessage().contains("MUTATION_PLAN_CHANGED_AFTER_REVIEW"));
        assertFalse(error.getMessage().contains("not-reviewed"));
        assertEquals(1, f.policies); assertEquals(0, f.commits); assertEquals(0, f.audits);
        int initialWrites = phase == Phase.PROVIDER ? 2 : 0;
        boolean transaction = phase == Phase.TRANSACTION || phase == Phase.PROVIDER;
        assertEquals(initialWrites, f.writeOperands.size()); assertEquals(initialWrites, f.sql.writes);
        assertEquals(transaction ? 1 : 0, f.begins); assertEquals(transaction ? 1 : 0, f.rollbacks);
        assertEquals(Long.valueOf(1), f.order.getVersion()); assertEquals(Long.valueOf(1), f.item.getVersion());
        assertEquals("not-reviewed", E.purchaseOrder(f.order).getContactPhone().eval());
        assertFalse(f.item.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        f.storage("initial", BigDecimal.ONE, 1);
        var rejected = new LinkedHashMap<>(f.effects());
        rejected.put("code", "MUTATION_PLAN_CHANGED_AFTER_REVIEW"); rejected.put("bothStoredRowsUnchanged", true);
        f.tamper = false; f.save();
        assertEquals(2, f.policies); assertEquals(initialWrites + 2, f.writeOperands.size());
        assertEquals(initialWrites + 2, f.sql.writes); assertEquals(1, f.commits); assertEquals(2, f.audits);
        assertEquals(Long.valueOf(2), f.order.getVersion()); assertEquals(Long.valueOf(2), f.item.getVersion());
        assertTrue(f.item.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        f.storage("not-reviewed", new BigDecimal("2"), 2); f.receipt(rejected);
    }
}
