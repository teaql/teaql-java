package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlobBindingIntentsTest {
    private static final EntityKey TARGET = new EntityKey("Asset", 42L);

    @Test public void onlyApprovedChangedBlobFieldsProduceUpdateIntents() {
        Fixture f = new Fixture();
        assertTrue(f.parse(MutationOperationKind.UPDATE, Map.of("title", "new", "#extension", "value")).mutations().isEmpty());
        BlobRef next = BlobRef.of("blob_next");
        var plan = f.parse(MutationOperationKind.UPDATE, Map.of("document", next, "title", "new"));
        assertEquals("approved-execution", plan.executionId());
        var mutation = plan.mutations().get(0);
        assertEquals(Map.of("document", next), mutation.assignments());
        assertEquals(Set.of("document"), mutation.fields().keySet());
        assertEquals(TARGET, mutation.entity());
        assertEquals(Long.valueOf(7), mutation.originalVersion());
        assertThrows(UnsupportedOperationException.class, () -> mutation.assignments().put("document", null));
        assertThrows(UnsupportedOperationException.class, () -> mutation.fields().clear());
        assertThrows(UnsupportedOperationException.class, () -> plan.mutations().clear());
        f.document.setName("changed-after-planning");
        assertEquals("document", mutation.fields().get("document").field());
    }

    @Test public void explicitClearingIsDifferentFromAbsenceAndNeverAcceptsWireStrings() {
        Fixture f = new Fixture();
        var changed = new HashMap<String, Object>(); changed.put("document", null);
        var mutation = f.parse(MutationOperationKind.UPDATE, changed).mutations().get(0);
        assertTrue(mutation.assignments().containsKey("document"));
        assertNull(mutation.assignments().get("document"));
        for (Object invalid : List.of("blob_key", "https://example.invalid/file.pdf", Map.of("key", "blob_key"), 42L)) {
            assertEquals(BlobException.Code.BLOB_REFERENCE_INVALID, assertThrows(BlobException.class,
                    () -> f.parse(MutationOperationKind.UPDATE, Map.of("document", invalid))).code());
        }
        assertEquals(BlobException.Code.BLOB_BIND_DENIED, assertThrows(BlobException.class,
                () -> f.parse(MutationOperationKind.UPDATE, Collections.singletonMap("required", null))).code());
    }

    @Test public void createRequiresRealKeyAndMakesOnlyNullableInitialNulls() {
        Fixture f = new Fixture();
        assertEquals(BlobException.Code.BLOB_BIND_DENIED, assertThrows(BlobException.class,
                () -> f.parse(MutationOperationKind.CREATE, Map.of("title", "new"))).code());
        BlobRef key = BlobRef.of("blob_created");
        var mutation = f.parse(MutationOperationKind.CREATE, Map.of("required", key)).mutations().get(0);
        assertEquals(Set.of("document", "required"), mutation.assignments().keySet());
        assertNull(mutation.assignments().get("document"));
        assertEquals(key, mutation.assignments().get("required"));
    }

    @Test public void inheritedDeleteAndRecoveryRequestAuthoritativeResolutionWithoutInventingOldKeys() {
        Fixture f = new Fixture();
        for (var kind : List.of(MutationOperationKind.DELETE, MutationOperationKind.RECOVER)) {
            var mutation = f.parse(kind, Map.of()).mutations().get(0);
            assertEquals(kind, mutation.kind());
            assertEquals(Set.of("document", "required"), mutation.fields().keySet());
            assertTrue(mutation.assignments().isEmpty());
        }
        BlobRef next = BlobRef.of("blob_recovered");
        var mutation = f.parse(MutationOperationKind.RECOVER, Map.of("document", next)).mutations().get(0);
        assertEquals(Map.of("document", next), mutation.assignments());
        assertFalse(mutation.assignments().containsKey("required"));
    }

    @Test public void blobTypeWithoutStructuredMetadataFailsClosed() {
        Fixture f = new Fixture(); f.owner.addSimpleProperty("broken", BlobRef.class);
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID, assertThrows(BlobException.class,
                () -> f.parse(MutationOperationKind.UPDATE, Map.of("broken", BlobRef.of("blob_next")))).code());
    }

    private static final class Fixture {
        final SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        final EntityDescriptor owner = new EntityDescriptor();
        final PropertyDescriptor document;
        Fixture() {
            owner.setType("AssetBase");
            document = owner.addSimpleProperty("document", BlobRef.class)
                    .withBlobFieldSpec(BlobFieldSpec.of("AssetBase", "document", List.of("pdf"), true, "default"));
            owner.addSimpleProperty("required", BlobRef.class)
                    .withBlobFieldSpec(BlobFieldSpec.of("AssetBase", "required", List.of("pdf"), false, "default"));
            EntityDescriptor child = new EntityDescriptor(); child.setType("Asset"); child.setParent(owner);
            child.addSimpleProperty("title", String.class); metadata.register(child);
        }
        BlobBindingIntents parse(MutationOperationKind kind, Map<String, Object> changes) {
            return BlobBindingIntents.from(metadata, new MutationPlan("approved-execution", "Asset.saveGraph", "Asset",
                    "replace asset", List.of(new MutationOperation(kind, TARGET, 7L, changes))));
        }
    }
}
