package io.teaql.data.dynamic;

public interface DynamicFieldContext {
    String scopeType();
    String scopeId();
    String userId();
    String purpose();
    String comment();
    boolean strictIntent();

    /**
     * Allocates a new unique ID for the given type name.
     * The runtime wires this to {@code InternalIdGenerationService}.
     *
     * @param typeName the logical type name, e.g. "DynamicFieldDef"
     * @return a new unique ID
     */
    long nextId(String typeName);
}
