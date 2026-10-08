package io.teaql.data.dynamic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable runtime-defined names/types, shared by compatible rows. No numeric slots. */
public final class DynamicFieldMetadata {
    private final Map<String, DynamicDataType> types;
    private final Object storageIdentity;
    private final String scopeType;
    private final String scopeId;
    private final String ownerType;
    // Value-free immutable absence wrappers, bounded by this definition snapshot's known names.
    private final java.util.concurrent.ConcurrentMap<String, DynamicFieldValue> absent = new java.util.concurrent.ConcurrentHashMap<>();

    public DynamicFieldMetadata(Map<String, DynamicDataType> types) {
        this.types = Collections.unmodifiableMap(new LinkedHashMap<>(types));
        storageIdentity = null; scopeType = null; scopeId = null; ownerType = null;
    }

    private DynamicFieldMetadata(DynamicFieldMetadata source, Object identity, String scopeType, String scopeId, String ownerType) {
        types = source.types;
        storageIdentity = java.util.Objects.requireNonNull(identity);
        this.scopeType = scopeType; this.scopeId = scopeId; this.ownerType = ownerType;
    }

    /** Trusted provider provenance only; no business meaning and no numeric dynamic slots. */
    public DynamicFieldMetadata withStorageBinding(Object identity, String scopeType, String scopeId, String ownerType) {
        java.util.Objects.requireNonNull(identity, "storage identity");
        if (matchesStorage(identity, scopeType, scopeId, ownerType)) return this;
        return new DynamicFieldMetadata(this, identity, scopeType, scopeId, ownerType);
    }

    public boolean matchesStorage(Object identity, String scopeType, String scopeId, String ownerType) {
        return storageIdentity != null && storageIdentity == identity && java.util.Objects.equals(this.scopeType, scopeType)
                && java.util.Objects.equals(this.scopeId, scopeId) && java.util.Objects.equals(this.ownerType, ownerType);
    }

    public static DynamicFieldMetadata fromDefinitions(List<DynamicFieldDef> definitions) {
        Map<String, DynamicDataType> types = new LinkedHashMap<>();
        for (DynamicFieldDef definition : definitions) {
            if (definition.isActive()) types.put(definition.getCode(), definition.getDataType());
        }
        return new DynamicFieldMetadata(types);
    }

    public DynamicDataType requireType(String code) {
        if (!types.containsKey(code)) throw DynamicFieldException.notFound(code);
        return types.get(code);
    }

    public Map<String, DynamicDataType> types() { return types; }

    public DynamicFieldValue notLoaded(String code) {
        DynamicDataType type = requireType(code);
        DynamicFieldValue cached = absent.get(code);
        if (cached != null) return cached;
        return absent.computeIfAbsent(code, name -> DynamicFieldValue.notLoaded(name, type));
    }
}
