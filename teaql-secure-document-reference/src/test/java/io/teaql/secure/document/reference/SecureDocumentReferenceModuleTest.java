package io.teaql.secure.document.reference;

import io.teaql.core.RuntimeModule;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceMode;
import io.teaql.core.reference.RoundTripReferenceCodec;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class SecureDocumentReferenceModuleTest {
    private AeadRoundTripReferenceProvider provider(AtomicInteger authorizations) {
        return AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST, "module-test", "test",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("fixture", new byte[32])),
                (context, principal, scope, identity) -> authorizations.incrementAndGet());
    }

    @Test public void explicitAssemblyIsPassiveAndRetainsBusinessMetadata() {
        AtomicInteger calls = new AtomicInteger();
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        SecureDocumentReferenceModule module = SecureDocumentReferenceModule.of(provider(calls));
        try (TeaQLRuntime runtime = module.installInto(TeaQLRuntime.builder().metadata(metadata)).build()) {
            runtime.install(RuntimeModule.of().withRequiredCapabilities(SecureDocumentReferenceModule.CAPABILITY));
            runtime.validateCapabilities();
            Assert.assertSame(metadata, runtime.getMetadata());
            Assert.assertEquals(0, calls.get());
            Assert.assertNotNull(new DefaultUserContext(runtime).capability(RoundTripReferenceCodec.class));
        }
    }

    @Test public void duplicateInstallationCannotReplaceProtection() {
        SecureDocumentReferenceModule module = SecureDocumentReferenceModule.of(provider(new AtomicInteger()));
        TeaQLRuntime.Builder builder = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory());
        module.installInto(builder);
        Assert.assertThrows(IllegalStateException.class, () -> module.installInto(builder));
    }
}
