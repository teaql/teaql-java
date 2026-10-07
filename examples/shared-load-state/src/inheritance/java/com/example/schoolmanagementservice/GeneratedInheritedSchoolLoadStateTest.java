package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.academy.Academy;
import com.example.schoolmanagementservice.school.School;
import io.teaql.core.FieldLayout;
import io.teaql.core.UserContext;
import io.teaql.core.checker.CheckException;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.sqlite.SQLiteDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedInheritedSchoolLoadStateTest {
    @Test
    void inheritedLayoutKeepsParentPositionsWithoutSharingTypeIdentity() {
        FieldLayout parent = School.__TEAQL_FIELD_LAYOUT;
        FieldLayout child = Academy.__TEAQL_FIELD_LAYOUT;
        School.__TEAQL_FIXED_FIELD_INDEXES.forEach((name, index) ->
                assertEquals(index, child.findIndex(name), "inherited field " + name));
        assertTrue(child.findIndex("campus_code") >= parent.indexes().size());
        assertNotEquals(parent.revision(), child.revision());
        assertNotSame(parent, child);
    }

    @Test
    void inheritedQEMutationAndSnapshots() {
        String round = System.getenv("TEAQL_LOAD_STATE_ROUND");
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + System.getenv("TEAQL_LOAD_STATE_DATABASE"));
        var sql = new JdbcSqlExecutor(dataSource);
        var ids = new IdSpaceIdGenerator(new GeneratedSchoolLoadStateTest.DatabaseBridge(sql));
        ids.ensureIdSpaceTable();
        var service = new SqliteDataServiceExecutor("sqlite", sql, dataSource);
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", service).dataService("sqlite", service).idGenerationService(ids)
                .build().install(GeneratedRuntimeModule.module());
        UserContext context = new GeneratedSchoolLoadStateTest.Context(runtime);
        context.ensureSchema();
        assertTrue(runtime.getMetadata().resolveEntityDescriptor("Academy").getAuditMaskFields().contains("address"));
        var platform = Q.platforms().withIdIs(1L).limit(1).comment("what: reuse the provisioned root")
                .purpose("why: attach the inherited fixture").executeForOne(context);
        String[] codes = {round + "-campus-a", round + "-campus-b"};
        for (String code : codes) {
            var row = Q.academies().comment("what: create a controlled inherited entity")
                    .purpose("why: verify all inherited model fields").newEntity(context);
            row.updatePlatform(platform);
            row.updateSchoolTypeToPrimary();
            row.updateName(code);
            row.updateAddress("Private inherited address");
            row.updateEstablishedDate(LocalDate.of(1995, 9, 1));
            row.updateStudentCapacity(0);
            row.updateActive(false);
            row.updateCreateTime(LocalDateTime.of(2023, 11, 14, 22, 13));
            row.updateUpdateTime(LocalDateTime.of(2023, 11, 14, 22, 13));
            row.updateCampusCode(code);
            row.auditAs("create inherited state fixture").save(context);
        }
        var full = Q.academies().withCampusCodeIn(codes).selectSelfFields().orderByIdAscending().limit(2)
                .comment("what: load two full inherited rows").purpose("why: inspect actual shared snapshots")
                .executeForList(context);
        assertEquals(2, full.size());
        var snapshot = full.get(0).__internalLoadState();
        assertSame(snapshot, full.get(1).__internalLoadState());
        assertEquals(codes[0], E.academy(full.get(0)).getName().eval());
        assertEquals(codes[0], E.academy(full.get(0)).getCampusCode().eval());
        assertEquals(0, E.academy(full.get(0)).getStudentCapacity().eval());
        assertEquals(false, E.academy(full.get(0)).isActive().eval());
        var streamRequest=Q.academiesWithMinimalFields().withCampusCodeIn(codes).selectCampusCode()
                .orderByIdAscending().limit(2).comment("what: stream two sparse inherited rows")
                .purpose("why: qualify typed cursor overflow and NotLoaded boundaries");
        try(var stream=context.executeForStream(streamRequest)) {
            var streamed=stream.toList();assertEquals(2,streamed.size());
            assertSame(streamed.get(0).__internalLoadState(),streamed.get(1).__internalLoadState());
            assertEquals(codes[0],E.academy(streamed.get(0)).getCampusCode().eval());
            assertFalse(streamed.get(0).isPropertyLoaded("name"));
            assertFalse(streamed.get(0).__internalHasMutationLedger());
            if("true".equals(System.getenv("TEAQL_LOAD_STATE_WIDE")))
                assertTrue(Academy.__TEAQL_FIXED_FIELD_INDEXES.get("campus_code")>=64);
        }
        if ("true".equals(System.getenv("TEAQL_LOAD_STATE_WIDE"))) {
            for (int slot : new int[]{63, 64, 65, 129}) {
                String field = School.__TEAQL_FIXED_FIELD_INDEXES.entrySet().stream()
                        .filter(entry -> entry.getValue() == slot).map(java.util.Map.Entry::getKey).findFirst().orElseThrow();
                assertTrue(snapshot.isLoaded(field));
                assertNull(full.get(0).__internalGet(School.__TEAQL_FIXED_FIELD_MAPPINGS.get(field).get(0)));
            }
        }
        Long id = full.get(0).getId();
        var sparse = Q.academiesWithMinimalFields().withIdIs(id).selectCampusCode().limit(1)
                .comment("what: load an incomplete inherited projection").purpose("why: retain full-object Checker enforcement")
                .executeForOne(context);
        assertFalse(sparse.__internalLoadState().isLoaded("name"));
        sparse.updateCampusCode("incomplete inherited mutation");
        assertThrows(CheckException.class, () -> sparse.auditAs("reject sparse inherited save").save(context));
        var changed = full.get(0);
        Long siblingVersion = full.get(1).getVersion();
        String renamed = round + " renamed inherited entity";
        changed.updateName(renamed);
        assertSame(snapshot, changed.__internalLoadState());
        changed.auditAs("change only one inherited object").save(context);
        assertEquals(2L, changed.getVersion(), "inherited version readback must update the saved instance");
        assertEquals(siblingVersion, full.get(1).getVersion());
        assertEquals(codes[1], E.academy(full.get(1)).getName().eval());
        var stored = Q.academies().withIdIs(id).selectSelfFields().limit(1)
                .comment("what: inspect the persisted inherited update").purpose("why: verify optimistic version and values")
                .executeForOne(context);
        assertEquals(2L, stored.getVersion());
        assertEquals(renamed, E.academy(stored).getName().eval());
        stored.markForDeletion().auditAs("delete only the changed inherited object").save(context);
        var remaining = Q.academies().withCampusCodeIn(codes).limit(2)
                .comment("what: inspect the independent inherited sibling").purpose("why: verify isolation after deletion")
                .executeForList(context);
        assertEquals(1, remaining.size());
        assertEquals(siblingVersion, remaining.get(0).getVersion());
        System.out.println("PASS generated Java inherited indexes Q/E/save and snapshot isolation " + round);
    }
}
