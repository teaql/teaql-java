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

    private Map<String, PropertyChange> updatedProperties = new ConcurrentHashMap<>();

    private static final ClassValue<LoadedPropertyLayout> LOADED_PROPERTY_LAYOUTS =
            new ClassValue<>() {
                @Override
                protected LoadedPropertyLayout computeValue(Class<?> type) {
                    return new LoadedPropertyLayout();
                }
            };

    private long loadedPropertyBits;
    private Set<String> overflowLoadedProperties;
    private boolean hydratingProperty;

    private Map<String, Object> additionalInfo = new ConcurrentHashMap<>();

    private DynamicFieldValues dynamicFieldValues;

    private Map<String, Entity> relationCache = new HashMap<>();

    private List<Object> actionList;

    private String _comment;

    /**
     * Shared change tracking root for the entire entity graph.
     */
    private EntityMutationLedger entityMutationLedger = new EntityMutationLedger();

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

    private String _traceChain;

    @Override
    public String getTraceChain() {
        return _traceChain;
    }

    @Override
    public void setTraceChain(String traceChain) {
        this._traceChain = traceChain;
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
                return new ArrayList<>(rootChanges);
            }
        }
        return new ArrayList<>(updatedProperties.keySet());
    }

    @Override
    public void addRelation(String relationName, Entity value) {
        io.teaql.core.meta.EntityDescriptor descriptor = io.teaql.core.meta.EntityMetaFactory.get()
                .resolveEntityDescriptor(this.typeName());
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
        if (value == null) return;
        additionalInfo.put(dynamicPropertyNameOf(propertyName), value);
    }

    @Override
    public void appendDynamicProperty(String propertyName, Object value) {
        String key = dynamicPropertyNameOf(propertyName);
        List<Object> existing = (List<Object>) additionalInfo.get(key);
        if (existing == null) {
            existing = new ArrayList<>();
            additionalInfo.put(key, existing);
        }
        existing.add(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getDynamicProperty(String propertyName) {
        return (T) additionalInfo.get(dynamicPropertyNameOf(propertyName));
    }

    private String dynamicPropertyNameOf(String propertyName) {
        if (propertyName.startsWith("#")) {
            return propertyName;
        }
        return "#" + propertyName;
    }

    @Override
    public BaseEntity markForDeletion() {
        gotoNextStatus(EntityAction.DELETE);
        if (entityMutationLedger != null && id != null) {
            entityMutationLedger.markAsDelete(new EntityKey(typeName(), id));
        }
        return this;
    }

    @Override
    public void markAsRecover() {
        gotoNextStatus(EntityAction.RECOVER);
    }

    @Override
    public boolean recoverItem() {
        return $status == EntityStatus.UPDATED_RECOVER;
    }

    public void clearUpdatedProperties() {
        this.updatedProperties.clear();
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
        return entityMutationLedger;
    }

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
        if (entityMutationLedger == null || id == null) {
            return null;
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
        markPropertyLoaded(loadedPropertyIndex(getClass(), propertyName), propertyName);
	}

    public boolean isPropertyLoaded(String propertyName) {
		if (propertyName == null) return false;
        Integer index = LOADED_PROPERTY_LAYOUTS.get(getClass()).find(propertyName);
        if (index == null) return overflowLoadedProperties != null
                && overflowLoadedProperties.contains(propertyName);
        if (index < Long.SIZE) return (loadedPropertyBits & (1L << index)) != 0;
        return overflowLoadedProperties != null && overflowLoadedProperties.contains(propertyName);
	}

    @FrameworkInternal("Compiled hydration infrastructure only")
    public static int loadedPropertyIndex(Class<? extends BaseEntity> entityType, String propertyName) {
        return LOADED_PROPERTY_LAYOUTS.get(entityType).index(propertyName);
    }

    @FrameworkInternal("Compiled hydration infrastructure only")
    public void __internalHydrate(String propertyName, Object value, int loadedPropertyIndex) {
        hydratingProperty = true;
        try {
            __internalSet(propertyName, value);
        } finally {
            hydratingProperty = false;
        }
        markPropertyLoaded(loadedPropertyIndex, propertyName);
    }

    private void markPropertyLoaded(int index, String propertyName) {
        if (index < Long.SIZE) {
            loadedPropertyBits |= 1L << index;
            return;
        }
        if (overflowLoadedProperties == null) overflowLoadedProperties = new java.util.HashSet<>();
        overflowLoadedProperties.add(propertyName);
    }

    private static final class LoadedPropertyLayout {
        private final ConcurrentHashMap<String, Integer> indexes = new ConcurrentHashMap<>();
        private final java.util.concurrent.atomic.AtomicInteger nextIndex =
                new java.util.concurrent.atomic.AtomicInteger();

        int index(String propertyName) {
            return indexes.computeIfAbsent(propertyName, ignored -> nextIndex.getAndIncrement());
        }

        Integer find(String propertyName) {
            return indexes.get(propertyName);
        }
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
        Object dynamicProperty = this.additionalInfo.get(dynamicPropertyNameOf(propertyName));
        if (dynamicProperty != null) {
            return (P) dynamicProperty;
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
            updatedProperties.remove(propertyName);
            return;
        }
        updatedProperties.put(propertyName, new PropertyChange(propertyName, oldValue, newValue));

        if (entityMutationLedger != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            entityMutationLedger.set(key, propertyName, newValue);
            if (_traceChain != null) {
                entityMutationLedger.setTraceChain(key, _traceChain);
            }
        }
    }

    public void gotoNextStatus(EntityAction action) {
        set$status(get$status().next(action));
    }

    public void cacheRelation(String relationName, Entity relation) {
		markPropertyLoaded(relationName);
        this.relationCache.put(relationName, relation);
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
        gotoNextStatus(EntityAction.RECOVER);
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
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public DynamicFieldValues getDynamicFieldValues() {
        return dynamicFieldValues;
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
        this.dynamicFieldValues = values;
        if (values != null) {
            for (Map.Entry<String, DynamicFieldValue> entry : values.toMap().entrySet()) {
                String key = "#" + entry.getKey();
                Object val = entry.getValue().value();
                additionalInfo.put(key, val);
            }
        }
    }

    /**
     * Put additional property directly without prefix.
     * Used by deserializers to populate entity fields.
     */
    public void putAdditional(String propertyName, Object value) {
        additionalInfo.put(propertyName, value);
    }
}
