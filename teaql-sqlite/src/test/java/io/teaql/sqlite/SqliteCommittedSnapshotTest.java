package io.teaql.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.teaql.core.BaseEntity;
import io.teaql.core.meta.*;
import io.teaql.core.sqlite.*;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class SqliteCommittedSnapshotTest {
    private static final String SOURCE = "snapshot-source", MODEL = "sha256:" + "0".repeat(64);
    private static final long ORDER = 9_007_199_254_740_993L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SqliteCommittedSnapshot.Limits LIMITS = SqliteCommittedSnapshot.Limits.defaults();
    private static final class Fixture implements AutoCloseable {
        final Path directory, path;
        final SQLiteDataSource dataSource = new SQLiteDataSource();
        final JdbcSqlExecutor sql;
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final DefaultUserContext context;
        Fixture(boolean legacy) throws Exception {
            directory = Files.createTempDirectory("teaql-snapshot-"); path = directory.resolve("data.sqlite");
            dataSource.setUrl("jdbc:sqlite:" + path);
            sql = executor(dataSource); sql.execute("PRAGMA journal_mode=WAL");
            EntityDescriptor root = entity("platform", "name", String.class);
            // Exercise JDBC label folding without changing the model identity.
            root.findIdProperty().with("columnName", "Id"); root.findVersionProperty().with("columnName", "Version");
            metadata.register(root);
            EntityDescriptor status = entity("order_status", "code", String.class);
            status.addObjectProperty(metadata, "platform", "platform", "status_list", BaseEntity.class);
            metadata.register(status);
            EntityDescriptor order = entity("customer_order", "name", String.class);
            order.addSimpleProperty("amount", java.math.BigDecimal.class).with("sqlType", "NUMERIC(19,7)");
            order.addObjectProperty(metadata, "platform", "platform", "order_list", BaseEntity.class);
            order.addObjectProperty(metadata, "status", "order_status", "order_list", BaseEntity.class);
            metadata.register(order);
            root.addObjectProperty(metadata, "favorite_order", "customer_order", "favorite_platform", BaseEntity.class);
            EntityDescriptor item = entity("order_item", "quantity", Long.class);
            item.addObjectProperty(metadata, "customer_order", "customer_order", "item_list", BaseEntity.class);
            item.addSimpleProperty("occurred_at", java.time.Instant.class);
            item.addSimpleProperty("planned_date", java.time.LocalDate.class);
            item.addSimpleProperty("active", Boolean.class);
            item.addSimpleProperty("ratio", Double.class);
            item.addSimpleProperty("note", String.class);
            metadata.register(item); metadata.register(entity("empty_probe", "name", String.class));
            context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
            sql.execute("CREATE TABLE platform_data(Id INTEGER PRIMARY KEY,Version INTEGER NOT NULL,name TEXT,favorite_order INTEGER REFERENCES customer_order_data(id))");
            sql.execute("CREATE TABLE order_status_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,code TEXT,platform INTEGER REFERENCES platform_data(Id))");
            sql.execute("CREATE TABLE customer_order_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,name TEXT,amount TEXT,platform INTEGER REFERENCES platform_data(Id),status INTEGER REFERENCES order_status_data(id))");
            sql.execute("CREATE TABLE order_item_data(id INTEGER PRIMARY KEY,version INTEGER NOT NULL,quantity INTEGER,customer_order INTEGER REFERENCES customer_order_data(id),occurred_at INTEGER,planned_date TEXT,active INTEGER,ratio REAL,note TEXT)");
            sql.execute("CREATE TABLE empty_probe_data(id INTEGER PRIMARY KEY,version INTEGER,name TEXT)");
            new SqliteCommittedEventStore(sql).ensureSchema();
            if (legacy) sql.executeInTransaction(() -> {
                sql.execute("PRAGMA defer_foreign_keys=ON");
                sql.update("INSERT INTO platform_data VALUES(1,3,'SOURCE ROOT',?)", new Object[] {ORDER});
                sql.execute("INSERT INTO order_status_data VALUES(1001,2,'OPEN',1)");
                sql.update("INSERT INTO customer_order_data VALUES(?,7,'LEGACY','123.4500000',1,1001)", new Object[] {ORDER});
                sql.update("INSERT INTO order_item_data VALUES(?,-4,2,?,-315619199877,'2026-10-16',1,0.125,NULL)", new Object[] {ORDER + 1, ORDER});
            });
        }
        SqliteCommittedSnapshot snapshots() { return new SqliteCommittedSnapshot(sql, SOURCE, MODEL); }
        SqliteModelCheckedCommittedTarget target() { return new SqliteModelCheckedCommittedTarget(sql, SOURCE, MODEL); }
        SqliteCommittedSnapshot.SnapshotRecord snapshot() { return snapshots().exportSnapshot(context, "snapshot-1", "copy canonical facts", "initialize analytics", LIMITS); }
        @Override public void close() throws Exception {
            // Only the exact owned fixture files; no shared DB/cache cleanup.
            for (String file : List.of("data.sqlite-wal", "data.sqlite-shm", "data.sqlite-journal", "data.sqlite")) Files.deleteIfExists(directory.resolve(file));
            Files.deleteIfExists(directory);
        }
    }
    private static EntityDescriptor entity(String name, String field, Class<?> type) {
        var entity = new EntityDescriptor(); entity.setType(name);
        entity.addSimpleProperty("id", Long.class); entity.addSimpleProperty("version", Long.class);
        entity.addSimpleProperty(field, type); return entity;
    }
    private static JdbcSqlExecutor executor(SQLiteDataSource dataSource) {
        return new JdbcSqlExecutor(dataSource).addConnectionInitializer(connection -> {
            try (var statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=ON"); statement.execute("PRAGMA busy_timeout=5000"); }
            catch (java.sql.SQLException failure) { throw new IllegalStateException("fixture connection setup failed", failure); }
        });
    }
    private static void expect(Runnable action) { try { action.run(); fail("expected snapshot rejection"); } catch (SqliteCommittedEventStore.CommittedEventException expected) { assertNotNull(expected.code()); } }
    private static long scalar(Fixture f, String sql) { return ((Number) f.sql.queryForList(sql, new Object[0]).get(0).values().iterator().next()).longValue(); }
    private static void sameState(Fixture a, Fixture b) {
        for (String table : List.of("platform_data", "order_status_data", "customer_order_data", "order_item_data", "empty_probe_data"))
            assertEquals(table, a.sql.queryForList("SELECT * FROM " + table + " ORDER BY 1", new Object[0]), b.sql.queryForList("SELECT * FROM " + table + " ORDER BY 1", new Object[0]));
    }
    private static String tail(long sequence) {
        return """
            {"protocol_version":1,"change_set_id":"tail-1","source_runtime_id":"snapshot-source",
             "source_sequence":"%d","source_model_hash":"%s","source_transaction_id":"tx-tail",
             "committed_at":"2026-10-22T00:00:00Z","audit":{"comment":"advance source","purpose":"verify snapshot tail"},
             "mutations":[{"entity_type":"customer_order","entity_id":"9007199254740993","operation":"update","base_version":"7","committed_version":"8","fields":{"name":"AFTER N","amount":"125.0000000"},"trace_chain":[]},
              {"entity_type":"order_item","entity_id":"9007199254740994","operation":"recover","base_version":"-4","committed_version":"5","fields":{"quantity":"3"},"trace_chain":[]}]}
            """.formatted(sequence, MODEL);
    }
    private static void commitTail(Fixture f) {
        JdbcSqlExecutor writer = executor(f.dataSource); // A second real connection while export is reading.
        writer.executeInTransaction(() -> {
            writer.update("UPDATE customer_order_data SET version=8,name='AFTER N',amount='125.0000000' WHERE id=?", new Object[] {ORDER});
            writer.update("UPDATE order_item_data SET version=5,quantity=3 WHERE id=?", new Object[] {ORDER + 1});
            new SqliteCommittedEventStore(writer).append(SOURCE, "tail-1", MODEL, SqliteCommittedSnapshotTest::tail);
        });
    }
    private static SqliteCommittedSnapshot.SnapshotRecord changed(SqliteCommittedSnapshot.SnapshotRecord original, java.util.function.Consumer<ObjectNode> change) throws Exception {
        ObjectNode body = (ObjectNode) JSON.readTree(original.payloadJson()); change.accept(body); String text = body.toString();
        return new SqliteCommittedSnapshot.SnapshotRecord(original.sourceRuntimeId(), original.sourceSequence(), original.snapshotId(), original.modelHash(),
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))), text);
    }

    @Test public void legacySnapshotPlusConcurrentTailMatchesSourceAndDoesNotRewind() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(false)) {
            AtomicBoolean once = new AtomicBoolean();
            EntityMetaFactory intercepted = new EntityMetaFactory() {
                public EntityDescriptor resolveEntityDescriptor(String type) { return source.metadata.resolveEntityDescriptor(type); }
                public void register(EntityDescriptor type) { throw new AssertionError("no model changes"); }
                public List<EntityDescriptor> allEntityDescriptors() { if (once.compareAndSet(false, true)) commitTail(source); return source.metadata.allEntityDescriptors(); }
            };
            DefaultUserContext context = new DefaultUserContext(TeaQLRuntime.builder().metadata(intercepted).build());
            var snapshot = source.snapshots().exportSnapshot(context, "snapshot-1", "copy legacy state", "verify consistent bootstrap", LIMITS);
            assertTrue(once.get()); assertEquals(0, snapshot.sourceSequence());
            JsonNode body = JSON.readTree(snapshot.payloadJson()); assertEquals(5, body.path("entity_types").size()); assertEquals(4, body.path("rows").size());
            assertTrue(snapshot.payloadJson().contains("LEGACY")); assertFalse(snapshot.payloadJson().contains("AFTER N"));
            assertTrue(snapshot.payloadJson().contains("\"version\":\"-4\""));
            assertEquals(0, target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).checkpoint());
            var events = new SqliteCommittedEventStore(source.sql).outboxAfter(SOURCE, snapshot.sourceSequence(), 10);
            assertEquals(1, events.size()); assertEquals(1, events.get(0).sourceSequence());
            assertEquals(1, target.target().apply(target.context, events.get(0)).checkpoint()); sameState(source, target);
            assertTrue(target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).duplicate());
            assertEquals(0, target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).checkpoint());
            assertEquals(1, scalar(target, "SELECT source_sequence FROM teaql_event_checkpoint")); sameState(source, target);
            assertTrue(target.target().apply(target.context, events.get(0)).duplicate());
            expect(() -> target.snapshots().initializeSnapshot(target.context, source.snapshot(), SqliteCommittedSnapshot.ImportMode.REPLACE_UNINITIALIZED, LIMITS));
            System.out.println("CMR11_SNAPSHOT_PASS language=java snapshot=0 tail=1 rows=4 catalog=5 concurrent_writer=1 duplicate_after_tail=1");
        }
    }
    @Test public void explicitReplacementHandlesBootstrapRowsButDefaultCannotOverwrite() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(true)) {
            target.sql.execute("UPDATE platform_data SET name='BOOTSTRAP'"); var snapshot = source.snapshot();
            expect(() -> target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS));
            assertEquals(0, scalar(target, "SELECT count(*) FROM teaql_event_snapshot"));
            target.snapshots().initializeSnapshot(target.context, snapshot, SqliteCommittedSnapshot.ImportMode.REPLACE_UNINITIALIZED, LIMITS); sameState(source, target);
        }
    }
    @Test public void nonzeroSnapshotStartsAtNPlusOneWithoutReplayingOlderHistory() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(false)) {
            commitTail(source); var snapshot = source.snapshot(); assertEquals(1, snapshot.sourceSequence());
            target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS); sameState(source, target);
            var old = new SqliteCommittedEventStore(source.sql).outboxAfter(SOURCE, 0, 10).get(0);
            expect(() -> target.target().apply(target.context, old)); // No fabricated pre-snapshot Inbox.
            source.sql.executeInTransaction(() -> {
                assertEquals(1, source.sql.update("UPDATE customer_order_data SET version=9,name='AT TWO' WHERE id=? AND version=8", new Object[] {ORDER}));
                new SqliteCommittedEventStore(source.sql).append(SOURCE, "tail-2", MODEL, sequence -> {
                    try {
                        ObjectNode body = (ObjectNode) JSON.readTree(tail(sequence)); body.put("change_set_id", "tail-2");
                        var mutations = body.withArray("mutations"); ObjectNode mutation = (ObjectNode) mutations.get(0);
                        mutation.put("base_version", "8"); mutation.put("committed_version", "9");
                        mutation.with("fields").removeAll().put("name", "AT TWO"); mutations.remove(1);
                        return body.toString();
                    } catch (Exception failure) { throw new AssertionError(failure); }
                });
            });
            var next = new SqliteCommittedEventStore(source.sql).outboxAfter(SOURCE, 1, 10);
            assertEquals(1, next.size()); assertEquals(2, next.get(0).sourceSequence());
            target.target().apply(target.context, next.get(0)); sameState(source, target);
            assertEquals(1, target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).checkpoint());
            assertEquals(2, scalar(target, "SELECT source_sequence FROM teaql_event_checkpoint"));
            System.out.println("CMR11_NONZERO_PASS language=java snapshot=1 tail=2 legacy_history_absent=1");
        }
    }
    @Test public void lateStorageFailureRollsBackReplacementRowsSnapshotAndCheckpoint() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(true)) {
            target.sql.execute("UPDATE platform_data SET name='BOOTSTRAP'");
            target.sql.execute("CREATE TRIGGER reject_snapshot BEFORE INSERT ON order_item_data BEGIN SELECT RAISE(ABORT,'snapshot fault'); END");
            var snapshot = source.snapshot();
            try { target.snapshots().initializeSnapshot(target.context, snapshot, SqliteCommittedSnapshot.ImportMode.REPLACE_UNINITIALIZED, LIMITS); fail("expected storage failure"); } catch (RuntimeException expected) { }
            assertEquals("BOOTSTRAP", target.sql.queryForList("SELECT name FROM platform_data", new Object[0]).get(0).get("name"));
            assertEquals(0, scalar(target, "SELECT count(*) FROM teaql_event_snapshot")); assertEquals(0, scalar(target, "SELECT count(*) FROM teaql_event_checkpoint"));
            target.sql.execute("DROP TRIGGER reject_snapshot"); target.snapshots().initializeSnapshot(target.context, snapshot, SqliteCommittedSnapshot.ImportMode.REPLACE_UNINITIALIZED, LIMITS); sameState(source, target);
        }
    }
    @Test public void strictCatalogRowsVersionsProtocolAndBudgetsRejectBeforeAnyTargetChange() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(false)) {
            var snapshot = source.snapshot();
            for (var invalid : List.of(
                    changed(snapshot, b -> b.withArray("entity_types").remove(0)),
                    changed(snapshot, b -> ((ObjectNode) b.withArray("rows").get(0)).with("fields").put("unknown", "bad")),
                    changed(snapshot, b -> ((ObjectNode) b.withArray("rows").get(0)).put("version", "0")),
                    changed(snapshot, b -> b.put("snapshot_version", 4294967297L)),
                    changed(snapshot, b -> b.withArray("rows").add(b.withArray("rows").get(0).deepCopy()))))
                expect(() -> target.snapshots().initializeSnapshot(target.context, invalid, LIMITS));
            expect(() -> target.snapshots().initializeSnapshot(target.context, snapshot, new SqliteCommittedSnapshot.Limits(3, LIMITS.maxPayloadBytes())));
            expect(() -> target.snapshots().initializeSnapshot(target.context, snapshot, new SqliteCommittedSnapshot.Limits(10, 1)));
            expect(() -> source.snapshots().exportSnapshot(source.context, "small", "copy", "budget", new SqliteCommittedSnapshot.Limits(3, 100_000)));
            expect(() -> source.snapshots().exportSnapshot(source.context, "small", "copy", "budget", new SqliteCommittedSnapshot.Limits(10, 1)));
            assertEquals(0, scalar(target, "SELECT count(*) FROM customer_order_data")); assertEquals(0, scalar(target, "SELECT count(*) FROM teaql_event_snapshot"));
            assertFalse(snapshot.toString().contains("SOURCE ROOT"));
        }
    }
    @Test public void callerTransactionsAndMissingIntentAreRejected() throws Exception {
        try (Fixture f = new Fixture(true)) {
            expect(() -> f.snapshots().exportSnapshot(f.context, "x", " ", "purpose", LIMITS));
            f.sql.executeInTransaction(() -> expect(() -> f.snapshot()));
        }
    }
    @Test public void ordinaryEventCannotUseSnapshotVersionEscapeHatch() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(false)) {
            String body = tail(1).replace("\"operation\":\"update\",\"base_version\":\"7\",\"committed_version\":\"8\"", "\"operation\":\"create\",\"base_version\":null,\"committed_version\":\"8\"");
            var record = new SqliteCommittedEventStore.OutboxRecord(SOURCE, 1, "tail-1", MODEL, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))), body);
            expect(() -> target.target().apply(target.context, record)); assertEquals(0, scalar(target, "SELECT count(*) FROM customer_order_data"));
        }
    }

    private static SqliteCommittedSnapshotPublisher publisher(Fixture source, String target) { return new SqliteCommittedSnapshotPublisher(source.sql,SOURCE,target,MODEL); }
    private static void secondTail(Fixture source) {
        source.sql.executeInTransaction(() -> {
            assertEquals(1,source.sql.update("UPDATE customer_order_data SET version=9,name='AT TWO' WHERE id=? AND version=8",new Object[]{ORDER}));
            new SqliteCommittedEventStore(source.sql).append(SOURCE,"tail-2",MODEL,sequence->{
                try { ObjectNode body=(ObjectNode)JSON.readTree(tail(sequence));body.put("change_set_id","tail-2");var mutations=body.withArray("mutations");var row=(ObjectNode)mutations.get(0);row.put("base_version","8").put("committed_version","9");row.with("fields").removeAll().put("name","AT TWO");mutations.remove(1);return body.toString(); }
                catch(Exception failure){throw new AssertionError(failure);}
            });
        });
    }
    @Test public void durableBootstrapKeepsOriginalBytesAndResumesOrdinaryPublisherAfterLostAck() throws Exception {
        try(Fixture source=new Fixture(true);Fixture target=new Fixture(false)) {
            commitTail(source);var pub=publisher(source,"analytics");
            var prepared=pub.prepare(source.context,"bootstrap","copy facts","initialize target",LIMITS);
            assertEquals(1,prepared.snapshot().sourceSequence());assertFalse(prepared.acknowledged());secondTail(source);
            assertEquals(prepared,pub.prepare(source.context,"bootstrap","retry copy","retry bootstrap",LIMITS));
            var normal=new SqliteCommittedOutboxPublisher(source.sql,SOURCE,"analytics",MODEL);
            expect(()->normal.publishBatch(10,row->{throw new AssertionError("pending bootstrap must block tail");}));
            assertThrows(IllegalStateException.class,()->pub.publish(source.context,"bootstrap",LIMITS,row->{
                assertFalse(source.sql.hasActiveTransaction());
                var receipt=SqliteCommittedSnapshotPublisher.initializeAndAcknowledge(target.context,target.snapshots(),row,"analytics",LIMITS);
                assertFalse(receipt.duplicate());throw new IllegalStateException("fixture loses committed target ACK");
            }));
            assertEquals(0,scalar(source,"SELECT count(*) FROM teaql_event_delivery_cursor"));
            var restarted=publisher(source,"analytics");
            assertTrue(restarted.publish(source.context,"bootstrap",LIMITS,row->{
                var receipt=SqliteCommittedSnapshotPublisher.initializeAndAcknowledge(target.context,target.snapshots(),row,"analytics",LIMITS);assertTrue(receipt.duplicate());return receipt;
            }).acknowledged());
            assertEquals(1,normal.status().ackedSequence());
            assertEquals(1,normal.publishBatch(10,row->{assertEquals(2,row.sourceSequence());return target.target().apply(target.context,row);}).acknowledged());
            sameState(source,target);assertEquals(2,normal.status().ackedSequence());
            assertTrue(restarted.publish(source.context,"bootstrap",LIMITS,row->{throw new AssertionError("known ACK must not redeliver");}).acknowledged());
            assertEquals(2,normal.status().ackedSequence());expect(()->pub.prepare(source.context,"different","copy","bootstrap",LIMITS));
            System.out.println("CMR12_BOOTSTRAP_PASS language=java baseline=1 tail=2 retained_bytes=1 lost_ack=1 pending_gate=1 no_rewind=1");
        }
    }
    @Test public void acknowledgementAndCursorAreAtomicAndEveryReceiptIdentityIsBound() throws Exception {
        try(Fixture source=new Fixture(true);Fixture target=new Fixture(false)) {
            commitTail(source);var pub=publisher(source,"analytics");var snapshot=pub.prepare(source.context,"bootstrap","copy","bootstrap",LIMITS).snapshot();
            var ack=SqliteCommittedSnapshotPublisher.initializeAndAcknowledge(target.context,target.snapshots(),snapshot,"analytics",LIMITS);
            for(var bad:List.of(
                    new SqliteCommittedSnapshotPublisher.Receipt("other",ack.targetId(),ack.snapshotId(),1,MODEL,ack.payloadSha256(),false),
                    new SqliteCommittedSnapshotPublisher.Receipt(SOURCE,"other",ack.snapshotId(),1,MODEL,ack.payloadSha256(),false),
                    new SqliteCommittedSnapshotPublisher.Receipt(SOURCE,ack.targetId(),"other",1,MODEL,ack.payloadSha256(),false),
                    new SqliteCommittedSnapshotPublisher.Receipt(SOURCE,ack.targetId(),ack.snapshotId(),2,MODEL,ack.payloadSha256(),false),
                    new SqliteCommittedSnapshotPublisher.Receipt(SOURCE,ack.targetId(),ack.snapshotId(),1,"sha256:"+"1".repeat(64),ack.payloadSha256(),false),
                    new SqliteCommittedSnapshotPublisher.Receipt(SOURCE,ack.targetId(),ack.snapshotId(),1,MODEL,"0".repeat(64),false)))
                expect(()->pub.publish(source.context,"bootstrap",LIMITS,row->bad));
            source.sql.execute("CREATE TRIGGER reject_bootstrap_ack BEFORE UPDATE ON teaql_event_snapshot_publication WHEN NEW.acknowledged=1 BEGIN SELECT RAISE(ABORT,'fixture rejects ACK after cursor insert'); END");
            assertThrows(RuntimeException.class,()->pub.publish(source.context,"bootstrap",LIMITS,row->ack));
            assertEquals(0,scalar(source,"SELECT count(*) FROM teaql_event_delivery_cursor"));
            assertEquals(0,scalar(source,"SELECT acknowledged FROM teaql_event_snapshot_publication"));
            source.sql.execute("DROP TRIGGER reject_bootstrap_ack");assertTrue(pub.publish(source.context,"bootstrap",LIMITS,row->ack).acknowledged());
            source.sql.execute("DELETE FROM teaql_event_delivery_cursor");
            expect(()->new SqliteCommittedOutboxPublisher(source.sql,SOURCE,"analytics",MODEL).publishBatch(10,row->{throw new AssertionError("corrupt ACK/cursor must fail before delivery");}));
            System.out.println("CMR12_ACK_ATOMIC_PASS language=java bound_fields=6 rollback=1 retry=1 corrupt_cursor=1");
        }
    }
    @Test public void bootstrapModeIsExclusiveAndDoesNotJoinCallerTransactionsOrResetPublicationState() throws Exception {
        try(Fixture source=new Fixture(true);Fixture target=new Fixture(false)) {
            var normal=new SqliteCommittedOutboxPublisher(source.sql,SOURCE,"incremental",MODEL);
            expect(()->normal.publishBatch(0,row->{throw new AssertionError();}));assertEquals(0,scalar(source,"SELECT count(*) FROM teaql_event_delivery_cursor"));
            assertEquals(0,normal.publishBatch(1,row->{throw new AssertionError();}).acknowledged());
            expect(()->publisher(source,"incremental").prepare(source.context,"bootstrap","copy","bootstrap",LIMITS));
            assertEquals(0,scalar(source,"SELECT count(*) FROM teaql_event_snapshot_publication"));
            source.sql.executeInTransaction(()->expect(()->publisher(source,"new-target").prepare(source.context,"bootstrap","copy","bootstrap",LIMITS)));
            var targetPub=publisher(target,"another-target");targetPub.prepare(target.context,"bootstrap","copy","bootstrap",LIMITS);
            expect(()->target.snapshots().initializeSnapshot(target.context,source.snapshot(),SqliteCommittedSnapshot.ImportMode.REPLACE_UNINITIALIZED,LIMITS));
            assertEquals(1,scalar(target,"SELECT count(*) FROM teaql_event_snapshot_publication"));
        }
    }
    @Test public void nontransactionalAdaptersCannotPretendToProvideAtomicBootstrap() throws Exception {
        try(Fixture source=new Fixture(true);Fixture target=new Fixture(false)) {
            var queries=new java.util.concurrent.atomic.AtomicInteger();
            var unsafe=(io.teaql.dataservice.sql.SqlExecutionAdapter)java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(),new Class<?>[]{io.teaql.dataservice.sql.SqlExecutionAdapter.class},(proxy,method,args)->{
                        if(method.getName().equals("executeInTransaction")){((Runnable)args[0]).run();return null;}
                        if(method.getName().startsWith("query")||method.getName().equals("update")||method.getName().equals("execute"))queries.incrementAndGet();
                        try{return method.invoke(target.sql,args);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
                    });
            var snapshot=source.snapshot();
            expect(()->new SqliteCommittedSnapshot(unsafe,SOURCE,MODEL).exportSnapshot(target.context,"test","copy","bootstrap",LIMITS));
            expect(()->new SqliteCommittedSnapshot(unsafe,SOURCE,MODEL).initializeSnapshot(target.context,snapshot,LIMITS));
            expect(()->new SqliteCommittedSnapshotPublisher(unsafe,SOURCE,"target",MODEL).prepare(target.context,"test","copy","bootstrap",LIMITS));
            expect(()->new SqliteCommittedOutboxPublisher(unsafe,SOURCE,"target",MODEL).publishBatch(1,row->{throw new AssertionError();}));
            assertEquals(0,queries.get());
        }
    }
    @Test public void snapshotHashAuthorityParserReferencesAndConflictingRetryFailClosed() throws Exception {
        try (Fixture source = new Fixture(true); Fixture target = new Fixture(false)) {
            var snapshot = source.snapshot();
            for (var invalid : List.of(
                    new SqliteCommittedSnapshot.SnapshotRecord("another-source", 0, snapshot.snapshotId(), MODEL, snapshot.payloadSha256(), snapshot.payloadJson()),
                    new SqliteCommittedSnapshot.SnapshotRecord(SOURCE, 0, snapshot.snapshotId(), "sha256:" + "1".repeat(64), snapshot.payloadSha256(), snapshot.payloadJson()),
                    new SqliteCommittedSnapshot.SnapshotRecord(SOURCE, 0, snapshot.snapshotId(), MODEL, "0".repeat(64), snapshot.payloadJson()),
                    changed(snapshot, body -> body.put("source_sequence", "00")),
                    changed(snapshot, body -> {
                        for (JsonNode row : body.withArray("rows")) if (row.path("entity_type").asText().equals("customer_order"))
                            ((ObjectNode) row.path("fields").path("platform")).put("entity_id", "999");
                    }))) expect(() -> target.snapshots().initializeSnapshot(target.context, invalid, LIMITS));
            for (String text : List.of(snapshot.payloadJson() + " {}",
                    snapshot.payloadJson().replace("\"snapshot_version\":1", "\"snapshot_version\":1,\"snapshot_version\":1"))) {
                var invalid = new SqliteCommittedSnapshot.SnapshotRecord(SOURCE, 0, snapshot.snapshotId(), MODEL,
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))), text);
                expect(() -> target.snapshots().initializeSnapshot(target.context, invalid, LIMITS));
            }
            assertEquals(0, scalar(target, "SELECT count(*) FROM platform_data"));
            assertEquals(0, scalar(target, "SELECT count(*) FROM customer_order_data"));
            assertEquals(0, scalar(target, "SELECT count(*) FROM teaql_event_snapshot"));
            assertEquals(0, scalar(target, "SELECT count(*) FROM teaql_event_checkpoint"));
            target.sql.executeInTransaction(() -> expect(() -> target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS)));
            target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS); sameState(source, target);
            var conflict = changed(snapshot, body -> ((ObjectNode) body.path("audit")).put("comment", "different bytes for same snapshot"));
            expect(() -> target.snapshots().initializeSnapshot(target.context, conflict, LIMITS));
            assertTrue(target.snapshots().initializeSnapshot(target.context, snapshot, LIMITS).duplicate());
            sameState(source, target);
            System.out.println("CMR11_GUARDS_PASS language=java source=1 model=1 digest=1 parser=2 dangling_fk=1 conflict=1 atomic=1 retry=1");
        }
    }
}
