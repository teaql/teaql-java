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

    private Map<String, Object> additionalInfo = new ConcurrentHashMap<>();

    private DynamicFieldValues dynamicFieldValues;

    private Map<String, Entity> relationCache = new HashMap<>();

    private List<Object> actionList;

    private String _comment;

    /**
     * Shared change tracking root for the entire entity graph.
     */
    private EntityRoot entityRoot = new EntityRoot();

    @Override
    public String getComment() {
        return _comment;
    }

    @Override
    public void setComment(String comment) {
        this._comment = comment;
        if (entityRoot != null) {
            entityRoot.setComment(comment);
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
        if (ObjectUtil.equals(this.version, version)) return this;
        handleUpdate(VERSION_PROPERTY, getVersion(), version);
        this.version = version;
        return this;
    }

    @FrameworkInternal("Business code must use updateXxx() methods")
    public void internalSet(String property, Object value) {
        switch (property) {
            case "id":      this.id = (Long) value; break;
            case "version": this.version = (Long) value; break;
            default:
                throw new IllegalArgumentException(typeName() + " has no property: " + property);
        }
    }

    @FrameworkInternal("Business code should use typed getXxx() methods")
    public Object internalGet(String property) {
        // First try to get from entityRoot if available
        if (entityRoot != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            Object value = entityRoot.get(key, property);
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
        if (entityRoot != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            return new ArrayList<>(entityRoot.changedFieldNames(key));
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
    public void markAsDeleted() {
        gotoNextStatus(EntityAction.DELETE);
        if (entityRoot != null && id != null) {
            entityRoot.markAsDelete(new EntityKey(typeName(), id));
        }
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

    // --- EntityRoot integration ---

    public EntityRoot getEntityRoot() {
        return entityRoot;
    }

    public void setEntityRoot(EntityRoot entityRoot) {
        this.entityRoot = entityRoot;
        if (entityRoot != null && id != null && newItem()) {
            entityRoot.markAsNew(new EntityKey(typeName(), id));
        }
        if (entityRoot != null && id != null && version != null) {
            entityRoot.setOriginalVersion(new EntityKey(typeName(), id), version);
        }
    }

    public Set<String> dirtyFields() {
        if (entityRoot == null || id == null) {
            return null;
        }
        EntityKey key = new EntityKey(typeName(), id);
        Set<String> fields = entityRoot.changedFieldNames(key);
        return fields.isEmpty() ? null : fields;
    }

    public boolean isMarkedAsDelete() {
        if (entityRoot == null || id == null) {
            return deleteItem();
        }
        return entityRoot.isMarkedAsDelete(new EntityKey(typeName(), id));
    }

    public boolean isNew() {
        if (entityRoot == null || id == null) {
            return newItem();
        }
        return entityRoot.isNew(new EntityKey(typeName(), id));
    }

    public Long getOriginalVersion() {
        if (entityRoot == null || id == null) {
            return null;
        }
        return entityRoot.getOriginalVersion(new EntityKey(typeName(), id));
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        this.internalSet(propertyName, value);
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

        if (entityRoot != null && id != null) {
            EntityKey key = new EntityKey(typeName(), id);
            entityRoot.set(key, propertyName, newValue);
            if (_traceChain != null) {
                entityRoot.setTraceChain(key, _traceChain);
            }
        }
    }

    public void gotoNextStatus(EntityAction action) {
        set$status(get$status().next(action));
    }

    public void cacheRelation(String relationName, Entity relation) {
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

    public BaseEntity markToRemove() {
        gotoNextStatus(EntityAction.DELETE);
        if (entityRoot != null && id != null) {
            entityRoot.markAsDelete(new EntityKey(typeName(), id));
        }
        return this;
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
        return Objects.hash(getId(), getVersion(), typeName());
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
