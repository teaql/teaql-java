package io.teaql.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class NamedRelationLoadStateAliasTest {
    private static final class Row extends BaseEntity {}
    private static FieldLayout layout(Set<String> relations) {
        return FieldLayout.generated(Row.class, "named-alias-v1",
                Map.of("id", 0, "version", 1, "base_url", 2),
                Map.of("id", List.of("id", "id"), "version", List.of("version", "version"),
                        "base_url", List.of("baseUrl", "legacy_url")), relations);
    }

    @Test public void modelAndNativeProjectionNamesProduceOneCanonicalShape() {
        var layout = layout(Set.of("school_list"));
        var canonical = LoadState.projection(layout, List.of("id", "base_url", "school_list"));
        var nativeNames = LoadState.projection(layout, List.of("id", "legacy_url", "schoolList"));
        assertEquals(canonical, nativeNames);
        assertEquals(canonical.hashCode(), nativeNames.hashCode());
        assertEquals(Set.of("school_list"), nativeNames.selectedNames());
        assertEquals(5L, nativeNames.bits());
    }

    @Test public void nativeLookupAndCopyOnWriteKeepOneSharedMarker() {
        var state = LoadState.projection(layout(Set.of("school_list")), List.of("school_list"));
        assertTrue(state.isLoaded("school_list"));
        assertTrue(state.isLoaded("schoolList"));
        assertSame(state, state.withLoaded("schoolList", true));
        var detached = state.withLoaded("schoolList", false);
        assertFalse(detached.isLoaded("school_list"));
        assertFalse(detached.isLoaded("schoolList"));
        assertTrue(state.isLoaded("schoolList"));
    }

    @Test public void persistentDynamicCodesRemainLiteralAndSeparate() {
        var state = layout(Set.of("school_list")).emptyState().withLoaded("schoolList", true)
                .withDynamicSelection(Set.of("schoolList"));
        assertEquals(Set.of("school_list", "#schoolList"), state.selectedNames());
        assertTrue(state.isLoaded("#schoolList"));
        assertFalse(state.isLoaded("#school_list"));
        assertTrue(state.isLoaded("school_list"));
    }

    @Test public void ambiguousNamedRelationAliasesAreRejectedLikeFixedAliases() {
        assertThrows(IllegalArgumentException.class, () -> layout(Set.of("school_list", "schoolList")));
    }
}
