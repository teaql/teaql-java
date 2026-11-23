package io.teaql.provider.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Completion belongs to the physical owner, never to an inner save call. */
public class JdbcTransactionCompletionTest {
    private record Source(String url, boolean loseCommitAcknowledgement) implements javax.sql.DataSource {
        Source(String url) { this(url, false); }
        public java.sql.Connection getConnection() throws java.sql.SQLException {
            var connection = java.sql.DriverManager.getConnection(url);
            if (!loseCommitAcknowledgement) return connection;
            return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{java.sql.Connection.class},
                    (proxy, method, args) -> {
                        try {
                            Object result = method.invoke(connection, args);
                            if (method.getName().equals("commit")) {
                                throw new java.sql.SQLException("PRIVATE_JDBC_COMMIT_CANARY");
                            }
                            return result;
                        } catch (java.lang.reflect.InvocationTargetException error) {
                            throw error.getCause();
                        }
                    });
        }
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

    @Test public void lostCommitAcknowledgementFencesEveryParticipantWithoutRollbackCallbacks() {
        var source = new Source("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", true);
        var sql = new JdbcSqlExecutor(source);
        sql.execute("CREATE TABLE completion_test(id INT PRIMARY KEY)");
        var observed = new ArrayList<String>();
        var failure = assertThrows(io.teaql.core.TransactionOutcomeUnknownException.class,
                () -> sql.executeInTransaction(() -> {
                    sql.update("INSERT INTO completion_test VALUES(?)", new Object[]{1});
                    assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> fail(), () -> {
                        assertFalse(sql.hasActiveTransaction());
                        observed.add("outer");
                    }));
                    sql.executeInTransaction(() -> assertTrue(sql.registerTransactionSynchronization(
                            () -> fail(), () -> fail(), () -> observed.add("inner"))));
                }));
        assertFalse(failure.getMessage().contains("PRIVATE_JDBC_COMMIT_CANARY"));
        assertEquals(List.of("inner", "outer"), observed);
        assertEquals(1, sql.queryForList("SELECT id FROM completion_test", new Object[0]).size());
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
