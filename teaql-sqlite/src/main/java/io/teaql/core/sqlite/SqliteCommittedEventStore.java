package io.teaql.core.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;

/**
 * SQLite durability boundary for already-committed logical mutation facts.
 *
 * <p>The caller must install the schema before use. Append and apply require the
 * same active SQL transaction as business writes. This class deliberately does
 * not infer mutations from a ledger, replay business logic, or expose a remote
 * replication endpoint. Projection writers are trusted infrastructure code.
 */
public final class SqliteCommittedEventStore {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final SqlExecutionAdapter sql;

    public SqliteCommittedEventStore(SqlExecutionAdapter sql) {
        this.sql = Objects.requireNonNull(sql, "sql");
    }

    public record OutboxRecord(String sourceRuntimeId, long sourceSequence,
            String changeSetId, String modelHash, String payloadSha256, String payloadJson) {
        @Override public String toString() {
            return "OutboxRecord[sourceRuntimeId=" + sourceRuntimeId
                    + ", sourceSequence=" + sourceSequence + ", payload=<redacted>]";
        }
    }

    public record InboxOutcome(boolean duplicate, long checkpoint) {
        public static InboxOutcome applied(long checkpoint) { return new InboxOutcome(false, checkpoint); }
        public static InboxOutcome duplicate(long checkpoint) { return new InboxOutcome(true, checkpoint); }
    }

    @FunctionalInterface
    interface TrustedProjectionWriter {
        void apply(SqlExecutionAdapter transaction, JsonNode committedFacts);
    }

    public static final class CommittedEventException extends RuntimeException {
        private final String code;

        CommittedEventException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() { return code; }
    }

    public void ensureSchema() {
        SqliteCommittedObservation.ensureSchema(sql);
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_sequence ("
                + "source_runtime_id TEXT PRIMARY KEY, "
                + "current_sequence INTEGER NOT NULL CHECK(current_sequence >= 0))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_outbox ("
                + "source_runtime_id TEXT NOT NULL, "
                + "source_sequence INTEGER NOT NULL CHECK(source_sequence > 0), "
                + "change_set_id TEXT NOT NULL, model_hash TEXT NOT NULL, "
                + "payload_sha256 TEXT NOT NULL, payload_json TEXT NOT NULL, "
                + "PRIMARY KEY(source_runtime_id, source_sequence), "
                + "UNIQUE(source_runtime_id, change_set_id))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_inbox ("
                + "source_runtime_id TEXT NOT NULL, change_set_id TEXT NOT NULL, "
                + "source_sequence INTEGER NOT NULL CHECK(source_sequence > 0), "
                + "payload_sha256 TEXT NOT NULL, PRIMARY KEY(source_runtime_id, change_set_id))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_applied_payload ("
                + "source_runtime_id TEXT NOT NULL, source_sequence INTEGER NOT NULL CHECK(source_sequence > 0), "
                + "change_set_id TEXT NOT NULL, payload_json TEXT NOT NULL, "
                + "PRIMARY KEY(source_runtime_id, source_sequence), "
                + "UNIQUE(source_runtime_id, change_set_id))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_checkpoint ("
                + "source_runtime_id TEXT PRIMARY KEY, "
                + "source_sequence INTEGER NOT NULL CHECK(source_sequence >= 0), "
                + "model_hash TEXT NOT NULL)");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_delivery_cursor ("
                + "source_runtime_id TEXT NOT NULL, target_id TEXT NOT NULL, "
                + "acked_sequence INTEGER NOT NULL CHECK(acked_sequence >= 0), "
                + "model_hash TEXT NOT NULL, PRIMARY KEY(source_runtime_id, target_id))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_snapshot ("
                + "source_runtime_id TEXT PRIMARY KEY, snapshot_id TEXT NOT NULL, "
                + "source_sequence INTEGER NOT NULL CHECK(source_sequence >= 0), model_hash TEXT NOT NULL, "
                + "payload_sha256 TEXT NOT NULL, payload_json TEXT NOT NULL)");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_snapshot_publication ("
                + "source_runtime_id TEXT NOT NULL, target_id TEXT NOT NULL, snapshot_id TEXT NOT NULL, "
                + "source_sequence INTEGER NOT NULL CHECK(source_sequence >= 0), model_hash TEXT NOT NULL, "
                + "payload_sha256 TEXT NOT NULL, payload_json TEXT NOT NULL, "
                + "acknowledged INTEGER NOT NULL CHECK(acknowledged IN (0,1)), "
                + "PRIMARY KEY(source_runtime_id,target_id))");
    }

    /** Bounded publisher read of one durable, ordered source stream. */
    public List<OutboxRecord> outboxAfter(String sourceRuntimeId, long after, int limit) {
        if (blank(sourceRuntimeId) || after < 0 || limit < 1 || limit > 1000) {
            throw invalid("invalid source, position, or read limit");
        }
        return sql.queryForList("SELECT source_runtime_id, source_sequence, change_set_id, "
                + "model_hash, payload_sha256, payload_json FROM teaql_event_outbox "
                + "WHERE source_runtime_id = ? AND source_sequence > ? "
                + "ORDER BY source_sequence ASC LIMIT ?", new Object[] {sourceRuntimeId, after, limit})
                .stream().map(row -> new OutboxRecord((String) row.get("source_runtime_id"),
                        number(row.get("source_sequence")), (String) row.get("change_set_id"),
                        (String) row.get("model_hash"), (String) row.get("payload_sha256"),
                        (String) row.get("payload_json"))).toList();
    }

    /** Allocate and append within the caller's already-open business transaction. */
    public OutboxRecord append(String sourceRuntimeId, String changeSetId, String modelHash,
            LongFunction<String> payloadBuilder) {
        requireTransaction();
        if (blank(sourceRuntimeId) || blank(changeSetId) || blank(modelHash) || payloadBuilder == null) {
            throw invalid("missing committed-event identity or payload builder");
        }
        return savepoint("teaql_event_append", () -> {
            sql.update("INSERT INTO teaql_event_sequence(source_runtime_id, current_sequence) "
                    + "VALUES (?, 0) ON CONFLICT(source_runtime_id) DO NOTHING",
                    new Object[] {sourceRuntimeId});
            int updated = sql.update("UPDATE teaql_event_sequence SET current_sequence = current_sequence + 1 "
                    + "WHERE source_runtime_id = ? AND current_sequence < 9223372036854775807",
                    new Object[] {sourceRuntimeId});
            if (updated != 1) throw new CommittedEventException("SEQUENCE_EXHAUSTED", "source sequence exhausted");
            long sequence = number(sql.queryForList("SELECT current_sequence FROM teaql_event_sequence "
                    + "WHERE source_runtime_id = ?", new Object[] {sourceRuntimeId}).get(0).get("current_sequence"));
            String payloadJson = payloadBuilder.apply(sequence);
            JsonNode payload = parse(payloadJson);
            validateHeader(payload, sourceRuntimeId, sequence, changeSetId, modelHash);
            String digest = digest(payloadJson);
            sql.update("INSERT INTO teaql_event_outbox "
                    + "(source_runtime_id, source_sequence, change_set_id, model_hash, payload_sha256, payload_json) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                    new Object[] {sourceRuntimeId, sequence, changeSetId, modelHash, digest, payloadJson});
            return new OutboxRecord(sourceRuntimeId, sequence, changeSetId, modelHash, digest, payloadJson);
        });
    }

    /**
     * Apply one set through a restricted projection writer. The writer must not
     * call normal entity save, Checker/Fix, ID generation or business hooks.
     */
    InboxOutcome apply(OutboxRecord record, String expectedModelHash,
            TrustedProjectionWriter writer) {
        requireTransaction();
        if (record == null || blank(expectedModelHash) || writer == null) {
            throw invalid("missing committed event, model hash, or projection writer");
        }
        if (!expectedModelHash.equals(record.modelHash())) {
            throw new CommittedEventException("MODEL_MISMATCH", "committed-event model hash mismatch");
        }
        JsonNode payload = parse(record.payloadJson());
        validateHeader(payload, record.sourceRuntimeId(), record.sourceSequence(),
                record.changeSetId(), record.modelHash());
        if (!digest(record.payloadJson()).equals(record.payloadSha256())) {
            throw invalid("payload digest mismatch");
        }
        List<Map<String, Object>> checkpoints = sql.queryForList(
                "SELECT source_sequence, model_hash FROM teaql_event_checkpoint WHERE source_runtime_id = ?",
                new Object[] {record.sourceRuntimeId()});
        long last = checkpoints.isEmpty() ? 0 : number(checkpoints.get(0).get("source_sequence"));
        if (!checkpoints.isEmpty() && !expectedModelHash.equals(checkpoints.get(0).get("model_hash"))) {
            throw new CommittedEventException("MODEL_MISMATCH", "committed-event checkpoint model mismatch");
        }
        List<Map<String, Object>> prior = sql.queryForList(
                "SELECT source_sequence, payload_sha256 FROM teaql_event_inbox "
                        + "WHERE source_runtime_id = ? AND change_set_id = ?",
                new Object[] {record.sourceRuntimeId(), record.changeSetId()});
        if (!prior.isEmpty()) {
            Map<String, Object> seen = prior.get(0);
            if (number(seen.get("source_sequence")) == record.sourceSequence()
                    && record.payloadSha256().equals(seen.get("payload_sha256"))) {
                List<Map<String, Object>> retained = sql.queryForList(
                        "SELECT source_sequence, payload_json FROM teaql_event_applied_payload "
                                + "WHERE source_runtime_id = ? AND change_set_id = ?",
                        new Object[] {record.sourceRuntimeId(), record.changeSetId()});
                if (retained.size() != 1
                        || number(retained.get(0).get("source_sequence")) != record.sourceSequence()
                        || !record.payloadSha256().equals(digest((String) retained.get(0).get("payload_json")))) {
                    throw invalid("prior Inbox identity lacks retained source evidence");
                }
                if (last < record.sourceSequence()) {
                    throw invalid("prior Inbox identity is ahead of target checkpoint");
                }
                return InboxOutcome.duplicate(record.sourceSequence());
            }
            throw new CommittedEventException("DUPLICATE_CONFLICT", "committed-event identity has conflicting content");
        }
        if (last == Long.MAX_VALUE) {
            throw new CommittedEventException("SEQUENCE_EXHAUSTED", "source sequence exhausted");
        }
        if (record.sourceSequence() != last + 1) {
            throw new CommittedEventException("SEQUENCE_GAP",
                    "committed-event sequence gap: expected " + (last + 1) + ", received " + record.sourceSequence());
        }
        return savepoint("teaql_event_apply", () -> {
            writer.apply(sql, payload);
            sql.update("INSERT INTO teaql_event_applied_payload "
                    + "(source_runtime_id, source_sequence, change_set_id, payload_json) VALUES (?, ?, ?, ?)",
                    new Object[] {record.sourceRuntimeId(), record.sourceSequence(),
                            record.changeSetId(), record.payloadJson()});
            sql.update("INSERT INTO teaql_event_inbox "
                    + "(source_runtime_id, change_set_id, source_sequence, payload_sha256) VALUES (?, ?, ?, ?)",
                    new Object[] {record.sourceRuntimeId(), record.changeSetId(),
                            record.sourceSequence(), record.payloadSha256()});
            sql.update("INSERT INTO teaql_event_checkpoint (source_runtime_id, source_sequence, model_hash) "
                    + "VALUES (?, ?, ?) ON CONFLICT(source_runtime_id) DO UPDATE SET "
                    + "source_sequence = excluded.source_sequence, model_hash = excluded.model_hash",
                    new Object[] {record.sourceRuntimeId(), record.sourceSequence(), record.modelHash()});
            return InboxOutcome.applied(record.sourceSequence());
        });
    }

    private static JsonNode parse(String payloadJson) {
        if (payloadJson == null) throw invalid("missing payload");
        try {
            return JSON.readTree(payloadJson);
        } catch (Exception ignored) {
            throw invalid("invalid payload JSON");
        }
    }

    private static void validateHeader(JsonNode payload, String source, long sequence,
            String changeSetId, String modelHash) {
        if (blank(source) || sequence < 1 || blank(changeSetId) || blank(modelHash)
                || payload == null || payload.path("protocol_version").asInt(-1) != 1
                || !source.equals(payload.path("source_runtime_id").asText())
                || !Long.toString(sequence).equals(payload.path("source_sequence").asText())
                || !changeSetId.equals(payload.path("change_set_id").asText())
                || !modelHash.equals(payload.path("source_model_hash").asText())
                || !payload.path("mutations").isArray() || payload.path("mutations").isEmpty()) {
            throw invalid("required v1 header or mutations mismatch");
        }
    }

    private <T> T savepoint(String name, java.util.function.Supplier<T> action) {
        sql.execute("SAVEPOINT " + name);
        try {
            T value = action.get();
            sql.execute("RELEASE SAVEPOINT " + name);
            return value;
        } catch (RuntimeException | Error failure) {
            try {
                sql.execute("ROLLBACK TO SAVEPOINT " + name);
                sql.execute("RELEASE SAVEPOINT " + name);
            } catch (RuntimeException rollbackFailure) {
                // A failed savepoint rollback must not leave partial rows committable.
                try { sql.execute("ROLLBACK"); } catch (RuntimeException ignored) { }
                throw new CommittedEventException("ROLLBACK_FAILED", "committed-event rollback failed");
            }
            throw failure;
        }
    }

    private void requireTransaction() {
        if (!sql.hasActiveTransaction()) throw invalid("an active SQL transaction is required");
    }

    private static long number(Object value) { return ((Number) value).longValue(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static CommittedEventException invalid(String reason) {
        return new CommittedEventException("INVALID_ENVELOPE", "invalid committed-event envelope: " + reason);
    }
    private static String digest(String payload) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("JDK SHA-256 is unavailable", impossible);
        }
    }
}
