package io.teaql.data.dynamic;

import java.util.Objects;

public final class DynamicFieldRef {

    private final DynamicFieldScope scope;
    private final String ownerType;
    private final String code;

    private DynamicFieldRef(DynamicFieldScope scope, String ownerType, String code) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType");
        this.code = Objects.requireNonNull(code, "code");
    }

    public static DynamicFieldRef of(DynamicFieldScope scope, String ownerType, String code) {
        return new DynamicFieldRef(scope, ownerType, code);
    }

    public DynamicFieldScope scope() {
        return scope;
    }

    public String ownerType() {
        return ownerType;
    }

    public String code() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicFieldRef that)) return false;
        return scope.equals(that.scope) && ownerType.equals(that.ownerType) && code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, ownerType, code);
    }

    @Override
    public String toString() {
        return "DynamicFieldRef{" + scope + '/' + ownerType + '/' + code + '}';
    }
}
