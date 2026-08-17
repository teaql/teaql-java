package io.teaql.core;

import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** An immutable manifest of generated runtime metadata. Installing it never changes a schema. */
public final class RuntimeModule {
    private final List<EntityMetaAssembler> metadataAssemblers;

    private RuntimeModule(List<EntityMetaAssembler> metadataAssemblers) {
        this.metadataAssemblers = Collections.unmodifiableList(new ArrayList<>(metadataAssemblers));
    }

    public static RuntimeModule of(EntityMetaAssembler... metadataAssemblers) {
        Objects.requireNonNull(metadataAssemblers, "metadataAssemblers");
        Arrays.stream(metadataAssemblers).forEach(assembler ->
                Objects.requireNonNull(assembler, "metadataAssembler"));
        return new RuntimeModule(Arrays.asList(metadataAssemblers));
    }

    public RuntimeModule and(RuntimeModule other) {
        Objects.requireNonNull(other, "other");
        List<EntityMetaAssembler> combined = new ArrayList<>(metadataAssemblers);
        combined.addAll(other.metadataAssemblers);
        return new RuntimeModule(combined);
    }

    public void install(EntityMetaFactory metadata) {
        Objects.requireNonNull(metadata, "metadata");
        metadataAssemblers.forEach(assembler -> assembler.assemble(metadata));
    }
}
