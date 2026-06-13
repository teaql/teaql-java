package io.teaql.core.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import io.teaql.core.RepositoryException;

public class ResultSetTool {

    public static Object getValue(ResultSet resultSet, String columnName) {
        try {
            return getValueInternal(resultSet, columnName);
        }
        catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    protected static Object getValueInternal(ResultSet resultSet, String columnName)
            throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();

        for (int i = 0; i < metaData.getColumnCount(); i++) {

            String columnNameInRs = metaData.getColumnName(i + 1);
            if (columnNameInRs.equalsIgnoreCase(columnName)) {
                return resultSet.getObject(i + 1);
            }
            String columnLabelInRs = metaData.getColumnLabel(i + 1);
            if (columnName.equalsIgnoreCase(columnLabelInRs)) {
                return resultSet.getObject(i + 1);
            }
        }

        throw new IllegalArgumentException("Column '" + columnName + "' is not found in ResultSet");
    }
}
