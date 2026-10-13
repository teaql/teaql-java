package io.teaql.core;

public interface QueryRequest {
    QueryIntent intent();
    QueryRequestScope requestScope();
    default QueryRequestScope parentRequest() { return requestScope().parentRequest(); }
    default QueryRequestScope rootRequest() { return requestScope().rootRequest(); }
    default String localComment() { return requestScope().comment(); }
    default java.util.List<TraceNode> businessTraceChain() { return requestScope().traceChain(); }
    default String comment() { return intent().comment(); }
    default String purpose() { return intent().purpose(); }
}
