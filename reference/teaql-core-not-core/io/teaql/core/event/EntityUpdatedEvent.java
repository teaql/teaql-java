package io.teaql.core.event;

import java.util.List;

import io.teaql.core.BaseEntity;

public class EntityUpdatedEvent {
    private BaseEntity item;

    // TODO: copy properties from item to local entity
    public EntityUpdatedEvent(BaseEntity item) {
        this.item = item;
    }

    public BaseEntity getItem() {
        return item;
    }

    public void setItem(BaseEntity pItem) {
        item = pItem;
    }

    public List<String> getUpdatedProperties() {
        return item.getUpdatedProperties();
    }

    public Object getOldValue(String propertyName) {
        return item.getOldValue(propertyName);
    }

    public Object getNewValue(String propertyName) {
        return item.getNewValue(propertyName);
    }
}
