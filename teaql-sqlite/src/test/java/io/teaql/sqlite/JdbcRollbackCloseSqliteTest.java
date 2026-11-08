package io.teaql.sqlite;

import io.teaql.core.TransactionOutcomeUnknownException;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** An unconfirmed rollback must not be followed by a close that can commit pending writes. */
public class JdbcRollbackCloseSqliteTest {
    private enum AbortBehavior { CLOSE, THROW, NO_OP }

    @Test public void failedRollbackDoesNotCommitPendingWriteOnClose() throws Exception {
        verifyFailedRollback(AbortBehavior.CLOSE);
    }

    @Test public void failedAbortDoesNotFallBackToUnsafeClose() throws Exception {
        verifyFailedRollback(AbortBehavior.THROW);
    }

    @Test public void noopAbortDoesNotFallBackToUnsafeClose() throws Exception {
        verifyFailedRollback(AbortBehavior.NO_OP);
    }

    @Test public void confirmedRollbackRetryAllowsSafeClose() throws Exception {
        var backing = new SQLiteDataSource();
        backing.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-rollback-retry-", ".db"));
        try (var connection = backing.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE rollback_close_probe (id INTEGER PRIMARY KEY)");
        }

        var rollbackAttempts = new AtomicInteger();
        var rollbackConfirmed = new AtomicBoolean();
        var closedAfterRollback = new AtomicBoolean();
        var committedOnUnsafeClose = new AtomicBoolean();
        var aborted = new AtomicBoolean();
        var opened = new AtomicReference<Connection>();
        DataSource injecting = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class},
                (proxy, method, args) -> {
                    Object value = invoke(backing, method, args);
                    if (!method.getName().equals("getConnection")) return value;
                    Connection actual = (Connection) value;
                    opened.set(actual);
                    return Proxy.newProxyInstance(Connection.class.getClassLoader(),
                            new Class<?>[] {Connection.class}, (connectionProxy, operation, parameters) -> {
                                if (operation.getName().equals("rollback")) {
                                    if (rollbackAttempts.incrementAndGet() == 1) {
                                        throw new SQLException("one-shot injected rollback failure");
                                    }
                                    Object result = invoke(actual, operation, parameters);
                                    rollbackConfirmed.set(true);
                                    return result;
                                }
                                if (operation.getName().equals("close")) {
                                    if (rollbackConfirmed.get()) {
                                        closedAfterRollback.set(true);
                                    } else {
                                        // Model a pool that commits a still-active transaction on close.
                                        actual.commit();
                                        committedOnUnsafeClose.set(true);
                                    }
                                }
                                if (operation.getName().equals("abort")) aborted.set(true);
                                return invoke(actual, operation, parameters);
                            });
                });
        var executor = new JdbcSqlExecutor(injecting);

        try {
            assertThrows(TransactionOutcomeUnknownException.class, () ->
                    executor.executeInTransaction(() -> {
                        executor.update("INSERT INTO rollback_close_probe (id) VALUES (?)", new Object[] {1L});
                        throw new IllegalStateException("abort this transaction");
                    }));
            assertEquals("rollback must be retried exactly once", 2, rollbackAttempts.get());
            assertTrue("the retry must confirm rollback", rollbackConfirmed.get());
            assertTrue("close is safe after confirmed rollback", closedAfterRollback.get());
            assertFalse("abort is unnecessary after confirmed rollback", aborted.get());
            assertFalse("close must not commit pending writes", committedOnUnsafeClose.get());
        } finally {
            Connection actual = opened.get();
            if (actual != null && !actual.isClosed()) {
                actual.rollback();
                actual.close();
            }
        }
        try (var connection = backing.getConnection();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM rollback_close_probe");
             var rows = statement.executeQuery()) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
        }
    }

    private static void verifyFailedRollback(AbortBehavior abortBehavior) throws Exception {
        var backing = new SQLiteDataSource();
        backing.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-rollback-close-", ".db"));
        try (var connection = backing.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE rollback_close_probe (id INTEGER PRIMARY KEY)");
        }

        var rollbackFailed = new AtomicBoolean();
        var committedOnClose = new AtomicBoolean();
        var aborted = new AtomicBoolean();
        var opened = new AtomicReference<Connection>();
        DataSource injecting = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class},
                (proxy, method, args) -> {
                    Object value = invoke(backing, method, args);
                    if (!method.getName().equals("getConnection")) return value;
                    Connection actual = (Connection) value;
                    opened.set(actual);
                    return Proxy.newProxyInstance(Connection.class.getClassLoader(),
                            new Class<?>[] {Connection.class}, (connectionProxy, operation, parameters) -> {
                                if (operation.getName().equals("rollback")) {
                                    rollbackFailed.set(true);
                                    throw new SQLException("injected rollback failure");
                                }
                                if (operation.getName().equals("abort")) {
                                    aborted.set(true);
                                    if (abortBehavior == AbortBehavior.THROW) {
                                        throw new SQLException("injected abort failure");
                                    }
                                    if (abortBehavior == AbortBehavior.NO_OP) return null;
                                    actual.rollback();
                                    actual.close();
                                    return null;
                                }
                                if (operation.getName().equals("close") && rollbackFailed.get()) {
                                    // JDBC leaves active-transaction close behavior implementation-defined.
                                    // Model a driver/pool that commits when close returns the connection.
                                    actual.commit();
                                    committedOnClose.set(true);
                                }
                                return invoke(actual, operation, parameters);
                            });
                });
        var executor = new JdbcSqlExecutor(injecting);

        try {
            assertThrows(TransactionOutcomeUnknownException.class, () ->
                    executor.executeInTransaction(() -> {
                        executor.update("INSERT INTO rollback_close_probe (id) VALUES (?)", new Object[] {1L});
                        throw new IllegalStateException("abort this transaction");
                    }));

            assertTrue("the rollback failure must be injected", rollbackFailed.get());
            assertFalse("an unsafe close must not commit pending writes", committedOnClose.get());
            assertTrue("unconfirmed transactions must invalidate the connection", aborted.get());
        } finally {
            // A failed/no-op abort leaves the backing connection open; release it
            // explicitly so the test does not strand SQLite's write lock.
            Connection actual = opened.get();
            if (actual != null && !actual.isClosed()) {
                actual.rollback();
                actual.close();
            }
        }
        try (var connection = backing.getConnection();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM rollback_close_probe");
             var rows = statement.executeQuery()) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
        }
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }
}
