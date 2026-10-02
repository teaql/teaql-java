package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** #202: actual SQLite execution and safe audit. Generated API acceptance is a separate gate. */
public class GraphTraceSqliteTest {
    public static final class GraphEntity extends BaseEntity {
        private final String type;
        private final Map<String, Object> values = new HashMap<>();
        public GraphEntity(String type) { this.type = type; }
        @Override public String typeName() { return type; }
        @Override public Object __internalGet(String field) {
            return field.equals("name") || field.equals("memo") || field.equals("children") ? values.get(field) : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name") || field.equals("memo") || field.equals("children")) values.put(field, value);
            else super.__internalSet(field, value);
        }
    }

    static final class Fixture {
        final List<ExecutionMetadata> sql = new CopyOnWriteArrayList<>();
        final List<SafeAuditEvent> audit = new CopyOnWriteArrayList<>();
        final List<EntityPersistenceMutation> commands = new CopyOnWriteArrayList<>();
        final List<Integer> itemInsertBatchSizes = new CopyOnWriteArrayList<>();
        final List<Integer> itemUpdateBatchSizes = new CopyOnWriteArrayList<>();
        final List<Integer> itemDeleteBatchSizes = new CopyOnWriteArrayList<>();
        final List<Integer> itemRecoverBatchSizes = new CopyOnWriteArrayList<>();
        final JdbcSqlExecutor driver;
        final DefaultUserContext context;
        volatile boolean failReadback;
        volatile boolean unknownUpdateCounts;

        Fixture() throws Exception {
            var ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-graph-trace-", ".db"));
            driver = new JdbcSqlExecutor(ds) {
                @Override public int[] batchUpdate(String text, List<Object[]> rows) {
                    if (text.startsWith("INSERT INTO order_item_data")) {
                        itemInsertBatchSizes.add(rows.size());
                        assertTrue("audit may be emitted only after the transaction commits", audit.isEmpty());
                    }
                    if (text.startsWith("UPDATE order_item_data") && !rows.isEmpty()) {
                        assertTrue("audit may be emitted only after the transaction commits", audit.isEmpty());
                        Object[] first = rows.get(0);
                        if (first.length != 3) itemUpdateBatchSizes.add(rows.size());
                        else if (((Number) first[0]).longValue() < 0) itemDeleteBatchSizes.add(rows.size());
                        else itemRecoverBatchSizes.add(rows.size());
                    }
                    int[] counts = super.batchUpdate(text, rows);
                    if (unknownUpdateCounts && text.startsWith("UPDATE order_item_data")) {
                        Arrays.fill(counts, java.sql.Statement.SUCCESS_NO_INFO);
                    }
                    return counts;
                }
                @Override public List<Map<String, Object>> queryForList(String text, Object[] args) {
                    if (failReadback && text.startsWith("SELECT * FROM") && text.contains("customer_order_data")) {
                        // A real driver failure after a successful write, inside the same transaction.
                        execute("DROP TABLE customer_order_data");
                    }
                    return super.queryForList(text, args);
                }
            };
            var metadata = new SimpleEntityMetaFactory();
            for (String type : List.of("CustomerOrder", "OrderItem", "Payment", "PaymentAttempt", "Shipment")) {
                var descriptor = new SQLEntityDescriptor();
                descriptor.setType(type);
                descriptor.setTargetType(GraphEntity.class);
                descriptor.setEntitySupplier(() -> new GraphEntity(type));
                descriptor.setDataService("sqlite");
                descriptor.setAuditMaskFields(List.of("name"));
                for (String field : List.of("id", "version", "name", "memo")) {
                    boolean textField = field.equals("name") || field.equals("memo");
                    var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                            textField ? String.class : Long.class);
                    property.setColumnType(textField ? "VARCHAR(255)" : "BIGINT");
                }
                var children = new Relation();
                children.setName("children");
                children.setOwner(descriptor);
                children.setType(new SimplePropertyType(SmartList.class));
                var properties = new ArrayList<>(descriptor.getProperties());
                properties.add(children);
                descriptor.setProperties(properties);
                metadata.register(descriptor);
            }
            var provider = new SqliteDataServiceExecutor("sqlite", driver, ds) {
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation mutation) {
                    commands.add((EntityPersistenceMutation) mutation);
                    return super.mutate(caller, mutation);
                }
                @Override public List<MutationResult> mutateBatch(UserContext caller, MutationBatchRequest request) {
                    request.items().forEach(item -> commands.add((EntityPersistenceMutation) item));
                    return super.mutateBatch(caller, request);
                }
            };
            var ids = new AtomicLong(1000);
            var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("sqlite", provider)
                    .idGenerationService((caller, entity) -> ids.getAndIncrement())
                    .logSink((caller, entry) -> sql.add(entry)).build();
            context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audit.add(event));
            context.ensureSchema();
            clear();
        }

        void clear() {
            sql.clear(); audit.clear(); commands.clear(); itemInsertBatchSizes.clear();
            itemUpdateBatchSizes.clear(); itemDeleteBatchSizes.clear(); itemRecoverBatchSizes.clear();
        }

        GraphEntity create(String type, long id, String name) {
            var entity = new GraphEntity(type);
            // Fixed identities are fixture setup, using the runtime bootstrap's new-key contract.
            entity.__internalInitializeNewEntityId(id);
            entity.updateProperty("name", name);
            return entity;
        }
    }

    @Test public void normativeGraphRetainsCommandSqlReadbackAndCommittedAuditLineage() throws Exception {
        var fixture = new Fixture();
        var removed = fixture.create("OrderItem", 202, "deleted item");
        removed.auditAs("seed deleted fixture").save(fixture.context);
        fixture.clear();
        var root = fixture.create("CustomerOrder", 100, "root value");
        var item = fixture.create("OrderItem", 201, "item value");
        var payment = fixture.create("Payment", 100, "payment value"); // Same ID, distinct model type.
        payment.setComment("authorize payment");
        var attempt = fixture.create("PaymentAttempt", 401, "attempt value");
        payment.__internalSet("children", List.of(attempt));
        var shipment = fixture.create("Shipment", 501, "shipment value");
        shipment.setComment("dispatch shipment");
        removed.markForDeletion();
        removed.setComment("remove unavailable item");
        root.__internalSet("children", List.of(item, payment, shipment, removed));

        root.auditAs("submit order").save(fixture.context);

        assertEquals(6, fixture.commands.size());
        assertEquals(6, fixture.audit.size());
        assertTrue(fixture.context.getTraceChain().isEmpty());
        for (var command : fixture.commands) {
            var entity = command.getEntity();
            var event = fixture.audit.stream().filter(value -> value.entityType().equals(entity.typeName())
                    && value.entityId().equals(entity.getId())).findFirst().orElseThrow();
            assertEquals(command.getTraceChain(), event.traceChain());
            var writes = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION
                    && entry.getTraceChain().get(1).getName().equals(entity.typeName())
                    && entry.getMutationLineage().equals(command.getTraceChain())).toList();
            assertFalse("actual mutation SQL missing for " + entity.typeName(), writes.isEmpty());
            for (var entry : writes) {
                assertEquals("success", entry.getExecutionOutcome());
                assertEquals("CustomerOrder", entry.getTraceChain().get(0).getName());
                assertEquals("sqlite", entry.getTraceChain().get(entry.getTraceChain().size() - 2).getName());
                assertEquals(command.getAction() == EntityPersistenceMutation.Action.DELETE ? "delete" : "insert",
                        entry.getTraceChain().get(entry.getTraceChain().size() - 1).getName());
            }
            assertTrue("authoritative readback must retain the same lineage", fixture.sql.stream().anyMatch(entry ->
                    entry.getOperation() == DataServiceOperation.QUERY && entry.getMutationLineage().equals(command.getTraceChain())));
        }
        assertLineage(fixture.audit, "Payment", 100, List.of("submit order", "authorize payment"));
        assertLineage(fixture.audit, "PaymentAttempt", 401, List.of("submit order", "authorize payment"));
        assertLineage(fixture.audit, "OrderItem", 202, List.of("submit order", "remove unavailable item"));
        assertLineage(fixture.audit, "OrderItem", 201, List.of("submit order"));
        assertEquals("root value", root.getProperty("name"));
        assertEquals(Long.valueOf(1), root.getVersion());
        assertEquals(Long.valueOf(-2), removed.getVersion());
    }

    @Test public void realSqliteProviderFailureKeepsAttemptedLineageWithoutCommittedAudit() throws Exception {
        var fixture = new Fixture();
        fixture.driver.execute("CREATE UNIQUE INDEX payment_name_unique ON payment_data(name)");
        fixture.create("Payment", 300, "duplicate payment").auditAs("seed conflict").save(fixture.context);
        fixture.clear();
        var root = fixture.create("CustomerOrder", 100, "failed root");
        var payment = fixture.create("Payment", 200, "duplicate payment");
        payment.setComment("reject duplicate payment");
        root.__internalSet("children", List.of(payment));
        assertThrows(RuntimeException.class, () -> root.auditAs("attempt graph transaction").save(fixture.context));
        assertTrue("rollback must not emit committed audit", fixture.audit.isEmpty());
        var failure = fixture.sql.stream().filter(entry -> "failure".equals(entry.getBatchOutcome())).findFirst().orElseThrow();
        assertEquals("JDBC provides no member counts for this failure; do not invent them", "unknown", failure.getExecutionOutcome());
        assertNull(failure.getAffectedRows());
        assertEquals(List.of("attempt graph transaction", "reject [REDACTED]"), reasons(failure.getMutationLineage()));
        assertEquals(Long.valueOf(200), failure.getMutationLineage().get(1).getEntityId());
        assertTrue("successful earlier statement is not rewritten as failure", fixture.sql.stream().anyMatch(entry ->
                entry.getOperation() == DataServiceOperation.MUTATION && "success".equals(entry.getExecutionOutcome())));
        assertTrue(fixture.driver.queryForList("SELECT id FROM customer_order_data WHERE id = ?", new Object[]{100L}).isEmpty());
        assertTrue(fixture.context.getTraceChain().isEmpty());
    }

    @Test public void realReadbackFailureDoesNotEraseSuccessfulWriteTraceAndCanRetry() throws Exception {
        var fixture = new Fixture();
        var root = fixture.create("CustomerOrder", 100, "readback fixture");
        fixture.failReadback = true;
        assertThrows(RuntimeException.class, () -> root.auditAs("readback failure intent").save(fixture.context));
        assertTrue(fixture.audit.isEmpty());
        var write = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION).findFirst().orElseThrow();
        var readback = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.QUERY
                && "failure".equals(entry.getExecutionOutcome())).findFirst().orElseThrow();
        assertEquals("success", write.getExecutionOutcome());
        assertEquals(write.getMutationLineage(), readback.getMutationLineage());
        assertEquals(List.of("readback failure intent"), reasons(write.getMutationLineage()));
        assertEquals("insert", write.getStatementOperation());
        assertEquals("select", readback.getStatementOperation());
        fixture.failReadback = false;
        fixture.clear();
        root.auditAs("retry completed mutation").save(fixture.context);
        assertEquals(1, fixture.audit.size());
        assertLineage(fixture.audit, "CustomerOrder", 100, List.of("retry completed mutation"));
        assertEquals(Long.valueOf(1), root.getVersion());
    }

    @Test public void safeSqlAndAuditRetainTypedIdsWhileMaskingReasonSecrets() throws Exception {
        var fixture = new Fixture();
        fixture.create("CustomerOrder", 100, "PRIVATE-TRACE-CANARY")
                .auditAs("change PRIVATE-TRACE-CANARY").save(fixture.context);
        assertFalse(fixture.sql.isEmpty());
        assertEquals(1, fixture.audit.size());
        assertEquals(Long.valueOf(100), fixture.audit.get(0).traceChain().get(0).getEntityId());
        for (var entry : fixture.sql) {
            assertEquals(Long.valueOf(100), entry.getMutationLineage().get(0).getEntityId());
            String exported = entry.getDebugQuery() + " " + entry.getAuditReason() + " " + entry.getComment()
                    + " " + entry.getTraceChain() + " " + entry.getMutationLineage();
            assertFalse(exported, exported.contains("PRIVATE-TRACE-CANARY"));
        }
        assertFalse(fixture.audit.get(0).traceChain().toString().contains("PRIVATE-TRACE-CANARY"));
    }

    @Test public void newlyAllocatedChildIdReachesRealSqlAndCommittedAudit() throws Exception {
        var fixture = new Fixture();
        var root = fixture.create("CustomerOrder", 100, "allocated graph");
        var payment = new GraphEntity("Payment");
        payment.updateProperty("name", "allocated payment");
        payment.setComment("authorize newly assigned object");
        root.__internalSet("children", List.of(payment));
        root.auditAs("create allocated child").save(fixture.context);
        assertEquals(Long.valueOf(1000), payment.getId());
        var event = fixture.audit.stream().filter(value -> value.entityType().equals("Payment")).findFirst().orElseThrow();
        assertEquals(Long.valueOf(1000), event.traceChain().get(1).getEntityId());
        assertEquals(List.of("create allocated child", "authorize newly assigned object"), reasons(event.traceChain()));
        assertTrue(fixture.sql.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION
                && entry.getMutationLineage().equals(event.traceChain())));
    }

    @Test public void sameTypePreparedInsertBatchKeepsEachItemLineage() throws Exception {
        var fixture = new Fixture();
        var root = fixture.create("CustomerOrder", 100, "batch owner");
        var first = fixture.create("OrderItem", 201, "alpha value");
        first.setComment("add first entry");
        var second = fixture.create("OrderItem", 202, "beta value");
        second.setComment("add second entry");
        root.__internalSet("children", List.of(first, second));

        root.auditAs("compose item batch").save(fixture.context);

        assertEquals("observe a real two-row JDBC prepared batch, not two singleton calls",
                List.of(2), fixture.itemInsertBatchSizes);
        assertEquals(3, fixture.audit.size());
        for (long id : List.of(201L, 202L)) {
            var event = fixture.audit.stream().filter(value -> value.entityType().equals("OrderItem")
                    && value.entityId().equals(id)).findFirst().orElseThrow();
            var write = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.MUTATION
                    && value.getTraceChain().get(1).getName().equals("OrderItem")
                    && Objects.equals(value.getMutationLineage().get(value.getMutationLineage().size() - 1).getEntityId(), id))
                    .findFirst().orElseThrow();
            assertEquals(event.traceChain(), write.getMutationLineage());
            assertEquals(List.of("compose item batch", id == 201L ? "add first entry" : "add second entry"),
                    reasons(write.getMutationLineage()));
            assertEquals("success", write.getExecutionOutcome());
            assertEquals(Long.valueOf(1), write.getAffectedRows());
            assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.QUERY
                    && value.getMutationLineage().equals(event.traceChain())));
        }
        assertEquals("alpha value", first.getProperty("name"));
        assertEquals("beta value", second.getProperty("name"));
    }

    @Test public void realSameTypeBatchFailureKeepsBothAttemptedLineagesAndRollsBack() throws Exception {
        var fixture = new Fixture();
        fixture.driver.execute("CREATE UNIQUE INDEX item_name_unique ON order_item_data(name)");
        var root = fixture.create("CustomerOrder", 100, "failure owner");
        var first = fixture.create("OrderItem", 201, "duplicate batch name");
        first.setComment("attempt entry alpha");
        var second = fixture.create("OrderItem", 202, "duplicate batch name");
        second.setComment("attempt entry beta");
        root.__internalSet("children", List.of(first, second));

        assertThrows(RuntimeException.class, () -> root.auditAs("attempt prepared graph").save(fixture.context));

        assertEquals(List.of(2), fixture.itemInsertBatchSizes);
        assertTrue(fixture.audit.isEmpty());
        var members = fixture.sql.stream().filter(value -> "failure".equals(value.getBatchOutcome())
                && value.getTraceChain().get(1).getName().equals("OrderItem")).toList();
        assertEquals(2, members.size());
        for (int index = 0; index < members.size(); index++) {
            var member = members.get(index);
            assertEquals(List.of("attempt prepared graph", index == 0 ? "attempt entry alpha" : "attempt entry beta"),
                    reasons(member.getMutationLineage()));
            assertEquals(Long.valueOf(201L + index), member.getMutationLineage().get(1).getEntityId());
            assertEquals("SQLite JDBC reports no per-member counts here", "unknown", member.getExecutionOutcome());
            assertNull(member.getAffectedRows());
        }
        assertTrue(fixture.driver.queryForList("SELECT id FROM order_item_data", new Object[]{}).isEmpty());
        assertTrue(fixture.driver.queryForList("SELECT id FROM customer_order_data", new Object[]{}).isEmpty());
        assertTrue(first.newItem());
        assertTrue(second.newItem());
        assertNull(first.getVersion());
        assertNull(second.getVersion());
    }

    @Test public void siblingSecretIsScrubbedFromEveryPreparedMemberAndReadback() throws Exception {
        var fixture = new Fixture();
        var root = fixture.create("CustomerOrder", 100, "privacy owner");
        var first = fixture.create("OrderItem", 201, "PUBLIC-LOOKING-ALPHA");
        first.setComment("first member refers to PRIVATE-FUTURE-BETA");
        var second = fixture.create("OrderItem", 202, "PRIVATE-FUTURE-BETA");
        second.setComment("second member request");
        root.__internalSet("children", List.of(first, second));

        root.auditAs("compose privacy batch").save(fixture.context);

        assertEquals(List.of(2), fixture.itemInsertBatchSizes);
        var statements = fixture.sql.stream().filter(value -> !value.getMutationLineage().isEmpty()
                && value.getMutationLineage().get(value.getMutationLineage().size() - 1).getName().equals("OrderItem")).toList();
        assertEquals("two writes and two readbacks", 4, statements.size());
        for (var statement : statements) {
            assertFalse(statement.getDebugQuery() + " " + statement.getAuditReason() + " " + statement.getComment()
                    + " " + statement.getMutationLineage(), statement.getMutationLineage().toString().contains("PRIVATE-FUTURE-BETA"));
        }
        assertEquals("private values still reach the database unchanged", "PRIVATE-FUTURE-BETA", second.getProperty("name"));
        assertEquals("caller-owned reason is not modified", "first member refers to PRIVATE-FUTURE-BETA", first.getComment());
    }

    @Test public void sameTypePreparedUpdatesKeepMemberVersionsAndLineage() throws Exception {
        var fixture = new Fixture();
        var root = seedTwoItems(fixture);
        var children = (List<GraphEntity>) root.__internalGet("children");
        children.get(0).updateProperty("name", "changed alpha");
        children.get(0).setComment("revise alpha entry");
        children.get(1).updateProperty("name", "changed beta");
        children.get(1).setComment("revise beta entry");
        root.auditAs("revise existing graph").save(fixture.context);

        assertEquals(List.of(2), fixture.itemUpdateBatchSizes);
        assertMemberWrites(fixture, "update", "revise existing graph", "revise alpha entry", "revise beta entry");
        assertEquals(Long.valueOf(2), children.get(0).getVersion());
        assertEquals(Long.valueOf(2), children.get(1).getVersion());
        assertEquals("changed alpha", children.get(0).getProperty("name"));
        assertEquals("changed beta", children.get(1).getProperty("name"));
    }

    @Test public void sameTypePreparedDeleteAndPureRecoveryKeepSeparateMemberLineage() throws Exception {
        var fixture = new Fixture();
        var root = seedTwoItems(fixture);
        var children = (List<GraphEntity>) root.__internalGet("children");
        for (var child : children) child.markForDeletion();
        children.get(0).setComment("remove alpha entry");
        children.get(1).setComment("remove beta entry");
        root.auditAs("remove graph entries").save(fixture.context);

        assertEquals(List.of(2), fixture.itemDeleteBatchSizes);
        assertMemberWrites(fixture, "delete", "remove graph entries", "remove alpha entry", "remove beta entry");
        for (var child : children) assertEquals(Long.valueOf(-2), child.getVersion());
        fixture.clear();
        for (var child : children) child.markAsRecover(); // No dirty scalar field: the operation itself must enter the ledger.
        children.get(0).setComment("restore alpha entry");
        children.get(1).setComment("restore beta entry");
        root.auditAs("restore graph entries").save(fixture.context);

        assertEquals(List.of(2), fixture.itemRecoverBatchSizes);
        assertMemberWrites(fixture, "recover", "restore graph entries", "restore alpha entry", "restore beta entry");
        for (var child : children) {
            assertEquals(Long.valueOf(3), child.getVersion());
            assertEquals(EntityStatus.PERSISTED, child.get$status());
        }
    }

    @Test public void singlePureRecoveryIsNotLostWhenNoScalarFieldsChange() throws Exception {
        var fixture = new Fixture();
        var item = fixture.create("OrderItem", 201, "recover fixture");
        item.auditAs("seed recovery fixture").save(fixture.context);
        fixture.clear();
        item.markForDeletion();
        item.auditAs("remove recovery fixture").save(fixture.context);
        fixture.clear();
        item.markToRecover();
        item.auditAs("restore recovery fixture").save(fixture.context);

        assertEquals(1, fixture.commands.size());
        assertEquals(1, fixture.audit.size());
        assertEquals(MutationAuditKind.RECOVERED, fixture.audit.get(0).kind());
        assertEquals(Long.valueOf(3), item.getVersion());
        assertEquals(EntityStatus.PERSISTED, item.get$status());
        assertTrue(fixture.sql.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION
                && entry.getStatementOperation().equals("recover")
                && reasons(entry.getMutationLineage()).equals(List.of("restore recovery fixture"))));
    }

    @Test public void staleSecondPreparedUpdateRollsBackFirstButRetainsRealRowCounts() throws Exception {
        var fixture = new Fixture();
        var root = seedTwoItems(fixture);
        var children = (List<GraphEntity>) root.__internalGet("children");
        // A separate writer advances only the second row's optimistic version.
        fixture.driver.update("UPDATE order_item_data SET version = 2 WHERE id = 202", new Object[]{});
        children.get(0).updateProperty("name", "attempted alpha");
        children.get(0).setComment("attempt alpha revision");
        children.get(1).updateProperty("name", "attempted beta");
        children.get(1).setComment("attempt beta revision");
        assertThrows(RuntimeException.class, () -> root.auditAs("attempt stale graph").save(fixture.context));

        assertEquals(List.of(2), fixture.itemUpdateBatchSizes);
        assertTrue(fixture.audit.isEmpty());
        var writes = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION).toList();
        assertEquals(2, writes.size());
        assertEquals(Long.valueOf(1), writes.get(0).getAffectedRows());
        assertEquals(Long.valueOf(0), writes.get(1).getAffectedRows());
        assertEquals(List.of("attempt stale graph", "attempt alpha revision"), reasons(writes.get(0).getMutationLineage()));
        assertEquals(List.of("attempt stale graph", "attempt beta revision"), reasons(writes.get(1).getMutationLineage()));
        var rows = fixture.driver.queryForList("SELECT id,name,version FROM order_item_data ORDER BY id", new Object[]{});
        assertEquals("original alpha", rows.get(0).get("name"));
        assertEquals("original beta", rows.get(1).get("name"));
        for (var child : children) {
            assertEquals(Long.valueOf(1), child.getVersion());
            assertEquals(EntityStatus.UPDATED, child.get$status());
        }
    }

    @Test public void distinctUpdateLayoutsAssociateTracesWithPhysicalRowsNotBatchIndex() throws Exception {
        var fixture = new Fixture();
        var root = seedTwoItems(fixture);
        var children = (List<GraphEntity>) root.__internalGet("children");
        children.get(0).updateProperty("name", "renamed alpha");
        children.get(0).setComment("rename alpha entry");
        children.get(1).updateProperty("memo", "beta note");
        children.get(1).setComment("annotate beta entry");
        root.auditAs("revise different projections").save(fixture.context);

        assertEquals("different SET layouts must not be merged into one prepared statement", List.of(1, 1), fixture.itemUpdateBatchSizes);
        assertMemberWrites(fixture, "update", "revise different projections", "rename alpha entry", "annotate beta entry");
        assertEquals("beta note", children.get(1).getProperty("memo"));
        assertEquals("original beta", children.get(1).getProperty("name"));
    }

    @Test public void staleDeleteAndRecoverMembersRollBackAndKeepAttemptedLineages() throws Exception {
        for (boolean recover : List.of(false, true)) {
            var fixture = new Fixture();
            var root = seedTwoItems(fixture);
            var children = (List<GraphEntity>) root.__internalGet("children");
            if (recover) {
                for (var child : children) child.markForDeletion();
                root.auditAs("prepare removed graph").save(fixture.context);
                fixture.clear();
            }
            long original = recover ? -2L : 1L;
            fixture.driver.update("UPDATE order_item_data SET version = ? WHERE id = ?",
                    new Object[]{recover ? -3L : 2L, 202L});
            for (var child : children) {
                if (recover) child.markAsRecover(); else child.markForDeletion();
            }
            children.get(0).setComment("attempt first member");
            children.get(1).setComment("attempt second member");
            assertThrows(RuntimeException.class, () -> root.auditAs("attempt stale lifecycle").save(fixture.context));

            assertEquals(List.of(2), recover ? fixture.itemRecoverBatchSizes : fixture.itemDeleteBatchSizes);
            assertTrue(fixture.audit.isEmpty());
            var writes = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION).toList();
            assertEquals(2, writes.size());
            assertEquals(Long.valueOf(1), writes.get(0).getAffectedRows());
            assertEquals(Long.valueOf(0), writes.get(1).getAffectedRows());
            assertEquals(recover ? "recover" : "delete", writes.get(0).getStatementOperation());
            assertEquals(List.of("attempt stale lifecycle", "attempt first member"), reasons(writes.get(0).getMutationLineage()));
            assertEquals(List.of("attempt stale lifecycle", "attempt second member"), reasons(writes.get(1).getMutationLineage()));
            for (var child : children) {
                assertEquals(Long.valueOf(original), child.getVersion());
                assertEquals(recover ? EntityStatus.UPDATED_RECOVER : EntityStatus.UPDATED_DELETED, child.get$status());
            }
            var row = fixture.driver.queryForList("SELECT version FROM order_item_data WHERE id = ?", new Object[]{201L}).get(0);
            assertEquals(original, ((Number) row.get("version")).longValue());
        }
    }

    @Test public void unknownPreparedUpdateCountsCannotProveOptimisticSuccess() throws Exception {
        var fixture = new Fixture();
        var root = seedTwoItems(fixture);
        var children = (List<GraphEntity>) root.__internalGet("children");
        for (var child : children) child.updateProperty("name", "attempted revision " + child.getId());
        fixture.unknownUpdateCounts = true;
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> root.auditAs("attempt unknown counts").save(fixture.context));
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        assertEquals("Prepared mutation requires an exact per-item affected-row count", cause.getMessage());

        assertEquals(List.of(2), fixture.itemUpdateBatchSizes);
        assertTrue(fixture.audit.isEmpty());
        var writes = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION).toList();
        assertEquals(2, writes.size());
        for (var statement : writes) assertNull("SUCCESS_NO_INFO must not become affectedRows=1", statement.getAffectedRows());
        var rows = fixture.driver.queryForList("SELECT name FROM order_item_data ORDER BY id", new Object[]{});
        assertEquals("original alpha", rows.get(0).get("name"));
        assertEquals("original beta", rows.get(1).get("name"));
    }

    @Test public void detachedLedgerRecoveryKeepsCompleteSpecificLineage() throws Exception {
        var fixture = new Fixture();
        var item = fixture.create("OrderItem", 201, "detached entry");
        item.auditAs("seed detached entry").save(fixture.context);
        fixture.clear();
        item.markForDeletion();
        item.auditAs("remove detached entry").save(fixture.context);
        var root = fixture.create("CustomerOrder", 100, "independent owner");
        root.auditAs("seed independent owner").save(fixture.context);
        fixture.clear();
        var key = new EntityKey("OrderItem", 201L);
        var complete = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 100L, "delegated restore root"),
                new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", 201L, "restore detached entry"));
        root.getEntityMutationLedger().markAsRecover(key);
        root.getEntityMutationLedger().setOriginalVersion(key, -2L);
        root.getEntityMutationLedger().setTraceChain(key, complete);
        root.auditAs("fallback must be replaced").save(fixture.context);

        assertEquals(1, fixture.audit.size());
        assertEquals(MutationAuditKind.RECOVERED, fixture.audit.get(0).kind());
        assertEquals(complete, fixture.audit.get(0).traceChain());
        assertEquals(complete, fixture.commands.get(0).getTraceChain());
        assertTrue(fixture.sql.stream().anyMatch(entry -> entry.getStatementOperation().equals("recover")
                && entry.getMutationLineage().equals(complete)));
        var row = fixture.driver.queryForList("SELECT version FROM order_item_data WHERE id = 201", new Object[]{}).get(0);
        assertEquals(3, ((Number) row.get("version")).longValue());
    }

    @Test public void oneGraphCanUpdateAndRecoverSameTypeWithoutMixingVersionTransitions() throws Exception {
        var fixture = new Fixture();
        var root = seedTwoItems(fixture);
        var children = (List<GraphEntity>) root.__internalGet("children");
        children.get(1).markForDeletion();
        root.auditAs("prepare one removed entry").save(fixture.context);
        fixture.clear();
        children.get(0).updateProperty("name", "revised active entry");
        children.get(0).setComment("revise active entry");
        children.get(1).markAsRecover();
        children.get(1).setComment("restore removed entry");
        root.auditAs("compose mixed lifecycle").save(fixture.context);

        assertEquals(Long.valueOf(2), children.get(0).getVersion());
        assertEquals(Long.valueOf(3), children.get(1).getVersion());
        assertEquals(2, fixture.audit.size());
        assertLineage(fixture.audit, "OrderItem", 201, List.of("compose mixed lifecycle", "revise active entry"));
        assertLineage(fixture.audit, "OrderItem", 202, List.of("compose mixed lifecycle", "restore removed entry"));
        assertEquals(List.of("update", "recover"), fixture.sql.stream()
                .filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION)
                .map(ExecutionMetadata::getStatementOperation).toList());
    }

    private static GraphEntity seedTwoItems(Fixture fixture) {
        var root = fixture.create("CustomerOrder", 100, "batch owner");
        root.__internalSet("children", List.of(fixture.create("OrderItem", 201, "original alpha"),
                fixture.create("OrderItem", 202, "original beta")));
        root.auditAs("seed graph fixtures").save(fixture.context);
        fixture.clear();
        return root;
    }

    private static void assertMemberWrites(Fixture fixture, String operation, String rootReason,
            String firstReason, String secondReason) {
        assertEquals(2, fixture.commands.size());
        assertEquals(2, fixture.audit.size());
        for (int index = 0; index < 2; index++) {
            var command = fixture.commands.get(index);
            var expected = List.of(rootReason, index == 0 ? firstReason : secondReason);
            assertEquals(expected, reasons(command.getTraceChain()));
            assertEquals(Long.valueOf(201L + index), command.getTraceChain().get(1).getEntityId());
            assertLineage(fixture.audit, "OrderItem", 201L + index, expected);
            var statement = fixture.sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION
                    && entry.getMutationLineage().equals(command.getTraceChain())).findFirst().orElseThrow();
            assertEquals(operation, statement.getStatementOperation());
            assertEquals("success", statement.getExecutionOutcome());
            assertEquals(Long.valueOf(1), statement.getAffectedRows());
            assertTrue(fixture.sql.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.QUERY
                    && entry.getMutationLineage().equals(command.getTraceChain())));
        }
    }

    static List<String> reasons(List<TraceNode> nodes) { return nodes.stream().map(TraceNode::getComment).toList(); }
    static void assertLineage(List<SafeAuditEvent> events, String type, long id, List<String> expected) {
        var event = events.stream().filter(value -> value.entityType().equals(type) && Objects.equals(value.entityId(), id))
                .findFirst().orElseThrow();
        assertEquals(expected, reasons(event.traceChain()));
    }
}
