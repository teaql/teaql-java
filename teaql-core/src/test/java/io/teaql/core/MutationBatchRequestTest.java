package io.teaql.core;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MutationBatchRequestTest {
    private static PersistenceMutation item(String comment) {
        MutationIntent intent = MutationIntent.of(comment);
        return () -> intent;
    }

    @Test public void annotatedChildrenCannotFillMissingRootIntent() {
        for (String blank : new String[]{null, "", " ", "\t\n", "\u2003\u00a0"}) {
            var error = assertThrows(RequestIntentException.class,
                    () -> new MutationBatchRequest(blank, List.of(item("annotated child"))));
            assertEquals("REQUEST_COMMENT_REQUIRED", error.getCode());
            assertEquals("comment", error.getField());
        }
    }

    @Test public void rootValidationPrecedesEvenMalformedMemberInput() {
        assertThrows(RequestIntentException.class, () -> new MutationBatchRequest("\u2003", null));
    }

    @Test public void ownsRootIntentAndImmutableOrderedMemberSnapshot() {
        var first = item("root reason");
        var second = item("root reason");
        var input = new ArrayList<PersistenceMutation>(List.of(first, second));
        var request = new MutationBatchRequest("root reason", input);
        input.clear();
        assertEquals(List.of(first, second), request.items());
        assertEquals("root reason", request.comment());
        assertThrows(UnsupportedOperationException.class, () -> request.items().clear());
        assertFalse(request.toString().contains("root reason"));
    }

    @Test public void differentlyOwnedRootIntentsAreRejectedInsteadOfSilentlyReplaced() {
        assertThrows(IllegalArgumentException.class,
                () -> new MutationBatchRequest("batch root", List.of(item("another request root"))));
    }
}
