package io.teaql.core.checker;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObjectLocationTest {

    @Test
    public void rendersCanonicalNativeAndExternalPaths() {
        ObjectLocation location = ObjectLocation.hashRoot("order_items")
                .element(2)
                .member("user_url");

        assertEquals("order_items[2].user_url", location.modelPath());
        assertEquals("orderItems[2].userUrl", location.nativePath());
        assertEquals("/orderItems/2/userUrl", location.instancePath());
        assertEquals("/order_items/2/user_url", location.instancePath(JsonFieldNamingProfile.SNAKE_CASE));
        assertEquals("/OrderItems/2/UserUrl", location.instancePath(JsonFieldNamingProfile.PASCAL_CASE));
        assertEquals("order_items[2].user_url", location.toString());
    }

    @Test
    public void projectsCheckerResultWithProfileAndSubmittedAlias() {
        CheckResult result = CheckResult.required(ObjectLocation.hashRoot("user_url"));
        result.setRootType("customer_account");
        result.setSourceInstancePath("/user_url");

        WireCheckResult wire = result.toWire(JsonFieldNamingProfile.CAMEL_CASE);
        assertEquals("required", wire.ruleId());
        assertEquals("customer_account", wire.entityType());
        assertEquals(List.of(WireLocationSegment.property("user_url")), wire.location());
        assertEquals("/userUrl", wire.instancePath());
        assertEquals("/user_url", wire.sourceInstancePath());
    }

    @Test
    public void escapesJsonPointerMembers() {
        assertEquals("/a~0~1b", ObjectLocation.hashRoot("a~/b").instancePath());
    }

    @Test
    public void testObjectLocationFormattingAndNestingLevels() {
        // Test hashRoot
        ObjectLocation hash = ObjectLocation.hashRoot("user");
        assertEquals(1, hash.getLevel());
        assertTrue(hash.isFirstLevel());
        assertFalse(hash.isSecondLevel());
        assertEquals("user", hash.toString());

        // Test arrayRoot
        ObjectLocation arr = ObjectLocation.arrayRoot(5);
        assertEquals(1, arr.getLevel());
        assertEquals("[5]", arr.toString());

        // Test nesting
        ObjectLocation nested = ObjectLocation.hashRoot("users")
                .element(2)
                .member("address")
                .member("city");

        assertEquals(4, nested.getLevel());
        assertFalse(nested.isFirstLevel());
        assertFalse(nested.isSecondLevel());
        assertFalse(nested.isThirdLevel());
        assertEquals("users[2].address.city", nested.toString());

        // Test level 2
        ObjectLocation level2 = ObjectLocation.hashRoot("users").element(2);
        assertTrue(level2.isSecondLevel());

        // Test level 3
        ObjectLocation level3 = ObjectLocation.hashRoot("users").element(2).member("address");
        assertTrue(level3.isThirdLevel());
    }
}
