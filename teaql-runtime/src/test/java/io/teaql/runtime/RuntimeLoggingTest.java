package io.teaql.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.DataServiceOperation;
import io.teaql.core.ExecutionMetadata;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RuntimeLoggingTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final ObjectMapper json = new ObjectMapper();
    private RuntimeLogger logger(Map<String, String> env) {
        return new RuntimeLogger(new RuntimeLogger.Options(null, List.of(), env, temporary.getRoot().toPath()));
    }
    private List<JsonNode> records(Path... files) throws Exception {
        List<JsonNode> result = new ArrayList<>();
        for (Path file : files) {
            String text = Files.readString(file);
            assertTrue("Whole JSON lines", text.endsWith("\n"));
            for (String line : text.stripTrailing().split("\n")) {
                JsonNode record = json.readTree(line);
                for (String key : List.of("timestamp", "level", "log_name", "runtime", "event", "message")) assertFalse(record.get(key).asText().isEmpty());
                assertEquals("java", record.get("runtime").asText());
                Instant.parse(record.get("timestamp").asText()); assertTrue(record.get("timestamp").asText().endsWith("Z"));
                result.add(record);
            }
        }
        return result;
    }

    @Test public void defaultFileBeforeFirstQueryAndAppendAcrossTwoStarts() throws Exception {
        var first = logger(Map.of());
        assertEquals(temporary.getRoot().toPath().resolve("logs/teaql.log"), first.path());
        assertEquals(50L * 1024 * 1024, first.maxSize()); assertEquals(7, first.maxFiles());
        assertEquals("runtime.start", records(first.path()).get(0).get("event").asText());
        var second = logger(Map.of()); assertTrue(second.write("INFO", "probe", "Completed", Map.of()));
        assertEquals(3, records(first.path()).size());
    }

    @Test public void namePrecedenceUnicodeOverrideAndMultipleModels() throws Exception {
        List<Object[]> cases = List.of(new Object[]{"application", "domain", List.of("model"), "application"},
            new Object[]{null, "domain", List.of("model"), "domain"}, new Object[]{null, "", List.of("model"), "model"},
            new Object[]{null, "", List.of("one", "two"), "teaql"}, new Object[]{"订单", "", List.of(), "订单"});
        int index = 0;
        for (Object[] test : cases) {
            Path base = temporary.newFolder("application" + index++).toPath();
            @SuppressWarnings("unchecked") List<String> models = (List<String>) test[2];
            var log = new RuntimeLogger(new RuntimeLogger.Options((String) test[0], models,
                    Map.of("TEAQL_DOMAIN", (String) test[1], "TEAQL_LOG_ENDPOINT", "other/output.jsonl"), base));
            assertEquals(test[3], log.logName()); assertEquals(base.resolve("other/output.jsonl"), log.path());
            assertEquals(test[3], records(log.path()).get(0).get("log_name").asText());
        }
    }

    @Test public void invalidNamesAndConfigurationFailStartupWithoutEchoingInput() throws Exception {
        for (String name : List.of("", ".", "..", "../secret", "a/b", "a\\b", "bad\nname", "NUL", "COM1.txt", "trailing.", "trailing ")) {
            try { new RuntimeLogger(new RuntimeLogger.Options(name, List.of(), Map.of(), temporary.getRoot().toPath())); fail("Accepted invalid name"); }
            catch (IllegalStateException expected) { assertEquals("Runtime logging initialization failed", expected.getMessage()); assertNull(expected.getCause()); }
        }
        for (String size : List.of("0", "-1", "1.2M", "1TB", "999999999999999999999G")) {
            try { logger(Map.of("TEAQL_LOG_MAX_SIZE", size)); fail("Accepted invalid size"); } catch (IllegalStateException expected) { }
        }
        for (String files : List.of("-1", "invalid", "1.5", "2147483648")) {
            try { logger(Map.of("TEAQL_LOG_MAX_FILES", files)); fail("Accepted invalid retention"); } catch (IllegalStateException expected) { }
        }
        assertEquals(1, RuntimeLogger.parseSize("1")); assertEquals(2048, RuntimeLogger.parseSize("2K")); assertEquals(3072, RuntimeLogger.parseSize("3KB"));
        assertEquals(2097152, RuntimeLogger.parseSize("2m")); assertEquals(1048576, RuntimeLogger.parseSize("1MB"));
        assertEquals(1073741824, RuntimeLogger.parseSize("1G")); assertEquals(1073741824, RuntimeLogger.parseSize("1GB"));
    }

    @Test public void utf8OversizedRecordsArchiveOrderAndZeroRetention() throws Exception {
        var log = logger(Map.of("TEAQL_LOG_MAX_SIZE", "256", "TEAQL_LOG_MAX_FILES", "2"));
        for (int i = 0; i < 5; i++) assertTrue(log.write("INFO", "probe", "界".repeat(200), Map.of("sequence", i)));
        for (int i = 0; i < 3; i++) {
            var record = records(Path.of(log.path() + (i == 0 ? "" : "." + i)));
            assertEquals(1, record.size()); assertEquals(4 - i, record.get(0).get("sequence").asInt()); assertEquals("界".repeat(200), record.get(0).get("message").asText());
        }
        assertFalse(Files.exists(Path.of(log.path() + ".3")));
        var zero = logger(Map.of("TEAQL_LOG_MAX_SIZE", "256", "TEAQL_LOG_MAX_FILES", "0"));
        zero.write("INFO", "probe", "x".repeat(400), Map.of());
        try (var files = Files.list(zero.path().getParent())) { assertEquals(1, files.count()); }
        assertEquals(1, records(zero.path()).size());
    }

    @Test public void utcDayRotationAndRestart() throws Exception {
        for (boolean restart : List.of(false, true)) {
            var base = temporary.newFolder("day-" + restart).toPath();
            var options = new RuntimeLogger.Options(null, List.of(), Map.of(), base);
            var log = new RuntimeLogger(options); byte[] before = Files.readAllBytes(log.path());
            Files.setLastModifiedTime(log.path(), FileTime.from(Instant.now().minusSeconds(86400)));
            if (restart) log = new RuntimeLogger(options); else assertTrue(log.write("INFO", "next.day", "Next day", Map.of()));
            assertArrayEquals(before, Files.readAllBytes(Path.of(log.path() + ".1"))); assertEquals(1, records(log.path()).size());
        }
    }

    @Test public void ordinaryUtf8RecordsRespectWholeLineByteLimit() throws Exception {
        var log = logger(Map.of("TEAQL_LOG_MAX_SIZE", "1K", "TEAQL_LOG_MAX_FILES", "1000"));
        for (int i = 0; i < 40; i++) assertTrue(log.write("INFO", "regular", "界".repeat(30), Map.of("index", i)));
        Path[] files;
        try (var paths = Files.list(log.path().getParent())) { files = paths.toArray(Path[]::new); }
        assertTrue(files.length > 1);
        for (Path file : files) assertTrue(Files.size(file) > 0 && Files.size(file) <= 1024);
        var all = records(files); assertEquals(41, all.size());
        var ids = new HashSet<Integer>();
        for (var row : all) if (row.has("index")) assertTrue(ids.add(row.get("index").asInt()));
        assertEquals(40, ids.size());
    }

    @Test public void rotationFailurePreservesForeignArchiveAndBusinessOutcome() throws Exception {
        var log = logger(Map.of()); byte[] before = Files.readAllBytes(log.path());
        Path archive = Path.of(log.path() + ".1"); Files.createDirectory(archive);
        Files.writeString(archive.resolve("marker"), "preserve");
        Files.setLastModifiedTime(log.path(), FileTime.from(Instant.now().minusSeconds(86400)));
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();
        var context = new DefaultUserContext(runtime); context.putToLocalCache("probe", 42, 60);
        assertEquals(Integer.valueOf(42), context.getFromLocalCache("probe", Integer.class));
        var original = new java.io.IOException("private-original-error");
        RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), new RuntimeTelemetry.Operation("query", "probe", Map.of())).failure(original);
        assertEquals("private-original-error", original.getMessage());
        assertFalse(log.write("INFO", "probe", "Completed", Map.of()));
        assertArrayEquals(before, Files.readAllBytes(log.path()));
        assertEquals("preserve", Files.readString(archive.resolve("marker")));
    }

    @Test public void nativeCheckAndIntentRejectionsAreWarnings() throws Exception {
        var log = logger(Map.of());
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();
        var context = new DefaultUserContext(runtime);
        RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), new RuntimeTelemetry.Operation("mutation", "probe", Map.of()))
                .failure(new io.teaql.core.checker.CheckException("private-validation-value"));
        var scope = RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), new RuntimeTelemetry.Operation("query", "probe", Map.of()));
        try { io.teaql.core.QueryIntent.of("read probe", ""); fail("Intent must be rejected"); }
        catch (io.teaql.core.RequestIntentException expected) { scope.failure(expected); }
        var all = records(log.path()); assertEquals(3, all.size());
        for (int i = 1; i < all.size(); i++) { assertEquals("WARN", all.get(i).get("level").asText()); assertEquals("validation", all.get(i).get("error_code").asText()); }
        assertFalse(Files.readString(log.path()).contains("private-validation-value"));
    }

    @Test public void sqlFileKeepsAvailableTraceCorrelation() throws Exception {
        var log = logger(Map.of());
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();
        var metadata = new ExecutionMetadata(); metadata.setOperation(DataServiceOperation.QUERY); metadata.setExecutionOutcome("success");
        metadata.setSqlExecutionCorrelation(new io.teaql.core.SqlExecutionCorrelation("0123456789abcdef0123456789abcdef", "0123456789abcdef"));
        runtime.recordExecutionMetadata(new DefaultUserContext(runtime), metadata);
        var all = records(log.path()); assertEquals(2, all.size());
        assertEquals("0123456789abcdef0123456789abcdef", all.get(1).get("trace_id").asText());
    }

    @Test public void nativePolicyRejectionKeepsBusinessAndSdkErrorIdentity() throws Exception {
        var log = logger(Map.of());
        var captured = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        RuntimeTelemetry adapter = operation -> new RuntimeTelemetry.Scope() {
            @Override public void success(Map<String, Object> attributes) { }
            @Override public void failure(Throwable error) { captured.set(error); }
        };
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).telemetry(adapter).build();
        var rejection = RuntimeLoggingTelemetry.policyRejection("private-policy-value");
        assertEquals(io.teaql.core.TeaQLRuntimeException.class, rejection.getClass());
        var operation = new RuntimeTelemetry.Operation("mutation", "probe", Map.of());
        RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), operation).failure(rejection);
        assertSame(rejection, captured.get()); assertEquals("private-policy-value", rejection.getMessage());
        var unrelated = new io.teaql.core.TeaQLRuntimeException("[MUTATION POLICY DENIED] private-unrelated-value");
        RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), operation).failure(unrelated);
        assertSame(unrelated, captured.get()); assertSame(adapter, runtime.getTelemetry());
        var all = records(log.path()); assertEquals(3, all.size());
        assertEquals("WARN", all.get(1).get("level").asText()); assertEquals("ERROR", all.get(2).get("level").asText());
        assertFalse(Files.readString(log.path()).contains("private-"));
    }

    @Test public void startupFailureAndPostStartupPreserveBusinessResultAndError() throws Exception {
        Path active = temporary.getRoot().toPath().resolve("logs/teaql.log"); Files.createDirectories(active);
        try { logger(Map.of()); fail("Startup must fail"); } catch (IllegalStateException expected) { }
        Files.delete(active); var log = logger(Map.of());
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();
        var context = new DefaultUserContext(runtime); context.putToLocalCache("private-cache-key", 42, 60);
        Files.delete(active); Files.createDirectory(active);
        assertEquals(Integer.valueOf(42), context.getFromLocalCache("private-cache-key", Integer.class));
        var original = new IllegalStateException("private-original-message");
        var scope = RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), new RuntimeTelemetry.Operation("query", "probe", Map.of()));
        scope.failure(original); assertEquals("private-original-message", original.getMessage());
        assertFalse(log.write("INFO", "probe", "Completed", Map.of()));
    }

    private static class ValidationException extends RuntimeException { ValidationException(String text) { super(text); } }
    @Test public void nativeOutcomeSeveritySafeSqlAndExtensionIdentity() throws Exception {
        var log = logger(Map.of()); RuntimeTelemetry adapter = RuntimeTelemetry.NOOP;
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).telemetry(adapter).build();
        assertSame(adapter, runtime.getTelemetry());
        for (Throwable error : Arrays.asList(null, new ValidationException("private-error-message"), new java.io.IOException("private-error-message"))) {
            var scope = RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(), new RuntimeTelemetry.Operation("query", "probe", Map.of()));
            if (error == null) scope.success(); else scope.failure(error); scope.success();
        }
        var metadata = new ExecutionMetadata(); metadata.setOperation(DataServiceOperation.QUERY); metadata.setDebugQuery("unprojected-sql-secret"); metadata.setExecutionOutcome("retrying");
        runtime.recordExecutionMetadata(new DefaultUserContext(runtime), metadata);
        var all = records(log.path()); assertEquals(5, all.size());
        assertEquals("INFO", all.get(1).get("level").asText()); assertEquals("WARN", all.get(2).get("level").asText());
        assertEquals("ERROR", all.get(3).get("level").asText()); assertEquals("WARN", all.get(4).get("level").asText()); assertEquals("SQL", all.get(4).get("category").asText());
        String text = Files.readString(log.path()); assertFalse(text.contains("private-error-message")); assertFalse(text.contains("unprojected-sql-secret"));
    }

    private void assertConcurrent(int events, int starts) throws Exception {
        List<JsonNode> all;
        try (var paths = Files.list(temporary.getRoot().toPath().resolve("logs"))) { all = records(paths.toArray(Path[]::new)); }
        Set<String> ids = new HashSet<>();
        for (JsonNode record : all) if (record.has("id")) assertTrue(ids.add(record.get("id").asText()));
        assertEquals(events, ids.size()); assertEquals(events + starts, all.size());
    }

    @Test public void concurrentStartupRotationAndReopening() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> children = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) { int id = worker;
                children.add(pool.submit(() -> { var log = logger(Map.of("TEAQL_LOG_MAX_SIZE", "1K", "TEAQL_LOG_MAX_FILES", "1000"));
                    for (int i = 0; i < 30; i++) assertTrue(log.write("INFO", "concurrent", "Completed", Map.of("id", id + "/" + i))); }));
            }
            for (var child : children) child.get(60, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
        assertConcurrent(240, 8);
    }

    public static class Writer {
        public static void main(String[] arguments) {
            var log = new RuntimeLogger(new RuntimeLogger.Options(null, List.of(), Map.of("TEAQL_LOG_MAX_SIZE", "1K", "TEAQL_LOG_MAX_FILES", "1000"), Path.of(arguments[0])));
            for (int i = 0; i < 30; i++) if (!log.write("INFO", "concurrent", "Completed", Map.of("id", arguments[1] + "/" + i))) System.exit(1);
        }
    }
    @Test public void independentProcessesCoordinateWholeLines() throws Exception {
        List<Process> children = new ArrayList<>();
        try {
            // Surefire keeps production modules off java.class.path in a
            // JPMS run. This independent probe loads both sets on its own
            // classpath, without inheriting Surefire's patched-module flags.
            String classPath = System.getProperty("java.class.path") + java.io.File.pathSeparator
                    + System.getProperty("jdk.module.path", "");
            for (int worker = 0; worker < 4; worker++) {
                var command = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", classPath, Writer.class.getName(), temporary.getRoot().toString(), Integer.toString(worker)).inheritIO();
                command.environment().clear(); children.add(command.start());
            }
            for (Process child : children) { assertTrue(child.waitFor(60, TimeUnit.SECONDS)); assertEquals(0, child.exitValue()); }
        } finally { for (Process child : children) { if (child.isAlive()) child.destroyForcibly(); child.waitFor(); } }
        assertConcurrent(120, 4);
    }
}
