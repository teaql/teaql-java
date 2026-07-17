package io.teaql.core.checker;

import org.junit.Test;
import static org.junit.Assert.*;

public class ObjectLocationTest {

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
    }
}
