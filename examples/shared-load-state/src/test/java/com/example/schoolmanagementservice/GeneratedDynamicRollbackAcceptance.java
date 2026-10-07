package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import static org.junit.jupiter.api.Assertions.*;

/** Real transaction/readback failure plus mixed-projection list lifetime. */
final class GeneratedDynamicRollbackAcceptance {
    static School verify(UserContext context, ObservedJdbcExecutor sql, School original,
            String firstName, String secondName, String code, JdbcDynamicFieldsProvider provider,
            DynamicFieldContext definitionContext) {
        String unused = code + "_unused";
        DynamicFieldDef def = new DynamicFieldDef();
        def.setScope(DynamicFieldScope.global()); def.setOwnerType("School");
        def.setCode(unused); def.setName(unused); def.setDataType(DynamicDataType.STRING);
        provider.registerFieldDef(definitionContext, def);
        var selection = new DynamicFieldSelection().selectString(code).selectString(unused);
        original.updateDynamicField(code, "matrix baseline");
        original.auditAs("seed the combined-state value control").save(context);
        var companionRequest = Q.schools().withNameIn(secondName);
        companionRequest.selectDynamicFieldsWith(selection);
        var companion = companionRequest.limit(1).comment("what: select companion extension metadata")
                .purpose("why: seed an explicit NULL through audited mutation").executeForOne(context);
        companion.updateDynamicField(code, null);
        companion.auditAs("seed the combined-state null control").save(context);
        var all = Q.schools().withNameIn(firstName, secondName);
        all.selectDynamicFieldsWith(selection);
        var rows = all.orderByIdAscending().limit(2).comment("what: load value and null extensions together")
                .purpose("why: combine shared geometry with private row payloads").executeForList(context);
        assertEquals(2, rows.size());
        var state = rows.get(0).__internalLoadState();
        assertSame(state, rows.get(1).__internalLoadState());
        if (Boolean.parseBoolean(System.getenv("TEAQL_LOAD_STATE_WIDE"))) assertFalse(state.overflow().isEmpty());
        assertEquals(DynamicFieldValue.State.VALUE, rows.get(0).dynamicFields().field(code).state());
        assertEquals(DynamicFieldValue.State.NULL, rows.get(1).dynamicFields().field(code).state());
        for (var row : rows.getData()) assertEquals(DynamicFieldValue.State.NOT_LOADED, row.dynamicFields().field(unused).state());
        var sparseRequest = Q.schoolsWithMinimalFields().withNameIn(firstName).selectName();
        sparseRequest.selectDynamicFieldsWith(selection);
        var sparse = sparseRequest.limit(1).comment("what: append the same identity with a sparse projection")
                .purpose("why: mixed lists must not union native loaded fields").executeForOne(context);
        var sparseState = sparse.__internalLoadState();
        SmartList<School> held = new SmartList<>();
        held.add(rows.get(0)); held.add(rows.get(1)); held.add(sparse);
        rows = null;
        assertFalse(held.get(2).isPropertyLoaded("address"));
        assertSame(sparseState, held.get(2).__internalLoadState());
        assertSame(state, held.get(0).__internalLoadState());
        assertSame(state, held.get(1).__internalLoadState());
        School pending = held.get(0);
        Long version = pending.getVersion(), companionVersion = held.get(1).getVersion();
        String nextName = firstName + " retry";
        pending.updateName(nextName); pending.updateDynamicField(code, "matrix retry");
        assertSame(state, pending.__internalLoadState());
        long[] before = sql.counts();
        sql.failNextDynamicReadbackAfterWrite();
        try { assertThrows(RuntimeException.class, () -> pending.auditAs("rollback a failed extension readback").save(context)); }
        finally { sql.clearReadbackFailure(); }
        assertTrue(sql.readbackFailureObserved(), "the fault must happen after actual extension DML");
        assertTrue(sql.counts()[1] > before[1]); assertTrue(sql.counts()[2] > before[2]);
        assertEquals(version, pending.getVersion());
        assertFalse(pending.__internalDynamicMutations().isEmpty());
        assertSame(state, pending.__internalLoadState());
        assertEquals(companionVersion, held.get(1).getVersion());
        assertTrue(held.get(1).getUpdatedProperties().isEmpty());
        assertEquals(DynamicFieldValue.State.NULL, held.get(1).dynamicFields().field(code).state());
        assertEquals("matrix baseline", held.get(2).dynamicFields().field(code).value());
        assertFalse(held.get(2).isPropertyLoaded("address"));
        var inspect = Q.schools().withNameIn(firstName, secondName);
        inspect.selectDynamicFieldsWith(selection);
        var stored = inspect.orderByIdAscending().limit(2).comment("what: inspect both stores after readback failure")
                .purpose("why: failure must roll back native and extension DML atomically").executeForList(context);
        assertEquals(2, stored.size()); assertEquals(version, stored.get(0).getVersion());
        assertEquals(firstName, E.school(stored.get(0)).getName().eval());
        assertEquals("matrix baseline", stored.get(0).dynamicFields().field(code).value());
        assertEquals(companionVersion, stored.get(1).getVersion());
        assertEquals(DynamicFieldValue.State.NULL, stored.get(1).dynamicFields().field(code).state());
        pending.auditAs("retry the original native and extension intent").save(context);
        assertEquals(version + 1, pending.getVersion());
        assertEquals(nextName, E.school(pending).getName().eval());
        assertEquals("matrix retry", pending.dynamicFields().field(code).value());
        assertEquals(DynamicFieldValue.State.NOT_LOADED, pending.dynamicFields().field(unused).state());
        assertSame(state, pending.__internalLoadState());
        assertTrue(pending.__internalDynamicMutations().isEmpty());
        assertTrue(pending.getUpdatedProperties().isEmpty());
        assertEquals(companionVersion, held.get(1).getVersion());
        assertSame(state, held.get(1).__internalLoadState());
        System.out.println("PASS generated Java mixed dynamic Value/Null/NotLoaded list lifetime readback rollback and retry");
        return pending;
    }
}
