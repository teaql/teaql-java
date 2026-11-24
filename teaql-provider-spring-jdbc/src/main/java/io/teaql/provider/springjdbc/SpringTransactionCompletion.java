package io.teaql.provider.springjdbc;

import io.teaql.core.TransactionOutcomeUnknownException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Provider-only completion bridge; mutation ledgers remain owned by their entity graphs. */
final class SpringTransactionCompletion {
    private final PhysicalTransactionManager manager;
    private final TransactionTemplate transactions;
    private final TransactionTemplate outsideTransaction;

    SpringTransactionCompletion(DataSource source) {
        manager = new PhysicalTransactionManager(source);
        transactions = new TransactionTemplate(manager);
        outsideTransaction = new TransactionTemplate(manager);
        outsideTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    boolean active() { return manager.active(); }

    <T> T withoutActiveTransaction(java.util.function.Supplier<T> work) {
        return outsideTransaction.execute(status -> work.get());
    }

    boolean register(Runnable committed, Runnable rolledBack, Runnable unknown) {
        if (!active()) return false;
        Objects.requireNonNull(committed, "committed");
        Objects.requireNonNull(rolledBack, "rolledBack");
        Objects.requireNonNull(unknown, "unknown");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Never fall back to an immediate audit inside an unsynchronized physical transaction.
            throw new IllegalStateException("Spring transaction synchronization is required for mutation completion");
        }
        Completion completion = currentCompletion();
        if (completion == null) {
            completion = new Completion(manager.getDataSource(), outsideTransaction);
            TransactionSynchronizationManager.registerSynchronization(completion);
        }
        completion.callbacks.add(new Callbacks(committed, rolledBack, unknown));
        return true;
    }

    void execute(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (active() && !TransactionSynchronizationManager.isSynchronizationActive()) {
            // A participating REQUIRED template could otherwise create its own synchronization
            // and publish completion while the unsynchronized physical owner is still pending.
            throw new IllegalStateException("Spring transaction synchronization is required for mutation completion");
        }
        var observed = new ArrayList<Completion>();
        try {
            transactions.executeWithoutResult(status -> {
                try { action.run(); }
                finally {
                    Completion completion = currentCompletion();
                    if (completion != null) observed.add(completion);
                }
            });
        } catch (RuntimeException | Error failure) {
            if (failure instanceof UnconfirmedCompletion unconfirmed)
                throw new TransactionOutcomeUnknownException(unconfirmed.originalError);
            // Spring deliberately logs afterCompletion failures instead of replacing an action's error.
            // For an adapter-owned invocation, retain that failure as a suppressed diagnostic as well.
            for (Completion completion : observed) {
                if (completion.completed && completion.outcome == TransactionSynchronization.STATUS_UNKNOWN) {
                    throw failure instanceof Error error
                            ? new io.teaql.core.TransactionOutcomeUnknownException(error)
                            : new io.teaql.core.TransactionOutcomeUnknownException();
                }
                Throwable callbackFailure = completion.failure;
                if (callbackFailure != null && callbackFailure != failure
                        && java.util.Arrays.stream(failure.getSuppressed()).noneMatch(v -> v == callbackFailure)) {
                    failure.addSuppressed(callbackFailure);
                }
            }
            throw failure;
        }
    }

    private Completion currentCompletion() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return null;
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof Completion completion && completion.source == manager.getDataSource()) {
                return completion;
            }
        }
        return null;
    }

    /** Use Spring's resource-specific transaction check, not merely a thread-wide active flag. */
    @SuppressWarnings("serial")
    private static final class PhysicalTransactionManager extends DataSourceTransactionManager {
        private final ThreadLocal<Set<Object>> unconfirmedRollbacks = new ThreadLocal<>();

        PhysicalTransactionManager(DataSource source) { super(source); }
        boolean active() { return isExistingTransaction(doGetTransaction()); }

        @Override protected void doCommit(DefaultTransactionStatus status) {
            try { super.doCommit(status); }
            catch (RuntimeException | Error failure) {
                // A physical rollback makes disposal safe. It cannot establish
                // whether the earlier commit took effect.
                try { super.doRollback(status); }
                catch (RuntimeException | Error rollbackFailure) { rememberUnconfirmedRollback(status); }
                throw new UnconfirmedCompletion(failure instanceof Error error ? error : null);
            }
        }

        @Override protected void doRollback(DefaultTransactionStatus status) {
            try { super.doRollback(status); }
            catch (RuntimeException | Error failure) {
                rememberUnconfirmedRollback(status);
                throw new UnconfirmedCompletion(failure instanceof Error error ? error : null);
            }
        }

        private void rememberUnconfirmedRollback(DefaultTransactionStatus status) {
            var pending = unconfirmedRollbacks.get();
            if (pending == null) {
                pending = Collections.newSetFromMap(new IdentityHashMap<>());
                unconfirmedRollbacks.set(pending);
            }
            pending.add(status.getTransaction());
        }

        @Override protected void doCleanupAfterCompletion(Object transaction) {
            var pending = unconfirmedRollbacks.get();
            if (pending == null || !pending.remove(transaction)) {
                super.doCleanupAfterCompletion(transaction);
                return;
            }
            if (pending.isEmpty()) unconfirmedRollbacks.remove();
            var source = obtainDataSource();
            if (!(TransactionSynchronizationManager.getResource(source) instanceof ConnectionHolder holder))
                return;
            var connection = holder.getConnection();
            try { connection.rollback(); }
            catch (SQLException | RuntimeException | Error failure) {
                // Spring's normal cleanup restores autoCommit and releases the
                // connection. Both can implicitly commit an active transaction.
                TransactionSynchronizationManager.unbindResourceIfPossible(source);
                holder.clear();
                try { connection.abort(Runnable::run); }
                catch (SQLException | RuntimeException | Error ignored) { }
                return;
            }
            super.doCleanupAfterCompletion(transaction);
        }
    }

    @SuppressWarnings("serial")
    private static final class UnconfirmedCompletion extends TransactionSystemException {
        private final Error originalError;

        UnconfirmedCompletion(Error originalError) {
            super("JDBC transaction outcome did not confirm");
            this.originalError = originalError;
        }
    }

    private record Callbacks(Runnable committed, Runnable rolledBack, Runnable unknown) {}

    private static final class Completion implements TransactionSynchronization {
        private final DataSource source;
        private final TransactionTemplate outsideTransaction;
        private final List<Callbacks> callbacks = new ArrayList<>();
        private final IdentityHashMap<Object, Integer> savepoints = new IdentityHashMap<>();
        private Throwable failure;
        private boolean completed;
        private int outcome;

        Completion(DataSource source, TransactionTemplate outsideTransaction) {
            this.source = source;
            this.outsideTransaction = outsideTransaction;
        }

        @Override public void savepoint(Object savepoint) { savepoints.put(savepoint, callbacks.size()); }

        @Override public void savepointRollback(Object savepoint) {
            // A first registration can occur after the savepoint was created. All callbacks in that
            // newly registered group then belong to the savepoint and must be undone, not committed.
            int start = savepoints.getOrDefault(savepoint, 0);
            var abandoned = new ArrayList<>(callbacks.subList(start, callbacks.size()));
            callbacks.subList(start, callbacks.size()).clear();
            // Equal positions include this savepoint and outer boundaries with no intervening
            // callbacks. Keep them valid for nested and repeated rollbacks to the same boundary.
            savepoints.entrySet().removeIf(entry -> entry.getValue() > start);
            // Spring invokes this hook before the physical savepoint rollback. These framework-only
            // rollback callbacks restore in-memory intent; they must not run new SQL or stop rollback.
            invoke(abandoned, STATUS_ROLLED_BACK);
        }

        @Override public void beforeCommit(boolean readOnly) { rethrow(failure); }

        @Override public void afterCommit() { complete(STATUS_COMMITTED); }

        @Override public void afterCompletion(int status) {
            // An earlier Spring synchronization may have thrown afterCommit. COMMITTED must still
            // deliver our remaining committed audits, never run the rollback restorations instead.
            if (!completed) complete(status);
        }

        private void complete(int status) {
            completed = true;
            outcome = status;
            var selected = new ArrayList<>(callbacks);
            callbacks.clear(); savepoints.clear();
            // Spring still binds the completed connection during these hooks. Suspend it through
            // supported propagation APIs so an observer cannot accidentally reuse uncommittable DML.
            outsideTransaction.executeWithoutResult(ignored -> invoke(selected, status));
            rethrow(failure);
        }

        private void invoke(List<Callbacks> selected, int status) {
            boolean committed = status == STATUS_COMMITTED;
            for (int i = 0; i < selected.size(); i++) {
                Callbacks next = selected.get(committed ? i : selected.size() - i - 1);
                Runnable action = switch (status) {
                    case STATUS_COMMITTED -> next.committed();
                    case STATUS_ROLLED_BACK -> next.rolledBack();
                    default -> next.unknown();
                };
                try { action.run(); }
                catch (RuntimeException | Error error) {
                    if (failure == null) failure = error;
                    else if (failure != error) failure.addSuppressed(error);
                }
            }
        }

        private static void rethrow(Throwable failure) {
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        }
    }
}
