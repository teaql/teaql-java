package io.teaql.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import org.junit.Test;

public class BaseEntitySerializationTest {
    static final class NamedRelationsEntity extends BaseEntity {
        static final io.teaql.core.FieldLayout LAYOUT = io.teaql.core.FieldLayout.installGenerated(
                io.teaql.core.FieldLayout.generated(NamedRelationsEntity.class, "named-relations-v1",
                        java.util.Map.of("id", 0, "version", 1),
                        java.util.Map.of("id", java.util.List.of("id", "id"),
                                "version", java.util.List.of("version", "version")),
                        java.util.Set.of("childList", "child")));
        io.teaql.core.SmartList<BaseEntity> childList;
        BaseEntity child;
        @Override public void __internalSet(String field, Object value) {
            switch (field) {
                case "childList" -> { markPropertyLoaded(field); childList = (io.teaql.core.SmartList<BaseEntity>) value; }
                case "child" -> { markPropertyLoaded(field); child = (BaseEntity) value; }
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) {
                case "childList" -> childList;
                case "child" -> child;
                default -> super.__internalGet(field);
            };
        }
    }

    @Test public void loadedNamedRelationsSerializeWithoutFixedIndexesOrBeanReflection() throws Exception {
        var parent = new NamedRelationsEntity(); parent.__internalSet("id", 1L); parent.__internalSet("version", 1L);
        var child = new JsonGraphEntity(); child.__internalSet("id", 1L); child.__internalSet("version", 1L); child.__internalSet("name", "same ID different type");
        var list = new io.teaql.core.SmartList<BaseEntity>(); list.add(child);
        parent.__internalSet("childList", list);
        parent.__internalSet("child", null);
        parent.putAdditional("childList", "must not replace the declared relation");
        parent.addDynamicProperty("childList", "readonly summary");
        var state = parent.__internalLoadState();
        boolean hadLedger = parent.__internalHasMutationLedger();
        var mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        var json = mapper.readTree(mapper.writeValueAsString(parent));
        assertEquals("same ID different type", json.get("childList").get(0).get("name").asText());
        assertEquals("readonly summary", json.get("_childList").asText());
        assertTrue(json.get("child").isNull());
        assertTrue(state == parent.__internalLoadState());
        assertEquals(hadLedger, parent.__internalHasMutationLedger());
        assertNull(NamedRelationsEntity.LAYOUT.findIndex("childList"));
        var empty = new NamedRelationsEntity(); empty.__internalSet("childList", new io.teaql.core.SmartList<BaseEntity>());
        json = mapper.readTree(mapper.writeValueAsString(empty));
        assertTrue(json.get("childList").isArray()); assertEquals(0, json.get("childList").size());
        assertFalse(json.has("child"));
        json = mapper.readTree(mapper.writeValueAsString(new NamedRelationsEntity()));
        assertFalse(json.has("childList")); assertFalse(json.has("child"));
    }

    static final class JsonGraphEntity extends BaseEntity {
        static final io.teaql.core.FieldLayout LAYOUT = io.teaql.core.FieldLayout.installGenerated(
                io.teaql.core.FieldLayout.generated(JsonGraphEntity.class, "nested-json-v1",
                        java.util.Map.of("id",0,"version",1,"name",2,"child",3),
                        java.util.Map.of("id",java.util.List.of("id","id"),"version",java.util.List.of("version","version"),
                                "name",java.util.List.of("name","name"),"child",java.util.List.of("child","child_id")),java.util.Set.of()));
        String name;
        BaseEntity child;
        @Override public void __internalSet(String field,Object value) {
            markPropertyLoaded(field);
            switch (field) { case "name" -> name=(String)value; case "child" -> child=(BaseEntity)value; default -> super.__internalSet(field,value); }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) { case "name" -> name; case "child" -> child; default -> super.__internalGet(field); };
        }
    }

    @Test public void nestedGraphsKeepThreeNamespacesAndLiteralPayloadWithoutInternalMetadata() throws Exception {
        var parent = new JsonGraphEntity(); parent.__internalSet("id",1L); parent.__internalSet("version",1L); parent.__internalSet("name","parent");
        var child = new JsonGraphEntity(); child.__internalSet("id",2L); child.__internalSet("version",1L); child.__internalSet("name","native");
        child.addDynamicProperty("name","derived"); child.setComment("private child reason");
        child.setDynamicFieldValues(io.teaql.data.dynamic.DynamicFieldValues.of(java.util.List.of(
                io.teaql.data.dynamic.DynamicFieldValue.ofString("name","persistent"),
                io.teaql.data.dynamic.DynamicFieldValue.ofNull("nil",io.teaql.data.dynamic.DynamicDataType.STRING),
                io.teaql.data.dynamic.DynamicFieldValue.notLoaded("missing",io.teaql.data.dynamic.DynamicDataType.STRING))));
        parent.__internalSet("child",child); parent.addDynamicProperty("children",java.util.List.of(child));
        parent.addDynamicProperty("literal",java.util.Map.of("_comment","literal business data"));
        var mapper=new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        var json=mapper.readTree(mapper.writeValueAsString(parent));
        for (var node : java.util.List.of(json.get("child"),json.get("_children").get(0))) {
            assertEquals("native",node.get("name").asText()); assertEquals("derived",node.get("_name").asText());
            assertEquals("persistent",node.get("#name").asText()); assertTrue(node.get("#nil").isNull());
            assertFalse(node.has("#missing")); assertFalse(node.has("comment")); assertFalse(node.has("entityMutationLedger"));
            assertFalse(node.has("child"));
        }
        assertEquals("literal business data",json.get("_literal").get("_comment").asText());
        child.__internalSet("child",null);
        json=mapper.readTree(mapper.writeValueAsString(parent)); assertTrue(json.get("child").get("child").isNull());
    }
    public static class CyclicEntity extends BaseEntity {
        static final io.teaql.core.FieldLayout LAYOUT = io.teaql.core.FieldLayout.installGenerated(
                io.teaql.core.FieldLayout.generated(CyclicEntity.class, "cycle-v1",
                        java.util.Map.of("id", 0, "version", 1, "peer", 2),
                        java.util.Map.of("id", java.util.List.of("id", "id"), "version", java.util.List.of("version", "version"),
                                "peer", java.util.List.of("peer", "peer")), java.util.Set.of()));
        CyclicEntity peer;
        @Override public Object __internalGet(String field) { return "peer".equals(field) ? peer : super.__internalGet(field); }
        @Override public void __internalSet(String field, Object value) {
            if ("peer".equals(field)) { markPropertyLoaded(field); peer = (CyclicEntity) value; }
            else super.__internalSet(field, value);
        }
    }

    @Test
    public void cyclicIndexedGraphsUseIdentityReferencesAndDoNotRetainAPathAcrossWrites() throws Exception {
        var first = new CyclicEntity(); first.updateId(1L); first.updateVersion(1L);
        var second = new CyclicEntity(); second.updateId(2L); second.updateVersion(1L);
        first.__internalSet("peer", second); second.__internalSet("peer", first);
        var mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        var json = mapper.readTree(mapper.writeValueAsString(first));
        assertEquals(2L, json.get("peer").get("id").asLong());
        assertEquals(1L, json.get("peer").get("peer").get("id").asLong());
        assertFalse(json.get("peer").get("peer").has("peer"));
        assertEquals(json, mapper.readTree(mapper.writeValueAsString(first)));
    }

    @Test
    public void sharedReferencesOutsideTheActivePathAreNotReducedToIdentityOnly() throws Exception {
        var first = new CyclicEntity(); first.updateId(1L); first.updateVersion(1L);
        var second = new CyclicEntity(); second.updateId(2L); second.updateVersion(1L);
        first.__internalSet("peer", second);
        second.putAdditional("_label", "shared child");
        first.putAdditional("_again", second);
        var mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        var json = mapper.readTree(mapper.writeValueAsString(java.util.List.of(first, first)));
        for (var row : json) {
            assertEquals("shared child", row.get("peer").get("_label").asText());
            assertEquals(row.get("peer"), row.get("_again"));
        }
        assertEquals(json.get(0), json.get(1));
    }

    public static class IndexedEntity extends BaseEntity {
        static final io.teaql.core.FieldLayout LAYOUT = io.teaql.core.FieldLayout.installGenerated(
                io.teaql.core.FieldLayout.generated(IndexedEntity.class, "serialization-v1",
                        java.util.Map.of("id", 0, "version", 1, "name", 2, "base_url", 3),
                        java.util.Map.of("id", java.util.List.of("id", "id"), "version", java.util.List.of("version", "version"),
                                "name", java.util.List.of("name", "name"), "base_url", java.util.List.of("baseUrl", "legacy_url")), java.util.Set.of()));
        private String name;
        private String baseUrl;
        @Override public void __internalSet(String field, Object value) {
            markPropertyLoaded(field);
            switch (field) {
                case "name" -> name = (String) value;
                case "baseUrl" -> baseUrl = (String) value;
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) { case "name" -> name; case "baseUrl" -> baseUrl; default -> super.__internalGet(field); };
        }
    }

    @Test
    public void indexedFixedDerivedAndPersistentNamespacesSurviveWithoutBeanReflection() throws Exception {
        var entity = new IndexedEntity();
        entity.__internalSet("id", 1L); entity.__internalSet("version", 1L);
        entity.__internalSet("name", "native");
        entity.addDynamicProperty("name", "derived");
        entity.addDynamicProperty("nil", null);
        entity.setDynamicFieldValues(io.teaql.data.dynamic.DynamicFieldValues.of(java.util.List.of(
                io.teaql.data.dynamic.DynamicFieldValue.ofString("name", "persistent"),
                io.teaql.data.dynamic.DynamicFieldValue.ofNull("nil", io.teaql.data.dynamic.DynamicDataType.STRING),
                io.teaql.data.dynamic.DynamicFieldValue.notLoaded("missing", io.teaql.data.dynamic.DynamicDataType.STRING))));
        entity.putAdditional("name", "must-not-shadow-native");
        entity.putAdditional("id", 999L);
        var mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        var json = mapper.readTree(mapper.writeValueAsString(entity));
        assertEquals("native", json.get("name").asText());
        assertEquals("derived", json.get("_name").asText());
        assertEquals("persistent", json.get("#name").asText());
        assertEquals(1L, json.get("id").asLong());
        assertTrue(json.get("_nil").isNull()); assertTrue(json.get("#nil").isNull());
        assertFalse(json.has("#missing")); assertFalse(json.has("baseUrl"));
        assertFalse(json.has("loadState")); assertFalse(json.has("entityMutationLedger"));
        entity.__internalSet("baseUrl", null);
        json = mapper.readTree(mapper.writeValueAsString(entity));
        assertTrue(json.has("baseUrl")); assertTrue(json.get("baseUrl").isNull());
        assertFalse(json.has("base_url")); assertFalse(json.has("legacy_url"));
    }


    public static class NamedEntity extends BaseEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void serializesDynamicPropertiesWithTeaQLModule() throws Exception {
        BaseEntity entity = new BaseEntity();
        entity.updateId(1001L);
        entity.updateVersion(7L);
        entity.setComment("internal comment");
        entity.setTraceChain(java.util.List.of(new io.teaql.core.TraceNode(
                io.teaql.core.TraceKind.AUDIT_REASON, entity.typeName(), 1001L, "internal trace")));
        entity.putAdditional("#customer_asset_no", "A-10086");

        ObjectMapper mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(entity));

        assertEquals(1001L, json.get("id").asLong());
        assertEquals(7L, json.get("version").asLong());
        assertEquals("A-10086", json.get("#customer_asset_no").asText());
        assertFalse(json.has("$status"));
        assertFalse(json.has("comment"));
        assertFalse(json.has("traceChain"));
        assertFalse(json.toString().contains("internal trace"));
        assertFalse(json.has("additionalInfo"));
    }

    @Test
    public void deserializesBaseEntityWithoutBeanMutation() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);

        BaseEntity entity =
                mapper.readValue(
                        "{\"id\":1001,\"version\":7,\"#customer_asset_no\":\"A-10086\",\"enabled\":true}",
                        BaseEntity.class);

        assertEquals(Long.valueOf(1001L), entity.getId());
        assertEquals(Long.valueOf(7L), entity.getVersion());
        assertEquals("A-10086", entity.getAdditionalInfo().get("#customer_asset_no"));
        assertEquals(Boolean.TRUE, entity.getAdditionalInfo().get("enabled"));
        assertNull(entity.getComment());
    }

    @Test
    public void serializesBaseEntitySubclassesWithTeaQLSerializer() throws Exception {
        NamedEntity entity = new NamedEntity();
        entity.updateId(1001L);
        entity.setName("should-not-use-bean-getter");
        entity.putAdditional("#customer_asset_no", "A-10086");

        ObjectMapper mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(entity));

        assertEquals(1001L, json.get("id").asLong());
        assertEquals("A-10086", json.get("#customer_asset_no").asText());
        assertFalse(json.has("name"));
    }
}
