package io.teaql.runtime;

import io.teaql.core.RuntimeModule;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import org.junit.Assert;
import org.junit.Test;

public class RuntimeModuleTest {
    @Test
    public void installsComposableMetadataWithoutSchemaSideEffects() {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        RuntimeModule first = RuntimeModule.of(factory -> {
            EntityDescriptor descriptor = new EntityDescriptor();
            descriptor.setType("First");
            factory.register(descriptor);
        });
        RuntimeModule second = RuntimeModule.of(factory -> {
            EntityDescriptor descriptor = new EntityDescriptor();
            descriptor.setType("Second");
            factory.register(descriptor);
        });

        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(metadata).build();
        Assert.assertSame(runtime, runtime.install(first.and(second)));
        Assert.assertEquals(2, metadata.allEntityDescriptors().size());
    }
}
