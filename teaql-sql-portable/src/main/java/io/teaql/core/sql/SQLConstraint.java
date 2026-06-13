package io.teaql.core.sql;

public record SQLConstraint(
        String name, String tableName, String columnName, String fTableName, String fColumnName) {
}
