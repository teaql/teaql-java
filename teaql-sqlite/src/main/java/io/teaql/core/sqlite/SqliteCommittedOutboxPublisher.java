package io.teaql.core.sqlite;

import io.teaql.core.sqlite.SqliteCommittedEventStore.CommittedEventException;
import io.teaql.core.sqlite.SqliteCommittedEventStore.InboxOutcome;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded ordered publication from one durable SQLite Outbox stream. The
 * supplied sink must own its authentication and network retry budget; this
 * publisher never treats a callback as proof of remote identity.
 */
public final class SqliteCommittedOutboxPublisher {
    @FunctionalInterface
    public interface DeliverySink {
        InboxOutcome deliver(OutboxRecord record);
    }

    public record DeliveryStatus(long ackedSequence, long sourceHighWatermark, long backlog) { }
    public record BatchOutcome(int acknowledged, DeliveryStatus status) { }

    private final SqlExecutionAdapter sql;
    private final SqliteCommittedEventStore events;
    private final String sourceRuntimeId;
    private final String targetId;
    private final String modelHash;

    public SqliteCommittedOutboxPublisher(SqlExecutionAdapter sql, String sourceRuntimeId,
            String targetId, String modelHash) {
        this.sql = Objects.requireNonNull(sql, "sql");
        if (sourceRuntimeId == null || sourceRuntimeId.isBlank()
                || targetId == null || targetId.isBlank()
                || modelHash == null || !modelHash.matches("sha256:[0-9a-f]{64}")) {
            throw failure("INVALID_ENVELOPE", "delivery source, target or model identity is invalid");
        }
        this.sourceRuntimeId = sourceRuntimeId;
        this.targetId = targetId;
        this.modelHash = modelHash;
        this.events = new SqliteCommittedEventStore(sql);
    }

    public DeliveryStatus status() {
        List<Map<String, Object>> rows = sql.queryForList(
                "SELECT acked_sequence, model_hash FROM teaql_event_delivery_cursor "
                        + "WHERE source_runtime_id = ? AND target_id = ?",
                new Object[] {sourceRuntimeId, targetId});
        long acked = 0;
        if (!rows.isEmpty()) {
            if (!modelHash.equals(rows.get(0).get("model_hash"))) {
                throw failure("MODEL_MISMATCH", "delivery cursor model hash mismatch");
            }
            acked = number(rows.get(0).get("acked_sequence"));
        }
        List<Map<String, Object>> sourceRows = sql.queryForList(
                "SELECT current_sequence FROM teaql_event_sequence WHERE source_runtime_id = ?",
                new Object[] {sourceRuntimeId});
        long highWatermark = sourceRows.isEmpty() ? 0
                : number(sourceRows.get(0).get("current_sequence"));
        if (highWatermark < acked) {
            throw failure("INVALID_ENVELOPE", "delivery cursor is ahead of source sequence");
        }
        return new DeliveryStatus(acked, highWatermark, highWatermark - acked);
    }

    /**
     * One attempt per event, at most {@code limit} events. The source cursor
     * advances only after an acknowledgement for the exact sequence. If the
     * target applied an event but the response is lost, a later call/restart
     * re-delivers it and the target Inbox supplies a duplicate acknowledgement.
     */
    public BatchOutcome publishBatch(int limit, DeliverySink sink) {
        Objects.requireNonNull(sink, "sink");
        if (limit < 1 || limit > 1000) throw failure("INVALID_ENVELOPE", "invalid delivery read limit");
        claimCursor();
        DeliveryStatus before = status();
        List<OutboxRecord> records = events.outboxAfter(sourceRuntimeId, before.ackedSequence(), limit);
        if (records.isEmpty() && before.backlog() > 0) {
            throw failure("SEQUENCE_GAP", "source Outbox has an unacknowledged gap");
        }
        long acked = before.ackedSequence();
        int count = 0;
        for (OutboxRecord record : records) {
            if (acked == Long.MAX_VALUE) {
                throw failure("SEQUENCE_EXHAUSTED", "source sequence exhausted");
            }
            if (record.sourceSequence() != acked + 1) {
                throw failure("SEQUENCE_GAP", "source Outbox sequence is not contiguous");
            }
            if (!modelHash.equals(record.modelHash())) {
                throw failure("MODEL_MISMATCH", "source Outbox model hash mismatch");
            }
            InboxOutcome outcome = sink.deliver(record);
            if (outcome == null || outcome.checkpoint() != record.sourceSequence()) {
                throw failure("ACK_MISMATCH", "target acknowledgement does not match delivered sequence");
            }
            int changed = sql.update("UPDATE teaql_event_delivery_cursor SET acked_sequence=? "
                    + "WHERE source_runtime_id=? AND target_id=? AND acked_sequence=? AND model_hash=?",
                    new Object[] {record.sourceSequence(),sourceRuntimeId,targetId,acked,modelHash});
            if (changed != 1) {
                throw failure("CURSOR_CONFLICT", "delivery cursor changed concurrently");
            }
            acked = record.sourceSequence();
            count++;
        }
        return new BatchOutcome(count, status());
    }

    private void claimCursor() {
        if (sql.hasActiveTransaction()) throw failure("INVALID_ENVELOPE", "publisher requires a standalone transaction");
        sql.executeInTransaction(() -> {
            if (!sql.hasActiveTransaction()) throw failure("INVALID_ENVELOPE", "publisher adapter must provide an active SQL transaction");
            var prepared = sql.queryForList("SELECT source_sequence,model_hash,acknowledged FROM teaql_event_snapshot_publication WHERE source_runtime_id=? AND target_id=?",
                    new Object[]{sourceRuntimeId,targetId});
            if (!prepared.isEmpty()) {
                var row = prepared.get(0);
                if (number(row.get("acknowledged")) == 0) throw failure("SNAPSHOT_PENDING", "snapshot bootstrap must be acknowledged before incremental delivery");
                var cursor = sql.queryForList("SELECT acked_sequence,model_hash FROM teaql_event_delivery_cursor WHERE source_runtime_id=? AND target_id=?",new Object[]{sourceRuntimeId,targetId});
                if (number(row.get("acknowledged")) != 1 || !modelHash.equals(row.get("model_hash")) || cursor.size()!=1
                        || !modelHash.equals(cursor.get(0).get("model_hash")) || number(cursor.get(0).get("acked_sequence"))<number(row.get("source_sequence")))
                    throw failure("CURSOR_CONFLICT", "snapshot/cursor state is inconsistent");
            }
            sql.update("INSERT INTO teaql_event_delivery_cursor(source_runtime_id,target_id,acked_sequence,model_hash) VALUES (?,?,0,?) "
                    + "ON CONFLICT(source_runtime_id,target_id) DO NOTHING",new Object[]{sourceRuntimeId,targetId,modelHash});
            status(); // Validate the claimed model and source position before exposing any event.
        });
    }

    private static long number(Object value) { return ((Number) value).longValue(); }
    private static CommittedEventException failure(String code, String message) {
        return new CommittedEventException(code, "committed-event " + message);
    }
}
