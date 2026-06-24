package io.teaql.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import org.junit.Test;

public class BaseEntitySerializationTest {

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
        entity.setTraceChain("internal trace");
        entity.putAdditional("#customer_asset_no", "A-10086");

        ObjectMapper mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(entity));

        assertEquals(1001L, json.get("id").asLong());
        assertEquals(7L, json.get("version").asLong());
        assertEquals("A-10086", json.get("#customer_asset_no").asText());
        assertFalse(json.has("$status"));
        assertFalse(json.has("comment"));
        assertFalse(json.has("traceChain"));
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
