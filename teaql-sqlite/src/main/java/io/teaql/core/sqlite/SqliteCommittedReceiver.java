package io.teaql.core.sqlite;

import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Startup-owned authenticated ingress. Never a Query, Mutation or TFP route. */
public final class SqliteCommittedReceiver {
    private static final int MAX_PAYLOAD=1024*1024, MAX_ID=256;
    private static final Set<String> REJECTIONS=Set.of("INVALID_ENVELOPE","MODEL_MISMATCH",
            "SEQUENCE_GAP","DUPLICATE_CONFLICT","VERSION_CONFLICT","SEQUENCE_EXHAUSTED");
    private final SqlExecutionAdapter sql;
    private final String source,target,model;
    private final Map<String,byte[]> keys;
    private final SqliteModelCheckedCommittedTarget delegate;

    public record Delivery(OutboxRecord event,String keyId,String targetId,byte[] signature) {
        public Delivery {signature=signature==null?new byte[0]:signature.clone();}
        @Override public byte[] signature(){return signature.clone();}
        @Override public String toString(){return "CommittedDelivery[content=<redacted>]";}
    }
    public static final class ReceiverException extends RuntimeException {
        private final String code;
        private ReceiverException(String code){super("committed receiver: "+code);this.code=code;}
        public String code(){return code;}
    }
    public SqliteCommittedReceiver(SqlExecutionAdapter sql,String source,String target,
            String model,Map<String,byte[]> keys) {
        this.sql=Objects.requireNonNull(sql,"sql");
        try {
            field(source,MAX_ID);field(target,MAX_ID);
            if(model==null||!model.matches("sha256:[0-9a-f]{64}")||keys==null||keys.isEmpty()||keys.size()>16)throw rejected("INVALID_ENVELOPE");
            Map<String,byte[]> copy=new HashMap<>();
            for(var entry:keys.entrySet()){field(entry.getKey(),MAX_ID);key(entry.getValue());copy.put(entry.getKey(),entry.getValue().clone());}
            this.keys=Map.copyOf(copy);
        }catch(ReceiverException failure){throw new IllegalArgumentException("invalid committed receiver configuration");}
        this.source=source;this.target=target;this.model=model;
        this.delegate=new SqliteModelCheckedCommittedTarget(sql,source,model);
    }
    /** Explicit provider-infrastructure installation; no domain-row seeding. */
    public void ensureSchema(UserContext context) {
        EntityMetaFactory.requireFrom(Objects.requireNonNull(context,"context"));
        if(sql.hasActiveTransaction())throw rejected("AMBIENT_TRANSACTION");
        delegate.ensureSchema();
        sql.execute("CREATE TABLE IF NOT EXISTS teaql_event_rejection (audit_id INTEGER PRIMARY KEY, "
                +"reason_code TEXT NOT NULL, source_sequence INTEGER NOT NULL, source_fingerprint TEXT NOT NULL, "
                +"change_fingerprint TEXT NOT NULL,target_fingerprint TEXT NOT NULL,payload_fingerprint TEXT NOT NULL)");
    }
    /** Original model facts remain unchanged; authenticated duplicate ACKs stay stable. */
    public SqliteCommittedEventStore.InboxOutcome receive(UserContext context,Delivery delivery) {
        Objects.requireNonNull(context,"context");
        // Infrastructure wiring error: never join/commit a caller's transaction.
        if(sql.hasActiveTransaction())throw rejected("AMBIENT_TRANSACTION");
        try {
            validate(delivery);
            OutboxRecord event=delivery.event();
            if(!source.equals(event.sourceRuntimeId()))throw rejected("UNAUTHORIZED_SOURCE");
            if(!target.equals(delivery.targetId()))throw rejected("UNAUTHORIZED_TARGET");
            if(!model.equals(event.modelHash()))throw rejected("MODEL_MISMATCH");
            byte[] secret=keys.get(delivery.keyId());
            if(secret==null)throw rejected("UNKNOWN_KEY");
            if(!MessageDigest.isEqual(tag(delivery.keyId(),delivery.targetId(),secret,event),delivery.signature))throw rejected("INVALID_SIGNATURE");
            return delegate.apply(context,event);
        }catch(ReceiverException failure){
            audit(delivery,failure.code());throw failure;
        }catch(SqliteCommittedEventStore.CommittedEventException failure){
            // Unknown/failed transaction outcomes are not security rejections.
            if(!REJECTIONS.contains(failure.code()))throw failure;
            audit(delivery,failure.code());throw rejected(failure.code());
        }
    }
    /** Source-side signing of original bytes; requires a deployment-owned secret. */
    public static Delivery sign(String keyId,String target,byte[] secret,OutboxRecord event) {
        field(keyId,MAX_ID);field(target,MAX_ID);key(secret);record(event);
        return new Delivery(event,keyId,target,tag(keyId,target,secret,event));
    }
    private static void validate(Delivery delivery){
        if(delivery==null)throw rejected("INVALID_ENVELOPE");
        field(delivery.keyId(),MAX_ID);field(delivery.targetId(),MAX_ID);record(delivery.event());
        if(delivery.signature.length!=32)throw rejected("INVALID_ENVELOPE");
    }
    private static void record(OutboxRecord e){
        if(e==null||e.sourceSequence()<=0)throw rejected("INVALID_ENVELOPE");
        field(e.sourceRuntimeId(),MAX_ID);field(e.changeSetId(),MAX_ID);field(e.modelHash(),MAX_ID);field(e.payloadSha256(),MAX_ID);field(e.payloadJson(),MAX_PAYLOAD);
    }
    private static byte[] field(String text,int limit){
        if(text==null||text.isEmpty()||text.length()>limit||!StandardCharsets.UTF_8.newEncoder().canEncode(text))throw rejected("INVALID_ENVELOPE");
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);if(bytes.length>limit)throw rejected("INVALID_ENVELOPE");return bytes;
    }
    private static void key(byte[] secret){if(secret==null||secret.length<32||secret.length>128)throw rejected("INVALID_ENVELOPE");}
    private static byte[] tag(String keyId,String target,byte[] secret,OutboxRecord event){
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            mac.update("teaql.committed.receiver.v1\0".getBytes(StandardCharsets.UTF_8));
            String[] fields={keyId,target,event.sourceRuntimeId(),Long.toString(event.sourceSequence()),event.changeSetId(),event.modelHash(),event.payloadSha256(),event.payloadJson()};
            for(String value:fields){byte[] bytes=value.getBytes(StandardCharsets.UTF_8);mac.update(ByteBuffer.allocate(4).putInt(bytes.length).array());mac.update(bytes);}
            return mac.doFinal();
        }catch(GeneralSecurityException failure){throw new IllegalStateException("standard HMAC-SHA256 unavailable");}
    }
    private void audit(Delivery delivery,String code){
        try {
            if(sql.hasActiveTransaction())throw new IllegalStateException("audit transaction unavailable");
            OutboxRecord e=delivery==null?null:delivery.event();
            Object[] values={code,e==null||e.sourceSequence()<1?0:e.sourceSequence(),fingerprint(e==null?null:e.sourceRuntimeId(),MAX_ID),fingerprint(e==null?null:e.changeSetId(),MAX_ID),fingerprint(target,MAX_ID),fingerprint(e==null?null:e.payloadJson(),MAX_PAYLOAD)};
            sql.executeInTransaction(()->{if(sql.update("INSERT INTO teaql_event_rejection(reason_code,source_sequence,source_fingerprint,change_fingerprint,target_fingerprint,payload_fingerprint) VALUES (?,?,?,?,?,?)",values)!=1)throw new IllegalStateException("audit row missing");});
        }catch(RuntimeException failure){throw rejected("AUDIT_UNAVAILABLE");}
    }
    private static String fingerprint(String text,int limit){
        if(text==null)return "missing";
        byte[] bytes;try{bytes=field(text,limit);}catch(ReceiverException invalid){return "invalid_or_oversize";}
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(GeneralSecurityException impossible){throw new IllegalStateException("standard SHA-256 unavailable");}
    }
    private static ReceiverException rejected(String code){return new ReceiverException(code);}
    @Override public String toString(){return "SqliteCommittedReceiver[authority=<redacted>]";}
}
