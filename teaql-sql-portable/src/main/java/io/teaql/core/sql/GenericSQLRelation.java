package io.teaql.core.sql;

import java.sql.ResultSet;
import java.util.List;

import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.ReflectUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.EntityStatus;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.Relation;

public class GenericSQLRelation extends Relation implements SQLProperty {
    private String tableName;
    private String columnName;
    private String columnType;

    @Override
    public void setName(String name) {
        super.setName(name);
        if (this.columnName == null) {
            this.columnName = io.teaql.core.utils.NamingCase.toUnderlineCase(name);
        }
    }

    @Override
    public List<SQLColumn> columns() {
        SQLColumn sqlColumn = new SQLColumn(tableName, columnName);
        sqlColumn.setType(columnType);
        return ListUtil.of(sqlColumn);
    }

    @Override
    public List<SQLData> toDBRaw(UserContext ctx, Entity entity, Object value) {
        SQLData d = new SQLData();
        d.setColumnName(columnName);
        d.setTableName(tableName);
        if (value == null) {
            d.setValue(null);
        }
        else if (value instanceof Entity) {
            d.setValue(((Entity) value).getId());
        }
        else {
            throw new TeaQLRuntimeException("Relation only support Entity class");
        }
        return ListUtil.of(d);
    }

    @Override
    public void setPropertyValue(UserContext ctx, Entity entity, ResultSet rs) {
        if (!findName(rs, getName())) {
            return;
        }
        Class targetType = getType().javaType();
        if (Entity.class.isAssignableFrom(targetType)) {
            Entity o = createRefer(rs);
            entity.setProperty(getName(), o);
            return;
        }
        throw new TeaQLRuntimeException("Relation only support Entity class");
    }

    protected boolean findName(ResultSet resultSet, String name) {
        try {
            int columnCount = resultSet.getMetaData().getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                String columnLabel = resultSet.getMetaData().getColumnLabel(i + 1);
                if (columnLabel.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        catch (Exception e) {

        }
        return false;
    }

    private Entity createRefer(ResultSet resultSet) {
        BaseEntity o = (BaseEntity) ReflectUtil.newInstance(getType().javaType());
        Object referId = getValue(resultSet);

        if (referId == null) {
            return null;
        }
        o.internalSet("id", ((Number) referId).longValue());
        o.set$status(EntityStatus.REFER);
        return o;
    }

    protected Object getValue(ResultSet resultSet) {
        return ResultSetTool.getValue(resultSet, getName());
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

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String pColumnType) {
        columnType = pColumnType;
    }
}
