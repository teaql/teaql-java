package io.teaql.core.sql;

import io.teaql.utils.reflect.BeanUtil;
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
        BeanUtil.setProperty(sqlProperty, "tableName", tableName);
        BeanUtil.setProperty(sqlProperty, "columnName", columnName);
        BeanUtil.setProperty(sqlProperty, "columnType", columnType);
    }
}
