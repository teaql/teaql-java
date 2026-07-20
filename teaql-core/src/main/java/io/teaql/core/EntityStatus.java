package io.teaql.core;

import io.teaql.core.utils.RowKeyTable;
import io.teaql.core.utils.StrUtil;

import static io.teaql.core.EntityAction.DELETE;
import static io.teaql.core.EntityAction.PERSIST;
import static io.teaql.core.EntityAction.RECOVER;
import static io.teaql.core.EntityAction.UPDATE;

// entity status definitions
public enum EntityStatus {
    // the entity is created from constructors
    NEW,

    // the entity from Repository save,query（with version > 0）
    PERSISTED,

    // the entity from Repository save,query,delete（with version < 0）
    PERSISTED_DELETED,

    // the PERSISTED entities after successfully updated
    UPDATED,

    // the PERSISTED entities after successfully deleted
    UPDATED_DELETED,

    // the PERSISTED_DELETED entities after successfully recover
    UPDATED_RECOVER,

    // refer only, cannot change it, and will not persist it
    REFER;

    private static final RowKeyTable<EntityStatus, EntityAction, EntityStatus> statusTransaction =
            new RowKeyTable<>();

    static {
        statusTransaction.put(NEW, UPDATE, NEW);
        statusTransaction.put(NEW, PERSIST, PERSISTED);
        statusTransaction.put(PERSISTED, UPDATE, UPDATED);
        statusTransaction.put(PERSISTED, DELETE, UPDATED_DELETED);
        statusTransaction.put(PERSISTED_DELETED, RECOVER, UPDATED_RECOVER);
        statusTransaction.put(UPDATED, UPDATE, UPDATED);
        statusTransaction.put(UPDATED, PERSIST, PERSISTED);
        statusTransaction.put(UPDATED_DELETED, PERSIST, PERSISTED_DELETED);
        statusTransaction.put(UPDATED_DELETED, DELETE, UPDATED_DELETED);
        statusTransaction.put(UPDATED_RECOVER, PERSIST, PERSISTED);
        statusTransaction.put(UPDATED_RECOVER, RECOVER, UPDATED_RECOVER);
    }

    public EntityStatus next(EntityAction action) {
        EntityStatus entityStatus = statusTransaction.get(this, action);
        if (entityStatus == null) {
            throw new TeaQLRuntimeException(
                    StrUtil.format("current status: {} cannot apply action: {}", this, action));
        }
        return entityStatus;
    }
}
