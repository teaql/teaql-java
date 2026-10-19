package io.teaql.core.duck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.teaql.core.duck.DuckDbCommittedInbox.Failure;
import io.teaql.core.duck.DuckDbCommittedInbox.Record;
import io.teaql.provider.jdbc.JdbcSqlExecutor;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DuckDbCommittedInboxTest {
    private static final String SOURCE = "duckdb-source";
    private static final String MODEL_HASH = "sha256:" + "a".repeat(64);
    private Path directory;
    private Path database;
    private JdbcSqlExecutor sql;
    private DuckDbCommittedInbox inbox;

    private record DriverManagerDataSource(String url) implements DataSource {
        @Override public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url);
        }
        @Override public Connection getConnection(String user, String password) throws SQLException {
            return getConnection();
        }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter writer) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("not a wrapper");
        }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    @Before public void setUp() throws Exception {
        directory = Files.createTempDirectory("teaql-duckdb-inbox-");
        database = directory.resolve("target.db");
        sql = new JdbcSqlExecutor(new DriverManagerDataSource("jdbc:duckdb:" + database));
        inbox = new DuckDbCommittedInbox(sql, SOURCE, MODEL_HASH);
        inbox.ensureSchema();
        sql.execute("CREATE TABLE replica_order (id BIGINT PRIMARY KEY, version BIGINT NOT NULL)");
    }

    @After public void tearDown() throws Exception {
        Files.deleteIfExists(database);
        Files.deleteIfExists(directory);
    }

    @Test public void appliesOnceAndRetainsReplayEvidence() {
        Record first = record(1, "set-1");
        AtomicInteger calls = new AtomicInteger();
        DuckDbCommittedInbox.TrustedProjectionWriter writer = (transaction, ignored) -> {
            calls.incrementAndGet();
            transaction.update("INSERT INTO replica_order VALUES (?, ?)", new Object[] {7L, 1L});
        };
        assertEquals(new DuckDbCommittedInbox.Outcome(false, 1), inbox.apply(first, writer));
        assertEquals(new DuckDbCommittedInbox.Outcome(true, 1), inbox.apply(first, writer));
        assertEquals(1, calls.get());
        assertEquals(1, count("replica_order"));
        assertEquals(1, count("teaql_event_inbox"));
        assertEquals(1, count("teaql_event_applied_payload"));
        assertEquals(1, count("teaql_event_checkpoint"));
        assertEquals(1L, checkpoint());

        DuckDbCommittedInbox reopened = new DuckDbCommittedInbox(
                new JdbcSqlExecutor(new DriverManagerDataSource("jdbc:duckdb:" + database)),
                SOURCE, MODEL_HASH);
        assertTrue(reopened.apply(first, (transaction, ignored) -> {
            throw new AssertionError("duplicate reran projection");
        }).duplicate());
    }

    @Test public void projectionFailureRollsBackAllStateAndCanRetry() {
        Record first = record(1, "set-1");
        Failure failure = assertThrows(Failure.class, () -> inbox.apply(first, (transaction, ignored) -> {
            transaction.update("INSERT INTO replica_order VALUES (?, ?)", new Object[] {7L, 1L});
            throw new IllegalStateException("private-sql-canary");
        }));
        assertEquals("TARGET_FAILURE", failure.code());
        assertFalse(failure.getMessage().contains("private-sql-canary"));
        for (String table : new String[] {"replica_order", "teaql_event_inbox",
                "teaql_event_applied_payload", "teaql_event_checkpoint"}) {
            assertEquals(table, 0, count(table));
        }
        assertFalse(inbox.apply(first, (transaction, ignored) -> transaction.update(
                "INSERT INTO replica_order VALUES (?, ?)", new Object[] {7L, 1L})).duplicate());
        assertEquals(1, count("replica_order"));
    }

    @Test public void gapTamperAndAmbientTransactionFailClosed() {
        assertCode("SEQUENCE_GAP", () -> inbox.apply(record(2, "set-2"),
                (transaction, ignored) -> { }));
        assertCode("INVALID_ENVELOPE", () -> inbox.apply(
                new Record(SOURCE, 1, "set-1", MODEL_HASH, "0".repeat(64), record(1, "set-1").payloadJson()),
                (transaction, ignored) -> { }));
        assertCode("UNAUTHORIZED_SOURCE", () -> inbox.apply(
                new Record("other-source", 1, "set-1", MODEL_HASH,
                        record(1, "set-1").payloadSha256(), record(1, "set-1").payloadJson()),
                (transaction, ignored) -> { }));
        sql.executeInTransaction(() -> assertCode("AMBIENT_TRANSACTION",
                () -> inbox.apply(record(1, "set-1"), (transaction, ignored) -> { })));
        assertEquals(0, count("teaql_event_inbox"));

        Record first = record(1, "set-1");
        inbox.apply(first, (transaction, ignored) -> transaction.update(
                "INSERT INTO replica_order VALUES (?, ?)", new Object[] {7L, 1L}));
        Record changed = record(1, "set-1", "update");
        assertCode("DUPLICATE_CONFLICT", () -> inbox.apply(changed,
                (transaction, ignored) -> { }));
        assertEquals(1L, checkpoint());
    }

    private long checkpoint() {
        return ((Number) sql.queryForList("SELECT source_sequence FROM teaql_event_checkpoint",
                new Object[0]).get(0).get("source_sequence")).longValue();
    }

    private int count(String table) {
        return ((Number) sql.queryForList("SELECT count(*) AS row_count FROM " + table,
                new Object[0]).get(0).get("row_count")).intValue();
    }

    private static Record record(long sequence, String changeSetId) {
        return record(sequence, changeSetId, "create");
    }

    private static Record record(long sequence, String changeSetId, String operation) {
        String payload = "{\"protocol_version\":1,\"source_runtime_id\":\"" + SOURCE
                + "\",\"source_sequence\":\"" + sequence
                + "\",\"change_set_id\":\"" + changeSetId
                + "\",\"source_model_hash\":\"" + MODEL_HASH
                + "\",\"mutations\":[{\"operation\":\"" + operation + "\"}]}";
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
            return new Record(SOURCE, sequence, changeSetId, MODEL_HASH, digest, payload);
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void assertCode(String code, Runnable action) {
        assertEquals(code, assertThrows(Failure.class, action::run).code());
    }
}
