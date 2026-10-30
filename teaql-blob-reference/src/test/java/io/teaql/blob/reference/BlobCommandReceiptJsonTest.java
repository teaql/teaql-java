package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlobCommandReceiptJsonTest {
    @Test public void explicitMetadataCodecRoundTripsWithoutArbitraryBusinessFields() {
        var values=List.of(new BlobCommandService.EntityRevision("Asset",42,1),new BlobCommandService.EntityRevision("Asset",43,-2),
                new BlobCommandService.EntityRevision("Asset",Long.MAX_VALUE,Long.MIN_VALUE+1));
        assertEquals(values,BlobCommandReceiptJson.decode(BlobCommandReceiptJson.encode(values)));
    }
    @Test public void malformedDuplicateTrailingAndOverlargeNumbersFailClosed() {
        String valid="{\"protocol\":\"blob.command.receipt.v1\",\"entities\":[{\"entity\":\"Asset\",\"id\":42,\"revision\":1}]}";
        for (String input:List.of(valid+"{}",valid.replace("42","9223372036854775808"),valid.replace("\"id\":42","\"id\":42,\"id\":43"),valid.replace("\"Asset\"","\"https://secret\""),valid.replace("\"revision\":1","\"revision\":1,\"url\":\"http://example\""))) {
            assertEquals(BlobException.Code.BLOB_CATALOG_FAILED,assertThrows(BlobException.class,() -> BlobCommandReceiptJson.decode(input)).code());
        }
    }
    @Test public void sharedCanonicalFingerprintVectorsMatchTheCoreApi() throws Exception {
        try (var source=getClass().getResourceAsStream("/command-cases.json")) {
            var cases=new ObjectMapper().readTree(source).path("fingerprints"); assertEquals(5,cases.size());
            for (var item:cases) assertEquals(item.path("id").asText(),item.path("fingerprint").asText(),new BlobCommandService.Request(item.path("operation").asText(),item.path("invocationKey").asText(),HexFormat.of().parseHex(item.path("canonicalHex").asText())).fingerprint());
        }
    }
}
