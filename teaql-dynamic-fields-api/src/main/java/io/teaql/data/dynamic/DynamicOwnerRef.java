package io.teaql.data.dynamic;

import java.util.Objects;

public final class DynamicOwnerRef {

    private final String ownerType;
    private final long ownerId;

    private DynamicOwnerRef(String ownerType, long ownerId) {
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType");
        this.ownerId = ownerId;
    }

    public static DynamicOwnerRef of(String ownerType, long ownerId) {
        return new DynamicOwnerRef(ownerType, ownerId);
    }

    public String ownerType() {
        return ownerType;
    }

    public long ownerId() {
        return ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicOwnerRef that)) return false;
        return ownerId == that.ownerId && ownerType.equals(that.ownerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerType, ownerId);
    }

    @Override
    public String toString() {
        return "DynamicOwnerRef{" + ownerType + '#' + ownerId + '}';
    }
}
