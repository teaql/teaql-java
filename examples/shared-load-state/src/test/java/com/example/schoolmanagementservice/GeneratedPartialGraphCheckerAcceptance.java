package com.example.schoolmanagementservice;

import io.teaql.core.UserContext;
import io.teaql.core.checker.CheckException;
import static org.junit.jupiter.api.Assertions.*;

/** A referenced object that is actually modified must itself be complete. */
final class GeneratedPartialGraphCheckerAcceptance {
    static void verify(UserContext context, ObservedJdbcExecutor sql, String name) {
        var row = Q.schools().withNameIn(name)
                .selectPlatformWith(Q.platformsWithMinimalFields().selectName()).limit(1)
                .comment("what: load a full School with partial Platform detail")
                .purpose("why: Checker must reject mutation of a partial referenced object").executeForOne(context);
        assertTrue(row.isPropertyLoaded("address"));
        assertTrue(row.getPlatform().isPropertyLoaded("name"));
        assertFalse(row.getPlatform().isPropertyLoaded("baseUrl"));
        var untouched = row.getPlatform();
        var untouchedState = untouched.__internalLoadState();
        row.updateName("Complete parent control");
        row.auditAs("save complete parent without modifying partial Platform").save(context);
        assertSame(untouchedState, untouched.__internalLoadState());
        assertFalse(untouched.isPropertyLoaded("baseUrl"));
        assertFalse(untouched.isPropertyLoaded("updateTime"));
        var restored = Q.schools().withNameIn("Complete parent control").limit(1)
                .comment("what: read saved complete parent")
                .purpose("why: verify untouched partial reference does not block persistence").executeForOne(context);
        assertNotNull(restored);
        restored.updateName(name);
        restored.auditAs("restore parent after modified-only Checker control").save(context);
        System.out.println("PASS generated Java complete parent saves with untouched partial reference");
        row = Q.schools().withNameIn(name)
                .selectPlatformWith(Q.platformsWithMinimalFields().selectName()).limit(1)
                .comment("what: reload partial Platform detail after the positive save")
                .purpose("why: mutate the materialized child rather than a readback identity reference").executeForOne(context);
        var child = row.getPlatform(); child.updateName("Partial child control");
        var state = row.__internalLoadState();
        long[] before = sql.counts();
        assertThrows(CheckException.class, () -> child.auditAs("reject mutation of the incomplete referenced object").save(context));
        assertArrayEquals(before, sql.counts(), "partial graph must reject before any provider entry");
        assertSame(state, row.__internalLoadState());
        assertFalse(row.getPlatform().isPropertyLoaded("baseUrl"));
        assertFalse(row.getPlatform().isPropertyLoaded("createTime"));
        // The declared updateTime Fix is a real assignment and may detach the
        // child's snapshot. It must not mark missing business fields loaded.
        assertTrue(row.getPlatform().isPropertyLoaded("updateTime"));
        assertFalse(untouchedState.isLoaded("baseUrl"));
        assertFalse(untouchedState.isLoaded("updateTime"));
        System.out.println("PASS generated Java modified partial referenced object rejects before provider entry without loading missing business fields");
    }
}
