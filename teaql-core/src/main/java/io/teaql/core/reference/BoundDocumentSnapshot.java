package io.teaql.core.reference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** tqd2 document authority: the original snapshot plus encrypted Blob binding facts. */
public record BoundDocumentSnapshot(
        DocumentSnapshot snapshot, List<DocumentBlobBindingSnapshot> bindings) {
    private static final Comparator<DocumentBlobBindingSnapshot> ORDER = (left, right) -> {
        int row = utf8Compare(left.rowKey(), right.rowKey());
        return row != 0 ? row : utf8Compare(left.canonicalField(), right.canonicalField());
    };

    public BoundDocumentSnapshot {
        Objects.requireNonNull(snapshot, "snapshot");
        bindings = List.copyOf(bindings);
        if (bindings.size() > 10000) throw projection();
        Map<String, DocumentSnapshot.Entity> entities = new HashMap<>();
        for (var entity : snapshot.entities()) entities.put(entity.rowKey(), entity);
        var canonical = new ArrayList<>(bindings);
        canonical.sort(ORDER);
        DocumentBlobBindingSnapshot previous = null;
        for (var binding : canonical) {
            var entity = entities.get(binding.rowKey());
            if (entity == null || !entity.identity().entityType().equals(binding.entityType())
                    || !entity.loadedFields().contains(binding.canonicalField())
                    || binding.writable()
                            && !entity.writableFields().contains(binding.canonicalField())
                    || previous != null && ORDER.compare(previous, binding) == 0) {
                throw projection();
            }
            previous = binding;
        }
        bindings = List.copyOf(canonical);
    }

    @Override public String toString() { return "BoundDocumentSnapshot[redacted]"; }

    private static int utf8Compare(String left, String right) {
        byte[] a = left.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = right.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int index = 0; index < Math.min(a.length, b.length); index++) {
            int difference = Byte.toUnsignedInt(a[index]) - Byte.toUnsignedInt(b[index]);
            if (difference != 0) return difference;
        }
        return Integer.compare(a.length, b.length);
    }

    private static RoundTripReferenceException projection() {
        return new RoundTripReferenceException(
                RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                "DOCUMENT_PROJECTION_VIOLATION");
    }
}
