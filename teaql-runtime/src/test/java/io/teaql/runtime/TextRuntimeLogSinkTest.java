package io.teaql.runtime;

import io.teaql.core.DataServiceOperation;
import io.teaql.core.ExecutionMetadata;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TextRuntimeLogSinkTest {
    private static final String SECRET = "customer-secret";

    @Test
    public void defaultSinkKeepsIntentAndParameterizedSqlButOmitsValues() {
        String text = render(new DefaultTextRuntimeLogSink(output()));

        assertTrue(text.contains("comment=what: load customer"));
        assertTrue(text.contains("purpose=why: render profile"));
        assertTrue(text.contains("WHERE name = ?"));
        assertFalse(text.contains("params="));
        assertFalse(text.contains("Debug SQL:"));
        assertFalse(text.contains(SECRET));
    }

    @Test
    public void sensitiveDiagnosticSinkExplicitlyEmitsValuesAndCopyPasteSql() {
        String text = render(new SensitiveDiagnosticTextRuntimeLogSink(output()));

        assertTrue(text.contains("params=[" + SECRET + "]"));
        assertTrue(text.contains("Debug SQL: SELECT id FROM customer_data WHERE name = '"
                + SECRET + "'"));
    }

    @Test
    public void diagnosticSelectionDoesNotDisableOperationLogging() {
        TeaQLRuntime safe = TeaQLRuntime.builder()
                .metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .diagnosticSqlLogging(false)
                .build();
        TeaQLRuntime sensitive = TeaQLRuntime.builder()
                .metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .queryExecutionLogging(false)
                .mutationExecutionLogging(true)
                .diagnosticSqlLogging(true)
                .build();

        assertTrue(safe.getLogSink() instanceof DefaultTextRuntimeLogSink);
        assertFalse(safe.getLogSink() instanceof SensitiveDiagnosticTextRuntimeLogSink);
        assertTrue(safe.isQueryExecutionLoggingEnabled());
        assertTrue(safe.isMutationExecutionLoggingEnabled());
        assertTrue(sensitive.getLogSink() instanceof SensitiveDiagnosticTextRuntimeLogSink);
        assertFalse(sensitive.isQueryExecutionLoggingEnabled());
        assertTrue(sensitive.isMutationExecutionLoggingEnabled());
    }

    private ByteArrayOutputStream bytes;

    private PrintStream output() {
        bytes = new ByteArrayOutputStream();
        return new PrintStream(bytes, true, StandardCharsets.UTF_8);
    }

    private String render(RuntimeLogSink sink) {
        ExecutionMetadata metadata = new ExecutionMetadata();
        metadata.setOperation(DataServiceOperation.QUERY);
        metadata.setComment("what: load customer");
        metadata.setPurpose("why: render profile");
        metadata.setParameterizedQuery("SELECT id FROM customer_data WHERE name = ?");
        metadata.setParameters(List.of(SECRET));
        metadata.setDebugQuery("SELECT id FROM customer_data WHERE name = '" + SECRET + "'");
        metadata.setResultCount(1);
        sink.writeExecutionLog(null, metadata);
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
