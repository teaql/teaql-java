package io.teaql.runtime.businessid;

import io.teaql.core.businessid.BusinessIdEncodingKey;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import io.teaql.core.businessid.BusinessIdScope;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class BusinessIdPermutationV1Test {
    @Test
    public void matchesEveryCrossLanguageGoldenVector() throws Exception {
        InputStream input = getClass().getResourceAsStream("/business-id-permutation-v1.csv");
        Assert.assertNotNull("golden vector resource", input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            Assert.assertEquals(
                    "case_id,key_hex,key_version,domain_root_key,aggregate_type,namespace,"
                            + "period_key,sequence,expected_code,expected_business_id",
                    reader.readLine());
            String line;
            int rows = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                Assert.assertEquals("vector columns for " + line, 10, fields.length);
                BusinessIdScope scope =
                        new BusinessIdScope(fields[3], fields[4], fields[5], fields[6]);
                BusinessIdEncodingKey key = new BusinessIdEncodingKey(
                        Integer.parseInt(fields[2]), hex(fields[1]));
                String actual = BusinessIdPermutationV1.encode(
                        Long.parseLong(fields[7]), scope, key);
                Assert.assertEquals(fields[0], fields[8], actual);
                Assert.assertEquals(fields[9], "ORD-" + fields[6] + "-" + actual);
                rows++;
            }
            Assert.assertEquals(10, rows);
        }
    }

    @Test
    public void isDeterministicUniqueAndAlwaysCanonicalForRetainedRange() {
        BusinessIdScope scope =
                new BusinessIdScope("tenant-a", "commerce_order", "order_number", "20260925");
        BusinessIdEncodingKey key = new BusinessIdEncodingKey(1, hex(
                "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"));
        Set<String> values = new HashSet<>();
        for (long sequence = 0; sequence < 20_000; sequence++) {
            String first = BusinessIdPermutationV1.encode(sequence, scope, key);
            String afterRestart = BusinessIdPermutationV1.encode(
                    sequence,
                    new BusinessIdScope(
                            "tenant-a", "commerce_order", "order_number", "20260925"),
                    new BusinessIdEncodingKey(1, key.bytes()));
            Assert.assertEquals(first, afterRestart);
            Assert.assertTrue(first.matches("[0-9A-Z]{6}"));
            Assert.assertTrue("duplicate at sequence " + sequence, values.add(first));
        }
    }

    @Test
    public void rejectsOutOfDomainSequenceAndMalformedKey() {
        BusinessIdScope scope =
                new BusinessIdScope("tenant-a", "commerce_order", "order_number", "20260925");
        BusinessIdEncodingKey key = new BusinessIdEncodingKey(1, new byte[32]);
        for (long sequence : new long[]{-1, BusinessIdPermutationV1.DOMAIN_SIZE}) {
            BusinessIdException error = Assert.assertThrows(
                    BusinessIdException.class,
                    () -> BusinessIdPermutationV1.encode(sequence, scope, key));
            Assert.assertEquals(BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED, error.getCode());
        }
        Assert.assertThrows(
                BusinessIdException.class, () -> new BusinessIdEncodingKey(1, new byte[31]));
        Assert.assertThrows(
                BusinessIdException.class, () -> new BusinessIdEncodingKey(0, new byte[32]));
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(value.substring(index * 2, index * 2 + 2), 16);
        }
        return result;
    }
}
