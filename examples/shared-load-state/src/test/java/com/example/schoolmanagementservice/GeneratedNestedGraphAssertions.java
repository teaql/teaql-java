package com.example.schoolmanagementservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.UserContext;
import io.teaql.data.dynamic.DynamicFieldSelection;
import io.teaql.data.dynamic.DynamicFieldValue;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/** Application-owned acceptance using current object/field Assist only. */
final class GeneratedNestedGraphAssertions {
    private GeneratedNestedGraphAssertions() {}

    static void verifyDynamic(UserContext context, Long schoolId, String renamed,
            String second, String extension, ObjectMapper mapper) throws Exception {
        String nullName = renamed + " Dynamic NULL Probe";
        var platform = Q.platforms().withIdIs(1L).limit(1)
                .comment("what: reuse the full seeded root for the NULL probe")
                .purpose("why: retain whole-object mutation validation").executeForOne(context);
        var probe = Q.schools().comment("what: allocate a separate nullable extension probe")
                .purpose("why: preserve ordinary sibling mutation isolation").newEntity(context);
        probe.updatePlatform(platform); probe.updateSchoolTypeToPrimary();
        probe.updateName(nullName); probe.updateAddress("Controlled extension probe");
        probe.updateEstablishedDate(LocalDate.of(1995,9,1)); probe.updateStudentCapacity(0); probe.updateActive(false);
        probe.updateCreateTime(LocalDateTime.of(2023,11,14,22,13)); probe.updateUpdateTime(LocalDateTime.of(2023,11,14,22,13));
        probe.auditAs("Create isolated nested extension probe").save(context);
        var reload = Q.schools().withIdIs(probe.getId());
        reload.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(extension));
        probe = reload.limit(1).comment("what: load storage-bound extension definitions")
                .purpose("why: seed explicit NULL through audited mutation").executeForOne(context);
        probe.updateDynamicField(extension, null);
        probe.auditAs("Seed explicit NULL through the generated mutation API").save(context);

        var child = Q.schoolsWithMinimalFields().withNameIn(renamed, second, nullName).selectName();
        child.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(extension));
        var loaded = Q.platformsWithMinimalFields().withIdIs(1L)
                .selectSchoolListWith(child.orderByIdAscending().limit(3)).limit(1)
                .comment("what: load Value, absent and NULL extensions on bounded children")
                .purpose("why: verify nested row-owned dynamic field carriers").executeForOne(context);
        var rows = loaded.getSchoolList();
        assertEquals(3, rows.size());
        var metadata = rows.get(0).dynamicFields().metadata();
        for (var row : rows.getData()) {
            assertSame(metadata, row.dynamicFields().metadata(), "compatible children share immutable name/type metadata");
            assertFalse(com.example.schoolmanagementservice.school.School.__TEAQL_FIXED_FIELD_INDEXES.containsKey("#" + extension));
            assertFalse(com.example.schoolmanagementservice.school.School.__TEAQL_FIXED_FIELD_INDEXES.containsKey("_" + extension));
        }
        assertEquals("Private extension", rows.get(0).dynamicFields().field(extension).value());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, rows.get(1).dynamicFields().field(extension).state());
        assertEquals(DynamicFieldValue.State.NULL, rows.get(2).dynamicFields().field(extension).state());
        assertSame(rows.get(0).__internalLoadState(), rows.get(2).__internalLoadState());
        assertNotSame(rows.get(0).__internalLoadState(), rows.get(1).__internalLoadState());
        var json = mapper.readTree(mapper.writeValueAsString(loaded));
        assertEquals("Private extension", json.get("schoolList").get(0).get("#" + extension).asText());
        assertFalse(json.get("schoolList").get(1).has("#" + extension));
        assertTrue(json.get("schoolList").get(2).get("#" + extension).isNull());

        var nestedChild = Q.schoolsWithMinimalFields().withNameIn(renamed, nullName).selectName();
        nestedChild.selectDynamicFieldsWith(new DynamicFieldSelection().selectString(extension));
        var nested = Q.schoolsWithMinimalFields().withIdIs(schoolId)
                .selectPlatformWith(Q.platformsWithMinimalFields()
                        .selectSchoolListWith(nestedChild.orderByIdAscending().limit(2)))
                .limit(1).comment("what: retain dynamic children through a forward ancestor")
                .purpose("why: verify nested graph presentation").executeForOne(context);
        json = mapper.readTree(mapper.writeValueAsString(nested));
        assertEquals("Private extension", json.get("platform").get("schoolList").get(0).get("#" + extension).asText());
        assertTrue(json.get("platform").get("schoolList").get(1).get("#" + extension).isNull());
        System.out.println("PASS generated Java nested dynamic Value/NULL/NotLoaded and shared snapshots");
        System.out.println("PASS generated Java LF17 dynamic metadata shared without fixed slots");
    }

    static void verify(UserContext context, Long schoolId, String renamed,
            String second, ObjectMapper mapper) throws Exception {
        var platform = Q.platformsWithMinimalFields().withIdIs(1L)
                .selectSchoolListWith(Q.schoolsWithMinimalFields().withNameIn(renamed, second)
                        .selectName().orderByIdAscending().limit(2))
                .limit(1).comment("what: load two selected Schools under the seeded Platform")
                .purpose("why: verify generated reverse graph state and JSON").executeForOne(context);
        assertTrue(platform.isPropertyLoaded("schoolList"));
        var children = platform.getSchoolList();
        assertEquals(2, children.size());
        assertEquals(renamed, E.school(children.get(0)).getName().eval());
        assertEquals(second, E.school(children.get(1)).getName().eval());
        var state = children.get(0).__internalLoadState();
        assertSame(state, children.get(1).__internalLoadState());
        var json = mapper.readTree(mapper.writeValueAsString(platform));
        assertEquals(renamed, json.get("schoolList").get(0).get("name").asText());
        assertEquals(second, json.get("schoolList").get(1).get("name").asText());
        assertFalse(json.get("schoolList").get(0).has("address"));
        assertFalse(json.get("schoolList").get(0).has("_original_values"));
        assertSame(state, children.get(0).__internalLoadState());
        var restored = mapper.treeToValue(json, platform.getClass());
        assertEquals(2, restored.getSchoolList().size());
        assertEquals(renamed, E.school(restored.getSchoolList().get(0)).getName().eval());
        assertSame(restored.getSchoolList().get(0).__internalLoadState(), restored.getSchoolList().get(1).__internalLoadState());
        assertFalse(restored.getSchoolList().get(0).isPropertyLoaded("address"));
        assertFalse(restored.__internalHasMutationLedger());

        var nested = Q.schoolsWithMinimalFields().withIdIs(schoolId)
                .selectPlatformWith(Q.platformsWithMinimalFields()
                        .selectSchoolListWith(Q.schoolsWithMinimalFields().withNameIn(renamed, second)
                                .selectName().orderByIdAscending().limit(2)))
                .limit(1).comment("what: load School to Platform to its bounded School list")
                .purpose("why: verify forward ancestor and reverse descendant availability")
                .executeForOne(context);
        var parent = E.school(nested).getPlatform().eval();
        assertTrue(parent.isPropertyLoaded("schoolList"));
        assertEquals(2, parent.getSchoolList().size());
        var nestedJson = mapper.readTree(mapper.writeValueAsString(nested));
        assertEquals(renamed, nestedJson.get("platform").get("schoolList").get(0).get("name").asText());
        assertEquals(second, nestedJson.get("platform").get("schoolList").get(1).get("name").asText());
        var restoredNested = mapper.treeToValue(nestedJson, nested.getClass());
        assertEquals(2, E.school(restoredNested).getPlatform().eval().getSchoolList().size());
        assertEquals(nestedJson, mapper.readTree(mapper.writeValueAsString(restoredNested)));

        var empty = Q.schoolTypesWithMinimalFields().withIdIs(1002L)
                .selectSchoolListWith(Q.schoolsWithMinimalFields().selectName().orderByIdAscending().limit(2))
                .limit(1).comment("what: explicitly load the unused Secondary constant's Schools")
                .purpose("why: distinguish loaded empty from omitted reverse detail").executeForOne(context);
        assertTrue(empty.isPropertyLoaded("schoolList"));
        assertNotNull(empty.getSchoolList());
        assertTrue(empty.getSchoolList().isEmpty());
        var emptyJson = mapper.readTree(mapper.writeValueAsString(empty));
        assertTrue(emptyJson.get("schoolList").isArray());
        assertEquals(0, emptyJson.get("schoolList").size());
        var restoredEmpty = mapper.treeToValue(emptyJson, empty.getClass());
        assertTrue(restoredEmpty.isPropertyLoaded("schoolList"));
        assertTrue(restoredEmpty.getSchoolList().isEmpty());

        var unselected = Q.schoolTypesWithMinimalFields().withIdIs(1002L)
                .limit(1).comment("what: load the same constant without selecting its Schools")
                .purpose("why: NotLoaded must not serialize as an empty list").executeForOne(context);
        assertFalse(unselected.isPropertyLoaded("schoolList"));
        assertFalse(mapper.readTree(mapper.writeValueAsString(unselected)).has("schoolList"));
        var restoredUnselected = mapper.readValue(mapper.writeValueAsString(unselected), unselected.getClass());
        assertFalse(restoredUnselected.isPropertyLoaded("schoolList"));
        System.out.println("PASS generated Java LF09 reverse Loaded/Empty/NotLoaded through Q/E/JSON");

        var filtered = Q.schoolsWithMinimalFields().withIdIs(schoolId)
                .selectSchoolTypeWith(Q.schoolTypesWithMinimalFields().withIdIs(1002L))
                .limit(1).comment("what: retain Primary FK while excluding it from selected target detail")
                .purpose("why: verify current filtered-forward NotLoaded semantics").executeForOne(context);
        assertEquals(1001L, filtered.getSchoolType().getId());
        assertTrue(filtered.getSchoolType().isPropertyLoaded("id"), "FK identity cannot become NotLoaded with excluded details");
        assertFalse(filtered.getSchoolType().isPropertyLoaded("code"));
        var filteredJson = mapper.readTree(mapper.writeValueAsString(filtered));
        assertFalse(filteredJson.get("schoolType").has("code"));
        var restoredFiltered = mapper.treeToValue(filteredJson, filtered.getClass());
        assertEquals(1001L, restoredFiltered.getSchoolType().getId());
        assertFalse(restoredFiltered.getSchoolType().isPropertyLoaded("code"));
        assertFalse(restoredFiltered.__internalHasMutationLedger());
        System.out.println("PASS generated Java LF08 loaded FK and excluded forward details stay distinct");
        System.out.println("PASS generated Java typed JSON graph roundtrip and Empty/NotLoaded isolation");
        System.out.println("PASS generated Java nested/reverse graph Q/E/JSON and Empty/NotLoaded isolation");
    }
}
