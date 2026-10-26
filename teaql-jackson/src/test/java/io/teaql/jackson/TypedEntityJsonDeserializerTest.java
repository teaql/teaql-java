package io.teaql.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.EntityKey;
import io.teaql.core.EntityStatus;
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
                        "base_url", List.of("baseUrl", "legacy_url")), Set.of("child", "child_list"),
                io.teaql.core.checker.JsonFieldNamingProfile.CAMEL_CASE, Map.of("legacy_url", "base_url")));
        String name, address, baseUrl, owner;
        Integer count;
        Boolean active;
        Row child;
        SmartList<Row> childList;
        void updateName(String value) {
            handleUpdate("name", name, value);
            name = value;
        }
        @Override public void __internalSet(String field, Object value) {
            markPropertyLoaded(field);
            switch (field) {
                case "name" -> name = (String) value;
                case "address" -> address = (String) value;
                case "base_url" -> baseUrl = (String) value;
                case "count" -> count = (Integer) value;
                case "active" -> active = (Boolean) value;
                case "child" -> child = (Row) value;
                case "child_list" -> childList = (SmartList<Row>) value;
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) {
                case "name" -> name; case "address" -> address; case "base_url" -> baseUrl;
                case "count" -> count; case "active" -> active; case "child" -> child; case "child_list" -> childList;
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
        descriptor.addSimpleProperty("base_url", String.class);
        metadata.register(descriptor);
        descriptor.addObjectProperty(metadata, "child", "Row", "child_list", Row.class);
        return new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
    }

    @Test public void canonicalNamedRelationsRoundTripThroughNativeJsonWithoutChangingAvailability() throws Exception {
        var constructed = new AtomicInteger();
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", constructed)));
        var row = mapper.readValue("{\"childList\":[{\"baseUrl\":\"\"}],\"_childList\":\"derived\"}", Row.class);
        assertTrue(row.isPropertyLoaded("child_list"));
        assertTrue(row.isPropertyLoaded("childList"));
        assertEquals("", row.childList.first().baseUrl);
        var state = row.__internalLoadState();
        var json = mapper.readTree(mapper.writeValueAsString(row));
        assertEquals("", json.get("childList").get(0).get("baseUrl").asText());
        assertEquals("derived", json.get("_childList").asText());
        assertFalse(json.has("child_list"));
        assertSame(state, row.__internalLoadState());
        assertFalse(row.__internalHasMutationLedger());
        assertFalse(row.childList.first().__internalHasMutationLedger());
        assertEquals(json, mapper.readTree(mapper.writeValueAsString(mapper.treeToValue(json, Row.class))));
        for (String value : List.of("[]", "null")) {
            var selected = mapper.readValue("{\"childList\":" + value + "}", Row.class);
            var selectedJson = mapper.readTree(mapper.writeValueAsString(selected));
            assertTrue(selected.isPropertyLoaded("child_list"));
            assertEquals(mapper.readTree(value), selectedJson.get("childList"));
        }
        var omitted = mapper.readValue("{}", Row.class);
        assertFalse(omitted.isPropertyLoaded("child_list"));
        assertFalse(mapper.readTree(mapper.writeValueAsString(omitted)).has("childList"));
        int before = constructed.get();
        assertThrows(JsonMappingException.class,
                () -> mapper.readValue("{\"childList\":[],\"child_list\":[]}", Row.class));
        assertEquals(before, constructed.get());
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

    @Test public void persistedIdentityIsCleanRegardlessOfJsonMemberOrder() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        for (String json : List.of("{\"id\":41,\"version\":7,\"name\":\"before\"}",
                "{\"version\":7,\"name\":\"before\",\"id\":41}")) {
            Row row = mapper.readValue(json, Row.class);
            assertEquals(EntityStatus.PERSISTED, row.get$status());
            assertFalse(row.newItem()); assertFalse(row.needPersist());
            assertFalse(row.__internalHasMutationLedger()); assertTrue(row.getUpdatedProperties().isEmpty());
            assertEquals(Long.valueOf(7), row.getOriginalVersion());
            row.updateName("after");
            assertEquals(EntityStatus.UPDATED, row.get$status()); assertTrue(row.needPersist());
            assertEquals(Set.of("name"), Set.copyOf(row.getUpdatedProperties()));
            assertEquals(Long.valueOf(7), row.getEntityMutationLedger().getOriginalVersion(new EntityKey(row.typeName(), 41L)));
            assertFalse(row.isNew());
        }
    }

    @Test public void deletedIdentityRequiresExplicitRecoveryRatherThanFreshInsertion() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        Row row = mapper.readValue("{\"id\":41,\"version\":-7,\"name\":\"before\"}", Row.class);
        assertEquals(EntityStatus.PERSISTED_DELETED, row.get$status());
        assertFalse(row.needPersist()); assertFalse(row.__internalHasMutationLedger());
        assertEquals(Long.valueOf(-7), row.getOriginalVersion());
        assertThrows(io.teaql.core.TeaQLRuntimeException.class, () -> row.updateName("invalid"));
        assertEquals("before", row.name); assertFalse(row.__internalHasMutationLedger());
        row.markAsRecover();
        assertEquals(EntityStatus.UPDATED_RECOVER, row.get$status()); assertTrue(row.needPersist());
        assertEquals(Long.valueOf(-7), row.getOriginalVersion()); assertFalse(row.isNew());
    }

    @Test public void independentDecodesShareAvailabilityButNeverMutationOwnership() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        String json = "{\"id\":41,\"version\":7,\"name\":\"before\"}";
        Row first = mapper.readValue(json, Row.class), second = mapper.readValue(json, Row.class);
        assertSame(first.__internalLoadState(), second.__internalLoadState());
        assertFalse(first.needPersist()); assertFalse(second.needPersist());
        first.updateName("first");
        assertFalse(second.__internalHasMutationLedger()); assertEquals("before", second.name);
        second.updateName("second");
        assertNotSame(first.getEntityMutationLedger(), second.getEntityMutationLedger());
        assertEquals(Long.valueOf(7), first.getOriginalVersion());
        assertEquals(Long.valueOf(7), second.getOriginalVersion());
        assertEquals("first", first.__internalGet("name")); assertEquals("second", second.__internalGet("name"));
    }

    @Test public void nestedExistingIdentitiesStayCleanWithoutWideningPartialProjections() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        Row row = mapper.readValue("{\"id\":41,\"version\":7,\"name\":\"parent\","
                + "\"child\":{\"id\":42,\"version\":3},\"childList\":[{\"id\":43,\"version\":4}]}", Row.class);
        for (Row value : List.of(row, row.child, row.childList.first())) {
            assertEquals(EntityStatus.PERSISTED, value.get$status());
            assertFalse(value.needPersist()); assertFalse(value.__internalHasMutationLedger());
        }
        assertFalse(row.child.isPropertyLoaded("name"));
        row.updateName("updated parent");
        assertTrue(row.needPersist()); assertFalse(row.child.needPersist()); assertFalse(row.childList.first().needPersist());
    }

    @Test public void jsonWithoutPersistedVersionKeepsTheExistingNewObjectContract() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        for (String json : List.of("{}", "{\"name\":\"new\"}", "{\"id\":41,\"name\":\"new with supplied id\"}")) {
            Row row = mapper.readValue(json, Row.class);
            assertEquals(EntityStatus.NEW, row.get$status()); assertTrue(row.newItem());
            assertTrue(row.needPersist()); assertFalse(row.__internalHasMutationLedger());
            assertNull(row.getOriginalVersion());
        }
    }

    @Test public void identityOnlyJsonRestoresAReadOnlyReferenceWithoutInventingANewEntity() throws Exception {
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context("A", new AtomicInteger())));
        for (String json : List.of("{\"id\":41}", "{\"id\":41,\"version\":null}", "{\"id\":41,\"_total\":3}")) {
            Row row = mapper.readValue(json, Row.class);
            assertEquals(EntityStatus.REFER, row.get$status()); assertFalse(row.needPersist());
            assertFalse(row.__internalHasMutationLedger()); assertNull(row.getOriginalVersion());
            assertFalse(row.isPropertyLoaded("name"));
            assertThrows(io.teaql.core.TeaQLRuntimeException.class, () -> row.updateName("must load a version first"));
            assertFalse(row.__internalHasMutationLedger()); assertNull(row.name);
        }
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
