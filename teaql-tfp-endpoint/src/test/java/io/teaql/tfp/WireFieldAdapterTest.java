package io.teaql.tfp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.Test;
import io.teaql.core.checker.CheckResult;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.checker.ObjectLocation;

public class WireFieldAdapterTest {
    private final ObjectMapper json = new ObjectMapper();
    private final WireEntityMetadata metadata = new WireEntityMetadata("School", Map.of(
            "user_url", "userUrl", "school_type", "schoolType"),
            Map.of("legacyUrl", "user_url"));

    @Test
    public void matchesTypescriptFixtureAndRetainsSubmittedPath() throws Exception {
        ObjectNode input = (ObjectNode) json.readTree(
                "{\"legacyUrl\":\"https://teaql.io\",\"schoolType\":1001}");
        NormalizedWireObject normalized = WireFieldAdapter.normalize(input, metadata);
        assertEquals("https://teaql.io", normalized.values().path("user_url").asText());
        assertEquals(1001, normalized.values().path("school_type").asInt());
        assertEquals("/legacyUrl", normalized.sourceInstancePaths().get("user_url"));
        assertEquals("/schoolType", normalized.sourceInstancePaths().get("school_type"));
        assertEquals("userUrl", metadata.wireField("user_url"));
        CheckResult violation = CheckResult.required(ObjectLocation.hashRoot("user_url"));
        WireFieldAdapter.retainSubmittedPaths(java.util.List.of(violation), normalized);
        assertEquals("/legacyUrl", violation.getSourceInstancePath());
    }

    @Test
    public void rejectsUnknownAndAliasCollisionBeforeSave() throws Exception {
        TfpEndpointException unknown = assertThrows(TfpEndpointException.class,
                () -> WireFieldAdapter.normalize((ObjectNode) json.readTree("{\"unknown\":1}"), metadata));
        assertEquals("WIRE_UNKNOWN_FIELD", unknown.getCode());
        TfpEndpointException collision = assertThrows(TfpEndpointException.class,
                () -> WireFieldAdapter.normalize((ObjectNode) json.readTree(
                        "{\"userUrl\":\"a\",\"legacyUrl\":\"a\"}"), metadata));
        assertEquals("WIRE_FIELD_COLLISION", collision.getCode());
    }

    @Test
    public void serializesCanonicalLocationAndSelectedWirePath() throws Exception {
        CheckResult violation = CheckResult.required(ObjectLocation.hashRoot("school_type"));
        violation.setRootType("School");
        violation.setSourceInstancePath("/legacySchoolType");

        ObjectNode wire = (ObjectNode) json.valueToTree(
                violation.toWire(JsonFieldNamingProfile.CAMEL_CASE));
        assertEquals("required", wire.path("ruleId").asText());
        assertEquals("School", wire.path("entityType").asText());
        assertEquals("property", wire.path("location").path(0).path("kind").asText());
        assertEquals("school_type", wire.path("location").path(0).path("name").asText());
        assertEquals("/schoolType", wire.path("instancePath").asText());
        assertEquals("/legacySchoolType", wire.path("sourceInstancePath").asText());
    }

    @Test
    public void parentAliasNeverTruncatesDescendantProvenance() {
        NormalizedWireObject normalized = new NormalizedWireObject(json.createObjectNode(),
                Map.of("order_item_list", "/legacy~1items", "product_name", "/legacy~0name"));
        ObjectLocation location = ObjectLocation.hashRoot("order_item_list").element(2).member("product_name");
        CheckResult nested = CheckResult.required(location);
        nested.setSourceInstancePath("/legacy~1items/2/legacy~0name");
        CheckResult absent = CheckResult.required(location);
        CheckResult indexed = CheckResult.required(ObjectLocation.hashRoot("product_name").element(2));
        CheckResult direct = CheckResult.required(ObjectLocation.hashRoot("order_item_list"));
        WireFieldAdapter.retainSubmittedPaths(java.util.List.of(nested, absent, indexed, direct), normalized);
        assertEquals("/legacy~1items/2/legacy~0name", nested.getSourceInstancePath());
        assertEquals("/orderItemList/2/productName", nested.toWire(JsonFieldNamingProfile.CAMEL_CASE).instancePath());
        assertEquals(null, absent.getSourceInstancePath());
        assertEquals(null, indexed.getSourceInstancePath());
        assertEquals("/legacy~1items", direct.getSourceInstancePath());
    }
}
