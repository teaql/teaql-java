package io.teaql.core;

/**
 * Generated, typed data bootstrap invoked only after physical schema reconciliation.
 * Implementations are generated into the domain library and use ordinary Mutation APIs.
 */
@FunctionalInterface
public interface GeneratedSchemaBootstrap {
    String AUDIT_ACTOR_ATTRIBUTE = "teaql.generated.bootstrap.audit.actor";
    String AUDIT_CATEGORY_ATTRIBUTE = "teaql.generated.bootstrap.audit.category";
    String AUDIT_ACTOR = "teaql-generated-bootstrap";
    String AUDIT_CATEGORY = "runtime-bootstrap";

    void ensure(UserContext context);

    /** Runtime-owned bridge used only by private generated bootstrap programs. */
    @FrameworkInternal("Generated schema bootstrap fixed-ID creation only")
    static <T extends BaseEntity> T initializeFixedId(UserContext context, T entity, long fixedId) {
        if (context == null || entity == null) {
            throw new IllegalArgumentException("Context and entity are required for bootstrap");
        }
        InternalIdGenerationService ids = context.capability(InternalIdGenerationService.class);
        if (ids == null) {
            throw new IllegalStateException(
                    "Generated bootstrap requires an InternalIdGenerationService");
        }
        ids.ensureFloor(entity.typeName(), fixedId);
        entity.__internalInitializeNewEntityId(fixedId);
        return entity;
    }
}
