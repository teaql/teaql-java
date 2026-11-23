package io.teaql.core;

import java.util.Set;
import org.junit.Test;
import static io.teaql.core.RejectedMutationStateTest.*;
import static org.junit.Assert.*;

public class RejectedMutationControlsTest {
    @Test public void newTypedMutationStillRecordsTheSelectedValue(){
        var row=row(EntityStatus.NEW,Availability.NOT_LOADED);row.updateName("new value");
        assertEquals(EntityStatus.NEW,row.get$status());assertTrue(row.isPropertyLoaded("name"));assertEquals("new value",row.name);assertTrue(row.getUpdatedProperties().contains("name"));
    }
    @Test public void persistedTypedMutationRetainsItsOriginalVersion(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var state=row.__internalLoadState();row.updateName("updated value");
        assertEquals(EntityStatus.UPDATED,row.get$status());assertSame(state,row.__internalLoadState());assertEquals(Long.valueOf(7),row.getOriginalVersion());assertEquals(Set.of("name"),Set.copyOf(row.getUpdatedProperties()));
    }
    @Test public void pendingDeletionSurvivesAnAllowedTypedUpdate(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.markForDeletion();row.updateName("final value");
        assertEquals(EntityStatus.UPDATED_DELETED,row.get$status());assertTrue(row.isMarkedAsDelete());assertEquals("final value",row.name);
    }
    @Test public void validGenericMutationStillUsesTheTypedStorage(){
        var row=row(EntityStatus.PERSISTED,Availability.NOT_LOADED);row.updateProperty("count",3);
        assertEquals(EntityStatus.UPDATED,row.get$status());assertEquals(Integer.valueOf(3),row.count);assertTrue(row.isPropertyLoaded("count"));assertEquals(Long.valueOf(7),row.getOriginalVersion());
    }
    @Test public void failedGenericCastDoesNotSelectAnUnloadedField(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var before=new Snapshot(row);
        assertThrows(ClassCastException.class,()->row.updateProperty("count","wrong type"));before.unchanged(row);
    }
    @Test public void unknownGenericFieldAlreadyFailsWithoutChangingTheEntity(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var before=new Snapshot(row);
        assertThrows(IllegalArgumentException.class,()->row.updateProperty("unknown_field","wrong value"));before.unchanged(row);
    }
    @Test public void allowedLegacyCacheAttachmentKeepsItsExistingReadContract(){
        var row=row(EntityStatus.NEW,Availability.NOT_LOADED);var owner=foreign(200);row.cacheRelation("owner",owner);
        assertSame(owner,row.getProperty("owner"));assertTrue(row.isPropertyLoaded("owner"));assertEquals(EntityStatus.NEW,row.get$status());
    }
    @Test public void rejectedConversionCannotAlterAnExistingUnrelatedLedgerIntent(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var ledger=row.getEntityMutationLedger();ledger.set(new EntityKey("Other",100L),"name","keep unrelated pending value");
        var before=new Snapshot(row);assertThrows(ClassCastException.class,()->row.updateProperty("count","wrong type"));before.unchanged(row);
    }
    @Test public void rejectedFrameworkAssignmentDoesNotPublishAvailability(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var before=new Snapshot(row);
        assertThrows(ClassCastException.class,()->row.setProperty("count","wrong type"));before.unchanged(row);
    }
}
