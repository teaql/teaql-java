package io.teaql.core.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.core.sqlite.SqliteCommittedEventStore;
import io.teaql.core.sqlite.SqliteCommittedEventStore.CommittedEventException;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Small SQLite graph fixture shared in meaning with CMR v1; no generated ERP model. */
public class SqliteCommittedEventStoreTest {
    private static final String MODEL_HASH = "sha256:" + "0".repeat(64);
    private static final String SOURCE = "sqlite-order-source";
    private static final long ORDER_ID = 9_007_199_254_740_993L;
    private static final long ITEM_ID = ORDER_ID + 1;

    private record Database(JdbcSqlExecutor driver, SqliteCommittedEventStore events) {}

    private static Database database(Path path) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + path);
        JdbcSqlExecutor driver = new JdbcSqlExecutor(source);
        return new Database(driver, new SqliteCommittedEventStore(driver));
    }

    private static void graphSchema(SqlExecutionAdapter sql) {
        sql.execute("CREATE TABLE orders (id INTEGER PRIMARY KEY, status TEXT NOT NULL, version INTEGER NOT NULL)");
        sql.execute("CREATE TABLE order_items (id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL, "
                + "quantity INTEGER NOT NULL, version INTEGER NOT NULL)");
    }

    private static long count(SqlExecutionAdapter sql, String table) {
        // This helper only receives the two fixed table names from this test.
        return ((Number) sql.queryForList("SELECT count(*) AS total FROM " + table,
                new Object[0]).get(0).get("total")).longValue();
    }

    private static String payload(long sequence, String change, boolean submitted) {
        String operation = submitted ? "update" : "create";
        String itemOperation = submitted ? "delete" : "create";
        String status = submitted ? "SUBMITTED" : "DRAFT";
        String version = submitted ? "2" : "1";
        String itemVersion = submitted ? "-2" : "1";
        String baseVersion = submitted ? "\"1\"" : "null";
        return ("""
                {"protocol_version":1,"source_runtime_id":"%s","source_sequence":"%d",
                 "change_set_id":"%s","source_model_hash":"%s","mutations":[
                  {"entity_type":"order","entity_id":"%d","operation":"%s",
                   "base_version":%s,"committed_version":"%s","fields":{"status":"%s"}},
                  {"entity_type":"order_item","entity_id":"%d","operation":"%s",
                   "base_version":%s,"committed_version":"%s",
                   "fields":{"order":{"entity_type":"order","entity_id":"%d"},"quantity":"2"}}
                 ]}
                """).formatted(SOURCE, sequence, change, MODEL_HASH, ORDER_ID, operation,
                baseVersion, version, status, ITEM_ID, itemOperation, baseVersion,
                itemVersion, ORDER_ID);
    }

    private static void applyGraph(SqlExecutionAdapter sql, JsonNode facts) {
        for (JsonNode mutation : facts.path("mutations")) {
            long id = Long.parseLong(mutation.path("entity_id").asText());
            long version = Long.parseLong(mutation.path("committed_version").asText());
            String operation = mutation.path("operation").asText();
            if (mutation.path("entity_type").asText().equals("order")) {
                String status = mutation.path("fields").path("status").asText();
                if (operation.equals("create")) {
                    sql.update("INSERT INTO orders (id, status, version) VALUES (?, ?, ?)",
                            new Object[] {id, status, version});
                } else {
                    sql.update("UPDATE orders SET status = ?, version = ? WHERE id = ?",
                            new Object[] {status, version, id});
                }
            } else if (operation.equals("create")) {
                long parent = Long.parseLong(mutation.path("fields").path("order").path("entity_id").asText());
                int quantity = mutation.path("fields").path("quantity").asInt();
                sql.update("INSERT INTO order_items (id, order_id, quantity, version) VALUES (?, ?, ?, ?)",
                        new Object[] {id, parent, quantity, version});
            } else {
                sql.update("UPDATE order_items SET version = ? WHERE id = ?", new Object[] {version, id});
            }
        }
    }

    private static OutboxRecord append(Database database, String change, boolean submitted) {
        AtomicReference<OutboxRecord> result = new AtomicReference<>();
        database.driver().executeInTransaction(() -> {
            if (submitted) {
                database.driver().update("UPDATE orders SET status = 'SUBMITTED', version = 2 WHERE id = ?",
                        new Object[] {ORDER_ID});
                database.driver().update("UPDATE order_items SET version = -2 WHERE id = ?",
                        new Object[] {ITEM_ID});
            } else {
                database.driver().update("INSERT INTO orders (id, status, version) VALUES (?, 'DRAFT', 1)",
                        new Object[] {ORDER_ID});
                database.driver().update("INSERT INTO order_items (id, order_id, quantity, version) "
                        + "VALUES (?, ?, 2, 1)", new Object[] {ITEM_ID, ORDER_ID});
            }
            result.set(database.events().append(SOURCE, change, MODEL_HASH,
                    sequence -> payload(sequence, change, submitted)));
        });
        return result.get();
    }

    private static CommittedEventException expectCode(String code, Runnable action) {
        try {
            action.run();
            fail("expected " + code);
            throw new AssertionError("unreachable");
        } catch (CommittedEventException failure) {
            assertEquals(code, failure.code());
            return failure;
        }
    }

    @Test public void sourceRollbackAndRestartPreserveAtomicOrderedFacts() throws Exception {
        Path path = Files.createTempFile("teaql-java-cmr-source-", ".sqlite");
        try {
            Database source = database(path);
            source.events().ensureSchema();
            graphSchema(source.driver());
            try {
                source.driver().executeInTransaction(() -> {
                    source.driver().update("INSERT INTO orders (id, status, version) VALUES (?, 'DRAFT', 1)",
                            new Object[] {ORDER_ID});
                    source.driver().update("INSERT INTO order_items (id, order_id, quantity, version) "
                            + "VALUES (?, ?, 2, 1)", new Object[] {ITEM_ID, ORDER_ID});
                    source.events().append(SOURCE, "change-a", MODEL_HASH,
                            sequence -> payload(sequence, "change-a", false));
                    throw new IllegalStateException("roll back complete graph");
                });
                fail("expected source rollback");
            } catch (IllegalStateException expected) {
                assertEquals("roll back complete graph", expected.getMessage());
            }
            assertEquals(0, count(source.driver(), "orders"));
            assertEquals(0, count(source.driver(), "order_items"));
            assertTrue(source.events().outboxAfter(SOURCE, 0, 10).isEmpty());

            OutboxRecord first = append(source, "change-a", false);
            OutboxRecord second = append(source, "change-b", true);
            assertFalse("raw facts must not appear in ordinary formatting", first.toString().contains("DRAFT"));
            assertFalse(first.toString().contains("order_item"));
            assertEquals(1, first.sourceSequence());
            assertEquals(2, second.sourceSequence());

            Database reopened = database(path);
            reopened.events().ensureSchema();
            List<OutboxRecord> rows = reopened.events().outboxAfter(SOURCE, 0, 10);
            assertEquals(List.of(first, second), rows);
            assertEquals(List.of(second), reopened.events().outboxAfter(SOURCE, 1, 1));
            assertEquals(1, count(reopened.driver(), "orders"));
            assertEquals(1, count(reopened.driver(), "order_items"));
            Map<String, Object> item = reopened.driver().queryForList(
                    "SELECT version FROM order_items WHERE id = ?", new Object[] {ITEM_ID}).get(0);
            assertEquals(-2, ((Number) item.get("version")).longValue());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test public void targetApplyDeduplicatesAndRollsBackWholeGraphOnFailure() throws Exception {
        Path path = Files.createTempFile("teaql-java-cmr-target-", ".sqlite");
        try {
            Database target = database(path);
            target.events().ensureSchema();
            graphSchema(target.driver());
            OutboxRecord first = record(1, "change-a", false);
            OutboxRecord second = record(2, "change-b", true);

            target.driver().executeInTransaction(() -> expectCode("SEQUENCE_GAP",
                    () -> target.events().apply(second, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph)));
            assertEquals(0, count(target.driver(), "orders"));
            assertEquals(0, count(target.driver(), "teaql_event_inbox"));

            target.driver().executeInTransaction(() -> {
                var outcome = target.events().apply(first, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph);
                assertFalse(outcome.duplicate());
                assertEquals(1, outcome.checkpoint());
            });
            target.driver().executeInTransaction(() -> {
                expectCode("MODEL_MISMATCH", () -> target.events().apply(second,
                        "sha256:" + "1".repeat(64), SqliteCommittedEventStoreTest::applyGraph));
                try {
                    target.events().apply(second, MODEL_HASH, (sql, facts) -> {
                        sql.update("UPDATE orders SET status = 'PARTIAL' WHERE id = ?", new Object[] {ORDER_ID});
                        sql.update("UPDATE order_items SET version = -99 WHERE id = ?", new Object[] {ITEM_ID});
                        throw new IllegalStateException("injected second-table failure");
                    });
                    fail("expected injected failure");
                } catch (IllegalStateException expected) {
                    assertEquals("injected second-table failure", expected.getMessage());
                }
            });
            assertEquals("DRAFT", target.driver().queryForList("SELECT status FROM orders", new Object[0])
                    .get(0).get("status"));
            assertEquals(1L, ((Number) target.driver().queryForList("SELECT version FROM order_items",
                    new Object[0]).get(0).get("version")).longValue());
            assertEquals(1, count(target.driver(), "teaql_event_inbox"));
            assertEquals(1, count(target.driver(), "teaql_event_applied_payload"));

            Database reopened = database(path);
            reopened.driver().executeInTransaction(() -> {
                var outcome = reopened.events().apply(second, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph);
                assertFalse(outcome.duplicate());
                assertEquals(2, outcome.checkpoint());
                var duplicate = reopened.events().apply(second, MODEL_HASH, (sql, facts) -> {
                    throw new AssertionError("duplicate must not rematerialize");
                });
                assertTrue(duplicate.duplicate());
                assertEquals(2, duplicate.checkpoint());
                var olderDuplicate = reopened.events().apply(first, MODEL_HASH, (sql, facts) -> {
                    throw new AssertionError("an older duplicate must not rematerialize");
                });
                assertTrue(olderDuplicate.duplicate());
                assertEquals(1, olderDuplicate.checkpoint());
            });
            assertEquals("SUBMITTED", reopened.driver().queryForList("SELECT status FROM orders", new Object[0])
                    .get(0).get("status"));
            assertEquals(-2L, ((Number) reopened.driver().queryForList(
                    "SELECT version FROM order_items", new Object[0]).get(0).get("version")).longValue());
            assertEquals(2, count(reopened.driver(), "teaql_event_inbox"));
            assertEquals(2, count(reopened.driver(), "teaql_event_applied_payload"));
            assertEquals(2L, ((Number) reopened.driver().queryForList(
                    "SELECT source_sequence FROM teaql_event_checkpoint", new Object[0])
                    .get(0).get("source_sequence")).longValue());

            OutboxRecord conflicted = record(2, "change-b", false);
            reopened.driver().executeInTransaction(() -> expectCode("DUPLICATE_CONFLICT",
                    () -> reopened.events().apply(conflicted, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph)));
            OutboxRecord invalidDigest = new OutboxRecord(second.sourceRuntimeId(), second.sourceSequence(),
                    second.changeSetId(), second.modelHash(), "f".repeat(64), second.payloadJson());
            reopened.driver().executeInTransaction(() -> expectCode("INVALID_ENVELOPE",
                    () -> reopened.events().apply(invalidDigest, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph)));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test public void appendAndApplyRequireTheCallerTransaction() throws Exception {
        Path path = Files.createTempFile("teaql-java-cmr-boundary-", ".sqlite");
        try {
            Database database = database(path);
            database.events().ensureSchema();
            OutboxRecord first = record(1, "change-a", false);
            expectCode("INVALID_ENVELOPE", () -> database.events().append(SOURCE, "change-a",
                    MODEL_HASH, sequence -> payload(sequence, "change-a", false)));
            expectCode("INVALID_ENVELOPE", () -> database.events().apply(first,
                    MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph));
            assertEquals(0, count(database.driver(), "teaql_event_outbox"));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test public void duplicateRequiresCheckpointCoveringRetainedInbox() throws Exception {
        Path path = Files.createTempFile("teaql-java-cmr-checkpoint-", ".sqlite");
        try {
            Database target = database(path);
            target.events().ensureSchema();
            graphSchema(target.driver());
            OutboxRecord first = record(1, "change-a", false);
            OutboxRecord second = record(2, "change-b", true);
            target.driver().executeInTransaction(() -> {
                target.events().apply(first, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph);
                target.events().apply(second, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph);
            });

            target.driver().update("UPDATE teaql_event_checkpoint SET source_sequence = 1 "
                    + "WHERE source_runtime_id = ?", new Object[] {SOURCE});
            target.driver().executeInTransaction(() -> expectCode("INVALID_ENVELOPE",
                    () -> target.events().apply(second, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph)));

            target.driver().update("DELETE FROM teaql_event_checkpoint WHERE source_runtime_id = ?",
                    new Object[] {SOURCE});
            target.driver().executeInTransaction(() -> expectCode("INVALID_ENVELOPE",
                    () -> target.events().apply(first, MODEL_HASH, SqliteCommittedEventStoreTest::applyGraph)));
            assertEquals(2, count(target.driver(), "teaql_event_inbox"));
            assertEquals(2, count(target.driver(), "teaql_event_applied_payload"));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static OutboxRecord record(long sequence, String change, boolean submitted) throws Exception {
        String json = payload(sequence, change, submitted);
        byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                .digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new OutboxRecord(SOURCE, sequence, change, MODEL_HASH,
                java.util.HexFormat.of().formatHex(bytes), json);
    }
}
