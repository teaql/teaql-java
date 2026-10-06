package io.teaql.core;

public interface QueryRequest {
    QueryIntent intent();
    default String comment() { return intent().comment(); }
    default String purpose() { return intent().purpose(); }
}
