package io.teaql.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class DynamicPropertyMetadataTest {
    static class Row extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(
            Row.class, "metadata-v1", Map.of("id",0,"version",1,"name",2),
            Map.of("id",List.of("id","id"),"version",List.of("version","version"),"name",List.of("name","name")), Set.of()));
        String name;
        @Override public Object __internalGet(String field) { return "name".equals(field) ? name : super.__internalGet(field); }
        @Override public void __internalSet(String field,Object value) {
            if("name".equals(field)) { name=(String)value;markPropertyLoaded(field); } else super.__internalSet(field,value);
        }
    }
    @Test public void sharedSchemaDoesNotInstallValuesOrNumberReadonlyProperties() {
        DynamicPropertyMetadata metadata = new DynamicPropertyMetadata(Map.of("_count",Long.class,"_missing",Long.class));
        LoadState state = LoadState.projection(Row.LAYOUT,List.of("id","version","name")).withDynamicPropertyMetadata(metadata);
        Row first=new Row();Row second=new Row();
        first.__internalUseLoadState(state);second.__internalUseLoadState(state);
        first.addDynamicProperty("count",0L);second.addDynamicProperty("count",null);
        SmartList<Row> rows=new SmartList<>(List.of(first,second));
        assertSame(first.__internalLoadState(),second.__internalLoadState());
        assertSame(metadata,first.__internalLoadState().dynamicPropertyMetadata());
        assertSame(Long.class,first.getDynamicPropertyType("missing"));
        assertNull(first.getDynamicProperty("missing"));assertFalse(first.getAdditionalInfo().containsKey("_missing"));
        assertEquals(Long.valueOf(0),first.getDynamicProperty("count"));assertNull(second.getDynamicProperty("count"));
        assertTrue(second.getAdditionalInfo().containsKey("_count"));
        assertFalse(state.isLoaded("_count"));assertNull(Row.LAYOUT.findIndex("_count"));
        assertTrue(first.getUpdatedProperties().isEmpty());assertEquals(0,first.collectDynamicFieldValues().size());
        assertEquals(2,rows.size());
    }
    @Test public void incompatibleSchemasNeverShareAndAvailabilityChangesKeepMetadata() {
        LoadState original=LoadState.projection(Row.LAYOUT,List.of("id"))
            .withDynamicPropertyMetadata(new DynamicPropertyMetadata(Map.of("_count",Long.class)));
        LoadState other=LoadState.projection(Row.LAYOUT,List.of("id"))
            .withDynamicPropertyMetadata(new DynamicPropertyMetadata(Map.of("_count",String.class)));
        assertNotEquals(original,other);
        LoadState changed=original.withLoaded("name",true).withDynamicSelection(Set.of("note"));
        assertSame(original.dynamicPropertyMetadata(),changed.dynamicPropertyMetadata());
        assertFalse(original.isLoaded("name"));assertTrue(changed.isLoaded("name"));
        assertFalse(changed.isLoaded("_count"));
    }
    @Test public void invalidTypesAndNamespacesRejectWithoutExposingValues() {
        assertThrows(IllegalArgumentException.class,()->new DynamicPropertyMetadata(Map.of("name",String.class)));
        assertThrows(IllegalArgumentException.class,()->new DynamicPropertyMetadata(Map.of("#count",Long.class)));
        assertThrows(IllegalArgumentException.class,()->new DynamicPropertyMetadata(Map.of("_count",long.class)));
        DynamicPropertyMetadata metadata=new DynamicPropertyMetadata(Map.of("_count",Long.class));
        Exception error=assertThrows(IllegalArgumentException.class,()->metadata.validate("_count","secret-value"));
        assertFalse(error.getMessage().contains("secret-value"));
        assertThrows(UnsupportedOperationException.class,()->metadata.types().put("_other",String.class));
    }
}
