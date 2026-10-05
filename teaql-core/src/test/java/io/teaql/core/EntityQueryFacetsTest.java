package io.teaql.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityQueryFacetsTest {
    @Test public void absentAndRequestedEmptyAreDifferentWithoutImplicitLoading() {
        var entity = new BaseEntity();
        assertNull(entity.getQueryFacet("orders"));
        entity.__internalSetQueryFacets(Map.of("orders", new SmartList<>()));
        assertNotNull(entity.getQueryFacet("orders"));
        assertTrue(entity.getQueryFacet("orders").isEmpty());
    }

    @Test public void sidecarCopiesTheMapAndDoesNotShareAnotherEntitysMetadata() {
        var source = new HashMap<String, SmartList<?>>();
        source.put("orders", new SmartList<>());
        var first = new BaseEntity(); first.__internalSetQueryFacets(source);
        source.clear();
        var second = new BaseEntity();
        assertNotNull(first.getQueryFacet("orders"));
        assertNull(second.getQueryFacet("orders"));
        assertThrows(UnsupportedOperationException.class, () -> first.getQueryFacets().clear());
    }

    @Test public void queryMetadataDoesNotBecomeAModelMutationOrJsonField() throws Exception {
        var entity = new BaseEntity();
        var before = entity.getUpdatedProperties();
        var ledger = entity.getEntityMutationLedger();
        entity.__internalSetQueryFacets(Map.of("orders", new SmartList<>()));
        assertEquals(before, entity.getUpdatedProperties());
        assertSame(ledger, entity.getEntityMutationLedger());
        assertNull(entity.getDynamicProperty("queryFacets"));
        assertFalse(new ObjectMapper().valueToTree(entity).has("queryFacets"));
    }

    @Test public void replacementClearsPreviouslyLoadedQueryMetadata() {
        var entity = new BaseEntity();
        entity.__internalSetQueryFacets(Map.of("orders", new SmartList<>()));
        entity.__internalSetQueryFacets(Map.of());
        assertNull(entity.getQueryFacet("orders"));
    }
}
