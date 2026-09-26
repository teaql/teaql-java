package io.teaql.examples.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceDocumentScope;
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
import org.junit.Test;

public class SecurityFoundationsExampleTest {
    private static final class OrderItem extends BaseEntity {
        OrderItem(long id, long version) {
            __internalSet("id", id);
            __internalSet("version", version);
        }

        @Override public String typeName() { return "OrderItem"; }
    }

    @Test
    public void userContextProtectsBoundaryReferences() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x22);
        AeadRoundTripReferenceProvider provider = AeadRoundTripReferenceProvider.governed(
                DeploymentProfile.TEST,
                "order-service",
                "test-a",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k2", key)),
                (context, principal, scope, identity) -> {});
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .roundTripReferenceProvider(provider)
                .build();
        UserContext context = new DefaultUserContext(runtime).withTrustedReferencePrincipal(
                new TrustedReferencePrincipal("oidc", "alice", "Platform", 7));
        ReferenceDocumentScope scope = new ReferenceDocumentScope(
                "doc-100", "edit-order", "Order", 100, 9);

        Object token = context.referenceFor(new OrderItem(42, 7), scope, Duration.ofHours(1));
        assertEquals(42, context.resolveReference(token, scope, "OrderItem").id());
        assertThrows(RoundTripReferenceException.class,
                () -> context.resolveReference(
                        token,
                        new ReferenceDocumentScope("doc-100", "view-order", "Order", 100, 9),
                        "OrderItem"));
        System.out.println("PASS Java security foundations: context-bound reference is opaque and purpose-bound");
    }
}
