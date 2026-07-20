package io.teaql.core.meta;

import java.util.List;

/**
 * entity meta factory
 */
public interface EntityMetaFactory {
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
