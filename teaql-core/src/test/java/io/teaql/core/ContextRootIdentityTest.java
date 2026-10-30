package io.teaql.core;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Typed context-root admission; no provider, metadata lookup or ID allocation. */
public class ContextRootIdentityTest {
    private UserContext context() {
        Map<String, Object> values = new HashMap<>();
        return new UserContextTest.DummyUserContext() {
            @Override public void putAttribute(String key, Object value) { values.put(key, value); }
            @Override public Object getAttribute(String key) { return values.get(key); }
            @Override public <T> T getAttribute(String key, Class<T> type) { return type.cast(values.get(key)); }
        };
    }

    private void invalid(long id, boolean directAttribute) {
        UserContext context = context();
        var root = new ContextEntityRef("Tenant", id);
        if (directAttribute) context.putAttribute(UserContext.TEAQL_ACTIVE_ROOT, root);
        else context.withActiveRoot(root);
        var error = assertThrows(ContextRootException.class, () -> context.requireActiveRoot("Tenant"));
        assertEquals("INVALID_ID", error.getReason().name());
        assertEquals("Tenant", error.getExpectedType());
        assertEquals(root, error.getActiveRoot());
        System.out.println("JAVA_CONTEXT_ROOT_IDENTITY={\"id\":\"" + id
                + "\",\"directAttribute\":" + directAttribute + ",\"reason\":\"INVALID_ID\",\"passed\":true}");
    }

    @Test public void zeroViaFluentApiIsNotAPersistentRoot() { invalid(0, false); }
    @Test public void zeroViaContextAttributeCannotBypassAdmission() { invalid(0, true); }
    @Test public void negativeRootIsNotAPersistentIdentity() { invalid(-1, false); }
    @Test public void minimumSignedRootIsRejectedWithoutAbsoluteValueOverflow() { invalid(Long.MIN_VALUE, true); }

    @Test public void fullPositiveLongDomainIsPreservedExactly() {
        for (long id : new long[] {1L, 42L, Long.MAX_VALUE}) {
            UserContext context = context();
            context.withActiveRoot(new ContextEntityRef("Tenant", id));
            assertEquals(Long.valueOf(id), context.requireActiveRoot("Tenant").id());
            context.verifyActiveRoot("Tenant", id);
        }
    }

    @Test public void missingAndWrongTypeRemainDistinct() {
        UserContext context = context();
        assertEquals(ContextRootException.Reason.MISSING,
                assertThrows(ContextRootException.class, () -> context.requireActiveRoot("Tenant")).getReason());
        context.withActiveRoot(new ContextEntityRef("Organization", 0L));
        assertEquals(ContextRootException.Reason.TYPE_MISMATCH,
                assertThrows(ContextRootException.class, () -> context.requireActiveRoot("Tenant")).getReason());
    }

    @Test public void suppliedRootVerificationCannotAcceptZeroContext() {
        UserContext context = context();
        context.withActiveRoot(new ContextEntityRef("Tenant", 0L));
        assertEquals("INVALID_ID", assertThrows(ContextRootException.class,
                () -> context.verifyActiveRoot("Tenant", 0L)).getReason().name());
    }
}
