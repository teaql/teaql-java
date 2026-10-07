package com.example.schoolmanagementservice;

import io.teaql.core.UserContext;
import static org.junit.jupiter.api.Assertions.*;

/** Two independently mutable roots may share availability, never save authority. */
final class GeneratedIndependentMutationAcceptance {
    static void verify(UserContext context, Long firstId, String firstName, String secondName) {
        var rows = Q.schools().withNameIn(firstName, secondName).orderByIdAscending().limit(2)
                .comment("what: load two roots with shared field availability")
                .purpose("why: saving one root must not consume another root's pending intent").executeForList(context);
        assertEquals(2, rows.size());
        var first = rows.get(0); var second = rows.get(1);
        assertEquals(firstId, first.getId());
        var state = first.__internalLoadState();
        assertSame(state, second.__internalLoadState());
        Long firstVersion = first.getVersion(), secondVersion = second.getVersion();
        String firstPending = firstName + " independent ledger probe";
        String secondPending = secondName + " abandoned ledger probe";
        first.updateName(firstPending); second.updateName(secondPending);
        assertSame(state, first.__internalLoadState());
        assertSame(state, second.__internalLoadState());
        assertNotSame(first.getEntityMutationLedger(), second.getEntityMutationLedger());
        first.auditAs("save only the first root while another root remains dirty").save(context);
        var stored = Q.schools().withNameIn(firstPending, secondName).orderByIdAscending().limit(2)
                .comment("what: inspect both roots after saving only the first")
                .purpose("why: detect accidental persistence of an independent pending mutation").executeForList(context);
        assertEquals(2, stored.size());
        assertEquals(firstPending, E.school(stored.get(0)).getName().eval());
        assertEquals(firstVersion + 1, stored.get(0).getVersion());
        assertEquals(secondName, E.school(stored.get(1)).getName().eval());
        assertEquals(secondVersion, stored.get(1).getVersion());
        assertEquals(secondPending, E.school(second).getName().eval());
        assertEquals(secondVersion, second.getVersion());
        assertTrue(second.getUpdatedProperties().contains("name"));
        assertTrue(first.getUpdatedProperties().isEmpty());
        assertSame(state, second.__internalLoadState());
        // Abandon the second root and restore the first through the same audited API.
        first.updateName(firstName);
        first.auditAs("restore the independently saved test root").save(context);
        System.out.println("PASS generated Java saving one dirty root leaves another dirty root unpersisted");
    }
}
