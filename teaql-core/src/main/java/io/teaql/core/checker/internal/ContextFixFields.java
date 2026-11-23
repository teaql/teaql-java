package io.teaql.core.checker.internal;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import java.util.LinkedHashSet;
import java.util.Set;

/** Model-declared Context fields, never a list of guessed time/IP/actor column names. */
public final class ContextFixFields {
    private ContextFixFields() {}

    public static Set<String> declared(EntityDescriptor descriptor) {
        var fields = new LinkedHashSet<String>();
        for (var current = descriptor; current != null; current = current.getParent()) {
            for (var property : current.getProperties()) {
                // A relation's effective attributes may traverse its reverse
                // owner; Context ownership belongs to this property itself.
                var attributes = property.getSelfAdditionalInfo();
                if (nonempty(attributes.get("createFunction")) || nonempty(attributes.get("updateFunction"))
                        || "true".equalsIgnoreCase(attributes.get("contextDerived"))) fields.add(property.getName());
            }
        }
        return fields;
    }

    private static boolean nonempty(String value) { return value != null && !value.isBlank(); }

    public static boolean eligible(UserContext context, BaseEntity entity) {
        if (entity.isMarkedAsDelete()) return false;
        if (entity.isNew()) return true;
        if (!entity.updateItem()) return false;
        if (!entity.__internalDynamicMutations().isEmpty()) return true;
        var fields = declared(EntityMetaFactory.requireFrom(context).resolveEntityDescriptor(entity.typeName()));
        for (String field : entity.getUpdatedProperties()) {
            if (!BaseEntity.ID_PROPERTY.equals(field) && !BaseEntity.VERSION_PROPERTY.equals(field)
                    && !fields.contains(field)) return true;
        }
        // A composed graph may have dynamic intent on this exact entity only.
        if (entity.__internalHasMutationLedger() && entity.getId() != null) {
            var key = new io.teaql.core.EntityKey(entity.typeName(), entity.getId());
            return entity.getEntityMutationLedger().changedFieldNames(key).stream().anyMatch(name -> name.startsWith("#"));
        }
        return false;
    }
}
