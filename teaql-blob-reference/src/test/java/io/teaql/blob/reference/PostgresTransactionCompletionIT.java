package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import java.util.*;
import org.junit.Test;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

/** Actual PostgreSQL physical-completion substrate; runtime ledger wiring is a separate gate. */
public class PostgresTransactionCompletionIT {
    @Test public void nestedSqlServiceAndCatalogNotifyOnlyAfterCallerOwnedPhysicalCommit() throws Exception {
        try (Fixture f=new Fixture()) {
            var service=new SqlDataServiceExecutor("default",f.sql); List<String> events=new ArrayList<>();
            f.sql.executeInTransaction(() -> {
                Object identity=f.sql.activeTransactionIdentity(); long transaction=f.scalar("SELECT txid_current()");
                service.executeInTransaction(f.context(),() -> {
                    f.catalog.begin(f.draft("transaction-completion","a".repeat(64)),NOW);
                    assertSame(identity,f.sql.activeTransactionIdentity()); assertEquals(transaction,f.scalar("SELECT txid_current()"));
                    service.afterTransaction(outcome -> { assertEquals(COMMITTED,outcome); assertFalse(f.sql.hasActiveTransaction()); assertNull(f.sql.activeTransactionIdentity()); assertEquals(1,count(f,"teaql_blob_object")); events.add("catalog"); });
                    return null;
                });
                assertTrue(events.isEmpty()); assertEquals(0,count(f,"teaql_blob_object"));
                f.sql.update("INSERT INTO asset_data(id,version,title) VALUES (42,1,'outer')",new Object[0]);
                f.sql.afterTransaction(outcome -> { assertEquals(COMMITTED,outcome); assertEquals(1,count(f,"asset_data")); events.add("business"); });
            });
            assertEquals(List.of("catalog","business"),events);
        }
    }

    @Test public void actualDeferredConstraintFailureCannotDeliverCommittedOrLeaveRows() throws Exception {
        try (Fixture f=new Fixture()) {
            f.sql.execute("CREATE TABLE tx_parent(id BIGINT PRIMARY KEY)");
            f.sql.execute("CREATE TABLE tx_child(id BIGINT PRIMARY KEY,parent_id BIGINT REFERENCES tx_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            List<TransactionCompletion.Outcome> outcomes=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.catalog.begin(f.draft("commit-fails","a".repeat(64)),NOW);
                f.sql.executeInTransaction(() -> f.sql.update("INSERT INTO tx_child VALUES (1,999)",new Object[0]));
                f.sql.afterTransaction(outcomes::add); assertEquals(1,f.scalar("SELECT count(*) FROM tx_child")); assertTrue(outcomes.isEmpty());
            }));
            // Generic JDBC cannot infer a durable outcome from arbitrary commit SQLExceptions.
            assertEquals(UNKNOWN,failure.outcome()); assertEquals(List.of(UNKNOWN),outcomes);
            assertEquals(0,count(f,"tx_child")); assertEquals(0,count(f,"teaql_blob_object"));
            assertFalse(f.sql.hasActiveTransaction()); assertNull(f.sql.activeTransactionIdentity());
        }
    }

    @Test public void caughtNestedFailureStillRollsBackBusinessAndCatalogAndReversesCallbacks() throws Exception {
        try (Fixture f=new Fixture()) {
            List<Integer> order=new ArrayList<>();
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.catalog.begin(f.draft("rollback-only","a".repeat(64)),NOW);
                f.sql.afterTransaction(outcome -> { assertEquals(ROLLED_BACK,outcome); order.add(1); });
                assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                    f.sql.update("INSERT INTO asset_data(id,version,title) VALUES (42,1,'nested')",new Object[0]);
                    f.sql.afterTransaction(outcome -> { assertEquals(ROLLED_BACK,outcome); order.add(2); });
                    throw new IllegalArgumentException("Test nested failure");
                }));
                assertTrue(order.isEmpty());
            }));
            assertEquals(ROLLED_BACK,failure.outcome()); assertEquals(List.of(2,1),order);
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_object"));
        }
    }

    @Test public void failedNotificationAfterActualCommitDrainsAllCallbacksAndPreservesRows() throws Exception {
        try (Fixture f=new Fixture()) {
            List<String> events=new ArrayList<>(); IllegalStateException original=new IllegalStateException("Test notification failure");
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.catalog.begin(f.draft("committed-delivery-failure","a".repeat(64)),NOW);
                f.sql.update("INSERT INTO asset_data(id,version,title) VALUES (42,1,'committed')",new Object[0]);
                f.sql.afterTransaction(outcome -> { assertEquals(COMMITTED,outcome); assertFalse(f.sql.hasActiveTransaction()); events.add("first"); throw original; });
                f.sql.afterTransaction(outcome -> { assertEquals(COMMITTED,outcome); assertEquals(1,count(f,"teaql_blob_object")); assertEquals(1,count(f,"asset_data")); events.add("second"); });
            }));
            assertEquals(COMMITTED,failure.outcome()); assertSame(original,failure.getCause());
            assertEquals(List.of("first","second"),events); assertEquals(1,count(f,"teaql_blob_object")); assertEquals(1,count(f,"asset_data"));
        }
    }

    private static int count(Fixture fixture,String table) {
        return ((Number)fixture.otherSql.queryForList("SELECT count(*) FROM "+table,new Object[0]).get(0).values().iterator().next()).intValue();
    }
}
