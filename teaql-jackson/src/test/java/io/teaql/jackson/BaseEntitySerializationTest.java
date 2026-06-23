package io.teaql.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import org.junit.Test;

public class BaseEntitySerializationTest {

    @Test
    public void serializesDynamicPropertiesWithTeaQLModule() throws Exception {
        BaseEntity entity = new BaseEntity();
        entity.updateId(1001L);
        entity.setComment("internal comment");
        entity.setTraceChain("internal trace");
        entity.putAdditional("#customer_asset_no", "A-10086");

        ObjectMapper mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(entity));

        assertEquals(1001L, json.get("id").asLong());
        assertEquals("A-10086", json.get("#customer_asset_no").asText());
        assertFalse(json.has("$status"));
        assertFalse(json.has("comment"));
        assertFalse(json.has("traceChain"));
        assertFalse(json.has("additionalInfo"));
    }
}
