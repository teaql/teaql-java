package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.Expression;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.sql.SQLColumnResolver;


public class ExpressionHelper {
    private static final java.util.Set<Class<?>> BUILTIN = java.util.Set.of(
            ANDExpressionParser.class, AggrExpressionParser.class, BetweenParser.class,
            FunctionApplyParser.class, NOTExpressionParser.class, NamedExpressionParser.class,
            ORExpressionParser.class, OneOperatorExpressionParser.class, OrderByExpressionParser.class,
            OrderBysParser.class, ParameterParser.class, PropertyParser.class, SubQueryParser.class,
            TwoOperatorExpressionParser.class, TypeCriteriaParser.class, VersionSearchCriteriaParser.class);

    public static String toSql(
            UserContext userContext,
            Expression expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        return toSqlInternal(userContext, expression, idTable, parameters,
                sqlColumnResolver.getExpressionParsers(), sqlColumnResolver);
    }

    public static String toSql(
            UserContext userContext,
            Expression expression,
            String idTable,
            Map<String, Object> parameters,
            Map<Class, SQLExpressionParser> parsers,
            SQLColumnResolver columnResolver) {
        return toSqlInternal(userContext, expression, idTable, parameters, parsers, columnResolver);
    }

    private static String toSqlInternal(
            UserContext userContext,
            Expression expression,
            String idTable,
            Map<String, Object> parameters,
            Map<Class, SQLExpressionParser> parsers,
            SQLColumnResolver columnResolver) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof SQLExpressionParser) {
            if (parameters instanceof io.teaql.core.sql.SqlParameters tracked) tracked.untrusted();
            return ((SQLExpressionParser) expression)
                    .toSql(userContext, expression, idTable, parameters, columnResolver);
        }

        Class expressionClass = expression.getClass();
        SQLExpressionParser parser = null;

        while (expressionClass != null) {
            parser = parsers.get(expressionClass);
            if (parser != null) {
                break;
            }
            expressionClass = expressionClass.getSuperclass();
        }
        if (parser == null) {
            throw new TeaQLRuntimeException("no parse for expression type:" + expression.getClass());
        }
        if (!(parameters instanceof io.teaql.core.sql.SqlParameters tracked))
            return parser.toSql(userContext, expression, idTable, parameters, columnResolver);
        if (!BUILTIN.contains(parser.getClass())) tracked.untrusted();
        var previous = tracked.currentPolicy();
        try {
            if (expression instanceof io.teaql.core.criteria.TwoOperatorCriteria
                    || expression instanceof io.teaql.core.criteria.Between) {
                var properties = expression.properties(userContext);
                var policy = io.teaql.core.SqlParameterLogPolicy.PLAIN;
                if (properties == null || properties.isEmpty()) policy = io.teaql.core.SqlParameterLogPolicy.UNKNOWN;
                else for (String property : properties) {
                    var candidate = columnResolver.parameterLogPolicy(property);
                    if (rank(candidate) > rank(policy)) policy = candidate;
                }
                tracked.currentPolicy(policy);
            }
            return parser.toSql(userContext, expression, idTable, parameters, columnResolver);
        } finally { tracked.currentPolicy(previous); }
    }

    private static int rank(io.teaql.core.SqlParameterLogPolicy policy) {
        return switch (policy) { case PLAIN -> 0; case UNKNOWN -> 1; case MASKED -> 2; case CREDENTIAL -> 3; };
    }
}
