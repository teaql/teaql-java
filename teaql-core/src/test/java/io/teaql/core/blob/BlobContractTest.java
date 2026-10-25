package io.teaql.core.blob;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class BlobContractTest {
    @Test public void sharedKeysAndAllFormatMappingsMatch() throws Exception {
        try (var input = getClass().getResourceAsStream("/blob-value-cases.json")) {
            var cases = new ObjectMapper().readTree(input);
            for (var value : cases.get("valid")) {
                var reference = BlobRef.of(value.textValue());
                Assert.assertEquals(value.textValue(), reference.key());
                Assert.assertEquals(reference, BlobRef.of(value.textValue()));
                Assert.assertFalse(reference.toString().contains(reference.key()));
            }
            for (var value : cases.get("invalid")) {
                Assert.assertThrows(IllegalArgumentException.class, () -> BlobRef.of(value.textValue()));
            }
            Assert.assertThrows(IllegalArgumentException.class, () -> BlobRef.of(null));
            var formats = cases.get("formats").fields();
            while (formats.hasNext()) {
                var pair = formats.next();
                var spec = BlobFieldSpec.of("Asset", "document", List.of(pair.getKey()), true, "default");
                Assert.assertEquals(Set.of(pair.getValue().textValue()), spec.allowedContentTypes());
            }
        }
    }

    @Test public void fieldContractIsImmutableSortedAndRejectsUnconstrainedFormats() {
        var spec = BlobFieldSpec.of("Asset", "document", List.of("pdf", "docx", "pdf"), false, "document");
        Assert.assertEquals(List.of("docx", "pdf"), List.copyOf(spec.allowedExtensions()));
        Assert.assertEquals("blob.v1", spec.protocolVersion());
        Assert.assertEquals(1, spec.maxFiles());
        Assert.assertThrows(UnsupportedOperationException.class, () -> spec.allowedExtensions().clear());
        Assert.assertThrows(IllegalArgumentException.class, () -> BlobFieldSpec.of("Asset", "document", List.of(), true, "default"));
        Assert.assertThrows(IllegalArgumentException.class, () -> BlobFieldSpec.of("Asset", "document", List.of("DOCX"), true, "default"));
    }
}
