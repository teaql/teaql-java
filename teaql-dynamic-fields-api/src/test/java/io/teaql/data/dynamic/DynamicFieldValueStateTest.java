package io.teaql.data.dynamic;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class DynamicFieldValueStateTest {
    @Test
    public void boundDefinitionsShareTheDictionaryButKeepStorageAndScopeIdentity() {
        var unbound = new DynamicFieldMetadata(Map.of("note", DynamicDataType.STRING));
        Object storage = new Object();
        var bound = unbound.withStorageBinding(storage, "PROFILE", "one", "School");
        assertSame(unbound.types(), bound.types());
        assertSame(bound, bound.withStorageBinding(storage, "PROFILE", "one", "School"));
        assertTrue(bound.matchesStorage(storage, "PROFILE", "one", "School"));
        assertFalse(unbound.matchesStorage(storage, "PROFILE", "one", "School"));
        assertFalse(unbound.matchesStorage(null, null, null, null));
        assertThrows(NullPointerException.class, () -> unbound.withStorageBinding(null, null, null, null));
        assertFalse(bound.matchesStorage(new Object(), "PROFILE", "one", "School"));
        assertFalse(bound.matchesStorage(storage, "PROFILE", "two", "School"));
        assertFalse(bound.matchesStorage(storage, "PROFILE", "one", "Platform"));
        assertThrows(UnsupportedOperationException.class, () -> bound.types().clear());
        assertFalse(bound.toString().contains("PROFILE"));
    }

    @Test
    public void zeroFalseEmptyNullAndNotLoadedRemainDistinct() {
        for (DynamicFieldValue value : new DynamicFieldValue[]{
                DynamicFieldValue.ofNumber("number", 0), DynamicFieldValue.ofBool("flag", false),
                DynamicFieldValue.ofString("text", "")}) {
            assertEquals(DynamicFieldValue.State.VALUE, value.state());
            assertTrue(value.isLoaded());
        }
        DynamicFieldValue nil = DynamicFieldValue.ofNull("text", DynamicDataType.STRING);
        DynamicFieldValue missing = DynamicFieldValue.notLoaded("text", DynamicDataType.STRING);
        assertEquals(DynamicFieldValue.State.NULL, nil.state());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, missing.state());
        assertNull(nil.value()); assertNull(missing.value());
        assertNotEquals(nil, missing);
    }

    @Test
    public void sharedMetadataKeepsRowPayloadsPrivateAndSynthesizesOnlyKnownWrappers() {
        DynamicFieldMetadata metadata = new DynamicFieldMetadata(Map.of("note", DynamicDataType.STRING, "extra", DynamicDataType.STRING));
        DynamicFieldValues first = new DynamicFieldValues(metadata, Map.of("note", DynamicFieldValue.ofString("note", "private")));
        DynamicFieldValues second = new DynamicFieldValues(metadata, Map.of("note", DynamicFieldValue.ofNull("note", DynamicDataType.STRING)));
        assertSame(first.metadata(), second.metadata());
        assertEquals(DynamicFieldValue.State.VALUE, first.field("note").state());
        assertEquals(DynamicFieldValue.State.NULL, second.field("note").state());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, first.field("extra").state());
        assertFalse(first.isSelected("extra")); assertFalse(first.isNull("extra"));
        assertEquals("private", first.field("note").value());
        assertThrows(DynamicFieldException.class, () -> first.field("unknown"));
        assertEquals(1, first.toMap().size());
        assertEquals(java.util.Set.of("note"), first.selectedCodes());
        assertEquals(first.selectedCodes(), second.selectedCodes());
        assertSame(first.selectedCodes(), first.selectedCodes());
        assertThrows(UnsupportedOperationException.class, () -> first.selectedCodes().clear());
    }

    @Test
    public void explicitNotLoadedWrapperDoesNotPretendToBeSelectedNull() {
        DynamicFieldValues values = DynamicFieldValues.of(java.util.List.of(DynamicFieldValue.notLoaded("note", DynamicDataType.STRING)));
        assertFalse(values.isSelected("note")); assertFalse(values.isNull("note"));
        assertThrows(DynamicFieldException.class, () -> values.getString("note"));
        assertNull(values.field("note").value());
        assertTrue(values.selectedCodes().isEmpty());
        assertFalse(values.selectedCodes().contains("note"));
    }

    @Test
    public void missingWrappersShareOnlyValueFreeMetadataAndCannotChangeLoadedRows() {
        DynamicFieldMetadata metadata = new DynamicFieldMetadata(Map.of("note", DynamicDataType.STRING));
        DynamicFieldValues first = new DynamicFieldValues(metadata, Map.of());
        DynamicFieldValues second = new DynamicFieldValues(metadata, Map.of());
        DynamicFieldValue missing = first.field("note");
        java.util.stream.IntStream.range(0, 10_000).parallel().forEach(i -> assertSame(missing, second.field("note")));
        assertSame(missing, first.field("note"));
        DynamicFieldValues loaded = new DynamicFieldValues(metadata, Map.of("note", DynamicFieldValue.ofString("note", "private")));
        DynamicFieldValues nil = new DynamicFieldValues(metadata, Map.of("note", DynamicFieldValue.ofNull("note", DynamicDataType.STRING)));
        assertNotSame(missing, loaded.field("note"));
        assertNotSame(missing, nil.field("note"));
        assertEquals(DynamicFieldValue.State.NOT_LOADED, missing.state());
        assertNull(missing.value());
        assertFalse(first.isSelected("note"));
        assertEquals("private", loaded.field("note").value());
        assertThrows(DynamicFieldException.class, () -> first.field("unknown"));
    }
}
