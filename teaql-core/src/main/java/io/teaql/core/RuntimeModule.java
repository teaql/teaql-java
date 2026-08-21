package io.teaql.core;

import io.teaql.core.meta.EntityMetaAssembler;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.checker.Checker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** An immutable manifest of generated runtime metadata. Installing it never changes a schema. */
public final class RuntimeModule {
    private final List<EntityMetaAssembler> metadataAssemblers;
    private final List<Checker<?>> checkers;

    private RuntimeModule(List<EntityMetaAssembler> metadataAssemblers, List<Checker<?>> checkers) {
        this.metadataAssemblers = Collections.unmodifiableList(new ArrayList<>(metadataAssemblers));
        this.checkers = Collections.unmodifiableList(new ArrayList<>(checkers));
    }

    public static RuntimeModule of(EntityMetaAssembler... metadataAssemblers) {
        Objects.requireNonNull(metadataAssemblers, "metadataAssemblers");
        Arrays.stream(metadataAssemblers).forEach(assembler ->
                Objects.requireNonNull(assembler, "metadataAssembler"));
        return new RuntimeModule(Arrays.asList(metadataAssemblers), Collections.emptyList());
    }

    /** Adds generated model checkers to this passive manifest. */
    public RuntimeModule withCheckers(Checker<?>... checkers) {
        Objects.requireNonNull(checkers, "checkers");
        List<Checker<?>> combined = new ArrayList<>(this.checkers);
        Arrays.stream(checkers).forEach(checker ->
                combined.add(Objects.requireNonNull(checker, "checker")));
        return new RuntimeModule(metadataAssemblers, combined);
    }

    public RuntimeModule and(RuntimeModule other) {
        Objects.requireNonNull(other, "other");
        List<EntityMetaAssembler> combined = new ArrayList<>(metadataAssemblers);
        combined.addAll(other.metadataAssemblers);
        List<Checker<?>> combinedCheckers = new ArrayList<>(checkers);
        combinedCheckers.addAll(other.checkers);
        return new RuntimeModule(combined, combinedCheckers);
    }

    public void install(EntityMetaFactory metadata) {
        Objects.requireNonNull(metadata, "metadata");
        metadataAssemblers.forEach(assembler -> assembler.assemble(metadata));
    }

    public List<Checker<?>> checkers() {
        return checkers;
    }
}
