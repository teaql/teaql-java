package io.teaql.provider.springjdbc;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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

    boolean register(Runnable committed, Runnable rolledBack) {
        if (!active()) return false;
        Objects.requireNonNull(committed, "committed");
        Objects.requireNonNull(rolledBack, "rolledBack");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Never fall back to an immediate audit inside an unsynchronized physical transaction.
            throw new IllegalStateException("Spring transaction synchronization is required for mutation completion");
        }
        Completion completion = currentCompletion();
        if (completion == null) {
            completion = new Completion(manager.getDataSource(), outsideTransaction);
            TransactionSynchronizationManager.registerSynchronization(completion);
        }
        completion.callbacks.add(new Callbacks(committed, rolledBack));
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
            // Spring deliberately logs afterCompletion failures instead of replacing an action's error.
            // For an adapter-owned invocation, retain that failure as a suppressed diagnostic as well.
            for (Completion completion : observed) {
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
        PhysicalTransactionManager(DataSource source) { super(source); }
        boolean active() { return isExistingTransaction(doGetTransaction()); }
    }

    private record Callbacks(Runnable committed, Runnable rolledBack) {}

    private static final class Completion implements TransactionSynchronization {
        private final DataSource source;
        private final TransactionTemplate outsideTransaction;
        private final List<Callbacks> callbacks = new ArrayList<>();
        private final IdentityHashMap<Object, Integer> savepoints = new IdentityHashMap<>();
        private Throwable failure;
        private boolean completed;

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
            invoke(abandoned, false);
        }

        @Override public void beforeCommit(boolean readOnly) { rethrow(failure); }

        @Override public void afterCommit() { complete(true); }

        @Override public void afterCompletion(int status) {
            // An earlier Spring synchronization may have thrown afterCommit. COMMITTED must still
            // deliver our remaining committed audits, never run the rollback restorations instead.
            if (!completed) complete(status == STATUS_COMMITTED);
        }

        private void complete(boolean committed) {
            completed = true;
            var selected = new ArrayList<>(callbacks);
            callbacks.clear(); savepoints.clear();
            // Spring still binds the completed connection during these hooks. Suspend it through
            // supported propagation APIs so an observer cannot accidentally reuse uncommittable DML.
            outsideTransaction.executeWithoutResult(status -> invoke(selected, committed));
            rethrow(failure);
        }

        private void invoke(List<Callbacks> selected, boolean committed) {
            for (int i = 0; i < selected.size(); i++) {
                Callbacks next = selected.get(committed ? i : selected.size() - i - 1);
                try { (committed ? next.committed() : next.rolledBack()).run(); }
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
