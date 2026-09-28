package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/** Acceptance contract, not a snapshot of the current implementation. */
public class MaskingContractTest {
    @Rule public ErrorCollector errors = new ErrorCollector();
    private static final String[][] CASES = {
        {"", ""}, {"Ada", "***"}, {"12345678", "********"},
        {"ABCDEFGH", "AB****GH"}, {"Riverside", "Ri*****de"}, {"O'Reilly", "O'****ly"}
    };

    @Test public void legacyMaskAlgorithmMustRemainAvailable() {
        for (var c : CASES) assertEquals(c[0], c[1], TeaQLRuntime.maskAuditValue(c[0]));
    }

    @Test public void sharedUnicodeGoldenVectors() throws Exception {
        var fixture = java.nio.file.Path.of("../test-vectors/masking-v1.tsv");
        for (String line : java.nio.file.Files.readAllLines(fixture).subList(1, 11)) {
            var values = line.split("\t", -1);
            errors.checkThat(values[0], TeaQLRuntime.maskAuditValue(values[1]), is(values[2]));
        }
    }

    @Test public void defaultSqlMustRemainExpandedWithUnknownParametersMasked() {
        for (var c : CASES) {
            var source = entry(c[0]);
            var bytes = new ByteArrayOutputStream();
            new DefaultTextRuntimeLogSink(new PrintStream(bytes)).writeExecutionLog(null, source);
            String log = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(c[0], source.getParameters().get(0));
            // No field policy is attached: unknown provenance must not expose a prefix/suffix.
            errors.checkThat(log, log.contains("name = '"), is(true));
            errors.checkThat(log, log.toLowerCase().contains("masked"), is(true));
            errors.checkThat(log, log.contains("name = ?"), is(false));
            errors.checkThat(log, log.contains("[REDACTED SQL"), is(false));
            if (!c[0].isEmpty()) errors.checkThat(log, log.contains("'" + c[0].replace("'", "''") + "'"), is(false));
            if (c[0].length() >= 8 && !c[1].startsWith("*")) errors.checkThat(log, log.contains(c[1].replace("'", "''")), is(false));
        }
    }

    @Test public void debugSinkMustLabelEveryPlaintextRecord() throws Exception {
        var process = new ProcessBuilder(
            java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", childClasspath(), MaskingContractTest.class.getName());
        process.environment().put(LogPrivacy.ENVIRONMENT, LogPrivacy.ACKNOWLEDGEMENT);
        var output = java.nio.file.Files.createTempFile("mask-contract-", ".log");
        try {
            var child = process.redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!child.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                child.destroyForcibly(); fail("log child timed out");
            }
            String text = java.nio.file.Files.readString(output);
            assertEquals(text, 0, child.exitValue());
            String[] records = text.split("RECORD_BOUNDARY", -1);
            assertEquals(text, 3, records.length);
            for (int i = 1; i < records.length; i++) {
                errors.checkThat(records[i], records[i].contains("'Riverside'"), is(true));
                errors.checkThat(records[i], records[i].toUpperCase().contains("DEBUG"), is(true));
                errors.checkThat(records[i], records[i].toUpperCase().contains("PLAINTEXT"), is(true));
            }
        } finally { java.nio.file.Files.deleteIfExists(output); }
    }

    public static void main(String[] args) {
        var sink = new SensitiveDiagnosticTextRuntimeLogSink(System.out);
        for (int i = 0; i < 2; i++) {
            System.out.println("RECORD_BOUNDARY");
            var business = entry("Riverside");
            business.setParameterLogPolicies(List.of(io.teaql.core.SqlParameterLogPolicy.MASKED));
            sink.writeExecutionLog(null, business);
        }
    }

    private static String childClasspath() throws Exception {
        var paths = new java.util.ArrayList<String>();
        paths.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        for (Class<?> type : List.of(MaskingContractTest.class, LogPrivacy.class, ExecutionMetadata.class,
                io.teaql.core.utils.SqlLogRenderer.class))
            paths.add(java.nio.file.Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        return String.join(java.io.File.pathSeparator, paths);
    }

    private static ExecutionMetadata entry(String value) {
        var m = new ExecutionMetadata();
        m.setParameterizedQuery("UPDATE customer SET name = ?");
        m.setParameters(List.of(value));
        m.setDebugQuery("UPDATE customer SET name = '" + value.replace("'", "''") + "'");
        m.setComment("what: edit customer"); m.setPurpose("why: verify mask contract");
        return m;
    }
}
