package io.teaql.examples.committed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteCommittedEventStore;
import io.teaql.core.sqlite.SqliteCommittedMutationRecorder;
import io.teaql.core.sqlite.SqliteCommittedOutboxPublisher;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.sqlite.SqliteModelCheckedCommittedTarget;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/** CMR-03: real source-process death, not an orderly in-process reopen. */
public class CommittedProcessCrashExampleTest {
    private static final String SOURCE = "cmr03-java-source";
    private static final String TARGET = "cmr03-java-target";
    private static final String MODEL = "sha256:" + "0".repeat(64);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration BUDGET = Duration.ofSeconds(30);
    private static final long MAX_CHILD_LOG = 256 * 1024;

    private record Fixture(JdbcSqlExecutor sql, DefaultUserContext context,
            TeaQLRuntime runtime) implements AutoCloseable {
        @Override public void close() { runtime.close(); }
    }

    private static Fixture fixture(Path file, boolean capture, int round) {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + file);
        JdbcSqlExecutor sql = new JdbcSqlExecutor(ds);
        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType("Order");
        descriptor.setTargetType(CommittedEventsExampleTest.Order.class);
        descriptor.setEntitySupplier(CommittedEventsExampleTest.Order::new);
        descriptor.setDataService("sqlite");
        for (String field : new String[] {"id", "version", "name", "memo"}) {
            boolean text = field.equals("name") || field.equals("memo");
            GenericSQLProperty property = (GenericSQLProperty) descriptor.addSimpleProperty(
                    field, text ? String.class : Long.class);
            property.setColumnType(text ? "VARCHAR(255)" : "BIGINT");
        }
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        metadata.register(descriptor);
        TeaQLRuntime.Builder builder = TeaQLRuntime.builder().metadata(metadata)
                .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", sql, ds))
                .executionLogging(false)
                .idGenerationService((context, entity) -> {
                    if (!capture) throw new AssertionError("replication must not allocate entity IDs");
                    return 100L + round;
                });
        if (capture) builder.committedMutationRecorder(new SqliteCommittedMutationRecorder(sql, SOURCE, MODEL));
        TeaQLRuntime runtime = builder.build();
        return new Fixture(sql, new DefaultUserContext(runtime), runtime);
    }

    private static SqliteCommittedOutboxPublisher publisher(JdbcSqlExecutor sql) {
        return new SqliteCommittedOutboxPublisher(sql, SOURCE, TARGET, MODEL);
    }

    private static long count(JdbcSqlExecutor sql, String trustedTable) {
        return ((Number) sql.queryForList("SELECT COUNT(*) AS n FROM " + trustedTable,
                new Object[0]).get(0).get("n")).longValue();
    }

    private static void sourceState(Path directory, int round, int ack) throws Exception {
        try (Fixture source = fixture(directory.resolve("source.sqlite"), false, round)) {
            assertEquals(round, count(source.sql(), "order_data"));
            assertEquals(round, new SqliteCommittedEventStore(source.sql()).outboxAfter(SOURCE, 0, 10).size());
            var status = publisher(source.sql()).status();
            assertEquals(round, status.sourceHighWatermark());
            assertEquals(ack, status.ackedSequence());
            assertEquals(round - ack, status.backlog());
            assertEquals(round, Files.readAllLines(directory.resolve("business-invocations.txt")).size());
        }
    }

    private static void targetState(Fixture target, int round) {
        assertEquals(round, count(target.sql(), "order_data"));
        assertEquals(round, count(target.sql(), "teaql_event_inbox"));
        for (int r = 1; r <= round; r++) {
            var row = target.sql().queryForList("SELECT version, name FROM order_data WHERE id = ?",
                    new Object[] {100L + r}).get(0);
            assertEquals(1L, ((Number) row.get("version")).longValue());
            assertEquals("persisted-" + r, row.get("name"));
        }
    }

    /** Private test worker; not an application endpoint or runtime API. */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("phase directory round required");
        String phase = args[0];
        Path directory = Path.of(args[1]);
        int round = Integer.parseInt(args[2]);
        if (phase.equals("premature")) return; // negative control: no commit/barrier
        if (phase.equals("seed")) {
            try (Fixture source = fixture(directory.resolve("source.sqlite"), true, round)) {
                source.context().ensureSchema();
                Files.writeString(directory.resolve("business-invocations.txt"), "seed-" + round + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                var order = new CommittedEventsExampleTest.Order();
                order.__internalInitializeNewEntityId(100L + round);
                order.updateProperty("name", "persisted-" + round);
                order.auditAs("commit order before controlled source process crash").save(source.context());
                sourceState(directory, round, round - 1);
                Path temporary = directory.resolve("ready-" + round + ".tmp");
                Files.writeString(temporary, JSON.writeValueAsString(Map.of("pid", ProcessHandle.current().pid(),
                        "round", round, "phase", "committed_before_publish")), StandardOpenOption.CREATE_NEW);
                Files.move(temporary, directory.resolve("ready-" + round + ".json"), StandardCopyOption.ATOMIC_MOVE);
                System.out.println("CMR03_BARRIER pid=" + ProcessHandle.current().pid() + " round=" + round);
                System.out.flush();
                for (;;) Thread.sleep(1000); // parent must kill us; do not close runtime normally
            }
        }
        if (!phase.equals("recover") && !phase.equals("verify")) throw new IllegalArgumentException("unknown phase");
        try (Fixture source = fixture(directory.resolve("source.sqlite"), false, round);
             Fixture target = fixture(directory.resolve("target.sqlite"), false, round)) {
            if (phase.equals("recover")) target.context().ensureSchema();
            var apply = new SqliteModelCheckedCommittedTarget(target.sql(), SOURCE, MODEL);
            apply.ensureSchema();
            var record = new SqliteCommittedEventStore(source.sql()).outboxAfter(SOURCE, round - 1, 1).get(0);
            assertEquals(round, record.sourceSequence());
            if (phase.equals("recover")) {
                sourceState(directory, round, round - 1);
                var applied = apply.apply(target.context(), record); // target commit; ack deliberately lost
                assertFalse(applied.duplicate());
                assertEquals(round, applied.checkpoint());
                assertEquals(round - 1, publisher(source.sql()).status().ackedSequence());
                var batch = publisher(source.sql()).publishBatch(1, item -> {
                    var duplicate = apply.apply(target.context(), item);
                    assertTrue(duplicate.duplicate());
                    assertEquals(round, duplicate.checkpoint());
                    return duplicate;
                });
                assertEquals(1, batch.acknowledged());
                assertEquals(round, batch.status().ackedSequence());
                assertEquals(0, batch.status().backlog());
            } else {
                assertTrue(apply.apply(target.context(), record).duplicate()); // durable across another new process
                assertEquals(0, publisher(source.sql()).publishBatch(1, item -> {
                    throw new AssertionError("acked events must not be sent again");
                }).acknowledged());
            }
            sourceState(directory, round, round);
            targetState(target, round);
            System.out.println("CMR03_WORKER_PASS phase=" + phase + " pid=" + ProcessHandle.current().pid()
                    + " round=" + round + " ack=" + round + " inbox=" + round + " business_invocations=" + round);
        }
    }

    private static Process start(String phase, Path directory, int round) throws Exception {
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-ea", "-cp", classpath, CommittedProcessCrashExampleTest.class.getName(), phase,
                directory.toString(), Integer.toString(round))
                .redirectErrorStream(true).redirectOutput(directory.resolve(phase + "-" + round + ".log").toFile()).start();
    }

    private static void checkLog(Path directory, String phase, int round) throws Exception {
        assertTrue("bounded worker log", Files.size(directory.resolve(phase + "-" + round + ".log")) <= MAX_CHILD_LOG);
    }

    private static void stopAndReap(Process child) throws Exception {
        if (child.isAlive()) child.destroyForcibly();
        assertTrue("owned child must be reaped", child.waitFor(10, TimeUnit.SECONDS));
    }

    private static void barrier(Process child, Path directory, int round) throws Exception {
        long deadline = System.nanoTime() + BUDGET.toNanos();
        Path ready = directory.resolve("ready-" + round + ".json");
        while (!Files.exists(ready)) {
            assertTrue("writer exited before durable barrier", child.isAlive());
            assertTrue("writer barrier deadline", System.nanoTime() < deadline);
            checkLog(directory, "seed", round);
            Thread.sleep(20);
        }
        var observed = JSON.readTree(Files.readString(ready));
        assertEquals(child.pid(), observed.path("pid").asLong());
        assertEquals(round, observed.path("round").asInt());
        assertEquals("committed_before_publish", observed.path("phase").asText());
        assertTrue("writer must still be waiting when killed", child.isAlive());
    }

    private static void finished(Process child, Path directory, String phase, int round) throws Exception {
        long deadline = System.nanoTime() + BUDGET.toNanos();
        while (child.isAlive()) {
            assertTrue("worker deadline", System.nanoTime() < deadline);
            checkLog(directory, phase, round);
            Thread.sleep(20);
        }
        assertEquals("worker log: " + directory.resolve(phase + "-" + round + ".log"), 0, child.exitValue());
        checkLog(directory, phase, round);
    }

    @Test public void killedSourceRecoversInFreshProcessesTwiceWithoutDatabaseReset() throws Exception {
        String configured = System.getenv("TEAQL_CMR03_EVIDENCE_DIR");
        Path directory = configured == null ? Files.createTempDirectory("teaql-cmr03-java-") : Path.of(configured).toAbsolutePath();
        Files.createDirectories(directory);
        assertFalse(Files.exists(directory.resolve("source.sqlite"))); // never erase a prior database
        Files.writeString(directory.resolve("owner.json"), JSON.writeValueAsString(Map.of("pid", ProcessHandle.current().pid(), "kind", "cmr03-example")), StandardOpenOption.CREATE_NEW);
        List<Map<String, Object>> rounds = new ArrayList<>();
        List<Process> children = new ArrayList<>();
        try {
            for (int round = 1; round <= 2; round++) {
                Process writer = start("seed", directory, round); children.add(writer);
                barrier(writer, directory, round);
                writer.destroyForcibly(); stopAndReap(writer);
                assertNotEquals("forced source termination", 0, writer.exitValue());
                sourceState(directory, round, round - 1); // independent read after actual process death
                Process recovered = start("recover", directory, round); children.add(recovered);
                finished(recovered, directory, "recover", round);
                Process verified = start("verify", directory, round); children.add(verified);
                finished(verified, directory, "verify", round);
                assertEquals(3, new HashSet<>(List.of(writer.pid(), recovered.pid(), verified.pid())).size());
                rounds.add(Map.of("round", round, "writerPid", writer.pid(), "writerExit", writer.exitValue(),
                        "recoveryPid", recovered.pid(), "verificationPid", verified.pid(), "ack", round,
                        "domainRows", round, "outboxRows", round, "inboxRows", round, "businessInvocations", round));
                System.out.println("CMR03_PROCESS_CRASH_PASS language=java round=" + round + " killed_pid=" + writer.pid()
                        + " killed_exit=" + writer.exitValue() + " recovery_pid=" + recovered.pid() + " verify_pid=" + verified.pid());
            }
            Files.writeString(directory.resolve("result.json"), JSON.writeValueAsString(Map.of("passed", true, "rounds", rounds,
                    "sameDatabases", true, "scope", "controlled SQLite source-process crash, not power failure or snapshot crash")));
        } finally {
            for (Process child : children) stopAndReap(child);
            System.out.println("CMR03_EVIDENCE language=java path=" + directory);
        }
    }

    @Test public void normalWorkerExitWithoutBarrierCannotPass() throws Exception {
        Path directory = Files.createTempDirectory("teaql-cmr03-java-negative-");
        Process child = start("premature", directory, 1);
        // Use the seed log name expected by the barrier observer, without inventing a ready marker.
        Files.writeString(directory.resolve("seed-1.log"), "");
        try { assertThrows(AssertionError.class, () -> barrier(child, directory, 1)); }
        finally { stopAndReap(child); }
        assertFalse(Files.exists(directory.resolve("source.sqlite")));
        assertFalse(Files.exists(directory.resolve("ready-1.json")));
        Files.delete(directory.resolve("seed-1.log"));
        Files.delete(directory.resolve("premature-1.log"));
        Files.delete(directory); // successful negative control has no DB or diagnostic to retain
    }
}
