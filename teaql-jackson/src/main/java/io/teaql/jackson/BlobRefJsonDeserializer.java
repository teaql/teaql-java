package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.teaql.core.blob.BlobRef;
import java.io.IOException;

final class BlobRefJsonDeserializer extends JsonDeserializer<BlobRef> {
    @Override public BlobRef deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return context.reportInputMismatch(BlobRef.class, "BLOB_REFERENCE_INVALID");
        }
        try { return BlobRef.of(parser.getText()); }
        catch (IllegalArgumentException error) {
            return context.reportInputMismatch(BlobRef.class, "BLOB_REFERENCE_INVALID");
        }
    }
}
