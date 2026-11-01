package io.teaql.core;

import io.teaql.data.dynamic.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class MutationRollbackReplayTest {
    static final class Row extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(Row.class, "rollback-replay-v1",
                Map.of("id",0,"version",1,"name",2,"note",3),
                Map.of("id",List.of("id","id"),"version",List.of("version","version"),
                        "name",List.of("name","name"),"note",List.of("note","note")),Set.of()));
        String name, note;
        @Override public Object __internalGet(String field) {
            return switch(field) { case "name" -> name; case "note" -> note; default -> super.__internalGet(field); };
        }
        @Override public void __internalSet(String field, Object value) {
            if(field.equals("name")) { name=(String)value; markPropertyLoaded(field); }
            else if(field.equals("note")) { note=(String)value; markPropertyLoaded(field); }
            else super.__internalSet(field,value);
        }
        void updateName(String value) { handleUpdate("name",name,value); __internalSet("name",value); }
    }
    private Row row(boolean selectedNote) {
        var row=new Row(); row.__internalHydrate("id",10L,0); row.__internalHydrate("version",1L,1);
        row.__internalHydrate("name","original",2); if(selectedNote) row.__internalHydrate("note",null,3);
        row.set$status(EntityStatus.PERSISTED); return row;
    }

    @Test public void replayKeepsTheFirstKnownNativeBaseline() {
        var row=row(true); row.updateName("first");
        var restore=row.__internalCaptureMutationRollback();var ledger=row.getEntityMutationLedger().__internalCaptureRollback();
        row.__internalSet("name","later");row.__internalSet("version",2L);row.clearUpdatedProperties();row.getEntityMutationLedger().clearCurrentChangeSet();
        row.__internalSet("name","first");row.__internalSet("note",null);restore.run();ledger.run();
        row.__internalReplayRetryNative("name","later");row.__internalReplayRetryNative("note","later note");
        assertEquals("original",row.getOldValue("name"));assertNull(row.getOldValue("note"));
        assertEquals(Set.of("name","note"),Set.copyOf(row.getUpdatedProperties()));assertEquals(Long.valueOf(1),row.getVersion());
        row.updateName("original");assertFalse(row.getUpdatedProperties().contains("name"));
        assertTrue(row.getUpdatedProperties().contains("note"));
    }

    @Test public void replayOfAnUnknownOriginalNullDoesNotCancelExplicitInput() {
        var row=row(false);assertFalse(row.isPropertyLoaded("note"));
        row.__internalReplayRetryNative("note",null);
        assertTrue(row.isPropertyLoaded("note"));assertTrue(row.getUpdatedProperties().contains("note"));
        assertFalse(row.__internalPropertyChange("note").wasOldValueLoaded());
        assertTrue(row.getEntityMutationLedger().containsField(new EntityKey(row.typeName(),10L),"note"));
    }

    @Test public void dynamicReplayKeepsOriginalNullAndUnselectedDefinition() {
        var row=row(true);var metadata=new DynamicFieldMetadata(Map.of("edited",DynamicDataType.STRING,"hidden",DynamicDataType.STRING));
        var original=new DynamicFieldValues(metadata,Map.of("edited",DynamicFieldValue.ofNull("edited",DynamicDataType.STRING)));
        row.__internalHydrateDynamicFields(original,row.__internalLoadState().withDynamicSelection(original.selectedCodes()));
        row.updateDynamicField("edited","first");var restore=row.__internalCaptureMutationRollback();
        var ledger=row.getEntityMutationLedger().__internalCaptureRollback();
        row.updateDynamicField("edited","later");var latest=row.__internalDynamicMutations().get("edited");
        restore.run();ledger.run();row.__internalReplayRetryDynamic(latest);
        assertEquals("later",row.dynamicFields().field("edited").value());
        assertEquals(DynamicFieldValue.State.NULL,row.__internalDynamicOriginalValues().get("edited").state());
        assertEquals(DynamicFieldValue.State.NOT_LOADED,row.dynamicFields().field("hidden").state());
        assertSame(metadata,row.dynamicFields().metadata());assertNull(Row.LAYOUT.findIndex("#edited"));
    }

    @Test public void restoringOwnershipCannotClearAnotherLedger() {
        var row=row(true);var original=row.getEntityMutationLedger();var restore=row.__internalCaptureMutationRollback();
        var other=new EntityMutationLedger();var unrelated=new EntityKey("Other",99L);other.set(unrelated,"note","pending");
        row.setEntityMutationLedger(other);restore.run();
        assertSame(original,row.getEntityMutationLedger());assertEquals("pending",other.get(unrelated,"note"));
        assertFalse(original.containsField(unrelated,"note"));
    }
}
