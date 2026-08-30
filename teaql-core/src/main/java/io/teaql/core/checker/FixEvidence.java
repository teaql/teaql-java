package io.teaql.core.checker;

/** Safe, value-free provenance for one framework-managed Fix. */
public record FixEvidence(
        String entityType,
        String modelPath,
        Source source,
        String sourceLabel) {

    public enum Source { CLOCK, CONTEXT }

    public FixEvidence {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        if (modelPath == null || modelPath.isBlank()) {
            throw new IllegalArgumentException("modelPath must not be blank");
        }
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (sourceLabel == null || sourceLabel.isBlank()) {
            throw new IllegalArgumentException("sourceLabel must not be blank");
        }
        String normalized = sourceLabel.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("authorization") || normalized.contains("cookie")
                || normalized.contains("token=")) {
            throw new IllegalArgumentException("sourceLabel must be a safe framework label");
        }
    }
}
