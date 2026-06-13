package io.teaql.core.sqlite;

import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.expression.TwoOperatorExpressionParser;

public class SQLiteTwoOperatorExpressionParser extends TwoOperatorExpressionParser {
    @Override
    public String getOp(Operator operator) {
        switch (operator) {
            case IN_LARGE:
                return "IN";
            case NOT_IN_LARGE:
                return "NOT IN";
        }
        return super.getOp(operator);
    }
}
