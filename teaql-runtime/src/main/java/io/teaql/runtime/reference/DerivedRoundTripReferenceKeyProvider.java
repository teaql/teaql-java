package io.teaql.runtime.reference;

import io.teaql.core.UserContext;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Derives context-specific AES keys without assigning meaning to the context binding. */
public final class DerivedRoundTripReferenceKeyProvider
        implements RoundTripReferenceKeyProvider {
    private static final byte[] SALT = "teaql-round-trip-reference-v1"
            .getBytes(StandardCharsets.UTF_8);

    private final RoundTripReferenceMasterKeyRing masterKeys;
    private final ContextReferenceBindingProvider bindings;
    private final byte[] namespace;

    public DerivedRoundTripReferenceKeyProvider(
            RoundTripReferenceMasterKeyRing masterKeys,
            ContextReferenceBindingProvider bindings,
            String namespace) {
        if (masterKeys == null || bindings == null) {
            throw new IllegalArgumentException("masterKeys and bindings are required");
        }
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        this.masterKeys = masterKeys;
        this.bindings = bindings;
        this.namespace = namespace.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public RoundTripReferenceKey currentKey(UserContext context) {
        return derive(context, masterKeys.currentKey());
    }

    @Override
    public RoundTripReferenceKey keyById(UserContext context, String keyId) {
        RoundTripReferenceKey master = masterKeys.keyById(keyId);
        return master == null ? null : derive(context, master);
    }

    private RoundTripReferenceKey derive(UserContext context, RoundTripReferenceKey master) {
        byte[] binding = bindings.bindingFor(context);
        if (binding == null || binding.length == 0) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.CONTEXT_BINDING_REQUIRED,
                    "Runtime customization did not provide a round-trip reference binding");
        }
        try {
            byte[] pseudoRandomKey = hmac(SALT, master.keyBytes());
            ByteArrayOutputStream info = new ByteArrayOutputStream();
            info.writeBytes("teaql:tqr1".getBytes(StandardCharsets.UTF_8));
            writeLengthPrefixed(info, namespace);
            writeLengthPrefixed(info, binding);
            byte[] output = hkdfExpand(pseudoRandomKey, info.toByteArray(), 32);
            return new RoundTripReferenceKey(master.keyId(), output);
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.INVALID_REFERENCE,
                    "Unable to derive a round-trip reference key", error);
        }
    }

    private static byte[] hmac(byte[] key, byte[] input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(input);
    }

    private static byte[] hkdfExpand(byte[] key, byte[] info, int length) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream(length);
        byte[] previous = new byte[0];
        int counter = 1;
        while (result.size() < length) {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(previous);
            mac.update(info);
            mac.update((byte) counter++);
            previous = mac.doFinal();
            result.write(previous, 0, Math.min(previous.length, length - result.size()));
        }
        return result.toByteArray();
    }

    private static void writeLengthPrefixed(ByteArrayOutputStream output, byte[] value) {
        output.write((value.length >>> 24) & 0xff);
        output.write((value.length >>> 16) & 0xff);
        output.write((value.length >>> 8) & 0xff);
        output.write(value.length & 0xff);
        output.writeBytes(value);
    }
}
