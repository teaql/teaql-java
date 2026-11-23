package io.teaql.core;

import java.util.List;

/** Framework query-body copy with an immutable header; never mutates a reusable builder. */
@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryRequestSnapshot<T extends Entity> extends BaseRequest<T> {
    private final transient QueryRequestScope scope;
    private final transient SearchRequest<T> original;
    private final String typeName;
    private final transient SearchRequest<?> diagnosticSource;
    protected QueryRequestSnapshot(Class<T> returnType, String typeName) {
        super(returnType);
        this.typeName = typeName; original = null; scope = null; diagnosticSource = null;
    }
    public QueryRequestSnapshot(SearchRequest<T> request, QueryRequestScope scope) {
        this(request, scope, request.diagnosticSource());
    }
    public QueryRequestSnapshot(SearchRequest<T> request, QueryRequestScope scope, SearchRequest<?> diagnosticSource) {
        this(request, scope, diagnosticSource, new QueryRequestGraphCopy(false), 0);
    }
    QueryRequestSnapshot(SearchRequest<T> request, QueryRequestScope scope, SearchRequest<?> diagnosticSource,
            QueryRequestGraphCopy graph, int depth) {
        super((Class<T>) request.returnType(), request::internalNewEntity);
        original = request;
        typeName = request.getTypeName();
        this.scope = scope;
        this.diagnosticSource = diagnosticSource;
        graph.populate(request, this, depth);
        if (scope != null && scope.parentRequest() != null && slice != null && !slice.hasExplicitPage())
            slice = null;
        comment = scope == null ? request.comment() : scope.intent().comment();
        purpose = scope == null ? request.purpose() : scope.purpose();
    }
    @Override public String getTypeName() { return typeName; }
    @Override public QueryIntent inheritedQueryIntent() { return scope != null ? scope.intent() : original == null ? null : original.inheritedQueryIntent(); }
    @Override public QueryRequestScope requestScope() { return scope; }
    @Override public SearchRequest<?> diagnosticSource() { return diagnosticSource; }
    @Override public java.util.function.Consumer<ExecutionMetadata> statementObserver() { return original == null ? null : original.statementObserver(); }
    @Override public SqlIntentRedactions sqlIntentSource() { return original == null ? null : original.sqlIntentSource(); }
    @Override public List<TraceNode> sqlTraceSource() { return original == null ? List.of() : original.sqlTraceSource(); }
    @Override public boolean tryUseSubQuery() { return original == null || original.tryUseSubQuery(); }
}
