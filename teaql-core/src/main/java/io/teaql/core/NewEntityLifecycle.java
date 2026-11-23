package io.teaql.core;

/** Runtime-owned admission for the public, Context-bearing creation boundary. */
final class NewEntityLifecycle {
    private NewEntityLifecycle() {}

    static <T extends Entity> T initialize(UserContext context, String entityName, T entity) {
        if (entityName == null || entityName.trim().isEmpty())
            throw new IllegalArgumentException("entityName must not be empty");
        if (entity == null) throw new IllegalArgumentException("entity must not be null");
        if (!entity.newItem())
            throw new TeaQLRuntimeException("Entity creation must return a new entity, not a persisted entity or reference");

        Long id = entity.getId();
        if (id == null) {
            InternalIdGenerationService generator = context.capability(InternalIdGenerationService.class);
            if (generator == null)
                throw new TeaQLRuntimeException("InternalIdGenerationService capability is required for entity creation");
            id = generator.generateId(context, entity);
            if (id == null || id <= 0)
                throw new TeaQLRuntimeException("InternalIdGenerationService must return a positive entity ID");
            // Identity initialization is not a user edit and has no persisted
            // baseline. Do not manufacture an ID update or original version.
            entity.setProperty(BaseEntity.ID_PROPERTY, id);
        }
        if (id <= 0 || !id.equals(entity.getId()))
            throw new TeaQLRuntimeException("Entity creation requires an assigned positive entity ID");
        if (entity instanceof BaseEntity base) {
            base.markPropertyLoaded(BaseEntity.ID_PROPERTY);
            base.getEntityMutationLedger().markAsNew(new EntityKey(entity.typeName(), id));
        }
        return entity;
    }
}
