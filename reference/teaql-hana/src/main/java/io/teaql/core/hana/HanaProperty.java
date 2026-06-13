package io.teaql.core.hana;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import io.teaql.core.sql.GenericSQLProperty;

public class HanaProperty extends GenericSQLProperty {
    public HanaProperty() {
    }

    @Override
    protected boolean findName(ResultSet resultSet, String name) {
        return super.findName(resultSet, name);
    }

    @Override
    protected Object getValue(ResultSet resultSet) {
        ResultSetMetaData metaData = null;
        String columnName = getName();
        try {
            metaData = resultSet.getMetaData();
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                String columnLabelInRs = metaData.getColumnLabel(i + 1);
                if (columnName.equalsIgnoreCase(columnLabelInRs)) {
                    return resultSet.getObject(i + 1);
                }
                String columnNameInRs = metaData.getColumnName(i + 1);
                if (columnNameInRs.equalsIgnoreCase(columnName)) {
                    return resultSet.getObject(i + 1);
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalArgumentException("Column '" + columnName + "' is not found in ResultSet");
    }
}
