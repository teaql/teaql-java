package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.SearchCriteria;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.VersionSearchCriteria;

import io.teaql.core.sql.SQLColumnResolver;
public class VersionSearchCriteriaParser implements SQLExpressionParser<VersionSearchCriteria> {
    public Class<VersionSearchCriteria> type() {
        return VersionSearchCriteria.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            VersionSearchCriteria expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        SearchCriteria searchCriteria = expression.getSearchCriteria();
        return ExpressionHelper.toSql(
                userContext, searchCriteria, idTable, parameters, sqlColumnResolver);
    }
}
