package io.teaql.core.blob;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/** Model restrictions only; server profiles may narrow these formats. */
public record BlobFieldSpec(String entity, String field, SortedSet<String> allowedExtensions,
                            boolean nullable, String policyProfile) {
    public static final String CAPABILITY = "blob.v1";
    private static final Map<String, String> MIME_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
            "mp4", "video/mp4", "mp3", "audio/mpeg", "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "pdf", "application/pdf");

    public BlobFieldSpec {
        if (entity == null || entity.isBlank() || field == null || field.isBlank()
                || policyProfile == null || policyProfile.isBlank()) {
            throw new IllegalArgumentException("BLOB_POLICY_INVALID");
        }
        Objects.requireNonNull(allowedExtensions, "allowedExtensions");
        if (allowedExtensions.isEmpty() || allowedExtensions.stream().anyMatch(e -> !MIME_TYPES.containsKey(e))) {
            throw new IllegalArgumentException("BLOB_FORMAT_NOT_ALLOWED");
        }
        allowedExtensions = Collections.unmodifiableSortedSet(new TreeSet<>(allowedExtensions));
    }

    public static BlobFieldSpec of(String entity, String field, Collection<String> extensions,
                                   boolean nullable, String profile) {
        return new BlobFieldSpec(entity, field, new TreeSet<>(extensions), nullable, profile);
    }

    public SortedSet<String> allowedContentTypes() {
        SortedSet<String> result = new TreeSet<>();
        allowedExtensions.forEach(e -> result.add(MIME_TYPES.get(e)));
        return Collections.unmodifiableSortedSet(result);
    }

    public String protocolVersion() { return CAPABILITY; }
    public int maxFiles() { return 1; }
}
