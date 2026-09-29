package io.teaql.core.hana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.SqlParameterLogPolicy;
import io.teaql.runtime.LogPrivacy;
import java.util.List;
import org.junit.Test;

public class HanaDialectTest {
    private static final class TestRequest extends BaseRequest<BaseEntity> {
        private TestRequest() { super(BaseEntity.class); }
        @Override public String getTypeName() { return "BaseEntity"; }
    }

    @Test
    public void usesHanaPaginationWindowAndLargeText() {
        HanaDialect dialect = new HanaDialect();
        assertEquals("LIMIT 2 OFFSET 5", dialect.prepareLimit(new TestRequest().offset(5, 2)));
        assertTrue(dialect.getPartitionSQL().contains("row_number() over(partition by"));
        assertEquals("NCLOB", dialect.mapColumnType("LARGE_TEXT"));
    }

    @Test
    public void hanaSqlDiagnosticsUseSafeProjectionWithoutChangingExecutionParameters() {
        ExecutionMetadata source = new ExecutionMetadata();
        source.setBackend("hana");
        source.setGeneratedSql(true);
        source.setParameterizedQuery(
                "SELECT \"NAME?\" FROM CUSTOMER WHERE NAME = ? AND ADDRESS = ? AND PASSWORD = ?");
        source.setParameters(List.of("Riverside", "1 Runtime Road", "PASSWORD-CANARY"));
        source.setParameterLogPolicies(List.of(
                SqlParameterLogPolicy.MASKED,
                SqlParameterLogPolicy.PLAIN,
                SqlParameterLogPolicy.CREDENTIAL));

        ExecutionMetadata projected = LogPrivacy.sql(source, false);
        String diagnostic = projected.getDebugQuery();
        assertTrue(diagnostic, diagnostic.contains("Ri*****de"));
        assertTrue(diagnostic, diagnostic.contains("1 Runtime Road"));
        assertTrue(diagnostic, diagnostic.contains("/* masked */"));
        assertTrue(diagnostic, diagnostic.contains("NOT REPLAYABLE"));
        assertTrue(diagnostic, diagnostic.contains("\"NAME?\""));
        assertTrue(diagnostic, diagnostic.contains("[REDACTED]"));
        assertFalse(diagnostic, diagnostic.contains("Riverside"));
        assertFalse(diagnostic, diagnostic.contains("PASSWORD-CANARY"));
        assertEquals(List.of("Riverside", "1 Runtime Road", "PASSWORD-CANARY"), source.getParameters());

        String explicitDebug = LogPrivacy.sql(source, true).getDebugQuery();
        assertTrue(explicitDebug, explicitDebug.contains("Riverside"));
        assertTrue(explicitDebug, explicitDebug.contains("1 Runtime Road"));
        assertFalse(explicitDebug, explicitDebug.contains("PASSWORD-CANARY"));
    }

    @Test
    public void oldHanaDescriptorsWithoutFieldPolicyFailClosed() {
        ExecutionMetadata source = new ExecutionMetadata();
        source.setBackend("hana");
        source.setGeneratedSql(true);
        source.setParameterizedQuery("SELECT id FROM CUSTOMER WHERE NAME = ?");
        source.setParameters(List.of("LEGACY-HANA-CANARY"));

        ExecutionMetadata projected = LogPrivacy.sql(source, false);
        assertTrue(projected.getDebugQuery(), projected.getDebugQuery().contains("[REDACTED]"));
        assertFalse(projected.getDebugQuery(), projected.getDebugQuery().contains("LEGACY-HANA-CANARY"));
        assertEquals(List.of("LEGACY-HANA-CANARY"), source.getParameters());
    }
}
