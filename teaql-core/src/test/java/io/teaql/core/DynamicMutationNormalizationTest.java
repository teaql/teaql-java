package io.teaql.core;

import io.teaql.data.dynamic.*;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class DynamicMutationNormalizationTest {
    private static DynamicMemberStateTest.Row loaded(boolean known, String value) {
        var row = new DynamicMemberStateTest.Row();
        row.__internalHydrate("id", 1L, row.__internalLoadState().layout().indexForLoad("id"));
        row.__internalHydrate("version", 7L, row.__internalLoadState().layout().indexForLoad("version"));
        row.setDynamicFieldValues(new DynamicFieldValues(new DynamicFieldMetadata(Map.of(
                "note", DynamicDataType.STRING, "other", DynamicDataType.STRING)),
                known ? Map.of("note", DynamicFieldValue.ofString("note", value)) : Map.of()));
        row.set$status(EntityStatus.PERSISTED); return row;
    }

    @Test public void equalLoadedValueDoesNotAllocateALedgerOrCreateMutationIntent() {
        for (String value : java.util.Arrays.asList("same", null)) {
            var row = loaded(true, value); var state = row.__internalLoadState();
            row.updateDynamicField("note", value);
            assertSame(state, row.__internalLoadState()); assertFalse(row.__internalHasMutationLedger());
            assertFalse(row.needPersist()); assertTrue(row.__internalDynamicMutations().isEmpty());
        }
    }
    @Test public void restoreKnownNullCancelsOnlyThisDynamicCodeAcrossAllFrames() {
        var row = loaded(true, null); row.updateDynamicField("note", "outer");
        var ledger = row.getEntityMutationLedger(); var key = new EntityKey(row.typeName(), row.getId());
        ledger.pushChangeSet(); row.updateDynamicField("note", "inner"); row.updateDynamicField("note", null);
        assertEquals(DynamicFieldValue.State.NULL, row.dynamicFields().field("note").state());
        assertFalse(row.needPersist()); assertTrue(ledger.changedFieldNames(key).isEmpty());
        ledger.popChangeSet(); assertTrue(ledger.changedFieldNames(key).isEmpty());
        assertEquals(Long.valueOf(7), row.getOriginalVersion());
    }
    @Test public void restorationRetainsAnotherDynamicFieldAndAnotherEntitysIntent() {
        var row = loaded(true, "original"); row.updateDynamicField("note", "edit");
        row.updateDynamicField("other", "keep"); var ledger = row.getEntityMutationLedger();
        var other = new EntityKey("AnotherType", 1L); ledger.set(other, "name", "keep other type");
        row.updateDynamicField("note", "original");
        assertEquals(java.util.Set.of("other"), row.__internalDynamicMutations().keySet());
        assertEquals("keep other type", ledger.get(other, "name")); assertTrue(row.needPersist());
    }
    @Test public void unknownNullCannotBeCancelledAgainstAnInventedBaseline() {
        var row = loaded(false, null); row.updateDynamicField("note", "edit"); row.updateDynamicField("note", null);
        assertTrue(row.needPersist()); assertTrue(row.__internalDynamicMutations().containsKey("note"));
    }
    @Test public void anEqualValueOnANewEntityIsStillAPersistableAssignment() {
        var row = new DynamicMemberStateTest.Row(); row.__internalInitializeNewEntityId(2L);
        row.setDynamicFieldValues(DynamicFieldValues.of(java.util.List.of(DynamicFieldValue.ofString("note", "same"))));
        row.updateDynamicField("note", "same");
        assertTrue(row.isNew()); assertTrue(row.__internalDynamicMutations().containsKey("note"));
    }
    @Test public void restoringAnExtensionRetainsAFixedFieldAndDeletion() {
        var row = loaded(true, "original"); row.markForDeletion(); row.updateProperty("name", "keep");
        row.updateDynamicField("note", "edit"); row.updateDynamicField("note", "original");
        assertTrue(row.isMarkedAsDelete()); assertTrue(row.needPersist());
        assertTrue(row.getUpdatedProperties().contains("name")); assertTrue(row.__internalDynamicMutations().isEmpty());
        assertEquals("keep", row.getEntityMutationLedger().get(new EntityKey(row.typeName(), row.getId()), "name"));
    }
    @Test public void readonlyLifecycleRejectsEvenAnEqualDynamicAssignment() {
        var row = loaded(true, "same"); row.set$status(EntityStatus.REFER);
        assertThrows(TeaQLRuntimeException.class, () -> row.updateDynamicField("note", "same"));
        assertFalse(row.__internalHasMutationLedger()); assertTrue(row.__internalDynamicMutations().isEmpty());
    }
}
