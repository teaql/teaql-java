package io.teaql.core.sql.expression;

import java.util.List;
import java.util.Map;

import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Parameter;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.Operator;

import io.teaql.core.sql.SQLColumnResolver;
public class ParameterParser implements SQLExpressionParser<Parameter> {
    @Override
    public Class<Parameter> type() {
        return Parameter.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            Parameter parameter,
            String pIdTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        String key = nextPropertyKey(parameters, parameter.getName());
        Operator operator = parameter.getOperator();
        Object value = parameter.getValue();
        if (operator != null) {
            value = fixValue(operator, parameter.getValue());
        }
        var policy = parameters instanceof io.teaql.core.sql.SqlParameters tracked
                ? tracked.currentPolicy() : io.teaql.core.SqlParameterLogPolicy.UNKNOWN;
        if (io.teaql.core.utils.SensitiveLogNames.credential(parameter.getName()))
            policy = io.teaql.core.SqlParameterLogPolicy.CREDENTIAL;
        io.teaql.core.sql.SqlParameters.bind(parameters, key, value, policy);
        return StrUtil.format(":{}", key);
    }

    public Object fixValue(Operator pOperator, Object pValue) {
        switch (pOperator) {
            case CONTAIN:
            case NOT_CONTAIN:
                return "%" + pValue + "%";
            case BEGIN_WITH:
            case NOT_BEGIN_WITH:
                return pValue + "%";
            case END_WITH:
            case NOT_END_WITH:
                return "%" + pValue;
            case IN:
            case NOT_IN:
                return Parameter.flatValues(pValue);
            case IN_LARGE:
            case NOT_IN_LARGE:
                List flatValues = Parameter.flatValues(pValue);
                Object o = flatValues.get(0);
                return ArrayUtil.toArray(flatValues, o.getClass());
        }
        return pValue;
    }
}
