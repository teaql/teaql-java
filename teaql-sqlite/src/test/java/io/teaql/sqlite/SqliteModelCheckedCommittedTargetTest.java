package io.teaql.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore;
import io.teaql.core.sqlite.SqliteCommittedEventStore.CommittedEventException;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.core.sqlite.SqliteModelCheckedCommittedTarget;
import io.teaql.core.sqlite.SqliteCommittedOutboxPublisher;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.junit.Assume;
import org.sqlite.SQLiteDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.Assert.*;

public class SqliteModelCheckedCommittedTargetTest {
    private static final String SOURCE = "sqlite-order-source";
    private static final String HASH = "sha256:" + "0".repeat(64);
    private static final long ORDER_ID = 9_007_199_254_740_993L;
    private static final long ITEM_ID = ORDER_ID + 1;

    private record Target(JdbcSqlExecutor sql, SqliteModelCheckedCommittedTarget target,
            DefaultUserContext context) { }

    private static Target target(Path path) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + path);
        JdbcSqlExecutor sql = new JdbcSqlExecutor(source);
        EntityDescriptor order = new EntityDescriptor();
        order.setType("order");
        order.addSimpleProperty("id", Long.class);
        order.addSimpleProperty("version", Long.class);
        order.addSimpleProperty("order_number", String.class);
        order.addSimpleProperty("status", String.class);
        order.addSimpleProperty("planned_date", java.time.LocalDate.class);
        order.addSimpleProperty("amount", java.math.BigDecimal.class);
        order.addSimpleProperty("active", Boolean.class).with("required", "true");
        order.addSimpleProperty("ratio", Double.class).with("required", "true");
        order.addSimpleProperty("optional_note", String.class);
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        metadata.register(order);
        EntityDescriptor item = new EntityDescriptor();
        item.setType("order_item");
        item.addSimpleProperty("id", Long.class);
        item.addSimpleProperty("version", Long.class);
        item.addObjectProperty(metadata, "order", "order", "items", BaseEntity.class)
                .with("columnName", "order_id");
        item.addSimpleProperty("quantity", Long.class);
        metadata.register(item);
        DefaultUserContext context = new DefaultUserContext(
                TeaQLRuntime.builder().metadata(metadata).build());
        sql.execute("CREATE TABLE order_data (id INTEGER PRIMARY KEY, version INTEGER NOT NULL, "
                + "order_number TEXT NOT NULL, status TEXT NOT NULL, planned_date DATE, amount NUMERIC, "
                + "active INTEGER NOT NULL, ratio REAL NOT NULL, optional_note TEXT)");
        sql.execute("CREATE TABLE order_item_data (id INTEGER PRIMARY KEY, version INTEGER NOT NULL, "
                + "order_id INTEGER NOT NULL, quantity INTEGER NOT NULL)");
        SqliteModelCheckedCommittedTarget target = new SqliteModelCheckedCommittedTarget(sql, SOURCE, HASH);
        target.ensureSchema();
        return new Target(sql, target, context);
    }

    private static String payload(long sequence, String change, String mutations) {
        return """
                {"protocol_version":1,"change_set_id":"%s","source_runtime_id":"%s",
                 "source_sequence":"%d","source_model_hash":"%s","source_transaction_id":"tx-%d",
                 "committed_at":"2026-10-16T00:00:00Z",
                 "audit":{"actor_ref":"fixture:operator","comment":"Apply order graph",
                          "purpose":"Verify target application","trace_id":"fixture-trace"},
                 "mutations":%s}
                """.formatted(change, SOURCE, sequence, HASH, sequence, mutations);
    }

    private static String created() {
        return """
                [{"entity_type":"order","entity_id":"9007199254740993","operation":"create",
                  "base_version":null,"committed_version":"1",
                  "fields":{"order_number":"ORD-0001","status":"DRAFT",
                            "planned_date":"2026-10-16","amount":"0",
                            "active":true,"ratio":0.5,"optional_note":null},"trace_chain":[]},
                 {"entity_type":"order_item","entity_id":"9007199254740994","operation":"create",
                  "base_version":null,"committed_version":"1",
                  "fields":{"order":{"entity_type":"order","entity_id":"9007199254740993"},
                            "quantity":"2"},"trace_chain":[]}]
                """;
    }

    private static String submitted() {
        return """
                [{"entity_type":"order","entity_id":"9007199254740993","operation":"update",
                  "base_version":"1","committed_version":"2","fields":{"status":"SUBMITTED"},"trace_chain":[]},
                 {"entity_type":"order_item","entity_id":"9007199254740994","operation":"delete",
                  "base_version":"1","committed_version":"-2","fields":{},"trace_chain":[]}]
                """;
    }

    private static OutboxRecord record(long sequence, String change, String mutations) {
        return recordBody(sequence, change, payload(sequence, change, mutations));
    }

    private static OutboxRecord recordBody(long sequence, String change, String body) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
            return new OutboxRecord(SOURCE, sequence, change, HASH, digest, body);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError("JDK SHA-256 is unavailable", impossible);
        }
    }

    private static void expect(String code, Runnable action) {
        try {
            action.run();
            fail("expected " + code);
        } catch (CommittedEventException failure) {
            assertEquals(code, failure.code());
        }
    }

    private static long count(JdbcSqlExecutor sql, String table) {
        return ((Number) sql.queryForList("SELECT COUNT(*) AS n FROM " + table,
                new Object[0]).get(0).get("n")).longValue();
    }

    @Test public void appliesSharedConformanceFixtureWhenConfigured() throws Exception {
        String fixturePath = System.getenv("TEAQL_COMMITTED_EVENT_FIXTURE");
        Assume.assumeTrue("shared fixture path was not supplied",
                fixturePath != null && !fixturePath.isBlank());
        JsonNode fixture = new ObjectMapper().readTree(Files.readString(Path.of(fixturePath)));
        assertEquals(1, fixture.path("fixture_version").asInt());
        assertEquals(2, fixture.path("sets").size());
        Path path = Files.createTempFile("teaql-shared-model-target-", ".sqlite");
        try {
            Target t = target(path);
            for (int index = 0; index < 2; index++) {
                JsonNode set = fixture.path("sets").get(index);
                long sequence = Long.parseLong(set.path("source_sequence").asText());
                OutboxRecord record = recordBody(sequence,
                        set.path("change_set_id").asText(), set.toString());
                assertEquals(sequence, t.target().apply(t.context(), record).checkpoint());
            }
            assertEquals("SUBMITTED", t.sql().queryForList(
                    "SELECT status FROM order_data WHERE id = ?", new Object[] {ORDER_ID})
                    .get(0).get("status"));
            assertEquals("2026-10-16", t.sql().queryForList(
                    "SELECT planned_date FROM order_data WHERE id = ?", new Object[] {ORDER_ID})
                    .get(0).get("planned_date"));
            Map<String, Object> scalars = t.sql().queryForList(
                    "SELECT active, ratio, optional_note FROM order_data WHERE id = ?",
                    new Object[] {ORDER_ID}).get(0);
            assertEquals(1L, ((Number) scalars.get("active")).longValue());
            assertEquals(0.5, ((Number) scalars.get("ratio")).doubleValue(), 0.0);
            assertNull(scalars.get("optional_note"));
            assertEquals(-2L, ((Number) t.sql().queryForList(
                    "SELECT version FROM order_item_data WHERE id = ?", new Object[] {ITEM_ID})
                    .get(0).get("version")).longValue());
            assertEquals(2, count(t.sql(), "teaql_event_applied_payload"));
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void appliesWholeOrderGraphAndReturnsStableDuplicateAcks() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-", ".sqlite");
        try {
            Target t = target(path);
            OutboxRecord first = record(1, "order-graph-0001", created());
            OutboxRecord second = record(2, "order-graph-0002", submitted());
            OutboxRecord otherSource = new OutboxRecord("other-source", first.sourceSequence(),
                    first.changeSetId(), first.modelHash(), first.payloadSha256(), first.payloadJson());
            expect("UNAUTHORIZED_SOURCE", () -> t.target().apply(t.context(), otherSource));
            OutboxRecord otherModel = new OutboxRecord(first.sourceRuntimeId(), first.sourceSequence(),
                    first.changeSetId(), "sha256:" + "1".repeat(64), first.payloadSha256(), first.payloadJson());
            expect("MODEL_MISMATCH", () -> t.target().apply(t.context(), otherModel));
            String unsupported = payload(1, "unsupported", created())
                    .replace("\"protocol_version\":1", "\"protocol_version\":2");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    recordBody(1, "unsupported", unsupported)));
            expect("SEQUENCE_GAP", () -> t.target().apply(t.context(), second));
            assertEquals(0, count(t.sql(), "order_data"));
            assertEquals(1, t.target().apply(t.context(), first).checkpoint());
            assertTrue(t.target().apply(t.context(), first).duplicate());
            assertEquals(2, t.target().apply(t.context(), second).checkpoint());
            var oldAck = t.target().apply(t.context(), first);
            assertTrue(oldAck.duplicate());
            assertEquals(1, oldAck.checkpoint());
            Map<String, Object> order = t.sql().queryForList(
                    "SELECT version, order_number, status FROM order_data WHERE id = ?",
                    new Object[] {ORDER_ID}).get(0);
            assertEquals(2L, ((Number) order.get("version")).longValue());
            assertEquals("ORD-0001", order.get("order_number"));
            assertEquals("SUBMITTED", order.get("status"));
            Map<String, Object> item = t.sql().queryForList(
                    "SELECT version, order_id, quantity FROM order_item_data WHERE id = ?",
                    new Object[] {ITEM_ID}).get(0);
            assertEquals(-2L, ((Number) item.get("version")).longValue());
            assertEquals(ORDER_ID, ((Number) item.get("order_id")).longValue());
            assertEquals(2L, ((Number) item.get("quantity")).longValue());
            assertEquals(2, count(t.sql(), "teaql_event_inbox"));
            assertEquals(2, count(t.sql(), "teaql_event_applied_payload"));
            SQLiteDataSource reopenedSource = new SQLiteDataSource();
            reopenedSource.setUrl("jdbc:sqlite:" + path);
            SqliteModelCheckedCommittedTarget reopened = new SqliteModelCheckedCommittedTarget(
                    new JdbcSqlExecutor(reopenedSource), SOURCE, HASH);
            var retryAfterRestart = reopened.apply(t.context(), first);
            assertTrue(retryAfterRestart.duplicate());
            assertEquals(1, retryAfterRestart.checkpoint());
            String retained = (String) t.sql().queryForList(
                    "SELECT payload_json FROM teaql_event_applied_payload WHERE source_sequence = 2",
                    new Object[0]).get(0).get("payload_json");
            assertTrue(retained.contains("fixture-trace"));
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void rejectsUnknownModelAndRollsBackWhenSecondObjectFails() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-negative-", ".sqlite");
        try {
            Target t = target(path);
            OutboxRecord first = record(1, "order-graph-0001", created());
            String unknown = created().replace("\"quantity\":\"2\"",
                    "\"quantity\":\"2\",\"not_in_model\":1");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(), record(1, "unknown", unknown)));
            String reverseRelation = created().replace("\"status\":\"DRAFT\"",
                    "\"status\":\"DRAFT\",\"items\":[]");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "reverse-relation", reverseRelation)));
            String invalidTrace = created().replace("\"trace_chain\":[]",
                    "\"trace_chain\":[{\"kind\":\"AuditReason\",\"entity_type\":\"order\","
                    + "\"entity_id\":\"9007199254740993\",\"comment\":\"Create order\"}]");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "invalid-trace", invalidTrace)));
            String missingTrace = created().replace(",\"trace_chain\":[]", "");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "missing-trace", missingTrace)));
            t.sql().execute("CREATE TRIGGER reject_item BEFORE INSERT ON order_item_data "
                    + "BEGIN SELECT RAISE(ABORT, 'reject item'); END");
            try {
                t.target().apply(t.context(), first);
                fail("expected second-object failure");
            } catch (RuntimeException expected) {
                assertEquals(0, count(t.sql(), "order_data"));
                assertEquals(0, count(t.sql(), "order_item_data"));
                assertEquals(0, count(t.sql(), "teaql_event_inbox"));
                assertEquals(0, count(t.sql(), "teaql_event_applied_payload"));
                assertEquals(0, count(t.sql(), "teaql_event_checkpoint"));
            }
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void preservesLargeLongFieldWithoutJsonNumberRounding() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-large-long-", ".sqlite");
        try {
            Target t = target(path);
            String numeric = created().replace("\"quantity\":\"2\"",
                    "\"quantity\":9007199254740993");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "unsafe-json-number", numeric)));
            for (String invalid : new String[] {"02", "-0", "9223372036854775808"}) {
                String malformed = created().replace("\"quantity\":\"2\"",
                        "\"quantity\":\"" + invalid + "\"");
                expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                        record(1, "invalid-integer", malformed)));
            }
            assertEquals(0, count(t.sql(), "order_data"));
            String textual = created().replace("\"quantity\":\"2\"",
                    "\"quantity\":\"9007199254740993\"");
            t.target().apply(t.context(), record(1, "lossless-long", textual));
            Object stored = t.sql().queryForList("SELECT quantity FROM order_item_data WHERE id = ?",
                    new Object[] {ITEM_ID}).get(0).get("quantity");
            assertEquals(9_007_199_254_740_993L, ((Number) stored).longValue());
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void acceptsOnlyCanonicalCalendarDate() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-date-", ".sqlite");
        try {
            Target t = target(path);
            for (String invalid : new String[] {"2026-2-3", "2026-02-30", "2026-10-16T00:00:00Z"}) {
                String malformed = created().replace("\"planned_date\":\"2026-10-16\"",
                        "\"planned_date\":\"" + invalid + "\"");
                expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                        record(1, "invalid-date", malformed)));
            }
            String numeric = created().replace("\"planned_date\":\"2026-10-16\"",
                    "\"planned_date\":20261016");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "numeric-date", numeric)));
            assertEquals(0, count(t.sql(), "order_data"));
            t.target().apply(t.context(), record(1, "valid-date", created()));
            assertEquals("2026-10-16", t.sql().queryForList(
                    "SELECT planned_date FROM order_data WHERE id = ?", new Object[] {ORDER_ID})
                    .get(0).get("planned_date"));
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void rejectsSqliteRealCoercionOfExactDecimal() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-decimal-", ".sqlite");
        try {
            Target t = target(path);
            for (String decimal : new String[] {"0.1", "123456789012345.6789"}) {
                String inexact = created().replace("\"amount\":\"0\"",
                        "\"amount\":\"" + decimal + "\"");
                expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                        record(1, "inexact-decimal", inexact)));
                assertEquals(0, count(t.sql(), "order_data"));
                assertEquals(0, count(t.sql(), "teaql_event_inbox"));
                assertEquals(0, count(t.sql(), "teaql_event_checkpoint"));
                assertEquals(0, count(t.sql(), "teaql_event_applied_payload"));
            }
            t.target().apply(t.context(), record(1, "exact-decimal", created()));
            Map<String, Object> stored = t.sql().queryForList(
                    "SELECT typeof(amount) AS storage_type, amount FROM order_data WHERE id = ?",
                    new Object[] {ORDER_ID}).get(0);
            assertEquals("integer", stored.get("storage_type"));
            assertEquals(0L, ((Number) stored.get("amount")).longValue());
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void preservesBooleanFiniteFloatAndExplicitNull() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-scalar-kinds-", ".sqlite");
        try {
            Target t = target(path);
            String textBoolean = created().replace("\"active\":true", "\"active\":\"true\"");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "text-boolean", textBoolean)));
            String textFloat = created().replace("\"ratio\":0.5", "\"ratio\":\"0.5\"");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "text-float", textFloat)));
            String nullBoolean = created().replace("\"active\":true", "\"active\":null");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "null-boolean", nullBoolean)));
            String omitted = created().replace(",\"optional_note\":null", "");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(),
                    record(1, "omitted-note", omitted)));
            assertEquals(0, count(t.sql(), "order_data"));

            t.target().apply(t.context(), record(1, "valid-scalar-kinds", created()));
            Map<String, Object> stored = t.sql().queryForList(
                    "SELECT active, ratio, optional_note FROM order_data WHERE id = ?",
                    new Object[] {ORDER_ID}).get(0);
            assertEquals(1L, ((Number) stored.get("active")).longValue());
            assertEquals(0.5, ((Number) stored.get("ratio")).doubleValue(), 0.0);
            assertNull(stored.get("optional_note"));
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void rejectsTriggerRewriteAndStalePredecessor() throws Exception {
        Path path = Files.createTempFile("teaql-model-target-trigger-", ".sqlite");
        try {
            Target t = target(path);
            OutboxRecord first = record(1, "order-graph-0001", created());
            t.sql().execute("CREATE TRIGGER rewrite_order AFTER INSERT ON order_data "
                    + "BEGIN UPDATE order_data SET status = 'TARGET_OVERRIDE' WHERE id = NEW.id; END");
            expect("INVALID_ENVELOPE", () -> t.target().apply(t.context(), first));
            assertEquals(0, count(t.sql(), "order_data"));
            assertEquals(0, count(t.sql(), "teaql_event_inbox"));
            t.sql().execute("DROP TRIGGER rewrite_order");
            t.target().apply(t.context(), first);
            String stale = submitted().replaceFirst("\\\"base_version\\\":\\\"1\\\"",
                    "\"base_version\":\"9\"").replaceFirst("\\\"committed_version\\\":\\\"2\\\"",
                    "\"committed_version\":\"10\"");
            expect("VERSION_CONFLICT", () -> t.target().apply(t.context(), record(2, "stale", stale)));
            assertEquals(1, count(t.sql(), "teaql_event_inbox"));
        } finally { Files.deleteIfExists(path); }
    }

    @Test public void orderedPublisherRecoversAmbiguousAckAfterSourceReopen() throws Exception {
        Path sourcePath = Files.createTempFile("teaql-source-delivery-", ".sqlite");
        Path targetPath = Files.createTempFile("teaql-target-delivery-", ".sqlite");
        try {
            SQLiteDataSource sourceDataSource = new SQLiteDataSource();
            sourceDataSource.setUrl("jdbc:sqlite:" + sourcePath);
            JdbcSqlExecutor sourceSql = new JdbcSqlExecutor(sourceDataSource);
            SqliteCommittedEventStore sourceEvents = new SqliteCommittedEventStore(sourceSql);
            sourceEvents.ensureSchema();
            for (String change : new String[] {"order-graph-0001", "order-graph-0002"}) {
                String mutations = change.endsWith("0001") ? created() : submitted();
                sourceSql.executeInTransaction(() -> sourceEvents.append(SOURCE, change, HASH,
                        sequence -> record(sequence, change, mutations).payloadJson()));
            }
            Target target = target(targetPath);
            SqliteCommittedOutboxPublisher publisher = new SqliteCommittedOutboxPublisher(
                    sourceSql, SOURCE, "analytics", HASH);
            assertEquals(2, publisher.status().backlog());
            expect("ACK_MISMATCH", () -> publisher.publishBatch(2,
                    record -> SqliteCommittedEventStore.InboxOutcome.applied(99)));
            assertEquals(0, publisher.status().ackedSequence());
            try {
                publisher.publishBatch(2, record -> {
                    target.target().apply(target.context(), record);
                    throw new IllegalStateException("acknowledgement lost after target commit");
                });
                fail("expected ambiguous acknowledgement");
            } catch (IllegalStateException expected) {
                assertEquals("acknowledgement lost after target commit", expected.getMessage());
            }
            assertEquals(0, publisher.status().ackedSequence());
            assertEquals(1, count(target.sql(), "teaql_event_inbox"));

            SqliteCommittedOutboxPublisher restarted = new SqliteCommittedOutboxPublisher(
                    new JdbcSqlExecutor(sourceDataSource), SOURCE, "analytics", HASH);
            var outcome = restarted.publishBatch(2,
                    record -> target.target().apply(target.context(), record));
            assertEquals(2, outcome.acknowledged());
            assertEquals(2, outcome.status().ackedSequence());
            assertEquals(0, outcome.status().backlog());
            assertEquals(0, restarted.publishBatch(2, record -> {
                throw new AssertionError("acked event must not be delivered again");
            }).acknowledged());
            assertEquals(2, count(target.sql(), "teaql_event_inbox"));
            assertEquals("SUBMITTED", target.sql().queryForList(
                    "SELECT status FROM order_data WHERE id = ?", new Object[] {ORDER_ID})
                    .get(0).get("status"));
            assertEquals(2, new SqliteCommittedOutboxPublisher(sourceSql, SOURCE,
                    "second-target", HASH).status().backlog());
            expect("MODEL_MISMATCH", () -> new SqliteCommittedOutboxPublisher(sourceSql,
                    SOURCE, "analytics", "sha256:" + "1".repeat(64)).status());
            sourceSql.update("DELETE FROM teaql_event_outbox WHERE source_runtime_id = ? "
                    + "AND source_sequence = 1", new Object[] {SOURCE});
            expect("SEQUENCE_GAP", () -> new SqliteCommittedOutboxPublisher(sourceSql,
                    SOURCE, "second-target", HASH).publishBatch(2, record -> {
                throw new AssertionError("gap must be rejected before delivery");
            }));
        } finally {
            Files.deleteIfExists(sourcePath);
            Files.deleteIfExists(targetPath);
        }
    }
}
