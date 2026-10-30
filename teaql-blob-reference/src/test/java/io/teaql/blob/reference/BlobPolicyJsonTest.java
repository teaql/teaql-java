package io.teaql.blob.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.teaql.core.blob.*;
import java.time.Duration;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlobPolicyJsonTest {
    @Test public void resolvedSnapshotsRoundTripWithoutChangingAcceptedPromises() {
        var policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").version(3)
                .minimumRetention(Duration.ofDays(30)).unreferencedRetention(Duration.ofDays(90)).build(),Map.of(),Map.of());
        for (String extension:BlobPolicies.FORMATS) {
            var snapshot=policies.resolve(BlobFieldSpec.of("附件","文件",List.of(extension),true,"default"));
            assertEquals(snapshot,BlobPolicyJson.decode(BlobPolicyJson.encode(snapshot)));
            assertEquals(Duration.ofDays(30),BlobPolicyJson.decode(BlobPolicyJson.encode(snapshot)).minimumRetention());
        }
    }
    @Test public void malformedAndUnsupportedSnapshotsFailClosed() throws Exception {
        var field=BlobFieldSpec.of("Asset","document",List.of("pdf"),true,"default");
        String original=BlobPolicyJson.encode(BlobPolicies.defaults().resolve(field));
        var json=new ObjectMapper();
        for (String property:List.of("schema","version","unbound","unreferenced","minimum","readUrl","maxSize")) {
            ObjectNode value=(ObjectNode)json.readTree(original); value.put(property,"0"); reject(value.toString());
        }
        for (String property:List.of("nullable","allowEmpty")) {
            ObjectNode value=(ObjectNode)json.readTree(original); value.put(property,"true"); reject(value.toString());
        }
        ObjectNode extra=(ObjectNode)json.readTree(original); extra.put("extra",1); reject(extra.toString());
        ObjectNode missing=(ObjectNode)json.readTree(original); missing.remove("unbound"); reject(missing.toString());
        reject(original.replace("\"schema\":1","\"schema\":2"));
        reject(original.replace("\"schema\":1","\"schema\":1,\"schema\":1"));
        reject(original.replace("[\"pdf\"]","[\"pdf\",\"pdf\"]"));
        reject("null"); reject("{}"); reject("x".repeat(16385));
    }
    private static void reject(String value) {
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> BlobPolicyJson.decode(value)).code());
    }
}
