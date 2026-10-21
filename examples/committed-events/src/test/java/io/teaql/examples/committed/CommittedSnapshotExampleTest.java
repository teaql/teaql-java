package io.teaql.examples.committed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.*;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static org.junit.Assert.*;

/** Trusted bootstrap example, not a remote mutation/restore API. */
public class CommittedSnapshotExampleTest {
    private static final String SOURCE = "snapshot-example", MODEL = "sha256:" + "0".repeat(64);
    private static final long ID = 9_007_199_254_740_993L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SqliteCommittedSnapshot.Limits LIMITS = new SqliteCommittedSnapshot.Limits(10, 64 * 1024);

    private static final class Fixture implements AutoCloseable {
        final Path directory = Files.createTempDirectory("teaql-snapshot-example-");
        final JdbcSqlExecutor sql;
        final DefaultUserContext context;
        Fixture(boolean legacy) throws Exception {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + directory.resolve("data.sqlite"));
            sql = new JdbcSqlExecutor(dataSource);
            SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
            metadata.register(entity("customer_order", true)); metadata.register(entity("empty_probe", false));
            context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                    .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", sql, dataSource))
                    .executionLogging(false).build());
            try {
                context.ensureSchema(); // Always through the Context, using the local runtime.
                new SqliteCommittedEventStore(sql).ensureSchema();
                if (legacy) {
                    // Only a legacy database fixture: these predate capture, not a seed API.
                    sql.update("INSERT INTO customer_order_data(id,version,name,occurred_at,active,note) VALUES(?,7,'LEGACY',-315619199877,1,NULL)", new Object[] {ID});
                    sql.update("INSERT INTO customer_order_data(id,version,name,occurred_at,active,note) VALUES(?,-4,'已删除',0,0,'retained tombstone')", new Object[] {ID + 1});
                }
            } catch (Exception | Error failure) {
                try { close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); } throw failure;
            }
        }
        SqliteCommittedSnapshot snapshots() { return new SqliteCommittedSnapshot(sql, SOURCE, MODEL); }
        @Override public void close() throws Exception {
            for (String name : List.of("data.sqlite-wal", "data.sqlite-shm", "data.sqlite-journal", "data.sqlite")) Files.deleteIfExists(directory.resolve(name));
            Files.deleteIfExists(directory);
        }
    }
    private static SQLEntityDescriptor entity(String name, boolean order) {
        SQLEntityDescriptor entity = new SQLEntityDescriptor(); entity.setType(name); entity.setDataService("sqlite");
        for (String field : order ? List.of("id", "version", "name", "occurred_at", "active", "note") : List.of("id", "version", "name")) {
            Class<?> type = switch (field) { case "id", "version" -> Long.class; case "occurred_at" -> Instant.class; case "active" -> Boolean.class; default -> String.class; };
            GenericSQLProperty property = (GenericSQLProperty) entity.addSimpleProperty(field, type);
            property.setColumnType(type == String.class ? "VARCHAR(255)" : "BIGINT");
        }
        return entity;
    }
    private static String tail(long n) {
        return """
            {"protocol_version":1,"change_set_id":"snapshot-example-tail","source_runtime_id":"snapshot-example",
             "source_sequence":"%d","source_model_hash":"%s","source_transaction_id":"snapshot-example-tx",
             "committed_at":"2026-10-22T00:00:00Z","audit":{"comment":"advance legacy order","purpose":"verify snapshot tail"},
             "mutations":[{"entity_type":"customer_order","entity_id":"9007199254740993","operation":"update",
             "base_version":"7","committed_version":"8","fields":{"name":"AFTER SNAPSHOT"},"trace_chain":[]}]}
            """.formatted(n, MODEL);
    }
    private static void commitFixtureTail(Fixture source) {
        // Infrastructure simulation; normal application saves are demonstrated by
        // CommittedEventsExampleTest and use the audited Mutation API instead.
        source.sql.executeInTransaction(() -> {
            assertEquals(1, source.sql.update("UPDATE customer_order_data SET version=8,name='AFTER SNAPSHOT' WHERE id=? AND version=7", new Object[] {ID}));
            new SqliteCommittedEventStore(source.sql).append(SOURCE, "snapshot-example-tail", MODEL, CommittedSnapshotExampleTest::tail);
        });
    }
    private static String digest(String text) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    private static void write(Path directory, String name, String text) throws Exception {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(name + ".json"), text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve(name + ".sha256"), digest(text), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    private static String read(Path directory, String name) throws Exception {
        String text = Files.readString(directory.resolve(name + ".json"), StandardCharsets.UTF_8);
        assertEquals("exchange digest", Files.readString(directory.resolve(name + ".sha256")).trim(), digest(text)); return text;
    }
    private static void assertTarget(Fixture target) {
        var rows = target.sql.queryForList("SELECT * FROM customer_order_data ORDER BY id", new Object[0]);
        assertEquals(2, rows.size()); assertEquals(ID, ((Number) rows.get(0).get("id")).longValue());
        assertEquals(8, ((Number) rows.get(0).get("version")).longValue()); assertEquals("AFTER SNAPSHOT", rows.get(0).get("name"));
        assertEquals(-315619199877L, ((Number) rows.get(0).get("occurred_at")).longValue());
        assertEquals(1, ((Number) rows.get(0).get("active")).intValue()); assertNull(rows.get(0).get("note"));
        assertEquals(-4, ((Number) rows.get(1).get("version")).longValue()); assertEquals("已删除", rows.get(1).get("name"));
        assertEquals(0, ((Number) target.sql.queryForList("SELECT count(*) AS n FROM empty_probe_data", new Object[0]).get(0).get("n")).intValue());
        assertEquals(1, ((Number) target.sql.queryForList("SELECT source_sequence FROM teaql_event_checkpoint", new Object[0]).get(0).get("source_sequence")).intValue());
    }

    @Test public void legacySnapshotAndTailPreserveFactsAndStableRetry() throws Exception {
        String input = System.getenv("TEAQL_SNAPSHOT_IMPORT_DIR"), output = System.getenv("TEAQL_SNAPSHOT_EXPORT_DIR");
        assertFalse("separate producer/consumer roles", input != null && output != null);
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(false)) {
            SqliteCommittedSnapshot.SnapshotRecord snapshot;
            SqliteCommittedEventStore.OutboxRecord event;
            if (input == null) {
                snapshot = source.snapshots().exportSnapshot(source.context, "example-snapshot", "copy legacy facts", "initialize analytics", LIMITS);
                assertEquals(0, snapshot.sourceSequence());
                commitFixtureTail(source); event = new SqliteCommittedEventStore(source.sql).outboxAfter(SOURCE, 0, 10).get(0);
                if (output != null) { write(Path.of(output), "snapshot", snapshot.payloadJson()); write(Path.of(output), "tail", event.payloadJson()); }
            } else {
                String text = read(Path.of(input), "snapshot"); JsonNode body = JSON.readTree(text);
                snapshot = new SqliteCommittedSnapshot.SnapshotRecord(SOURCE, Long.parseLong(body.path("source_sequence").asText()), body.path("snapshot_id").asText(), MODEL, digest(text), text);
                text = read(Path.of(input), "tail"); body = JSON.readTree(text);
                event = new SqliteCommittedEventStore.OutboxRecord(SOURCE, Long.parseLong(body.path("source_sequence").asText()), body.path("change_set_id").asText(), MODEL, digest(text), text);
            }
            assertEquals(0, target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).checkpoint());
            var applier = new SqliteModelCheckedCommittedTarget(target.sql, SOURCE, MODEL);
            assertEquals(1, applier.apply(target.context, event).checkpoint()); assertTrue(applier.apply(target.context, event).duplicate());
            assertEquals(0, target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).checkpoint()); assertTarget(target);
            if (input == null) assertEquals(source.sql.queryForList("SELECT * FROM customer_order_data ORDER BY id", new Object[0]), target.sql.queryForList("SELECT * FROM customer_order_data ORDER BY id", new Object[0]));
            System.out.println("CMR11_EXAMPLE_PASS language=java mode=" + (input == null ? "local" : "exchange") + " snapshot=0 tail=1 rows=2 catalog=2 duplicate_after_tail=1");
        }
    }
}
