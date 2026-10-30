package io.teaql.provider.jdbc;

import io.teaql.core.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.h2.jdbc.JdbcConnection;
import org.junit.Test;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

/** Real H2 transactions with explicit non-reflective driver-boundary fault injection. */
public class JdbcTransactionCompletionTest {
    @Test public void allStatePreparationFinishesBeforeAnyCallbackCanReenter() throws Exception {
        try (Fixture f=new Fixture()) {
            List<String> events=new ArrayList<>();
            f.sql.executeInTransaction(() -> {
                for (String name:List.of("first","second")) f.sql.afterTransaction(new TransactionCompletion() {
                    @Override public void beforeNotifications(Outcome outcome) { assertEquals(COMMITTED,outcome); f.assertCompleted(); events.add("prepare-"+name); }
                    @Override public void afterCompletion(Outcome outcome) { assertTrue(events.contains("prepare-second")); events.add("deliver-"+name); }
                });
            });
            assertEquals(List.of("prepare-first","prepare-second","deliver-first","deliver-second"),events);
        }
    }

    @Test public void failedPreparationDoesNotSkipOtherStateOrDeliveryAndKeepsCommittedOutcome() throws Exception {
        try (Fixture f=new Fixture()) {
            List<String> events=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.insert(1); f.sql.afterTransaction(new TransactionCompletion() {
                    @Override public void beforeNotifications(Outcome outcome) { events.add("prepare-first"); throw new IllegalArgumentException("test preparation failure"); }
                    @Override public void afterCompletion(Outcome outcome) { events.add("deliver-first"); }
                });
                f.sql.afterTransaction(new TransactionCompletion() {
                    @Override public void beforeNotifications(Outcome outcome) { events.add("prepare-second"); }
                    @Override public void afterCompletion(Outcome outcome) { events.add("deliver-second"); }
                });
            }));
            assertEquals(COMMITTED,failure.outcome()); assertEquals(1,f.count()); assertEquals(List.of("prepare-first","prepare-second","deliver-first","deliver-second"),events);
        }
    }

    @Test public void nestedCompletionWaitsForOuterCommitAndSeesClosedResourcesAndCommittedRows() throws Exception {
        try (Fixture f=new Fixture()) {
            List<String> events=new ArrayList<>(); var service=new SqlDataServiceExecutor("test",f.sql);
            assertTrue(service.supportsTransactionCompletion()); assertSame(f.sql,service.transactionResource());
            assertThrows(IllegalStateException.class,() -> service.afterTransaction(outcome -> fail()));
            f.sql.executeInTransaction(() -> {
                Object identity=f.sql.activeTransactionIdentity(); f.insert(1);
                service.afterTransaction(outcome -> { assertEquals(COMMITTED,outcome); f.assertCompleted(); assertEquals(1,f.count()); events.add("first"); });
                f.sql.executeInTransaction(() -> { assertSame(identity,f.sql.activeTransactionIdentity()); service.afterTransaction(outcome -> events.add("second")); });
                assertTrue(events.isEmpty()); assertEquals(0,f.count()); events.add("body");
            });
            assertEquals(List.of("body","first","second"),events); assertEquals(1,f.count()); f.assertCompleted();
        }
    }

    @Test public void outerFailureRollsBackAllNestedWritesAndNotifiesInReverseOrder() throws Exception {
        try (Fixture f=new Fixture()) {
            List<String> events=new ArrayList<>(); IllegalStateException original=new IllegalStateException("test business failure");
            assertSame(original,assertThrows(IllegalStateException.class,() -> f.sql.executeInTransaction(() -> {
                f.insert(1); f.sql.afterTransaction(outcome -> { assertEquals(ROLLED_BACK,outcome); events.add("first"); });
                f.sql.executeInTransaction(() -> { f.insert(2); f.sql.afterTransaction(outcome -> { f.assertCompleted(); assertEquals(0,f.count()); events.add("second"); }); });
                assertTrue(events.isEmpty()); throw original;
            })));
            assertEquals(List.of("second","first"),events); assertEquals(0,f.count());
        }
    }

    @Test public void swallowedNestedFailureMarksTheWholePhysicalTransactionRollbackOnly() throws Exception {
        try (Fixture f=new Fixture()) {
            List<TransactionCompletion.Outcome> outcomes=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.insert(1); f.sql.afterTransaction(outcomes::add);
                assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> { f.insert(2); throw new IllegalArgumentException("nested test failure"); }));
                f.insert(3); assertEquals(0,f.count());
            }));
            assertEquals(ROLLED_BACK,failure.outcome()); assertEquals(List.of(ROLLED_BACK),outcomes); assertEquals(0,f.count());
        }
    }

    @Test public void failedCommitAcknowledgementIsUnknownWhetherOrNotTheCommitReachedDatabase() throws Exception {
        for (Mode mode:List.of(Mode.BEFORE_COMMIT,Mode.AFTER_COMMIT)) try (Fixture f=new Fixture()) {
            f.next.set(mode); List<TransactionCompletion.Outcome> outcomes=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> { f.insert(1); f.sql.afterTransaction(outcomes::add); }));
            assertEquals(UNKNOWN,failure.outcome()); assertEquals(List.of(UNKNOWN),outcomes);
            assertEquals(mode==Mode.AFTER_COMMIT?1:0,f.count()); assertEquals(1,f.faultConnection.commits); assertEquals(1,f.faultConnection.rollbacks);
            f.assertCompleted();
        }
    }

    @Test public void failedRollbackNeverRestoresAutoCommitAndKeepsOriginalFailureAsCause() throws Exception {
        try (Fixture f=new Fixture()) {
            f.next.set(Mode.ROLLBACK); IllegalStateException original=new IllegalStateException("test body failed");
            List<TransactionCompletion.Outcome> outcomes=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> { f.insert(1); f.sql.afterTransaction(outcomes::add); throw original; }));
            assertEquals(UNKNOWN,failure.outcome()); assertSame(original,failure.getCause()); assertEquals(1,original.getSuppressed().length);
            assertEquals(List.of(UNKNOWN),outcomes); assertEquals(0,f.faultConnection.resets); assertTrue(f.faultConnection.isClosed()); assertEquals(0,f.count());
        }
    }

    @Test public void cleanupFailureAfterCommitStillDeliversCommittedAndCannotRollBack() throws Exception {
        for (Mode mode:List.of(Mode.RESET,Mode.CLOSE)) try (Fixture f=new Fixture()) {
            f.next.set(mode); List<TransactionCompletion.Outcome> outcomes=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> { f.insert(1); f.sql.afterTransaction(outcomes::add); }));
            assertEquals(COMMITTED,failure.outcome()); assertEquals(List.of(COMMITTED),outcomes); assertEquals(1,f.count());
            assertEquals(1,f.faultConnection.commits); assertEquals(0,f.faultConnection.rollbacks); assertTrue(f.faultConnection.isClosed()); f.assertCompleted();
        }
    }

    @Test public void oneFailedCommitNotificationDoesNotSkipRemainingNotificationsOrReplayWrites() throws Exception {
        try (Fixture f=new Fixture()) {
            List<String> events=new ArrayList<>(); IllegalStateException first=new IllegalStateException("first callback");
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.insert(1);
                f.sql.afterTransaction(outcome -> { events.add("first"); throw first; });
                f.sql.afterTransaction(outcome -> { events.add("second"); throw new IllegalArgumentException("second callback"); });
                f.sql.afterTransaction(outcome -> events.add("third"));
            }));
            assertEquals(COMMITTED,failure.outcome()); assertSame(first,failure.getCause()); assertEquals(1,first.getSuppressed().length);
            assertEquals(List.of("first","second","third"),events); assertEquals(1,f.count()); f.assertCompleted();
        }
    }

    @Test public void rollbackNotificationFailurePreservesBusinessFailureAndDrainsRemainingCallbacks() throws Exception {
        try (Fixture f=new Fixture()) {
            List<Integer> order=new ArrayList<>(); IllegalArgumentException original=new IllegalArgumentException("business test error");
            assertSame(original,assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                f.insert(1); f.sql.afterTransaction(outcome -> order.add(1));
                f.sql.afterTransaction(outcome -> { order.add(2); throw new IllegalStateException("rollback callback"); });
                f.sql.afterTransaction(outcome -> order.add(3)); throw original;
            })));
            assertEquals(List.of(3,2,1),order); assertEquals(1,original.getSuppressed().length); assertEquals(0,f.count());
        }
    }

    @Test public void callbackMayStartAnIndependentTransactionAfterOriginalIdentityIsReleased() throws Exception {
        try (Fixture f=new Fixture()) {
            Object[] original={null}; List<TransactionCompletion.Outcome> outcomes=new ArrayList<>();
            f.sql.executeInTransaction(() -> {
                original[0]=f.sql.activeTransactionIdentity(); f.insert(1);
                f.sql.afterTransaction(outcome -> {
                    f.assertCompleted(); assertEquals(COMMITTED,outcome);
                    f.sql.executeInTransaction(() -> { assertNotSame(original[0],f.sql.activeTransactionIdentity()); f.insert(2); f.sql.afterTransaction(outcomes::add); });
                });
            });
            assertEquals(List.of(COMMITTED),outcomes); assertEquals(2,f.count()); f.assertCompleted();
        }
    }

    @Test public void fatalCallbackIsNotHiddenBehindAnEarlierNotificationFailure() throws Exception {
        try (Fixture f=new Fixture()) {
            List<Integer> order=new ArrayList<>(); AssertionError fatal=new AssertionError("Test callback error");
            assertSame(fatal,assertThrows(AssertionError.class,() -> f.sql.executeInTransaction(() -> {
                f.insert(1); f.sql.afterTransaction(outcome -> { order.add(1); throw new IllegalArgumentException("Test callback failure"); });
                f.sql.afterTransaction(outcome -> { order.add(2); throw fatal; });
                f.sql.afterTransaction(outcome -> { assertEquals(COMMITTED,outcome); order.add(3); });
            })));
            assertEquals(List.of(1,2,3),order); assertEquals(1,fatal.getSuppressed().length); assertEquals(1,f.count()); f.assertCompleted();
        }
    }

    @Test public void concurrentTransactionsKeepIdentitiesCallbacksAndRollbackOnlyStateSeparate() throws Exception {
        try (Fixture f=new Fixture()) {
            ExecutorService workers=Executors.newFixedThreadPool(2); CountDownLatch both=new CountDownLatch(2); var identities=ConcurrentHashMap.newKeySet();
            try {
                List<Future<TransactionCompletion.Outcome>> results=new ArrayList<>();
                for (int id=1;id<=2;id++) { int value=id;
                    results.add(workers.submit(() -> {
                        AtomicReference<TransactionCompletion.Outcome> outcome=new AtomicReference<>();
                        try { f.sql.executeInTransaction(() -> {
                            assertTrue(identities.add(f.sql.activeTransactionIdentity())); f.insert(value); f.sql.afterTransaction(outcome::set); both.countDown();
                            try { assertTrue(both.await(10,TimeUnit.SECONDS)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                            assertNull(outcome.get()); if (value==2) throw new IllegalArgumentException("second transaction fails");
                        }); } catch (IllegalArgumentException expected) { assertEquals(2,value); }
                        f.assertCompleted(); return outcome.get();
                    }));
                }
                assertEquals(COMMITTED,results.get(0).get(15,TimeUnit.SECONDS)); assertEquals(ROLLED_BACK,results.get(1).get(15,TimeUnit.SECONDS)); assertEquals(1,f.count());
            } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS)); }
        }
    }

    private enum Mode { NONE, BEFORE_COMMIT, AFTER_COMMIT, ROLLBACK, RESET, CLOSE }
    private static final class FaultConnection extends JdbcConnection {
        final Mode mode; int commits,rollbacks,resets; boolean resetting;
        FaultConnection(String url,Mode mode) throws SQLException { super(url,new Properties(),"sa","",false); this.mode=mode; }
        @Override public void commit() throws SQLException {
            if (resetting) { super.commit(); return; }
            commits++; if (mode==Mode.BEFORE_COMMIT) throw new SQLException("Test commit not sent"); super.commit(); if (mode==Mode.AFTER_COMMIT) throw new SQLException("Test commit response lost");
        }
        @Override public void rollback() throws SQLException { rollbacks++; if (mode==Mode.ROLLBACK) throw new SQLException("Test rollback unavailable"); super.rollback(); }
        @Override public void setAutoCommit(boolean enabled) throws SQLException {
            if (enabled) { resets++; if (mode==Mode.RESET) throw new SQLException("Test reset failed"); }
            resetting=enabled; try { super.setAutoCommit(enabled); } finally { resetting=false; }
        }
        @Override public void close() throws SQLException { super.close(); if (mode==Mode.CLOSE) throw new SQLException("Test close response failed"); }
    }
    private static final class Fixture implements AutoCloseable,DataSource {
        final String url="jdbc:h2:mem:completion_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
        final AtomicReference<Mode> next=new AtomicReference<>(Mode.NONE); volatile FaultConnection faultConnection;
        final JdbcSqlExecutor sql=new JdbcSqlExecutor(this);
        Fixture() { sql.execute("CREATE TABLE entries(id INT PRIMARY KEY)"); }
        void insert(int id) { sql.update("INSERT INTO entries(id) VALUES (?)",new Object[]{id}); }
        int count() {
            try (Connection connection=DriverManager.getConnection(url,"sa",""); Statement statement=connection.createStatement(); ResultSet rows=statement.executeQuery("SELECT count(*) FROM entries")) { assertTrue(rows.next()); return rows.getInt(1); }
            catch (SQLException failure) { throw new IllegalStateException(failure); }
        }
        void assertCompleted() { assertFalse(sql.hasActiveTransaction()); assertNull(sql.activeTransactionIdentity()); }
        @Override public Connection getConnection() throws SQLException { var connection=new FaultConnection(url,next.getAndSet(Mode.NONE)); if (connection.mode!=Mode.NONE) faultConnection=connection; return connection; }
        @Override public Connection getConnection(String user,String password) throws SQLException { throw new SQLFeatureNotSupportedException(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter writer) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getLogger("teaql.transaction.test"); }
        @Override public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLFeatureNotSupportedException(); }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
        @Override public void close() { sql.execute("SHUTDOWN"); }
    }
}
