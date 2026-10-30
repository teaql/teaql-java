package io.teaql.blob.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonParser;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobPolicyProfile.*;
import java.time.Duration;
import java.util.TreeSet;
import java.util.Set;

/** Explicit versioned storage encoding. No record reflection, identity or storage credentials. */
final class BlobPolicyJson {
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final Set<String> FIELDS=Set.of("schema","profile","version","entity","field","fieldProfile","nullable","extensions",
            "maxSize","allowEmpty","uploadSession","uploadUrl","unbound","unreferenced","minimum","readUrl","uploadMode","sharing","disposition","responseShape","cache");
    private BlobPolicyJson() {}
    static String encode(BlobPolicySnapshot policy) {
        ObjectNode n = JSON.createObjectNode();
        n.put("schema", 1); n.put("profile", policy.profile()); n.put("version", policy.version());
        n.put("entity", policy.field().entity()); n.put("field", policy.field().field());
        n.put("fieldProfile", policy.field().policyProfile()); n.put("nullable", policy.field().nullable());
        var extensions = n.putArray("extensions"); policy.field().allowedExtensions().forEach(extensions::add);
        n.put("maxSize", policy.maxSizeBytes()); n.put("allowEmpty", policy.allowEmpty());
        n.put("uploadSession", policy.uploadSessionTtl().getSeconds()); n.put("uploadUrl", policy.uploadUrlTtl().getSeconds());
        n.put("unbound", policy.unboundRetention().getSeconds()); n.put("unreferenced", policy.unreferencedRetention().getSeconds());
        n.put("minimum", policy.minimumRetention().getSeconds()); n.put("readUrl", policy.readUrlTtl().getSeconds());
        n.put("uploadMode", policy.uploadMode().name()); n.put("sharing", policy.sharing().name());
        n.put("disposition", policy.disposition().name()); n.put("responseShape", policy.responseShape().name());
        n.put("cache", policy.cachePolicy());
        return n.toString();
    }
    static BlobPolicySnapshot decode(String json) {
        try {
            if (json==null || json.length()>16384) throw new IllegalArgumentException();
            JsonNode n = JSON.readTree(json);
            if (n==null || !n.isObject() || n.size()!=FIELDS.size() || integer(n,"schema") != 1) throw new IllegalArgumentException();
            for (String name:FIELDS) n.required(name);
            if (!n.required("extensions").isArray()) throw new IllegalArgumentException();
            var extensions = new TreeSet<String>(); n.required("extensions").forEach(e -> {
                if (!e.isTextual() || !extensions.add(e.textValue())) throw new IllegalArgumentException();
            });
            var field = BlobFieldSpec.of(text(n,"entity"), text(n,"field"), extensions,
                    bool(n,"nullable"), text(n,"fieldProfile"));
            return new BlobPolicySnapshot(text(n,"profile"), integer(n,"version"), field,
                    integer(n,"maxSize"), bool(n,"allowEmpty"),
                    duration(n,"uploadSession"), duration(n,"uploadUrl"), duration(n,"unbound"),
                    duration(n,"unreferenced"), duration(n,"minimum"), duration(n,"readUrl"),
                    UploadMode.valueOf(text(n,"uploadMode")), Sharing.valueOf(text(n,"sharing")),
                    Disposition.valueOf(text(n,"disposition")), ResponseShape.valueOf(text(n,"responseShape")), text(n,"cache"));
        } catch (java.io.IOException | RuntimeException invalid) {
            throw new BlobException(BlobException.Code.BLOB_POLICY_INVALID);
        }
    }
    private static String text(JsonNode n, String key) {
        JsonNode value=n.required(key); if (!value.isTextual()) throw new IllegalArgumentException(); return value.textValue();
    }
    private static long integer(JsonNode n,String key) {
        JsonNode value=n.required(key); if (!value.isIntegralNumber() || !value.canConvertToLong()) throw new IllegalArgumentException(); return value.longValue();
    }
    private static boolean bool(JsonNode n,String key) {
        JsonNode value=n.required(key); if (!value.isBoolean()) throw new IllegalArgumentException(); return value.booleanValue();
    }
    private static Duration duration(JsonNode n, String key) { return Duration.ofSeconds(integer(n,key)); }
}
