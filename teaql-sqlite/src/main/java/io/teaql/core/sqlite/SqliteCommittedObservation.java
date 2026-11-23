package io.teaql.core.sqlite;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.runtime.RuntimeTelemetry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Durable infrastructure observations. Source ACK is never labeled target watermark. */
public final class SqliteCommittedObservation {
    public record SourceObservation(long sourceHighWatermark,long ackedSequence,long sequenceLag,
            long backlog,long pendingSnapshots,long attempts,long retries,long failures,
            long rejections,long deadLetters,long unresolvedAttempts) { }
    public record TargetObservation(long checkpoint,long snapshotBaseline,long inboxRows,
            OptionalLong sequenceLag,long rejections) { }
    public record DeadLetter(String kind,long sequence,String identityFingerprint,
            String payloadFingerprint,long attempts,String reason) { }
    record Attempt(String kind,long sequence,long ordinal) { }
    private final SqlExecutionAdapter sql;
    private final String source,target,model;
    private static final Set<String> PERMANENT=Set.of("UNAUTHORIZED_SOURCE","UNAUTHORIZED_TARGET",
            "UNKNOWN_KEY","INVALID_SIGNATURE","INVALID_ENVELOPE","MODEL_MISMATCH",
            "SEQUENCE_GAP","DUPLICATE_CONFLICT","VERSION_CONFLICT","SEQUENCE_EXHAUSTED",
            "INVALID_SNAPSHOT","SNAPSHOT_CONFLICT","TARGET_NOT_EMPTY");
    public SqliteCommittedObservation(SqlExecutionAdapter sql,String source,String target,String model){
        this.sql=Objects.requireNonNull(sql,"sql");
        if(source==null||source.isBlank()||target==null||target.isBlank()||model==null||!model.matches("sha256:[0-9a-f]{64}"))throw unavailable();
        this.source=source;this.target=target;this.model=model;
    }
    static void ensureSchema(SqlExecutionAdapter sql){
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_delivery_journal (source_runtime_id TEXT NOT NULL,target_id TEXT NOT NULL,kind TEXT NOT NULL CHECK(kind IN ('event','snapshot')),source_sequence INTEGER NOT NULL CHECK(source_sequence>=0),model_hash TEXT NOT NULL,identity_fingerprint TEXT NOT NULL,payload_fingerprint TEXT NOT NULL,attempts INTEGER NOT NULL CHECK(typeof(attempts)='integer' AND attempts>0),failures INTEGER NOT NULL CHECK(typeof(failures)='integer' AND failures>=0),rejections INTEGER NOT NULL CHECK(typeof(rejections)='integer' AND rejections>=0),dead_letter INTEGER NOT NULL CHECK(dead_letter IN (0,1)),last_outcome TEXT NOT NULL CHECK(last_outcome IN ('STARTED','ACKNOWLEDGED','FAILED')),last_reason TEXT NOT NULL,dead_reason TEXT NOT NULL,PRIMARY KEY(source_runtime_id,target_id,kind,source_sequence))");
    }
    Attempt begin(String kind,long sequence,String identity,String payload){
        if(!Set.of("event","snapshot").contains(kind)||sequence<0||identity==null||payload==null||!payload.matches("[0-9a-f]{64}"))throw unavailable();
        return transaction(()->{
            var rows=sql.queryForList("SELECT * FROM teaql_event_delivery_journal WHERE source_runtime_id=? AND target_id=? AND kind=? AND source_sequence=?",new Object[]{source,target,kind,sequence});
            long ordinal=1;
            if(rows.isEmpty())sql.update("INSERT INTO teaql_event_delivery_journal VALUES(?,?,?,?,?,?,?,1,0,0,0,'STARTED','NONE','NONE')",new Object[]{source,target,kind,sequence,model,fingerprint(identity),payload});
            else{
                var row=rows.get(0);long prior=number(row.get("attempts"));
                if(!model.equals(row.get("model_hash"))||!fingerprint(identity).equals(row.get("identity_fingerprint"))||!payload.equals(row.get("payload_fingerprint"))||"ACKNOWLEDGED".equals(row.get("last_outcome")))throw conflict();
                ordinal=Math.addExact(prior,1);
                if(sql.update("UPDATE teaql_event_delivery_journal SET attempts=?,last_outcome='STARTED',last_reason='NONE' WHERE source_runtime_id=? AND target_id=? AND kind=? AND source_sequence=? AND attempts=?",new Object[]{ordinal,source,target,kind,sequence,prior})!=1)throw conflict();
            }
            return new Attempt(kind,sequence,ordinal);
        });
    }
    void failed(Attempt attempt,RuntimeException error){
        String code=error instanceof SqliteCommittedReceiver.ReceiverException r?r.code():error instanceof SqliteCommittedEventStore.CommittedEventException e?e.code():"TRANSPORT_UNAVAILABLE";
        String normalized=code.startsWith("SNAPSHOT_")&&PERMANENT.contains(code.substring(9))?code.substring(9):code;
        boolean permanent=PERMANENT.contains(normalized);
        String safe=permanent?normalized:Set.of("ACK_MISMATCH","AUDIT_UNAVAILABLE","CURSOR_CONFLICT").contains(code)?code:"TRANSPORT_UNAVAILABLE";
        transaction(()->{if(sql.update("UPDATE teaql_event_delivery_journal SET failures=failures+1,rejections=rejections+?,dead_letter=CASE WHEN ?=1 THEN 1 ELSE dead_letter END,dead_reason=CASE WHEN ?=1 THEN ? ELSE dead_reason END,last_outcome='FAILED',last_reason=? WHERE source_runtime_id=? AND target_id=? AND kind=? AND source_sequence=? AND attempts=? AND last_outcome='STARTED'",new Object[]{permanent?1:0,permanent?1:0,permanent?1:0,safe,safe,source,target,attempt.kind(),attempt.sequence(),attempt.ordinal()})!=1)throw conflict();return null;});
    }
    <T>T acknowledged(Attempt attempt,Supplier<T> cursorUpdate){
        return transaction(()->{
            T value=cursorUpdate.get();
            if(sql.update("UPDATE teaql_event_delivery_journal SET last_outcome='ACKNOWLEDGED',last_reason='NONE',dead_letter=0,dead_reason='NONE' WHERE source_runtime_id=? AND target_id=? AND kind=? AND source_sequence=? AND attempts=? AND last_outcome='STARTED'",new Object[]{source,target,attempt.kind(),attempt.sequence(),attempt.ordinal()})!=1)throw conflict();
            return value;
        });
    }
    public SourceObservation source(UserContext context){
        require(context);
        SourceObservation value=transaction(()->{
            if(count("SELECT count(*) n FROM teaql_event_delivery_journal WHERE source_runtime_id=? AND target_id=? AND model_hash<>?",new Object[]{source,target,model})!=0)throw unavailable();
            var cursor=sql.queryForList("SELECT acked_sequence,model_hash FROM teaql_event_delivery_cursor WHERE source_runtime_id=? AND target_id=?",new Object[]{source,target});
            long ack=cursor.isEmpty()?0:number(cursor.get(0).get("acked_sequence"));
            if(!cursor.isEmpty()&&!model.equals(cursor.get(0).get("model_hash")))throw unavailable();
            var highRows=sql.queryForList("SELECT current_sequence FROM teaql_event_sequence WHERE source_runtime_id=?",new Object[]{source});long high=highRows.isEmpty()?0:number(highRows.get(0).get("current_sequence"));
            if(high<ack)throw unavailable();
            var sums=sql.queryForList("SELECT COALESCE(SUM(attempts),0) attempts,COALESCE(SUM(attempts-1),0) retries,COALESCE(SUM(failures),0) failures,COALESCE(SUM(rejections),0) rejections,COALESCE(SUM(dead_letter),0) dead,COALESCE(SUM(CASE WHEN last_outcome='STARTED' THEN 1 ELSE 0 END),0) unresolved FROM teaql_event_delivery_journal WHERE source_runtime_id=? AND target_id=? AND model_hash=?",new Object[]{source,target,model}).get(0);
            long backlog=count("SELECT count(*) n FROM teaql_event_outbox WHERE source_runtime_id=? AND source_sequence>?",new Object[]{source,ack});
            long pending=count("SELECT count(*) n FROM teaql_event_snapshot_publication WHERE source_runtime_id=? AND target_id=? AND model_hash=? AND acknowledged=0",new Object[]{source,target,model});
            return new SourceObservation(high,ack,high-ack,backlog,pending,number(sums.get("attempts")),number(sums.get("retries")),number(sums.get("failures")),number(sums.get("rejections")),number(sums.get("dead")),number(sums.get("unresolved")));
        });
        emit(context,"source",Map.of("source_watermark",value.sourceHighWatermark(),"source_ack",value.ackedSequence(),"sequence_lag",value.sequenceLag(),"backlog",value.backlog(),"pending_snapshots",value.pendingSnapshots(),"attempts",value.attempts(),"retries",value.retries(),"failures",value.failures(),"rejections",value.rejections(),"dead_letters",value.deadLetters()),Map.of("unresolved_attempts",value.unresolvedAttempts()));
        return value;
    }
    public TargetObservation target(UserContext context,OptionalLong observedSourceHigh){
        require(context);Objects.requireNonNull(observedSourceHigh,"observedSourceHigh");
        TargetObservation value=transaction(()->{
            var rows=sql.queryForList("SELECT source_sequence,model_hash FROM teaql_event_checkpoint WHERE source_runtime_id=?",new Object[]{source});
            long checkpoint=rows.isEmpty()?0:number(rows.get(0).get("source_sequence"));
            if(!rows.isEmpty()&&!model.equals(rows.get(0).get("model_hash")))throw unavailable();
            if(observedSourceHigh.isPresent()&&observedSourceHigh.getAsLong()<checkpoint)throw unavailable();
            var baseline=sql.queryForList("SELECT source_sequence,model_hash FROM teaql_event_snapshot WHERE source_runtime_id=?",new Object[]{source});
            if(!baseline.isEmpty()&&!model.equals(baseline.get(0).get("model_hash")))throw unavailable();
            long n=baseline.isEmpty()?0:number(baseline.get(0).get("source_sequence"));if(n>checkpoint)throw unavailable();
            return new TargetObservation(checkpoint,n,count("SELECT count(*) n FROM teaql_event_inbox WHERE source_runtime_id=?",new Object[]{source}),observedSourceHigh.isPresent()?OptionalLong.of(observedSourceHigh.getAsLong()-checkpoint):OptionalLong.empty(),count("SELECT count(*) n FROM teaql_event_rejection WHERE target_fingerprint=?",new Object[]{fingerprint(target)}));
        });
        var attrs=new LinkedHashMap<String,Object>();attrs.put("target_watermark",value.checkpoint());attrs.put("snapshot_baseline",value.snapshotBaseline());attrs.put("inbox_rows",value.inboxRows());attrs.put("rejections",value.rejections());if(value.sequenceLag().isPresent())attrs.put("sequence_lag",value.sequenceLag().getAsLong());emit(context,"target",attrs,Map.of());return value;
    }
    public List<DeadLetter> deadLetters(UserContext context,int limit){
        require(context);if(limit<1||limit>1000)throw unavailable();
        return transaction(()->sql.queryForList("SELECT kind,source_sequence,identity_fingerprint,payload_fingerprint,attempts,dead_reason FROM teaql_event_delivery_journal WHERE source_runtime_id=? AND target_id=? AND model_hash=? AND dead_letter=1 ORDER BY source_sequence,kind LIMIT ?",new Object[]{source,target,model,limit}).stream().map(r->new DeadLetter((String)r.get("kind"),number(r.get("source_sequence")),(String)r.get("identity_fingerprint"),(String)r.get("payload_fingerprint"),number(r.get("attempts")),(String)r.get("dead_reason"))).toList());
    }
    private void emit(UserContext context,String name,Map<String,?> values,Map<String,?> extra){
        var attributes=new LinkedHashMap<String,Object>();attributes.put("teaql.replication.source_fingerprint",fingerprint(source));attributes.put("teaql.replication.target_fingerprint",fingerprint(target));values.forEach((k,v)->attributes.put("teaql.replication."+k,v));extra.forEach((k,v)->attributes.put("teaql.replication."+k,v));
        RuntimeTelemetry.startSafely(context.capability(RuntimeTelemetry.class),new RuntimeTelemetry.Operation("replication",name,attributes)).success();
    }
    private long count(String query,Object[] args){return number(sql.queryForList(query,args).get(0).get("n"));}
    private void require(UserContext context){Objects.requireNonNull(context,"context");EntityMetaFactory.requireFrom(context);if(sql.hasActiveTransaction())throw unavailable();}
    private <T>T transaction(Supplier<T> action){
        if(sql.hasActiveTransaction())throw unavailable();
        var value=new AtomicReference<T>();sql.executeInTransaction(()->{if(!sql.hasActiveTransaction())throw unavailable();value.set(action.get());});return value.get();
    }
    private static long number(Object value){if(!(value instanceof Number n)||n instanceof Double||n instanceof Float||n.longValue()<0)throw unavailable();return n.longValue();}
    static String fingerprint(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static SqliteCommittedEventStore.CommittedEventException unavailable(){return new SqliteCommittedEventStore.CommittedEventException("OBSERVATION_UNAVAILABLE","committed delivery observation is unavailable");}
    private static SqliteCommittedEventStore.CommittedEventException conflict(){return new SqliteCommittedEventStore.CommittedEventException("CURSOR_CONFLICT","committed delivery attempt changed concurrently");}
}
