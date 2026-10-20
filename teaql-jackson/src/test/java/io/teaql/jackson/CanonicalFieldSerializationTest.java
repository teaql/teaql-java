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
                        "optional_note", List.of("optionalNote", "optional_note")), Set.of("child_node")));
        private String displayName;
        private String optionalNote;
        private Row childNode;
        @Override public void __internalSet(String field, Object value) {
            switch (field) {
                case "display_name" -> { markPropertyLoaded(field); displayName = (String) value; }
                case "optional_note" -> { markPropertyLoaded(field); optionalNote = (String) value; }
                case "child_node" -> { markPropertyLoaded(field); childNode = (Row) value; }
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) {
                case "display_name" -> displayName;
                case "optional_note" -> optionalNote;
                case "child_node" -> childNode;
                default -> super.__internalGet(field);
            };
        }
    }

    private static ObjectMapper mapper(boolean singletonFirst) {
        assertTrue(Row.LAYOUT.isGenerated());
        var metadata = new io.teaql.core.meta.SimpleEntityMetaFactory();
        var descriptor = new io.teaql.core.meta.EntityDescriptor();
        descriptor.setType("CanonicalRow");
        descriptor.setTargetType(Row.class);
        descriptor.withEntitySupplier(Row::new);
        descriptor.addSimpleProperty("id", Long.class);
        descriptor.addSimpleProperty("version", Long.class);
        descriptor.addSimpleProperty("display_name", String.class);
        descriptor.addSimpleProperty("optional_note", String.class);
        descriptor.addSimpleProperty("child_node", Row.class);
        metadata.register(descriptor);
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
        var child = new Row();
        child.__internalSet("display_name", "child");
        row.__internalSet("child_node", child);
        var state = row.__internalLoadState();
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertEquals("child", json.get("childNode").get("displayName").asText());
        assertFalse(json.has("child_node"));
        var restored = mapper.treeToValue(json, Row.class);
        assertEquals("child", ((Row) restored.__internalGet("child_node")).__internalGet("display_name"));
        assertTrue(restored.isPropertyLoaded("child_node"));
        assertSame(state, row.__internalLoadState());
        assertFalse(restored.__internalHasMutationLedger());
    }

    @Test public void namedRelationNullAndNotLoadedRemainDistinct() throws Exception {
        var row = new Row();
        assertFalse(mapper.readTree(mapper.writeValueAsString(row)).has("childNode"));
        row.__internalSet("child_node", null);
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertTrue(json.get("childNode").isNull());
        var restored = mapper.treeToValue(json, Row.class);
        assertTrue(restored.isPropertyLoaded("child_node"));
        assertNull(restored.__internalGet("child_node"));
        assertFalse(restored.__internalHasMutationLedger());
    }

    @Test public void duplicateNamedRelationAliasesAreRejected() {
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("{\"childNode\":null,\"child_node\":null}", Row.class));
    }
}
