package io.teaql.core;

import java.beans.PropertyChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.ReflectUtil;

public class BaseEntity implements Entity {
    public static final String ID_PROPERTY = "id";
    public static final String VERSION_PROPERTY = "version";
    private Long id;
    private Long version;

    private EntityStatus $status = EntityStatus.NEW;

    @JsonIgnore
    private String subType;

    private String displayName;

    @JsonIgnore
    private Map<String, PropertyChangeEvent> updatedProperties = new ConcurrentHashMap<>();

    @JsonIgnore
    private Map<String, Object> additionalInfo = new ConcurrentHashMap<>();

    @JsonIgnore
    private Map<String, Entity> relationCache = new HashMap<>();

    private List<Object> actionList;

    @JsonIgnore
    private String _comment;

    @Override
    public String getComment() {
        return _comment;
    }

    @Override
    public void setComment(String comment) {
        this._comment = comment;
    }

    @JsonIgnore
    private String _traceChain;

    @Override
    public String getTraceChain() {
        return _traceChain;
    }

    @Override
    public void setTraceChain(String traceChain) {
        this._traceChain = traceChain;
    }

    @JsonIgnore
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

    /**
     * Framework-internal property assignment channel.
     * Used by database hydrators and JSON deserializers to populate entity fields
     * during object construction. Performs raw assignment WITHOUT change tracking.
     * Business code MUST use updateXxx() methods instead.
     */
    @FrameworkInternal("Business code must use updateXxx() methods")
    public void internalSet(String property, Object value) {
        switch (property) {
            case "id":      this.id = (Long) value; break;
            case "version": this.version = (Long) value; break;
            default:
                throw new IllegalArgumentException(
                    typeName() + " has no property: " + property);
        }
    }

    /**
     * Framework-internal generic property read channel.
     * Used by serializers, diff engines, and audit trail builders for
     * reflection-free, uniform property access on BaseEntity references.
     */
    @FrameworkInternal("Business code should use typed getXxx() methods")
    public Object internalGet(String property) {
        switch (property) {
            case "id":      return this.id;
            case "version": return this.version;
            default:
                throw new IllegalArgumentException(
                    typeName() + " has no property: " + property);
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
        return new ArrayList<>(updatedProperties.keySet());
    }

    @Override
    public void addRelation(String relationName, Entity value) {
        io.teaql.core.meta.EntityDescriptor descriptor = io.teaql.core.meta.EntityMetaFactory.get().resolveEntityDescriptor(this.typeName());
        if (descriptor == null) return;
        io.teaql.core.meta.PropertyDescriptor pd = descriptor.findProperty(relationName);
        if (pd == null || pd.getType() == null) return;
        Class<?> type = pd.getType().javaType();
        if (SmartList.class.isAssignableFrom(type)) {
            SmartList existing = getProperty(relationName);
            if (existing == null) {
                existing = new SmartList();
                setProperty(relationName, existing);
            }
            existing.add(value);
        }
        else if (Entity.class.isAssignableFrom(type)) {
            setProperty(relationName, value);
        }
    }

    @Override
    public void addDynamicProperty(String propertyName, Object value) {
        if (value == null) {
            return;
        }
        this.additionalInfo.put(dynamicPropertyNameOf(propertyName), value);
    }

    public String dynamicPropertyNameOf(String propertyName) {
        if (propertyName.startsWith(".") && propertyName.length() > 1) {
            return propertyName.substring(1);
        }
        return String.join("", "_", propertyName);
    }

    @Override
    public void appendDynamicProperty(String propertyName, Object value) {
        propertyName = dynamicPropertyNameOf(propertyName);
        List list = (List) this.additionalInfo.get(propertyName);
        if (list == null) {
            list = new ArrayList<>();
            this.additionalInfo.put(propertyName, list);
        }
        list.add(value);
    }

    @Override
    public <T> T getDynamicProperty(String propertyName) {
        return getDynamicProperty(propertyName, null);
    }

    public Long sumDynaPropOfNumberAsLong(List<String> propertyNames) {
        AtomicLong atomicLong = new AtomicLong(0);
        propertyNames.forEach(
                prop -> {
                    Long ele = ((Number) getDynamicProperty(prop, 0L)).longValue();
                    atomicLong.getAndAdd(ele);
                });
        return atomicLong.longValue();
    }

    @Override
    public void markAsDeleted() {
        gotoNextStatus(EntityAction.DELETE);
    }

    @Override
    public void markAsRecover() {
        gotoNextStatus(EntityAction.RECOVER);
    }

    public <T> T getDynamicProperty(String propertyName, T defaultValue) {
        Object o = this.additionalInfo.get(dynamicPropertyNameOf(propertyName));
        if (o == null) {
            return defaultValue;
        }
        return (T) o;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> pAdditionalInfo) {
        additionalInfo = pAdditionalInfo;
    }

    @JsonAnySetter
    public void putAdditional(String propertyName, Object value) {
        additionalInfo.put(propertyName, value);
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

    @Override
    public <P> P getProperty(String propertyName) {
        Entity o = this.relationCache.get(propertyName);
        if (o != null) {
            return (P) o;
        }
        return Entity.super.getProperty(propertyName);
    }

    /**
     * callbacks for updateXXX methods
     *
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    public void handleUpdate(String propertyName, Object oldValue, Object newValue) {
        gotoNextStatus(EntityAction.UPDATE);
        PropertyChangeEvent propertyChangeEvent = updatedProperties.get(propertyName);
        // find the older value
        if (propertyChangeEvent != null) {
            oldValue = propertyChangeEvent.getOldValue();
        }
        // value changed back, then no changes
        if (ObjectUtil.equals(oldValue, newValue)) {
            updatedProperties.remove(propertyName);
            return;
        }
        updatedProperties.put(
                propertyName, new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }

    public void gotoNextStatus(EntityAction action) {
        set$status(get$status().next(action));
    }

    public void cacheRelation(String relationName, Entity relation) {
        this.relationCache.put(relationName, relation);
        Object initValue = Entity.super.getProperty(relationName);
        handleUpdate(relationName, initValue, relation);
    }

    public Object getOldValue(String propertyName) {
        PropertyChangeEvent propertyChangeEvent = updatedProperties.get(propertyName);
        if (propertyChangeEvent == null) {
            return null;
        }
        return propertyChangeEvent.getOldValue();
    }

    public Object getNewValue(String propertyName) {
        PropertyChangeEvent propertyChangeEvent = updatedProperties.get(propertyName);
        if (propertyChangeEvent == null) {
            return null;
        }
        return propertyChangeEvent.getNewValue();
    }

    public BaseEntity markToRemove() {
        gotoNextStatus(EntityAction.DELETE);
        return this;
    }

    public BaseEntity markToRecover() {
        gotoNextStatus(EntityAction.RECOVER);
        return this;
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
}
