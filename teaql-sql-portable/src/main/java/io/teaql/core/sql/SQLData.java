package io.teaql.core.sql;

// SQLData the column value in one row，
// we will calculate the slq data（save or update entity)
public class SQLData {
    // table name
    String tableName;

    // column name
    String columnName;

    // persist column value
    Object value;

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

    public Object getValue() {
        return value;
    }

    public void setValue(Object pValue) {
        value = pValue;
    }
}
