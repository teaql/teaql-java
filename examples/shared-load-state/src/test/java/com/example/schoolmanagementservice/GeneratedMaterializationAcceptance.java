package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.schoolcapacitysummary.SchoolCapacitySummary;
import io.teaql.core.FieldLayout;
import io.teaql.core.UserContext;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded Q/E calculation followed by an explicitly modeled, audited write. */
final class GeneratedMaterializationAcceptance {
    static void verify(UserContext context, Long id, String name, String sibling, String round) {
        var source = Q.schools().withIdIs(id).selectSelfFields().limit(1)
                .comment("what: load the capacity source before its audited change")
                .purpose("why: retain complete Checker input").executeForOne(context);
        source.updateStudentCapacity(37);
        source.auditAs("Set the nonzero materialization control").save(context);
        var rows = Q.schools().withNameIn(name, sibling).selectSelfFields()
                .orderByIdAscending().limit(2).comment("what: read the two capacity contributors")
                .purpose("why: calculate the bounded reporting snapshot with E").executeForList(context);
        assertEquals(2, rows.size());
        int total = rows.stream().mapToInt(row -> E.school(row).getStudentCapacity().eval()).sum();
        assertEquals(37, total);
        var shared = rows.get(0).__internalLoadState();
        assertSame(shared, rows.get(1).__internalLoadState());
        var view = rows.get(0);
        view.addDynamicProperty("total_capacity", total);
        assertEquals(37, ((Number) view.getProperty("_total_capacity")).intValue());
        assertNull(view.getProperty("_missing_total"));
        assertTrue(view.getUpdatedProperties().isEmpty());
        assertNull(FieldLayout.forType(School.class).findIndex("_total_capacity"));
        view.updateActive(true);
        assertFalse(view.getUpdatedProperties().contains("_total_capacity"));
        view.auditAs("Save a native change without persisting its readonly total").save(context);

        var platform = Q.platforms().withIdIs(1L).limit(1)
                .comment("what: reuse the model-owned reporting root")
                .purpose("why: attach the explicit summary target").executeForOne(context);
        var target = Q.schoolCapacitySummaries()
                .comment("what: allocate a modeled capacity summary")
                .purpose("why: explicitly persist the reporting result").newEntity(context);
        target.updatePlatform(platform);
        target.updateName(round + " capacity snapshot");
        target.updateTotalCapacity(total);
        target.updateSchoolCount(rows.size());
        target.auditAs("Materialize the calculated total in its own entity").save(context);
        var read = Q.schoolCapacitySummaries().withIdIs(target.getId()).selectSelfFields().limit(1)
                .comment("what: reload the persisted summary")
                .purpose("why: verify modeled values through E").executeForOne(context);
        assertEquals(37, E.schoolCapacitySummary(read).getTotalCapacity().eval());
        assertEquals(2, E.schoolCapacitySummary(read).getSchoolCount().eval());
        assertNotNull(FieldLayout.forType(SchoolCapacitySummary.class).findIndex("total_capacity"));
        assertSame(shared, rows.get(1).__internalLoadState());
        assertTrue(rows.get(1).getUpdatedProperties().isEmpty());
        var fresh = Q.schools().withIdIs(id).selectSelfFields().limit(1)
                .comment("what: reload the source after materialization")
                .purpose("why: prove the readonly total never became stored source data").executeForOne(context);
        assertNull(fresh.getProperty("_total_capacity"));
        assertEquals(37, E.school(fresh).getStudentCapacity().eval());
        assertEquals(true, E.school(fresh).isActive().eval());
        System.out.println("PASS generated Java LF20 readonly total persists only through modeled materialization");
    }
}
