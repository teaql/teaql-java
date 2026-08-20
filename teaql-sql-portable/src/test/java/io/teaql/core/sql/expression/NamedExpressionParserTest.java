package io.teaql.core.sql.expression;

import static org.junit.Assert.assertEquals;

import io.teaql.core.Expression;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.UserContext;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLColumnResolver;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class NamedExpressionParserTest {
    private static final class RawExpression implements Expression {
        private final String sql;
        private RawExpression(String sql) { this.sql = sql; }
    }

    private static final class RawParser implements SQLExpressionParser<RawExpression> {
        @Override public Class<RawExpression> type() { return RawExpression.class; }
        @Override
        public String toSql(UserContext context, RawExpression expression, String idTable,
                Map<String, Object> parameters, SQLColumnResolver resolver) {
            return expression.sql;
        }
    }

    private static final SQLColumnResolver RESOLVER = new SQLColumnResolver() {
        @Override public List<SQLColumn> getPropertyColumns(String idTable, String property) {
            return List.of();
        }
        @Override public Map<Class, SQLExpressionParser> getExpressionParsers() {
            return Map.of(RawExpression.class, new RawParser());
        }
        @Override public String escapeIdentifier(String identifier) {
            return "\"" + identifier + "\"";
        }
    };

    @Test
    public void doesNotAddAnUnquotedAliasForReservedPropertyName() {
        String sql = new NamedExpressionParser().toSql(null,
                new SimpleNamedExpression("order", new RawExpression("\"order\"")),
                "pet_order_item_data", new java.util.HashMap<>(), RESOLVER);
        assertEquals("\"order\"", sql);
    }

    @Test
    public void quotesAliasWhenPhysicalAndLogicalNamesDiffer() {
        String sql = new NamedExpressionParser().toSql(null,
                new SimpleNamedExpression("unitPrice", new RawExpression("unit_price")),
                "pet_order_item_data", new java.util.HashMap<>(), RESOLVER);
        assertEquals("unit_price AS \"unitPrice\"", sql);
    }
}
