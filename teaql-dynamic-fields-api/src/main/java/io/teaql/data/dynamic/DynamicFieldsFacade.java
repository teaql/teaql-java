package io.teaql.data.dynamic;

public interface DynamicFieldsFacade {

    DynamicFieldsFacade withContext(Object userContext);

    DynamicFieldsFacade purpose(String purpose);

    DynamicFieldsFacade comment(String comment);

    OwnerBound owner(String ownerType, long ownerId);

    /** Prepare definitions for new entities, without an owner ID or owner-data query. */
    default DynamicFieldMetadata metadata(String ownerType) {
        throw new UnsupportedOperationException("Dynamic field definition preparation is not supported");
    }

    /** Framework graph participant; not an independent save entry point. */
    default void prepareGraphMutations(Object resource, java.util.List<DynamicGraphMutation> mutations) {
        throw new DynamicFieldException("DYNAMIC_FIELD_TRANSACTION_BINDING_REQUIRED", "Dynamic field provider is not bound to the graph transaction");
    }

    default void applyGraphMutations(Object resource, java.util.List<DynamicGraphMutation> mutations) {
        throw new DynamicFieldException("DYNAMIC_FIELD_TRANSACTION_BINDING_REQUIRED", "Dynamic field provider is not bound to the graph transaction");
    }

    /** Explicit bulk read; custom facades may override to preserve their permission boundary. */
    default java.util.Map<DynamicOwnerRef, DynamicFieldValues> readAll(
            java.util.List<DynamicOwnerRef> owners, DynamicFieldSelection selection) {
        java.util.Map<DynamicOwnerRef, DynamicFieldValues> result = new java.util.LinkedHashMap<>();
        for (DynamicOwnerRef owner : owners) result.put(owner, owner(owner.ownerType(), owner.ownerId()).readAll(selection));
        return result;
    }

    interface OwnerBound {
        StringFieldBound string(String fieldCode);
        NumberFieldBound number(String fieldCode);
        BoolFieldBound bool(String fieldCode);
        DynamicFieldValues readAll(DynamicFieldSelection selection);
    }

    interface StringFieldBound {
        void set(String value);
        String get();
    }

    interface NumberFieldBound {
        void set(Number value);
        Number get();
    }

    interface BoolFieldBound {
        void set(Boolean value);
        Boolean get();
    }
}
