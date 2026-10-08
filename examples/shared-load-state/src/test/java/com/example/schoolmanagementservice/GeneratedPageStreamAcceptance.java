package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import io.teaql.core.UserContext;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Generated APIs; retained rows must remain readable after the cursor closes. */
final class GeneratedPageStreamAcceptance {
    static void verify(UserContext context, String first, String second) {
        var left = Q.schoolsWithMinimalFields().withNameIn(first,second).selectName().orderByIdAscending()
                .comment("what: load the first matching School page").purpose("why: qualify sparse page hydration")
                .executeForPage(context,0,1);
        var right = Q.schoolsWithMinimalFields().withNameIn(first,second).selectName().orderByIdAscending()
                .comment("what: load the second matching School page").purpose("why: qualify adjacent pages and filtered total")
                .executeForPage(context,1,1);
        assertEquals(2,left.getTotalCount());assertEquals(2,right.getTotalCount());
        assertEquals(1,left.size());assertEquals(1,right.size());
        assertNotEquals(left.first().getId(),right.first().getId());
        assertEquals(first,E.school(left.first()).getName().eval());
        assertEquals(second,E.school(right.first()).getName().eval());
        assertFalse(left.first().isPropertyLoaded("address"));
        assertFalse(left.first().__internalHasMutationLedger());
        assertEquals(left.first().__internalLoadState(),right.first().__internalLoadState());
        for (boolean full : List.of(false,true)) {
            List<School> rows;
            if (full) {
                try (var stream = Q.schools().withNameIn(first,second).selectSelfFields().orderByIdAscending().limit(2)
                        .comment("what: stream two complete Schools").purpose("why: retain loaded NULL and overflow after cursor cleanup")
                        .executeForStream(context)) { rows = stream.toList(); }
            } else {
                try (var stream = Q.schoolsWithMinimalFields().withNameIn(first,second).selectName().orderByIdAscending().limit(2)
                        .comment("what: stream two sparse Schools").purpose("why: retain NotLoaded boundaries after cursor cleanup")
                        .executeForStream(context)) { rows = stream.toList(); }
            }
            assertEquals(2,rows.size());
            var state=rows.get(0).__internalLoadState();
            for (int i=0;i<rows.size();i++) {
                var row=rows.get(i);assertSame(state,row.__internalLoadState());
                assertEquals(i==0?first:second,E.school(row).getName().eval());
                assertEquals(full,row.isPropertyLoaded("address"));
                assertTrue(row.isPropertyLoaded("id") && row.isPropertyLoaded("version"));
                assertFalse(row.__internalHasMutationLedger());assertTrue(row.getUpdatedProperties().isEmpty());
                if (full && Boolean.parseBoolean(System.getenv("TEAQL_LOAD_STATE_WIDE"))) {
                    for (int slot : new int[]{63,64,65,129}) {
                        String name=School.__TEAQL_FIXED_FIELD_INDEXES.entrySet().stream().filter(e->e.getValue()==slot).findFirst().orElseThrow().getKey();
                        assertTrue(row.isPropertyLoaded(name));
                        assertNull(row.__internalGet(School.__TEAQL_FIXED_FIELD_MAPPINGS.get(name).get(0)));
                    }
                }
            }
        }
        System.out.println("PASS generated Java page and stream shared load state");
    }
}
