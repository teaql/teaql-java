package io.teaql.data.dynamic;

public interface DynamicFieldContext {
    String scopeType();
    String scopeId();
    String userId();
    String purpose();
    String comment();
    boolean strictIntent();
}
