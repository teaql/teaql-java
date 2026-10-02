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
            return field.equals("name") || field.equals("children") ? values.get(field) : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name") || field.equals("children")) values.put(field, value);
            else super.__internalSet(field, value);
        }
    }

    static final class Fixture {
        final List<ExecutionMetadata> sql = new CopyOnWriteArrayList<>();
        final List<SafeAuditEvent> audit = new CopyOnWriteArrayList<>();
        final List<EntityPersistenceMutation> commands = new CopyOnWriteArrayList<>();
        final JdbcSqlExecutor driver;
        final DefaultUserContext context;
        volatile boolean failReadback;

        Fixture() throws Exception {
            var ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-graph-trace-", ".db"));
            driver = new JdbcSqlExecutor(ds) {
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
                for (String field : List.of("id", "version", "name")) {
                    var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                            field.equals("name") ? String.class : Long.class);
                    property.setColumnType(field.equals("name") ? "VARCHAR(255)" : "BIGINT");
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

        void clear() { sql.clear(); audit.clear(); commands.clear(); }

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

    static List<String> reasons(List<TraceNode> nodes) { return nodes.stream().map(TraceNode::getComment).toList(); }
    static void assertLineage(List<SafeAuditEvent> events, String type, long id, List<String> expected) {
        var event = events.stream().filter(value -> value.entityType().equals(type) && Objects.equals(value.entityId(), id))
                .findFirst().orElseThrow();
        assertEquals(expected, reasons(event.traceChain()));
    }
}
