package com.example.schoolmanagementservice;

import io.teaql.core.UserContext;
import javax.sql.DataSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/** Native-only invalid authoritative readback, not an extension-provider failure. */
final class GeneratedNativeRollbackAcceptance {
    static void verify(UserContext context, DataSource source, ObservedJdbcExecutor sql,
            String name, String sibling) throws Exception {
        var rows = Q.schools().withNameIn(name, sibling).selectSelfFields().orderByIdAscending().limit(2)
                .comment("what: load native-only rollback controls")
                .purpose("why: preserve siblings through authoritative readback failure").executeForList(context);
        assertEquals(2, rows.size());
        var snapshot = rows.get(0).__internalLoadState();
        assertSame(snapshot, rows.get(1).__internalLoadState());
        var pending = rows.get(0);
        Long version = pending.getVersion(), siblingVersion = rows.get(1).getVersion();
        String nextName = name + " native-retry";
        pending.updateName(nextName);
        assertSame(snapshot, pending.__internalLoadState());
        long[] before = sql.counts();
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER controlled_native_readback AFTER UPDATE OF name ON school_data "
                    + "WHEN NEW.name LIKE '% native-retry' "
                    + "BEGIN UPDATE school_data SET established_date='not-a-date' WHERE id=NEW.id; END");
            try {
                assertThrows(RuntimeException.class, () -> pending
                        .auditAs("Rollback an invalid native authoritative readback").save(context));
            } finally { statement.execute("DROP TRIGGER controlled_native_readback"); }
        }
        assertTrue(sql.counts()[1] > before[1], "native DML must occur before readback fails");
        assertTrue(sql.counts()[2] > before[2]);
        assertEquals(version, pending.getVersion());
        assertTrue(pending.getUpdatedProperties().contains("name"));
        assertSame(snapshot, pending.__internalLoadState());
        assertEquals(siblingVersion, rows.get(1).getVersion());
        assertTrue(rows.get(1).getUpdatedProperties().isEmpty());
        assertSame(snapshot, rows.get(1).__internalLoadState());
        var stored = Q.schools().withNameIn(name, sibling).selectSelfFields().orderByIdAscending().limit(2)
                .comment("what: inspect native rows after readback failure")
                .purpose("why: prove the native transaction was rolled back").executeForList(context);
        assertEquals(2, stored.size());
        assertEquals(name, E.school(stored.get(0)).getName().eval());
        assertEquals(version, stored.get(0).getVersion());
        assertEquals(LocalDate.of(1995, 9, 1), E.school(stored.get(0)).getEstablishedDate().eval());
        assertEquals(siblingVersion, stored.get(1).getVersion());
        pending.auditAs("Retry the unchanged native mutation intent").save(context);
        assertEquals(version + 1, pending.getVersion());
        assertEquals(nextName, E.school(pending).getName().eval());
        assertTrue(pending.getUpdatedProperties().isEmpty());
        assertSame(snapshot, pending.__internalLoadState());
        pending.updateName(name);
        pending.auditAs("Restore the native rollback fixture name").save(context);
        System.out.println("PASS generated Java LF11 native readback rollback retains loaded state and retry intent");
    }
}
