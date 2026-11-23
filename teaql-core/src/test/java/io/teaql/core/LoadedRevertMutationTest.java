package io.teaql.core;

import java.util.Set;
import org.junit.Test;
import io.teaql.data.dynamic.DynamicDataType;
import io.teaql.data.dynamic.DynamicFieldMutation;
import static io.teaql.core.RejectedMutationStateTest.*;
import static org.junit.Assert.*;

/** Returning to a known persisted baseline must not retain an intermediate write. */
public class LoadedRevertMutationTest {
    static EntityKey key(Row row){return new EntityKey(row.typeName(),row.getId());}
    static void assertNoNameIntent(Row row){
        assertFalse(row.getUpdatedProperties().contains("name"));
        assertFalse(row.getEntityMutationLedger().changedFieldNames(key(row)).contains("name"));
        assertNull(row.getEntityMutationLedger().get(key(row),"name"));
    }
    @Test public void typedKnownValueRevertRemovesTheIntermediateLedgerValue(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateName("intermediate");row.updateName("kept");
        assertEquals("kept",row.name);assertNoNameIntent(row);assertTrue(row.getEntityMutationLedger().currentChangeSet().isEmpty());
    }
    @Test public void genericKnownNullRevertRemovesTheIntermediateLedgerValue(){
        var row=row(EntityStatus.PERSISTED,Availability.NULL);row.updateProperty("name","intermediate");row.updateProperty("name",null);
        assertNull(row.name);assertNoNameIntent(row);
    }
    @Test public void restoringTheOnlyFieldRestoresTheCleanLifecycle(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateName("intermediate");row.updateName("kept");
        assertEquals(EntityStatus.PERSISTED,row.get$status());assertFalse(row.needPersist());
    }
    @Test public void restoringOneFieldRetainsAnotherFieldsIntent(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateProperty("count",3);row.updateName("intermediate");row.updateName("kept");
        assertNoNameIntent(row);assertEquals(Set.of("count"),Set.copyOf(row.getUpdatedProperties()));
        assertEquals(Integer.valueOf(3),row.getEntityMutationLedger().get(key(row),"count"));assertEquals(EntityStatus.UPDATED,row.get$status());
    }
    @Test public void restoringOneEntityCannotEraseAnotherTypeWithTheSameId(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var ledger=row.getEntityMutationLedger();var other=new EntityKey("Other",row.getId());
        ledger.set(other,"name","unrelated pending value");ledger.setOriginalVersion(other,19L);
        row.updateName("intermediate");row.updateName("kept");assertNoNameIntent(row);
        assertEquals("unrelated pending value",ledger.get(other,"name"));assertEquals(Long.valueOf(19),ledger.getOriginalVersion(other));
        assertEquals(Set.of("name"),ledger.changedFieldNames(other));
    }
    @Test public void restoringAFieldDoesNotCancelPendingDeletion(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.markForDeletion();row.updateName("intermediate");row.updateName("kept");
        assertNoNameIntent(row);assertEquals(EntityStatus.UPDATED_DELETED,row.get$status());assertTrue(row.isMarkedAsDelete());
        assertTrue(row.getEntityMutationLedger().deletedKeys().contains(key(row)));
    }
    @Test public void restoringRetainsTheLoadSnapshotVersionAndLineage(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);var state=row.__internalLoadState();var trace=row.getTraceChain();var comment=row.getComment();
        row.updateName("intermediate");var ledger=row.getEntityMutationLedger();row.updateName("kept");
        assertSame(state,row.__internalLoadState());assertSame(ledger,row.getEntityMutationLedger());
        assertEquals(Long.valueOf(7),row.getOriginalVersion());assertEquals(Long.valueOf(7),ledger.getOriginalVersion(key(row)));
        assertEquals(trace,row.getTraceChain());assertEquals(trace,ledger.getTraceChain(key(row)));assertEquals(comment,row.getComment());
    }
    @Test public void restoringThePersistentBaselineDiscardsOlderStagedValuesOfOnlyThatField(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateProperty("count",3);row.updateName("outer");
        var ledger=row.getEntityMutationLedger();ledger.pushChangeSet();row.updateName("inner");row.updateName("kept");
        assertNoNameIntent(row);ledger.popChangeSet();assertNoNameIntent(row);
        assertEquals(Integer.valueOf(3),ledger.get(key(row),"count"));assertEquals(EntityStatus.UPDATED,row.get$status());
    }
    @Test public void restoringANativeFieldCannotEraseDynamicIntent(){
        var row=row(EntityStatus.PERSISTED,Availability.VALUE);row.updateName("intermediate");var ledger=row.getEntityMutationLedger();
        var mutation=DynamicFieldMutation.set("note",DynamicDataType.STRING,"pending dynamic value");ledger.setDynamic(key(row),mutation);
        row.updateName("kept");assertNoNameIntent(row);assertEquals(EntityStatus.UPDATED,row.get$status());
        assertEquals(mutation,ledger.currentChangeSet().dynamicChanges().get(key(row)).get("note"));assertTrue(row.needPersist());
    }
    @Test public void anUnknownOriginalNullMustNotBeTreatedAsAKnownBaseline(){
        var row=row(EntityStatus.PERSISTED,Availability.NOT_LOADED);row.updateProperty("name","intermediate");row.updateProperty("name",null);
        assertEquals(EntityStatus.UPDATED,row.get$status());assertTrue(row.getUpdatedProperties().contains("name"));
        assertTrue(row.getEntityMutationLedger().currentChangeSet().changes().get(key(row)).containsKey("name"));
        assertNull(row.getEntityMutationLedger().get(key(row),"name"));
    }
}
