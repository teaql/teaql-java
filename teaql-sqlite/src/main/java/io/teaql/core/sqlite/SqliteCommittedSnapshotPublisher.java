package io.teaql.core.sqlite;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedSnapshot.Limits;
import io.teaql.core.sqlite.SqliteCommittedSnapshot.SnapshotRecord;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Durable startup bootstrap for one trusted source/target slot, not a remote API. */
public final class SqliteCommittedSnapshotPublisher {
    public record PreparedSnapshot(SnapshotRecord snapshot, boolean acknowledged) { }
    /** Correlation, not authentication: the transport must authenticate its peer. */
    public record Receipt(String sourceRuntimeId, String targetId, String snapshotId,
            long sourceSequence, String modelHash, String payloadSha256, boolean duplicate) { }
    @FunctionalInterface public interface SnapshotSink { Receipt deliver(SnapshotRecord record); }

    private final SqlExecutionAdapter sql;
    private final String source, target, model;
    private final SqliteCommittedSnapshot snapshots;
    public SqliteCommittedSnapshotPublisher(SqlExecutionAdapter sql, String source, String target, String model) {
        this.sql = Objects.requireNonNull(sql, "sql");
        if (target == null || target.isBlank()) throw failure("INVALID_SNAPSHOT", "target identity is required");
        this.source = source; this.target = target; this.model = model;
        this.snapshots = new SqliteCommittedSnapshot(sql, source, model);
    }

    /** Retain original bytes before delivery; retries never re-export a published identity. */
    public PreparedSnapshot prepare(UserContext context, String snapshotId, String comment, String purpose, Limits limits) {
        require(context, snapshotId, limits);
        if (comment == null || comment.isBlank() || purpose == null || purpose.isBlank())
            throw failure("INVALID_SNAPSHOT", "snapshot intent is required");
        PreparedSnapshot prior = transaction(() -> existing(snapshotId, limits));
        if (prior != null) return prior;
        SnapshotRecord captured = snapshots.exportSnapshot(context, snapshotId, comment, purpose, limits);
        return transaction(() -> {
            PreparedSnapshot raced = existing(snapshotId, limits);
            if (raced != null) return raced; // First durable record wins, never replace with a newer view.
            if (!cursor().isEmpty()) throw failure("CURSOR_CONFLICT", "target stream has already started");
            sql.update("INSERT INTO teaql_event_snapshot_publication "
                    + "(source_runtime_id,target_id,snapshot_id,source_sequence,model_hash,payload_sha256,payload_json,acknowledged) "
                    + "VALUES (?,?,?,?,?,?,?,0)", new Object[] {source,target,captured.snapshotId(),captured.sourceSequence(),model,captured.payloadSha256(),captured.payloadJson()});
            return new PreparedSnapshot(captured, false);
        });
    }

    /** One attempt. Lost or mismatching ACK leaves the retained job/cursor untouched. */
    public PreparedSnapshot publish(UserContext context, String snapshotId, Limits limits, SnapshotSink sink) {
        require(context, snapshotId, limits); Objects.requireNonNull(sink, "sink");
        PreparedSnapshot prepared = transaction(() -> existing(snapshotId, limits));
        if (prepared == null) throw failure("INVALID_SNAPSHOT", "snapshot has not been durably prepared");
        if (prepared.acknowledged()) return prepared;
        SnapshotRecord record = prepared.snapshot();
        var journal = new SqliteCommittedObservation(sql,source,target,model);
        var attempt = journal.begin("snapshot",record.sourceSequence(),record.snapshotId(),record.payloadSha256());
        Receipt receipt;
        try {
        receipt = sink.deliver(record); // No source SQL transaction/connection held across transport.
        if (receipt == null || !source.equals(receipt.sourceRuntimeId()) || !target.equals(receipt.targetId())
                || !record.snapshotId().equals(receipt.snapshotId()) || record.sourceSequence() != receipt.sourceSequence()
                || !model.equals(receipt.modelHash()) || !record.payloadSha256().equals(receipt.payloadSha256()))
            throw failure("ACK_MISMATCH", "snapshot receipt does not match this source/target record");
        } catch (RuntimeException error) { journal.failed(attempt,error); throw error; }
        return journal.acknowledged(attempt, () -> {
            PreparedSnapshot current = existing(snapshotId, limits);
            if (current == null || !current.snapshot().equals(record)) throw failure("SNAPSHOT_CONFLICT", "prepared snapshot changed");
            if (current.acknowledged()) return current; // Tail may already have advanced; never rewind.
            if (!cursor().isEmpty()) throw failure("CURSOR_CONFLICT", "bootstrap cursor changed concurrently");
            var status = new SqliteCommittedOutboxPublisher(sql,source,target,model).status();
            if (status.sourceHighWatermark() < record.sourceSequence()) throw failure("INVALID_SNAPSHOT", "snapshot is ahead of source");
            sql.update("INSERT INTO teaql_event_delivery_cursor(source_runtime_id,target_id,acked_sequence,model_hash) VALUES (?,?,?,?)",
                    new Object[] {source,target,record.sourceSequence(),model});
            if (sql.update("UPDATE teaql_event_snapshot_publication SET acknowledged=1 WHERE source_runtime_id=? AND target_id=? "
                    + "AND snapshot_id=? AND payload_sha256=? AND acknowledged=0", new Object[] {source,target,snapshotId,record.payloadSha256()}) != 1)
                throw failure("CURSOR_CONFLICT", "bootstrap acknowledgement changed concurrently");
            return new PreparedSnapshot(record, true);
        });
    }

    /** Target helper: construct a receipt only after the authoritative target transaction commits. */
    public static Receipt initializeAndAcknowledge(UserContext context, SqliteCommittedSnapshot target,
            SnapshotRecord record, String targetId, Limits limits) {
        return initializeAndAcknowledge(context,target,record,targetId,SqliteCommittedSnapshot.ImportMode.REQUIRE_EMPTY,limits);
    }
    public static Receipt initializeAndAcknowledge(UserContext context, SqliteCommittedSnapshot target,
            SnapshotRecord record, String targetId, SqliteCommittedSnapshot.ImportMode mode, Limits limits) {
        if (targetId == null || targetId.isBlank()) throw failure("INVALID_SNAPSHOT", "target identity is required");
        var outcome = target.initializeSnapshot(context,record,mode,limits);
        if (outcome.checkpoint() != record.sourceSequence()) throw failure("ACK_MISMATCH", "target snapshot position differs");
        return new Receipt(record.sourceRuntimeId(),targetId,record.snapshotId(),record.sourceSequence(),record.modelHash(),record.payloadSha256(),outcome.duplicate());
    }

    private PreparedSnapshot existing(String snapshotId, Limits limits) {
        var rows = sql.queryForList("SELECT snapshot_id,source_sequence,model_hash,payload_sha256,payload_json,acknowledged "
                + "FROM teaql_event_snapshot_publication WHERE source_runtime_id=? AND target_id=?", new Object[] {source,target});
        if (rows.isEmpty()) return null;
        var row = rows.get(0);
        if (!snapshotId.equals(row.get("snapshot_id"))) throw failure("SNAPSHOT_CONFLICT", "target slot already has a different snapshot");
        var record = new SnapshotRecord(source,((Number)row.get("source_sequence")).longValue(),snapshotId,
                (String)row.get("model_hash"),(String)row.get("payload_sha256"),(String)row.get("payload_json"));
        snapshots.validate(record,limits);
        int flag = ((Number)row.get("acknowledged")).intValue();
        if (flag != 0 && flag != 1) throw failure("INVALID_SNAPSHOT", "invalid acknowledgement state");
        boolean acked = flag == 1;
        var cursors = cursor();
        if (!acked && !cursors.isEmpty() || acked && (cursors.size()!=1 || !model.equals(cursors.get(0).get("model_hash"))
                || ((Number)cursors.get(0).get("acked_sequence")).longValue()<record.sourceSequence()))
            throw failure("CURSOR_CONFLICT", "snapshot/cursor state is inconsistent");
        return new PreparedSnapshot(record,acked);
    }
    private java.util.List<java.util.Map<String,Object>> cursor() { return sql.queryForList(
            "SELECT acked_sequence,model_hash FROM teaql_event_delivery_cursor WHERE source_runtime_id=? AND target_id=?",new Object[]{source,target}); }
    private void require(UserContext context, String snapshotId, Limits limits) {
        Objects.requireNonNull(context,"context"); Objects.requireNonNull(limits,"limits");
        EntityMetaFactory.requireFrom(context);
        if (snapshotId == null || snapshotId.isBlank()) throw failure("INVALID_SNAPSHOT","snapshot identity is required");
        if (sql.hasActiveTransaction()) throw failure("INVALID_SNAPSHOT","bootstrap requires a standalone transaction");
    }
    private <T> T transaction(Supplier<T> action) { AtomicReference<T> value=new AtomicReference<>();sql.executeInTransaction(()->{
        if (!sql.hasActiveTransaction()) throw failure("INVALID_SNAPSHOT","bootstrap adapter must provide an active SQL transaction");
        value.set(action.get());});return value.get(); }
    private static SqliteCommittedEventStore.CommittedEventException failure(String code,String message) { return new SqliteCommittedEventStore.CommittedEventException(code,message); }
}
