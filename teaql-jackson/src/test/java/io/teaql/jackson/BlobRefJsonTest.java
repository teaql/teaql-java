package io.teaql.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.blob.BlobRef;
import org.junit.Assert;
import org.junit.Test;

public class BlobRefJsonTest {
    @Test public void stableKeyRoundTripsAndInputRejectsUrlOrObject() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new TeaQLModule());
        try (var input = getClass().getResourceAsStream("/blob-value-cases.json")) {
            var cases = mapper.readTree(input);
            for (var value : cases.get("valid")) {
                BlobRef ref = mapper.readValue(value.toString(), BlobRef.class);
                Assert.assertEquals(value.textValue(), ref.key());
                Assert.assertEquals(value.toString(), mapper.writeValueAsString(ref));
            }
            for (String group : new String[]{"invalid", "invalidJson"}) {
                for (var value : cases.get(group)) {
                    Assert.assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                            () -> mapper.readValue(value.toString(), BlobRef.class));
                }
            }
            Assert.assertNull(mapper.readValue("null", BlobRef.class));
        }
    }
}
