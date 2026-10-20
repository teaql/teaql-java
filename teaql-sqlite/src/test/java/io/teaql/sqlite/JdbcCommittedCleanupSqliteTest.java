package io.teaql.sqlite;

import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** A committed JDBC write must not look like a rolled-back, retryable save. */
public class JdbcCommittedCleanupSqliteTest {
    private static final String DIAGNOSTIC_CANARY = "POST-COMMIT-CLEANUP-SECRET-CANARY";
    private enum Fault { AUTO_COMMIT_RESTORE, CLOSE, AUTO_COMMIT_RESTORE_ERROR, CLOSE_ERROR }

    private static final class NoSuppressionError extends Error {
        private NoSuppressionError() {
            super(DIAGNOSTIC_CANARY, null, false, false);
        }
    }

    @Test public void failedAutoCommitRestoreAfterCommitKeepsCommittedOutcome() throws Exception {
        verifyCommittedCleanupFailure(Fault.AUTO_COMMIT_RESTORE);
    }

    @Test public void failedCloseAfterCommitKeepsCommittedOutcome() throws Exception {
        verifyCommittedCleanupFailure(Fault.CLOSE);
    }

    @Test public void failedWarningHandlerAfterCommitKeepsCommittedOutcome() throws Exception {
        verifyCommittedCleanupFailure(Fault.AUTO_COMMIT_RESTORE, true);
    }

    @Test public void autoCommitRestoreErrorAfterCommitKeepsCommittedOutcome() throws Exception {
        verifyCommittedCleanupFailure(Fault.AUTO_COMMIT_RESTORE_ERROR);
    }

    @Test public void closeErrorAfterCommitKeepsCommittedOutcome() throws Exception {
        verifyCommittedCleanupFailure(Fault.CLOSE_ERROR);
    }

    @Test public void warningHandlerErrorAfterCommitKeepsCommittedOutcome() throws Exception {
        verifyCommittedCleanupFailure(Fault.AUTO_COMMIT_RESTORE, false, true);
    }

    private static void verifyCommittedCleanupFailure(Fault fault) throws Exception {
        verifyCommittedCleanupFailure(fault, false);
    }

    private static void verifyCommittedCleanupFailure(Fault fault, boolean warningHandlerFails)
            throws Exception {
        verifyCommittedCleanupFailure(fault, warningHandlerFails, false);
    }

    private static void verifyCommittedCleanupFailure(Fault fault, boolean warningHandlerFails,
            boolean warningHandlerThrowsError) throws Exception {
        var backing = new SQLiteDataSource();
        backing.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-committed-cleanup-", ".db"));
        try (var connection = backing.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE committed_cleanup_probe (id INTEGER PRIMARY KEY)");
        }
        var committed = new AtomicBoolean();
        DataSource injecting = (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class},
                (proxy, method, args) -> {
                    Object value = invoke(backing, method, args);
                    if (!method.getName().equals("getConnection")) return value;
                    Connection actual = (Connection) value;
                    return Proxy.newProxyInstance(Connection.class.getClassLoader(),
                            new Class<?>[] {Connection.class}, (connectionProxy, operation, parameters) -> {
                                if (operation.getName().equals("setAutoCommit")
                                        && Boolean.TRUE.equals(parameters[0]) && committed.get()
                                        && (fault == Fault.AUTO_COMMIT_RESTORE
                                        || fault == Fault.AUTO_COMMIT_RESTORE_ERROR)) {
                                    if (fault == Fault.AUTO_COMMIT_RESTORE_ERROR) throw new NoSuppressionError();
                                    throw new SQLException(DIAGNOSTIC_CANARY);
                                }
                                Object result = invoke(actual, operation, parameters);
                                if (operation.getName().equals("commit")) committed.set(true);
                                if (operation.getName().equals("close") && committed.get()
                                        && (fault == Fault.CLOSE || fault == Fault.CLOSE_ERROR)) {
                                    if (fault == Fault.CLOSE_ERROR) throw new NoSuppressionError();
                                    throw new SQLException(DIAGNOSTIC_CANARY);
                                }
                                return result;
                            });
                });
        var executor = new JdbcSqlExecutor(injecting);
        List<String> warnings = new ArrayList<>();
        Logger logger = Logger.getLogger(JdbcSqlExecutor.class.getName());
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) {
                warnings.add(record.getMessage());
                if (warningHandlerThrowsError) throw new NoSuppressionError();
                if (warningHandlerFails) throw new IllegalStateException(DIAGNOSTIC_CANARY);
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(capture);

        Throwable reported = null;
        try {
            executor.executeInTransaction(() -> executor.update(
                    "INSERT INTO committed_cleanup_probe (id) VALUES (?)", new Object[] {1L}));
        } catch (RuntimeException | Error failure) {
            reported = failure;
        } finally {
            logger.removeHandler(capture);
        }

        assertTrue("the injected failure must occur after the actual commit", committed.get());
        try (var connection = backing.getConnection();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM committed_cleanup_probe");
             var rows = statement.executeQuery()) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt(1));
        }
        assertTrue("cleanup failure needs an operational warning", warnings.stream()
                .anyMatch(message -> message.contains("JDBC transaction committed")));
        assertTrue("the warning must not reveal the JDBC exception", warnings.stream()
                .noneMatch(message -> message.contains(DIAGNOSTIC_CANARY)));
        if (reported != null) {
            throw new AssertionError("committed row was reported as a failed transaction", reported);
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
