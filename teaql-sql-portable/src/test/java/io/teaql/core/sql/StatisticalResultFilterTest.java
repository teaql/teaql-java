package io.teaql.core.sql;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.core.sql.expression.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticalResultFilterTest {
    private final SQLColumnResolver provider = new SQLColumnResolver() {
        @Override public List<SQLColumn> getPropertyColumns(String table, String property) { throw new AssertionError("No source column fallback"); }
        @Override public String escapeIdentifier(String name) { return "\"" + name.replace("\"", "\"\"") + "\""; }
        @Override public Map<Class, SQLExpressionParser> getExpressionParsers() {
            var result = new HashMap<Class, SQLExpressionParser>();
            for (var parser : List.of(new PropertyParser(), new ParameterParser(), new TwoOperatorExpressionParser(),
                    new OneOperatorExpressionParser(), new BetweenParser(), new ANDExpressionParser(),
                    new ORExpressionParser(), new NOTExpressionParser(), new FunctionApplyParser())) result.put(parser.type(), parser);
            return result;
        }
    };
    private final Map<String, StatisticalResultFilter.Column> columns = Map.of(
            "kind.privateTotal", new StatisticalResultFilter.Column("__teaql_m0", SqlParameterLogPolicy.MASKED),
            "plainTotal", new StatisticalResultFilter.Column("__teaql_m1", SqlParameterLogPolicy.PLAIN));

    private String apply(SearchCriteria condition, SqlParameters parameters) {
        var context = new io.teaql.runtime.DefaultUserContext(io.teaql.runtime.TeaQLRuntime.builder()
                .metadata(new io.teaql.core.meta.SimpleEntityMetaFactory()).build());
        return StatisticalResultFilter.apply(context, "SELECT 1", condition, columns, provider, parameters);
    }
    private static SearchCriteria comparison(String alias, String parameter, Object value) {
        return new TwoOperatorCriteria(Operator.GREATER_THAN, new PropertyReference(alias), new Parameter(parameter, value, Operator.GREATER_THAN));
    }
    @Test public void nestedAliasUsesSafeParameterNamesAndUnderlyingMaskPolicy() {
        var parameters = new SqlParameters();
        String sql = apply(comparison("kind.privateTotal", "kind.privateTotal", 123L), parameters);
        assertTrue(sql.contains("\"__teaql_m0\" > :having0"));
        assertEquals(123L, parameters.get("having0")); assertFalse(parameters.containsKey("kind.privateTotal"));
        assertEquals(SqlParameterLogPolicy.MASKED, parameters.policy("having0"));
        assertTrue(parameters.generated());
    }
    @Test public void credentialParameterCannotBeDowngradedByAPlainAlias() {
        var parameters = new SqlParameters(); apply(comparison("plainTotal", "password", 123L), parameters);
        assertEquals(SqlParameterLogPolicy.CREDENTIAL, parameters.policy("password_having0"));
    }
    @Test public void unknownAliasFailsBeforeBindingAnyValue() {
        var parameters = new SqlParameters();
        assertThrows(TeaQLRuntimeException.class, () -> apply(SearchCriteria.and(
                comparison("plainTotal", "first", 1L), comparison("notSelected", "second", 2L)), parameters));
        assertTrue(parameters.isEmpty());
    }
    @Test public void aggregateOverAggregateIsNotMistakenForAnAlias() {
        var condition = new TwoOperatorCriteria(Operator.GREATER_THAN,
                new AggrExpression(AggrFunction.SUM, new PropertyReference("plainTotal")),
                new Parameter("value", 1L, Operator.GREATER_THAN));
        assertThrows(TeaQLRuntimeException.class, () -> apply(condition, new SqlParameters()));
    }
    @Test public void cyclesAndExcessiveDepthAreRejectedBeforeCompilation() {
        var cyclic = new AND(comparison("plainTotal", "first", 1L)); cyclic.getExpressions().add(cyclic);
        assertThrows(TeaQLRuntimeException.class, () -> apply(cyclic, new SqlParameters()));
        SearchCriteria deep = comparison("plainTotal", "first", 1L);
        for (int i = 0; i < 70; i++) deep = new NOT(deep);
        SearchCriteria condition = deep;
        assertThrows(TeaQLRuntimeException.class, () -> apply(condition, new SqlParameters()));
    }
    @Test public void customSqlExpressionIsNeverInvoked() {
        class Raw implements SearchCriteria, SQLExpressionParser<Raw> {
            boolean invoked;
            @Override public String toSql(UserContext context, Raw value, String table, Map<String, Object> params, SQLColumnResolver resolver) {
                invoked = true; return "1=1";
            }
        }
        var raw = new Raw(); assertThrows(TeaQLRuntimeException.class, () -> apply(raw, new SqlParameters()));
        assertFalse(raw.invoked);
    }
}
