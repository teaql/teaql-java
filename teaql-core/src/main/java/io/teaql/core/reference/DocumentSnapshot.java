package io.teaql.core.reference;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded, application-owned document authority captured before an edit round trip. */
public record DocumentSnapshot(
        String modelFingerprint,
        String businessId,
        ReferenceDocumentScope scope,
        InternalEntityIdentity aggregate,
        List<Entity> entities,
        List<Relation> relations) {
    public enum MutationKind { UPDATE, CREATE_CHILD, REMOVE_CHILD }
    public enum Completeness { COMPLETE, PARTIAL }

    public record Entity(
            String rowKey,
            InternalEntityIdentity identity,
            Set<String> loadedFields,
            Set<String> writableFields,
            Set<MutationKind> allowedMutations) {
        public Entity {
            text(rowKey);
            Objects.requireNonNull(identity, "identity");
            loadedFields = Set.copyOf(loadedFields);
            writableFields = Set.copyOf(writableFields);
            allowedMutations = Set.copyOf(allowedMutations);
            if (loadedFields.size() > 1024 || writableFields.size() > 1024
                    || !loadedFields.containsAll(writableFields) || allowedMutations.isEmpty()) {
                throw projection();
            }
            loadedFields.forEach(DocumentSnapshot::text);
            writableFields.forEach(DocumentSnapshot::text);
        }
    }

    public record Relation(
            String name,
            List<String> rowKeys,
            Completeness completeness,
            boolean writable,
            Set<MutationKind> allowedMutations) {
        public Relation {
            text(name);
            rowKeys = List.copyOf(rowKeys);
            completeness = Objects.requireNonNull(completeness, "completeness");
            allowedMutations = Set.copyOf(allowedMutations);
            if (rowKeys.size() > 10000 || (!writable && !allowedMutations.isEmpty())) {
                throw projection();
            }
            Set<String> unique = new HashSet<>();
            for (String key : rowKeys) {
                text(key);
                if (!unique.add(key)) throw projection();
            }
        }
    }

    public DocumentSnapshot {
        text(modelFingerprint);
        text(businessId);
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(aggregate, "aggregate");
        entities = List.copyOf(entities);
        relations = List.copyOf(relations);
        if (entities.size() > 10000 || relations.size() > 1024
                || !aggregate.entityType().equals(scope.aggregateType())
                || aggregate.id() != scope.aggregateId()
                || aggregate.version() != scope.aggregateRevision()) {
            throw projection();
        }
        Set<String> keys = new HashSet<>();
        for (Entity entity : entities) {
            if (!keys.add(entity.rowKey())) throw projection();
        }
        for (Relation relation : relations) {
            if (!keys.containsAll(relation.rowKeys())) throw projection();
        }
    }

    private static void text(String value) {
        if (value == null || value.trim().isEmpty()
                || value.getBytes(StandardCharsets.UTF_8).length > 16384) {
            throw projection();
        }
    }

    private static RoundTripReferenceException projection() {
        return new RoundTripReferenceException(
                RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                "DOCUMENT_PROJECTION_VIOLATION");
    }
}
