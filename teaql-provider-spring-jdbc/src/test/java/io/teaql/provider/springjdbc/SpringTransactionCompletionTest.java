package io.teaql.provider.springjdbc;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.Assert.*;

/** Completion belongs to the physical Spring owner, including an application-owned transaction. */
public class SpringTransactionCompletionTest {
    private EmbeddedDatabase source;
    private SpringJdbcSqlExecutor sql;

    @Before public void prepare() {
        source = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        sql = new SpringJdbcSqlExecutor(new NamedParameterJdbcTemplate(source));
        sql.execute("CREATE TABLE completion_test(id INT PRIMARY KEY)");
    }

    @After public void close() { if (source != null) source.shutdown(); }

    private TransactionTemplate external(int propagation) {
        var template = new TransactionTemplate(new DataSourceTransactionManager(source));
        template.setPropagationBehavior(propagation);
        return template;
    }

    private void insert(int id) { sql.update("INSERT INTO completion_test VALUES(?)", new Object[]{id}); }

    private int physicalCount() {
        try (var connection = source.getConnection(); var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT COUNT(*) FROM completion_test")) {
            assertTrue(connection.getAutoCommit()); assertTrue(rows.next()); return rows.getInt(1);
        } catch (java.sql.SQLException error) { throw new AssertionError(error); }
    }

    private void outside() {
        assertFalse(sql.hasActiveTransaction());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    @Test public void noActiveTransactionCannotAcceptCallbacks() {
        assertFalse(sql.hasActiveTransaction());
        assertFalse(sql.registerTransactionSynchronization(() -> fail(), () -> fail()));
    }

    @Test public void nestedCallbacksWaitForPhysicalCommitAndRunOutsideIt() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            assertTrue(sql.hasActiveTransaction()); insert(1);
            assertTrue(sql.registerTransactionSynchronization(() -> { outside(); assertEquals(1, physicalCount()); observed.add("outer"); }, () -> fail()));
            sql.executeInTransaction(() -> assertTrue(sql.registerTransactionSynchronization(() -> observed.add("inner"), () -> fail())));
            assertTrue(observed.isEmpty());
        });
        assertEquals(List.of("outer", "inner"), observed); outside();
    }

    @Test public void applicationOwnedCommitDoesNotDeliverAtInnerSaveReturn() {
        var observed = new ArrayList<String>();
        external(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            sql.executeInTransaction(() -> {
                insert(1); assertTrue(sql.registerTransactionSynchronization(() -> { outside(); assertEquals(1, physicalCount()); observed.add("commit"); }, () -> fail()));
            });
            assertTrue(observed.isEmpty()); assertEquals(0, physicalCount());
        });
        assertEquals(List.of("commit"), observed); outside();
    }

    @Test public void rollbackRunsInReverseAndPreservesTheOriginalFailure() {
        var observed = new ArrayList<Integer>(); var original = new IllegalArgumentException("intentional action failure");
        var actual = assertThrows(IllegalArgumentException.class, () -> sql.executeInTransaction(() -> {
            insert(1);
            assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> { outside(); observed.add(1); }));
            assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> { observed.add(2); throw new IllegalStateException("rollback observer"); }));
            assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add(3)));
            throw original;
        }));
        assertSame(original, actual); assertEquals(1, actual.getSuppressed().length);
        assertEquals(List.of(3, 2, 1), observed); assertEquals(0, physicalCount()); outside();
    }

    @Test public void applicationOwnedRollbackRestoresOnlyAtOuterCompletion() {
        var observed = new ArrayList<String>(); var original = new IllegalArgumentException("outer decision");
        assertSame(original, assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            sql.executeInTransaction(() -> {
                insert(1); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> { outside(); assertEquals(0, physicalCount()); observed.add("rollback"); }));
            });
            assertTrue(observed.isEmpty()); throw original;
        })));
        assertEquals(List.of("rollback"), observed); outside();
    }

    @Test public void swallowedInnerFailureStillForcesOuterRollback() {
        var observed = new ArrayList<String>();
        assertThrows(UnexpectedRollbackException.class, () -> sql.executeInTransaction(() -> {
            assertTrue(sql.registerTransactionSynchronization(() -> observed.add("commit"), () -> observed.add("rollback")));
            try { sql.executeInTransaction(() -> { insert(1); throw new IllegalArgumentException("inner failure"); }); }
            catch (IllegalArgumentException expected) { /* REQUIRED does not make partial DML committable. */ }
        }));
        assertEquals(List.of("rollback"), observed); assertEquals(0, physicalCount()); outside();
    }

    @Test public void postCommitFailureCannotRollbackDataOrSkipOtherAudits() {
        var observed = new ArrayList<String>(); var original = new IllegalStateException("audit unavailable");
        assertSame(original, assertThrows(IllegalStateException.class, () -> sql.executeInTransaction(() -> {
            insert(1);
            assertTrue(sql.registerTransactionSynchronization(() -> { outside(); throw original; }, () -> fail("already committed")));
            assertTrue(sql.registerTransactionSynchronization(() -> observed.add("delivered"), () -> fail("already committed")));
        })));
        assertEquals(List.of("delivered"), observed); assertEquals(1, physicalCount()); outside();
    }

    @Test public void assertionRollbackDoesNotLeakCallbacksIntoTheNextTransaction() {
        var observed = new ArrayList<String>();
        assertThrows(AssertionError.class, () -> sql.executeInTransaction(() -> {
            assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("rollback"))); throw new AssertionError("action");
        }));
        sql.executeInTransaction(() -> assertTrue(sql.registerTransactionSynchronization(() -> observed.add("commit"), () -> fail())));
        assertEquals(List.of("rollback", "commit"), observed); outside();
    }

    @Test public void commitObserverCanBeginAFreshIndependentTransaction() {
        sql.executeInTransaction(() -> {
            insert(1);
            assertTrue(sql.registerTransactionSynchronization(() -> {
                outside(); sql.executeInTransaction(() -> insert(2));
            }, () -> fail()));
        });
        assertEquals(2, physicalCount()); outside();
    }

    @Test public void requiresNewKeepsPhysicalOwnersIndependent() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> observed.add("outer"), () -> fail()));
            external(TransactionDefinition.PROPAGATION_REQUIRES_NEW).executeWithoutResult(status -> {
                insert(2); assertTrue(sql.registerTransactionSynchronization(() -> { outside(); assertEquals(1, physicalCount()); observed.add("inner"); }, () -> fail()));
            });
            assertEquals(List.of("inner"), observed);
        });
        assertEquals(List.of("inner", "outer"), observed); assertEquals(2, physicalCount()); outside();
    }

    @Test public void anotherDataSourceTransactionCannotOwnTheseCallbacks() {
        var other = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        try {
            new TransactionTemplate(new DataSourceTransactionManager(other)).executeWithoutResult(status -> {
                // A read can bind a nontransactional connection to this unrelated synchronization.
                assertTrue(sql.queryForList("SELECT * FROM completion_test", new Object[0]).isEmpty());
                assertFalse(sql.hasActiveTransaction()); assertFalse(sql.registerTransactionSynchronization(() -> fail(), () -> fail()));
            });
        } finally { other.shutdown(); }
        outside();
    }

    @Test public void emptySynchronizationIsNotAPhysicalTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        try { assertFalse(sql.hasActiveTransaction()); assertFalse(sql.registerTransactionSynchronization(() -> fail(), () -> fail())); }
        finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test public void savepointRollbackRemovesOnlyNestedAudits() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> observed.add("outer"), () -> observed.add("outer rollback")));
            assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(status -> {
                insert(2); assertTrue(sql.registerTransactionSynchronization(() -> fail("nested SQL rolled back"), () -> observed.add("nested rollback")));
                throw new IllegalArgumentException("nested failure");
            }));
            assertEquals(List.of("nested rollback"), observed);
        });
        assertEquals(List.of("nested rollback", "outer"), observed); assertEquals(1, physicalCount()); outside();
    }

    @Test public void firstRegistrationInsideAnExistingSavepointStillRollsBack() {
        var observed = new ArrayList<String>();
        external(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(nested -> {
                sql.executeInTransaction(() -> { insert(1); assertTrue(sql.registerTransactionSynchronization(() -> fail("rolled back savepoint"), () -> observed.add("rollback"))); });
                throw new IllegalArgumentException("nested failure");
            }));
            assertEquals(List.of("rollback"), observed);
        });
        assertEquals(List.of("rollback"), observed); assertEquals(0, physicalCount()); outside();
    }

    @Test public void savepointRetryCannotDeliverTheAbandonedAudit() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(status -> {
                insert(1); assertTrue(sql.registerTransactionSynchronization(() -> fail("abandoned"), () -> observed.add("restored")));
                throw new IllegalArgumentException("retry");
            }));
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> observed.add("retry commit"), () -> fail()));
        });
        assertEquals(List.of("restored", "retry commit"), observed); assertEquals(1, physicalCount()); outside();
    }

    @Test public void adaptersSharingOneDataSourceSharePhysicalCallbackOrder() {
        var other = new SpringJdbcSqlExecutor(new NamedParameterJdbcTemplate(source)); var observed = new ArrayList<Integer>();
        assertThrows(IllegalArgumentException.class, () -> sql.executeInTransaction(() -> {
            assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add(1)));
            assertTrue(other.registerTransactionSynchronization(() -> fail(), () -> observed.add(2)));
            assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add(3)));
            throw new IllegalArgumentException("rollback both adapters");
        }));
        assertEquals(List.of(3, 2, 1), observed); outside();
    }

    @Test public void nullCallbackCannotPartiallyRegister() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            assertThrows(NullPointerException.class, () -> sql.registerTransactionSynchronization(() -> fail(), null));
            assertTrue(sql.registerTransactionSynchronization(() -> observed.add("valid"), () -> fail()));
        });
        assertEquals(List.of("valid"), observed); outside();
    }

    @Test public void suspendedTransactionCannotCaptureUnrelatedCallbacks() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            assertTrue(sql.registerTransactionSynchronization(() -> observed.add("outer"), () -> fail()));
            external(TransactionDefinition.PROPAGATION_NOT_SUPPORTED).executeWithoutResult(status -> {
                outside(); assertFalse(sql.registerTransactionSynchronization(() -> fail(), () -> fail()));
            });
        });
        assertEquals(List.of("outer"), observed); outside();
    }

    @Test public void synchronizationDisabledOuterRejectsBeforeAction() {
        var manager = new DataSourceTransactionManager(source);
        manager.setTransactionSynchronization(org.springframework.transaction.support.AbstractPlatformTransactionManager.SYNCHRONIZATION_NEVER);
        var called = new java.util.concurrent.atomic.AtomicBoolean();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            assertThrows(IllegalStateException.class, () -> sql.executeInTransaction(() -> {
                called.set(true); insert(1);
                assertTrue(sql.registerTransactionSynchronization(() -> fail("outer has not committed"), () -> fail()));
            }));
            assertFalse(called.get());
        });
        assertEquals(0, physicalCount()); outside();
    }

    @Test public void earlierSynchronizationFailureStillDeliversCommittedAudits() {
        var observed = new ArrayList<String>(); var original = new IllegalStateException("earlier completion failed");
        assertSame(original, assertThrows(IllegalStateException.class, () -> sql.executeInTransaction(() -> {
            insert(1);
            TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() { throw original; }
            });
            assertTrue(sql.registerTransactionSynchronization(() -> { outside(); observed.add("committed"); }, () -> fail("already committed")));
        })));
        assertEquals(List.of("committed"), observed); assertEquals(1, physicalCount()); outside();
    }

    @Test public void savepointRestorationFailureCannotStopRollbackOrPermitCommit() {
        var observed = new ArrayList<String>(); var failure = new IllegalStateException("restoration failed");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> sql.executeInTransaction(() -> {
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> fail("restoration failed"), () -> observed.add("outer rollback")));
            assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(status -> {
                insert(2); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> { observed.add("nested rollback"); throw failure; }));
                throw new IllegalArgumentException("nested failure");
            }));
            assertEquals(1, sql.queryForList("SELECT * FROM completion_test", new Object[0]).size());
            assertEquals(List.of("nested rollback"), observed);
        })));
        assertEquals(List.of("nested rollback", "outer rollback"), observed); assertEquals(0, physicalCount()); outside();
    }

    @Test public void doublyNestedSavepointsRetainOnlyOuterAudit() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> observed.add("outer commit"), () -> fail()));
            assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(first -> {
                insert(2); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("first rollback")));
                assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(second -> {
                    insert(3); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("second rollback")));
                    throw new IllegalArgumentException("second");
                }));
                throw new IllegalArgumentException("first");
            }));
            assertEquals(List.of("second rollback", "first rollback"), observed);
        });
        assertEquals(List.of("second rollback", "first rollback", "outer commit"), observed); assertEquals(1, physicalCount()); outside();
    }

    @Test public void equalPositionNestedSavepointsCannotUndoOuterCallbacks() {
        var observed = new ArrayList<String>();
        sql.executeInTransaction(() -> {
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> observed.add("outer commit"), () -> observed.add("outer rollback")));
            assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(first -> {
                // There is no callback between these two savepoint creations: both boundaries are1.
                assertThrows(IllegalArgumentException.class, () -> external(TransactionDefinition.PROPAGATION_NESTED).executeWithoutResult(second -> {
                    insert(2); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("second rollback")));
                    throw new IllegalArgumentException("second");
                }));
                insert(3); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("first rollback")));
                throw new IllegalArgumentException("first");
            }));
            assertEquals(List.of("second rollback", "first rollback"), observed);
        });
        assertEquals(List.of("second rollback", "first rollback", "outer commit"), observed); assertEquals(1, physicalCount()); outside();
    }

    @Test public void repeatedRollbackToSameSavepointRetainsPrecedingCallbacks() {
        var observed = new ArrayList<String>();
        external(TransactionDefinition.PROPAGATION_REQUIRED).executeWithoutResult(status -> {
            insert(1); assertTrue(sql.registerTransactionSynchronization(() -> observed.add("outer commit"), () -> observed.add("outer rollback")));
            var savepoint = status.createSavepoint();
            insert(2); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("first rollback")));
            status.rollbackToSavepoint(savepoint);
            insert(3); assertTrue(sql.registerTransactionSynchronization(() -> fail(), () -> observed.add("second rollback")));
            status.rollbackToSavepoint(savepoint);
            status.releaseSavepoint(savepoint);
            assertEquals(List.of("first rollback", "second rollback"), observed);
        });
        assertEquals(List.of("first rollback", "second rollback", "outer commit"), observed); assertEquals(1, physicalCount()); outside();
    }
}
