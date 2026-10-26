package io.teaql.core;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import static io.teaql.core.RejectedMutationStateTest.*;
import static org.junit.Assert.*;

public class ExplicitNullMutationTest {
    @Test public void missingNullViaHookIsAnExplicitMutation(){
        var row=row(EntityStatus.PERSISTED,Availability.NOT_LOADED);row.handleUpdate("name",null,null);
        assertTrue(row.isPropertyLoaded("name"));assertEquals(EntityStatus.UPDATED,row.get$status());
        assertEquals(List.of("name"),row.getUpdatedProperties());
        var key=new EntityKey(row.typeName(),row.getId());
        assertTrue(row.getEntityMutationLedger().currentChangeSet().changes().get(key).containsKey("name"));
        assertNull(row.getEntityMutationLedger().get(key,"name"));assertEquals(Long.valueOf(7),row.getOriginalVersion());
    }
    @Test public void genericMissingNullRetainsIntent(){
        var row=row(EntityStatus.PERSISTED,Availability.NOT_LOADED);row.updateProperty("name",null);
        assertTrue(row.isPropertyLoaded("name"));assertEquals(List.of("name"),row.getUpdatedProperties());
        assertEquals(EntityStatus.UPDATED,row.get$status());
    }
    @Test public void nullIntentWithUnknownBaselineSurvivesLaterChanges(){
        var row=row(EntityStatus.PERSISTED,Availability.NOT_LOADED);
        row.updateProperty("name",null);row.updateProperty("name","later");row.updateProperty("name",null);row.updateProperty("name",null);
        assertEquals(List.of("name"),row.getUpdatedProperties());assertTrue(row.isPropertyLoaded("name"));assertNull(row.name);
        var key=new EntityKey(row.typeName(),row.getId());assertTrue(row.getEntityMutationLedger().changedFieldNames(key).contains("name"));
        assertNull(row.getEntityMutationLedger().currentChangeSet().get(key,"name"));
    }
    @Test public void loadedNullGenericMutationIsANoop(){
        var row=row(EntityStatus.PERSISTED,Availability.NULL);var snapshot=new Snapshot(row);row.updateProperty("name",null);snapshot.unchanged(row);
    }
    @Test public void loadedEqualGenericMutationIsANoop(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var snapshot=new Snapshot(row);row.updateProperty("name","kept");snapshot.unchanged(row);
    }
    @Test public void loadedEqualHookIsANoop(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var snapshot=new Snapshot(row);row.handleUpdate("name","kept","kept");snapshot.unchanged(row);
    }
    @Test public void equalLoadedValueCannotEraseAnotherPendingIntent(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateProperty("count",3);var snapshot=new Snapshot(row);row.updateProperty("name","kept");snapshot.unchanged(row);
        assertEquals(Set.of("count"),Set.copyOf(row.getUpdatedProperties()));
    }
    @Test public void referenceNullVersionIsRejectedWithoutSelectingIt(){
        var row=row(EntityStatus.REFER,Availability.NOT_LOADED);var snapshot=new Snapshot(row);
        assertThrows(TeaQLRuntimeException.class,()->row.updateVersion(null));snapshot.unchanged(row);
    }
    @Test public void referenceGenericNullStillRejectsWithoutAnyChange(){
        var row=row(EntityStatus.REFER,Availability.NOT_LOADED);var snapshot=new Snapshot(row);
        assertThrows(TeaQLRuntimeException.class,()->row.updateProperty("name",null));snapshot.unchanged(row);
    }
    @Test public void knownValueToNullIsStillAnEdit(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateProperty("name",null);
        assertEquals(EntityStatus.UPDATED,row.get$status());assertTrue(row.isPropertyLoaded("name"));assertNull(row.name);
        assertEquals(List.of("name"),row.getUpdatedProperties());assertEquals("kept",row.getOldValue("name"));
    }
}
