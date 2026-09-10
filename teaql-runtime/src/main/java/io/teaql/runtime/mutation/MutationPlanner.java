package io.teaql.runtime.mutation;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.runtime.DefaultMutationRequest;

import java.util.*;

/**
 * Plans and executes mutation operations on entity graphs.
 * Extracted from TeaQLRuntime to follow Single Responsibility Principle.
 *
 * Responsibilities:
 * - Merge related entity roots into a single change set
 * - Collect real entities from the graph
 * - Execute the ledger plan (deletes, inserts, updates)
 */
public class MutationPlanner {

    private final EntityMetaFactory metadata;
    private final InternalIdGenerationService idGenerationService;

    public MutationPlanner(EntityMetaFactory metadata, InternalIdGenerationService idGenerationService) {
        this.metadata = metadata;
        this.idGenerationService = idGenerationService;
    }

    /**
     * Prepare an entity for mutation by:
     * 1. Merging related entity roots
     * 2. Generating ID if needed
     * 3. Tracking property changes in the entity root
     */
    public EntityRoot prepareEntity(UserContext ctx, Entity entity) {
        EntityRoot entityRoot = ((BaseEntity) entity).getEntityRoot();
        mergeRelatedEntityRoots(entity, entityRoot);

        if (entity.getId() == null && idGenerationService != null) {
            Long newId = idGenerationService.generateId(ctx, entity);
            ((BaseEntity) entity).__internalSet("id", newId);
            entityRoot.markAsNew(new EntityKey(entity.typeName(), newId));
        }

        if (entity instanceof BaseEntity be && be.getId() != null) {
            EntityKey key = new EntityKey(be.typeName(), be.getId());
            for (String prop : be.getUpdatedProperties()) {
                entityRoot.set(key, prop, be.__internalGet(prop));
            }
        }

        return entityRoot;
    }

    /**
     * Execute the mutation plan: deletes first, then inserts, then updates.
     */
    public void executePlan(UserContext ctx, EntityRoot entityRoot, Entity entity,
                           MutationExecutor mutationExecutor) {
        Map<EntityKey, BaseEntity> realEntities = new HashMap<>();
        collectRealEntities(entity, realEntities);
        executeLedgerPlan(ctx, entityRoot, mutationExecutor, realEntities);
        entityRoot.clearCurrentChangeSet();
    }

    /**
     * Merge related entities' EntityRoots into the main entity's EntityRoot.
     * This ensures that when saving an Order, its OrderItems' changes are also saved.
     */
    void mergeRelatedEntityRoots(Entity entity, EntityRoot targetRoot) {
        if (!(entity instanceof BaseEntity baseEntity)) {
            return;
        }

        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        if (descriptor == null) return;

        for (PropertyDescriptor prop : descriptor.getProperties()) {
            if (!(prop instanceof io.teaql.core.meta.Relation)) continue;
            Object value = entity.getProperty(prop.getName());
            if (value instanceof Entity relEntity) {
                mergeSingleRelatedEntity(relEntity, targetRoot);
                mergeRelatedEntityRoots(relEntity, targetRoot);
            } else if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Entity relEntity) {
                        mergeSingleRelatedEntity(relEntity, targetRoot);
                        mergeRelatedEntityRoots(relEntity, targetRoot);
                    }
                }
            }
        }
    }

    private void mergeSingleRelatedEntity(Entity relEntity, EntityRoot targetRoot) {
        EntityRoot relRoot = ((BaseEntity) relEntity).getEntityRoot();
        if (relRoot != null && relRoot != targetRoot) {
            targetRoot.mergeFrom(relRoot);
            ((BaseEntity) relEntity).setEntityRoot(targetRoot);
        }
    }

    /**
     * Collect all real entities from the entity graph into a map keyed by EntityKey.
     */
    void collectRealEntities(Entity entity, Map<EntityKey, BaseEntity> realEntities) {
        if (!(entity instanceof BaseEntity baseEntity)) return;
        if (baseEntity.getId() != null) {
            realEntities.put(new EntityKey(baseEntity.typeName(), baseEntity.getId()), baseEntity);
        }
        EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entity.typeName());
        if (descriptor == null) return;
        for (PropertyDescriptor prop : descriptor.getProperties()) {
            if (!(prop instanceof io.teaql.core.meta.Relation)) continue;
            Object value = entity.getProperty(prop.getName());
            if (value instanceof Entity relEntity) {
                collectRealEntities(relEntity, realEntities);
            } else if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Entity relEntity) {
                        collectRealEntities(relEntity, realEntities);
                    }
                }
            }
        }
    }

    /**
     * Execute the ledger plan: deletes, inserts, updates in that order.
     */
    void executeLedgerPlan(UserContext ctx, EntityRoot root, MutationExecutor mutationExecutor,
                          Map<EntityKey, BaseEntity> realEntities) {
        EntityChangeSet changeSet = root.currentChangeSet();
        Set<EntityKey> deletedKeys = root.deletedKeys();
        Set<EntityKey> newKeys = root.newKeys();

        // 1. Execute Deletes
        List<EntityKey> sortedDeletedKeys = new ArrayList<>(deletedKeys);
        Collections.sort(sortedDeletedKeys);
        for (EntityKey key : sortedDeletedKeys) {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(key.entity());
            if (descriptor == null) {
                throw new TeaQLRuntimeException("No entity descriptor for: " + key.entity());
            }
            BaseEntity deleteEntity = realEntities.get(key);
            if (deleteEntity == null) {
                deleteEntity = (BaseEntity) descriptor.createEntity();
                deleteEntity.__internalSet("id", key.id());
                deleteEntity.set$status(io.teaql.core.EntityStatus.PERSISTED);
            }
            deleteEntity.markToRemove();
            if (root.getComment() != null) deleteEntity.setComment(root.getComment());

            DefaultMutationRequest mutationRequest = new DefaultMutationRequest(
                deleteEntity, DefaultMutationRequest.Action.DELETE);
            mutationExecutor.mutate(ctx, mutationRequest);
        }

        // 2. Group changes by operation type
        Map<String, List<EntityKey>> insertBatches = new TreeMap<>();
        Map<String, List<EntityKey>> updateBatches = new TreeMap<>();

        for (Map.Entry<EntityKey, Map<String, Object>> entry : changeSet.changes().entrySet()) {
            EntityKey key = entry.getKey();
            if (deletedKeys.contains(key)) continue;

            boolean isNew = newKeys.contains(key) || key.id() == null;
            if (isNew) {
                insertBatches.computeIfAbsent(key.entity(), k -> new ArrayList<>()).add(key);
            } else {
                updateBatches.computeIfAbsent(key.entity(), k -> new ArrayList<>()).add(key);
            }
        }

        // 3. Execute Inserts
        executeBatchMutations(ctx, insertBatches, changeSet, realEntities, root, mutationExecutor,
                            DefaultMutationRequest.Action.SAVE, false);

        // 4. Execute Updates
        executeBatchMutations(ctx, updateBatches, changeSet, realEntities, root, mutationExecutor,
                            DefaultMutationRequest.Action.SAVE, true);
    }

    private void executeBatchMutations(UserContext ctx, Map<String, List<EntityKey>> batches,
                                      EntityChangeSet changeSet, Map<EntityKey, BaseEntity> realEntities,
                                      EntityRoot root, MutationExecutor mutationExecutor,
                                      DefaultMutationRequest.Action action, boolean markAsUpdated) {
        for (Map.Entry<String, List<EntityKey>> entry : batches.entrySet()) {
            String entityName = entry.getKey();
            List<EntityKey> keys = entry.getValue();
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(entityName);
            if (descriptor == null) {
                throw new TeaQLRuntimeException("No entity descriptor for: " + entityName);
            }
            for (EntityKey key : keys) {
                Map<String, Object> changes = changeSet.changes().get(key);
                if (changes == null) continue;
                BaseEntity entity = realEntities.get(key);
                if (entity == null) {
                    entity = (BaseEntity) descriptor.createEntity();
                    entity.__internalSet("id", key.id());
                }
                Long version = root.getOriginalVersion(key);
                if (version != null) {
                    entity.__internalSet("version", version);
                }
                for (Map.Entry<String, Object> change : changes.entrySet()) {
                    entity.updateProperty(change.getKey(), change.getValue());
                }
                if (markAsUpdated) {
                    entity.set$status(io.teaql.core.EntityStatus.UPDATED);
                }
                if (root.getComment() != null) entity.setComment(root.getComment());

                DefaultMutationRequest mutationRequest = new DefaultMutationRequest(entity, action);
                mutationExecutor.mutate(ctx, mutationRequest);
                entity.clearUpdatedProperties();
            }
        }
    }
}
