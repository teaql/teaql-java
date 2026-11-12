package io.teaql.core.sql;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.core.sql.expression.ExpressionHelper;
import io.teaql.core.sql.expression.SQLExpressionParser;
import io.teaql.core.utils.SensitiveLogNames;
import java.util.*;

/** Portable HAVING semantics over a completed statistical relation, not source rows. */
public final class StatisticalResultFilter {
    public record Column(String label, SqlParameterLogPolicy policy) {}

    private StatisticalResultFilter() {}

    public static String apply(UserContext context, String sql, SearchCriteria condition,
            Map<String, Column> columns, SQLColumnResolver provider, Map<String, Object> parameters) {
        if (condition == null) return sql;
        // Resolve aliases only against this layer's output. Never fall back to an
        // entity column or accept an aggregate-over-aggregate/raw SQL expression.
        SQLColumnResolver resolver = new SQLColumnResolver() {
            @Override public List<SQLColumn> getPropertyColumns(String table, String property) {
                Column column = columns.get(property);
                if (column == null) throw invalid("Unknown or unselected statistical result: " + property);
                return List.of(new SQLColumn("__teaql_result", column.label()));
            }
            @Override public SqlParameterLogPolicy parameterLogPolicy(String property) {
                Column column = columns.get(property);
                if (column == null) throw invalid("Unknown or unselected statistical result: " + property);
                return column.policy();
            }
            @Override public Map<Class, SQLExpressionParser> getExpressionParsers() { return provider.getExpressionParsers(); }
            @Override public String escapeIdentifier(String name) { return provider.escapeIdentifier(name); }
        };
        Expression normalized = new Normalizer(columns).copy(condition, 0);
        String predicate = ExpressionHelper.toSql(context, normalized, "__teaql_result", parameters, resolver);
        return "SELECT * FROM (" + sql + ") AS " + provider.escapeIdentifier("__teaql_result") + " WHERE " + predicate;
    }

    private static final class Normalizer {
        private final Map<String, Column> columns;
        private final IdentityHashMap<Expression, Boolean> active = new IdentityHashMap<>();
        private int nodes, parameter;
        Normalizer(Map<String, Column> columns) { this.columns = columns; }

        Expression copy(Expression expression, int depth) {
            if (expression == null || depth > 64 || ++nodes > 1024 || active.put(expression, true) != null)
                throw invalid("Cyclic or oversized statistical result condition");
            try {
                Class<?> type = expression.getClass();
                if (type == PropertyReference.class) {
                    String key = ((PropertyReference) expression).getPropertyName();
                    if (!columns.containsKey(key)) throw invalid("Unknown or unselected statistical result: " + key);
                    return new PropertyReference(key);
                }
                if (type == Parameter.class) {
                    var value = (Parameter) expression;
                    // Nested/literal aliases may contain dots, spaces or colons.
                    // They must not become named SQL parameter syntax.
                    String name = (SensitiveLogNames.credential(value.getName()) ? "password_having" : "having") + parameter++;
                    return new Parameter(name, value.getValue(), value.getOperator());
                }
                if (!(expression instanceof FunctionApply function)) throw invalid("Unsupported statistical result condition");
                var operands = function.getExpressions();
                var operator = function.getOperator();
                if (type == AND.class || type == OR.class) {
                    if (operands.isEmpty()) throw invalid("Empty statistical result condition");
                    var children = new SearchCriteria[operands.size()];
                    for (int i = 0; i < children.length; i++) children[i] = criterion(operands.get(i), depth);
                    return type == AND.class ? new AND(children) : new OR(children);
                }
                if (type == NOT.class && operands.size() == 1) return new NOT(criterion(function.first(), depth));
                if ((type == TwoOperatorCriteria.class || type == EQ.class) && operands.size() == 2
                        && operator instanceof Operator comparison && comparison.hasTwoOperator() && comparison != Operator.SOUNDS_LIKE)
                    return new TwoOperatorCriteria(comparison, copy(function.first(), depth + 1), copy(function.second(), depth + 1));
                if (type == OneOperatorCriteria.class && operands.size() == 1
                        && operator instanceof Operator unary && unary.hasOneOperator())
                    return new OneOperatorCriteria(unary, copy(function.first(), depth + 1));
                if (type == Between.class && operands.size() == 3)
                    return new Between(copy(function.first(), depth + 1), copy(function.second(), depth + 1), copy(function.third(), depth + 1));
                if (type == FunctionApply.class && operator == Operator.SOUNDS_LIKE && operands.size() == 1)
                    return new FunctionApply(operator, copy(function.first(), depth + 1));
                throw invalid("Unsupported statistical result condition: " + type.getSimpleName());
            } finally { active.remove(expression); }
        }
        private SearchCriteria criterion(Expression expression, int depth) {
            Expression result = copy(expression, depth + 1);
            if (result instanceof SearchCriteria criteria) return criteria;
            throw invalid("Boolean statistical result condition required");
        }
    }
    private static TeaQLRuntimeException invalid(String message) { return new TeaQLRuntimeException(message); }
}
