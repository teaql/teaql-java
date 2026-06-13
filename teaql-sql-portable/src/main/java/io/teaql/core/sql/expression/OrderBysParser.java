package io.teaql.core.sql.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.teaql.core.OrderBy;
import io.teaql.core.OrderBys;
import io.teaql.core.UserContext;

import io.teaql.core.sql.SQLColumnResolver;
public class OrderBysParser implements SQLExpressionParser<OrderBys> {
    @Override
    public Class<OrderBys> type() {
        return OrderBys.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            OrderBys expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        List<OrderBy> orderBys = expression.getOrderBys();
        if (orderBys.isEmpty()) {
            return null;
        }
        return orderBys.stream()
                .map(
                        order ->
                                ExpressionHelper.toSql(userContext, order, idTable, parameters, sqlColumnResolver))
                .collect(Collectors.joining(", ", "ORDER BY ", ""));
    }
}
