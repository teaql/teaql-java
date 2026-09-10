package io.teaql.core.meta;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;

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
    
    /**
     * Default implementation that delegates to EntityDescriptor.createEntity()
     */
    @SuppressWarnings("unchecked")
    static EntityFactory defaultFactory() {
        return new EntityFactory() {
            @Override
            public <T extends Entity> T createEntity(Class<T> entityType) {
                EntityMetaFactory metaFactory = EntityMetaFactory.get();
                if (metaFactory == null) {
                    throw new IllegalStateException("EntityMetaFactory not initialized");
                }
                for (EntityDescriptor descriptor : metaFactory.allEntityDescriptors()) {
                    if (descriptor.getTargetType() == entityType) {
                        return (T) descriptor.createEntity();
                    }
                }
                throw new IllegalArgumentException("No entity descriptor registered for " + entityType.getName());
            }
            
            @Override
            public Entity createEntity(String typeName) {
                EntityDescriptor descriptor = EntityMetaFactory.get().resolveEntityDescriptor(typeName);
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
}
