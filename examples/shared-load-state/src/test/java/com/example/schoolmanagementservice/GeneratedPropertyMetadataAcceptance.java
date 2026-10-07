package com.example.schoolmanagementservice;

import io.teaql.core.DynamicPropertyMetadata;
import io.teaql.core.UserContext;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class GeneratedPropertyMetadataAcceptance {
    static void verify(UserContext context, String first, String second) {
        var request = Q.schoolsWithMinimalFields().withNameIn(first,second).selectName().orderByIdAscending().limit(2);
        var metadata = new DynamicPropertyMetadata(Map.of("_missing_count",Long.class,"_count",Number.class,"_null",String.class));
        request.addSimpleDynamicProperty("_count",new io.teaql.core.PropertyReference("studentCapacity"));
        String nullableMember=io.teaql.core.FieldLayout.forType(com.example.schoolmanagementservice.school.School.class).memberName("probe_0");
        if(nullableMember!=null) request.addSimpleDynamicProperty("_null",new io.teaql.core.PropertyReference(nullableMember));
        request.setDynamicPropertyMetadata(metadata);
        var rows = request.comment("what: load generated School with a readonly schema")
            .purpose("why: keep schema sharing distinct from value presence").executeForList(context);
        assertEquals(2,rows.size());
        assertEquals(first,E.school(rows.first()).getName().eval());
        assertSame(rows.get(0).__internalLoadState(),rows.get(1).__internalLoadState());
        for(var row:rows.getData()) {
            assertSame(metadata,row.__internalLoadState().dynamicPropertyMetadata());
            assertSame(Long.class,row.getDynamicPropertyType("_missing_count"));
            assertNull(row.getDynamicProperty("_missing_count"));
            assertFalse(row.hasDynamicProperty("_missing_count"));
            assertFalse(row.isPropertyLoaded("_missing_count"));
            assertTrue(row.getUpdatedProperties().isEmpty());
            assertEquals(0,((Number)row.getDynamicProperty("_count")).intValue());
            assertTrue(row.hasDynamicProperty("_count"));
            assertNull(row.getDynamicProperty("_null"));
            assertEquals(nullableMember!=null,row.hasDynamicProperty("_null"));
        }
        try(var stream=request.comment("what: stream generated School readonly schema")
            .purpose("why: retain schema after cursor cleanup").executeForStream(context)) {
            var streamed=stream.toList();assertEquals(2,streamed.size());
            assertSame(streamed.get(0).__internalLoadState(),streamed.get(1).__internalLoadState());
            assertSame(Long.class,streamed.get(0).getDynamicPropertyType("_missing_count"));
        }
        System.out.println("PASS generated Java readonly property metadata shares schema without installing values or fixed slots");
    }
}
