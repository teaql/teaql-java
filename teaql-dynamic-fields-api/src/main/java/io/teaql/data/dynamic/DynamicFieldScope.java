package io.teaql.data.dynamic;

import java.util.Objects;

public final class DynamicFieldScope {

    private final String scopeType;
    private final String scopeId;

    private DynamicFieldScope(String scopeType, String scopeId) {
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType");
        this.scopeId = Objects.requireNonNull(scopeId, "scopeId");
    }

    public static DynamicFieldScope of(String scopeType, String scopeId) {
        return new DynamicFieldScope(scopeType, scopeId);
    }

    public static DynamicFieldScope global() {
        return new DynamicFieldScope("GLOBAL", "default");
    }

    public String scopeType() {
        return scopeType;
    }

    public String scopeId() {
        return scopeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicFieldScope that)) return false;
        return scopeType.equals(that.scopeType) && scopeId.equals(that.scopeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopeType, scopeId);
    }

    @Override
    public String toString() {
        return "DynamicFieldScope{" + scopeType + '/' + scopeId + '}';
    }
}
