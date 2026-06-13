package io.teaql.core.sqlite;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.AggrFunction;
import io.teaql.core.sql.expression.AggrExpressionParser;

public class SQLiteAggrExpressionParser extends AggrExpressionParser {
    @Override
    public String genAggrSQL(AggrFunction operator, String sqlColumn) {
        if (operator == AggrFunction.GBK) {
            return StrUtil.format("convert({} using gbk)", sqlColumn);
        }
        return super.genAggrSQL(operator, sqlColumn);
    }
}
