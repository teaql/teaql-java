package io.teaql.provider.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Completion belongs to the physical owner, never to an inner save call. */
public class JdbcTransactionCompletionTest {
    private record Source(String url) implements javax.sql.DataSource {
        public java.sql.Connection getConnection() throws java.sql.SQLException { return java.sql.DriverManager.getConnection(url); }
        public java.sql.Connection getConnection(String user, String password) throws java.sql.SQLException { return getConnection(); }
        public java.io.PrintWriter getLogWriter() { return null; }
        public void setLogWriter(java.io.PrintWriter value) {}
        public void setLoginTimeout(int value) {}
        public int getLoginTimeout() { return 0; }
        public java.util.logging.Logger getParentLogger() { return java.util.logging.Logger.getGlobal(); }
        public <T> T unwrap(Class<T> type) throws java.sql.SQLException { throw new java.sql.SQLException("Not a wrapper"); }
        public boolean isWrapperFor(Class<?> type) { return false; }
    }

    private JdbcSqlExecutor fixture() {
        var source = new Source("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        var sql = new JdbcSqlExecutor(source);
        sql.execute("CREATE TABLE completion_test(id INT PRIMARY KEY)");
        return sql;
    }

    @Test public void noActiveTransactionCannotAcceptCallbacks() {
        var sql = fixture();
        assertFalse(sql.registerTransactionSynchronization(() -> fail(), () -> fail()));
        assertFalse(sql.hasActiveTransaction());
    }

    @Test public void nestedCallbacksWaitForPhysicalCommitAndRunOutsideIt() {
        var sql = fixture(); var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            sql.update("INSERT INTO completion_test VALUES(?)", new Object[]{1});
            assertTrue(sql.registerTransactionSynchronization(() -> {
                assertFalse(sql.hasActiveTransaction());
                assertEquals(1, sql.queryForList("SELECT id FROM completion_test", new Object[0]).size());
                observed.add("outer");
            }, () -> fail("unexpected rollback")));
            sql.executeInTransaction(() -> assertTrue(sql.registerTransactionSynchronization(
                    () -> observed.add("inner"), () -> fail("unexpected rollback"))));
            assertTrue(observed.isEmpty());
        });
        assertEquals(List.of("outer", "inner"), observed);
        assertFalse(sql.registerTransactionSynchronization(() -> fail(), () -> fail()));
    }

    @Test public void rollbackCallbacksRunInReverseAndRetainOriginalFailure() {
        var sql = fixture(); var observed = new ArrayList<Integer>();
        var original = new IllegalArgumentException("intentional action failure");
        var actual = assertThrows(IllegalArgumentException.class, () -> sql.executeInTransaction(() -> {
            sql.update("INSERT INTO completion_test VALUES(?)", new Object[]{1});
            sql.registerTransactionSynchronization(() -> fail(), () -> { assertFalse(sql.hasActiveTransaction()); observed.add(1); });
            sql.registerTransactionSynchronization(() -> fail(), () -> { observed.add(2); throw new IllegalStateException("rollback observer"); });
            sql.registerTransactionSynchronization(() -> fail(), () -> observed.add(3));
            throw original;
        }));
        assertSame(original, actual); assertEquals(1, actual.getSuppressed().length);
        assertEquals(List.of(3, 2, 1), observed);
        assertTrue(sql.queryForList("SELECT id FROM completion_test", new Object[0]).isEmpty());
    }

    @Test public void swallowedInnerFailureStillForcesOuterRollback() {
        var sql = fixture(); var observed = new ArrayList<String>();
        var actual = assertThrows(IllegalStateException.class, () -> sql.executeInTransaction(() -> {
            sql.registerTransactionSynchronization(() -> observed.add("commit"), () -> observed.add("rollback"));
            try {
                sql.executeInTransaction(() -> {
                    sql.update("INSERT INTO completion_test VALUES(?)", new Object[]{1});
                    throw new IllegalArgumentException("inner failure");
                });
            } catch (IllegalArgumentException expected) { /* the caller may not make partial DML committable */ }
        }));
        assertTrue(actual.getMessage().contains("rollback-only"));
        assertEquals(List.of("rollback"), observed);
        assertTrue(sql.queryForList("SELECT id FROM completion_test", new Object[0]).isEmpty());
    }

    @Test public void postCommitFailureCannotRollbackCommittedDataOrSkipOtherAudits() {
        var sql = fixture(); var observed = new ArrayList<String>();
        var actual = assertThrows(IllegalStateException.class, () -> sql.executeInTransaction(() -> {
            sql.update("INSERT INTO completion_test VALUES(?)", new Object[]{1});
            sql.registerTransactionSynchronization(() -> { throw new IllegalStateException("audit unavailable"); }, () -> fail("already committed"));
            sql.registerTransactionSynchronization(() -> observed.add("delivered"), () -> fail("already committed"));
        }));
        assertEquals("audit unavailable", actual.getMessage()); assertEquals(List.of("delivered"), observed);
        assertEquals(1, sql.queryForList("SELECT id FROM completion_test", new Object[0]).size());
        assertFalse(sql.hasActiveTransaction());
    }

    @Test public void nextTransactionCannotReuseFailedScopeCallbacks() {
        var sql = fixture(); var observed = new ArrayList<String>();
        assertThrows(AssertionError.class, () -> sql.executeInTransaction(() -> {
            sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("rollback"));
            throw new AssertionError("intentional");
        }));
        sql.executeInTransaction(() -> sql.registerTransactionSynchronization(() -> observed.add("commit"), () -> fail()));
        assertEquals(List.of("rollback", "commit"), observed);
        assertFalse(sql.hasActiveTransaction());
    }
}
