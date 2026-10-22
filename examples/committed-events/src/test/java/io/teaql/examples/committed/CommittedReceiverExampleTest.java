package io.teaql.examples.committed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.*;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.core.sqlite.SqliteCommittedReceiver.Delivery;
import io.teaql.core.sqlite.SqliteCommittedReceiver.SnapshotDelivery;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

import static org.junit.Assert.*;

/** Actual audited source saves, authenticated local delivery and durable readback. */
public class CommittedReceiverExampleTest {
    private static final String SOURCE = "cmr13-example-source", TARGET = "analytics";
    private static final String MODEL = "sha256:" + "0".repeat(64);
    private static final String CANARY = "private-business-fact";
    // Public test material only. Deployments provision their own independent secret.
    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final long ID = 9_007_199_254_740_993L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SqliteCommittedSnapshot.Limits LIMITS = new SqliteCommittedSnapshot.Limits(8, 65536);

    public static final class CustomerOrder extends BaseEntity {
        private final Map<String, Object> fields = new HashMap<>();
        @Override public String typeName() { return "CustomerOrder"; }
        @Override public Object __internalGet(String field) {
            return field.equals("name") || field.equals("memo") ? fields.get(field) : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name") || field.equals("memo")) fields.put(field, value);
            else super.__internalSet(field, value);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final JdbcSqlExecutor sql;
        final TeaQLRuntime runtime;
        final DefaultUserContext context;
        final SqliteCommittedMutationRecorder recorder;
        Fixture(Path file, boolean capture) {
            SQLiteDataSource data = new SQLiteDataSource(); data.setUrl("jdbc:sqlite:" + file);
            sql = new JdbcSqlExecutor(data);
            SQLEntityDescriptor entity = new SQLEntityDescriptor();
            entity.setType("CustomerOrder"); entity.setTargetType(CustomerOrder.class);
            entity.setEntitySupplier(CustomerOrder::new); entity.setDataService("sqlite");
            for (String field : List.of("id", "version", "name", "memo")) {
                boolean text = field.equals("name") || field.equals("memo");
                GenericSQLProperty property = (GenericSQLProperty) entity.addSimpleProperty(field, text ? String.class : Long.class);
                property.setColumnType(text ? "VARCHAR(255)" : "BIGINT");
            }
            SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory(); metadata.register(entity);
            recorder = capture ? new SqliteCommittedMutationRecorder(sql, SOURCE, MODEL) : null;
            var builder = TeaQLRuntime.builder().metadata(metadata)
                    .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", sql, data))
                    .idGenerationService((caller, value) -> ID).executionLogging(false);
            if (capture) builder.committedMutationRecorder(recorder);
            runtime = builder.build(); context = new DefaultUserContext(runtime); context.ensureSchema();
        }
        SqliteCommittedReceiver receiver() {
            var receiver = new SqliteCommittedReceiver(sql, SOURCE, TARGET, MODEL, Map.of("current", KEY));
            receiver.ensureSchema(context); return receiver;
        }
        @Override public void close() { runtime.close(); }
    }
    private static long scalar(JdbcSqlExecutor sql, String query) {
        return ((Number) sql.queryForList(query, new Object[0]).get(0).values().iterator().next()).longValue();
    }
    private static Map<String,Object> domain(JdbcSqlExecutor sql) {
        var rows = sql.queryForList("SELECT id,version,name,memo FROM customer_order_data", new Object[0]);
        assertEquals(1, rows.size()); return rows.get(0);
    }
    private static void position(Fixture target, long checkpoint, long inbox, long audits, String name) {
        assertEquals(checkpoint, scalar(target.sql, "SELECT source_sequence FROM teaql_event_checkpoint"));
        assertEquals(inbox, scalar(target.sql, "SELECT count(*) FROM teaql_event_inbox"));
        assertEquals(audits, scalar(target.sql, "SELECT count(*) FROM teaql_event_rejection"));
        assertEquals(name, domain(target.sql).get("name"));
    }
    private static void rejected(String code, Runnable call) {
        var failure = assertThrows(SqliteCommittedReceiver.ReceiverException.class, call::run);
        assertEquals(code, failure.code()); assertFalse(failure.toString().contains(CANARY));
    }
    private static String digest(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static void businessCall(Path directory, CustomerOrder order, DefaultUserContext context, String comment) throws Exception {
        Files.writeString(directory.resolve("business-calls.txt"), comment + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        order.auditAs(comment).save(context);
    }
    @Test public void auditedSaveSnapshotAndTailAuthenticateBeforeApplication() throws Exception {
        String retained = System.getenv("TEAQL_CMR13_EVIDENCE_DIR");
        Path directory;
        if (retained == null) directory = Files.createTempDirectory("teaql-java-cmr13-");
        else { directory = Path.of(retained); assertTrue(directory.isAbsolute()); assertFalse(Files.exists(directory)); Files.createDirectory(directory); }
        boolean passed = false;
        try (Fixture source = new Fixture(directory.resolve("source.sqlite"), true);
             Fixture target = new Fixture(directory.resolve("target.sqlite"), false)) {
            var receiver = target.receiver();
            CustomerOrder order = new CustomerOrder(); order.__internalInitializeNewEntityId(ID);
            order.updateProperty("name", "draft"); order.updateProperty("memo", CANARY);
            businessCall(directory, order, source.context, "create authenticated replication example");
            var first = source.recorder.eventStore().outboxAfter(SOURCE, 0, 8).get(0);
            assertEquals(1, first.sourceSequence());
            var snapshots = new SqliteCommittedSnapshotPublisher(source.sql, SOURCE, TARGET, MODEL);
            var prepared = snapshots.prepare(source.context, "baseline", "capture accepted order", "bootstrap analytics", LIMITS);
            assertEquals(1, prepared.snapshot().sourceSequence()); assertFalse(prepared.acknowledged());
            SnapshotDelivery signed = SqliteCommittedReceiver.signSnapshot("current", TARGET, KEY, prepared.snapshot());
            byte[] invalid = signed.signature(); invalid[0] ^= 1;
            rejected("INVALID_SIGNATURE", () -> snapshots.publish(source.context, "baseline", LIMITS,
                    record -> receiver.receiveSnapshot(target.context, new SnapshotDelivery(record, "current", TARGET, invalid))));
            assertEquals(0, scalar(source.sql, "SELECT count(*) FROM teaql_event_delivery_cursor"));
            assertEquals(0, scalar(source.sql, "SELECT acknowledged FROM teaql_event_snapshot_publication"));
            assertEquals(0, scalar(target.sql, "SELECT count(*) FROM customer_order_data"));
            assertEquals(1, scalar(target.sql, "SELECT count(*) FROM teaql_event_rejection"));

            // Delivery commits, but the source does not get its receipt. This is a lost ACK,
            // not a process-crash claim (the complete gate has real SIGKILL cases separately).
            assertThrows(IllegalStateException.class, () -> snapshots.publish(source.context, "baseline", LIMITS, record -> {
                receiver.receiveSnapshot(target.context, SqliteCommittedReceiver.signSnapshot("current", TARGET, KEY, record));
                throw new IllegalStateException("example lost snapshot receipt");
            }));
            assertEquals(0, scalar(source.sql, "SELECT count(*) FROM teaql_event_delivery_cursor"));
            position(target, 1, 0, 1, "draft");
            var recovered = snapshots.publish(source.context, "baseline", LIMITS, record -> {
                var receipt = receiver.receiveSnapshot(target.context, SqliteCommittedReceiver.signSnapshot("current", TARGET, KEY, record));
                assertTrue(receipt.duplicate()); return receipt;
            });
            assertTrue(recovered.acknowledged()); assertEquals(prepared.snapshot(), recovered.snapshot());
            assertEquals(1, scalar(source.sql, "SELECT acked_sequence FROM teaql_event_delivery_cursor"));

            order.updateProperty("name", "submitted");
            businessCall(directory, order, source.context, "submit authenticated replication example");
            OutboxRecord tail = source.recorder.eventStore().outboxAfter(SOURCE, 1, 8).get(0);
            assertEquals(2, tail.sourceSequence());
            Delivery original = SqliteCommittedReceiver.sign("current", TARGET, KEY, tail);
            String forged = tail.payloadJson().replace("submitted", "forged"); assertNotEquals(tail.payloadJson(), forged);
            var altered = new OutboxRecord(SOURCE, 2, tail.changeSetId(), MODEL, digest(forged), forged);
            var publisher = new SqliteCommittedOutboxPublisher(source.sql, SOURCE, TARGET, MODEL);
            rejected("INVALID_SIGNATURE", () -> publisher.publishBatch(8, record -> receiver.receive(target.context,
                    new Delivery(altered, "current", TARGET, original.signature()))));
            position(target, 1, 0, 2, "draft"); assertEquals(1, publisher.status().ackedSequence());
            var stranger = new OutboxRecord("untrusted-source", 2, tail.changeSetId(), MODEL, tail.payloadSha256(), tail.payloadJson());
            rejected("UNAUTHORIZED_SOURCE", () -> receiver.receive(target.context,
                    SqliteCommittedReceiver.sign("current", TARGET, KEY, stranger)));
            position(target, 1, 0, 3, "draft");
            var delivered = publisher.publishBatch(8, record -> receiver.receive(target.context,
                    SqliteCommittedReceiver.sign("current", TARGET, KEY, record)));
            assertEquals(1, delivered.acknowledged()); assertEquals(2, delivered.status().ackedSequence());
            assertEquals(0, delivered.status().backlog()); assertTrue(receiver.receive(target.context, original).duplicate());
            rejected("SEQUENCE_GAP", () -> receiver.receive(target.context, SqliteCommittedReceiver.sign("current", TARGET, KEY, first)));
            assertTrue(receiver.receiveSnapshot(target.context, signed).duplicate());
            position(target, 2, 1, 4, "submitted");
            var newer = new SqliteCommittedSnapshot(source.sql, SOURCE, MODEL).exportSnapshot(source.context,
                    "replacement", "capture a newer baseline", "prove active stream cannot reset", LIMITS);
            rejected("SNAPSHOT_CONFLICT", () -> receiver.receiveSnapshot(target.context,
                    SqliteCommittedReceiver.signSnapshot("current", TARGET, KEY, newer)));
            position(target, 2, 1, 5, "submitted");

            // Fresh contexts/connections inspect durable state; no business method is replayed.
            try (Fixture reopened = new Fixture(directory.resolve("target.sqlite"), false)) {
                assertTrue(reopened.receiver().receive(reopened.context, original).duplicate());
                assertTrue(reopened.receiver().receiveSnapshot(reopened.context, signed).duplicate());
                position(reopened, 2, 1, 5, "submitted"); assertEquals(domain(source.sql), domain(reopened.sql));
                var audits = reopened.sql.queryForList("SELECT * FROM teaql_event_rejection ORDER BY audit_id", new Object[0]);
                assertEquals(List.of("SNAPSHOT_INVALID_SIGNATURE", "INVALID_SIGNATURE", "UNAUTHORIZED_SOURCE", "SEQUENCE_GAP", "SNAPSHOT_CONFLICT"),
                        audits.stream().map(row -> row.get("reason_code")).toList());
                String auditText = JSON.writeValueAsString(audits); assertFalse(auditText.contains(CANARY));
                assertFalse(auditText.contains("submitted")); assertFalse(auditText.contains(new String(KEY, StandardCharsets.US_ASCII)));
                for (var row : audits) for (String field : List.of("source_fingerprint", "change_fingerprint", "target_fingerprint", "payload_fingerprint"))
                    assertTrue(((String)row.get(field)).matches("[a-f0-9]{64}"));
                long calls = Files.readAllLines(directory.resolve("business-calls.txt")).size(); assertEquals(2, calls);
                var result = new LinkedHashMap<String,Object>();
                result.put("schema", "teaql.cmr13-example/v1"); result.put("language", "java");
                result.put("source", domain(source.sql)); result.put("target", domain(reopened.sql));
                result.put("businessCalls", calls); result.put("sourceCursor", publisher.status().ackedSequence());
                result.put("targetCheckpoint", scalar(reopened.sql, "SELECT source_sequence FROM teaql_event_checkpoint"));
                result.put("snapshotSequence", prepared.snapshot().sourceSequence()); result.put("snapshotHash", prepared.snapshot().payloadSha256());
                result.put("rejections", audits); result.put("outbox", source.recorder.eventStore().outboxAfter(SOURCE, 0, 8).size());
                result.put("inbox", scalar(reopened.sql, "SELECT count(*) FROM teaql_event_inbox"));
                result.put("receiverOrigin", SqliteCommittedReceiver.class.getProtectionDomain().getCodeSource().getLocation().toString());
                Files.writeString(directory.resolve("result.json"), JSON.writeValueAsString(result), StandardOpenOption.CREATE_NEW);
            }
            passed = true;
            System.out.println("CMR13_SECURITY_EXAMPLE_PASS language=java snapshot=1 tail=2 audited_saves=2 rejected=5 lost_ack=1 duplicate_no_rewind=1 reopen=1");
        } finally {
            if (retained == null && passed) try (var files = Files.walk(directory)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            } else if (!passed) System.err.println("CMR13 failed compact diagnostic: " + directory);
        }
    }
}
