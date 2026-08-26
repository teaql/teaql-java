package io.teaql.core;

public interface SchemaExecutor extends DataServiceExecutor {
    void ensureSchema(UserContext context, Invocation invocation);

    /** Unforgeable marker proving that UserContext initiated schema reconciliation. */
    final class Invocation {
        private static final Invocation CONTEXT = new Invocation();

        private Invocation() {}

        static Invocation contextOwned() {
            return CONTEXT;
        }

        public static void requireContextOwned(Invocation invocation) {
            if (invocation != CONTEXT) {
                throw new SecurityException("Ensure Schema must be invoked through UserContext.ensureSchema()");
            }
        }
    }
}
