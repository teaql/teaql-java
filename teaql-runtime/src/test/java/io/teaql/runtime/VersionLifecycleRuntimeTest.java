package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.checker.CheckException;
import java.util.List;
import org.junit.Test;
import static io.teaql.runtime.CanonicalMutationFieldsTest.*;
import static org.junit.Assert.*;

/** #236: version admission is graph-wide and precedes customer code and I/O. */
public class VersionLifecycleRuntimeTest {
    static Probe persisted(Long version) {
        var entity = new Probe();
        entity.__internalSet("id", 1L);
        entity.__internalSet("version", version);
        entity.set$status(version != null && version < 0 ? EntityStatus.PERSISTED_DELETED : EntityStatus.PERSISTED);
        entity.setEntityMutationLedger(new EntityMutationLedger());
        return entity;
    }
    static void rejected(Fixture fixture, Probe root, Probe invalid, String path, int expectedCheckers) {
        Long before = invalid.getVersion();
        EntityStatus status = invalid.get$status();
        Throwable failure = null;
        try { root.auditAs("validate version lifecycle before side effects").save(fixture.context); }
        catch (RuntimeException error) { failure = error; }
        int[] observed = {fixture.checkerCalls, fixture.policyCalls, fixture.ids,
                fixture.provider.begins, fixture.provider.mutations, fixture.audits};
        System.out.println("VERSION_ADMISSION path=" + path + " version=" + before
                + " hooks=" + java.util.Arrays.toString(observed)
                + " failure=" + (failure == null ? "none" : failure.getClass().getSimpleName()));
        assertArrayEquals("checker, policy, IDs, transaction, provider, committed audit",
                new int[]{expectedCheckers, 0, 0, 0, 0, 0}, observed);
        assertTrue("typed checker rejection required", failure instanceof CheckException);
        var error = (CheckException) failure;
        assertEquals(path, error.getViolates().get(0).getLocation().instancePath());
        assertEquals(before, invalid.getVersion());
        assertEquals(status, invalid.get$status());
    }
    static void rejectUpdate(Long version) {
        var fixture = new Fixture(true, null);
        var entity = persisted(version);
        entity.updateProperty("required_string", "pending value");
        rejected(fixture, entity, entity, "/version", 0);
    }
    static void rejectRecover(long version) {
        var fixture = new Fixture(true, null);
        var entity = persisted(version);
        // The provider must validate the version, not trust a forged lifecycle label.
        entity.set$status(EntityStatus.PERSISTED_DELETED);
        entity.markAsRecover();
        rejected(fixture, entity, entity, "/version", 0);
    }
    @Test public void exhaustedUpdateRejectedBeforeAnyHook() { rejectUpdate(Long.MAX_VALUE); }
    @Test public void zeroUpdateRejectedBeforeAnyHook() { rejectUpdate(0L); }
    @Test public void negativeUpdateRejectedBeforeAnyHook() {
        var fixture = new Fixture(true, null); var entity = persisted(-1L);
        entity.set$status(EntityStatus.PERSISTED); entity.updateProperty("required_string", "pending value");
        rejected(fixture, entity, entity, "/version", 0);
    }
    @Test public void missingUpdateVersionRejectedBeforeAnyHook() { rejectUpdate(null); }
    @Test public void minimumRecoveryRejectedBeforeAnyHook() { rejectRecover(Long.MIN_VALUE); }
    @Test public void minimumPlusOneRecoveryRejectedBeforeAnyHook() { rejectRecover(Long.MIN_VALUE + 1); }
    @Test public void positiveRecoveryRejectedBeforeAnyHook() { rejectRecover(1L); }
    @Test public void zeroRecoveryRejectedBeforeAnyHook() { rejectRecover(0L); }
    @Test public void invalidChildRejectsWholeGraphBeforeFirstChecker() {
        var fixture = new Fixture(true, null); var root = fixture.entity(false);
        var child = persisted(Long.MAX_VALUE); child.updateProperty("required_string", "pending child");
        root.updateProperty("children", List.of(child));
        rejected(fixture, root, child, "/children/0/version", 0);
    }
    @Test public void postFixVersionRejectedBeforePolicyAllocationOrStorage() {
        var fixture = new Fixture(true, entity -> {
            entity.__internalSet("version", Long.MAX_VALUE);
            entity.getEntityMutationLedger().setOriginalVersion(new EntityKey("NameProbe", 1L), Long.MAX_VALUE);
        });
        var entity = persisted(1L); entity.updateProperty("required_string", "pending value");
        // Rejection preserves the Checker-produced pending state, not a fake successful readback.
        Throwable failure = null;
        try { entity.auditAs("reject invalid post-fix version").save(fixture.context); }
        catch (RuntimeException error) { failure = error; }
        assertEquals(1, fixture.checkerCalls); assertEquals(0, fixture.policyCalls);
        assertEquals(0, fixture.ids); assertEquals(0, fixture.provider.begins);
        assertEquals(0, fixture.provider.mutations); assertEquals(0, fixture.audits);
        assertTrue(failure instanceof CheckException);
        assertEquals(Long.valueOf(Long.MAX_VALUE), entity.getVersion());
        assertEquals("/version", ((CheckException) failure).getViolates().get(0).getLocation().instancePath());
    }
    @Test public void unchangedExhaustedObjectNeedsNoCheckerPolicyOrTransaction() {
        var fixture = new Fixture(true, null); var entity = persisted(Long.MAX_VALUE);
        entity.auditAs("inspect unchanged exhausted object").save(fixture.context);
        assertEquals(0, fixture.checkerCalls); assertEquals(0, fixture.policyCalls);
        assertEquals(0, fixture.provider.begins); assertEquals(0, fixture.provider.mutations); assertEquals(0, fixture.audits);
    }
    @Test public void restoredKnownNullAtMaxIsARealNoOp() {
        var fixture = new Fixture(true, null); var entity = persisted(Long.MAX_VALUE);
        entity.markPropertyLoaded("required_string");
        entity.updateProperty("required_string", "temporary"); entity.updateProperty("required_string", null);
        assertEquals(EntityStatus.PERSISTED, entity.get$status());
        entity.auditAs("retain restored exhausted object").save(fixture.context);
        assertEquals(0, fixture.checkerCalls); assertEquals(0, fixture.policyCalls); assertEquals(0, fixture.provider.mutations);
        assertEquals(Long.valueOf(Long.MAX_VALUE), entity.getVersion());
    }
    @Test public void untouchedExhaustedAssociationDoesNotBlockNewParent() {
        var fixture = new Fixture(true, null); var root = fixture.entity(false); var child = persisted(Long.MAX_VALUE);
        root.updateProperty("children", List.of(child)); root.auditAs("create parent around unchanged reference").save(fixture.context);
        assertEquals(1, fixture.ids); assertEquals(1, fixture.checkerCalls); assertEquals(1, fixture.policyCalls);
        assertEquals(1, fixture.provider.mutations); assertEquals(Long.valueOf(Long.MAX_VALUE), child.getVersion());
    }
    @Test public void exhaustedRawOwnLedgerRejectsEvenWithCleanEntityStatus() {
        var fixture = new Fixture(true, null); var entity = persisted(Long.MAX_VALUE);
        entity.getEntityMutationLedger().set(new EntityKey("NameProbe", 1L), "required_string", "raw pending");
        rejected(fixture, entity, entity, "/version", 0);
    }
    @Test public void exhaustedDetachedKeyRejectsBeforeNewRootIdAllocation() {
        var fixture = new Fixture(true, null); var root = fixture.entity(false); var key = new EntityKey("NameProbe", 23L);
        root.getEntityMutationLedger().set(key, "required_string", "detached pending");
        root.getEntityMutationLedger().setOriginalVersion(key, Long.MAX_VALUE);
        rejected(fixture, root, root, "/version", 0);
    }
    @Test public void legalDeleteAtMaxStillEntersReviewedExecution() {
        var fixture = new Fixture(true, null); var entity = persisted(Long.MAX_VALUE); entity.markForDeletion();
        entity.auditAs("delete last representable live version").save(fixture.context);
        assertEquals(1, fixture.policyCalls); assertEquals(1, fixture.provider.mutations); assertEquals(1, fixture.audits);
    }
    @Test public void legalRecoveryToMaxStillEntersReviewedExecution() {
        var fixture = new Fixture(true, null); var entity = persisted(Long.MIN_VALUE + 2); entity.markAsRecover();
        entity.auditAs("recover last representable tombstone").save(fixture.context);
        assertEquals(1, fixture.policyCalls); assertEquals(1, fixture.provider.mutations); assertEquals(1, fixture.audits);
    }
    @Test public void oneRejectedGraphDoesNotPoisonLaterSaveOnSameContext() {
        var fixture = new Fixture(true, null); var bad = persisted(Long.MAX_VALUE); bad.updateProperty("required_string", "bad");
        rejected(fixture, bad, bad, "/version", 0);
        fixture.entity(false).auditAs("save independent valid graph").save(fixture.context);
        assertEquals(1, fixture.checkerCalls); assertEquals(1, fixture.policyCalls); assertEquals(1, fixture.provider.mutations);
    }
}
