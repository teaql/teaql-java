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
    private final List<GeneratedSchemaBootstrap> bootstraps;
    private final io.teaql.core.checker.CheckerPresentation.Model checkerPresentation;

    private RuntimeModule(List<EntityMetaAssembler> metadataAssemblers, List<Checker<?>> checkers,
                          List<GeneratedSchemaBootstrap> bootstraps,
                          io.teaql.core.checker.CheckerPresentation.Model checkerPresentation) {
        this.metadataAssemblers = Collections.unmodifiableList(new ArrayList<>(metadataAssemblers));
        this.checkers = Collections.unmodifiableList(new ArrayList<>(checkers));
        this.bootstraps = Collections.unmodifiableList(new ArrayList<>(bootstraps));
        this.checkerPresentation = checkerPresentation;
    }

    public static RuntimeModule of(EntityMetaAssembler... metadataAssemblers) {
        Objects.requireNonNull(metadataAssemblers, "metadataAssemblers");
        Arrays.stream(metadataAssemblers).forEach(assembler ->
                Objects.requireNonNull(assembler, "metadataAssembler"));
        return new RuntimeModule(Arrays.asList(metadataAssemblers), Collections.emptyList(), Collections.emptyList(),
                io.teaql.core.checker.CheckerPresentation.Model.empty());
    }

    /** Adds generated model checkers to this passive manifest. */
    public RuntimeModule withCheckers(Checker<?>... checkers) {
        Objects.requireNonNull(checkers, "checkers");
        List<Checker<?>> combined = new ArrayList<>(this.checkers);
        Arrays.stream(checkers).forEach(checker ->
                combined.add(Objects.requireNonNull(checker, "checker")));
        return new RuntimeModule(metadataAssemblers, combined, bootstraps, checkerPresentation);
    }

    /** Adds generated typed bootstrap code while keeping installation passive. */
    public RuntimeModule withBootstrap(GeneratedSchemaBootstrap bootstrap) {
        List<GeneratedSchemaBootstrap> combined = new ArrayList<>(bootstraps);
        combined.add(Objects.requireNonNull(bootstrap, "bootstrap"));
        return new RuntimeModule(metadataAssemblers, checkers, combined, checkerPresentation);
    }

    /** Adds immutable generated vocabulary and explicit canonical/alias mappings; installation performs no I/O. */
    public RuntimeModule withCheckerPresentation(io.teaql.core.checker.CheckerPresentation.Model model) {
        return new RuntimeModule(metadataAssemblers, checkers, bootstraps, Objects.requireNonNull(model, "model"));
    }

    public io.teaql.core.checker.CheckerPresentation.Model checkerPresentation() { return checkerPresentation; }

    public RuntimeModule and(RuntimeModule other) {
        Objects.requireNonNull(other, "other");
        List<EntityMetaAssembler> combined = new ArrayList<>(metadataAssemblers);
        combined.addAll(other.metadataAssemblers);
        List<Checker<?>> combinedCheckers = new ArrayList<>(checkers);
        combinedCheckers.addAll(other.checkers);
        List<GeneratedSchemaBootstrap> combinedBootstraps = new ArrayList<>(bootstraps);
        combinedBootstraps.addAll(other.bootstraps);
        return new RuntimeModule(combined, combinedCheckers, combinedBootstraps, checkerPresentation.merge(other.checkerPresentation));
    }

    public void install(EntityMetaFactory metadata) {
        Objects.requireNonNull(metadata, "metadata");
        metadataAssemblers.forEach(assembler -> assembler.assemble(metadata));
    }

    public List<Checker<?>> checkers() {
        return checkers;
    }

    public List<GeneratedSchemaBootstrap> bootstraps() { return bootstraps; }
}
