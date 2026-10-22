package io.teaql.sqlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore.OutboxRecord;
import io.teaql.core.sqlite.SqliteCommittedReceiver;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Small signed protocol fixture, not a high-level business save/example. */
public class SqliteCommittedBoundaryTest {
    static final ObjectMapper JSON=new ObjectMapper();
    static final String SOURCE="boundary-source",TARGET="boundary-target",MODEL="sha256:"+"0".repeat(64);
    static final String SQL="INSERT INTO school_data(id,version,name) VALUES(99,1,'SQL-CANARY')";
    static final byte[] KEY="0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    static ObjectNode body()throws Exception{
        return (ObjectNode)JSON.readTree("{\"protocol_version\":1,\"change_set_id\":\"fact\",\"source_runtime_id\":\"boundary-source\",\"source_sequence\":\"1\",\"source_model_hash\":\""+MODEL+"\",\"source_transaction_id\":\"tx\",\"committed_at\":\"2026-10-22T00:00:00Z\",\"audit\":{\"comment\":\"receive school\",\"purpose\":\"check protocol boundary\"},\"mutations\":[{\"entity_type\":\"school\",\"entity_id\":\"7\",\"operation\":\"create\",\"base_version\":null,\"committed_version\":\"1\",\"fields\":{\"name\":\"original\"},\"trace_chain\":[]}]}");
    }
    static ObjectNode snapshotBody()throws Exception{
        return (ObjectNode)JSON.readTree("{\"snapshot_version\":1,\"snapshot_id\":\"baseline\",\"source_runtime_id\":\"boundary-source\",\"source_sequence\":\"0\",\"source_model_hash\":\""+MODEL+"\",\"audit\":{\"comment\":\"receive baseline\",\"purpose\":\"check snapshot boundary\"},\"entity_types\":[\"school\"],\"rows\":[{\"entity_type\":\"school\",\"entity_id\":\"7\",\"version\":\"1\",\"fields\":{\"name\":\"original\"}}]}");
    }
    static final class Fixture implements AutoCloseable{
        final Path owner=Files.createTempDirectory("cmr15-java-");final JdbcSqlExecutor sql;final DefaultUserContext context;final SqliteCommittedReceiver receiver;
        Fixture()throws Exception{
            var ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+owner.resolve("target.sqlite"));sql=new JdbcSqlExecutor(ds);
            var school=new EntityDescriptor();school.setType("school");school.addSimpleProperty("id",Long.class);school.addSimpleProperty("version",Long.class);school.addSimpleProperty("name",String.class);
            var metadata=new SimpleEntityMetaFactory();metadata.register(school);context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).executionLogging(false).build());
            sql.execute("CREATE TABLE school_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,name TEXT NOT NULL)");
            receiver=new SqliteCommittedReceiver(sql,SOURCE,TARGET,MODEL,Map.of("current",KEY));receiver.ensureSchema(context);
        }
        long count(String table){return ((Number)sql.queryForList("SELECT count(*) n FROM "+table,new Object[0]).get(0).get("n")).longValue();}
        io.teaql.core.sqlite.SqliteCommittedEventStore.InboxOutcome receive(ObjectNode body)throws Exception{
            String text=JSON.writeValueAsString(body),hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            var event=new OutboxRecord(SOURCE,1,"fact",MODEL,hash,text);
            return receiver.receive(context,SqliteCommittedReceiver.sign("current",TARGET,KEY,event));
        }
        void reject(ObjectNode body)throws Exception{
            try{receive(body);fail("noncanonical controls must be rejected");}catch(SqliteCommittedReceiver.ReceiverException error){assertEquals("INVALID_ENVELOPE",error.code());assertFalse(error.toString().contains(SQL));}
            assertEquals(0,count("school_data"));assertEquals(0,count("teaql_event_inbox"));assertEquals(0,count("teaql_event_checkpoint"));assertEquals(1,count("teaql_event_rejection"));
            assertFalse(receive(body()).duplicate());assertEquals(1,count("school_data"));assertEquals(1,count("teaql_event_inbox"));
        }
        void receiveSnapshot(ObjectNode body)throws Exception{
            String text=JSON.writeValueAsString(body),hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            var record=new io.teaql.core.sqlite.SqliteCommittedSnapshot.SnapshotRecord(SOURCE,0,"baseline",MODEL,hash,text);
            receiver.receiveSnapshot(context,SqliteCommittedReceiver.signSnapshot("current",TARGET,KEY,record));
        }
        void rejectSnapshot(ObjectNode body)throws Exception{
            try{receiveSnapshot(body);fail("noncanonical snapshot controls must be rejected");}catch(SqliteCommittedReceiver.ReceiverException error){assertEquals("INVALID_SNAPSHOT",error.code());assertFalse(error.toString().contains(SQL));}
            for(String table:List.of("school_data","teaql_event_snapshot","teaql_event_inbox","teaql_event_checkpoint"))assertEquals(table,0,count(table));
            assertEquals(1,count("teaql_event_rejection"));receiveSnapshot(snapshotBody());assertEquals(1,count("school_data"));assertEquals(1,count("teaql_event_snapshot"));
        }
        @Override public void close()throws Exception{try(var all=Files.walk(owner)){for(var p:all.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    @Test public void envelopeControlsCannotSmuggleSql()throws Exception{
        for(String key:List.of("sql","rawSql","raw_sql","statement","statements","provider","mutation_plan","replace"))try(var f=new Fixture()){var b=body();b.put(key,SQL);f.reject(b);}
        System.out.println("CMR15_CONTROL_FRAMES_PASS language=java scope=envelope rejected=8");
    }
    @Test public void mutationControlsCannotSmuggleSql()throws Exception{
        for(String key:List.of("sql","rawSql","raw_sql","statement","table","provider","physical_table","command"))try(var f=new Fixture()){var b=body();((ObjectNode)b.path("mutations").get(0)).put(key,SQL);f.reject(b);}
    }
    @Test public void auditControlsCannotSmuggleSql()throws Exception{
        for(String key:List.of("sql","rawSql","statement","provider","class","command","tenantId"))try(var f=new Fixture()){var b=body();((ObjectNode)b.path("audit")).put(key,SQL);f.reject(b);}
    }
    @Test public void traceControlsStayStrict()throws Exception{
        try(var f=new Fixture()){var b=body();((com.fasterxml.jackson.databind.node.ArrayNode)b.path("mutations").get(0).path("trace_chain")).addObject().put("kind","SQL").put("name","diagnostic").putNull("entity_id").put("comment","trace metadata").put("statement",SQL);f.reject(b);}
    }
    @Test public void sqlLookingBusinessAndAuditTextRemainBoundData()throws Exception{
        try(var f=new Fixture()){var b=body();((ObjectNode)b.path("mutations").get(0).path("fields")).put("name",SQL);((ObjectNode)b.path("audit")).put("comment",SQL).put("actor_ref","user:7").put("trace_id","trace:1");
            assertFalse(f.receive(b).duplicate());assertEquals(1,f.count("school_data"));assertEquals(SQL,f.sql.queryForList("SELECT name FROM school_data WHERE id=7",new Object[0]).get(0).get("name"));assertEquals(0,f.count("teaql_event_rejection"));
        }
    }
    @Test public void ordinaryRequestsAreNotCommittedEnvelopes()throws Exception{
        for(String input:List.of("{\"sql\":\"SELECT 1\"}","{\"entity\":\"school\",\"limitValue\":1,\"commentText\":\"read\",\"purposeText\":\"inspect\"}","{\"entity\":\"school\",\"action\":\"Create\",\"payload\":{\"name\":\"business\"},\"comment\":\"create school\"}"))try(var f=new Fixture()){f.reject((ObjectNode)JSON.readTree(input));}
    }
    @Test public void snapshotContainerControlsStayStrict()throws Exception{
        for(String key:List.of("sql","rawSql","statement","provider","replace"))try(var f=new Fixture()){var b=snapshotBody();b.put(key,SQL);f.rejectSnapshot(b);}
    }
    @Test public void snapshotRowControlsStayStrict()throws Exception{
        for(String key:List.of("sql","statement","table","command","operation"))try(var f=new Fixture()){var b=snapshotBody();((ObjectNode)b.path("rows").get(0)).put(key,SQL);f.rejectSnapshot(b);}
    }
    @Test public void snapshotAuditControlsCannotSmuggleSql()throws Exception{
        for(String key:List.of("sql","rawSql","statement","provider","class","command","tenantId"))try(var f=new Fixture()){var b=snapshotBody();((ObjectNode)b.path("audit")).put(key,SQL);f.rejectSnapshot(b);}
        System.out.println("CMR15_SNAPSHOT_CONTROLS_PASS language=java container=5 row=5 audit=7");
    }
    @Test public void snapshotSqlLookingBusinessAndAuditTextRemainBoundData()throws Exception{
        try(var f=new Fixture()){var b=snapshotBody();((ObjectNode)b.path("rows").get(0).path("fields")).put("name",SQL);((ObjectNode)b.path("audit")).put("comment",SQL).put("actor_ref","user:7").put("trace_id","trace:1");
            f.receiveSnapshot(b);assertEquals(1,f.count("school_data"));assertEquals(SQL,f.sql.queryForList("SELECT name FROM school_data WHERE id=7",new Object[0]).get(0).get("name"));assertEquals(0,f.count("teaql_event_rejection"));
        }
    }
}
