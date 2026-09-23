package io.teaql.core.meta;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import java.util.Objects;

/**
 * Centralized factory for entity creation.
 * Provides a single point of control for entity instantiation,
 * making it easier to test and extend.
 */
public interface EntityFactory {
    
    /**
     * Create a new entity instance of the specified type.
     * @param entityType the entity class
     * @return a new entity instance
     */
    <T extends Entity> T createEntity(Class<T> entityType);
    
    /**
     * Create a new entity instance by type name.
     * @param typeName the entity type name
     * @return a new entity instance
     */
    Entity createEntity(String typeName);
    
    /**
     * Create a new entity instance from an EntityDescriptor.
     * @param descriptor the entity descriptor
     * @return a new entity instance
     */
    <T extends Entity> T createEntity(EntityDescriptor descriptor);
    
    /** Creates an entity factory bound to the invoking runtime's metadata. */
    static EntityFactory forContext(UserContext context) {
        return forMetadata(EntityMetaFactory.requireFrom(context));
    }

    /** Creates an entity factory bound to an explicit metadata snapshot. */
    @SuppressWarnings("unchecked")
    static EntityFactory forMetadata(EntityMetaFactory metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        return new EntityFactory() {
            @Override
            public <T extends Entity> T createEntity(Class<T> entityType) {
                for (EntityDescriptor descriptor : metadata.allEntityDescriptors()) {
                    if (descriptor.getTargetType() == entityType) {
                        return (T) descriptor.createEntity();
                    }
                }
                throw new IllegalArgumentException("No entity descriptor registered for " + entityType.getName());
            }
            
            @Override
            public Entity createEntity(String typeName) {
                EntityDescriptor descriptor = metadata.resolveEntityDescriptor(typeName);
                if (descriptor == null) {
                    throw new IllegalArgumentException("No entity descriptor registered for " + typeName);
                }
                return descriptor.createEntity();
            }
            
            @Override
            public <T extends Entity> T createEntity(EntityDescriptor descriptor) {
                return (T) descriptor.createEntity();
            }
        };
    }

    /**
     * Legacy process-global factory. Prefer {@link #forContext(UserContext)} so
     * two runtimes in one process cannot silently share entity metadata.
     */
    @Deprecated
    static EntityFactory defaultFactory() {
        EntityMetaFactory metadata = EntityMetaFactory.get();
        if (metadata == null) {
            throw new IllegalStateException("Global EntityMetaFactory is not initialized; "
                    + "use EntityFactory.forContext(context)");
        }
        return forMetadata(metadata);
    }
}
