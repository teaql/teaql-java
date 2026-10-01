package io.teaql.core.sql.portable;

import io.teaql.core.SearchRequest;
import io.teaql.core.QueryIntent;
import io.teaql.core.SqlIntentRedactions;
import io.teaql.core.UserContext;
import io.teaql.core.internal.TempRequest;

/** Invocation-local provenance, never an extension/wire field or context/repository state. */
final class SqlDiagnosticRequest extends TempRequest {
    private final transient SqlIntentRedactions source;
    private final transient boolean executionScope;
    private final transient SearchRequest<?> original;
    private final transient QueryIntent rootIntent;

    SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source) {
        this(request, source, request.inheritedQueryIntent() == null
                ? QueryIntent.of(request.comment(), request.purpose()) : request.inheritedQueryIntent(), false);
    }

    SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source, QueryIntent rootIntent) {
        this(request, source, rootIntent, false);
    }

    private SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source,
                                 QueryIntent rootIntent, boolean executionScope) {
        super(request);
        this.original = request;
        // TempRequest's relation-oriented copy omits these root-query semantics.
        this.rootIntent = java.util.Objects.requireNonNull(rootIntent, "rootIntent");
        this.comment = rootIntent.comment();
        this.purpose = rootIntent.purpose();
        this.searchForText = request.getSearchForText();
        this.dynamicFieldSelection = request.getDynamicFieldSelection();
        this.hardLimit = request.hardLimit();
        this.source = source == null || executionScope ? source : source.copy();
        this.executionScope = executionScope;
    }

    static SqlDiagnosticRequest forExecution(SearchRequest<?> request, SqlIntentRedactions source) {
        QueryIntent intent = request.inheritedQueryIntent() == null
                ? QueryIntent.of(request.comment(), request.purpose()) : request.inheritedQueryIntent();
        return forExecution(request, source, intent);
    }

    static SqlDiagnosticRequest forExecution(SearchRequest<?> request, SqlIntentRedactions source, QueryIntent intent) {
        return new SqlDiagnosticRequest(request, source, intent, true);
    }

    @Override public QueryIntent inheritedQueryIntent() { return rootIntent; }

    @Override public io.teaql.core.Entity internalNewEntity() { return original.internalNewEntity(); }
    @Override public boolean tryUseSubQuery() { return original.tryUseSubQuery(); }

    static SqlIntentRedactions source(UserContext context, SearchRequest<?> request) {
        if (!context.isQueryExecutionLoggingEnabled()) return null;
        if (request instanceof SqlDiagnosticRequest scoped && scoped.source != null)
            return scoped.executionScope ? scoped.source : scoped.source.copy();
        return new SqlIntentRedactions();
    }
}
