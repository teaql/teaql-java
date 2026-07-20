package io.teaql.core.spi;

import io.teaql.core.UserContext;

/**
 * The core SPI interface for auto-assembling the UserContext environment.
 * Any module (like teaql-sqlite, teaql-spring) that wants to contribute to the runtime environment
 * must implement this interface and register it via META-INF/services/io.teaql.core.spi.ContextAssembler.
 */
public interface ContextAssembler extends Comparable<ContextAssembler> {

    /**
     * Determines the execution order of this assembler.
     * Lower numbers have higher priority (executed earlier).
     * Recommended order:
     * 0-99: Core/Base generic layers
     * 100-199: Dialect/SQL generic layers
     * 200-299: Specific database dialects (e.g. Postgres, SQLite)
     * 300-399: Physical executors (e.g. JDBC, Spring JdbcTemplate, SQLDroid)
     *
     * @return the order priority
     */
    int getOrder();

    /**
     * Called exactly ONCE during system startup (cold boot).
     * Use this method to initialize heavy global resources, such as database connection pools, 
     * metadata caches, or complex dialect instances.
     * These resources should be held in static fields or internal registries within the implementing class.
     */
    default void initGlobalResources() {
        // Optional default implementation, do nothing if there are no global resources
    }

    /**
     * Called every time a new UserContext is created (per request).
     * The implementation must be extremely fast (nanosecond level). 
     * It should simply mount/attach the references of global resources to the provided context.
     * 
     * Example: ctx.put("DIALECT", myCachedPgDialect);
     *
     * @param ctx The newly instantiated UserContext
     */
    void mountTo(UserContext ctx);

    @Override
    default int compareTo(ContextAssembler other) {
        return Integer.compare(this.getOrder(), other.getOrder());
    }
}
