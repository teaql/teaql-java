package io.teaql.core.duck;

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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transaction-owned target Inbox for already committed model facts. The
 * projection writer is trusted infrastructure, never a remote SQL callback.
 * DuckDB has no SAVEPOINT in the qualified versions, so apply owns the entire
 * transaction and refuses to run inside an ambient one.
 */
public final class DuckDbCommittedInbox {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final SqlExecutionAdapter sql;
    private final String sourceRuntimeId;
    private final String modelHash;

    public record Record(String sourceRuntimeId, long sourceSequence, String changeSetId,
            String modelHash, String payloadSha256, String payloadJson) {
        @Override public String toString() {
            return "Record[sourceRuntimeId=" + sourceRuntimeId + ", sourceSequence="
                    + sourceSequence + ", payload=<redacted>]";
        }
    }

    public record Outcome(boolean duplicate, long checkpoint) { }

    public static final class Failure extends RuntimeException {
        private final String code;

        private Failure(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() { return code; }
    }

    @FunctionalInterface
    interface TrustedProjectionWriter {
        void apply(SqlExecutionAdapter transaction, JsonNode committedFacts);
    }

    public DuckDbCommittedInbox(SqlExecutionAdapter sql, String sourceRuntimeId, String modelHash) {
        this.sql = Objects.requireNonNull(sql, "sql");
        if (blank(sourceRuntimeId) || modelHash == null
                || !modelHash.matches("sha256:[0-9a-f]{64}")) {
            throw invalid("target source identity or model hash is invalid");
        }
        this.sourceRuntimeId = sourceRuntimeId;
        this.modelHash = modelHash;
    }

    public void ensureSchema() {
        if (sql.hasActiveTransaction()) throw invalid("schema setup requires no active transaction");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_inbox ("
                + "source_runtime_id VARCHAR NOT NULL, change_set_id VARCHAR NOT NULL, "
                + "source_sequence BIGINT NOT NULL CHECK(source_sequence > 0), "
                + "payload_sha256 VARCHAR NOT NULL, PRIMARY KEY(source_runtime_id, change_set_id))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_applied_payload ("
                + "source_runtime_id VARCHAR NOT NULL, source_sequence BIGINT NOT NULL "
                + "CHECK(source_sequence > 0), change_set_id VARCHAR NOT NULL, "
                + "payload_json VARCHAR NOT NULL, PRIMARY KEY(source_runtime_id, source_sequence), "
                + "UNIQUE(source_runtime_id, change_set_id))");
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_checkpoint ("
                + "source_runtime_id VARCHAR PRIMARY KEY, source_sequence BIGINT NOT NULL "
                + "CHECK(source_sequence >= 0), model_hash VARCHAR NOT NULL)");
    }

    Outcome apply(Record record, TrustedProjectionWriter writer) {
        if (sql.hasActiveTransaction()) {
            throw new Failure("AMBIENT_TRANSACTION", "DuckDB committed apply requires its own transaction");
        }
        if (record == null || writer == null) throw invalid("missing record or projection writer");
        if (!sourceRuntimeId.equals(record.sourceRuntimeId())) {
            throw new Failure("UNAUTHORIZED_SOURCE", "committed-event source is not authorized");
        }
        if (!modelHash.equals(record.modelHash())) {
            throw new Failure("MODEL_MISMATCH", "committed-event model hash mismatch");
        }
        if (record.sourceSequence() < 1 || blank(record.changeSetId())
                || record.payloadSha256() == null
                || !record.payloadSha256().matches("[0-9a-f]{64}")) {
            throw invalid("record identity or digest is invalid");
        }
        JsonNode payload = parse(record.payloadJson());
        validateHeader(record, payload);
        if (!digest(record.payloadJson()).equals(record.payloadSha256())) {
            throw invalid("payload digest mismatch");
        }
        AtomicReference<Outcome> outcome = new AtomicReference<>();
        try {
            sql.executeInTransaction(() -> outcome.set(applyInside(record, payload, writer)));
        } catch (Failure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            // JDBC exceptions may include raw SQL and bound values. The
            // caller receives a stable category, never provider diagnostics.
            throw new Failure("TARGET_FAILURE", "committed target transaction failed");
        }
        return outcome.get();
    }

    private Outcome applyInside(Record record, JsonNode payload, TrustedProjectionWriter writer) {
        if (!sql.hasActiveTransaction()) {
            throw new Failure("TRANSACTION_REQUIRED", "committed target transaction is unavailable");
        }
        List<Map<String, Object>> checkpoints = sql.queryForList(
                "SELECT source_sequence, model_hash FROM teaql_event_checkpoint "
                        + "WHERE source_runtime_id = ?", new Object[] {sourceRuntimeId});
        long last = checkpoints.isEmpty() ? 0 : number(checkpoints.get(0).get("source_sequence"));
        if (!checkpoints.isEmpty() && !modelHash.equals(checkpoints.get(0).get("model_hash"))) {
            throw new Failure("MODEL_MISMATCH", "committed-event checkpoint model mismatch");
        }
        List<Map<String, Object>> prior = sql.queryForList(
                "SELECT source_sequence, payload_sha256 FROM teaql_event_inbox "
                        + "WHERE source_runtime_id = ? AND change_set_id = ?",
                new Object[] {sourceRuntimeId, record.changeSetId()});
        if (!prior.isEmpty()) {
            Map<String, Object> seen = prior.get(0);
            List<Map<String, Object>> retained = sql.queryForList(
                    "SELECT source_sequence, payload_json FROM teaql_event_applied_payload "
                            + "WHERE source_runtime_id = ? AND change_set_id = ?",
                    new Object[] {sourceRuntimeId, record.changeSetId()});
            if (number(seen.get("source_sequence")) == record.sourceSequence()
                    && record.payloadSha256().equals(seen.get("payload_sha256"))
                    && retained.size() == 1
                    && number(retained.get(0).get("source_sequence")) == record.sourceSequence()
                    && record.payloadSha256().equals(digest((String) retained.get(0).get("payload_json")))) {
                return new Outcome(true, record.sourceSequence());
            }
            throw new Failure("DUPLICATE_CONFLICT", "committed-event identity has conflicting content");
        }
        if (last == Long.MAX_VALUE) {
            throw new Failure("SEQUENCE_EXHAUSTED", "source sequence exhausted");
        }
        if (record.sourceSequence() != last + 1) {
            throw new Failure("SEQUENCE_GAP", "committed-event source sequence is not contiguous");
        }
        writer.apply(sql, payload);
        one(sql.update("INSERT INTO teaql_event_applied_payload "
                        + "(source_runtime_id, source_sequence, change_set_id, payload_json) "
                        + "VALUES (?, ?, ?, ?)",
                new Object[] {sourceRuntimeId, record.sourceSequence(),
                        record.changeSetId(), record.payloadJson()}));
        one(sql.update("INSERT INTO teaql_event_inbox "
                        + "(source_runtime_id, change_set_id, source_sequence, payload_sha256) "
                        + "VALUES (?, ?, ?, ?)",
                new Object[] {sourceRuntimeId, record.changeSetId(),
                        record.sourceSequence(), record.payloadSha256()}));
        one(sql.update("INSERT INTO teaql_event_checkpoint "
                        + "(source_runtime_id, source_sequence, model_hash) VALUES (?, ?, ?) "
                        + "ON CONFLICT(source_runtime_id) DO UPDATE SET "
                        + "source_sequence = excluded.source_sequence, model_hash = excluded.model_hash",
                new Object[] {sourceRuntimeId, record.sourceSequence(), modelHash}));
        return new Outcome(false, record.sourceSequence());
    }

    private static void validateHeader(Record record, JsonNode payload) {
        if (!payload.isObject() || !payload.path("protocol_version").isInt()
                || payload.path("protocol_version").intValue() != 1
                || !payload.path("source_runtime_id").isTextual()
                || !record.sourceRuntimeId().equals(payload.path("source_runtime_id").textValue())
                || !payload.path("source_sequence").isTextual()
                || !Long.toString(record.sourceSequence()).equals(payload.path("source_sequence").textValue())
                || !payload.path("change_set_id").isTextual()
                || !record.changeSetId().equals(payload.path("change_set_id").textValue())
                || !payload.path("source_model_hash").isTextual()
                || !record.modelHash().equals(payload.path("source_model_hash").textValue())
                || !payload.path("mutations").isArray()
                || payload.path("mutations").isEmpty()) {
            throw invalid("required v1 header or mutations mismatch");
        }
    }

    private static JsonNode parse(String json) {
        if (json == null) throw invalid("missing payload");
        try {
            JsonNode payload = JSON.readTree(json);
            if (payload == null) throw invalid("payload JSON is empty");
            return payload;
        }
        catch (Exception failure) { throw invalid("payload JSON is invalid"); }
    }

    private static String digest(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("JDK SHA-256 is unavailable", impossible);
        }
    }

    private static void one(int changed) {
        if (changed != 1) throw new Failure("TARGET_FAILURE", "committed target write count mismatch");
    }

    private static long number(Object value) { return ((Number) value).longValue(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static Failure invalid(String reason) {
        return new Failure("INVALID_ENVELOPE", "invalid committed-event envelope: " + reason);
    }
}
