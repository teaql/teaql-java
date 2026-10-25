package io.teaql.core.sql;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobRef;
import java.sql.ResultSet;
import java.util.List;

/** SQL text codec only. Catalog validation and binding occur in the business transaction. */
public final class BlobSQLProperty extends GenericSQLProperty {
    @Override public List<SQLData> toDBRaw(UserContext context, Entity entity, Object value) {
        if (value != null && !(value instanceof BlobRef)) {
            throw new IllegalArgumentException("BLOB_REFERENCE_INVALID");
        }
        SQLData data = new SQLData();
        data.setColumnName(getColumnName());
        data.setTableName(getTableName());
        data.setValue(value == null ? null : ((BlobRef) value).key());
        return List.of(data);
    }

    public static BlobRef decode(Object value) {
        if (value == null) return null;
        if (value instanceof BlobRef ref) return ref;
        if (!(value instanceof String text)) throw new IllegalArgumentException("BLOB_REFERENCE_INVALID");
        return BlobRef.of(text);
    }

    @Override public void setPropertyValue(UserContext context, Entity entity, ResultSet rows) {
        if (findName(rows, getName())) entity.setProperty(getName(), decode(getValue(rows)));
    }
}
