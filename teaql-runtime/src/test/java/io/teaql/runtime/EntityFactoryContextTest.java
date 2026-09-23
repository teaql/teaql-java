package io.teaql.runtime;

import io.teaql.core.BaseEntity;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityFactory;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class EntityFactoryContextTest {
    public static class FirstEntity extends BaseEntity {
        @Override public String typeName() { return "Shared"; }
    }

    public static class SecondEntity extends BaseEntity {
        @Override public String typeName() { return "Shared"; }
    }

    @Test
    public void sameTypeNameUsesEachInvokingContextEvenWithPoisonedGlobalMetadata() {
        SimpleEntityMetaFactory firstMetadata = metadata(FirstEntity.class, FirstEntity::new);
        SimpleEntityMetaFactory secondMetadata = metadata(SecondEntity.class, SecondEntity::new);
        EntityFactory firstFactory = EntityFactory.forContext(new DefaultUserContext(
                TeaQLRuntime.builder().metadata(firstMetadata).build()));
        EntityFactory secondFactory = EntityFactory.forContext(new DefaultUserContext(
                TeaQLRuntime.builder().metadata(secondMetadata).build()));

        EntityMetaFactory previousGlobal = EntityMetaFactory.get();
        EntityMetaFactory.registerGlobal(firstMetadata);
        try {
            assertTrue(firstFactory.createEntity("Shared") instanceof FirstEntity);
            assertTrue(firstFactory.createEntity(FirstEntity.class) instanceof FirstEntity);
            assertTrue(secondFactory.createEntity("Shared") instanceof SecondEntity);
            assertTrue(secondFactory.createEntity(SecondEntity.class) instanceof SecondEntity);
        } finally {
            EntityMetaFactory.registerGlobal(previousGlobal);
        }
    }

    private static <T extends BaseEntity> SimpleEntityMetaFactory metadata(
            Class<T> type, java.util.function.Supplier<T> supplier) {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("Shared");
        descriptor.setTargetType(type);
        descriptor.setEntitySupplier(supplier);
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        metadata.register(descriptor);
        return metadata;
    }
}
