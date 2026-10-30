package io.teaql.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.teaql.core.blob.BlobRef;
import java.io.IOException;

/** Pure stable-key encoding; never signs a URL. */
final class BlobRefJsonSerializer extends JsonSerializer<BlobRef> {
    @Override public void serialize(BlobRef value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeString(value.key());
    }
}
