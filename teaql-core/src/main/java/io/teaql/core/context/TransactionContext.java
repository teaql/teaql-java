package io.teaql.core.context;

import io.teaql.core.DataServiceRoute;

/**
 * Transaction boundary context interface.
 * Provides transaction management capabilities for multi-step business operations.
 */
public interface TransactionContext {
    
    /**
     * Begin a transaction on the specified data service route.
     * @param route the data service route to begin transaction on
     */
    void beginTransaction(DataServiceRoute route);
    
    /**
     * Begin a transaction on the default data service route.
     */
    default void beginTransaction() {
        beginTransaction(DataServiceRoute.DEFAULT);
    }
    
    /**
     * Commit the current transaction on the specified data service route.
     * @param route the data service route to commit transaction on
     */
    void commitTransaction(DataServiceRoute route);
    
    /**
     * Commit the current transaction on the default data service route.
     */
    default void commitTransaction() {
        commitTransaction(DataServiceRoute.DEFAULT);
    }
    
    /**
     * Rollback the current transaction on the specified data service route.
     * @param route the data service route to rollback transaction on
     */
    void rollbackTransaction(DataServiceRoute route);
    
    /**
     * Rollback the current transaction on the default data service route.
     */
    default void rollbackTransaction() {
        rollbackTransaction(DataServiceRoute.DEFAULT);
    }
    
    /**
     * Execute a block of code within a transaction.
     * Automatically commits on success, rolls back on exception.
     * @param route the data service route
     * @param action the action to execute
     */
    default void executeInTransaction(DataServiceRoute route, Runnable action) {
        beginTransaction(route);
        try {
            action.run();
            commitTransaction(route);
        } catch (Exception e) {
            rollbackTransaction(route);
            throw e;
        }
    }
    
    /**
     * Execute a block of code within a transaction on the default route.
     * @param action the action to execute
     */
    default void executeInTransaction(Runnable action) {
        executeInTransaction(DataServiceRoute.DEFAULT, action);
    }
    
    /**
     * Check if currently in a transaction on the specified route.
     * @param route the data service route
     * @return true if in a transaction
     */
    boolean isInTransaction(DataServiceRoute route);
    
    /**
     * Check if currently in a transaction on the default route.
     * @return true if in a transaction
     */
    default boolean isInTransaction() {
        return isInTransaction(DataServiceRoute.DEFAULT);
    }
}
