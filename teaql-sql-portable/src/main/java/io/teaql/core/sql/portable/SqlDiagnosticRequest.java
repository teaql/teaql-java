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
    private final transient io.teaql.core.QueryRequestScope requestScope;
    private final transient java.util.List<io.teaql.core.TraceNode> traceSource;
    private final transient java.util.function.Consumer<io.teaql.core.ExecutionMetadata> statementObserver;

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
        this(request, source, rootIntent, executionScope, traceSource, observer(request));
    }

    private SqlDiagnosticRequest(SearchRequest<?> request, SqlIntentRedactions source,
                                 QueryIntent rootIntent, boolean executionScope,
                                 java.util.List<io.teaql.core.TraceNode> traceSource,
                                 java.util.function.Consumer<io.teaql.core.ExecutionMetadata> observer) {
        super(request);
        this.statementObserver = observer;
        this.original = request;
        // TempRequest's relation-oriented copy omits these root-query semantics.
        this.rootIntent = java.util.Objects.requireNonNull(rootIntent, "rootIntent");
        this.requestScope = request.requestScope() == null
                ? io.teaql.core.QueryRequestScope.root(request.getTypeName(), rootIntent) : request.requestScope();
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

    static SqlDiagnosticRequest collecting(SearchRequest<?> request, SqlIntentRedactions source,
            QueryIntent intent, java.util.function.Consumer<io.teaql.core.ExecutionMetadata> observer) {
        // A relation lookup can re-enter the provider. Its result has its own
        // collection while the root invocation still owns all descendant facts.
        var parentObserver = observer(request);
        var combined = parentObserver == null ? observer : parentObserver.andThen(observer);
        return new SqlDiagnosticRequest(request, source, intent, true, request.sqlTraceSource(), combined);
    }

    private static java.util.function.Consumer<io.teaql.core.ExecutionMetadata> observer(SearchRequest<?> request) {
        return request.statementObserver();
    }

    static io.teaql.core.SqlExecutionTrace statementTrace(SearchRequest<?> request) {
        return io.teaql.core.SqlExecutionTrace.query(request).collecting(observer(request));
    }

    @Override public QueryIntent inheritedQueryIntent() { return rootIntent; }
    @Override public java.util.function.Consumer<io.teaql.core.ExecutionMetadata> statementObserver() { return statementObserver; }
    @Override public SqlIntentRedactions sqlIntentSource() { return source == null ? null : source.copy(); }
    @Override public io.teaql.core.QueryRequestScope requestScope() { return requestScope; }
    @Override public java.util.List<io.teaql.core.TraceNode> sqlTraceSource() { return traceSource; }

    /** Derived work keeps its parent's origin even when it does not traverse a model relation. */
    static SqlDiagnosticRequest forDerived(SearchRequest<?> child, SqlIntentRedactions source,
                                           SearchRequest<?> parent) {
        QueryIntent intent = parentIntent(parent);
        var parentScope = io.teaql.core.QueryRequestScope.capture(parent);
        var scope = parentScope.deriveHelper(child.getTypeName(), "execute query helper " + child.getTypeName());
        return new SqlDiagnosticRequest(new io.teaql.core.QueryRequestSnapshot(child, scope),
                source, intent, false, parentTrace(parent, intent), observer(parent));
    }

    static SqlDiagnosticRequest forRelation(SearchRequest<?> child, SqlIntentRedactions source,
                                            SearchRequest<?> parent, String relationName) {
        QueryIntent intent = parentIntent(parent);
        var trace = new java.util.ArrayList<>(parentTrace(parent, intent));
        trace.add(new io.teaql.core.TraceNode(io.teaql.core.TraceKind.RELATION, relationName,
                parent.getTypeName() + "." + relationName));
        var operation = parent.queryOperations().getOrDefault(relationName,
                io.teaql.core.QueryOperation.framework(io.teaql.core.QueryOperation.Kind.RELATION_SELECT, null, relationName));
        var operationScope = io.teaql.core.QueryRequestScope.capture(parent).deriveOperation(parent.getTypeName(), operation);
        String comment = child.requestScope() == null ? child.comment() : child.requestScope().comment();
        var scope = operationScope.deriveChild(child.getTypeName(), comment);
        return new SqlDiagnosticRequest(new io.teaql.core.QueryRequestSnapshot(child, scope), source, intent, false, trace, observer(parent));
    }

    static SqlDiagnosticRequest forOwnedScope(SearchRequest<?> child, SqlIntentRedactions source,
            SearchRequest<?> parent, io.teaql.core.QueryRequestScope scope, String relationName) {
        QueryIntent intent = parentIntent(parent);
        var trace = new java.util.ArrayList<>(parentTrace(parent, intent));
        if (relationName != null) trace.add(new io.teaql.core.TraceNode(io.teaql.core.TraceKind.RELATION,
                relationName, parent.getTypeName() + "." + relationName));
        return new SqlDiagnosticRequest(new io.teaql.core.QueryRequestSnapshot(child, scope), source, intent, false, trace, observer(parent));
    }

    static SqlDiagnosticRequest forAggregation(SearchRequest<?> child, SqlIntentRedactions source,
            SearchRequest<?> parent, io.teaql.core.SimpleAggregation aggregate, String relationName) {
        var parentScope = io.teaql.core.QueryRequestScope.capture(parent);
        var facet = parentScope.parentRequest();
        io.teaql.core.QueryRequestScope operationScope;
        if (facet != null && facet.operation() != null && facet.operation().kind() == io.teaql.core.QueryOperation.Kind.FACET) {
            operationScope = facet.deriveHelper(child.getTypeName(), "aggregate " + facet.operation().relation());
        } else {
            var operation = aggregate.getOperation() == null
                    ? io.teaql.core.QueryOperation.framework(io.teaql.core.QueryOperation.Kind.AGGREGATE, aggregate.getName(), relationName)
                    : aggregate.getOperation();
            operationScope = parentScope.deriveOperation(child.getTypeName(), operation);
        }
        String comment = child.requestScope() == null ? child.comment() : child.requestScope().comment();
        var scope = comment == null ? operationScope : operationScope.deriveChild(child.getTypeName(), comment);
        return forOwnedScope(child, source, parent, scope, relationName);
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
        if (!context.isQueryExecutionLoggingEnabled() && observer(request) == null) return null;
        if (request instanceof SqlDiagnosticRequest scoped && scoped.source != null)
            return scoped.executionScope ? scoped.source : scoped.source.copy();
        if (request.sqlIntentSource() != null) return request.sqlIntentSource();
        return new SqlIntentRedactions();
    }
}
