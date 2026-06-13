package io.teaql.core.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.web.WebResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;

class WebResponseDataSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(TeaQLModule.INSTANCE);

    @Test
    void webResponseDataRoundTripsWithEntityFields() throws Exception {
        Product product = new Product();
        product.setId(7L);
        product.setVersion(3L);
        product.setName("USB-C Cable");

        String json = mapper.writeValueAsString(WebResponse.of(product));

        assertTrue(json.contains("\"data\""));
        assertTrue(json.contains("\"name\":\"USB-C Cable\""));

        WebResponse response = mapper.readValue(json, WebResponse.class);

        assertEquals(0, response.getResultCode());
        assertEquals("YES", response.getStatus());
        assertEquals(1, response.getRecordCount());
        assertEquals(1, response.getData().size());

        BaseEntity entity = response.getData().get(0);
        assertNotNull(entity);
        assertEquals(7L, entity.getId());
        assertEquals(3L, entity.getVersion());
        assertEquals("USB-C Cable", entity.getAdditionalInfo().get("name"));
    }

    @Test
    void webResponseDataRoundTripsWithFacets() throws Exception {
        Product product = new Product();
        product.setId(7L);
        product.setVersion(3L);
        product.setName("USB-C Cable");

        Product facetProduct = new Product();
        facetProduct.setId(9L);
        facetProduct.setVersion(1L);
        facetProduct.setName("Facet Item");

        io.teaql.core.SmartList<Product> facetList = new io.teaql.core.SmartList<>();
        facetList.add(facetProduct);

        io.teaql.core.SmartList<Product> parentList = new io.teaql.core.SmartList<>();
        parentList.add(product);
        parentList.addFacet("status", facetList);

        // Verify getter/mutator APIs on SmartList
        assertNotNull(parentList.getFacets());
        assertTrue(parentList.getFacets().containsKey("status"));
        assertEquals(1, parentList.getFacet("status").size());

        String json = mapper.writeValueAsString(WebResponse.of(parentList));

        assertTrue(json.contains("\"facets\""));
        assertTrue(json.contains("\"status\""));
        assertTrue(json.contains("\"name\":\"Facet Item\""));

        WebResponse response = mapper.readValue(json, WebResponse.class);
        assertNotNull(response.getFacets());
        assertTrue(response.getFacets().containsKey("status"));
        
        io.teaql.core.SmartList deserializedFacet = response.getFacets().get("status");
        assertNotNull(deserializedFacet);
        assertEquals(1, deserializedFacet.size());

        // Test removing and clearing facets
        io.teaql.core.SmartList removed = parentList.removeFacet("status");
        assertNotNull(removed);
        assertTrue(parentList.getFacets().isEmpty());

        parentList.addFacet("status", facetList);
        parentList.clearFacets();
        assertTrue(parentList.getFacets().isEmpty());
    }

    @Test
    void testRemoteInputChecking() throws Exception {
        String json = "{\"id\":1,\"name\":\"USB-C Cable\"}";

        // Without active request, it should deserialize fine
        Product product = mapper.readValue(json, Product.class);
        assertNotNull(product);
        assertEquals("USB-C Cable", product.getName());

        // With active request, since Product does not implement RemoteInput, it should fail
        try {
            System.setProperty("io.teaql.core.jackson.testing.forceRemoteInputCheck", "true");

            Exception exception = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                mapper.readValue(json, Product.class);
            });
            assertTrue(exception.getMessage().contains("is rejected because it does not implement"));

            // RemoteProduct implements RemoteInput, so it should deserialize fine even with active request
            RemoteProduct remoteProduct = mapper.readValue(json, RemoteProduct.class);
            assertNotNull(remoteProduct);
            assertEquals("USB-C Cable", remoteProduct.getName());
        } finally {
            System.clearProperty("io.teaql.core.jackson.testing.forceRemoteInputCheck");
        }
    }

    @Test
    void testDictionaryBasedTranslation() throws Exception {
        // 1. Without system property, instantiating BaseLanguageTranslator("zh_CN") should throw IllegalStateException
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            new io.teaql.core.language.BaseLanguageTranslator("zh_CN");
        });

        // 2. With valid system property, instantiating should succeed and translate correctly
        try {
            java.io.File file = new java.io.File("src/test/resources/teaql-i18n.json");
            if (!file.exists()) {
                file = new java.io.File("teaql-autoconfigure/src/test/resources/teaql-i18n.json");
            }
            System.setProperty("teaql.i18n.path", file.getAbsolutePath());
            
            // Force reload dictionary using reflection
            java.lang.reflect.Field loadedField = io.teaql.core.language.BaseLanguageTranslator.class.getDeclaredField("loaded");
            loadedField.setAccessible(true);
            loadedField.set(null, false);

            io.teaql.core.language.BaseLanguageTranslator zhTranslator = new io.teaql.core.language.BaseLanguageTranslator("zh_CN");
            io.teaql.core.checker.CheckResult errorZh = io.teaql.core.checker.CheckResult.required(
                new io.teaql.core.checker.HashLocation(null, "workedHour")
            );
            
            zhTranslator.translateError(null, java.util.Collections.singletonList(errorZh));
            assertEquals("工作时间 是必填项", errorZh.getNaturalLanguageStatement());

            io.teaql.core.language.BaseLanguageTranslator esTranslator = new io.teaql.core.language.BaseLanguageTranslator("es");
            io.teaql.core.checker.CheckResult errorEs = io.teaql.core.checker.CheckResult.required(
                new io.teaql.core.checker.HashLocation(null, "workedHour")
            );
            
            esTranslator.translateError(null, java.util.Collections.singletonList(errorEs));
            assertEquals("Hora trabajada es requerido/a", errorEs.getNaturalLanguageStatement());
        } finally {
            System.clearProperty("teaql.i18n.path");
            // Force reset back to default using reflection
            java.lang.reflect.Field loadedField = io.teaql.core.language.BaseLanguageTranslator.class.getDeclaredField("loaded");
            loadedField.setAccessible(true);
            loadedField.set(null, false);
        }
    }

    static class RemoteProduct extends BaseEntity implements io.teaql.core.RemoteInput {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class Product extends BaseEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
