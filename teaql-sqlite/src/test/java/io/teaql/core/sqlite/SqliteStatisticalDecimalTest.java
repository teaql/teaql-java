package io.teaql.core.sqlite;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.GenericSQLRelation;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exact modeled values through the actual SQLite Context, not a numeric SQL mock. */
public class SqliteStatisticalDecimalTest {
    public static class School extends BaseEntity { @Override public String typeName() { return "NumericSchool"; } }
    public static class Kind extends BaseEntity { @Override public String typeName() { return "NumericKind"; } }
    private static class Request<T extends BaseEntity> extends BaseRequest<T> {
        private final String type;
        Request(Class<T> type, String name) { super(type); this.type=name; }
        @Override public String getTypeName() { return type; }
        Request<T> comment(String value) { internalComment(value); return this; }
    }
    private static class Fixture implements AutoCloseable {
        final SingleConnectionDataSource source=new SingleConnectionDataSource("jdbc:sqlite::memory:");
        final JdbcSqlExecutor jdbc=new JdbcSqlExecutor(source);
        final SqliteDataServiceExecutor provider=new SqliteDataServiceExecutor("sqlite",jdbc,source);
        final UserContext context;
        Fixture(String first,String second) throws Exception {
            SqliteDecimalFunctions.register(source.getConnection());
            var metadata=new SimpleEntityMetaFactory();
            var kind=entity("NumericKind",Kind.class,"numeric_kind");
            add(kind,"label","Legacy Label",String.class,"TEXT");
            add(kind,"ordinal","Legacy Rank",BigDecimal.class,"DECIMAL_TEXT");
            var school=entity("NumericSchool",School.class,"numeric_school");
            add(school,"capacity","Legacy Capacity",Long.class,"BIGINT");
            metadata.register(kind);metadata.register(school);
            var relation=(GenericSQLRelation)school.addObjectProperty(metadata,"kind","NumericKind","schoolList",Kind.class,GenericSQLRelation::new);
            relation.setTableName("numeric_school");relation.setColumnName("Legacy Kind Key");relation.setColumnType("BIGINT");
            jdbc.execute("CREATE TABLE numeric_kind(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Label\" TEXT,\"Legacy Rank\" DECIMAL_TEXT COLLATE TEAQL_DECIMAL)");
            jdbc.execute("CREATE TABLE numeric_school(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Capacity\" BIGINT,\"Legacy Kind Key\" BIGINT)");
            jdbc.update("INSERT INTO numeric_kind VALUES(1001,1,'shared',?),(1002,1,'shared',?)",new Object[]{new BigDecimal(first),new BigDecimal(second)});
            jdbc.execute("INSERT INTO numeric_school VALUES(1,1,800,1001),(2,1,800,1001),(3,1,800,1001),(4,1,800,1001),(5,1,100,1002)");
            context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",provider).build());
        }
        List<Map<String,Object>> execute(Request<?> request) {
            return request.comment("Select exact statistical cells at their own grain")
                    .purpose("Verify numeric order and precision through SQLite Context").aggregation(context).toList();
        }
        @Override public void close() throws Exception { source.close(); }
    }
    private static EntityDescriptor entity(String name,Class<? extends BaseEntity> target,String table) {
        var entity=new EntityDescriptor();entity.setType(name);entity.setTargetType(target);entity.setDataService("sqlite");
        add(entity,"id","id",Long.class,"BIGINT",table);add(entity,"version","version",Long.class,"BIGINT",table);
        return entity;
    }
    private static void add(EntityDescriptor owner,String name,String column,Class<?> type,String sqlType) {
        String table=owner.getProperties().get(0) instanceof GenericSQLProperty property ? property.getTableName() : null;
        add(owner,name,column,type,sqlType,table);
    }
    private static void add(EntityDescriptor owner,String name,String column,Class<?> type,String sqlType,String table) {
        var property=new GenericSQLProperty(table,column,sqlType);property.setName(name);property.setOwner(owner);
        property.setType(new SimplePropertyType(type));owner.getProperties().add(property);
    }
    private static Request<Kind> parent() {
        var request=new Request<>(Kind.class,"NumericKind");request.groupBy("id");request.sum("total","ordinal");return request;
    }
    private static Request<School> source(Request<Kind> parent) {
        var request=new Request<>(School.class,"NumericSchool");request.count("schools");request.sum("capacity","capacity");request.groupBy("kind",parent);return request;
    }
    private static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    @SuppressWarnings("unchecked") private static Map<String,Object> kind(Map<String,Object> row) { return (Map<String,Object>)row.get("kind"); }

    @Test public void ordinaryMeasurePageUsesExactNumericOrdering() throws Exception {
        for(var pair:List.of(new String[]{"2","10","1002"},new String[]{"-2","-10","1001"},
                new String[]{"9007199254740992.000000001","9007199254740992.000000002","1002"})) {
            try(var fixture=new Fixture(pair[0],pair[1])) {
                var request=parent();request.addOrderByDescending("total");request.top(1);
                for(int round=0;round<2;round++)assertEquals(Long.parseLong(pair[2]),number(fixture.execute(request).get(0),"id"));
            }
        }
    }
    @Test public void parentCellPageUsesExactNumericOrdering() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            var parent=parent();parent.addOrderByDescending("total");parent.top(1);
            var request=source(parent);
            for(int round=0;round<2;round++) {
                var rows=fixture.execute(request);assertEquals(1,rows.size());assertEquals(1,number(rows.get(0),"schools"));
                assertEquals(1002,number(kind(rows.get(0)),"id"));
                assertEquals(0,new BigDecimal("10").compareTo((BigDecimal)kind(rows.get(0)).get("total")));
            }
        }
    }
    @Test public void nestedFinalCellPageUsesExactNumericOrdering() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            var request=source(parent());request.addOrderByDescending("kind.total");request.top(1);
            var rows=fixture.execute(request);assertEquals(1,rows.size());assertEquals(1,number(rows.get(0),"schools"));
            assertEquals(1002,number(kind(rows.get(0)),"id"));
        }
    }
    @Test public void largeDecimalTotalNeverFallsBackToBinaryFloat() throws Exception {
        try(var fixture=new Fixture("79228162514264337593543950335","1")) {
            var request=new Request<>(Kind.class,"NumericKind");request.groupBy("label");request.sum("total","ordinal");
            var rows=fixture.execute(source(request));
            assertEquals(0,new BigDecimal("79228162514264337593543950336").compareTo((BigDecimal)kind(rows.get(0)).get("total")));
        }
    }
    @Test public void integerSumOverflowIsNotSilentlyApproximated() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            fixture.jdbc.update("UPDATE numeric_school SET \"Legacy Capacity\"=CASE id WHEN 1 THEN ? WHEN 2 THEN 1 ELSE 0 END",new Object[]{Long.MAX_VALUE});
            var parent=new Request<>(Kind.class,"NumericKind");parent.groupBy("label");
            var error=assertThrows(RuntimeException.class,()->fixture.execute(source(parent)));
            assertTrue(causes(error).contains("overflow"));assertFalse(causes(error).contains(Long.toString(Long.MAX_VALUE)));
        }
    }
    @Test public void unsupportedExactDecimalFunctionsAreRejected() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            for(var function:List.of(AggrFunction.AVG,AggrFunction.STDDEV,AggrFunction.STDDEV_POP,
                    AggrFunction.VAR_SAMP,AggrFunction.VAR_POP,AggrFunction.BIT_AND,AggrFunction.BIT_OR,AggrFunction.BIT_XOR)) {
                var request=new Request<>(Kind.class,"NumericKind");request.groupBy("label");request.addAggregate("result","ordinal",function);
                var error=assertThrows(RuntimeException.class,()->fixture.execute(source(request)));
                String expected=Set.of(AggrFunction.BIT_AND,AggrFunction.BIT_OR,AggrFunction.BIT_XOR).contains(function)
                        ?"bitwise aggregate requires an integer field":"SQLite exact Decimal aggregate";
                assertTrue(causes(error),causes(error).contains(expected));
            }
        }
    }

    @Test public void exactHavingUsesNumericAliasesForScalarComparisons() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            for(var rule:List.of(new Object[]{Operator.EQUAL,2L,1001L},new Object[]{Operator.NOT_EQUAL,2L,1002L},
                    new Object[]{Operator.GREATER_THAN,2L,1002L},new Object[]{Operator.GREATER_THAN_OR_EQUAL,10L,1002L},
                    new Object[]{Operator.LESS_THAN,10L,1001L},new Object[]{Operator.LESS_THAN_OR_EQUAL,2L,1001L})) {
                for(var threshold:List.of(rule[1],new BigDecimal(rule[1].toString()))) {
                    var request=parent();request.having("total",(Operator)rule[0],threshold);
                    var rows=fixture.execute(request);assertEquals(1,rows.size());assertEquals((long)rule[2],number(rows.get(0),"id"));
                }
            }
            var between=parent();between.having("total",Operator.BETWEEN,2L,2L);
            var rows=fixture.execute(between);assertEquals(1,rows.size());assertEquals(1001,number(rows.get(0),"id"));
            var membership=parent();membership.having("total",Operator.IN,2L);
            rows=fixture.execute(membership);assertEquals(1,rows.size());assertEquals(1001,number(rows.get(0),"id"));
            membership=parent();membership.having("total",Operator.NOT_IN,2L);
            rows=fixture.execute(membership);assertEquals(1,rows.size());assertEquals(1002,number(rows.get(0),"id"));
        }
    }
    @Test public void exactHavingRetainsTheParentAndNestedOwnerTypes() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            var parent=parent();parent.having("total",Operator.GREATER_THAN,2L);
            var rows=fixture.execute(source(parent));assertEquals(1,rows.size());assertEquals(1,number(rows.get(0),"schools"));
            var request=source(parent());request.having("kind.total",Operator.GREATER_THAN,new BigDecimal("2"));
            rows=fixture.execute(request);assertEquals(1,rows.size());assertEquals(1,number(rows.get(0),"schools"));
        }
    }
    @Test public void exactHavingRejectsApproximateThresholdsBeforeSqlite() throws Exception {
        try(var fixture=new Fixture("2","10")) {
            for(var threshold:List.of(2.0,Float.valueOf(2),Double.NaN,"PRIVATE-DECIMAL-CANARY")) {
                var request=parent();request.having("total",Operator.GREATER_THAN,threshold);
                var error=assertThrows(RuntimeException.class,()->fixture.execute(request));
                assertTrue(causes(error),causes(error).contains("exact numeric operand"));
                assertFalse(causes(error).contains("PRIVATE-DECIMAL-CANARY"));
            }
        }
    }
    private static String causes(Throwable error) {
        var result=new StringBuilder();for(var next=error;next!=null;next=next.getCause())result.append(next.getMessage()).append('\n');return result.toString();
    }
}
