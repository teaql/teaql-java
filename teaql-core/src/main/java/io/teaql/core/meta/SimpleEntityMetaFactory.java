package io.teaql.core.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.teaql.core.TeaQLRuntimeException;

public class SimpleEntityMetaFactory implements EntityMetaFactory {
    Map<String, EntityDescriptor> registeredEntities = new ConcurrentHashMap<>();

    @Override
    public EntityDescriptor resolveEntityDescriptor(String type) {
        EntityDescriptor entityDescriptor = registeredEntities.get(type);
        if (entityDescriptor == null) {
            throw new TeaQLRuntimeException("entityDescriptor " + type + " cannot be resolved");
        }
        return entityDescriptor;
    }

    public void register(EntityDescriptor entityDescriptor) {
        if (entityDescriptor == null) {
            return;
        }
        if (entityDescriptor.getTargetType() != null
                && io.teaql.core.BaseEntity.class.isAssignableFrom(entityDescriptor.getTargetType())) {
            io.teaql.core.FieldLayout layout = io.teaql.core.FieldLayout.forType(entityDescriptor.getTargetType());
            if (layout.isGenerated()) {
                java.util.Set<String> members = new java.util.HashSet<>();
                for (EntityDescriptor current = entityDescriptor; current != null; current = current.getParent()) {
                    for (PropertyDescriptor property : current.getOwnProperties()) members.add(property.getName());
                }
                layout.validateExpectedMembers(members);
            }
        }
        registeredEntities.put(entityDescriptor.getType(), entityDescriptor);
    }

    @Override
    public List<EntityDescriptor> allEntityDescriptors() {
        return new ArrayList<>(registeredEntities.values());
    }
}
