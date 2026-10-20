package io.teaql.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.FieldLayout;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class CanonicalFieldSerializationTest {
    static final class Row extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(
                Row.class, "canonical-json-v1",
                Map.of("id", 0, "version", 1, "display_name", 2, "optional_note", 3),
                Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                        "display_name", List.of("displayName", "display_name"),
                        "optional_note", List.of("optionalNote", "optional_note")), Set.of("child_list")));
        private String displayName;
        private String optionalNote;
        private io.teaql.core.SmartList<Child> childList;
        @Override public void __internalSet(String field, Object value) {
            switch (field) {
                case "display_name" -> { markPropertyLoaded(field); displayName = (String) value; }
                case "optional_note" -> { markPropertyLoaded(field); optionalNote = (String) value; }
                case "child_list" -> { markPropertyLoaded(field); childList = (io.teaql.core.SmartList<Child>) value; }
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) {
                case "display_name" -> displayName;
                case "optional_note" -> optionalNote;
                case "child_list" -> childList;
                default -> super.__internalGet(field);
            };
        }
    }

    static final class Child extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(
                Child.class, "canonical-child-json-v1",
                Map.of("id", 0, "version", 1, "parent_row", 2),
                Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                        "parent_row", List.of("parentRow", "parent_row")), Set.of()));
        private Row parentRow;
        @Override public void __internalSet(String field, Object value) {
            if ("parent_row".equals(field)) { markPropertyLoaded(field); parentRow = (Row) value; }
            else super.__internalSet(field, value);
        }
        @Override public Object __internalGet(String field) {
            return "parent_row".equals(field) ? parentRow : super.__internalGet(field);
        }
    }

    private static ObjectMapper mapper(boolean singletonFirst) {
        assertTrue(Row.LAYOUT.isGenerated());
        assertTrue(Child.LAYOUT.isGenerated());
        var metadata = new io.teaql.core.meta.SimpleEntityMetaFactory();
        var descriptor = new io.teaql.core.meta.EntityDescriptor();
        descriptor.setType("CanonicalRow");
        descriptor.setTargetType(Row.class);
        descriptor.withEntitySupplier(Row::new);
        descriptor.addSimpleProperty("id", Long.class);
        descriptor.addSimpleProperty("version", Long.class);
        descriptor.addSimpleProperty("display_name", String.class);
        descriptor.addSimpleProperty("optional_note", String.class);
        metadata.register(descriptor);
        var child = new io.teaql.core.meta.EntityDescriptor();
        child.setType("CanonicalChild");
        child.setTargetType(Child.class);
        child.withEntitySupplier(Child::new);
        child.addSimpleProperty("id", Long.class);
        child.addSimpleProperty("version", Long.class);
        child.addObjectProperty(metadata, "parent_row", "CanonicalRow", "child_list",
                Row.class, io.teaql.core.meta.Relation.class);
        metadata.register(child);
        var context = new io.teaql.runtime.DefaultUserContext(
                io.teaql.runtime.TeaQLRuntime.builder().metadata(metadata).build());
        var mapper = new ObjectMapper();
        if (singletonFirst) mapper.registerModule(TeaQLModule.INSTANCE);
        return mapper.registerModule(new TeaQLModule(context));
    }

    private final ObjectMapper mapper = mapper(false);

    @Test public void loadedValueUsesCanonicalAccessorAndCamelCaseWireName() throws Exception {
        var row = new Row();
        row.__internalSet("display_name", "sample");
        var state = row.__internalLoadState();
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertEquals("sample", json.get("displayName").asText());
        assertFalse(json.has("display_name"));
        assertFalse(json.has("optionalNote"));
        assertSame(state, row.__internalLoadState());
        assertFalse(row.__internalHasMutationLedger());
        var restored = mapper.readValue(mapper.writeValueAsString(row), Row.class);
        assertEquals("sample", restored.__internalGet("display_name"));
        assertFalse(restored.__internalHasMutationLedger());
    }

    @Test public void loadedNullIsNotConfusedWithNotLoaded() throws Exception {
        var row = new Row();
        row.__internalSet("optional_note", null);
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertTrue(json.has("optionalNote"));
        assertTrue(json.get("optionalNote").isNull());
        assertFalse(json.has("optional_note"));
        assertFalse(json.has("displayName"));
        assertFalse(row.__internalHasMutationLedger());
    }

    @Test public void missingFieldsRemainOmittedWithoutAllocatingMutationState() throws Exception {
        var row = new Row();
        var state = row.__internalLoadState();
        assertEquals(0, mapper.readTree(mapper.writeValueAsString(row)).size());
        assertSame(state, row.__internalLoadState());
        assertFalse(row.__internalHasMutationLedger());
    }

    @Test public void contextWriterOverridesTheSerializationOnlySingleton() throws Exception {
        var row = new Row();
        row.__internalSet("display_name", "context-bound");
        var mapper = mapper(true);
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertEquals("context-bound", json.get("displayName").asText());
        assertFalse(json.has("display_name"));
        assertFalse(row.__internalHasMutationLedger());
    }

    @Test public void canonicalNamedRelationUsesCamelCaseAndRoundTrips() throws Exception {
        var row = new Row();
        var child = new Child();
        child.__internalSet("id", 7L);
        var children = new io.teaql.core.SmartList<Child>();
        children.add(child);
        row.__internalSet("child_list", children);
        var state = row.__internalLoadState();
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertEquals(7L, json.get("childList").get(0).get("id").asLong());
        assertFalse(json.has("child_list"));
        var restored = mapper.treeToValue(json, Row.class);
        assertEquals(Long.valueOf(7), ((io.teaql.core.SmartList<?>) restored.__internalGet("child_list")).first().getId());
        assertTrue(restored.isPropertyLoaded("child_list"));
        assertSame(state, row.__internalLoadState());
        assertFalse(restored.__internalHasMutationLedger());
    }

    @Test public void namedRelationNullAndNotLoadedRemainDistinct() throws Exception {
        var row = new Row();
        assertFalse(mapper.readTree(mapper.writeValueAsString(row)).has("childList"));
        row.__internalSet("child_list", null);
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertTrue(json.get("childList").isNull());
        var restored = mapper.treeToValue(json, Row.class);
        assertTrue(restored.isPropertyLoaded("child_list"));
        assertNull(restored.__internalGet("child_list"));
        assertFalse(restored.__internalHasMutationLedger());
        row.__internalSet("child_list", new io.teaql.core.SmartList<Child>());
        json = mapper.readTree(mapper.writeValueAsString(row));
        assertTrue(json.get("childList").isArray());
        assertEquals(0, json.get("childList").size());
    }

    @Test public void duplicateNamedRelationAliasesAreRejected() {
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("{\"childList\":null,\"child_list\":null}", Row.class));
    }
}
