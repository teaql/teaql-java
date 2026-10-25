package io.teaql.blob.reference;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobCommandService.EntityRevision;
import java.util.*;

/** Closed metadata-only receipt codec. No reflective object/POJO serialization. */
final class BlobCommandReceiptJson {
    private static final ObjectMapper JSON=new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(8).maxStringLength(256).maxNumberLength(19).build()).build());
    private BlobCommandReceiptJson() {}
    static String encode(List<EntityRevision> entities) {
        var node=JSON.createObjectNode(); node.put("protocol","blob.command.receipt.v1"); var array=node.putArray("entities");
        for (var entity:entities) { var item=array.addObject(); item.put("entity",entity.entity()); item.put("id",entity.id()); item.put("revision",entity.revision()); }
        return node.toString();
    }
    static List<EntityRevision> decode(String encoded) {
        try {
            if (encoded==null || encoded.length()>1048576) throw invalid();
            JsonNode node;
            try (var parser=JSON.createParser(encoded)) { node=JSON.readTree(parser); if (parser.nextToken()!=null) throw invalid(); }
            if (node==null || !node.isObject() || node.size()!=2 || !node.path("protocol").isTextual() || !node.path("protocol").asText().equals("blob.command.receipt.v1")) throw invalid();
            var items=node.path("entities"); if (!items.isArray() || items.size()>1000) throw invalid();
            List<EntityRevision> result=new ArrayList<>();
            for (var item:items) {
                if (!item.isObject() || item.size()!=3 || !item.path("entity").isTextual() || !item.path("id").isIntegralNumber() || !item.path("id").canConvertToLong()
                        || !item.path("revision").isIntegralNumber() || !item.path("revision").canConvertToLong()) throw invalid();
                result.add(new EntityRevision(item.path("entity").asText(),item.path("id").longValue(),item.path("revision").longValue()));
            }
            return List.copyOf(result);
        } catch (java.io.IOException | RuntimeException malformed) { throw invalid(); }
    }
    private static BlobException invalid() { return new BlobException(BlobException.Code.BLOB_CATALOG_FAILED); }
}
