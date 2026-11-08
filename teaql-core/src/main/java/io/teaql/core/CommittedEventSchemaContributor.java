package io.teaql.core;

/** Trusted infrastructure tables installed only in the Context schema lifecycle. */
public interface CommittedEventSchemaContributor {
    void ensureSchema(UserContext context, SchemaExecutor.Invocation invocation);
}
