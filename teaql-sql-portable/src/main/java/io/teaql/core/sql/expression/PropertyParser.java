package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.PropertyReference;
import io.teaql.core.UserContext;
import io.teaql.core.sql.SQLColumn;

import io.teaql.core.sql.SQLColumnResolver;
public class PropertyParser implements SQLExpressionParser<PropertyReference> {

    @Override
    public Class<PropertyReference> type() {
        return PropertyReference.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            PropertyReference property,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        String propertyName = property.getPropertyName();
        SQLColumn propertyColumn = sqlColumnResolver.getPropertyColumn(idTable, propertyName);
        if (userContext.getBool("MULTI_TABLE", false)) {
            return StrUtil.format("{}.{}", sqlColumnResolver.escapeIdentifier(propertyColumn.getTableName()), sqlColumnResolver.escapeIdentifier(propertyColumn.getColumnName()));
        }
        return StrUtil.format("{}", sqlColumnResolver.escapeIdentifier(propertyColumn.getColumnName()));
    }
}
