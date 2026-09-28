package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.SqlParameterLogPolicy;
import io.teaql.core.TraceNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlMaskingPolicyTest {
    private ExecutionMetadata mixed() {
        var m = new ExecutionMetadata();
        m.setBackend("sqlite");
        m.setGeneratedSql(true);
        m.setParameterizedQuery("UPDATE \"customer\" SET name = ?, active = ?, password = ? WHERE id = ?");
        m.setParameters(List.of("Riverside", true, "PASSWORD-CANARY", 1L));
        m.setParameterLogPolicies(List.of(SqlParameterLogPolicy.MASKED, SqlParameterLogPolicy.PLAIN,
                SqlParameterLogPolicy.CREDENTIAL, SqlParameterLogPolicy.PLAIN));
        m.setComment("what: replace PASSWORD-CANARY");
        m.setPurpose("why: edit customer");
        m.setTraceChain(List.of(new TraceNode("PASSWORD-CANARY")));
        m.setAffectedRows(1L);
        return m;
    }

    @Test public void mixedPoliciesPreserveOrdinaryValuesAndIntentButNotCredentials() {
        var original = mixed();
        var safe = LogPrivacy.sql(original, false);
        assertTrue(safe.getDebugQuery().contains("name = 'Ri*****de' /* masked */"));
        assertTrue(safe.getDebugQuery().contains("active = TRUE"));
        assertTrue(safe.getDebugQuery().contains("password = '[REDACTED]' /* masked */ WHERE id = 1"));
        assertFalse(safe.getDebugQuery().contains("Riverside"));
        assertFalse(safe.getDebugQuery().contains("PASSWORD-CANARY"));
        assertFalse(safe.getComment().contains("PASSWORD-CANARY"));
        assertFalse(safe.getTraceChain().toString().contains("PASSWORD-CANARY"));
        assertEquals("why: edit customer", safe.getPurpose());
        assertEquals("1 rows affected", safe.getResultSummary());
        assertEquals(List.of(true, false, true, false), safe.getParameterMasked());
        assertEquals(List.of("Riverside", true, "PASSWORD-CANARY", 1L), original.getParameters());
        assertNull(original.getDebugQuery());
    }

    @Test public void repeatedProjectionDoesNotUpgradeMaskedValuesOrDuplicateHeaders() {
        var once = LogPrivacy.sql(mixed(), false);
        var twice = LogPrivacy.sql(once, false);
        assertEquals(once.getDebugQuery(), twice.getDebugQuery());
        assertEquals(once.getParameters(), twice.getParameters());
        var upgrade = LogPrivacy.sql(twice, true);
        assertFalse(upgrade.getDebugQuery().contains("Riverside"));
        assertTrue(upgrade.getDebugQuery().contains("NOT REPLAYABLE"));
    }

    @Test public void debugIsPerRecordAndStillProtectsCredentials() {
        for (int i = 0; i < 2; i++) {
            var safe = LogPrivacy.sql(mixed(), true);
            assertTrue(safe.getDebugQuery().contains("DEBUG PLAINTEXT; EXPLICIT OPT-IN"));
            assertTrue(safe.getDebugQuery().contains("NOT REPLAYABLE"));
            assertTrue(safe.getDebugQuery().contains("name = 'Riverside'"));
            assertFalse(safe.getDebugQuery().contains("PASSWORD-CANARY"));
            assertEquals("DEBUG PARTIALLY MASKED", safe.getLogMode());
        }
    }

    @Test public void policyAndBindMismatchesHaveSafeReasons() {
        var m = mixed();
        m.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN));
        var safe = LogPrivacy.sql(m, true);
        assertEquals("policy_count_mismatch", safe.getSqlOmissionReason());
        assertFalse(safe.getParameters().toString().contains("PASSWORD-CANARY"));
        m = mixed(); m.setParameterizedQuery("SELECT ?");
        safe = LogPrivacy.sql(m, false);
        assertEquals("unsupported_literal_or_binding_mismatch", safe.getSqlOmissionReason());
        assertFalse(safe.getDebugQuery().contains("PASSWORD-CANARY"));
    }

    @Test public void unknownNestedCredentialAndNullValuesRemainSafe() {
        var m = new ExecutionMetadata();
        m.setParameterizedQuery("SELECT ?, ?, ?");
        m.setParameters(Arrays.asList(null, new Object[]{Map.of("access_token", "NESTED-TOKEN-CANARY")}, "Riverside"));
        var safe = LogPrivacy.sql(m, false);
        assertTrue(safe.getDebugQuery().contains("NULL /* masked */"));
        assertFalse(safe.getDebugQuery().contains("Ri*****de"));
        assertFalse(safe.getDebugQuery().contains("NESTED-TOKEN-CANARY"));
        var debug = LogPrivacy.sql(m, true);
        assertFalse(debug.getDebugQuery().contains("Riverside"));
        assertFalse(debug.getDebugQuery().contains("NESTED-TOKEN-CANARY"));
    }

    @Test public void binaryLogCopiesCannotMutateDriverParameters() {
        var m = new ExecutionMetadata();
        byte[] original = {1, 2};
        m.setParameterizedQuery("SELECT ?");
        m.setParameters(List.of(original));
        m.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN));
        var safe = LogPrivacy.sql(m, false);
        assertTrue(safe.getDebugQuery().contains("X'0102'"));
        ((byte[]) safe.getParameters().get(0))[0] = 42;
        assertArrayEquals(new byte[]{1, 2}, original);
    }

    @Test public void unknownBindingsStayPrivateWithDebugOptIn() {
        for (var policies : List.of(List.<SqlParameterLogPolicy>of(), List.of(SqlParameterLogPolicy.UNKNOWN))) {
            var raw = new ExecutionMetadata();
            raw.setParameterizedQuery("SELECT ?");
            raw.setParameters(List.of("UNKNOWN-BINDING-CANARY"));
            raw.setParameterLogPolicies(policies);
            raw.setComment("what: locate UNKNOWN-BINDING-CANARY");
            var safe = LogPrivacy.sql(raw, true);
            assertFalse(safe.getDebugQuery().contains("UNKNOWN-BINDING-CANARY"));
            assertFalse(safe.getComment().contains("UNKNOWN-BINDING-CANARY"));
            assertEquals(List.of("[REDACTED]"), safe.getParameters());
            assertEquals(List.of(true), safe.getParameterMasked());
            assertTrue(safe.getDebugQuery().contains("SELECT"));
            assertTrue(safe.getDebugQuery().contains("NOT REPLAYABLE"));
            assertEquals(List.of("UNKNOWN-BINDING-CANARY"), raw.getParameters());
        }
    }

    @Test public void debugOnlyExposesExplicitBusinessPolicies() {
        var raw = new ExecutionMetadata();
        raw.setParameterizedQuery("SELECT ?, ?, ?, ?");
        raw.setParameters(List.of("Ordinary", "Riverside", "UNKNOWN-BINDING-CANARY", "CREDENTIAL-CANARY"));
        raw.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN, SqlParameterLogPolicy.MASKED,
                SqlParameterLogPolicy.UNKNOWN, SqlParameterLogPolicy.CREDENTIAL));
        var safe = LogPrivacy.sql(raw, true);
        assertEquals(List.of("Ordinary", "Riverside", "[REDACTED]", "[REDACTED]"), safe.getParameters());
        assertEquals(List.of(false, false, true, true), safe.getParameterMasked());
    }

    @Test public void untrustedInlineCredentialCannotEscapeUnderDebug() {
        var m = mixed(); m.setGeneratedSql(false);
        m.setParameterizedQuery("SELECT 'INLINE-SECRET-CANARY' AS password, ?, ?, ?, ?");
        var safe = LogPrivacy.sql(m, true);
        assertEquals("untrusted_inline_sql", safe.getSqlOmissionReason());
        assertFalse(safe.getDebugQuery().contains("INLINE-SECRET-CANARY"));
        assertFalse(safe.getParameterizedQuery().contains("INLINE-SECRET-CANARY"));
        var twice = LogPrivacy.sql(safe, false);
        assertEquals(safe.getSqlOmissionReason(), twice.getSqlOmissionReason());
        assertEquals(safe.getDebugQuery(), twice.getDebugQuery());
        assertEquals("OMITTED", twice.getLogMode());
    }

    @Test public void safeProjectionCannotBeRelabeledAsDebugPlaintext() {
        var safe = LogPrivacy.sql(mixed(), false);
        assertEquals(safe.getDebugQuery(), LogPrivacy.sql(safe, true).getDebugQuery());
        assertEquals(safe.getLogMode(), LogPrivacy.sql(safe, true).getLogMode());
    }

    @Test public void invalidMaskFlagsFailClosedInsteadOfTrustingPlainPolicy() {
        var raw = mixed();
        raw.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN, SqlParameterLogPolicy.PLAIN,
                SqlParameterLogPolicy.PLAIN, SqlParameterLogPolicy.PLAIN));
        raw.setParameterMasked(List.of(true));
        var safe = LogPrivacy.sql(raw, true);
        assertEquals("mask_count_mismatch", safe.getSqlOmissionReason());
        assertFalse(safe.getParameters().toString().contains("CANARY"));
        assertFalse(safe.getParameters().toString().contains("Riverside"));
        assertEquals(safe.getDebugQuery(), LogPrivacy.sql(safe, false).getDebugQuery());
    }

    @Test public void debugRevocationPreservesOriginalSafeIntent() {
        var raw = mixed();
        raw.setAuditReason("what: persist Riverside PASSWORD-CANARY");
        var debug = LogPrivacy.sql(raw, true);
        var safe = LogPrivacy.sql(debug, false);
        assertEquals("what: persist [REDACTED] [REDACTED]", safe.getAuditReason());
        assertEquals("why: edit customer", safe.getPurpose());
        // A caller may mutate a returned record; this must not corrupt cached safety.
        safe.setAuditReason("Riverside");
        assertEquals("what: persist [REDACTED] [REDACTED]", LogPrivacy.sql(debug, false).getAuditReason());
    }

    @Test public void modifiedDebugRecordCannotLeakInheritedIntent() {
        var raw = mixed();
        raw.setAuditReason("what: persist Riverside");
        var debug = LogPrivacy.sql(raw, true);
        // A derived ID-only request no longer carries the original secret bindings.
        debug.setParameterizedQuery("SELECT name FROM customer WHERE id=? LIMIT 10000");
        debug.setParameters(List.of(1L));
        debug.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN));
        debug.setParameterMasked(List.of(false));
        var safe = LogPrivacy.sql(debug, false);
        assertFalse(safe.getAuditReason().contains("Riverside"));
        assertEquals("[REDACTED]", safe.getAuditReason());
        assertEquals(debug.getParameterizedQuery(), safe.getParameterizedQuery());
        assertTrue(safe.getDebugQuery().contains("LIMIT 10000"));
        assertEquals(List.of(1L), safe.getParameters());
    }

    @Test public void copiedDebugRecordWithoutPrivateProvenanceHidesIntent() {
        var copy = new ExecutionMetadata();
        copy.setBackend("sqlite"); copy.setGeneratedSql(true);
        copy.setLogMode("DEBUG PLAINTEXT");
        copy.setParameterizedQuery("SELECT name FROM customer WHERE id=? LIMIT 10000");
        copy.setParameters(List.of(1L));
        copy.setParameterLogPolicies(List.of(SqlParameterLogPolicy.PLAIN));
        copy.setComment("what: reload Riverside"); copy.setPurpose("why: confirm Riverside");
        copy.setAuditReason("persist Riverside"); copy.setBackendRequestId("Riverside");
        copy.setTraceChain(List.of(new TraceNode("Riverside")));
        var safe = LogPrivacy.sql(copy, false);
        for (String text : List.of(safe.getComment(), safe.getPurpose(), safe.getAuditReason(),
                safe.getBackendRequestId(), safe.getTraceChain().toString())) assertFalse(text, text.contains("Riverside"));
        assertTrue(safe.getDebugQuery().contains("LIMIT 10000"));
        copy.setLogMode(null);
        copy.setDebugQuery("-- TeaQL DEBUG PLAINTEXT; EXPLICIT OPT-IN\nSELECT name FROM customer WHERE id=1 LIMIT 10000");
        assertEquals("[REDACTED]", LogPrivacy.sql(copy, false).getAuditReason());
    }

    @Test public void debugFallbackCopiesMutableBinaryAndNullableBindings() {
        var raw = mixed();
        raw.setParameterizedQuery("SELECT ?, ?, ?");
        raw.setParameters(Arrays.asList("Riverside", new byte[]{1, 2}, null));
        raw.setParameterLogPolicies(List.of(SqlParameterLogPolicy.MASKED, SqlParameterLogPolicy.PLAIN, SqlParameterLogPolicy.PLAIN));
        raw.setAuditReason("Riverside");
        var debug = LogPrivacy.sql(raw, true);
        var safe = LogPrivacy.sql(debug, false);
        ((byte[]) safe.getParameters().get(1))[0] = 42;
        assertArrayEquals(new byte[]{1, 2}, (byte[]) LogPrivacy.sql(debug, false).getParameters().get(1));
        assertNull(safe.getParameters().get(2));
        ((byte[]) debug.getParameters().get(1))[0] = 99;
        assertEquals("[REDACTED]", LogPrivacy.sql(debug, false).getAuditReason());
        assertArrayEquals(new byte[]{1, 2}, (byte[]) raw.getParameters().get(1));
    }

    @Test public void numericAndTableNameParametersDoNotChangeSqlStructure() {
        for (Object value : List.of(1L, "customer")) {
            var raw = mixed();
            raw.setParameterizedQuery("SELECT name FROM customer WHERE name=? LIMIT 10000");
            raw.setParameters(List.of(value)); raw.setParameterLogPolicies(List.of(SqlParameterLogPolicy.MASKED));
            var safe = LogPrivacy.sql(raw, false);
            assertEquals(raw.getParameterizedQuery(), LogPrivacy.sql(safe, false).getParameterizedQuery());
            assertEquals(safe.getDebugQuery(), LogPrivacy.sql(safe, false).getDebugQuery());
        }
    }
}
