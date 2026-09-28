package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogPrivacyTest {
    @Test public void realProcessEnvironmentControlsFileOutput() throws Exception {
        for (String setting : new String[] { "", "true", LogPrivacy.ACKNOWLEDGEMENT + " ", LogPrivacy.ACKNOWLEDGEMENT }) {
            var output = Files.createTempFile("teaql-log-process-", ".log");
            var process = new ProcessBuilder(
                    java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", childClasspath(),
                    LogPrivacyTest.class.getName());
            process.environment().remove(LogPrivacy.ENVIRONMENT);
            if (!setting.isEmpty()) process.environment().put(LogPrivacy.ENVIRONMENT, setting);
            process.redirectErrorStream(true).redirectOutput(output.toFile());
            var child = process.start();
            assertTrue("privacy child timed out", child.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
            String text = Files.readString(output);
            assertEquals(text, 0, child.exitValue());
            boolean enabled = setting.equals(LogPrivacy.ACKNOWLEDGEMENT);
            assertEquals(text, enabled, text.contains("PRIVATE-CUSTOMER-CANARY"));
            assertEquals(text, enabled, text.contains("may be written to disk"));
            assertFalse(text, text.contains("PASSWORD-CANARY"));
        }
    }

    public static void main(String[] args) {
        var sink = new SensitiveDiagnosticTextRuntimeLogSink(System.out);
        var business = entry("name", "PRIVATE-CUSTOMER-CANARY");
        business.setParameterLogPolicies(List.of(io.teaql.core.SqlParameterLogPolicy.MASKED));
        sink.writeExecutionLog(null, business);
        sink.writeExecutionLog(null, entry("password", "PASSWORD-CANARY"));
    }

    private static String childClasspath() throws Exception {
        var paths = new java.util.ArrayList<String>();
        paths.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        // Surefire places named runtime modules on a module path, not necessarily
        // in java.class.path. Locate the actually loaded local classes explicitly.
        for (Class<?> type : List.of(LogPrivacyTest.class, LogPrivacy.class, ExecutionMetadata.class,
                io.teaql.core.utils.SqlLogRenderer.class))
            paths.add(java.nio.file.Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        return String.join(java.io.File.pathSeparator, paths);
    }

    @Test public void nestedAuditCredentialsAndTraceStayMaskedUnderOptIn() {
        var oldValue = java.util.Map.of("access_token", "OLD-TOKEN-CANARY");
        var newValue = java.util.Map.of("access_token", "NEW-TOKEN-CANARY");
        var source = new RawAuditEvent(MutationAuditKind.UPDATED, "Customer", 1L,
                List.of(new AuditFieldChange("settings", oldValue, newValue)),
                List.of(new TraceNode("OLD-TOKEN-CANARY became NEW-TOKEN-CANARY")),
                "operator", "mutation", "replace NEW-TOKEN-CANARY", 2L, null);
        var safe = LogPrivacy.audit(source, true);
        assertEquals("[REDACTED]", safe.changes().get(0).newValue());
        assertFalse(safe.toString().contains("TOKEN-CANARY"));
        assertEquals(newValue, source.changes().get(0).newValue());
    }

    @Test public void projectionIsIndependentAndCredentialsStayHidden() {
        ExecutionMetadata source = entry("name", "PRIVATE-CUSTOMER-CANARY");
        ExecutionMetadata safe = LogPrivacy.sql(source, false);
        assertEquals(LogPrivacy.REDACTED, safe.getParameters().get(0));
        assertFalse(safe.getComment().contains("PRIVATE-CUSTOMER-CANARY"));
        assertFalse(safe.getTraceChain().toString().contains("PRIVATE-CUSTOMER-CANARY"));
        assertEquals("PRIVATE-CUSTOMER-CANARY", source.getParameters().get(0));
        assertEquals(List.of(LogPrivacy.REDACTED), LogPrivacy.sql(source, true).getParameters());
        source.setParameterLogPolicies(List.of(io.teaql.core.SqlParameterLogPolicy.MASKED));
        assertEquals(source.getParameters(), LogPrivacy.sql(source, true).getParameters());
        ExecutionMetadata credential = LogPrivacy.sql(entry("password", "PASSWORD-CANARY"), true);
        assertEquals(LogPrivacy.REDACTED, credential.getParameters().get(0));
        assertTrue(credential.getDebugQuery().contains("/* masked */"));
        assertFalse(credential.getDebugQuery().contains("PASSWORD-CANARY"));
    }

    @Test public void directFileSinkCannotLeakCredentials() throws Exception {
        var file = Files.createTempFile("teaql-privacy-", ".log");
        try (var output = new PrintStream(Files.newOutputStream(file))) {
            new SensitiveDiagnosticTextRuntimeLogSink(output).writeExecutionLog(null, entry("password", "PASSWORD-CANARY"));
        }
        assertFalse(Files.readString(file).contains("PASSWORD-CANARY"));
    }

    @Test public void literalSqlIsNotPresentedAsReplayable() {
        ExecutionMetadata source = entry("name", "PRIVATE-CUSTOMER-CANARY");
        source.setParameterizedQuery(source.getDebugQuery());
        assertEquals("[REDACTED SQL; NOT REPLAYABLE]", LogPrivacy.sql(source, false).getParameterizedQuery());
    }

    @Test public void mutatedDebugRecordIsSafeInDefaultFileSink() throws Exception {
        var source = entry("name", "PRIVATE-CUSTOMER-CANARY");
        source.setParameterLogPolicies(List.of(io.teaql.core.SqlParameterLogPolicy.MASKED));
        source.setGeneratedSql(true);
        var debug = LogPrivacy.sql(source, true);
        debug.setParameterizedQuery("SELECT id FROM customer WHERE id=? LIMIT 10000");
        debug.setParameters(List.of(1L));
        debug.setParameterLogPolicies(List.of(io.teaql.core.SqlParameterLogPolicy.PLAIN));
        var file = Files.createTempFile("teaql-debug-downgrade-", ".log");
        try (var out = new PrintStream(Files.newOutputStream(file))) {
            new DefaultTextRuntimeLogSink(out).writeExecutionLog(null, debug);
        }
        var text = Files.readString(file);
        assertFalse(text, text.contains("PRIVATE-CUSTOMER-CANARY"));
        assertTrue(text, text.contains("LIMIT 10000"));
        assertTrue(text, text.contains("[REDACTED]"));
        assertTrue(debug.getComment().contains("PRIVATE-CUSTOMER-CANARY"));
    }

    private static ExecutionMetadata entry(String field, String value) {
        var metadata = new ExecutionMetadata();
        metadata.setParameterizedQuery("select * from customer where " + field + " = ?");
        metadata.setDebugQuery("select * from customer where " + field + " = '" + value + "'");
        metadata.setParameters(List.of(value));
        metadata.setComment("load " + value);
        metadata.setTraceChain(List.of(new TraceNode("load " + value)));
        return metadata;
    }
}
