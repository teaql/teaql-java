package io.teaql.runtime;

import io.teaql.core.RuntimeModule;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.RoundTripReferenceCodec;
import io.teaql.core.reference.RoundTripReferenceException;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/** Runs in the base runtime's own dependency graph, without the concrete secure artifact. */
public class OptionalReferenceCapabilityTest {
    @Test public void ordinaryRuntimeNeedsNoSecureImplementationOrKey() {
        try (TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).build()) {
            runtime.validateCapabilities();
            DefaultUserContext context = new DefaultUserContext(runtime);
            Assert.assertNull(context.capability(RoundTripReferenceCodec.class));
            Assert.assertThrows(RoundTripReferenceException.class, context::roundTripReferences);
            ClassLoader loader = TeaQLRuntime.class.getClassLoader();
            Assert.assertNull(loader.getResource("io/teaql/runtime/reference/AeadRoundTripReferenceProvider.class"));
            Assert.assertNull(loader.getResource("io/teaql/secure/document/reference/AeadRoundTripReferenceProvider.class"));
        }
    }

    @Test public void governedRequirementFailsReadinessWithoutProvider() {
        try (TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).build()) {
            runtime.install(RuntimeModule.of().withRequiredCapabilities("secure_document.v1"));
            TeaQLRuntimeException error = Assert.assertThrows(TeaQLRuntimeException.class, runtime::validateCapabilities);
            Assert.assertTrue(error.getMessage().startsWith("VIEW_CAPABILITY_MISSING"));
        }
    }

    @Test public void passiveManifestCombinesImmutableRequirementsAndKeepsThemThroughCopies() {
        RuntimeModule first = RuntimeModule.of().withRequiredCapabilities("blob.v1");
        RuntimeModule second = RuntimeModule.of().withRequiredCapabilities("secure_document.v1", "blob.v1");
        RuntimeModule combined = first.and(second).withCheckers();
        Assert.assertEquals(Set.of("blob.v1", "secure_document.v1"), combined.requiredCapabilities());
        Assert.assertEquals(Set.of("blob.v1"), first.requiredCapabilities());
        Assert.assertThrows(UnsupportedOperationException.class, () -> combined.requiredCapabilities().clear());
    }
}
