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
    public void safeFormattersOmitValueBearingFields() {
        ExecutionMetadata metadata = metadata();

        String human = new HumanReaderFormatter().formatExecutionLog(metadata);
        String json = new JsonReaderFormatter().formatExecutionLog(metadata);

        assertTrue(human.contains("WHERE name = ?"));
        assertFalse(human.contains("params="));
        assertFalse(human.contains("Debug SQL:"));
        assertFalse(human.contains(SECRET));
        assertTrue(json.contains("\"parameterizedSQL\""));
        assertFalse(json.contains("\"parameters\""));
        assertFalse(json.contains("\"debugSQL\""));
        assertFalse(json.contains(SECRET));
    }

    @Test
    public void explicitDiagnosticMetadataRetainsCopyPasteSql() {
        ExecutionMetadata metadata = metadata();
        metadata.setParameters(List.of(SECRET));
        metadata.setDebugQuery("SELECT id FROM customer_data WHERE name = '" + SECRET + "'");

        String human = new HumanReaderFormatter().formatExecutionLog(metadata);
        String json = new JsonReaderFormatter().formatExecutionLog(metadata);

        assertTrue(human.contains("params=[" + SECRET + "]"));
        assertTrue(human.contains("Debug SQL: SELECT id FROM customer_data WHERE name = '" + SECRET + "'"));
        assertTrue(json.contains("\"parameters\""));
        assertTrue(json.contains("\"debugSQL\""));
        assertTrue(json.contains(SECRET));
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
}
