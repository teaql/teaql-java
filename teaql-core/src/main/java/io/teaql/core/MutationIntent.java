package io.teaql.core;

/** Root mutation comment; exposed as auditReason in mutation SQL and committed audit events. */
public final class MutationIntent {
    private final String comment;

    private MutationIntent(String comment) {
        this.comment = RequestIntentException.requireComment(comment, "mutation");
    }

    public static MutationIntent of(String comment) { return new MutationIntent(comment); }
    public String comment() { return comment; }
    public String auditReason() { return comment; }
    public QueryIntent readbackIntent() {
        return QueryIntent.of(comment, "runtime: read authoritative persisted mutation result");
    }
    @Override public String toString() { return "MutationIntent[validated]"; }
}
