package io.teaql.data.dynamic;

import java.util.Objects;

public final class DynamicValueRef {

    private final DynamicOwnerRef ownerRef;
    private final long fieldId;

    private DynamicValueRef(DynamicOwnerRef ownerRef, long fieldId) {
        this.ownerRef = Objects.requireNonNull(ownerRef, "ownerRef");
        this.fieldId = fieldId;
    }

    public static DynamicValueRef of(DynamicOwnerRef ownerRef, long fieldId) {
        return new DynamicValueRef(ownerRef, fieldId);
    }

    public DynamicOwnerRef ownerRef() {
        return ownerRef;
    }

    public long fieldId() {
        return fieldId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicValueRef that)) return false;
        return fieldId == that.fieldId && ownerRef.equals(that.ownerRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerRef, fieldId);
    }

    @Override
    public String toString() {
        return "DynamicValueRef{" + ownerRef + "/field=" + fieldId + '}';
    }
}
