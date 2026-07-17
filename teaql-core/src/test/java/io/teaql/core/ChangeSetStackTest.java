package io.teaql.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

public class ChangeSetStackTest {

    private static final EntityKey ORDER = new EntityKey("Order", 1L);
    private static final EntityKey OTHER_ORDER = new EntityKey("Order", 2L);

    @Test
    public void emptyStackHasNoCurrentOrPoppedChangeSet() {
        ChangeSetStack stack = new ChangeSetStack();

        assertNull(stack.current());
        assertNull(stack.pop());
    }

    @Test
    public void firstSetLazilyCreatesCurrentChangeSet() {
        ChangeSetStack stack = new ChangeSetStack();

        stack.set(ORDER, "status", "CREATED");

        assertNotNull(stack.current());
        assertEquals("CREATED", stack.current().get(ORDER, "status"));
    }

    @Test
    public void topScopeShadowsLowerScopeAndPopRestoresIt() {
        ChangeSetStack stack = new ChangeSetStack();
        stack.set(ORDER, "status", "CREATED");
        stack.push();
        stack.set(ORDER, "status", "PAID");

        assertEquals("PAID", stack.get(ORDER, "status"));

        EntityChangeSet popped = stack.pop();

        assertEquals("PAID", popped.get(ORDER, "status"));
        assertEquals("CREATED", stack.get(ORDER, "status"));
    }

    @Test
    public void clearCurrentClearsOnlyTopScope() {
        ChangeSetStack stack = new ChangeSetStack();
        stack.set(ORDER, "status", "CREATED");
        stack.push();
        EntityChangeSet top = stack.current();
        stack.set(ORDER, "status", "PAID");

        stack.clearCurrent();

        assertEquals("CREATED", stack.get(ORDER, "status"));
        assertTrue(stack.current().isEmpty());
        assertNotSame(top, stack.current());
    }

    @Test
    public void changedFieldNamesAreCombinedAcrossScopes() {
        ChangeSetStack stack = new ChangeSetStack();
        stack.set(ORDER, "status", "CREATED");
        stack.push();
        stack.set(ORDER, "total", 100L);
        stack.set(OTHER_ORDER, "comment", "other");

        assertEquals(Set.of("status", "total"), stack.changedFieldNames(ORDER));
        assertEquals(Set.of("comment"), stack.changedFieldNames(OTHER_ORDER));
    }

    @Test
    public void clearEntityRemovesItFromEveryScopeOnly() {
        ChangeSetStack stack = new ChangeSetStack();
        stack.set(ORDER, "status", "CREATED");
        stack.set(OTHER_ORDER, "status", "CREATED");
        stack.push();
        stack.set(ORDER, "total", 100L);
        stack.set(OTHER_ORDER, "total", 200L);

        stack.clearEntity(ORDER);

        assertNull(stack.get(ORDER, "status"));
        assertNull(stack.get(ORDER, "total"));
        assertEquals("CREATED", stack.get(OTHER_ORDER, "status"));
        assertEquals(200L, stack.get(OTHER_ORDER, "total"));
    }

    @Test
    public void pushCreatesASeparateCurrentChangeSet() {
        ChangeSetStack stack = new ChangeSetStack();
        EntityChangeSet first = stack.currentMut();

        stack.push();

        assertSame(stack.current(), stack.currentMut());
        assertNotSame(first, stack.current());
    }
}
