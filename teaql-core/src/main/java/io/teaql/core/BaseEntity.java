package io.teaql.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.teaql.core.utils.ObjectUtil;
import io.teaql.data.dynamic.DynamicFieldValue;
import io.teaql.data.dynamic.DynamicFieldValues;

public class BaseEntity implements Entity {
    public static final String ID_PROPERTY = "id";
    public static final String VERSION_PROPERTY = "version";
    private Long id;
    private Long version;

    private EntityStatus $status = EntityStatus.NEW;

    private String subType;

    private String displayName;

    private static final Map<String, PropertyChange> NO_UPDATES = Map.of();
    private Map<String, PropertyChange> updatedProperties = NO_UPDATES;

    private LoadState loadState = FieldLayout.forType(getClass()).emptyState();
    private boolean hydratingProperty;

    private Map<String, Object> additionalInfo = Map.of();

    private DynamicFieldValues dynamicFieldValues;
    private Map<String, io.teaql.data.dynamic.DynamicFieldMutation> dynamicMutations = Map.of();
    private Map<String, DynamicFieldValue> dynamicOriginalValues = Map.of();

    // Query-only sidecar. Never a model property or mutation-ledger entry.
    private transient Map<String, SmartList<?>> queryFacets = Map.of();

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Map<String, SmartList<?>> getQueryFacets() {
        return queryFacets;
    }

    @FrameworkInternal
    public void __internalSetQueryFacets(Map<String, ? extends SmartList<?>> facets) {
        queryFacets = facets == null || facets.isEmpty() ? Map.of() : Map.copyOf(facets);
    }

    private Map<String, Entity> relationCache = Map.of();

    private List<Object> actionList;

    private String _comment;

    /**
     * Shared change tracking root for the entire entity graph.
     */
    private EntityMutationLedger entityMutationLedger;
    // Database baseline is a row-owned scalar, not a reason to allocate a ledger.
    private Long hydratedOriginalVersion;

    @Override
    public String getComment() {
        return _comment;
    }

    @Override
    public void setComment(String comment) {
        this._comment = comment;
        if (entityMutationLedger != null) {
            entityMutationLedger.setComment(comment);
        }
    }

    private List<TraceNode> _traceChain = List.of();

    @Override
    public List<TraceNode> getTraceChain() {
        return _traceChain;
    }

    @Override
    public void setTraceChain(List<TraceNode> traceChain) {
        this._traceChain = traceChain == null ? List.of() : List.copyOf(traceChain);
        if (entityMutationLedger != null && id != null) {
            entityMutationLedger.setTraceChain(new EntityKey(typeName(), id), this._traceChain);
        }
    }

    public EntityStatus get$status() {
        return $status;
    }

    public void set$status(EntityStatus p$status) {
        $status = p$status;
    }

    @Override
    public Long getId() {
        return id;
    }

    public BaseEntity updateId(Long id) {
		markPropertyLoaded(ID_PROPERTY);
        if (ObjectUtil.equals(this.id, id)) return this;
        handleUpdate(ID_PROPERTY, getId(), id);
        this.id = id;
        return this;
    }

    @FrameworkInternal("Generated schema bootstrap fixed-ID creation only")
    public void __internalInitializeNewEntityId(Long fixedId) {
        if (fixedId == null || fixedId <= 0) {
            throw new IllegalArgumentException("Generated bootstrap ID must be positive");
        }
        if (id != null || !newItem()) {
            throw new IllegalStateException("Fixed bootstrap ID can only initialize a new entity");
        }
        id = fixedId;
        markPropertyLoaded(ID_PROPERTY);
        getEntityMutationLedger().markAsNew(new EntityKey(typeName(), fixedId));
    }

    @Override
    public Long getVersion() {
        return version;
    }

    public BaseEntity updateVersion(Long version) {
		markPropertyLoaded(VERSION_PROPERTY);
        if (ObjectUtil.equals(this.version, version)) return this;
        handleUpdate(VERSION_PROPERTY, getVersion(), version);
        this.version = version;
        return this;
    }

    @FrameworkInternal("Business code must use updateXxx() methods")
    public void __internalSet(String property, Object value) {
		markPropertyLoaded(property);
        switch (property) {
            case "id":      this.id = (Long) value; break;
            case "version": this.version = (Long) value; break;
            default:
                throw new IllegalArgumentException(typeName() + " has no property: " + property);
        }
    }

    @FrameworkInternal("Mutation transaction rollback only")
    public void __internalRestorePersistenceState(
            Long restoredVersion, EntityStatus restoredStatus, boolean versionWasLoaded) {
        this.version = restoredVersion;
        this.$status = restoredStatus;
        if (hydratedOriginalVersion != null) hydratedOriginalVersion = restoredVersion;
        restorePropertyLoaded(VERSION_PROPERTY, versionWasLoaded);
    }

    @FrameworkInternal("Business code should use typed getXxx() methods")
    public Object __internalGet(String property) {
        // First try to get from entityMutationLedger if available
        if (entityMutationLedger != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            Object value = entityMutationLedger.get(key, property);
            if (value != null) {
                return value;
            }
        }
        // Fall back to direct field access
        switch (property) {
            case "id":      return this.id;
            case "version": return this.version;
            default:
                throw new IllegalArgumentException(typeName() + " has no property: " + property);
        }
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String pSubType) {
        subType = pSubType;
    }

    public List<Object> getActionList() {
        return actionList;
    }

    public void setActionList(List<Object> pActionList) {
        actionList = pActionList;
    }

    @Override
    public String runtimeType() {
        if (subType == null) {
            return Entity.super.runtimeType();
        }
        return subType;
    }

    @Override
    public void setRuntimeType(String runtimeType) {
        setSubType(runtimeType);
    }

    @Override
    public boolean newItem() {
        return $status == EntityStatus.NEW;
    }

    @Override
    public boolean updateItem() {
        return $status == EntityStatus.UPDATED;
    }

    @Override
    public boolean deleteItem() {
        return $status == EntityStatus.UPDATED_DELETED;
    }

    @Override
    public boolean needPersist() {
        return $status == EntityStatus.NEW
                || $status == EntityStatus.UPDATED
                || $status == EntityStatus.UPDATED_DELETED
                || $status == EntityStatus.UPDATED_RECOVER;
    }

    @Override
    public List<String> getUpdatedProperties() {
        if (entityMutationLedger != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            Set<String> rootChanges = entityMutationLedger.changedFieldNames(key);
            if (rootChanges != null && !rootChanges.isEmpty()) {
                // Native SQL/checker callers consume predefined properties.
                // dirtyFields()/the ledger expose # extension intents separately.
                var nativeChanges = new ArrayList<String>();
                for (String field : rootChanges) if (!field.startsWith("#")) nativeChanges.add(field);
                if (nativeChanges.isEmpty()) return new ArrayList<>(updatedProperties.keySet());
                if (updatedProperties.containsKey(VERSION_PROPERTY) && !nativeChanges.contains(VERSION_PROPERTY)) nativeChanges.add(VERSION_PROPERTY);
                return nativeChanges;
            }
        }
        return new ArrayList<>(updatedProperties.keySet());
    }

    @Override
    @Deprecated
    public void addRelation(String relationName, Entity value) {
        io.teaql.core.meta.EntityMetaFactory metadata =
                io.teaql.core.meta.EntityMetaFactory.get();
        if (metadata == null) {
            throw new IllegalStateException("Global EntityMetaFactory is not initialized; "
                    + "use addRelation(context, relationName, value)");
        }
        io.teaql.core.spi.InternalLogger.getLogger(BaseEntity.class).warn(
                "Deprecated addRelation(name, value) uses process-global metadata and may "
                        + "select another runtime's model; use addRelation(context, name, value)");
        addRelation(metadata.resolveEntityDescriptor(this.typeName()), relationName, value);
    }

    /** Attach a relation using metadata installed in the invoking context. */
    @Override
    public void addRelation(UserContext context, String relationName, Entity value) {
        io.teaql.core.meta.EntityDescriptor descriptor =
                io.teaql.core.meta.EntityMetaFactory.requireFrom(context)
                        .resolveEntityDescriptor(this.typeName());
        if (descriptor == null) {
            throw new IllegalStateException("No entity descriptor registered in the invoking context for "
                    + this.typeName());
        }
        io.teaql.core.meta.PropertyDescriptor property = descriptor.findProperty(relationName);
        if (property == null || property.getType() == null) {
            throw new IllegalArgumentException("No relation " + this.typeName() + "." + relationName
                    + " registered in the invoking context");
        }
        Class<?> relationType = property.getType().javaType();
        if (!SmartList.class.isAssignableFrom(relationType)
                && !Entity.class.isAssignableFrom(relationType)) {
            throw new IllegalArgumentException("Property " + this.typeName() + "." + relationName
                    + " is not a relation in the invoking context");
        }
        addRelation(descriptor, relationName, value);
    }

    private void addRelation(
            io.teaql.core.meta.EntityDescriptor descriptor, String relationName, Entity value) {
        if (descriptor == null) return;
        io.teaql.core.meta.PropertyDescriptor pd = descriptor.findProperty(relationName);
        if (pd == null || pd.getType() == null) return;
        Class<?> type = pd.getType().javaType();
        if (SmartList.class.isAssignableFrom(type)) {
            SmartList existing = getProperty(relationName);
            if (existing == null) {
                existing = new SmartList<>();
                setProperty(relationName, existing);
            }
            existing.add(value);
        } else if (Entity.class.isAssignableFrom(type)) {
            setProperty(relationName, value);
        }
    }

    @Override
    public void addDynamicProperty(String propertyName, Object value) {
        String key = dynamicPropertyNameOf(propertyName);
        if (loadState.dynamicPropertyMetadata() != null) loadState.dynamicPropertyMetadata().validate(key, value);
        mutableAdditionalInfo().put(key, value);
    }

    @Override
    public void appendDynamicProperty(String propertyName, Object value) {
        String key = dynamicPropertyNameOf(propertyName);
        List<Object> existing = (List<Object>) additionalInfo.get(key);
        if (existing == null) {
            existing = new ArrayList<>();
            mutableAdditionalInfo().put(key, existing);
        }
        existing.add(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getDynamicProperty(String propertyName) {
        return (T) additionalInfo.get(dynamicPropertyNameOf(propertyName));
    }

    public boolean hasDynamicProperty(String propertyName) {
        return additionalInfo.containsKey(dynamicPropertyNameOf(propertyName));
    }

    @Override
    public Class<?> getDynamicPropertyType(String propertyName) {
        DynamicPropertyMetadata metadata = loadState.dynamicPropertyMetadata();
        return metadata == null ? null : metadata.type(dynamicPropertyNameOf(propertyName));
    }

    private String dynamicPropertyNameOf(String propertyName) {
        if (propertyName == null || propertyName.isBlank() || propertyName.startsWith("#")) {
            throw new IllegalArgumentException("Dynamic properties use '_' names; '#' belongs to persistent dynamic fields");
        }
        if (propertyName.startsWith("_")) {
            return propertyName;
        }
        return "_" + propertyName;
    }

    @Override
    public BaseEntity markForDeletion() {
        gotoNextStatus(EntityAction.DELETE);
        if (id != null) {
            getEntityMutationLedger().markAsDelete(new EntityKey(typeName(), id));
        }
        return this;
    }

    @Override
    public void markAsRecover() {
        gotoNextStatus(EntityAction.RECOVER);
        if (id != null) {
            getEntityMutationLedger().markAsRecover(new EntityKey(typeName(), id));
        }
    }

    @Override
    public boolean recoverItem() {
        return $status == EntityStatus.UPDATED_RECOVER;
    }

    public void clearUpdatedProperties() {
        if (!updatedProperties.isEmpty()) updatedProperties.clear();
    }

    public void addAction(Object action) {
        synchronized (this) {
            if (actionList == null) {
                actionList = new ArrayList<>();
            }
        }
        actionList.add(action);
    }

    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        try {
            Object name = getProperty("name");
            if (name != null) {
                return String.valueOf(name);
            }
            Object title = getProperty("title");
            if (title != null) {
                return String.valueOf(title);
            }
        } catch (Exception ignored) {
        }
        return typeName() + ":" + getId();
    }

    public void setDisplayName(String pDisplayName) {
        displayName = pDisplayName;
    }

    // --- EntityMutationLedger integration ---

    public EntityMutationLedger getEntityMutationLedger() {
        if (entityMutationLedger == null) {
            entityMutationLedger = new EntityMutationLedger();
            entityMutationLedger.setComment(_comment);
            if (id != null) {
                EntityKey key = new EntityKey(typeName(), id);
                // New-entity intent is installed by its explicit lifecycle entry
                // point, not invented merely by requesting an empty ledger.
                if (hydratedOriginalVersion != null) entityMutationLedger.setOriginalVersion(key, hydratedOriginalVersion);
                if (!_traceChain.isEmpty()) entityMutationLedger.setTraceChain(key, _traceChain);
            }
        }
        return entityMutationLedger;
    }

    @FrameworkInternal("Allocation and hydration qualification only")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean __internalHasMutationLedger() { return entityMutationLedger != null; }

    public void setEntityMutationLedger(EntityMutationLedger entityMutationLedger) {
        this.entityMutationLedger = entityMutationLedger;
        if (entityMutationLedger != null && id != null && newItem()) {
            entityMutationLedger.markAsNew(new EntityKey(typeName(), id));
        }
        if (entityMutationLedger != null && id != null && version != null) {
            entityMutationLedger.setOriginalVersion(new EntityKey(typeName(), id), version);
        }
    }

    public Set<String> dirtyFields() {
        if (entityMutationLedger != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            Set<String> fields = entityMutationLedger.changedFieldNames(key);
            if (fields != null && !fields.isEmpty()) {
                return fields;
            }
        }
        return updatedProperties.isEmpty() ? null : updatedProperties.keySet();
    }

    public boolean isMarkedAsDelete() {
        if (entityMutationLedger == null || id == null) {
            return deleteItem();
        }
        return entityMutationLedger.isMarkedAsDelete(new EntityKey(typeName(), id));
    }

    public boolean isNew() {
        if (entityMutationLedger == null || id == null) {
            return newItem();
        }
        return entityMutationLedger.isNew(new EntityKey(typeName(), id));
    }

    public Long getOriginalVersion() {
        if (id == null) return null;
        if (entityMutationLedger == null) {
            return hydratedOriginalVersion;
        }
        return entityMutationLedger.getOriginalVersion(new EntityKey(typeName(), id));
    }

    @Override
    public void setProperty(String propertyName, Object value) {
		markPropertyLoaded(propertyName);
        this.__internalSet(propertyName, value);
    }

    @FrameworkInternal("Expression and hydration infrastructure only")
    public void markPropertyLoaded(String propertyName) {
		if (propertyName == null || hydratingProperty) return;
        loadState = loadState.withLoaded(propertyName, true);
	}

    public boolean isPropertyLoaded(String propertyName) {
		return loadState.isLoaded(propertyName);
	}

    @FrameworkInternal("Compiled hydration infrastructure only")
    public static int loadedPropertyIndex(Class<? extends BaseEntity> entityType, String propertyName) {
        return FieldLayout.forType(entityType).indexForLoad(propertyName);
    }

    @FrameworkInternal("Immutable loaded-state sharing and hydration only")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public LoadState __internalLoadState() { return loadState; }

    @FrameworkInternal("Immutable loaded-state sharing and hydration only")
    public void __internalUseLoadState(LoadState state) {
        if (state == null || state.layout() != FieldLayout.forType(getClass())) {
            throw new IllegalArgumentException("Incompatible entity load-state layout: " + typeName());
        }
        if (state.dynamicPropertyMetadata() != null) {
            additionalInfo.forEach((name, value) -> state.dynamicPropertyMetadata().validate(name, value));
        }
        loadState = state;
    }

    @FrameworkInternal("Compiled hydration infrastructure only")
    public void __internalHydrate(String propertyName, Object value, int loadedPropertyIndex) {
        if (loadState.layout().indexForLoad(propertyName) != loadedPropertyIndex) {
            throw new IllegalArgumentException("Hydration index disagrees with generated layout: " + propertyName);
        }
        hydratingProperty = true;
        try {
            __internalSet(propertyName, value);
        } finally {
            hydratingProperty = false;
        }
        loadState = loadState.withLoaded(propertyName, true);
        captureHydratedOriginalVersion();
    }

    /**
     * Hydration may assign {@code id} and {@code version} in either column order.
     * Once both are available, retain the authoritative database version in the
     * entity-owned Mutation Ledger without creating a business mutation.
     */
    private void captureHydratedOriginalVersion() {
        if (id == null || version == null) return;
        hydratedOriginalVersion = version;
        if (entityMutationLedger != null) entityMutationLedger.setOriginalVersion(new EntityKey(typeName(), id), version);
    }

    private void restorePropertyLoaded(String propertyName, boolean loaded) {
        loadState = loadState.withLoaded(propertyName, loaded);
    }

    @Override
    public Entity updateProperty(String propertyName, Object value) {
        Object oldValue = getProperty(propertyName);
        setProperty(propertyName, value);
        handleUpdate(propertyName, oldValue, value);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> P getProperty(String propertyName) {
        Entity o = this.relationCache.get(propertyName);
        if (o != null) {
            return (P) o;
        }
        if (propertyName != null && (propertyName.startsWith("_") || propertyName.startsWith("#"))) {
            return (P) additionalInfo.get(propertyName);
        }
        return Entity.super.getProperty(propertyName);
    }

    public void handleUpdate(String propertyName, Object oldValue, Object newValue) {
		markPropertyLoaded(propertyName);
        gotoNextStatus(EntityAction.UPDATE);
        PropertyChange propertyChange = updatedProperties.get(propertyName);
        if (propertyChange != null) {
            oldValue = propertyChange.getOldValue();
        }
        if (ObjectUtil.equals(oldValue, newValue)) {
            if (!updatedProperties.isEmpty()) updatedProperties.remove(propertyName);
            return;
        }
        mutableUpdatedProperties().put(propertyName, new PropertyChange(propertyName, oldValue, newValue));

        if (id != null) {
            EntityMutationLedger entityMutationLedger = getEntityMutationLedger();
            EntityKey key = new EntityKey(typeName(), id);
            entityMutationLedger.set(key, propertyName, newValue);
            if (!_traceChain.isEmpty()) {
                entityMutationLedger.setTraceChain(key, _traceChain);
            }
        }
    }

    private Map<String, PropertyChange> mutableUpdatedProperties() {
        if (updatedProperties == NO_UPDATES) updatedProperties = new ConcurrentHashMap<>();
        return updatedProperties;
    }

    public void gotoNextStatus(EntityAction action) {
        set$status(get$status().next(action));
    }

    public void cacheRelation(String relationName, Entity relation) {
		markPropertyLoaded(relationName);
        if (relationCache.isEmpty()) relationCache = new HashMap<>();
        relationCache.put(relationName, relation);
        Object initValue = getProperty(relationName);
        handleUpdate(relationName, initValue, relation);
    }

    public Object getOldValue(String propertyName) {
        PropertyChange propertyChange = updatedProperties.get(propertyName);
        if (propertyChange == null) return null;
        return propertyChange.getOldValue();
    }

    public Object getNewValue(String propertyName) {
        PropertyChange propertyChange = updatedProperties.get(propertyName);
        if (propertyChange == null) return null;
        return propertyChange.getNewValue();
    }

    public BaseEntity markToRecover() {
        markAsRecover();
        return this;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (pO == null || getClass() != pO.getClass()) return false;
        BaseEntity that = (BaseEntity) pO;
        return Objects.equals(getId(), that.getId()) && Objects.equals(typeName(), that.typeName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), typeName());
    }

    public Map<String, Object> getAdditionalInfo() {
        return mutableAdditionalInfo();
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo == null || additionalInfo.isEmpty()
                ? Map.of() : new HashMap<>(additionalInfo);
    }

    private Map<String, Object> mutableAdditionalInfo() {
        if (additionalInfo.isEmpty()) additionalInfo = new HashMap<>();
        return additionalInfo;
    }

    @Override
    public DynamicFieldValues dynamicFields() {
        return dynamicFieldValues == null ? collectDynamicFieldValues() : dynamicFieldValues;
    }

    public DynamicFieldValues getDynamicFieldValues() {
        return dynamicFieldValues;
    }

    public BaseEntity updateDynamicField(String code, Object value) {
        if (dynamicFieldValues == null) throw new io.teaql.data.dynamic.DynamicFieldException("DYNAMIC_FIELD_DEFINITIONS_MISSING", "Load dynamic field definitions before editing");
        var mutation = io.teaql.data.dynamic.DynamicFieldMutation.set(code, dynamicFieldValues.metadata().requireType(code), value);
        stageDynamicMutation(mutation);
        return this;
    }

    public BaseEntity deleteDynamicField(String code) {
        if (dynamicFieldValues == null) throw new io.teaql.data.dynamic.DynamicFieldException("DYNAMIC_FIELD_DEFINITIONS_MISSING", "Load dynamic field definitions before editing");
        stageDynamicMutation(io.teaql.data.dynamic.DynamicFieldMutation.delete(code, dynamicFieldValues.metadata().requireType(code)));
        return this;
    }

    private void stageDynamicMutation(io.teaql.data.dynamic.DynamicFieldMutation mutation) {
        EntityStatus nextStatus = get$status().next(EntityAction.UPDATE);
        Map<String, DynamicFieldValue> next = new HashMap<>(dynamicFieldValues.toMap());
        if (mutation.kind() == io.teaql.data.dynamic.DynamicFieldMutation.Kind.SET) next.put(mutation.code(), mutation.asLoadedValue());
        else next.remove(mutation.code());
        DynamicFieldValues values = new DynamicFieldValues(dynamicFieldValues.metadata(), next);
        LoadState state = loadState.withDynamicSelection(values.selectedCodes());
        if (dynamicOriginalValues.isEmpty()) dynamicOriginalValues = new HashMap<>();
        dynamicOriginalValues.putIfAbsent(mutation.code(), dynamicFieldValues.field(mutation.code()));
        __internalHydrateDynamicFields(values, state);
        if (dynamicMutations.isEmpty()) dynamicMutations = new HashMap<>();
        dynamicMutations.put(mutation.code(), mutation);
        set$status(nextStatus);
        if (id != null) getEntityMutationLedger().setDynamic(new EntityKey(typeName(), id), mutation);
    }

    @FrameworkInternal("Graph mutation planning only")
    public Map<String, io.teaql.data.dynamic.DynamicFieldMutation> __internalDynamicMutations() {
        return dynamicMutations.isEmpty() ? Map.of() : java.util.Collections.unmodifiableMap(dynamicMutations);
    }

    @FrameworkInternal("Invocation-local audit and log privacy provenance only")
    public Map<String, DynamicFieldValue> __internalDynamicOriginalValues() {
        return dynamicOriginalValues.isEmpty() ? Map.of() : java.util.Collections.unmodifiableMap(dynamicOriginalValues);
    }

    @FrameworkInternal("Successful graph commit only")
    public void __internalClearDynamicMutations() { dynamicMutations = Map.of(); dynamicOriginalValues = Map.of(); }

    @FrameworkInternal("Runtime optimistic guard for an extension-only update")
    public void __internalRequireVersionUpdate() {
        if (version == null) throw new TeaQLRuntimeException("Dynamic update requires a loaded optimistic version");
        mutableUpdatedProperties().put(VERSION_PROPERTY, new PropertyChange(VERSION_PROPERTY, version, version));
    }

    @FrameworkInternal("Replay validated ledger intent into a provider-owned mutation entity")
    public void __internalApplyRecordedMutation(String property, Object value) {
        Object before = __internalGet(property);
        __internalSet(property, value);
        // The blank provider entity is not the original loaded snapshot. Equal
        // defaults here must not discard an explicit NULL/zero/false intent.
        mutableUpdatedProperties().put(property, new PropertyChange(property, before, value));
    }

    public DynamicFieldValues collectDynamicFieldValues() {
        List<DynamicFieldValue> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : additionalInfo.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("#")) continue;
            String fieldCode = key.substring(1);
            Object value = entry.getValue();
            if (value instanceof String s) {
                fields.add(DynamicFieldValue.ofString(fieldCode, s));
                continue;
            }
            if (value instanceof Number n) {
                fields.add(DynamicFieldValue.ofNumber(fieldCode, n));
                continue;
            }
            if (value instanceof Boolean b) {
                fields.add(DynamicFieldValue.ofBool(fieldCode, b));
                continue;
            }
            if (value == null) {
                fields.add(DynamicFieldValue.ofNull(fieldCode, null));
                continue;
            }
            fields.add(DynamicFieldValue.ofString(fieldCode, value.toString()));
        }
        return DynamicFieldValues.of(fields);
    }

    public void setDynamicFieldValues(DynamicFieldValues values) {
        // Hydration replaces this view's selection, not stored values or mutation intent.
        if (!additionalInfo.isEmpty()) {
            for (String key : new ArrayList<>(additionalInfo.keySet())) {
                if (key.startsWith("#")) {
                    additionalInfo.remove(key);
                    loadState = loadState.withLoaded(key, false);
                }
            }
        }
        this.dynamicFieldValues = values;
        if (values != null) {
            for (Map.Entry<String, DynamicFieldValue> entry : values.toMap().entrySet()) {
                String key = "#" + entry.getKey();
                if (!entry.getValue().isLoaded()) continue;
                mutableAdditionalInfo().put(key, entry.getValue().value());
                loadState = loadState.withLoaded(key, true);
            }
        }
    }

    /** Bulk hydration supplies a shared final selection; values and wrappers stay row-owned. */
    @FrameworkInternal("Dynamic-field query hydration only")
    public void __internalHydrateDynamicFields(DynamicFieldValues values, LoadState sharedState) {
        __internalUseLoadState(sharedState);
        if (!additionalInfo.isEmpty()) additionalInfo.keySet().removeIf(key -> key.startsWith("#"));
        dynamicFieldValues = values;
        for (Map.Entry<String, DynamicFieldValue> entry : values.toMap().entrySet()) {
            if (entry.getValue().isLoaded()) mutableAdditionalInfo().put("#" + entry.getKey(), entry.getValue().value());
        }
    }

    /**
     * Put additional property directly without prefix.
     * Used by deserializers to populate entity fields.
     */
    public void putAdditional(String propertyName, Object value) {
        mutableAdditionalInfo().put(propertyName, value);
        if (propertyName != null && propertyName.startsWith("#")) {
            loadState = loadState.withLoaded(propertyName, true);
        }
    }
}
