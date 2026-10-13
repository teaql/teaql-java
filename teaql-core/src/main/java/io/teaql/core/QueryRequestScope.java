package io.teaql.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, execution-owned header. It contains no AST, Context or mutation state. */
public final class QueryRequestScope {
    private final QueryRequestScope parent;
    private final String name;
    private final String comment;
    private final QueryIntent intent;
    private final QueryOperation operation;

    private QueryRequestScope(QueryRequestScope parent, String name, String comment,
            QueryIntent intent, QueryOperation operation) {
        this.parent = parent;
        this.name = java.util.Objects.requireNonNull(name);
        this.comment = comment;
        this.intent = java.util.Objects.requireNonNull(intent);
        this.operation = operation;
    }
    public static QueryRequestScope root(String name, QueryIntent intent) {
        return new QueryRequestScope(null, name, intent.comment(), intent, null);
    }
    public static QueryRequestScope capture(SearchRequest<?> request) {
        return request.requestScope() != null ? request.requestScope()
                : root(request.getTypeName(), request.inheritedQueryIntent() != null
                    ? request.inheritedQueryIntent() : QueryIntent.of(request.comment(), request.purpose()));
    }
    public QueryRequestScope parentRequest() { return parent; }
    public QueryRequestScope rootRequest() {
        QueryRequestScope result = this;
        while (result.parent != null) result = result.parent;
        return result;
    }
    public String requestName() { return name; }
    public String comment() { return comment; }
    public String purpose() { return intent.purpose(); }
    public QueryIntent intent() { return intent; }
    public QueryOperation operation() { return operation; }
    public QueryRequestScope deriveOperation(String name, QueryOperation operation) {
        return new QueryRequestScope(this, name, operation.comment(), intent, operation);
    }
    public QueryRequestScope deriveHelper(String name, String comment) {
        // Reuse the same Unicode-aware, value-free required-comment diagnostic.
        QueryIntent.of(comment, intent.purpose());
        return new QueryRequestScope(this, name, comment, intent, null);
    }
    public QueryRequestScope deriveChild(String name, String comment) {
        return deriveHelper(name, comment == null ? "load " + name : comment);
    }
    public List<TraceNode> traceChain() {
        ArrayList<TraceNode> nodes = new ArrayList<>();
        for (QueryRequestScope current = this; current != null; current = current.parent)
            nodes.add(new TraceNode(TraceKind.COMMENT, current.name, current.comment));
        nodes.add(new TraceNode(TraceKind.PURPOSE, rootRequest().name, intent.purpose()));
        Collections.reverse(nodes);
        return List.copyOf(nodes);
    }
    @Override public String toString() { return "QueryRequestScope[name=" + name + ", hasParent=" + (parent != null) + "]"; }
}
