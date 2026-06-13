package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.UserContext;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class RawSqlParser implements SQLExpressionParser<RawSql> {

    @Override
    public Class<RawSql> type() {
        return RawSql.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            RawSql expression,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        return expression.getSql();
    }
}
