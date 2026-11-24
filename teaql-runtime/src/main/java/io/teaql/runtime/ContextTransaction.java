package io.teaql.runtime;

import io.teaql.core.BaseEntity;
import io.teaql.core.EntityMutationLedger;
import io.teaql.core.TransactionExecutor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class ContextTransaction {
    private final Thread owner = Thread.currentThread();
    private final TransactionExecutor executor;
    private final Set<BaseEntity> attempted = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<EntityMutationLedger> ledgers = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean active = true;
    private boolean committed;
    private Throwable failure;

    ContextTransaction(TransactionExecutor executor) {
        this.executor = executor;
    }

    void requireAccess() {
        if (Thread.currentThread() != owner || !active)
            throw new IllegalStateException("Transaction UserContext is outside its callback");
    }

    void requireExecutor(Object resolved) {
        requireAccess();
        if (resolved != executor)
            throw new IllegalStateException("Transaction UserContext requires a single data service");
    }

    void fail(Throwable cause) {
        if (Thread.currentThread() == owner && active && failure == null) failure = cause;
    }

    void requireWritable() {
        requireAccess();
        if (failure != null)
            throw new IllegalStateException("Transaction is marked for rollback", failure);
    }

    void throwIfFailed() {
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    void track(BaseEntity entity) {
        attempted.add(entity);
        if (entity.__internalHasMutationLedger()) ledgers.add(entity.getEntityMutationLedger());
    }

    void fenceReplay() {
        attempted.forEach(BaseEntity::__internalFenceTransactionReplay);
        ledgers.forEach(EntityMutationLedger::__internalFenceTransactionReplay);
    }

    void confirmCommit() { committed = true; }

    boolean committed() { return committed; }

    TransactionExecutor executor() { return executor; }

    void close() {
        active = false;
    }

    void release() {
        close();
        attempted.clear();
        ledgers.clear();
        failure = null;
    }
}
