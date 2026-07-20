package io.teaql.core.sql;

import io.teaql.core.BaseEntity;

public class SQLColumn {
    String tableName;
    String columnName;
    String type;

    public SQLColumn(String pTableName, String pColumnName) {
        tableName = pTableName;
        columnName = pColumnName;
    }

    public boolean isIdColumn() {
        return BaseEntity.ID_PROPERTY.equals(this.columnName);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String pTableName) {
        tableName = pTableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String pColumnName) {
        columnName = pColumnName;
    }

    public String getType() {
        return type;
    }

    public void setType(String pType) {
        type = pType;
    }
}
