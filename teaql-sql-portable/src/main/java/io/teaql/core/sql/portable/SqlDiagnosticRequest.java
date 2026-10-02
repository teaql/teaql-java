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
    private final transient java.util.List<io.teaql.core.TraceNode> traceSource;

    SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source) {
        this(request, source, request.inheritedQueryIntent() == null
                ? QueryIntent.of(request.comment(), request.purpose()) : request.inheritedQueryIntent(), false);
    }

    private SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source,
                                 QueryIntent rootIntent, boolean executionScope) {
        this(request, source, rootIntent, executionScope, request.sqlTraceSource());
    }

    private SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source,
                                 QueryIntent rootIntent, boolean executionScope,
                                 java.util.List<io.teaql.core.TraceNode> traceSource) {
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
        this.traceSource = traceSource.isEmpty()
                ? java.util.List.of(
                    new io.teaql.core.TraceNode(io.teaql.core.TraceKind.COMMENT, request.getTypeName(), rootIntent.comment()),
                    new io.teaql.core.TraceNode(io.teaql.core.TraceKind.PURPOSE, request.getTypeName(), rootIntent.purpose()))
                : java.util.List.copyOf(traceSource);
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
    @Override public java.util.List<io.teaql.core.TraceNode> sqlTraceSource() { return traceSource; }

    /** Derived work keeps its parent's origin even when it does not traverse a model relation. */
    static SqlDiagnosticRequest forDerived(SearchRequest<?> child, SqlIntentRedactions source,
                                           SearchRequest<?> parent) {
        QueryIntent intent = parentIntent(parent);
        return new SqlDiagnosticRequest(child, source, intent, false, parentTrace(parent, intent));
    }

    static SqlDiagnosticRequest forRelation(SearchRequest<?> child, SqlIntentRedactions source,
                                            SearchRequest<?> parent, String relationName) {
        QueryIntent intent = parentIntent(parent);
        var trace = new java.util.ArrayList<>(parentTrace(parent, intent));
        trace.add(new io.teaql.core.TraceNode(io.teaql.core.TraceKind.RELATION, relationName,
                parent.getTypeName() + "." + relationName));
        return new SqlDiagnosticRequest(child, source, intent, false, trace);
    }

    private static QueryIntent parentIntent(SearchRequest<?> parent) {
        QueryIntent inherited = parent.inheritedQueryIntent();
        return inherited == null ? QueryIntent.of(parent.comment(), parent.purpose()) : inherited;
    }

    private static java.util.List<io.teaql.core.TraceNode> parentTrace(SearchRequest<?> parent, QueryIntent intent) {
        var trace = parent.sqlTraceSource();
        return trace.isEmpty() ? java.util.List.of(
                new io.teaql.core.TraceNode(io.teaql.core.TraceKind.COMMENT, parent.getTypeName(), intent.comment()),
                new io.teaql.core.TraceNode(io.teaql.core.TraceKind.PURPOSE, parent.getTypeName(), intent.purpose()))
                : trace;
    }

    @Override public io.teaql.core.Entity internalNewEntity() { return original.internalNewEntity(); }
    @Override public boolean tryUseSubQuery() { return original.tryUseSubQuery(); }

    static SqlIntentRedactions source(UserContext context, SearchRequest<?> request) {
        if (!context.isQueryExecutionLoggingEnabled()) return null;
        if (request instanceof SqlDiagnosticRequest scoped && scoped.source != null)
            return scoped.executionScope ? scoped.source : scoped.source.copy();
        return new SqlIntentRedactions();
    }
}
