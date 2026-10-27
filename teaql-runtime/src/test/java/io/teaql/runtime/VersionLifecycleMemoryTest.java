package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.runtime.memory.MemoryDataService;
import org.junit.Test;
import static io.teaql.runtime.CanonicalMutationFieldsTest.*;
import static org.junit.Assert.*;

/** Production memory provider arithmetic; SQL tombstone retention is a separate gate. */
public class VersionLifecycleMemoryTest {
    static EntityPersistenceMutation request(Probe entity, EntityPersistenceMutation.Action action) {
        return new EntityPersistenceMutation(entity, action, MutationIntent.of("verify production memory versions"));
    }
    @Test public void invalidUpdatesDoNotPublishRowsOrChangePendingState() {
        for (Long version : new Long[]{null, 0L, -1L, Long.MAX_VALUE}) {
            var context = new Fixture(false, null).context; var memory = new MemoryDataService("fixture");
            var row = VersionLifecycleRuntimeTest.persisted(version); row.set$status(EntityStatus.PERSISTED);
            row.updateProperty("required_string", "pending");
            assertThrows(MutationVersionException.class, () -> memory.mutate(context, request(row, EntityPersistenceMutation.Action.SAVE)));
            assertEquals(version, row.getVersion()); assertEquals(EntityStatus.UPDATED, row.get$status());
            var query = new BaseRequest<Probe>(Probe.class, Probe::new) {
                @Override public String getTypeName() { return "NameProbe"; }
            };
            var result = (DefaultQueryResult) memory.query(context,
                    new DefaultQueryRequest(query, QueryIntent.of("inspect rejected memory write", "prove zero published rows")));
            assertTrue(result.getResult().isEmpty());
        }
    }
    @Test public void createDoesNotIncrementAPreassignedNonPersistedVersion() {
        var context = new Fixture(false, null).context; var memory = new MemoryDataService("fixture"); var row = new Probe();
        row.__internalInitializeNewEntityId(1L); row.__internalSet("version", Long.MAX_VALUE);
        row.updateProperty("required_string", "new");
        memory.mutate(context, request(row, EntityPersistenceMutation.Action.SAVE));
        assertEquals(Long.valueOf(1), row.getVersion()); assertEquals(EntityStatus.PERSISTED, row.get$status());
    }
    @Test public void lastUpdateAndRecoveryHaveExactEndpoints() {
        var context = new Fixture(false, null).context; var memory = new MemoryDataService("fixture");
        var live = VersionLifecycleRuntimeTest.persisted(Long.MAX_VALUE - 1); live.updateProperty("required_string", "pending");
        memory.mutate(context, request(live, EntityPersistenceMutation.Action.SAVE)); assertEquals(Long.valueOf(Long.MAX_VALUE), live.getVersion());
        var recover = VersionLifecycleRuntimeTest.persisted(Long.MIN_VALUE + 2); recover.markAsRecover();
        memory.mutate(context, request(recover, EntityPersistenceMutation.Action.SAVE)); assertEquals(Long.valueOf(Long.MAX_VALUE), recover.getVersion());
    }
    @Test public void exhaustedRecoveryAndWrongSignDeleteCannotChangeTheInput() {
        var context = new Fixture(false, null).context; var memory = new MemoryDataService("fixture");
        for (long version : new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1}) {
            var row = VersionLifecycleRuntimeTest.persisted(version); row.markAsRecover();
            assertThrows(MutationVersionException.class, () -> memory.mutate(context, request(row, EntityPersistenceMutation.Action.SAVE)));
            assertEquals(Long.valueOf(version), row.getVersion()); assertEquals(EntityStatus.UPDATED_RECOVER, row.get$status());
        }
        var row = VersionLifecycleRuntimeTest.persisted(-2L); row.set$status(EntityStatus.PERSISTED); row.markForDeletion();
        assertThrows(MutationVersionException.class, () -> memory.mutate(context, request(row, EntityPersistenceMutation.Action.DELETE)));
        assertEquals(Long.valueOf(-2), row.getVersion()); assertEquals(EntityStatus.UPDATED_DELETED, row.get$status());
    }
}
