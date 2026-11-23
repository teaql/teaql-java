package io.teaql.sqlite;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.core.sqlite.SqliteCommittedReceiver;
import io.teaql.core.sqlite.SqliteCommittedReceiver.Delivery;
import io.teaql.core.sqlite.SqliteCommittedReceiver.ReceiverException;
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

/** Public receiver APIs only: reusable by isolated installed consumers. */
public class SqliteCommittedReceiverTest {
    private static final String SOURCE="receiver-source", TARGET="receiver-target";
    private static final String MODEL="sha256:"+"0".repeat(64);
    private static final byte[] KEY="0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final String CANARY="BUSINESS_SECRET_must_not_appear_in_audit";

    private static String digest(String text) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    private static OutboxRecord event(long sequence,String change,String source,boolean update) throws Exception {
        String body="{\"protocol_version\":1,\"change_set_id\":\""+change+"\",\"source_runtime_id\":\""+source+"\",\"source_sequence\":\""+sequence+"\",\"source_model_hash\":\""+MODEL+"\",\"source_transaction_id\":\"tx\",\"committed_at\":\"2026-10-22T00:00:00Z\",\"audit\":{\"comment\":\"receive school\",\"purpose\":\"verify signed delivery\"},\"mutations\":[{\"entity_type\":\"school\",\"entity_id\":\"7\",\"operation\":\""+(update?"update":"create")+"\",\"base_version\":"+(update?"\"1\"":"null")+",\"committed_version\":\""+(update?"2":"1")+"\",\"fields\":{\"name\":\""+(update?"updated":CANARY)+"\"},\"trace_chain\":[]}]}";
        return new OutboxRecord(source,sequence,change,MODEL,digest(body),body);
    }
    private static Delivery sign(OutboxRecord event){return SqliteCommittedReceiver.sign("current",TARGET,KEY,event);}
    private static Delivery replace(Delivery original,OutboxRecord event){return new Delivery(event,original.keyId(),original.targetId(),original.signature());}
    private static final class Fixture implements AutoCloseable {
        final Path dir=Files.createTempDirectory("cmr13-java-");
        final JdbcSqlExecutor sql; final DefaultUserContext context; final SqliteCommittedReceiver receiver;
        Fixture() throws Exception {
            SQLiteDataSource ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+dir.resolve("target.sqlite"));sql=new JdbcSqlExecutor(ds);
            EntityDescriptor school=new EntityDescriptor();school.setType("school");school.addSimpleProperty("id",Long.class);school.addSimpleProperty("version",Long.class);school.addSimpleProperty("name",String.class);
            SimpleEntityMetaFactory metadata=new SimpleEntityMetaFactory();metadata.register(school);context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
            sql.execute("CREATE TABLE school_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,name TEXT NOT NULL)");
            receiver=new SqliteCommittedReceiver(sql,SOURCE,TARGET,MODEL,Map.of("current",KEY));receiver.ensureSchema(context);
        }
        long count(String table){return ((Number)sql.queryForList("SELECT count(*) AS n FROM "+table,new Object[0]).get(0).get("n")).longValue();}
        void unchanged(long rows,long checkpoint,long audits){assertEquals(rows,count("school_data"));assertEquals(checkpoint,count("teaql_event_inbox"));assertEquals(audits,count("teaql_event_rejection"));}
        void rejected(String code,Delivery delivery) {try{receiver.receive(context,delivery);fail("receiver must reject");}catch(ReceiverException error){assertEquals(code,error.code());assertFalse(error.toString().contains(CANARY));assertNull(error.getCause());}}
        @Override public void close()throws Exception{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(p);}}
    }

    @Test public void validSignedApplyUpdateAndRetainedDuplicate()throws Exception{
        try(var f=new Fixture()){
            var a=sign(event(1,"create",SOURCE,false));var b=sign(event(2,"update",SOURCE,true));
            assertFalse(f.receiver.receive(f.context,a).duplicate());assertFalse(f.receiver.receive(f.context,b).duplicate());assertTrue(f.receiver.receive(f.context,a).duplicate());assertTrue(f.receiver.receive(f.context,b).duplicate());f.unchanged(1,2,0);
            assertEquals("updated",f.sql.queryForList("SELECT name FROM school_data",new Object[0]).get(0).get("name"));
            String origin=SqliteCommittedReceiver.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            if(System.getProperty("cmr13.source.root")!=null)assertTrue(origin,origin.startsWith(System.getProperty("cmr13.source.root")+"/teaql-sqlite/target/classes"));
            System.out.println("CMR13_SOURCE_PASS language=java origin="+origin+" valid=2 duplicates=2");
        }
    }
    @Test public void tamperingEvenWithRecomputedChecksumRejectsWithoutWrites()throws Exception{
        try(var f=new Fixture()){
            var record=event(1,"a",SOURCE,false);var signed=sign(record);String changed=record.payloadJson().replace(CANARY,"attacker-value");
            for(String hash:new String[]{record.payloadSha256(),digest(changed)})f.rejected("INVALID_SIGNATURE",replace(signed,new OutboxRecord(SOURCE,1,"a",MODEL,hash,changed)));
            f.unchanged(0,0,2);
        }
    }
    @Test public void routingAndKeyChangesRejectAndAudit()throws Exception{
        try(var f=new Fixture()){
            var e=event(1,"a",SOURCE,false);var d=sign(e);
            f.rejected("UNAUTHORIZED_SOURCE",sign(event(1,"a","other-source",false)));
            f.rejected("UNAUTHORIZED_TARGET",new Delivery(e,d.keyId(),"other-target",d.signature()));
            f.rejected("UNKNOWN_KEY",new Delivery(e,"retired",TARGET,d.signature()));
            f.rejected("INVALID_SIGNATURE",new Delivery(e,d.keyId(),TARGET,new byte[32]));
            f.rejected("INVALID_ENVELOPE",new Delivery(e,d.keyId(),TARGET,new byte[0]));
            f.rejected("INVALID_SIGNATURE",replace(d,new OutboxRecord(SOURCE,2,"a",MODEL,e.payloadSha256(),e.payloadJson())));
            f.rejected("INVALID_SIGNATURE",replace(d,new OutboxRecord(SOURCE,1,"b",MODEL,e.payloadSha256(),e.payloadJson())));
            f.rejected("MODEL_MISMATCH",replace(d,new OutboxRecord(SOURCE,1,"a","sha256:"+"1".repeat(64),e.payloadSha256(),e.payloadJson())));
            f.unchanged(0,0,8);
        }
    }
    @Test public void signedOutOfStreamReplayAndConflictAreAudited()throws Exception{
        try(var f=new Fixture()){
            var e=event(1,"a",SOURCE,false);f.receiver.receive(f.context,sign(e));f.receiver.receive(f.context,sign(event(2,"b",SOURCE,true)));
            f.rejected("SEQUENCE_GAP",sign(event(1,"old-unseen",SOURCE,false)));
            f.rejected("SEQUENCE_GAP",sign(event(4,"gap",SOURCE,true)));
            String body=e.payloadJson().replace(CANARY,"changed");f.rejected("DUPLICATE_CONFLICT",sign(new OutboxRecord(SOURCE,1,"a",MODEL,digest(body),body)));
            f.unchanged(1,2,3);
            assertEquals(2L,((Number)f.sql.queryForList("SELECT source_sequence FROM teaql_event_checkpoint",new Object[0]).get(0).get("source_sequence")).longValue());
        }
    }
    @Test public void auditIsDurableHashOnlyAndUnavailableIsDistinct()throws Exception{
        try(var f=new Fixture()){
            var e=event(1,CANARY,SOURCE,false);f.rejected("INVALID_SIGNATURE",new Delivery(e,"current",TARGET,new byte[32]));
            SQLiteDataSource ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+f.dir.resolve("target.sqlite"));var reopened=new JdbcSqlExecutor(ds);
            var audit=reopened.queryForList("SELECT * FROM teaql_event_rejection",new Object[0]);assertEquals(1,audit.size());assertFalse(audit.toString().contains(CANARY));assertFalse(audit.toString().contains(new String(KEY,StandardCharsets.UTF_8)));assertEquals("INVALID_SIGNATURE",audit.get(0).get("reason_code"));
            f.sql.execute("CREATE TRIGGER reject_audit BEFORE INSERT ON teaql_event_rejection BEGIN SELECT RAISE(ABORT,'"+CANARY+"'); END");
            f.rejected("AUDIT_UNAVAILABLE",new Delivery(e,"current",TARGET,new byte[32]));f.unchanged(0,0,1);
        }
    }
    @Test public void immutableKeyAndTagCopiesAndRotation()throws Exception{
        try(var f=new Fixture()){
            byte[] original=KEY.clone();var receiver=new SqliteCommittedReceiver(f.sql,SOURCE,TARGET,MODEL,Map.of("current",original,"previous",KEY));Arrays.fill(original,(byte)0);
            var d=sign(event(1,"a",SOURCE,false));Arrays.fill(d.signature(),(byte)0);assertFalse(receiver.receive(f.context,d).duplicate());
            var old=SqliteCommittedReceiver.sign("previous",TARGET,KEY,event(2,"b",SOURCE,true));assertFalse(receiver.receive(f.context,old).duplicate());
            f.rejected("UNKNOWN_KEY",old);f.unchanged(1,2,1);assertFalse(d.toString().contains(CANARY));assertFalse(receiver.toString().contains(new String(KEY,StandardCharsets.UTF_8)));
        }
    }
    @Test public void limitsAndAmbientTransactionFailClosed()throws Exception{
        try(var f=new Fixture()){
            var e=event(1,"a",SOURCE,false);var d=sign(e);
            f.rejected("INVALID_ENVELOPE",replace(d,new OutboxRecord(SOURCE,0,"a",MODEL,e.payloadSha256(),e.payloadJson())));
            f.rejected("INVALID_ENVELOPE",replace(d,new OutboxRecord(SOURCE,1,"a",MODEL,e.payloadSha256(),"x".repeat(1024*1024+1))));
            f.rejected("INVALID_ENVELOPE",new Delivery(e,"x".repeat(257),TARGET,d.signature()));f.unchanged(0,0,3);
            f.sql.executeInTransaction(()->{f.rejected("AMBIENT_TRANSACTION",d);assertEquals(0,f.count("school_data"));});f.unchanged(0,0,3);
            try{new SqliteCommittedReceiver(f.sql,SOURCE,TARGET,MODEL,Map.of("weak",new byte[8]));fail();}catch(IllegalArgumentException expected){assertFalse(expected.toString().contains(CANARY));}
        }
    }
    @Test public void goldenTranscriptHasCrossLanguageStableTag()throws Exception{
        var e=new OutboxRecord("source",42,"change",MODEL,"a".repeat(64),"{\"x\":\"雪\"}");
        var signed=SqliteCommittedReceiver.sign("key","target",KEY,e);
        // Independent Python stdlib HMAC fixture; neither runtime supplies this oracle.
        assertEquals("f1405877cd5999425693e559a147aa27294240456f88e307208b7162ff0bd8ac",HexFormat.of().formatHex(signed.signature()));
    }
}
