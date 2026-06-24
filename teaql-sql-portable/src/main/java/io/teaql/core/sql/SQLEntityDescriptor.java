package io.teaql.core.sql;

import io.teaql.core.utils.NamingCase;
import io.teaql.core.meta.EntityDescriptor;

public class SQLEntityDescriptor extends EntityDescriptor {

    @Override
    protected GenericSQLProperty createPropertyDescriptor() {
        GenericSQLProperty p = new GenericSQLProperty();
        p.setTableName(NamingCase.toUnderlineCase(this.getType() + "_data"));
        return p;
    }

    @Override
    protected GenericSQLRelation createRelation() {
        GenericSQLRelation p = new GenericSQLRelation();
        p.setTableName(NamingCase.toUnderlineCase(this.getType() + "_data"));
        return p;
    }

    public void prepareSQLMeta(
            SQLProperty sqlProperty, String tableName, String columnName, String columnType) {
        if (sqlProperty instanceof GenericSQLProperty property) {
            property.setTableName(tableName);
            property.setColumnName(columnName);
            property.setColumnType(columnType);
            return;
        }
        if (sqlProperty instanceof GenericSQLRelation relation) {
            relation.setTableName(tableName);
            relation.setColumnName(columnName);
            relation.setColumnType(columnType);
            return;
        }
        throw new IllegalArgumentException("Unsupported SQLProperty type: " + sqlProperty.getClass().getName());
    }
}
