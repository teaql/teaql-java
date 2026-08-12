package io.teaql.runtime;

public record AuditFieldChange(String field, Object oldValue, Object newValue) {}
