package io.teaql.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.FieldLayout;
import io.teaql.core.SmartList;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class TypedEntityJsonDeserializerTest {
    public static class UnregisteredRow extends BaseEntity {}
    public static class Row extends BaseEntity {
        static final FieldLayout LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(Row.class, "json-v1",
                Map.of("id", 0, "version", 1, "name", 2, "address", 3, "count", 4, "active", 5, "base_url", 6),
                Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                        "name", List.of("name", "name"), "address", List.of("address", "address"),
                        "count", List.of("count", "count"), "active", List.of("active", "active"),
                        "base_url", List.of("baseUrl", "legacy_url")), Set.of("child", "childList")));
        String name, address, baseUrl, owner;
        Integer count;
        Boolean active;
        Row child;
        SmartList<Row> childList;
        @Override public void __internalSet(String field, Object value) {
            markPropertyLoaded(field);
            switch (field) {
                case "name" -> name = (String) value;
                case "address" -> address = (String) value;
                case "baseUrl" -> baseUrl = (String) value;
                case "count" -> count = (Integer) value;
                case "active" -> active = (Boolean) value;
                case "child" -> child = (Row) value;
                case "childList" -> childList = (SmartList<Row>) value;
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) {
                case "name" -> name; case "address" -> address; case "baseUrl" -> baseUrl;
                case "count" -> count; case "active" -> active; case "child" -> child; case "childList" -> childList;
                default -> super.__internalGet(field);
            };
        }
    }

    private static DefaultUserContext context(String owner, AtomicInteger constructed) {
        assertTrue(Row.LAYOUT.isGenerated());
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new EntityDescriptor(); descriptor.setType("Row"); descriptor.setTargetType(Row.class);
        descriptor.withEntitySupplier(() -> { constructed.incrementAndGet(); var row = new Row(); row.owner = owner; return row; });
        descriptor.addSimpleProperty("id", Long.class); descriptor.addSimpleProperty("version", Long.class);
        descriptor.addSimpleProperty("name", String.class); descriptor.addSimpleProperty("address", String.class);
        descriptor.addSimpleProperty("count", Integer.class); descriptor.addSimpleProperty("active", Boolean.class);
        descriptor.addSimpleProperty("baseUrl", String.class);
        metadata.register(descriptor);
        return new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
    }

    @Test public void typedRoundTripPreservesLoadedNullOmissionAndFalsyValuesWithoutMutation() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        var row = mapper.readValue("{\"id\":1,\"version\":7,\"name\":null,\"count\":0,\"active\":false,\"baseUrl\":\"\",\"_total\":null}", Row.class);
        assertEquals(Long.valueOf(1), row.getId()); assertEquals(Long.valueOf(7), row.getVersion());
        assertTrue(row.isPropertyLoaded("name")); assertNull(row.name);
        assertFalse(row.isPropertyLoaded("address")); assertNull(row.address);
        assertEquals(Integer.valueOf(0), row.count); assertEquals(Boolean.FALSE, row.active); assertEquals("", row.baseUrl);
        assertTrue(row.isPropertyLoaded("base_url")); assertTrue(row.getAdditionalInfo().containsKey("_total"));
        assertNull(row.getDynamicProperty("total")); assertNull(row.getDynamicProperty("missing"));
        assertFalse(row.__internalHasMutationLedger()); assertTrue(row.getUpdatedProperties().isEmpty());
        var json = mapper.writeValueAsString(row); var restored = mapper.readValue(json, Row.class);
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(restored)));
        assertSame(row.__internalLoadState(), restored.__internalLoadState());
        assertFalse(restored.__internalHasMutationLedger()); assertFalse(restored.isPropertyLoaded("address"));
    }

    @Test public void sameShapesShareStateDespiteFieldOrderAliasesAndPrivateValues() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        Row first = mapper.readValue("{\"id\":1,\"baseUrl\":null}", Row.class);
        Row second = mapper.readValue("{\"legacy_url\":\"private\",\"id\":2}", Row.class);
        assertSame(first.__internalLoadState(), second.__internalLoadState()); assertNull(first.baseUrl); assertEquals("private", second.baseUrl);
        Row minimal = mapper.readValue("{\"id\":3}", Row.class);
        assertNotSame(first.__internalLoadState(), minimal.__internalLoadState()); assertFalse(minimal.isPropertyLoaded("base_url"));
        first.__internalHydrate("address", "only first", 3);
        assertFalse(second.isPropertyLoaded("address")); assertFalse(minimal.isPropertyLoaded("address"));
    }

    @Test public void unknownFieldsInternalStateAndDuplicateAliasesRejectBeforeConstruction() throws Exception {
        var count = new AtomicInteger(); var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", count)));
        for (String json : List.of("{\"unknown\":1}", "{\"entityMutationLedger\":{}}", "{\"loadState\":{}}",
                "{\"$status\":\"PERSISTED\"}", "{\"comment\":\"untrusted\"}", "{\"#name\":\"needs definitions\"}",
                "{\"baseUrl\":\"A\",\"legacy_url\":\"B\"}")) {
            assertThrows(JsonMappingException.class, () -> mapper.readValue(json, Row.class));
        }
        assertEquals(0, count.get());
    }

    @Test public void contextReadersUseIndependentSuppliersAndCanFollowTheSingletonModule() throws Exception {
        var first = new ObjectMapper().registerModule(TeaQLModule.INSTANCE).registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        var second = new ObjectMapper().registerModule(new TeaQLModule(context("B", new AtomicInteger())));
        assertEquals("A", first.readValue("{\"id\":1}", Row.class).owner);
        assertEquals("B", second.readValue("{\"id\":1}", Row.class).owner);
        assertThrows(RuntimeException.class, () -> new TeaQLModule((io.teaql.core.UserContext) null));
    }

    @Test public void nonFreshOrWrongTypeSuppliersCannotHideValuesBehindANewProjection() throws Exception {
        var context = context("A", new AtomicInteger());
        var descriptor = context.capability(io.teaql.core.meta.EntityMetaFactory.class).resolveEntityDescriptor("Row");
        descriptor.setEntitySupplier(() -> {
            var row = new Row(); row.__internalSet("name", "preset must not become NotLoaded"); return row;
        });
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context));
        assertThrows(JsonMappingException.class, () -> mapper.readValue("{\"id\":1}", Row.class));
        descriptor.setEntitySupplier(BaseEntity::new);
        var wrongType = new ObjectMapper().registerModule(new TeaQLModule(context));
        assertThrows(JsonMappingException.class, () -> wrongType.readValue("{}", Row.class));
    }

    @Test public void derivedNamespaceCannotChangeNativeAvailabilityOrCreateMutationIntent() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        var row = mapper.readValue("{\"name\":\"fixed\",\"_name\":\"derived\",\"_address\":\"not native\",\"_loadState\":{\"bits\":127}}", Row.class);
        assertEquals("fixed", row.name); assertEquals("derived", row.getDynamicProperty("name"));
        assertFalse(row.isPropertyLoaded("address")); assertNull(row.address);
        assertFalse(row.__internalHasMutationLedger()); assertTrue(row.getUpdatedProperties().isEmpty());
    }

    @Test public void reservedRuntimeKeysCannotMasqueradeAsReadonlyProperties() throws Exception {
        var constructed = new AtomicInteger();
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", constructed)));
        for (String key : List.of("_comment", "_dirty_fields", "_original_values", "_is_new",
                "_is_deleted", "__load_state", "__teaql_runtime_state")) {
            var input = mapper.createObjectNode().put("id", 1).put("name", "legitimate name");
            input.putObject(key).put("payload", "PRIVATE-STATE-CANARY");
            var error = assertThrows(JsonMappingException.class,
                    () -> mapper.treeToValue(input, Row.class));
            assertTrue(error.getMessage().contains("Incoming runtime state is forbidden"));
            assertFalse(error.getMessage().contains("PRIVATE-STATE-CANARY"));
        }
        assertEquals("reject before constructing any model object", 0, constructed.get());
    }

    @Test public void reservedRuntimeKeysRejectTheWholeArrayWithoutConstructingTheInvalidRow() throws Exception {
        var constructed = new AtomicInteger();
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", constructed)));
        for (String key : List.of("_comment", "_dirty_fields", "_original_values", "_is_new",
                "_is_deleted", "__load_state", "__teaql_runtime_state")) {
            var input = mapper.createObjectNode().put("id", 1);
            input.putObject(key).put("payload", "PRIVATE-STATE-CANARY");
            var batch = mapper.createArrayNode();
            batch.addObject().put("id", 2).put("name", "valid first row");
            batch.add(input);
            var error = assertThrows(JsonMappingException.class,
                    () -> mapper.readerForListOf(Row.class).readValue(batch));
            assertTrue(error.getMessage().contains("Incoming runtime state is forbidden"));
            assertFalse(error.getMessage().contains("PRIVATE-STATE-CANARY"));
        }
        assertEquals("only each valid first row may reach the supplier", 7, constructed.get());
    }

    @Test public void unregisteredEntityCannotFallBackToBeanMutation() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        var error = assertThrows(JsonMappingException.class,
                () -> mapper.readValue("{\"comment\":\"untrusted\"}", UnregisteredRow.class));
        assertTrue(error.getMessage().contains("not installed in this context"));
    }
}
