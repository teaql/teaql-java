package io.teaql.examples.reference;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.*;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.reference.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class RoundTripReferenceRuntimeExampleTest {
    private static final byte[] CURRENT = bytes(1);
    private static final byte[] PREVIOUS = bytes(33);

    private static final class OrderItem extends BaseEntity {
        OrderItem(long id, long version) {
            __internalSet("id", id);
            __internalSet("version", version);
        }

        @Override public String typeName() { return "OrderItem"; }
    }

    @Test
    public void frameworkNeutralCodecRoundTripsAndRejectsTransferOrTampering() {
        RoundTripReferenceMasterKeyRing ring = new StaticRoundTripReferenceMasterKeyRing(
                new RoundTripReferenceKey("k2", CURRENT),
                new RoundTripReferenceKey("k1", PREVIOUS));
        ContextReferenceBindingProvider binding = context -> {
            String value = (String) context.getAttribute("example.reference.binding");
            return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
        };
        AeadRoundTripReferenceProvider provider = new AeadRoundTripReferenceProvider(
                new DerivedRoundTripReferenceKeyProvider(ring, binding, "reference-example"));

        UserContext alice = context(provider, "job-task:alice:42");
        RoundTripReference wire = alice.referenceFor(new OrderItem(8172, 3));
        RoundTripReference secondWire = alice.referenceFor(new OrderItem(8172, 3));
        Assert.assertTrue(wire.value().startsWith("tqr1.k2."));
        Assert.assertFalse(wire.value().contains("8172"));
        Assert.assertNotEquals("fresh nonces must produce unlinkable wire values", wire, secondWire);
        Assert.assertEquals(
                new ResolvedRoundTripReference("OrderItem", 8172, 3),
                alice.resolveReference(wire.value(), "OrderItem"));

        UserContext personalTask = context(provider, "personal-task:alice:42");
        RoundTripReferenceException transfer = Assert.assertThrows(
                RoundTripReferenceException.class,
                () -> personalTask.resolveReference(wire.value(), "OrderItem"));
        Assert.assertEquals(RoundTripReferenceErrorCode.CONTEXT_MISMATCH, transfer.getCode());

        int payloadStart = wire.value().lastIndexOf('.') + 1;
        char original = wire.value().charAt(payloadStart);
        String tampered = wire.value().substring(0, payloadStart)
                + (original == 'A' ? 'B' : 'A')
                + wire.value().substring(payloadStart + 1);
        Assert.assertThrows(
                RoundTripReferenceException.class,
                () -> alice.resolveReference(tampered, "OrderItem"));
        RoundTripReferenceException wrongType = Assert.assertThrows(
                RoundTripReferenceException.class,
                () -> alice.resolveReference(wire.value(), "Order"));
        Assert.assertEquals(RoundTripReferenceErrorCode.TYPE_MISMATCH, wrongType.getCode());
    }

    @Test
    public void missingProviderBindingAndPersistedVersionFailClosed() {
        TeaQLRuntime noProvider = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .build();
        UserContext plain = new DefaultUserContext(noProvider);
        RoundTripReferenceException missingProvider = Assert.assertThrows(
                RoundTripReferenceException.class,
                () -> plain.referenceFor(new OrderItem(1, 1)));
        Assert.assertEquals(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                missingProvider.getCode());

        ContextReferenceBindingProvider absentBinding = ignored -> null;
        UserContext unbound = context(
                provider(
                        new StaticRoundTripReferenceMasterKeyRing(
                                new RoundTripReferenceKey("k2", CURRENT)),
                        absentBinding),
                "unused");
        RoundTripReferenceException missingBinding = Assert.assertThrows(
                RoundTripReferenceException.class,
                () -> unbound.referenceFor(new OrderItem(1, 1)));
        Assert.assertEquals(
                RoundTripReferenceErrorCode.CONTEXT_BINDING_REQUIRED,
                missingBinding.getCode());

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> new InternalEntityIdentity("OrderItem", 1, 0));
    }

    @Test
    public void previousKeyDecodesAfterRotationAndRawModeIsExplicit() {
        ContextReferenceBindingProvider binding = context ->
                ((String) context.getAttribute("example.reference.binding"))
                        .getBytes(StandardCharsets.UTF_8);
        AeadRoundTripReferenceProvider oldProvider = provider(
                new StaticRoundTripReferenceMasterKeyRing(
                        new RoundTripReferenceKey("k1", PREVIOUS)), binding);
        UserContext beforeRotation = context(oldProvider, "task:7");
        String oldReference = beforeRotation.referenceFor(new OrderItem(9, 1)).value();

        AeadRoundTripReferenceProvider rotated = provider(
                new StaticRoundTripReferenceMasterKeyRing(
                        new RoundTripReferenceKey("k2", CURRENT),
                        new RoundTripReferenceKey("k1", PREVIOUS)), binding);
        UserContext afterRotation = context(rotated, "task:7");
        Assert.assertEquals(9, afterRotation.resolveReference(oldReference, "OrderItem").id());
        Assert.assertTrue(afterRotation.referenceFor(new OrderItem(10, 2)).value().startsWith("tqr1.k2."));

        Assert.assertEquals(
                RoundTripReferenceConfiguration.Mode.GOVERNED,
                RoundTripReferenceConfiguration.mode("development", "true"));
        Assert.assertEquals(
                RoundTripReferenceConfiguration.Mode.RAW_DIAGNOSTIC,
                RoundTripReferenceConfiguration.mode(
                        "development", RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT));
        Assert.assertThrows(
                IllegalStateException.class,
                () -> RoundTripReferenceConfiguration.mode(
                        "production", RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT));

        UserContext raw = context(new RawRoundTripReferenceProvider(), "ignored");
        String readable = raw.referenceFor(new OrderItem(8172, 3)).value();
        Assert.assertEquals("raw1.OrderItem.8172.3", readable);
        Assert.assertEquals(8172, raw.resolveReference(readable, "OrderItem").id());
    }

    private static AeadRoundTripReferenceProvider provider(
            RoundTripReferenceMasterKeyRing ring, ContextReferenceBindingProvider binding) {
        return new AeadRoundTripReferenceProvider(
                new DerivedRoundTripReferenceKeyProvider(ring, binding, "reference-example"));
    }

    private static UserContext context(RoundTripReferenceProvider provider, String binding) {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .roundTripReferenceProvider(provider)
                .build();
        UserContext context = new DefaultUserContext(runtime);
        context.putAttribute("example.reference.binding", binding);
        return context;
    }

    private static byte[] bytes(int start) {
        byte[] value = new byte[32];
        for (int index = 0; index < value.length; index++) value[index] = (byte) (start + index);
        return Arrays.copyOf(value, value.length);
    }
}
