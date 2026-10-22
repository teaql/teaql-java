package io.teaql.sqlite;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.core.sqlite.SqliteCommittedReceiver;
import io.teaql.core.sqlite.SqliteCommittedReceiver.Delivery;
import io.teaql.core.sqlite.SqliteCommittedReceiver.SnapshotDelivery;
import io.teaql.core.sqlite.SqliteCommittedReceiver.ReceiverException;
import io.teaql.core.sqlite.SqliteCommittedSnapshot.Limits;
import io.teaql.core.sqlite.SqliteCommittedSnapshot.SnapshotRecord;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import static org.junit.Assert.*;

public class SqliteCommittedReceiverSnapshotTest {
    private static final String SOURCE="snapshot-source",TARGET="snapshot-target",MODEL="sha256:"+"0".repeat(64);
    private static final byte[] KEY="0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final String CANARY="SNAPSHOT_SECRET_never_in_rejection_audit";
    private static String digest(String s)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
    private static SnapshotRecord snapshot(long n,String id)throws Exception{
        String body="{\"snapshot_version\":1,\"snapshot_id\":\""+id+"\",\"source_runtime_id\":\""+SOURCE+"\",\"source_sequence\":\""+n+"\",\"source_model_hash\":\""+MODEL+"\",\"audit\":{\"comment\":\"load baseline\",\"purpose\":\"verify authenticated recovery\"},\"entity_types\":[\"school\"],\"rows\":[{\"entity_type\":\"school\",\"entity_id\":\"7\",\"version\":\"1\",\"fields\":{\"name\":\""+CANARY+"\"}}]}";
        return new SnapshotRecord(SOURCE,n,id,MODEL,digest(body),body);
    }
    private static OutboxRecord tail(long n)throws Exception{
        String body="{\"protocol_version\":1,\"change_set_id\":\"tail\",\"source_runtime_id\":\""+SOURCE+"\",\"source_sequence\":\""+n+"\",\"source_model_hash\":\""+MODEL+"\",\"source_transaction_id\":\"tx\",\"committed_at\":\"2026-10-22T00:00:00Z\",\"audit\":{\"comment\":\"apply tail\",\"purpose\":\"verify recovery\"},\"mutations\":[{\"entity_type\":\"school\",\"entity_id\":\"7\",\"operation\":\"update\",\"base_version\":\"1\",\"committed_version\":\"2\",\"fields\":{\"name\":\"updated\"},\"trace_chain\":[]}]}";
        return new OutboxRecord(SOURCE,n,"tail",MODEL,digest(body),body);
    }
    private static SnapshotDelivery signed(SnapshotRecord s){return SqliteCommittedReceiver.signSnapshot("current",TARGET,KEY,s);}
    private static SnapshotDelivery replace(SnapshotDelivery d,SnapshotRecord s){return new SnapshotDelivery(s,d.keyId(),d.targetId(),d.signature());}
    private static final class Fixture implements AutoCloseable{
        final Path dir=Files.createTempDirectory("cmr13-snapshot-java-");final JdbcSqlExecutor sql;final DefaultUserContext context;final SqliteCommittedReceiver receiver;
        Fixture()throws Exception{
            SQLiteDataSource ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+dir.resolve("target.sqlite"));sql=new JdbcSqlExecutor(ds);
            EntityDescriptor e=new EntityDescriptor();e.setType("school");e.addSimpleProperty("id",Long.class);e.addSimpleProperty("version",Long.class);e.addSimpleProperty("name",String.class);
            SimpleEntityMetaFactory meta=new SimpleEntityMetaFactory();meta.register(e);context=new DefaultUserContext(TeaQLRuntime.builder().metadata(meta).build());
            sql.execute("CREATE TABLE school_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,name TEXT NOT NULL)");receiver=new SqliteCommittedReceiver(sql,SOURCE,TARGET,MODEL,Map.of("current",KEY));receiver.ensureSchema(context);
        }
        long count(String table){return ((Number)sql.queryForList("SELECT count(*) AS n FROM "+table,new Object[0]).get(0).get("n")).longValue();}
        void empty(long audits){for(String t:new String[]{"school_data","teaql_event_snapshot","teaql_event_checkpoint","teaql_event_inbox"})assertEquals(t,0,count(t));assertEquals(audits,count("teaql_event_rejection"));}
        void reject(String code,SnapshotDelivery d){try{receiver.receiveSnapshot(context,d);fail();}catch(ReceiverException e){assertEquals(code,e.code());assertNull(e.getCause());assertFalse(e.toString().contains(CANARY));}}
        @Override public void close()throws Exception{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    @Test public void zeroAndNonzeroSnapshotTailAndDuplicateNeverRewind()throws Exception{
        for(long n:new long[]{0,5})try(var f=new Fixture()){
            var d=signed(snapshot(n,"baseline"));var ack=f.receiver.receiveSnapshot(f.context,d);assertEquals(n,ack.sourceSequence());assertFalse(ack.duplicate());assertEquals(TARGET,ack.targetId());assertEquals(d.snapshot().payloadSha256(),ack.payloadSha256());
            f.receiver.receive(f.context,SqliteCommittedReceiver.sign("current",TARGET,KEY,tail(n+1)));var duplicate=f.receiver.receiveSnapshot(f.context,d);assertTrue(duplicate.duplicate());assertEquals(ack.sourceSequence(),duplicate.sourceSequence());
            assertEquals("updated",f.sql.queryForList("SELECT name FROM school_data",new Object[0]).get(0).get("name"));assertEquals(n+1,((Number)f.sql.queryForList("SELECT source_sequence FROM teaql_event_checkpoint",new Object[0]).get(0).get("source_sequence")).longValue());assertEquals(0,f.count("teaql_event_rejection"));
            System.out.println("CMR13_SNAPSHOT_SOURCE_PASS language=java baseline="+n+" tail="+(n+1)+" duplicate_no_rewind=1");
        }
    }
    @Test public void sameEightFieldsCannotCrossEventSnapshotSignatureDomains()throws Exception{
        try(var f=new Fixture()){
            var s=snapshot(1,"same");var e=new OutboxRecord(s.sourceRuntimeId(),1,s.snapshotId(),s.modelHash(),s.payloadSha256(),s.payloadJson());var eventTag=SqliteCommittedReceiver.sign("current",TARGET,KEY,e).signature();
            f.reject("INVALID_SIGNATURE",new SnapshotDelivery(s,"current",TARGET,eventTag));
            try{f.receiver.receive(f.context,new Delivery(e,"current",TARGET,signed(s).signature()));fail();}catch(ReceiverException error){assertEquals("INVALID_SIGNATURE",error.code());}
            f.empty(2);
        }
    }
    @Test public void snapshotTamperRoutingAndEveryIdentityFieldReject()throws Exception{
        try(var f=new Fixture()){
            var s=snapshot(0,"baseline");var d=signed(s);String altered=s.payloadJson().replace(CANARY,"attacker");
            for(String hash:new String[]{s.payloadSha256(),digest(altered)})f.reject("INVALID_SIGNATURE",replace(d,new SnapshotRecord(SOURCE,0,s.snapshotId(),MODEL,hash,altered)));
            f.reject("INVALID_SIGNATURE",replace(d,new SnapshotRecord(SOURCE,1,s.snapshotId(),MODEL,s.payloadSha256(),s.payloadJson())));
            f.reject("INVALID_SIGNATURE",replace(d,new SnapshotRecord(SOURCE,0,"other",MODEL,s.payloadSha256(),s.payloadJson())));
            f.reject("UNAUTHORIZED_SOURCE",replace(d,new SnapshotRecord("other",0,s.snapshotId(),MODEL,s.payloadSha256(),s.payloadJson())));
            f.reject("UNAUTHORIZED_TARGET",new SnapshotDelivery(s,"current","other",d.signature()));f.reject("UNKNOWN_KEY",new SnapshotDelivery(s,"retired",TARGET,d.signature()));
            f.reject("MODEL_MISMATCH",replace(d,new SnapshotRecord(SOURCE,0,s.snapshotId(),"sha256:"+"1".repeat(64),s.payloadSha256(),s.payloadJson())));f.empty(8);
        }
    }
    @Test public void authenticatedMalformedSnapshotAndConflictingBaselineAreAudited()throws Exception{
        try(var f=new Fixture()){
            var s=snapshot(0,"baseline");String invalid=s.payloadJson().replace("\"school\"","\"unknown\"");f.reject("INVALID_SNAPSHOT",signed(new SnapshotRecord(SOURCE,0,s.snapshotId(),MODEL,digest(invalid),invalid)));f.empty(1);
            f.receiver.receiveSnapshot(f.context,signed(s));f.reject("SNAPSHOT_CONFLICT",signed(snapshot(1,"different")));assertEquals(1,f.count("school_data"));assertEquals(2,f.count("teaql_event_rejection"));
            assertEquals(CANARY,f.sql.queryForList("SELECT name FROM school_data",new Object[0]).get(0).get("name"));
        }
    }
    @Test public void snapshotAuditReopensAndAuditFailureNeverAcknowledges()throws Exception{
        try(var f=new Fixture()){
            var d=new SnapshotDelivery(snapshot(0,CANARY),"current",TARGET,new byte[32]);f.reject("INVALID_SIGNATURE",d);f.empty(1);
            SQLiteDataSource ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+f.dir.resolve("target.sqlite"));var rows=new JdbcSqlExecutor(ds).queryForList("SELECT * FROM teaql_event_rejection",new Object[0]);assertEquals(1,rows.size());assertFalse(rows.toString().contains(CANARY));assertFalse(rows.toString().contains(new String(KEY,StandardCharsets.UTF_8)));
            f.sql.execute("CREATE TRIGGER reject_audit BEFORE INSERT ON teaql_event_rejection BEGIN SELECT RAISE(ABORT,'"+CANARY+"'); END");f.reject("AUDIT_UNAVAILABLE",d);f.empty(1);
        }
    }
    @Test public void receiverOwnedLimitsAndAmbientTransactionsFailClosed()throws Exception{
        try(var f=new Fixture()){
            var d=signed(snapshot(0,"baseline"));var strict=new SqliteCommittedReceiver(f.sql,SOURCE,TARGET,MODEL,Map.of("current",KEY),new Limits(1,16));
            try{strict.receiveSnapshot(f.context,d);fail();}catch(ReceiverException e){assertEquals("INVALID_ENVELOPE",e.code());}f.empty(1);
            f.reject("INVALID_ENVELOPE",replace(d,new SnapshotRecord(SOURCE,-1,"baseline",MODEL,d.snapshot().payloadSha256(),d.snapshot().payloadJson())));
            f.reject("INVALID_ENVELOPE",new SnapshotDelivery(d.snapshot(),"current",TARGET,new byte[0]));f.empty(3);
            f.sql.executeInTransaction(()->f.reject("AMBIENT_TRANSACTION",d));f.empty(3);
        }
    }
    @Test public void knownKeyAliasDestinationAndLengthFramingAreBound()throws Exception{
        try(var f=new Fixture()){
            var s=snapshot(0,"baseline");var d=signed(s);var aliases=new SqliteCommittedReceiver(f.sql,SOURCE,TARGET,MODEL,Map.of("current",KEY,"alias",KEY));
            try{aliases.receiveSnapshot(f.context,new SnapshotDelivery(s,"alias",TARGET,d.signature()));fail();}catch(ReceiverException e){assertEquals("INVALID_SIGNATURE",e.code());}
            var destination=new SqliteCommittedReceiver(f.sql,SOURCE,"other",MODEL,Map.of("current",KEY));try{destination.receiveSnapshot(f.context,new SnapshotDelivery(s,"current","other",d.signature()));fail();}catch(ReceiverException e){assertEquals("INVALID_SIGNATURE",e.code());}
            assertFalse(Arrays.equals(SqliteCommittedReceiver.signSnapshot("a","bc",KEY,s).signature(),SqliteCommittedReceiver.signSnapshot("ab","c",KEY,s).signature()));f.empty(2);
        }
    }
    @Test public void independentSnapshotGoldenAndDefensiveTagCopies()throws Exception{
        var s=new SnapshotRecord("source",0,"baseline",MODEL,"a".repeat(64),"{\"x\":\"雪\"}");var d=SqliteCommittedReceiver.signSnapshot("key","target",KEY,s);
        assertEquals("21dd4226dec5f6513b30b82b9bc4b8a340421a5663e4635658c54291226fe8d9",HexFormat.of().formatHex(d.signature()));byte[] first=d.signature();Arrays.fill(first,(byte)0);assertFalse(Arrays.equals(first,d.signature()));assertFalse(d.toString().contains(s.payloadJson()));
    }
}
