package io.teaql.core;

/** SDK-neutral physical execution lifecycle. Implementations must be fail-open. */
public interface SqlStatementObservation {
    default boolean enabled() { return false; }
    default SqlExecutionCorrelation correlation() { return null; }
    default void success() {}
    default void failure(Throwable failure) {}
    /** Release initial thread activation before returning a lazy cursor. */
    default void detach() {}
    default Activation activate() { return () -> {}; }
    interface Activation extends AutoCloseable { @Override void close(); }
    SqlStatementObservation NOOP = new SqlStatementObservation() {};

    static SqlStatementObservation startSafely(UserContext context, String backend, DataServiceOperation operation) {
        try {
            var observation = context.startSqlStatement(backend, operation);
            if (observation == null || observation == NOOP) return NOOP;
            return new SqlStatementObservation() {
                @Override public boolean enabled() { try { return observation.enabled(); } catch (Throwable ignored) { return false; } }
                @Override public SqlExecutionCorrelation correlation() { try { return observation.correlation(); } catch (Throwable ignored) { return null; } }
                @Override public void success() { try { observation.success(); } catch (Throwable ignored) {} }
                @Override public void failure(Throwable error) { try { observation.failure(error); } catch (Throwable ignored) {} }
                @Override public void detach() { try { observation.detach(); } catch (Throwable ignored) {} }
                @Override public Activation activate() {
                    try {
                        var scope = observation.activate();
                        return () -> { try { if (scope != null) scope.close(); } catch (Throwable ignored) {} };
                    } catch (Throwable ignored) { return () -> {}; }
                }
            };
        } catch (Throwable ignored) {
            return NOOP;
        }
    }
}
