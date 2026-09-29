package io.teaql.runtime.log;

import io.teaql.core.ExecutionMetadata;
import io.teaql.runtime.config.TeaQLEnv;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecutionLogPrivacyTest {
    private static final String SECRET = "customer-secret";

    @Test
    public void safeFormattersExpandMaskedValuesWithoutSeparateParameterArrays() {
        ExecutionMetadata metadata = metadata();
        // A custom provider may populate parameters without rendered SQL.
        // The ordinary formatters must still keep those values private.
        metadata.setParameters(List.of(SECRET));

        String human = new HumanReaderFormatter().formatExecutionLog(metadata);
        String json = new JsonReaderFormatter().formatExecutionLog(metadata);

        assertTrue(human.contains("WHERE name = '[REDACTED]' /* masked */"));
        assertFalse(human.contains("WHERE name = ?"));
        assertFalse(human.contains("params="));
        assertFalse(human.contains("Debug SQL:"));
        assertFalse(human.contains(SECRET));
        assertTrue(json.contains("\"sql\""));
        assertTrue(json.contains("WHERE name = '[REDACTED]' /* masked */"));
        assertTrue(json.contains("\"maskedParameters\":[true]"));
        assertFalse(json.contains("\"parameterizedSQL\""));
        assertFalse(json.contains("\"parameters\""));
        assertFalse(json.contains("\"debugSQL\""));
        assertFalse(json.contains(SECRET));
    }

    @Test
    public void diagnosticMetadataAloneDoesNotAuthorizePlaintext() {
        ExecutionMetadata metadata = metadata();
        metadata.setParameters(List.of(SECRET));
        metadata.setDebugQuery("SELECT id FROM customer_data WHERE name = '" + SECRET + "'");

        String human = new HumanReaderFormatter().formatExecutionLog(metadata);
        String json = new JsonReaderFormatter().formatExecutionLog(metadata);

        assertFalse(human.contains(SECRET));
        assertFalse(json.contains(SECRET));
        assertFalse(json.contains("\"parameters\""));
        assertFalse(json.contains("\"debugSQL\""));
    }

    @Test
    public void fileSinkRequestsSensitiveDataOnlyAtPayloadLevel() {
        assertEquals(LogConfig.LogLevel.FULL_WITH_PAYLOAD,
                LogConfig.LogLevel.parse("_full_with_payload", LogConfig.LogLevel.SUMMARY));
        String configured = TeaQLEnv.get("TEAQL_SQL_LOG");
        boolean explicitlyEnabled = LogConfig.LogLevel.parse(
                configured, LogConfig.LogLevel.SUMMARY) == LogConfig.LogLevel.FULL_WITH_PAYLOAD;
        assertEquals(explicitlyEnabled, LogConfig.getInstance().includesSensitiveSqlData());
    }

    private ExecutionMetadata metadata() {
        ExecutionMetadata metadata = new ExecutionMetadata();
        metadata.setParameterizedQuery("SELECT id FROM customer_data WHERE name = ?");
        return metadata;
    }

    @Test
    public void failureOutcomeAndUnknownCountsSurviveBothFormatters() {
        var metadata = metadata();
        metadata.setParameters(List.of(SECRET));
        metadata.setExecutionOutcome("failure");
        metadata.setResultSummary("Statement did not complete; row count unknown");
        String human = new HumanReaderFormatter().formatExecutionLog(metadata);
        String json = new JsonReaderFormatter().formatExecutionLog(metadata);
        assertTrue(human.contains("outcome=failure"));
        assertTrue(json.contains("\"executionOutcome\":\"failure\""));
        assertTrue(json.contains("\"resultCount\":null,\"affectedRows\":null"));
        assertFalse(human.contains(SECRET));
        assertFalse(json.contains(SECRET));
    }

    @Test
    public void copiedDebugIntentIsHiddenByBothFormatters() {
        var copy = new ExecutionMetadata();
        copy.setBackend("sqlite"); copy.setGeneratedSql(true);
        copy.setParameterizedQuery("SELECT name FROM customer_data WHERE id=? LIMIT 10000");
        copy.setParameters(List.of(1L));
        copy.setParameterLogPolicies(List.of(io.teaql.core.SqlParameterLogPolicy.PLAIN));
        copy.setLogMode("DEBUG PLAINTEXT");
        copy.setComment("what: reload " + SECRET); copy.setPurpose("why: check " + SECRET);
        copy.setAuditReason("persist " + SECRET);
        copy.setTraceChain(List.of(new io.teaql.core.TraceNode(SECRET)));
        for (String text : List.of(new HumanReaderFormatter().formatExecutionLog(copy),
                new JsonReaderFormatter().formatExecutionLog(copy))) {
            assertFalse(text, text.contains(SECRET));
            assertTrue(text, text.contains("LIMIT 10000"));
            assertTrue(text, text.contains("[REDACTED]"));
        }
    }
}
