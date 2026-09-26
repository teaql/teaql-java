package io.teaql.examples.reference;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.reference.AeadRoundTripReferenceProvider;
import io.teaql.runtime.reference.DeploymentProfile;
import io.teaql.runtime.reference.RoundTripReferenceKey;
import io.teaql.runtime.reference.StaticRoundTripReferenceKeyProvider;
import java.time.Duration;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class RoundTripReferenceRuntimeExampleTest {
    private static final class OrderItem extends BaseEntity {
        OrderItem(long id, long version) {
            __internalSet("id", id);
            __internalSet("version", version);
        }

        @Override public String typeName() { return "OrderItem"; }
    }

    @Test
    public void contextApiIssuesAndConsumesAnOpaqueReference() {
        byte[] current = new byte[32];
        byte[] previous = new byte[32];
        Arrays.fill(current, (byte) 0x22);
        Arrays.fill(previous, (byte) 0x11);
        AeadRoundTripReferenceProvider provider = AeadRoundTripReferenceProvider.governed(
                DeploymentProfile.DEVELOPMENT,
                "order-service",
                "local",
                new StaticRoundTripReferenceKeyProvider(
                        new RoundTripReferenceKey("k2", current),
                        new RoundTripReferenceKey("k1", previous)),
                (context, principal, scope, identity) -> {
                    if (!"alice".equals(principal.subject())
                            || !"OrderItem".equals(identity.entityType())) {
                        throw new RoundTripReferenceException(
                                RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "current authorization rejected the entity reference");
                    }
                });
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .roundTripReferenceProvider(provider)
                .build();
        UserContext context = new DefaultUserContext(runtime).withTrustedReferencePrincipal(
                new TrustedReferencePrincipal("oidc", "alice", "Platform", 7));
        ReferenceDocumentScope scope = new ReferenceDocumentScope(
                "order-editor-100", "edit-order", "Order", 100, 9);

        Object wire = context.referenceFor(new OrderItem(42, 3), scope, Duration.ofMinutes(15));
        Assert.assertTrue(wire instanceof String);
        Assert.assertTrue(wire.toString().startsWith("tqr1.k2."));
        Assert.assertFalse(wire.toString().contains("OrderItem"));
        Assert.assertEquals(42, context.resolveReference(wire, scope, "OrderItem").id());

        ReferenceDocumentScope anotherDocument = new ReferenceDocumentScope(
                "order-editor-101", "edit-order", "Order", 100, 9);
        Assert.assertThrows(
                RoundTripReferenceException.class,
                () -> context.resolveReference(wire, anotherDocument, "OrderItem"));
    }
}
