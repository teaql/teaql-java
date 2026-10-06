package io.teaql.core;

/** Every provider-bound mutation owns a validated root business reason. */
public interface MutationRequest {
    MutationIntent intent();
    default String comment() { return intent().comment(); }
}
