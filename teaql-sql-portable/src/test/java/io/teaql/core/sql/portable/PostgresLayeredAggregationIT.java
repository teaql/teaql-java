package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import java.sql.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Runs every shared SQLite assertion unchanged, plus PostgreSQL extended
 * measures. Explicit IT gate only: a missing live database is a failure. */
public class PostgresLayeredAggregationIT extends LayeredAggregationTest {
    private static String url() {
        String url = System.getenv("TEAQL_TEST_POSTGRES_JDBC_URL");
        if (url == null || url.isBlank()) throw new IllegalStateException("TEAQL_TEST_POSTGRES_JDBC_URL is required");
        return url;
    }

    @Override protected FixtureDatabase createDatabase() throws Exception {
        Class.forName("org.postgresql.Driver");
        var properties = new Properties();
        properties.setProperty("user", System.getenv("TEAQL_TEST_POSTGRES_USER"));
        properties.setProperty("password", System.getenv("TEAQL_TEST_POSTGRES_PASSWORD"));
        var connection = DriverManager.getConnection(url(), properties);
        try (var statement = connection.createStatement(); var result = statement.executeQuery(
                "SELECT current_database(),shobj_description(oid,'pg_database') FROM pg_database WHERE datname=current_database()")) {
            assertTrue(result.next());
            assertEquals("teaql_rollup_279", result.getString(1));
            assertEquals("TeaQL #279 owned layered statistics fixture", result.getString(2));
        } catch (Throwable failure) { connection.close(); throw failure; }
        connection.setAutoCommit(false);
        return new JdbcFixture(connection);
    }

    private static final class JdbcFixture implements FixtureDatabase {
        private final List<SqlLogBindings> bindings = new ArrayList<>();
        @Override public List<Map<String,Object>> query(io.teaql.core.UserContext context, String sql, Object[] args, SqlLogBindings logBindings) {
            bindings.add(logBindings); return query(sql, args);
        }
        @Override public List<SqlLogBindings> statementBindings() { return List.copyOf(bindings); }
        private final Connection connection;
        private final List<String> queries = new ArrayList<>();
        JdbcFixture(Connection connection) { this.connection = connection; }
        private PreparedStatement prepare(String sql, Object[] args) throws SQLException {
            var statement = connection.prepareStatement(sql);
            try {
                for (int i=0; i<args.length; i++) statement.setObject(i+1,args[i]);
                return statement;
            } catch (Throwable failure) { statement.close(); throw failure; }
        }
        @Override public List<Map<String,Object>> query(String sql, Object[] args) {
            queries.add(sql);
            try (var statement = prepare(sql,args); var result = statement.executeQuery()) {
                var rows = new ArrayList<Map<String,Object>>(); var metadata = result.getMetaData();
                while(result.next()) {
                    var row = new LinkedHashMap<String,Object>();
                    for(int i=1; i<=metadata.getColumnCount(); i++) row.put(metadata.getColumnLabel(i),result.getObject(i));
                    rows.add(row);
                }
                return rows;
            } catch(SQLException error) { throw new RuntimeException(error); }
        }
        @Override public int executeUpdate(String sql, Object[] args) {
            try(var statement=prepare(sql,args)) { return statement.executeUpdate(); }
            catch(SQLException error) { throw new RuntimeException(error); }
        }
        @Override public int[] batchUpdate(String sql, List<Object[]> args) {
            try(var statement=connection.prepareStatement(sql)) {
                for(var values:args) { for(int i=0;i<values.length;i++) statement.setObject(i+1,values[i]); statement.addBatch(); }
                return statement.executeBatch();
            } catch(SQLException error) { throw new RuntimeException(error); }
        }
        @Override public void execute(String sql) {
            try(var statement=connection.createStatement()) { statement.execute(sql); }
            catch(SQLException error) { throw new RuntimeException(error); }
        }
        @Override public void executeInTransaction(Runnable action) { action.run(); }
        @Override public List<Map<String,Object>> getTableColumns(String table) {
            throw new UnsupportedOperationException("The statistics fixture does not emulate schema inspection");
        }
        @Override public void clearQueryTrace() { queries.clear(); bindings.clear(); }
        @Override public List<String> queryTrace() { return List.copyOf(queries); }
        @Override public void close() throws SQLException {
            try { connection.rollback(); } finally { connection.close(); }
        }
    }

    private static void measures(BaseRequest<?> request, String field) {
        request.countProperty("nonnull",field); request.sum("total",field);
        request.min("minimum",field); request.max("maximum",field); request.avg("mean",field);
        request.standardDeviation("stddev",field);
        request.squareRootOfPopulationStandardDeviation("stddevPop",field);
        request.sampleVariance("variance",field); request.samplePopulationVariance("variancePop",field);
        request.addAggregate("bitAnd",field,AggrFunction.BIT_AND);
        request.addAggregate("bitOr",field,AggrFunction.BIT_OR);
        request.addAggregate("bitXor",field,AggrFunction.BIT_XOR);
    }

    @Test public void gbkOrdersSelectedChineseCellsAtEveryLayer() throws Exception {
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=CASE id WHEN 1001 THEN '中' ELSE '阿' END");
        database.execute("INSERT INTO rollup_kind VALUES(1003,1,'文',3,1)");
        database.execute("INSERT INTO rollup_school VALUES(6,1,300,1003)");
        var oracle=database.query("SELECT \"Legacy Kind Label\" label,encode(convert_to(\"Legacy Kind Label\",'GBK'),'hex') bytes FROM rollup_kind ORDER BY convert_to(\"Legacy Kind Label\",'GBK')",new Object[0]);
        assertEquals(List.of("阿","文","中"),oracle.stream().map(r->r.get("label")).toList());
        assertEquals(List.of("b0a2","cec4","d6d0"),oracle.stream().map(r->r.get("bytes")).toList());
        var ordinary=kind("label");ordinary.count("types");ordinary.addOrderByAscendingUsingGBK("label");ordinary.top(2);
        var rows=ordinary.comment("Order completed Chinese cells by their GBK bytes")
                .purpose("Distinguish GBK sort from native UTF8 order").aggregation(context).toList();
        assertEquals(List.of("阿","文"),rows.stream().map(r->r.get("label")).toList());observe("gbk-ordinary",rows);
        var parent=kind("label");parent.count("types");parent.addOrderByAscendingUsingGBK("label");parent.top(1);
        rows=execute(source(parent));assertGbkFirst(rows);observe("gbk-parent",rows);
        var nested=source(kind("label"));nested.addOrderByAscendingUsingGBK("kind.label");nested.top(1);
        rows=execute(nested);assertGbkFirst(rows);observe("gbk-nested",rows);
    }

    @Test public void binaryGbkGroupingPreservesKeysNullsAndParentGrain() throws Exception {
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=CASE id WHEN 1001 THEN '中' ELSE '阿' END");
        database.execute("INSERT INTO rollup_kind VALUES(1003,1,'',3,1),(1004,1,NULL,4,1)");
        for (byte[] bytes : List.of(new byte[0], new byte[]{0,(byte)0xff,0x27}, new byte[]{(byte)0xb0,(byte)0xa2}))
            assertArrayEquals(bytes, (byte[]) database.query("SELECT ?::bytea encoded", new Object[]{bytes}).get(0).get("encoded"));
        assertNull(database.query("SELECT ?::bytea encoded", new Object[]{null}).get(0).get("encoded"));
        var ordinary = new Request<>(StatKind.class,"StatKind");
        ordinary.groupBy("encoded","label",AggrFunction.GBK); ordinary.count("types");
        ordinary.addOrderByAscending("encoded"); ordinary.top(10);
        var rows = ordinary.comment("Group by exact GBK bytes").purpose("Verify binary result keys on PostgreSQL").aggregation(context).toList();
        assertEquals(4,rows.size());
        assertArrayEquals(new byte[0],(byte[])rows.get(0).get("encoded"));
        assertArrayEquals(new byte[]{(byte)0xb0,(byte)0xa2},(byte[])rows.get(1).get("encoded"));
        assertArrayEquals(new byte[]{(byte)0xd6,(byte)0xd0},(byte[])rows.get(2).get("encoded"));
        assertNull(rows.get(3).get("encoded")); observe("gbk-binary-ordinary",rows);
        var parent = new Request<>(StatKind.class,"StatKind");
        parent.groupBy("encoded","label",AggrFunction.GBK); parent.count("types");
        parent.where("label",Operator.EQUAL,"阿"); parent.addOrderByAscending("encoded"); parent.top(1);
        rows = execute(source(parent)); assertEquals(1,rows.size());
        assertArrayEquals(new byte[]{(byte)0xb0,(byte)0xa2},(byte[])object(rows.get(0),"kind").get("encoded"));
        assertEquals(1,number(rows.get(0),"schoolCount")); assertEquals(100,number(rows.get(0),"capacityTotal"));
        observe("gbk-binary-parent",rows);
    }

    @SuppressWarnings("unchecked") private static void assertGbkFirst(List<Map<String,Object>> rows) {
        assertEquals(1,rows.size());assertEquals(1,number(rows.get(0),"schoolCount"));
        assertEquals(100,number(rows.get(0),"capacityTotal"));
        assertEquals("阿",((Map<String,Object>)rows.get(0).get("kind")).get("label"));
    }

    @Test public void exactDecimalOrderingPagesTheCompletedCellsAtEveryLayer() throws Exception {
        var property=(io.teaql.core.sql.GenericSQLProperty)metadata.resolveEntityDescriptor("StatKind")
                .getProperties().stream().filter(item->item.getName().equals("ordinal")).findFirst().orElseThrow();
        property.setType(new io.teaql.core.meta.SimplePropertyType(java.math.BigDecimal.class));
        property.setColumnType("NUMERIC");
        database.execute("ALTER TABLE rollup_kind ALTER COLUMN ordinal TYPE NUMERIC USING ordinal::numeric");
        for(var sample:List.of(new String[]{"positive","2","10","1002"},new String[]{"negative","-2","-10","1001"},
                new String[]{"neighboring","9007199254740992.000000001","9007199254740992.000000002","1002"})) {
            database.executeUpdate("UPDATE rollup_kind SET ordinal=CASE id WHEN 1001 THEN ? ELSE ? END",
                    new Object[]{new java.math.BigDecimal(sample[1]),new java.math.BigDecimal(sample[2])});
            long larger=Long.parseLong(sample[3]),schools=larger==1001?4:1;
            var ordinary=kind("id");ordinary.sum("total","ordinal");ordinary.addOrderByDescending("total");ordinary.top(1);
            var rows=ordinary.comment("Page exact numeric statistical cells")
                    .purpose("Verify Decimal order without binary float conversion").aggregation(context).toList();
            assertEquals(1,rows.size());assertEquals(larger,number(rows.get(0),"id"));
            observe("numeric-ordinary-"+sample[0],rows);
            var parent=kind("id");parent.sum("total","ordinal");parent.addOrderByDescending("total");parent.top(1);
            rows=execute(source(parent));assertEquals(1,rows.size());assertEquals(schools,number(rows.get(0),"schoolCount"));
            assertEquals(larger,number(object(rows.get(0),"kind"),"id"));
            observe("numeric-parent-"+sample[0],rows);
            parent=kind("id");parent.sum("total","ordinal");
            var request=source(parent);request.addOrderByDescending("kind.total");request.top(1);
            rows=execute(request);assertEquals(1,rows.size());assertEquals(schools,number(rows.get(0),"schoolCount"));
            assertEquals(larger,number(object(rows.get(0),"kind"),"id"));
            observe("numeric-nested-"+sample[0],rows);
            var threshold=new java.math.BigDecimal(larger==1001?sample[2]:sample[1]);
            var expected=new java.math.BigDecimal(larger==1001?sample[1]:sample[2]);
            var allowed=kind("id");allowed.sum("total","ordinal");allowed.having("total",Operator.GREATER_THAN,threshold);
            rows=allowed.comment("Filter exact completed Decimal totals")
                    .purpose("Verify selected alias types and numeric thresholds").aggregation(context).toList();
            assertEquals(1,rows.size());assertEquals(larger,number(rows.get(0),"id"));
            assertEquals(0,expected.compareTo((java.math.BigDecimal)rows.get(0).get("total")));
            observe("numeric-having-ordinary-"+sample[0],rows);
            parent=kind("id");parent.sum("total","ordinal");parent.having("total",Operator.GREATER_THAN,threshold);
            rows=execute(source(parent));assertEquals(1,rows.size());assertEquals(schools,number(rows.get(0),"schoolCount"));
            assertEquals(0,expected.compareTo((java.math.BigDecimal)object(rows.get(0),"kind").get("total")));
            observe("numeric-having-parent-"+sample[0],rows);
            parent=kind("id");parent.sum("total","ordinal");
            request=source(parent);request.having("kind.total",Operator.GREATER_THAN,threshold);
            rows=execute(request);assertEquals(1,rows.size());assertEquals(schools,number(rows.get(0),"schoolCount"));
            assertEquals(0,expected.compareTo((java.math.BigDecimal)object(rows.get(0),"kind").get("total")));
            observe("numeric-having-nested-"+sample[0],rows);
        }
    }
    private static void numeric(Map<String,Object> row, String key, double expected) {
        assertNotNull(key,row.get(key));
        assertEquals(key,expected,((Number)row.get(key)).doubleValue(),1e-10*Math.max(1,Math.abs(expected)));
    }
    private void observe(String label,List<Map<String,Object>> rows) throws Exception {
        System.out.println("JAVA_POSTGRES_ROLLUP="+new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Map.of("case",label,"rows",rows)));
    }

    @Test public void bitwiseExpressionsRetainWrappedOwnerFieldsAndSignedParameters()throws Exception{
        List<Map<String,Object>> fieldRows=List.of(),parameterRows=List.of();
        for(var function:List.of(AggrFunction.BIT_AND,AggrFunction.BIT_OR,AggrFunction.BIT_XOR)){
            Expression operand=new PropertyReference("id");for(int depth=0;depth<16;depth++)operand=new AggrExpression(AggrFunction.SELF,operand);
            var parent=kind("label");parent.addAggregate(new SimpleNamedExpression("bits",new AggrExpression(function,operand)));
            var rows=execute(source(parent));assertEquals(1,rows.size());assertEquals(5,number(rows.get(0),"schoolCount"));
            var bits=object(rows.get(0),"kind").get("bits");assertTrue(bits instanceof Long || bits instanceof Integer);
            assertEquals(function==AggrFunction.BIT_AND?1000:function==AggrFunction.BIT_OR?1003:3,((Number)bits).longValue());
            if(function==AggrFunction.BIT_XOR)fieldRows=rows;
            for(Long value:Arrays.asList(Long.MIN_VALUE,Long.MAX_VALUE,null)){
                Expression parameter=new Parameter("integerInput",value,Operator.EQUAL);
                parameter=new AggrExpression(AggrFunction.SELF,new AggrExpression(AggrFunction.SELF,parameter));
                var request=new Request<>(StatKind.class,"StatKind");request.addAggregate(new SimpleNamedExpression("bits",new AggrExpression(function,parameter)));
                var actual=request.comment("Aggregate exact integer parameters")
                        .purpose("Disambiguate PostgreSQL overloads without changing values").aggregation(context).toList();
                assertEquals(1,actual.size());assertTrue(actual.get(0).containsKey("bits"));var result=actual.get(0).get("bits");
                if(value==null)assertNull(result);else{
                    assertTrue(result instanceof Long || result instanceof Integer);
                    assertEquals(function==AggrFunction.BIT_XOR?0:value.longValue(),((Number)result).longValue());
                }
                if(Objects.equals(value,Long.MAX_VALUE)&&function==AggrFunction.BIT_XOR)parameterRows=actual;
            }
        }
        observe("bitwise-expression-fields",fieldRows);observe("bitwise-expression-parameters",parameterRows);
    }

    @Test public void extendedOperatorsRetainSourceAndParentIdentityGrain() throws Exception {
        var parent = kind("label"); parent.count("n"); measures(parent,"ordinal");
        var request = source(parent); measures(request,"capacity");
        var rows = execute(request); assertEquals(1,rows.size()); var row=rows.get(0); var type=object(row,"kind");
        for(var expected:Map.of("nonnull",5L,"total",3300L,"minimum",100L,"maximum",800L,"bitAnd",32L,"bitOr",868L,"bitXor",100L).entrySet())
            assertEquals(expected.getKey(),expected.getValue().longValue(),number(row,expected.getKey()));
        for(var expected:Map.of("mean",660.0,"variance",98000.0,"variancePop",78400.0,"stddev",Math.sqrt(98000),"stddevPop",280.0).entrySet())
            numeric(row,expected.getKey(),expected.getValue());
        assertEquals(2,number(type,"n")); assertEquals(2,number(type,"nonnull"));
        assertEquals(1,number(type,"minimum")); assertEquals(2,number(type,"maximum")); assertEquals(3,number(type,"total"));
        numeric(type,"mean",1.5); numeric(type,"variance",0.5); numeric(type,"variancePop",0.25);
        numeric(type,"stddev",Math.sqrt(0.5)); numeric(type,"stddevPop",0.5);
        assertEquals(0,number(type,"bitAnd")); assertEquals(3,number(type,"bitOr")); assertEquals(3,number(type,"bitXor"));
        var oracle=database.query("SELECT COUNT(*) AS n,SUM(\"Legacy Capacity\") AS total,AVG(\"Legacy Capacity\") AS mean,VAR_SAMP(\"Legacy Capacity\") AS variance,VAR_POP(\"Legacy Capacity\") AS population FROM rollup_school",new Object[0]).get(0);
        assertEquals(number(oracle,"n"),number(row,"schoolCount")); assertEquals(number(oracle,"total"),number(row,"total"));
        numeric(row,"mean",((Number)oracle.get("mean")).doubleValue());
        numeric(row,"variance",((Number)oracle.get("variance")).doubleValue());
        numeric(row,"variancePop",((Number)oracle.get("population")).doubleValue());
        observe("extended-owner-grain",rows);
    }

    @Test public void layeredExpressionMeasuresRetainTypedParametersAndOwnerGrain()throws Exception{
        var parent=kind("platform");
        parent.addAggregate(new SimpleNamedExpression("perType",new AggrExpression(AggrFunction.SUM,new Parameter("constant",7L,Operator.EQUAL))));
        parent.addAggregate(new SimpleNamedExpression("parentXor",new AggrExpression(AggrFunction.BIT_XOR,
                new AggrExpression(AggrFunction.SELF,new PropertyReference("id")))));
        parent.having("perType",Operator.EQUAL,14L);parent.addOrderByDescending("perType");parent.getSlice().setSize(1);
        var request=source(parent);request.addAggregate(new SimpleNamedExpression("perSchool",new AggrExpression(AggrFunction.SUM,
                new Parameter("constant",3L,Operator.EQUAL))));
        request.having("perSchool",Operator.EQUAL,15L);request.addOrderByDescending("kind.parentXor");
        var rows=execute(request);assertEquals(1,rows.size());assertEquals(15,number(rows.get(0),"perSchool"));
        assertEquals(14,number(object(rows.get(0),"kind"),"perType"));assertEquals(3,number(object(rows.get(0),"kind"),"parentXor"));
        var oracle=database.query("SELECT SUM(7) total FROM rollup_kind WHERE id IN(1001,1002)",new Object[0]).get(0);
        assertEquals(number(oracle,"total"),number(object(rows.get(0),"kind"),"perType"));
        observe("expression-measure-owner-grain",rows);
        for(var function:AggrFunction.values())if(function!=AggrFunction.SELF&&function!=AggrFunction.GBK){
            parent=kind("platform");parent.addAggregate(new SimpleNamedExpression("computed",new AggrExpression(function,
                    new AggrExpression(AggrFunction.SELF,new PropertyReference("ordinal")))));
            rows=execute(source(parent));double expected=switch(function){case COUNT,COUNT_DISTINCT,MAX->2;case SUM,BIT_OR,BIT_XOR->3;
                case MIN->1;case AVG->1.5;case STDDEV->Math.sqrt(0.5);case STDDEV_POP,VAR_SAMP->0.5;case VAR_POP->0.25;default->0;};
            numeric(object(rows.get(0),"kind"),"computed",expected);
            parent=new Request<>(StatKind.class,"StatKind");parent.addAggregate(new SimpleNamedExpression("computed",new AggrExpression(function,
                    new AggrExpression(AggrFunction.SELF,new Parameter("nullable",null,Operator.EQUAL)))));
            for(boolean empty:List.of(false,true)){
                request=source(parent);if(empty)request.where("id",Operator.LESS_THAN,0L);
                rows=execute(request);assertEquals(1,rows.size());var value=object(rows.get(0),"kind").get("computed");
                if(function==AggrFunction.COUNT||function==AggrFunction.COUNT_DISTINCT)assertEquals(0,((Number)value).longValue());else assertNull(value);
            }
        }
        parent=new Request<>(StatKind.class,"StatKind");
        parent.addAggregate(new SimpleNamedExpression("labels",new AggrExpression(AggrFunction.COUNT_DISTINCT,
                new FunctionApply(Operator.SOUNDS_LIKE,new PropertyReference("label")))));
        parent.addAggregate(new SimpleNamedExpression("decimal",new AggrExpression(AggrFunction.SUM,
                new Parameter("decimal",new java.math.BigDecimal("0.000000001"),Operator.EQUAL))));
        rows=execute(source(parent));assertEquals(1,number(object(rows.get(0),"kind"),"labels"));
        assertTrue(object(rows.get(0),"kind").get("decimal") instanceof java.math.BigDecimal);
        assertEquals(0,new java.math.BigDecimal("0.000000002").compareTo((java.math.BigDecimal)object(rows.get(0),"kind").get("decimal")));
        observe("expression-measure-transforms",rows);
    }

    @Test public void emptyExtendedMeasuresKeepCountsZeroAndOtherValuesNull() throws Exception {
        var parent=new Request<>(StatKind.class,"StatKind"); parent.count("n"); measures(parent,"ordinal");
        var request=source(parent).where("capacity",Operator.LESS_THAN,0L); measures(request,"capacity");
        var rows=execute(request); assertEquals(1,rows.size());
        for(var owner:List.of(rows.get(0),object(rows.get(0),"kind"))) {
            assertEquals(0,number(owner,"nonnull"));
            for(var key:List.of("total","minimum","maximum","mean","stddev","stddevPop","variance","variancePop","bitAnd","bitOr","bitXor")) assertNull(key,owner.get(key));
        }
        assertEquals(0,number(rows.get(0),"schoolCount")); assertEquals(0,number(object(rows.get(0),"kind"),"n"));
        observe("empty-extended",rows);
    }

    @Test public void nullSourceMeasuresAndSingletonParentDistinguishSampleAndPopulation() throws Exception {
        database.execute("UPDATE rollup_school SET \"Legacy Capacity\"=NULL WHERE id=5");
        var parent=kind("label"); parent.count("n"); measures(parent,"ordinal");
        var request=source(parent).where("id",Operator.EQUAL,5L); measures(request,"capacity");
        var rows=execute(request); assertEquals(1,rows.size()); var row=rows.get(0); var type=object(row,"kind");
        assertEquals(1,number(row,"schoolCount")); assertEquals(0,number(row,"nonnull"));
        for(var key:List.of("total","minimum","maximum","mean","stddev","stddevPop","variance","variancePop","bitAnd","bitOr","bitXor")) assertNull(key,row.get(key));
        assertEquals(1,number(type,"n")); assertNull(type.get("variance")); assertNull(type.get("stddev"));
        numeric(type,"variancePop",0); numeric(type,"stddevPop",0);
        observe("all-null-source-singleton-parent",rows);
    }

    @Test public void parentAndSourceVarianceHavingRunAfterAggregation() throws Exception {
        var parent=kind("label"); measures(parent,"ordinal"); parent.having("variancePop",Operator.EQUAL,new java.math.BigDecimal("0.25"));
        var request=source(parent); measures(request,"capacity"); request.having("variance",Operator.GREATER_THAN,97000L);
        var rows=execute(request); assertEquals(1,rows.size()); assertEquals(5,number(rows.get(0),"schoolCount"));
        parent.having("variance",Operator.LESS_THAN,new java.math.BigDecimal("0.5"));
        assertTrue(execute(request).isEmpty()); observe("variance-having",rows);
    }
}
