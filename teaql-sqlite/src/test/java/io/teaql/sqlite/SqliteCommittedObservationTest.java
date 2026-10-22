package io.teaql.sqlite;

import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore;
import io.teaql.core.sqlite.SqliteCommittedEventStore.InboxOutcome;
import io.teaql.core.sqlite.SqliteCommittedOutboxPublisher;
import io.teaql.core.sqlite.SqliteCommittedObservation;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.TeaQLRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Small infrastructure fixture; not an audited business-save/example claim. */
public class SqliteCommittedObservationTest {
    static final String SOURCE="source-CANARY", TARGET="target-CANARY", MODEL="sha256:"+"0".repeat(64);
    static final class Fixture implements AutoCloseable {
        final Path directory=Files.createTempDirectory("cmr14-java-");
        final JdbcSqlExecutor sql; final DefaultUserContext context;
        final SqliteCommittedOutboxPublisher publisher;
        final SqliteCommittedObservation observations;
        final List<Map<String,Object>> telemetry=new ArrayList<>();
        Fixture() throws Exception {
            var ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+directory.resolve("source.sqlite"));
            sql=new JdbcSqlExecutor(ds);new SqliteCommittedEventStore(sql).ensureSchema();
            RuntimeTelemetry t=op->{telemetry.add(op.attributes());return new RuntimeTelemetry.Scope(){
                public void success(Map<String,Object> attrs){telemetry.add(attrs);}
                public void failure(Throwable ignored){fail("observation should succeed");}};};
            context=new DefaultUserContext(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).telemetry(t).build());
            publisher=new SqliteCommittedOutboxPublisher(sql,SOURCE,TARGET,MODEL);
            observations=new SqliteCommittedObservation(sql,SOURCE,TARGET,MODEL);
            sql.update("INSERT INTO teaql_event_sequence VALUES(?,2)",new Object[]{SOURCE});
            for(int n=1;n<=2;n++)sql.update("INSERT INTO teaql_event_outbox VALUES(?,?,?,?,?,?)",
                new Object[]{SOURCE,n,"fact-CANARY-"+n,MODEL,"a".repeat(64),"payload-CANARY"});
        }
        @Override public void close()throws Exception{try(var paths=Files.walk(directory)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    @Test public void lostResponseRetryAndReopenKeepDurableFacts()throws Exception {
        try(var f=new Fixture()){
            try{f.publisher.publishBatch(1,event->{throw new IllegalStateException("transport-CANARY");});fail();}catch(IllegalStateException expected){}
            var a=f.observations.source(f.context);assertEquals(2,a.sourceHighWatermark());assertEquals(0,a.ackedSequence());
            assertEquals(2,a.sequenceLag());assertEquals(2,a.backlog());assertEquals(1,a.attempts());assertEquals(1,a.failures());assertEquals(0,a.retries());assertEquals(0,a.deadLetters());
            assertEquals(1,f.publisher.publishBatch(1,event->new InboxOutcome(false,1)).acknowledged());
            var b=new SqliteCommittedObservation(f.sql,SOURCE,TARGET,MODEL).source(f.context);
            assertEquals(1,b.ackedSequence());assertEquals(1,b.backlog());assertEquals(2,b.attempts());assertEquals(1,b.retries());assertEquals(1,b.failures());
            assertEquals(2,((Number)f.sql.queryForList("SELECT count(*) n FROM teaql_event_outbox",new Object[0]).get(0).get("n")).intValue());
            assertFalse(f.telemetry.toString().contains("CANARY"));assertTrue(f.telemetry.toString().contains("teaql.replication.retries"));
            System.out.println("CMR14_SOURCE_PASS language=java retry=1 backlog=1");
        }
    }
    @Test public void pendingRowsAreNotSequenceDistance()throws Exception{
        try(var f=new Fixture()){
            f.sql.update("DELETE FROM teaql_event_outbox WHERE source_sequence=1",new Object[0]);
            var s=f.observations.source(f.context);assertEquals(2,s.sequenceLag());assertEquals(1,s.backlog());
            try{f.publisher.publishBatch(2,e->new InboxOutcome(false,e.sourceSequence()));fail();}catch(SqliteCommittedEventStore.CommittedEventException expected){assertEquals("SEQUENCE_GAP",expected.code());}
            assertEquals(0,f.observations.source(f.context).attempts());
        }
    }
    @Test public void badAckDoesNotAcknowledgeOrHideFailure()throws Exception{
        try(var f=new Fixture()){
            try{f.publisher.publishBatch(1,e->new InboxOutcome(false,2));fail();}catch(SqliteCommittedEventStore.CommittedEventException expected){assertEquals("ACK_MISMATCH",expected.code());}
            var s=f.observations.source(f.context);assertEquals(0,s.ackedSequence());assertEquals(1,s.failures());assertEquals(0,s.deadLetters());
        }
    }
    @Test public void journalFailureRollsBackSourceAck()throws Exception{
        try(var f=new Fixture()){
            f.sql.execute("CREATE TRIGGER refuse_ack BEFORE UPDATE ON teaql_event_delivery_journal WHEN NEW.last_outcome='ACKNOWLEDGED' BEGIN SELECT RAISE(ABORT,'journal-CANARY'); END");
            try{f.publisher.publishBatch(1,e->new InboxOutcome(false,1));fail();}catch(RuntimeException expected){}
            var s=f.observations.source(f.context);assertEquals(0,s.ackedSequence());assertEquals(1,s.unresolvedAttempts());assertEquals(0,s.failures());
        }
    }
    @Test public void observationsRejectAmbientAndInconsistentPositions()throws Exception{
        try(var f=new Fixture()){
            f.sql.executeInTransaction(()->{try{f.observations.source(f.context);fail();}catch(SqliteCommittedEventStore.CommittedEventException expected){assertEquals("OBSERVATION_UNAVAILABLE",expected.code());}});
            f.sql.update("INSERT INTO teaql_event_delivery_cursor VALUES(?,?,3,?)",new Object[]{SOURCE,TARGET,MODEL});
            try{f.observations.source(f.context);fail();}catch(SqliteCommittedEventStore.CommittedEventException expected){assertEquals("OBSERVATION_UNAVAILABLE",expected.code());}
        }
    }
}
