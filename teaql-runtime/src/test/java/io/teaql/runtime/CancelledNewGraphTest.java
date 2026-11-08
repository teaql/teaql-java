package io.teaql.runtime;

import io.teaql.core.*;
import java.util.List;
import org.junit.Test;
import static io.teaql.runtime.GraphTraceChainTest.*;
import static org.junit.Assert.*;

/** ML-12: cancellation is intent; save owns the graph's persistence decision. */
public class CancelledNewGraphTest {
    private static GraphEntity fresh(String type, long id) {
        var entity = new GraphEntity(type);
        entity.__internalInitializeNewEntityId(id);
        entity.updateProperty("name", type + " new");
        return entity;
    }

    @Test public void cancelledNewRootRecordsIntentAndRejectsOnlyAtSave() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        var root = fresh("CustomerOrder", 100L);
        assertSame(root, root.markForDeletion());
        assertTrue(root.isNew()); assertTrue(root.isMarkedAsDelete());
        assertTrue(provider.requests.isEmpty());
        root.markForDeletion();
        root.updateProperty("name", "editing does not undo cancellation");
        var error = assertThrows(TeaQLRuntimeException.class,
                () -> root.auditAs("cancel before first persistence").save(context));
        assertTrue(error.getMessage().contains("CANCELLED_NEW_ROOT"));
        assertTrue(provider.requests.isEmpty());
        assertNull(root.getVersion()); assertEquals(Long.valueOf(100), root.getId());
        assertTrue(root.isNew()); assertTrue(root.isMarkedAsDelete());
    }

    @Test public void cancelledNewChildDoesNotSuppressItsLiveSiblingOrParent() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        var root = fresh("CustomerOrder", 100L);
        var cancelled = fresh("OrderItem", 201L);
        var live = fresh("OrderItem", 202L);
        cancelled.markForDeletion();
        root.updateProperty("children", List.of(cancelled, live));
        root.auditAs("omit only a cancelled new child").save(context);
        assertEquals(2, provider.requests.size());
        assertTrue(provider.requests.stream().noneMatch(v -> Long.valueOf(201).equals(v.getEntity().getId())));
        assertTrue(provider.requests.stream().anyMatch(v -> Long.valueOf(100).equals(v.getEntity().getId())));
        assertTrue(provider.requests.stream().anyMatch(v -> Long.valueOf(202).equals(v.getEntity().getId())));
        assertNull(cancelled.getVersion());
        assertTrue(cancelled.isNew()); assertTrue(cancelled.isMarkedAsDelete());
        var error = assertThrows(TeaQLRuntimeException.class,
                () -> cancelled.auditAs("cancelled child cannot be resurrected after parent save").save(context));
        assertTrue(error.getMessage().contains("CANCELLED_NEW_ROOT"));
        assertEquals(2, provider.requests.size());
    }

    @Test public void anIncompleteCancelledChildIsNotValidatedOrWritten() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var root = fresh("CustomerOrder", 100L);
        var cancelled = fresh("OrderItem", 201L);
        cancelled.updateProperty("name", Double.NaN);
        root.updateProperty("children", List.of(cancelled));
        cancelled.markForDeletion();
        root.auditAs("discard unfinished child before save").save(new DefaultUserContext(runtime));
        assertEquals(1, provider.requests.size());
        assertEquals(Long.valueOf(100), provider.requests.get(0).getEntity().getId());
        assertNull(cancelled.getVersion());
    }

    @Test public void cancellationAfterCompositionCannotBecomeAPersistedDelete() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var root = fresh("CustomerOrder", 100L);
        var cancelled = fresh("OrderItem", 201L);
        root.updateProperty("children", List.of(cancelled));
        cancelled.setEntityMutationLedger(root.getEntityMutationLedger());
        root.getEntityMutationLedger().markAsNew(new EntityKey("OrderItem", 201L));
        cancelled.markForDeletion();
        // Editing after cancellation must not silently undo PendingDeletion.
        cancelled.updateProperty("name", "still cancelled");
        assertTrue(cancelled.isMarkedAsDelete());
        root.auditAs("retain precomposed cancellation intent").save(new DefaultUserContext(runtime));
        assertEquals(1, provider.requests.size());
        assertEquals(Long.valueOf(100), provider.requests.get(0).getEntity().getId());
        assertNull(cancelled.getVersion());
    }

    @Test public void cancellingOneChildDoesNotDisableValidationOfALiveSibling() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var root = fresh("CustomerOrder", 100L);
        var cancelled = fresh("OrderItem", 201L);
        var live = fresh("OrderItem", 202L);
        cancelled.updateProperty("name", Double.NaN);
        cancelled.markForDeletion();
        live.updateProperty("name", Double.NaN);
        root.updateProperty("children", List.of(cancelled, live));
        assertThrows(io.teaql.core.checker.CheckException.class,
                () -> root.auditAs("live sibling still requires valid values").save(new DefaultUserContext(runtime)));
        assertTrue(provider.requests.isEmpty());
        assertTrue(cancelled.isMarkedAsDelete());
        // Standalone checking remains distinct from mutation-only graph admission.
        assertFalse(io.teaql.core.checker.internal.FiniteScalarAdmission.collect(cancelled).isEmpty());
    }

    @Test public void pendingDescendantsOfACancelledBranchCannotLeakIntoTheSavePlan() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var root = fresh("CustomerOrder", 100L);
        var cancelled = fresh("OrderItem", 201L);
        var descendant = fresh("Payment", 301L);
        cancelled.updateProperty("children", List.of(descendant));
        cancelled.markForDeletion();
        root.updateProperty("children", List.of(cancelled));
        var error = assertThrows(TeaQLRuntimeException.class,
                () -> root.auditAs("do not persist an orphan from a cancelled branch").save(new DefaultUserContext(runtime)));
        assertTrue(error.getMessage().contains("CANCELLED_NEW_BRANCH_HAS_PENDING_DESCENDANTS"));
        assertTrue(provider.requests.isEmpty());
        assertNull(root.getVersion()); assertNull(descendant.getVersion());
    }

    @Test public void anExplicitLiveAliasOfTheDescendantKeepsItsOwnIntent() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var root = fresh("CustomerOrder", 100L);
        var cancelled = fresh("OrderItem", 201L);
        var descendant = fresh("Payment", 301L);
        cancelled.updateProperty("children", List.of(descendant));
        cancelled.markForDeletion();
        root.updateProperty("children", List.of(cancelled, descendant));
        root.auditAs("retain the descendant explicitly reached through a live branch").save(new DefaultUserContext(runtime));
        assertEquals(2, provider.requests.size());
        assertTrue(provider.requests.stream().noneMatch(v -> Long.valueOf(201).equals(v.getEntity().getId())));
        assertTrue(provider.requests.stream().anyMatch(v -> Long.valueOf(301).equals(v.getEntity().getId())));
    }

    @Test public void sameNumericIdAndABackReferenceDoNotCancelTheLiveRoot() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var root = fresh("CustomerOrder", 1L);
        var cancelled = fresh("OrderItem", 1L);
        cancelled.updateProperty("children", List.of(root));
        cancelled.markForDeletion();
        root.updateProperty("children", List.of(cancelled));
        root.auditAs("preserve the distinct live root through a cyclic reference").save(new DefaultUserContext(runtime));
        assertEquals(1, provider.requests.size());
        assertEquals("CustomerOrder", provider.requests.get(0).getEntity().typeName());
        assertNull(cancelled.getVersion()); assertTrue(cancelled.isMarkedAsDelete());
    }
}
