package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.Parameter;
import io.teaql.core.SearchCriteria;
import io.teaql.core.TypeCriteria;
import io.teaql.core.UserContext;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class TypeCriteriaParser implements SQLExpressionParser<TypeCriteria> {
    @Override
    public Class<TypeCriteria> type() {
        return TypeCriteria.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            TypeCriteria expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        SQLColumn childType = sqlColumnResolver.getPropertyColumn(idTable, "_child_type");
        if (childType == null) {
            return SearchCriteria.TRUE;
        }
        Parameter typeParameter = expression.getTypeParameter();
        String parameterSql =
                ExpressionHelper.toSql(userContext, typeParameter, idTable, parameters, sqlColumnResolver);

        if (userContext.getBool(SQLRepository.MULTI_TABLE, false)) {
            return StrUtil.format("{}._child_type in ({})", childType.getTableName(), parameterSql);
        }
        return StrUtil.format("_child_type in ({})", parameterSql);
    }
}
