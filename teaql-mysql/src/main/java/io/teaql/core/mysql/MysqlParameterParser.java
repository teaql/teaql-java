package io.teaql.core.mysql;

import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.expression.ParameterParser;

public class MysqlParameterParser extends ParameterParser {
    @Override
    public Object fixValue(Operator operator, Object pValue) {
        switch (operator) {
            case IN_LARGE:
            case NOT_IN_LARGE:
                return pValue;
        }
        return super.fixValue(operator, pValue);
    }
}
