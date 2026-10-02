package io.teaql.runtime;

import io.teaql.core.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlInheritedIntentTest {
    private ExecutionMetadata readback() {
        var source = new SqlIntentRedactions();
        source.capture(List.of(SqlParameterLogPolicy.MASKED, SqlParameterLogPolicy.PLAIN,
                SqlParameterLogPolicy.CREDENTIAL, SqlParameterLogPolicy.UNKNOWN),
                new Object[]{"Riverside", "PublicAddress", "PASSWORD-CANARY", "UNKNOWN-CANARY"});
        var m = new ExecutionMetadata();
        m.setBackend("sqlite"); m.setGeneratedSql(true);
        m.setParameterizedQuery("SELECT * FROM customer WHERE id = ? LIMIT 10000");
        m.setParameters(List.of(1L)); m.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN));
        m.setIntentRedactions(source);
        m.setComment("what: Riverside PublicAddress PASSWORD-CANARY UNKNOWN-CANARY");
        m.setPurpose(m.getComment()); m.setAuditReason(m.getComment());
        m.setTraceChain(List.of(new TraceNode(TraceKind.AUDIT_REASON, "Customer", 1L, m.getComment())));
        m.setMutationLineage(m.getTraceChain());
        m.setResultCount(1);
        return m;
    }

    @Test public void safeAndDebugViewsClearRawProvenance() {
        var m = readback();
        for (boolean debug : new boolean[]{false, true}) {
            var safe = LogPrivacy.sql(m, debug);
            assertNull(safe.getIntentRedactions());
            assertEquals(debug, safe.getAuditReason().contains("Riverside"));
            assertTrue(safe.getAuditReason().contains("PublicAddress"));
            assertFalse(safe.getTraceChain().toString().contains("CANARY"));
            assertFalse(safe.getMutationLineage().toString().contains("CANARY"));
            assertEquals(debug, safe.getMutationLineage().get(0).getComment().contains("Riverside"));
            assertEquals(Long.valueOf(1), safe.getMutationLineage().get(0).getEntityId());
            assertTrue(safe.getDebugQuery().contains("id = 1 LIMIT 10000"));
            assertEquals(Integer.valueOf(1), safe.getResultCount());
        }
        assertTrue(m.getAuditReason().contains("Riverside"));
        assertFalse(m.getIntentRedactions().toString().contains("Riverside"));
    }

    @Test public void retainedDebugDowngradesWithoutLosingSafeIntent() {
        var debug = LogPrivacy.sql(readback(), true);
        var safe = LogPrivacy.sql(debug, false);
        assertEquals("what: [REDACTED] PublicAddress [REDACTED] [REDACTED]", safe.getAuditReason());
        assertNull(safe.getIntentRedactions());
        assertEquals(safe.getAuditReason(), safe.getMutationLineage().get(0).getComment());
        assertEquals(safe.getAuditReason(), LogPrivacy.sql(LogPrivacy.sql(debug, true), false).getAuditReason());
    }

    @Test public void changingTypedIdentityInvalidatesTheRememberedDebugProjection() {
        var debug = LogPrivacy.sql(readback(), true);
        var old = debug.getMutationLineage().get(0);
        debug.setMutationLineage(List.of(new TraceNode(old.getKind(), old.getName(), 2L, old.getComment())));
        var safe = LogPrivacy.sql(debug, false);
        assertEquals(Long.valueOf(2), safe.getMutationLineage().get(0).getEntityId());
        assertFalse(safe.getMutationLineage().toString().contains("Riverside"));
        assertFalse(safe.getMutationLineage().toString().contains("CANARY"));
    }

    @Test public void nestedCredentialsAndMismatchedPoliciesStayHiddenInDebug() {
        var m = readback();
        var intent = new SqlIntentRedactions();
        intent.capture(List.of(SqlParameterLogPolicy.PLAIN), new Object[]{Map.of("api_key", "NESTED-CANARY")});
        intent.capture(List.of(), new Object[]{"MISMATCH-CANARY"});
        intent.capture(List.of(SqlParameterLogPolicy.MASKED), new Object[]{new java.math.BigDecimal("12.3400")});
        m.setIntentRedactions(intent);
        m.setAuditReason("NESTED-CANARY MISMATCH-CANARY 12.3400");
        assertEquals("[REDACTED] [REDACTED] [REDACTED]", LogPrivacy.sql(m, false).getAuditReason());
        assertEquals("[REDACTED] [REDACTED] 12.3400", LogPrivacy.sql(m, true).getAuditReason());
    }

    @Test public void mutationTargetIdMasksIntentButKeepsPlainBindingAndRowCount() {
        var source = new SqlIntentRedactions();
        source.captureTargetId(1L);
        var m = new ExecutionMetadata();
        m.setBackend("sqlite"); m.setGeneratedSql(true);
        m.setParameterizedQuery("UPDATE customer SET version = version + 1 WHERE id = ?");
        m.setParameters(List.of(1L)); m.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN));
        m.setIntentRedactions(source);
        m.setAuditReason("what: update customer 1");
        m.setComment("what: update customer 1");
        m.setTraceChain(List.of(new TraceNode(TraceKind.AUDIT_REASON, "Customer", "what: update customer 1")));
        m.setAffectedRows(1L);
        for (boolean debug : new boolean[]{false, true}) {
            var safe = LogPrivacy.sql(m, debug);
            assertEquals("what: update customer [REDACTED]", safe.getAuditReason());
            assertEquals("what: update customer [REDACTED]", safe.getComment());
            assertEquals("what: update customer [REDACTED]", safe.getTraceChain().get(0).getComment());
            assertEquals("1 rows affected", safe.getResultSummary());
            assertEquals(List.of(1L), safe.getParameters());
            assertTrue(safe.getDebugQuery().contains("id = 1"));
            assertNull(safe.getIntentRedactions());
        }
        assertEquals("what: update customer 1", m.getAuditReason());
    }
}
