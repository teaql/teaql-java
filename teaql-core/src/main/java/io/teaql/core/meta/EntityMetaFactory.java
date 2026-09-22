package io.teaql.core.meta;

import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import java.util.List;

/**
 * entity meta factory
 */
public interface EntityMetaFactory {
    /** Governed runtime operations must use descriptors installed in the invoking context. */
    static EntityMetaFactory requireFrom(UserContext context) {
        if (context == null) {
            throw new TeaQLRuntimeException(
                    "Entity metadata requires a non-null UserContext");
        }
        EntityMetaFactory factory = context.capability(EntityMetaFactory.class);
        if (factory == null) {
            throw new TeaQLRuntimeException(
                    "Entity metadata is not configured in this UserContext");
        }
        return factory;
    }
    static EntityMetaFactory get() {
        return Holder.factory;
    }
    static void registerGlobal(EntityMetaFactory factory) {
        Holder.factory = factory;
    }
    class Holder {
        private static EntityMetaFactory factory;
    }

    EntityDescriptor resolveEntityDescriptor(String type);

    void register(EntityDescriptor type);

    List<EntityDescriptor> allEntityDescriptors();
}
