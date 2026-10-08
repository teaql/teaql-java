package io.teaql.data.dynamic;

/** Runtime-owned graph write request. Provider scopes/permissions are still resolved from context. */
public record DynamicGraphMutation(DynamicOwnerRef owner, DynamicFieldMutation mutation, DynamicFieldMetadata sourceMetadata) {
    public DynamicGraphMutation {
        java.util.Objects.requireNonNull(owner, "owner");
        java.util.Objects.requireNonNull(mutation, "mutation");
        java.util.Objects.requireNonNull(sourceMetadata, "source metadata");
    }
}
