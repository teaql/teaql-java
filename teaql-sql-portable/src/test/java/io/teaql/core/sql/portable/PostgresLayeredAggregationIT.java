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
        @Override public void clearQueryTrace() { queries.clear(); }
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
