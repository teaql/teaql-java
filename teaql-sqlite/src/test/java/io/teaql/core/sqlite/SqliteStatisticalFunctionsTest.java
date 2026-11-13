package io.teaql.core.sqlite;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.GenericSQLRelation;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqliteStatisticalFunctionsTest {
    private static final class Fixture implements AutoCloseable {
        final SingleConnectionDataSource source=new SingleConnectionDataSource("jdbc:sqlite::memory:");
        final JdbcSqlExecutor jdbc=new JdbcSqlExecutor(source);
        final SqliteDataServiceExecutor provider=new SqliteDataServiceExecutor("sqlite",jdbc,source);
        Map<String,Object> row(String sql,Object...args){return jdbc.queryForList(sql,args).get(0);}
        @Override public void close() throws Exception{source.close();}
    }
    private static double value(Map<String,Object> row,String name){return ((Number)row.get(name)).doubleValue();}
    @Test public void knownSamplePopulationAndStandardDeviationIgnoreNulls()throws Exception{
        try(var f=new Fixture()){
            var r=f.row("SELECT var_samp(v) sample,var_pop(v) population,stddev(v) sd,stddev_pop(v) sdp,stddev_samp(v) sds FROM(SELECT 800 v UNION ALL SELECT 800 UNION ALL SELECT 800 UNION ALL SELECT 800 UNION ALL SELECT 100 UNION ALL SELECT NULL)");
            assertEquals(98000,value(r,"sample"),0);assertEquals(78400,value(r,"population"),0);
            assertEquals(Math.sqrt(98000),value(r,"sd"),1e-10);assertEquals(280,value(r,"sdp"),0);assertEquals(value(r,"sd"),value(r,"sds"),0);
        }
    }
    @Test public void emptyNullAndSingletonDoNotReturnInventedIdentityValues()throws Exception{
        try(var f=new Fixture()){
            for(var data:List.of("SELECT 1 v WHERE 0","SELECT NULL v")){
                var r=f.row("SELECT var_samp(v) a,var_pop(v) b,stddev(v) c,bit_and(v) d,bit_or(v) e,bit_xor(v) f FROM("+data+")");
                assertTrue(r.values().stream().allMatch(Objects::isNull));
            }
            var r=f.row("SELECT var_samp(7) a,var_pop(7) b,stddev(7) c,stddev_pop(7) d");
            assertNull(r.get("a"));assertNull(r.get("c"));assertEquals(0,value(r,"b"),0);assertEquals(0,value(r,"d"),0);
        }
    }
    @Test public void neighboringLargeIntegersKeepTheirUnitDifference()throws Exception{
        try(var f=new Fixture()){
            for(var pair:new long[][]{{9007199254740992L,9007199254740993L},{Long.MAX_VALUE-1,Long.MAX_VALUE},{Long.MIN_VALUE,Long.MIN_VALUE+1}}){
                var r=f.row("SELECT var_samp(v) a,var_pop(v) b FROM(SELECT ? v UNION ALL SELECT ?)",pair[0],pair[1]);
                assertEquals(0.5,value(r,"a"),0);assertEquals(0.25,value(r,"b"),0);
            }
        }
    }
    @Test public void mixedIntegerAndRealCenteringPreservesRepresentableDifferences()throws Exception{
        try(var f=new Fixture()){
            for(var data:List.of("SELECT 9007199254740993 v UNION ALL SELECT 9007199254740992.0",
                    "SELECT 9007199254740992.0 v UNION ALL SELECT 9007199254740993",
                    "SELECT 9223372036854775807 v UNION ALL SELECT 9223372036854775808.0"))
                assertEquals(0.25,value(f.row("SELECT var_pop(v) a FROM("+data+")"),"a"),0);
        }
    }
    @Test public void bitwiseAggregatesPreserveSignedLongBits()throws Exception{
        try(var f=new Fixture()){
            var r=f.row("SELECT bit_and(v) a,bit_or(v) b,bit_xor(v) c FROM(SELECT ? v UNION ALL SELECT ? UNION ALL SELECT NULL)",-1L,Long.MIN_VALUE);
            assertEquals(Long.MIN_VALUE,((Number)r.get("a")).longValue());assertEquals(-1,((Number)r.get("b")).longValue());
            assertEquals(Long.MAX_VALUE,((Number)r.get("c")).longValue());
        }
    }
    @Test public void separateGroupsAndRepeatedQueriesHaveIndependentState()throws Exception{
        try(var f=new Fixture()){
            for(int attempt=0;attempt<2;attempt++){
                var rows=f.jdbc.queryForList("SELECT k,var_pop(v) a,bit_xor(v) b FROM(SELECT 1 k,1 v UNION ALL SELECT 1,3 UNION ALL SELECT 2,8) GROUP BY k ORDER BY k",new Object[0]);
                assertEquals(2,rows.size());assertEquals(1,value(rows.get(0),"a"),0);assertEquals(2,value(rows.get(0),"b"),0);
                assertEquals(0,value(rows.get(1),"a"),0);assertEquals(8,value(rows.get(1),"b"),0);
            }
        }
    }
    @Test public void invalidStorageAndOverflowFailWithoutEchoingBoundValues()throws Exception{
        try(var f=new Fixture()){
            var error=assertThrows(RuntimeException.class,()->f.row("SELECT var_pop(?)","PRIVATE-DECIMAL-CANARY"));
            assertCauseContains(error,"requires finite INTEGER or REAL");
            for(Throwable cause=error;cause!=null;cause=cause.getCause())assertFalse(String.valueOf(cause.getMessage()).contains("PRIVATE-DECIMAL-CANARY"));
            assertCauseContains(assertThrows(RuntimeException.class,()->f.row("SELECT var_pop(X'31')")),"requires finite INTEGER or REAL");
            assertCauseContains(assertThrows(RuntimeException.class,()->f.row("SELECT bit_xor(1.5)")),"requires INTEGER");
            assertCauseContains(assertThrows(RuntimeException.class,()->f.row("SELECT var_samp(v) FROM(SELECT -1e308 v UNION ALL SELECT 1e308)")),"exceeds finite numeric range");
        }
    }
    private static void assertCauseContains(Throwable error,String expected){
        for(Throwable cause=error;cause!=null;cause=cause.getCause())if(String.valueOf(cause.getMessage()).contains(expected))return;
        fail("Expected sanitized diagnostic: "+expected);
    }
    @Test public void installationIsLazyAndCoversEveryPhysicalConnection()throws Exception{
        var opened=new AtomicInteger();
        var source=new org.sqlite.SQLiteDataSource(){@Override public Connection getConnection()throws SQLException{opened.incrementAndGet();return super.getConnection();}};
        source.setUrl("jdbc:sqlite::memory:");var jdbc=new JdbcSqlExecutor(source);
        new SqliteDataServiceExecutor("sqlite",jdbc,source);assertEquals(0,opened.get());
        for(int i=1;i<=2;i++){
            assertEquals(0,((Number)jdbc.queryForList("SELECT var_pop(1) a",new Object[0]).get(0).get("a")).doubleValue(),0);
            assertEquals(i,opened.get());
        }
    }
    @Test public void modeledDecimalFunctionsRemainExplicitlyUnsupportedInsteadOfApproximate(){
        var dialect=new SqliteDialect();
        for(var function:List.of(AggrFunction.AVG,AggrFunction.STDDEV,AggrFunction.STDDEV_POP,AggrFunction.VAR_SAMP,AggrFunction.VAR_POP,AggrFunction.BIT_AND,AggrFunction.BIT_OR,AggrFunction.BIT_XOR))
            assertThrows(IllegalArgumentException.class,()->dialect.decimalAggregateSql(function,"amount",true));
    }
    @Test public void nonTextGbkOperandsFailBeforeDatabaseExecution()throws Exception{
        try(var f=new Fixture()){
            var metadata=new SimpleEntityMetaFactory();
            metadata.register(entity("StatKind",Kind.class,"missing_gbk_table",
                    new String[][]{{"id","id"},{"version","version"},{"label","label"}}));
            var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",f.provider).build());
            for(String key:List.of("types","id")){
                var request=new Request<Kind>(Kind.class,"StatKind");request.groupBy("label");request.count("types");
                request.addOrderByAscendingUsingGBK(key);request.top(1);
                var error=assertThrows(TeaQLRuntimeException.class,()->request.comment("Reject invalid GBK sorting")
                        .purpose("Prove typed sort validation precedes the missing database table").aggregation(context));
                String expected=key.equals("types")?"selected text result":"statistical result";
                assertTrue(error.getMessage(),error.getMessage().contains(expected));
                assertFalse(error.getMessage().contains("no such table"));
            }
        }
    }
    public static class School extends BaseEntity{@Override public String typeName(){return "StatSchool";}}

    @Test public void gbkOrdersOrdinaryParentAndNestedCompletedCells()throws Exception{
        try(var f=new Fixture()){
            var context=expressionContext(f,false);
            f.jdbc.execute("UPDATE expression_kind SET label='中' WHERE id=1001");
            f.jdbc.execute("UPDATE expression_kind SET label='阿' WHERE id=1002");
            f.jdbc.execute("INSERT INTO expression_kind VALUES(1003,1,'文',3),(1004,1,NULL,4)");
            f.jdbc.execute("INSERT INTO expression_school VALUES(6,1,50,1003)");
            for(int round=0;round<2;round++){
                var ordinary=new Request<>(Kind.class,"StatKind");ordinary.groupBy("label");ordinary.count("types");
                ordinary.appendSearchCriteria(new io.teaql.core.criteria.IsNotNull(new PropertyReference("label")));
                ordinary.addOrderByAscendingUsingGBK("label");ordinary.top(2);
                var rows=ordinary.comment("Order completed Chinese cells by encoded bytes")
                        .purpose("Distinguish GBK from unchanged UTF8 ordering").aggregation(context).toList();
                assertEquals(List.of("阿","文"),rows.stream().map(row->row.get("label")).toList());
                var descending=new Request<>(Kind.class,"StatKind");descending.groupBy("label");descending.count("types");
                descending.appendSearchCriteria(new io.teaql.core.criteria.IsNotNull(new PropertyReference("label")));
                descending.addOrderByDescendingUsingGBK("label");descending.offset(1,1);
                rows=descending.comment("Page completed Chinese cells in reverse GBK order")
                        .purpose("Apply offset after statistical sorting").aggregation(context).toList();
                assertEquals("文",rows.get(0).get("label"));
                for(boolean parentPage:List.of(true,false)){
                    var parent=new Request<>(Kind.class,"StatKind");parent.groupBy("label");parent.count("types");
                    var request=new Request<>(School.class,"StatSchool");request.count("schools");request.sum("total","capacity");
                    request.groupBy("kind",parent);
                    if(parentPage){parent.addOrderByAscendingUsingGBK("label");parent.offset(1,1);}
                    else{request.addOrderByAscendingUsingGBK("kind.label");request.top(1);}
                    rows=request.comment("Order a parent or final statistical cell")
                            .purpose("Keep source totals and parent navigation while applying GBK").aggregation(context).toList();
                    assertEquals(1,rows.size());assertEquals(1,((Number)rows.get(0).get("schools")).longValue());
                    assertEquals(parentPage?50:100,((Number)rows.get(0).get("total")).longValue());
                    assertEquals(parentPage?"文":"阿",((Map<?,?>)rows.get(0).get("kind")).get("label"));
                }
                var nulls=new Request<>(Kind.class,"StatKind");nulls.groupBy("label");nulls.count("types");
                nulls.appendSearchCriteria(new io.teaql.core.criteria.IsNull(new PropertyReference("label")));
                nulls.addOrderByAscendingUsingGBK("label");
                rows=nulls.comment("Retain null-valued GBK keys").purpose("Do not replace null with empty text").aggregation(context).toList();
                assertEquals(1,rows.size());assertNull(rows.get(0).get("label"));assertEquals(1,((Number)rows.get(0).get("types")).longValue());
            }
        }
    }

    private static UserContext expressionContext(Fixture f,boolean decimal)throws Exception{
        // Legacy-schema unit fixture: install the same connection primitives
        // used by ensureSchema, without reconciling these hand-bound tables.
        if(decimal){
            SqliteDecimalFunctions.register(f.source.getConnection());
            org.sqlite.Function.create(f.source.getConnection().unwrap(org.sqlite.SQLiteConnection.class),"soundex",new org.sqlite.Function(){
                @Override protected void xFunc()throws SQLException{result(SqliteDataServiceExecutor.sqliteCompatibleSoundex(value_text(0)));}
            },1,org.sqlite.Function.FLAG_DETERMINISTIC);
        }
        var metadata=new SimpleEntityMetaFactory();
        var kind=entity("StatKind",Kind.class,"expression_kind",new String[][]{{"id","id"},{"version","version"},{"label","label"},{"ordinal","Legacy Rank"}});
        if(decimal)kind.getProperties().stream().filter(p->p.getName().equals("ordinal")).findFirst().orElseThrow()
                .setType(new SimplePropertyType(java.math.BigDecimal.class));
        var school=entity("StatSchool",School.class,"expression_school",new String[][]{{"id","id"},{"version","version"},{"capacity","capacity"}});
        metadata.register(kind);metadata.register(school);
        var relation=(GenericSQLRelation)school.addObjectProperty(metadata,"kind","StatKind","schoolList",Kind.class,GenericSQLRelation::new);
        relation.setTableName("expression_school");relation.setColumnName("kind");relation.setColumnType("BIGINT");
        f.jdbc.execute("CREATE TABLE expression_kind(id BIGINT,version BIGINT,label TEXT,\"Legacy Rank\" "+(decimal?"TEXT":"BIGINT")+")");
        f.jdbc.execute("CREATE TABLE expression_school(id BIGINT,version BIGINT,capacity BIGINT,kind BIGINT)");
        f.jdbc.execute(decimal?"INSERT INTO expression_kind VALUES(1001,1,'shared','2'),(1002,1,'shared','10')"
                :"INSERT INTO expression_kind VALUES(1001,1,'shared',1),(1002,1,'shared',2)");
        f.jdbc.execute("INSERT INTO expression_school VALUES(1,1,800,1001),(2,1,800,1001),(3,1,800,1001),(4,1,800,1001),(5,1,100,1002)");
        return new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",f.provider).build());
    }
    @Test public void layeredExpressionOperatorsAndNullsRetainOwnerGrain()throws Exception{
        try(var f=new Fixture()){
            var context=expressionContext(f,false);
            for(var function:AggrFunction.values())if(function!=AggrFunction.SELF&&function!=AggrFunction.GBK){
                var parent=new Request<>(Kind.class,"StatKind");parent.groupBy("label");
                parent.addAggregate(new SimpleNamedExpression("computed",new AggrExpression(function,
                        new AggrExpression(AggrFunction.SELF,new PropertyReference("ordinal")))));
                var request=new Request<>(School.class,"StatSchool");request.count("schools");request.groupBy("kind",parent);
                var rows=request.comment("Evaluate scalar expressions at the parent observation grain")
                        .purpose("Verify all supported aggregate functions over mapped operands").aggregation(context).toList();
                assertEquals(1,rows.size());assertEquals(5,((Number)rows.get(0).get("schools")).longValue());
                var result=(Map<?,?>)rows.get(0).get("kind");
                double expected=switch(function){case COUNT,COUNT_DISTINCT,MAX->2;case SUM,BIT_OR,BIT_XOR->3;
                    case MIN->1;case AVG->1.5;case STDDEV->Math.sqrt(0.5);case STDDEV_POP,VAR_SAMP->0.5;case VAR_POP->0.25;default->0;};
                assertEquals(function.name(),expected,((Number)result.get("computed")).doubleValue(),1e-10);
                for(boolean empty:List.of(false,true)){
                    var nullParent=new Request<>(Kind.class,"StatKind");
                    nullParent.addAggregate(new SimpleNamedExpression("computed",new AggrExpression(function,
                            new AggrExpression(AggrFunction.SELF,new Parameter("nullable",null,io.teaql.core.criteria.Operator.EQUAL)))));
                    var outer=new Request<>(School.class,"StatSchool");outer.count("schools");outer.groupBy("kind",nullParent);
                    if(empty)outer.appendSearchCriteria(new io.teaql.core.criteria.IsNull(new PropertyReference("id")));
                    var values=outer.comment("Retain null and empty aggregate populations").purpose("Do not invent scalar identities")
                            .aggregation(context).toList();assertEquals(1,values.size());
                    var actual=((Map<?,?>)values.get(0).get("kind")).get("computed");
                    if(function==AggrFunction.COUNT||function==AggrFunction.COUNT_DISTINCT)assertEquals(0,((Number)actual).longValue());else assertNull(actual);
                }
            }
        }
    }
    @Test public void layeredDecimalAndTextExpressionsKeepPrecisionAndTypedResults()throws Exception{
        try(var f=new Fixture()){
            var context=expressionContext(f,true);var parent=new Request<>(Kind.class,"StatKind");parent.groupBy("label");
            for(var function:List.of(AggrFunction.MIN,AggrFunction.MAX,AggrFunction.SUM))
                parent.addAggregate(new SimpleNamedExpression(function.name(),new AggrExpression(function,
                        new AggrExpression(AggrFunction.SELF,new PropertyReference("ordinal")))));
            parent.addAggregate(new SimpleNamedExpression("literal",new AggrExpression(AggrFunction.SUM,
                    new Parameter("decimal",new java.math.BigDecimal("0.000000001"),io.teaql.core.criteria.Operator.EQUAL))));
            parent.addAggregate(new SimpleNamedExpression("labels",new AggrExpression(AggrFunction.COUNT_DISTINCT,
                    new FunctionApply(io.teaql.core.criteria.Operator.SOUNDS_LIKE,new PropertyReference("label")))));
            parent.having("MAX",io.teaql.core.criteria.Operator.GREATER_THAN,new java.math.BigDecimal("9"));
            parent.addOrderByDescending("SUM");parent.getSlice().setSize(1);
            var request=new Request<>(School.class,"StatSchool");request.count("schools");request.groupBy("kind",parent);
            var rows=request.comment("Keep exact decimal and transformed text operands")
                    .purpose("Verify computed input typing, collation and decoding").aggregation(context).toList();assertEquals(1,rows.size());
            var row=(Map<?,?>)rows.get(0).get("kind");
            for(var entry:Map.of("MIN","2","MAX","10","SUM","12","literal","0.000000002").entrySet()){
                assertTrue(row.get(entry.getKey()) instanceof java.math.BigDecimal);
                assertEquals(0,new java.math.BigDecimal(entry.getValue()).compareTo((java.math.BigDecimal)row.get(entry.getKey())));
            }
            assertEquals(1,((Number)row.get("labels")).longValue());
        }
    }

    @Test public void bitwiseParametersAndNestedSelfKeepExactIntegerResults()throws Exception{
        try(var f=new Fixture()){
            var metadata=new SimpleEntityMetaFactory();
            var kind=entity("StatKind",Kind.class,"bit_kind",new String[][]{{"id","id"},{"version","version"},{"label","label"}});
            var school=entity("StatSchool",School.class,"bit_school",new String[][]{{"id","id"},{"version","version"},{"capacity","capacity"}});
            metadata.register(kind);metadata.register(school);
            var relation=(GenericSQLRelation)school.addObjectProperty(metadata,"kind","StatKind","schoolList",Kind.class,GenericSQLRelation::new);
            relation.setTableName("bit_school");relation.setColumnName("kind");relation.setColumnType("BIGINT");
            f.jdbc.execute("CREATE TABLE bit_kind(id BIGINT,version BIGINT,label TEXT)");
            f.jdbc.execute("CREATE TABLE bit_school(id BIGINT,version BIGINT,capacity BIGINT,kind BIGINT)");
            f.jdbc.execute("INSERT INTO bit_kind VALUES(1001,1,'shared'),(1002,1,'shared')");
            f.jdbc.execute("INSERT INTO bit_school VALUES(1,1,800,1001),(2,1,800,1001),(3,1,800,1001),(4,1,800,1001),(5,1,100,1002)");
            var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",f.provider).build());
            for(var function:List.of(AggrFunction.BIT_AND,AggrFunction.BIT_OR,AggrFunction.BIT_XOR)){
                for(Long value:Arrays.asList(Long.MIN_VALUE,Long.MAX_VALUE,null)){
                    Expression operand=new Parameter("integerInput",value,io.teaql.core.criteria.Operator.EQUAL);
                    operand=new AggrExpression(AggrFunction.SELF,new AggrExpression(AggrFunction.SELF,operand));
                    var request=new Request<>(Kind.class,"StatKind");request.addAggregate(new SimpleNamedExpression("bits",new AggrExpression(function,operand)));
                    var rows=request.comment("Aggregate exact integer parameters").purpose("Retain signed bounds and typed null results").aggregation(context).toList();
                    assertEquals(1,rows.size());assertTrue(rows.get(0).containsKey("bits"));
                    var actual=rows.get(0).get("bits");
                    if(value==null)assertNull(actual);else{
                        assertTrue(actual instanceof Long || actual instanceof Integer);
                        assertEquals(function==AggrFunction.BIT_XOR?0:value.longValue(),((Number)actual).longValue());
                    }
                }
                Expression operand=new PropertyReference("id");for(int depth=0;depth<16;depth++)operand=new AggrExpression(AggrFunction.SELF,operand);
                var parent=new Request<>(Kind.class,"StatKind");parent.groupBy("label");
                parent.addAggregate(new SimpleNamedExpression("bits",new AggrExpression(function,operand)));
                var request=new Request<>(School.class,"StatSchool");request.count("schools");request.groupBy("kind",parent);
                var rows=request.comment("Aggregate wrapped parent fields once per identity").purpose("Preserve nested SELF and owner grain").aggregation(context).toList();
                assertEquals(1,rows.size());assertEquals(5,((Number)rows.get(0).get("schools")).longValue());
                var values=(Map<?,?>)rows.get(0).get("kind");
                assertEquals(function==AggrFunction.BIT_AND?1000:function==AggrFunction.BIT_OR?1003:3,((Number)values.get("bits")).longValue());
            }
        }
    }
    @Test public void invalidModeledBitwiseFieldsFailBeforeOrdinaryOrLayeredSql()throws Exception{
        try(var f=new Fixture()){
            for(Class<?> type:List.of(String.class,java.math.BigDecimal.class,Double.class,Float.class,
                    Boolean.class,java.time.LocalDate.class,java.time.LocalDateTime.class)){
                var metadata=new SimpleEntityMetaFactory();
                var kind=entity("StatKind",Kind.class,"missing_bitwise_kind",new String[][]{{"id","id"},{"version","version"},{"label","Legacy Kind Label"}});
                kind.getProperties().stream().filter(p->p.getName().equals("label")).findFirst().orElseThrow().setType(new SimplePropertyType(type));
                var school=entity("StatSchool",School.class,"missing_bitwise_school",new String[][]{{"id","id"},{"version","version"}});
                metadata.register(kind);metadata.register(school);
                var relation=(GenericSQLRelation)school.addObjectProperty(metadata,"kind","StatKind","schoolList",Kind.class,GenericSQLRelation::new);
                relation.setTableName("missing_bitwise_school");relation.setColumnName("kind");relation.setColumnType("BIGINT");
                var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",f.provider).build());
                for(var function:List.of(AggrFunction.BIT_AND,AggrFunction.BIT_OR,AggrFunction.BIT_XOR)){
                    var parent=new Request<>(Kind.class,"StatKind");parent.addAggregate("bits","label",function);
                    var child=new Request<>(School.class,"StatSchool");child.count("schools");child.groupBy("kind",parent);
                    for(Request<?> request:List.of(parent,child)){
                        request.top(1);
                        var error=assertThrows(TeaQLRuntimeException.class,()->request.comment("Reject invalid modeled bitwise operands")
                                .purpose("Prove type validation precedes database access").aggregation(context));
                        assertTrue(type+": "+error.getMessage(),error.getMessage().contains("bitwise aggregate requires an integer field"));
                        assertFalse(error.getMessage().contains("no such table"));
                        assertFalse(error.getMessage().contains("Legacy Kind Label"));
                    }
                }
            }
        }
    }
    public static class Kind extends BaseEntity{@Override public String typeName(){return "StatKind";}}
    private static class Request<T extends BaseEntity> extends BaseRequest<T>{
        private final String type;Request(Class<T> cls,String type){super(cls);this.type=type;}
        @Override public String getTypeName(){return type;}Request<T> comment(String comment){internalComment(comment);return this;}
    }
    private static EntityDescriptor entity(String type,Class<?extends BaseEntity> target,String table,String[][] fields){
        var d=new EntityDescriptor();d.setType(type);d.setTargetType(target);d.setDataService("sqlite");
        for(var field:fields){boolean text=field[0].equals("label");var p=new GenericSQLProperty(table,field[1],text?"TEXT":"BIGINT");
            p.setName(field[0]);p.setOwner(d);p.setType(new SimplePropertyType(text?String.class:Long.class));d.getProperties().add(p);}
        return d;
    }
    @Test public void realContextRollupDeduplicatesParentStatisticsBeforeAggregation()throws Exception{
        try(var f=new Fixture()){
            var metadata=new SimpleEntityMetaFactory();
            var kind=entity("StatKind",Kind.class,"stat_kind",new String[][]{{"id","id"},{"version","version"},{"label","label"},{"ordinal","ordinal"}});
            var school=entity("StatSchool",School.class,"stat_school",new String[][]{{"id","id"},{"version","version"},{"capacity","Legacy Capacity"}});
            metadata.register(kind);metadata.register(school);
            var relation=(GenericSQLRelation)school.addObjectProperty(metadata,"kind","StatKind","schoolList",Kind.class,GenericSQLRelation::new);
            relation.setTableName("stat_school");relation.setColumnName("Legacy Kind Key");relation.setColumnType("BIGINT");
            f.jdbc.execute("CREATE TABLE stat_kind(id INTEGER PRIMARY KEY,version INTEGER,label TEXT,ordinal INTEGER)");
            f.jdbc.execute("CREATE TABLE stat_school(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Capacity\" INTEGER,\"Legacy Kind Key\" INTEGER)");
            f.jdbc.execute("INSERT INTO stat_kind VALUES(1001,1,'shared',1),(1002,1,'shared',2)");
            f.jdbc.execute("INSERT INTO stat_school VALUES(1,1,800,1001),(2,1,800,1001),(3,1,800,1001),(4,1,800,1001),(5,1,100,1002)");
            var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("sqlite",f.provider).build());
            var parent=new Request<>(Kind.class,"StatKind");parent.groupBy("label");parent.count("types");parent.sampleVariance("variance","ordinal");parent.addAggregate("bits","ordinal",AggrFunction.BIT_XOR);
            var request=new Request<>(School.class,"StatSchool");request.count("schools");request.sampleVariance("variance","capacity");request.samplePopulationVariance("population","capacity");request.addAggregate("bits","capacity",AggrFunction.BIT_XOR);request.groupBy("kind",parent);
            var rows=request.comment("Compute each source and parent at its own identity grain").purpose("Verify real SQLite statistical kernel integration").aggregation(context).toList();
            assertEquals(1,rows.size());var row=rows.get(0);assertEquals(5,value(row,"schools"),0);assertEquals(98000,value(row,"variance"),0);assertEquals(78400,value(row,"population"),0);assertEquals(100,value(row,"bits"),0);
            @SuppressWarnings("unchecked")var type=(Map<String,Object>)row.get("kind");assertEquals(2,value(type,"types"),0);assertEquals(0.5,value(type,"variance"),0);assertEquals(3,value(type,"bits"),0);
        }
    }
}
