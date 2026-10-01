package io.teaql.core;

/** Immutable validated intent owned by one query request, independent of logging and context. */
public final class QueryIntent {
    private final String comment;
    private final String purpose;

    private QueryIntent(String comment, String purpose) {
        this.comment = RequestIntentException.requireComment(comment, "query");
        if (RequestIntentException.blank(purpose)) {
            throw new RequestIntentException("QUERY_PURPOSE_REQUIRED", "purpose", "query");
        }
        this.purpose = purpose;
    }

    public static QueryIntent of(String comment, String purpose) { return new QueryIntent(comment, purpose); }
    public String comment() { return comment; }
    public String purpose() { return purpose; }
    @Override public String toString() { return "QueryIntent[validated]"; }
}
