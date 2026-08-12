package io.teaql.runtime;

public record SafeAuditField(
        String name,
        String value,
        boolean masked,
        boolean truncated,
        Integer rawLength,
        Integer outputLength) {}
