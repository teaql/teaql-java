package io.teaql.runtime.businessid;

import io.teaql.core.businessid.BusinessIdEncodingKey;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import io.teaql.core.businessid.BusinessIdScope;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Canonical TeaQL Business ID fixed-domain permutation profile V1. */
public final class BusinessIdPermutationV1 {
    public static final int WIDTH = 6;
    public static final long DOMAIN_SIZE = 2_176_782_336L;
    public static final long MAX_SEQUENCE = DOMAIN_SIZE - 1;
    public static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final byte[] MAGIC =
            "teaql-business-id-fp-v1\0".getBytes(StandardCharsets.US_ASCII);
    private static final int ROUNDS = 8;

    private BusinessIdPermutationV1() {
    }

    public static String encode(
            long sequence, BusinessIdScope scope, BusinessIdEncodingKey key) {
        if (sequence < 0 || sequence >= DOMAIN_SIZE) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                    "Business ID V1 sequence must be in 0.." + MAX_SEQUENCE);
        }
        byte[] tweak = canonicalTweak(scope, key.version());
        long candidate = sequence;
        do {
            candidate = permute32(candidate, tweak, key.bytes());
        } while (candidate >= DOMAIN_SIZE);
        String encoded = Long.toString(candidate, 36).toUpperCase(Locale.ROOT);
        return "0".repeat(WIDTH - encoded.length()) + encoded;
    }

    private static long permute32(long value, byte[] tweak, byte[] key) {
        int left = (int) ((value >>> 16) & 0xffff);
        int right = (int) (value & 0xffff);
        for (int round = 0; round < ROUNDS; round++) {
            int output = roundFunction(key, tweak, round, right);
            int nextLeft = right;
            int nextRight = (left ^ output) & 0xffff;
            left = nextLeft;
            right = nextRight;
        }
        return Integer.toUnsignedLong((left << 16) | right);
    }

    private static int roundFunction(byte[] key, byte[] tweak, int round, int right) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key, "HmacSHA256"));
            hmac.update(tweak);
            hmac.update((byte) round);
            hmac.update((byte) (right >>> 8));
            hmac.update((byte) right);
            byte[] digest = hmac.doFinal();
            return ((digest[0] & 0xff) << 8) | (digest[1] & 0xff);
        } catch (GeneralSecurityException error) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_ENCODING_FAILED,
                    "HMAC-SHA256 is unavailable for Business ID V1",
                    error);
        }
    }

    private static byte[] canonicalTweak(BusinessIdScope scope, int keyVersion) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.write(MAGIC);
            output.writeByte(1);
            output.writeInt(keyVersion);
            writeField(output, scope.domainRootKey());
            writeField(output, scope.aggregateType());
            writeField(output, scope.namespace());
            writeField(output, scope.periodKey());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_ENCODING_FAILED,
                    "Unable to frame Business ID V1 scope",
                    impossible);
        }
    }

    private static void writeField(DataOutputStream output, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }
}
