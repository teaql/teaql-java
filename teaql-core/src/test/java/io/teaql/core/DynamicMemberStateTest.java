package io.teaql.core;

import io.teaql.data.dynamic.*;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class DynamicMemberStateTest {
    static class Row extends BaseEntity {
        String name;
        @Override public Object __internalGet(String field) {
            return "name".equals(field) ? name : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if ("name".equals(field)) name = (String) value; else super.__internalSet(field, value);
        }
    }

    @Test
    public void fixedNameDerivedNameAndPersistentNameDoNotShadowEachOther() {
        Row row = new Row(); row.setProperty("name", "fixed");
        row.addDynamicProperty("name", "derived");
        row.setDynamicFieldValues(DynamicFieldValues.of(List.of(DynamicFieldValue.ofString("name", "extension"))));
        assertEquals("fixed", row.getProperty("name"));
        assertEquals("derived", row.getProperty("_name"));
        assertEquals("extension", row.getProperty("#name"));
        assertEquals("derived", row.getDynamicProperty("name"));
        assertEquals("extension", row.dynamicFields().getString("name"));
        assertEquals(1, row.collectDynamicFieldValues().size());
        assertTrue(row.getUpdatedProperties().isEmpty());
    }

    @Test
    public void derivedNullAndMissingReadNullWithoutInventingZeroOrPersistentFields() {
        Row row = new Row();
        assertNull(row.getDynamicProperty("missing"));
        row.addDynamicProperty("count", null);
        assertTrue(row.getAdditionalInfo().containsKey("_count"));
        assertNull(row.getDynamicProperty("count"));
        row.addDynamicProperty("count", 0);
        assertEquals(Integer.valueOf(0), row.getDynamicProperty("count"));
        assertFalse(row.isPropertyLoaded("_count"));
        assertEquals(0, row.collectDynamicFieldValues().size());
    }

    @Test
    public void readonlyFalseEmptyNullAndAbsenceHavePrivatePresenceWithoutMutationOrSlots() {
        Row row = new Row();
        var state = row.__internalLoadState();
        row.addDynamicProperty("flag", false);
        row.addDynamicProperty("empty", "");
        row.addDynamicProperty("null", null);
        assertEquals(Boolean.FALSE, row.getDynamicProperty("flag"));
        assertEquals("", row.getDynamicProperty("empty"));
        assertNull(row.getDynamicProperty("null"));
        assertNull(row.getDynamicProperty("absent"));
        assertTrue(row.hasDynamicProperty("flag"));
        assertTrue(row.hasDynamicProperty("empty"));
        assertTrue(row.hasDynamicProperty("null"));
        assertFalse(row.hasDynamicProperty("absent"));
        assertSame(state, row.__internalLoadState());
        for (String name : List.of("_flag", "_empty", "_null", "_absent")) {
            assertFalse(row.isPropertyLoaded(name));
            assertNull(state.layout().findIndex(name));
        }
        assertTrue(row.getUpdatedProperties().isEmpty());
        assertFalse(row.__internalHasMutationLedger());
        assertEquals(0, row.collectDynamicFieldValues().size());
    }

    @Test
    public void sharedSelectionKeepsWrapperNullAndValuePrivateAndLeavesFixedSlotsUnchanged() {
        DynamicFieldMetadata metadata = new DynamicFieldMetadata(Map.of("note", DynamicDataType.STRING, "extra", DynamicDataType.STRING));
        Row first = new Row(); Row second = new Row();
        first.setDynamicFieldValues(new DynamicFieldValues(metadata, Map.of("note", DynamicFieldValue.ofString("note", "first"))));
        second.setDynamicFieldValues(new DynamicFieldValues(metadata, Map.of("note", DynamicFieldValue.ofNull("note", DynamicDataType.STRING))));
        SmartList<Row> list = new SmartList<>(List.of(first, second));
        assertSame(first.__internalLoadState(), second.__internalLoadState());
        assertEquals(DynamicFieldValue.State.VALUE, first.dynamicFields().field("note").state());
        assertEquals(DynamicFieldValue.State.NULL, second.dynamicFields().field("note").state());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, second.dynamicFields().field("extra").state());
        assertTrue(first.isPropertyLoaded("#note"));
        assertFalse(first.isPropertyLoaded("name"));
        assertEquals(0L, first.__internalLoadState().bits());
        assertNotSame(first.getEntityMutationLedger(), second.getEntityMutationLedger());
        assertEquals(2, list.size());
    }
}
