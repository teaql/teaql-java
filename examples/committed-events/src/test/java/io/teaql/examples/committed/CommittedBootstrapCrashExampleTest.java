package io.teaql.examples.committed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.*;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.sqlite.Function;
import org.sqlite.SQLiteDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/** Real owned-process death at SQLite bootstrap/tail boundaries; not power-loss proof. */
public class CommittedBootstrapCrashExampleTest {
    private static final String SOURCE = "cmr12-java-source", MODEL = "sha256:" + "0".repeat(64);
    private static final long LEGACY_ID = 9_007_199_254_740_993L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SqliteCommittedSnapshot.Limits LIMITS = new SqliteCommittedSnapshot.Limits(32, 65536);
    private static final long WAIT_SECONDS = 30, MAX_LOG = 256 * 1024;

    private static String slot(int round) { return "analytics-" + round; }
    private static String identity(int round) { return "bootstrap-" + round; }
    private static long baseline(int round) { return 2L * round - 1; }
    private static String digest(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static void writeNew(Path file, Object value) throws Exception {
        Files.writeString(file, JSON.writeValueAsString(value), StandardOpenOption.CREATE_NEW);
    }
    private static void pause(Path directory, String phase, int round, long visibleRows) {
        try {
            Path temporary = directory.resolve(phase + "-" + round + ".ready.tmp");
            // Flush diagnostics before announcing readiness; SIGKILL may follow the rename immediately.
            System.out.println("CMR12_SQL_BARRIER phase=" + phase + " round=" + round + " visible_rows=" + visibleRows);
            System.out.flush();
            writeNew(temporary, Map.of("pid", ProcessHandle.current().pid(), "phase", phase,
                    "round", round, "visibleRows", visibleRows));
            Files.move(temporary, directory.resolve(phase + "-" + round + ".ready.json"), StandardCopyOption.ATOMIC_MOVE);
            for (;;) Thread.sleep(1000); // parent must SIGKILL; no graceful transaction cleanup
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    /** Test adapter only: delegates the real SELECT first, then pauses its active read transaction. */
    private static final class BarrierSql extends JdbcSqlExecutor {
        final Path directory;
        final String phase;
        final int round;
        boolean armed;
        BarrierSql(SQLiteDataSource ds, Path directory, String phase, int round) {
            super(ds); this.directory = directory; this.phase = phase; this.round = round;
            addConnectionInitializer(connection -> {
                try {
                    Function.create(connection, "cmr12_test_barrier", new Function() {
                        @Override protected void xFunc() throws java.sql.SQLException {
                            String wanted = value_text(0);
                            if (armed && phase.equals(wanted)) {
                                assertFalse("trigger must run inside a real transaction", connection.getAutoCommit());
                                pause(directory, phase, round, value_long(1));
                            }
                            result(0);
                        }
                    });
                } catch (java.sql.SQLException failure) { throw new IllegalStateException(failure); }
            });
        }
        @Override public List<Map<String,Object>> queryForList(String statement, Object[] values) {
            var rows = super.queryForList(statement, values);
            if (armed && phase.equals("capture") && statement.startsWith("SELECT current_sequence")) {
                assertTrue(hasActiveTransaction());
                assertEquals(baseline(round), ((Number) rows.get(0).get("current_sequence")).longValue());
                pause(directory, phase, round, -1); // sequence read is established; domain rows are not read yet
            }
            return rows;
        }
    }
    private record Fixture(BarrierSql sql, DefaultUserContext context, TeaQLRuntime runtime) implements AutoCloseable {
        SqliteCommittedSnapshot snapshots() { return new SqliteCommittedSnapshot(sql, SOURCE, MODEL); }
        SqliteCommittedSnapshotPublisher bootstrap(int round) { return new SqliteCommittedSnapshotPublisher(sql, SOURCE, slot(round), MODEL); }
        SqliteCommittedOutboxPublisher publisher(int round) { return new SqliteCommittedOutboxPublisher(sql, SOURCE, slot(round), MODEL); }
        @Override public void close() { runtime.close(); }
        void schema() { context.ensureSchema(); new SqliteCommittedEventStore(sql).ensureSchema(); }
    }
    private static Fixture fixture(Path directory, String phase, int round, boolean source, boolean capture) {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + directory.resolve(source ? "source.sqlite" : "target-" + round + ".sqlite"));
        BarrierSql sql = new BarrierSql(ds, directory, phase, round);
        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType("Order"); descriptor.setTargetType(CommittedEventsExampleTest.Order.class);
        descriptor.setEntitySupplier(CommittedEventsExampleTest.Order::new); descriptor.setDataService("sqlite");
        for (String field : List.of("id", "version", "name", "memo")) {
            boolean text = field.equals("name") || field.equals("memo");
            GenericSQLProperty property = (GenericSQLProperty) descriptor.addSimpleProperty(field, text ? String.class : Long.class);
            property.setColumnType(text ? "VARCHAR(255)" : "BIGINT");
        }
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
        var builder = TeaQLRuntime.builder().metadata(metadata)
                .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", sql, ds)).executionLogging(false)
                .idGenerationService((context, entity) -> { throw new AssertionError("fixture already assigns IDs; replication must not allocate"); });
        if (capture) builder.committedMutationRecorder(new SqliteCommittedMutationRecorder(sql, SOURCE, MODEL));
        TeaQLRuntime runtime = builder.build();
        return new Fixture(sql, new DefaultUserContext(runtime), runtime);
    }
    private static long count(Fixture f, String table) {
        return ((Number) f.sql.queryForList("SELECT count(*) AS n FROM " + table, new Object[0]).get(0).get("n")).longValue();
    }
    private static long checkpoint(Fixture f) {
        var rows = f.sql.queryForList("SELECT source_sequence FROM teaql_event_checkpoint WHERE source_runtime_id=?", new Object[]{SOURCE});
        return rows.isEmpty() ? 0 : ((Number) rows.get(0).get("source_sequence")).longValue();
    }
    private static SqliteCommittedSnapshot.SnapshotRecord retained(Fixture f, Path directory, int round) throws Exception {
        var rows = f.sql.queryForList("SELECT * FROM teaql_event_snapshot_publication WHERE source_runtime_id=? AND target_id=?", new Object[]{SOURCE, slot(round)});
        assertEquals(1, rows.size()); var row = rows.get(0);
        String payload = (String) row.get("payload_json");
        assertEquals(Files.readString(directory.resolve("snapshot-" + round + ".json")), payload);
        assertEquals(Files.readString(directory.resolve("snapshot-" + round + ".sha256")), digest(payload));
        assertEquals(baseline(round), ((Number) row.get("source_sequence")).longValue());
        return new SqliteCommittedSnapshot.SnapshotRecord(SOURCE, baseline(round), identity(round), MODEL, digest(payload), payload);
    }
    private static void save(Fixture source, Path directory, int round, boolean tail) throws Exception {
        Files.writeString(directory.resolve("business-calls.txt"), (tail ? "tail-" : "seed-") + round + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        var order = new CommittedEventsExampleTest.Order();
        order.__internalInitializeNewEntityId((tail ? 200L : 100L) + round);
        order.updateProperty("name", (tail ? "tail-" : "seed-") + round);
        order.updateProperty("memo", "业务事实");
        order.auditAs("persist source facts for controlled bootstrap recovery").save(source.context);
    }
    private static void sourceState(Path directory, int round, boolean job, boolean acked, boolean tail, boolean delivered) throws Exception {
        try (Fixture source = fixture(directory, "read", round, true, false)) {
            long n = baseline(round), high = n + (tail ? 1 : 0);
            assertEquals(high, new SqliteCommittedEventStore(source.sql).outboxAfter(SOURCE, 0, 10).size());
            assertEquals(high + 1, count(source, "order_data"));
            assertEquals(high, Files.readAllLines(directory.resolve("business-calls.txt")).size());
            var jobs = source.sql.queryForList("SELECT acknowledged FROM teaql_event_snapshot_publication WHERE target_id=?", new Object[]{slot(round)});
            assertEquals(job ? 1 : 0, jobs.size());
            if (job) { retained(source, directory, round); assertEquals(acked ? 1 : 0, ((Number) jobs.get(0).get("acknowledged")).intValue()); }
            var cursors = source.sql.queryForList("SELECT acked_sequence FROM teaql_event_delivery_cursor WHERE target_id=?", new Object[]{slot(round)});
            assertEquals(acked ? 1 : 0, cursors.size());
            if (acked) assertEquals(delivered ? high : n, ((Number) cursors.get(0).get("acked_sequence")).longValue());
        }
    }
    private static void targetState(Path directory, int round, boolean installed, boolean applied) throws Exception {
        try (Fixture target = fixture(directory, "read", round, false, false)) {
            long n = baseline(round), position = installed ? n + (applied ? 1 : 0) : 0;
            assertEquals(installed ? position + 1 : 0, count(target, "order_data"));
            assertEquals(installed ? 1 : 0, count(target, "teaql_event_snapshot"));
            assertEquals(installed ? 1 : 0, count(target, "teaql_event_checkpoint"));
            assertEquals(position, checkpoint(target));
            assertEquals(applied ? 1 : 0, count(target, "teaql_event_inbox"));
            assertEquals(applied ? 1 : 0, count(target, "teaql_event_applied_payload"));
            if (installed) assertEquals(-4L, ((Number) target.sql.queryForList("SELECT version FROM order_data WHERE id=?", new Object[]{LEGACY_ID}).get(0).get("version")).longValue());
        }
    }

    /** Actual fresh-connection readback after each owned worker exits; retained for deterministic audit. */
    private static Map<String,Object> observed(Path directory, int round) throws Exception {
        Map<String,Object> state = new LinkedHashMap<>();
        try (Fixture source = fixture(directory, "read", round, true, false)) {
            var jobs = source.sql.queryForList("SELECT source_sequence,acknowledged,payload_sha256 FROM teaql_event_snapshot_publication WHERE target_id=?", new Object[]{slot(round)});
            var cursors = source.sql.queryForList("SELECT acked_sequence FROM teaql_event_delivery_cursor WHERE target_id=?", new Object[]{slot(round)});
            state.put("source", Map.of("high", source.publisher(round).status().sourceHighWatermark(),
                    "domainRows", count(source, "order_data"), "businessCalls", Files.readAllLines(directory.resolve("business-calls.txt")).size(),
                    "snapshotSequence", jobs.isEmpty() ? -1 : ((Number) jobs.get(0).get("source_sequence")).longValue(),
                    "snapshotAcknowledged", jobs.isEmpty() ? -1 : ((Number) jobs.get(0).get("acknowledged")).intValue(),
                    "snapshotHash", jobs.isEmpty() ? "" : jobs.get(0).get("payload_sha256"),
                    "cursor", cursors.isEmpty() ? -1 : ((Number) cursors.get(0).get("acked_sequence")).longValue()));
        }
        if (Files.exists(directory.resolve("target-" + round + ".sqlite"))) {
            try (Fixture target = fixture(directory, "read", round, false, false)) {
                state.put("target", Map.of("present", true, "domainRows", count(target, "order_data"),
                        "snapshotRows", count(target, "teaql_event_snapshot"), "checkpoint", checkpoint(target),
                        "inbox", count(target, "teaql_event_inbox"), "applied", count(target, "teaql_event_applied_payload")));
            }
        } else state.put("target", Map.of("present", false));
        return state;
    }

    /** Test worker only. All barriers are in this fixture, not runtime production code. */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("phase directory round required");
        String phase = args[0]; Path directory = Path.of(args[1]); int round = Integer.parseInt(args[2]);
        if (phase.equals("premature")) return;
        try (Fixture source = fixture(directory, phase, round, true, phase.equals("seed") || phase.equals("tail"))) {
            if (phase.equals("seed")) {
                source.schema();
                if (round == 1) source.sql.update("INSERT INTO order_data(id,version,name,memo) VALUES(?,-4,'旧记录',NULL)", new Object[]{LEGACY_ID}); // legacy pre-capture fixture only
                save(source, directory, round, false);
            } else if (phase.equals("tail")) {
                save(source, directory, round, true);
            } else if (Set.of("capture", "retain", "prepare").contains(phase)) {
                if (phase.equals("retain")) source.sql.execute("CREATE TRIGGER retain_barrier_" + round
                        + " AFTER INSERT ON teaql_event_snapshot_publication WHEN NEW.target_id='" + slot(round)
                        + "' BEGIN SELECT cmr12_test_barrier('retain',(SELECT count(*) FROM teaql_event_snapshot_publication WHERE target_id=NEW.target_id)); END");
                source.sql.armed = true;
                var prepared = source.bootstrap(round).prepare(source.context, identity(round), "capture bootstrap facts", "recover analytics without replay", LIMITS);
                assertFalse(prepared.acknowledged()); assertEquals(baseline(round), prepared.snapshot().sourceSequence());
                Files.writeString(directory.resolve("snapshot-" + round + ".json"), prepared.snapshot().payloadJson(), StandardOpenOption.CREATE_NEW);
                Files.writeString(directory.resolve("snapshot-" + round + ".sha256"), prepared.snapshot().payloadSha256(), StandardOpenOption.CREATE_NEW);
            } else {
                retained(source, directory, round);
                try (Fixture target = fixture(directory, phase, round, false, false)) {
                    if (phase.equals("target_apply")) {
                        target.schema();
                        target.sql.execute("CREATE TRIGGER snapshot_barrier AFTER INSERT ON order_data WHEN NEW.id=101 "
                                + "BEGIN SELECT cmr12_test_barrier('target_apply',(SELECT count(*) FROM order_data)); END");
                        target.sql.armed = true;
                    }
                    if (phase.equals("source_ack")) {
                        source.sql.execute("CREATE TRIGGER ack_barrier_" + round + " AFTER INSERT ON teaql_event_delivery_cursor WHEN NEW.target_id='" + slot(round)
                                + "' BEGIN SELECT cmr12_test_barrier('source_ack',(SELECT count(*) FROM teaql_event_delivery_cursor WHERE target_id=NEW.target_id)); END");
                        source.sql.armed = true;
                    }
                    if (Set.of("target_apply", "snapshot_receipt_lost", "source_ack", "ack").contains(phase)) {
                        var prepared = source.bootstrap(round).publish(source.context, identity(round), LIMITS, record -> {
                            var receipt = SqliteCommittedSnapshotPublisher.initializeAndAcknowledge(target.context, target.snapshots(), record, slot(round), LIMITS);
                            assertEquals(!phase.equals("snapshot_receipt_lost") && !phase.equals("target_apply"), receipt.duplicate());
                            if (phase.equals("snapshot_receipt_lost")) pause(directory, phase, round, count(target, "order_data"));
                            return receipt;
                        });
                        assertTrue(prepared.acknowledged());
                    } else if (Set.of("tail_apply", "tail_receipt_lost", "tail_ack").contains(phase)) {
                        if (phase.equals("tail_apply")) {
                            target.sql.execute("CREATE TRIGGER tail_barrier AFTER INSERT ON order_data WHEN NEW.id=" + (200 + round)
                                    + " BEGIN SELECT cmr12_test_barrier('tail_apply',(SELECT count(*) FROM order_data)); END");
                            target.sql.armed = true;
                        }
                        var outcome = source.publisher(round).publishBatch(1, record -> {
                            assertEquals(baseline(round) + 1, record.sourceSequence());
                            var result = new SqliteModelCheckedCommittedTarget(target.sql, SOURCE, MODEL).apply(target.context, record);
                            assertEquals(phase.equals("tail_ack"), result.duplicate());
                            if (phase.equals("tail_receipt_lost")) pause(directory, phase, round, count(target, "order_data"));
                            return result;
                        });
                        assertEquals(1, outcome.acknowledged());
                    } else if (phase.equals("verify")) {
                        assertTrue(source.bootstrap(round).publish(source.context, identity(round), LIMITS, ignored -> { throw new AssertionError("known snapshot ACK must not redeliver"); }).acknowledged());
                        assertEquals(0, source.publisher(round).publishBatch(1, ignored -> { throw new AssertionError("known tail ACK must not redeliver"); }).acknowledged());
                        assertEquals(source.sql.queryForList("SELECT * FROM order_data ORDER BY id", new Object[0]), target.sql.queryForList("SELECT * FROM order_data ORDER BY id", new Object[0]));
                    } else if (phase.equals("catchup")) {
                        var batch = source.publisher(round).publishBatch(10, record -> new SqliteModelCheckedCommittedTarget(target.sql, SOURCE, MODEL).apply(target.context, record));
                        assertEquals(2, batch.acknowledged()); assertEquals(4, batch.status().ackedSequence());
                        assertEquals(1, source.bootstrap(round).publish(source.context, identity(round), LIMITS, ignored -> { throw new AssertionError("snapshot must not reset after catchup"); }).snapshot().sourceSequence());
                        assertEquals(source.sql.queryForList("SELECT * FROM order_data ORDER BY id", new Object[0]), target.sql.queryForList("SELECT * FROM order_data ORDER BY id", new Object[0]));
                    } else throw new IllegalArgumentException("unknown phase " + phase);
                }
            }
            System.out.println("CMR12_WORKER_PASS phase=" + phase + " round=" + round + " pid=" + ProcessHandle.current().pid());
        }
    }

    private static Process start(String phase, Path directory, int round, List<Process> children) throws Exception {
        String cp = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-ea",
                "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"), "-cp", cp,
                CommittedBootstrapCrashExampleTest.class.getName(), phase, directory.toString(), Integer.toString(round))
                .redirectErrorStream(true).redirectOutput(directory.resolve(phase + "-" + round + ".log").toFile()).start();
        children.add(child); return child;
    }
    private static void bounded(Process child, Path log, long deadline) throws Exception {
        assertTrue("worker deadline: " + log, System.nanoTime() < deadline);
        assertTrue("worker log budget: " + log, Files.size(log) <= MAX_LOG);
    }
    private static void reap(Process child) throws Exception {
        if (child.isAlive()) child.destroyForcibly();
        assertTrue("owned child reaped", child.waitFor(10, TimeUnit.SECONDS));
    }
    private static Map<String,Object> execute(String phase, Path directory, int round, boolean crash, List<Process> children) throws Exception {
        Process child = start(phase, directory, round, children);
        Path log = directory.resolve(phase + "-" + round + ".log"), ready = directory.resolve(phase + "-" + round + ".ready.json");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        try {
            if (crash) {
                while (!Files.exists(ready)) {
                    assertTrue("exited before SQL/commit barrier: " + log, child.isAlive());
                    bounded(child, log, deadline); Thread.sleep(20);
                }
                var state = JSON.readTree(Files.readString(ready));
                assertEquals(child.pid(), state.path("pid").asLong()); assertEquals(phase, state.path("phase").asText());
                assertEquals(round, state.path("round").asInt()); assertTrue(child.isAlive());
                if (phase.equals("target_apply")) assertEquals(1, state.path("visibleRows").asLong());
                if (phase.equals("retain") || phase.equals("source_ack")) assertEquals(1, state.path("visibleRows").asLong());
                if (phase.equals("tail_apply")) assertEquals(baseline(round) + 2, state.path("visibleRows").asLong());
                child.destroyForcibly(); reap(child); assertEquals("SIGKILL, not orderly close", 137, child.exitValue());
            } else {
                while (child.isAlive()) { bounded(child, log, deadline); Thread.sleep(20); }
                assertEquals("worker failure: " + log, 0, child.exitValue());
                assertTrue(Files.readString(log).contains("CMR12_WORKER_PASS phase=" + phase + " round=" + round));
            }
            assertTrue(Files.size(log) <= MAX_LOG);
            return Map.of("phase", phase, "round", round, "pid", child.pid(), "exit", child.exitValue(), "killed", crash,
                    "logSha256", digest(Files.readString(log)), "observed", observed(directory, round));
        } finally { reap(child); }
    }
    private static void removeOwnedFixture(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    @Test public void bootstrapAndTailSurviveSevenRealCrashBoundariesTwiceWithoutSourceReset() throws Exception {
        assertTrue("controlled Linux SIGKILL gate", System.getProperty("os.name").startsWith("Linux"));
        String sourceRoot = System.getProperty("cmr12.source.root");
        if (sourceRoot != null) for (Class<?> type : List.of(SqliteCommittedSnapshotPublisher.class,
                SqliteCommittedSnapshot.class, SqliteCommittedOutboxPublisher.class, TeaQLRuntime.class)) {
            String origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().toString();
            assertTrue("archive source origin: " + origin, origin.startsWith(sourceRoot + "/") && origin.endsWith("/target/classes"));
            System.out.println("CMR12_PROCESS_SOURCE_ORIGIN class=" + type.getName() + " path=" + origin);
        }
        String configured = System.getenv("TEAQL_CMR12_EVIDENCE_DIR");
        Path directory = configured == null ? Files.createTempDirectory("teaql-cmr12-java-") : Path.of(configured).toAbsolutePath();
        Files.createDirectories(directory); assertFalse("fresh fixture only; never reset an old DB", Files.exists(directory.resolve("source.sqlite")));
        writeNew(directory.resolve("owner.json"), Map.of("pid", ProcessHandle.current().pid(), "scope", "cmr12-java-process-example"));
        List<Process> children = new ArrayList<>(); List<Map<String,Object>> workers = new ArrayList<>(); boolean passed = false;
        try {
            for (int round = 1; round <= 2; round++) {
                workers.add(execute("seed", directory, round, false, children));
                sourceState(directory, round, false, false, false, false);
                for (String phase : List.of("capture", "retain")) {
                    workers.add(execute(phase, directory, round, true, children));
                    sourceState(directory, round, false, false, false, false);
                    assertFalse(Files.exists(directory.resolve("snapshot-" + round + ".json")));
                }
                workers.add(execute("prepare", directory, round, false, children));
                sourceState(directory, round, true, false, false, false);
                workers.add(execute("tail", directory, round, false, children));
                sourceState(directory, round, true, false, true, false);
                workers.add(execute("target_apply", directory, round, true, children));
                targetState(directory, round, false, false); sourceState(directory, round, true, false, true, false);
                workers.add(execute("snapshot_receipt_lost", directory, round, true, children));
                targetState(directory, round, true, false); sourceState(directory, round, true, false, true, false);
                workers.add(execute("source_ack", directory, round, true, children));
                targetState(directory, round, true, false); sourceState(directory, round, true, false, true, false);
                workers.add(execute("ack", directory, round, false, children));
                sourceState(directory, round, true, true, true, false);
                workers.add(execute("tail_apply", directory, round, true, children));
                targetState(directory, round, true, false); sourceState(directory, round, true, true, true, false);
                workers.add(execute("tail_receipt_lost", directory, round, true, children));
                targetState(directory, round, true, true); sourceState(directory, round, true, true, true, false);
                workers.add(execute("tail_ack", directory, round, false, children));
                workers.add(execute("verify", directory, round, false, children));
                targetState(directory, round, true, true); sourceState(directory, round, true, true, true, true);
                System.out.println("CMR12_PROCESS_PASS language=java round=" + round + " baseline=" + baseline(round)
                        + " tail=" + (baseline(round) + 1) + " killed=7 same_source_database=1 original_bytes=1");
            }
            workers.add(execute("catchup", directory, 1, false, children));
            assertEquals(workers.size(), workers.stream().map(w -> w.get("pid")).distinct().count());
            try (Fixture source = fixture(directory, "read", 2, true, false);
                 Fixture first = fixture(directory, "read", 1, false, false);
                 Fixture second = fixture(directory, "read", 2, false, false)) {
                assertEquals(4, Files.readAllLines(directory.resolve("business-calls.txt")).size());
                assertEquals(2, count(source, "teaql_event_snapshot_publication"));
                assertEquals(4, source.publisher(1).status().ackedSequence()); assertEquals(4, source.publisher(2).status().ackedSequence());
                assertEquals(4, checkpoint(first)); assertEquals(4, checkpoint(second));
                assertEquals(3, count(first, "teaql_event_inbox")); assertEquals(1, count(second, "teaql_event_inbox"));
                assertEquals(source.sql.queryForList("SELECT * FROM order_data ORDER BY id", new Object[0]), first.sql.queryForList("SELECT * FROM order_data ORDER BY id", new Object[0]));
            }
            writeNew(directory.resolve("result.json"), Map.of("passed", true, "workers", workers, "rounds", 2, "kills", 14,
                    "sameSourceDatabase", true, "sourceHighWatermark", 4, "businessCalls", 4, "targetCheckpoints", List.of(4,4),
                    "scope", "controlled Linux SQLite process crash; not power loss, installed artifacts or full CMR-12 acceptance"));
            passed = true;
        } finally {
            for (Process child : children) reap(child);
            if (configured == null && passed) removeOwnedFixture(directory);
            else System.out.println("CMR12_PROCESS_EVIDENCE language=java path=" + directory);
        }
    }

    @Test public void normalExitWithoutBarrierIsRejected() throws Exception {
        Path directory = Files.createTempDirectory("teaql-cmr12-java-negative-"); List<Process> children = new ArrayList<>();
        try {
            assertThrows(AssertionError.class, () -> execute("premature", directory, 1, true, children));
            assertFalse(Files.exists(directory.resolve("source.sqlite")));
            assertFalse(Files.exists(directory.resolve("premature-1.ready.json")));
        } finally { for (Process child : children) reap(child); removeOwnedFixture(directory); }
    }
}
