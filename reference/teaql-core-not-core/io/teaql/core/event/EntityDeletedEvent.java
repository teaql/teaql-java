package io.teaql.core.event;

import io.teaql.core.BaseEntity;

public class EntityDeletedEvent {
    private BaseEntity item;

    // TODO: copy properties from item to local entity
    public EntityDeletedEvent(BaseEntity item) {
        this.item = item;
    }

    public BaseEntity getItem() {
        return item;
    }

    public void setItem(BaseEntity pItem) {
        item = pItem;
    }
}
