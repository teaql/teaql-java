package io.teaql.core.sql.portable;

import java.util.List;

import io.teaql.core.Entity;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLData;
import io.teaql.core.sql.SQLProperty;
import io.teaql.core.utils.ListUtil;

public class SQLPropertyUtil {

    public static String getTableName(PropertyDescriptor property) {
        String tableName = property.getSelfAdditionalInfo().get("tableName");
        if (tableName == null && property.getOwner() != null) {
            tableName = io.teaql.core.utils.NamingCase.toUnderlineCase(property.getOwner().getType()) + "_data";
        }
        return tableName;
    }

    public static String getColumnName(PropertyDescriptor property) {
        String columnName = property.getSelfAdditionalInfo().get("columnName");
        if (columnName == null) {
            columnName = io.teaql.core.utils.NamingCase.toUnderlineCase(property.getName());
        }
        return columnName;
    }

    public static List<SQLColumn> getColumns(PropertyDescriptor property) {
        if (property instanceof SQLProperty) {
            return ((SQLProperty) property).columns();
        }
        String tableName = getTableName(property);
        String columnName = getColumnName(property);
        String defaultType = "VARCHAR(255)";
        if (property.getType() != null && property.getType().javaType() != null) {
            Class<?> jType = property.getType().javaType();
            if (io.teaql.core.Entity.class.isAssignableFrom(jType) || jType == Long.class || jType == long.class) {
                defaultType = "BIGINT";
            } else if (jType == Integer.class || jType == int.class) {
                defaultType = "INTEGER";
            } else if (jType == Double.class || jType == double.class || jType == Float.class || jType == float.class) {
                defaultType = "DOUBLE";
            } else if (jType == java.util.Date.class || jType == java.time.LocalDateTime.class) {
                defaultType = "TIMESTAMP";
            } else if (jType == Boolean.class || jType == boolean.class) {
                defaultType = "TINYINT";
            }
        }
        String columnType = property.getStr("sqlType", defaultType);
        
        if (tableName != null && columnName != null) {
            SQLColumn sqlColumn = new SQLColumn(tableName, columnName);
            sqlColumn.setType(columnType);
            return ListUtil.of(sqlColumn);
        }
        throw new TeaQLRuntimeException("Cannot derive SQL metadata for property: " + property.getName() + " (class: " + property.getClass().getName() + ")");
    }

    public static List<SQLData> toDBRaw(UserContext ctx, Entity entity, Object value, PropertyDescriptor property) {
        if (property instanceof SQLProperty) {
            return ((SQLProperty) property).toDBRaw(ctx, entity, value);
        }
        String tableName = getTableName(property);
        String columnName = getColumnName(property);
        
        if (tableName != null && columnName != null) {
            SQLData d = new SQLData();
            d.setColumnName(columnName);
            d.setTableName(tableName);
            d.setValue(unwrapEntityId(value));
            return ListUtil.of(d);
        }
        throw new TeaQLRuntimeException("Cannot derive SQL metadata for property: " + property.getName() + " (class: " + property.getClass().getName() + ")");
    }

    public static Object unwrapEntityId(Object value) {
        return value instanceof Entity e ? e.getId() : value;
    }
}
