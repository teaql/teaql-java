package com.example.scalartypeconformanceservice;

import com.example.scalartypeconformanceservice.scalartypeprobe.ScalarTypeProbe;
import com.example.scalartypeconformanceservice.typeplatform.TypePlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Application-owned generated Q/E acceptance; DDL below is isolated fault injection only. */
public class JavaEmptyAggregateTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final String PURPOSE = "verify generated empty aggregate contract";
    static final class Context extends DefaultUserContext implements ScalarTypeConformanceServiceUserContext {
        final List<ExecutionMetadata> facts = new ArrayList<>();
        Context(TeaQLRuntime runtime) { super(runtime); }
        @Override public void recordExecutionMetadata(ExecutionMetadata fact) {
            facts.add(fact); super.recordExecutionMetadata(fact);
        }
    }
    static final class Fixture {
        final List<String> driverSQL = new ArrayList<>();
        final List<ExecutionMetadata> logs = new ArrayList<>();
        final List<RawAuditEvent> audit = new ArrayList<>();
        final Context context;
        Fixture(String url, int round, boolean logging) {
            var source = new SQLiteDataSource(); source.setUrl(url);
            var driver = new JdbcSqlExecutor(source) {
                @Override public List<Map<String,Object>> queryForList(String sql, Object[] args) {
                    driverSQL.add(sql); return super.queryForList(sql,args);
                }
                @Override public <T extends Entity> List<T> query(String sql, Object[] args, CompiledRowMapper<T> mapper) {
                    driverSQL.add(sql); return super.query(sql,args,mapper);
                }
            };
            var service = new SqliteDataServiceExecutor("sqlite",driver,source);
            var ids = new AtomicLong(round*100000L);
            var sequence = new AtomicLong();
            var telemetry = new RuntimeTelemetry() {
                public boolean recordsSqlDiagnostics() { return true; }
                public Scope start(Operation operation) {
                    var correlation = new SqlExecutionCorrelation("00000000000000000000000000000236",
                            String.format("%016x", sequence.incrementAndGet()));
                    return new Scope() {
                        public SqlExecutionCorrelation correlation() { return correlation; }
                        public void success(Map<String,Object> attributes) {}
                        public void failure(Throwable error) {}
                    };
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default",service).dataService("sqlite",service)
                    .idGenerationService(new InternalIdGenerationService() {
                        public Long generateId(UserContext caller,Entity entity) { return ids.incrementAndGet(); }
                        public void ensureFloor(String type,long floor) { ids.accumulateAndGet(floor,Math::max); }
                    }).queryExecutionLogging(logging).mutationExecutionLogging(false).telemetry(telemetry)
                    .logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext caller,ExecutionMetadata fact) { logs.add(fact); }
                        public void writeAuditEvent(UserContext caller,RawAuditEvent fact) { audit.add(fact); }
                    }).build().install(GeneratedRuntimeModule.module());
            context = new Context(runtime); context.ensureSchema(); context.ensureSchema();
        }
        TypePlatform root(String name) {
            var root = Q.typePlatforms().comment("prepare an aggregate bucket").purpose(PURPOSE).newEntity(context);
            root.updateName(name); root.auditAs("Seed an aggregate boundary bucket").save(context); return root;
        }
        void contribution(TypePlatform root,Integer value,int number) {
            var row = Q.scalarTypeProbes().comment("prepare an aggregate contribution").purpose(PURPOSE).newEntity(context);
            row.updateTypePlatform(root); row.updateRequiredString("EA236 contribution " + number);
            row.updateRequiredBoolean(true); row.updateRequiredInteger(1); row.updateOptionalInteger(value);
            row.updateRequiredLong(9007199254740993L); row.updateRequiredDecimal(new BigDecimal("7.50"));
            row.updateRequiredFloat(1.25f); row.updateRequiredDouble(2.5d);
            row.updateRequiredDate(LocalDate.of(2026,10,20)); row.updateRequiredTime(LocalTime.of(9,30,15));
            row.updateRequiredDatetime(LocalDateTime.of(2026,10,20,9,30));
            row.auditAs("Seed a generated aggregate contribution").save(context);
        }
        void reset() { driverSQL.clear(); logs.clear(); context.facts.clear(); audit.clear(); }
        List<Map<String,Object>> facts(boolean logging,String outcome) {
            assertEquals(driverSQL.size(),context.facts.size()); assertEquals(logging?driverSQL.size():0,logs.size());
            var rows = new ArrayList<Map<String,Object>>();
            for (int i=0;i<driverSQL.size();i++) {
                var fact=context.facts.get(i); assertEquals(driverSQL.get(i),fact.getParameterizedQuery());
                assertEquals(PURPOSE,fact.getPurpose()); assertNotNull(fact.getComment()); assertFalse(fact.getComment().isBlank());
                assertEquals(outcome,fact.getExecutionOutcome()); var trace=fact.getTracePath(); assertTrue(trace.size()>=4);
                assertEquals(TraceKind.PROVIDER,trace.get(trace.size()-2).getKind());
                assertEquals("sqlite",trace.get(trace.size()-2).getName());
                assertEquals(TraceKind.SQL,trace.get(trace.size()-1).getKind());
                assertEquals("select",trace.get(trace.size()-1).getName());
                assertFalse(fact.getRequestTraceChain().isEmpty());
                rows.add(Map.of("sql",driverSQL.get(i),"parameterizedSQL",fact.getParameterizedQuery(),"outcome",outcome,"comment",fact.getComment(),
                        "purpose",fact.getPurpose(),"tracePath",trace,"traceChain",fact.getRequestTraceChain()));
            }
            assertTrue(context.getTraceChain().isEmpty()); assertTrue(audit.isEmpty()); return rows;
        }
    }
    static Object projection(TypePlatform row,String alias) {
        assertTrue("selected alias must remain present: "+alias,row.hasDynamicProperty(alias));
        return row.getDynamicProperty(alias);
    }
    static void number(Object expected,Object actual) {
        if(expected==null) assertNull(actual); else {
            assertTrue(actual instanceof Number); assertEquals(new BigDecimal(expected.toString()),new BigDecimal(actual.toString()));
        }
    }
    @Test public void generatedEmptyAggregatesAndRealSQLiteFailure() throws Exception {
        var expected=Path.of(System.getProperty("teaql.expected.runtime.root")).toRealPath();
        for(var type:List.of(UserContext.class,TeaQLRuntime.class,SqliteDataServiceExecutor.class))
            assertTrue(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().startsWith(expected));
        int round=Integer.parseInt(System.getenv("TEAQL_EMPTY_ROUND"));
        var vectors=JSON.readTree(Files.readAllBytes(Path.of(System.getenv("TEAQL_EMPTY_VECTORS"))));
        String url="jdbc:sqlite:"+Path.of(System.getenv("TEAQL_EMPTY_DB")).toAbsolutePath();
        var seed=new Fixture(url,round,false); var scalar=new ArrayList<TypePlatform>(); var bucket=new ArrayList<TypePlatform>();
        int contribution=0;
        for(var vector:vectors.get("scalarCases")) {
            var root=seed.root("EA236 round "+round+" scalar "+vector.get("id").asText()); scalar.add(root);
            for(var value:vector.get("values")) seed.contribution(root,value.isNull()?null:value.intValue(),++contribution);
        }
        var mixed=vectors.get("bucketCases").get(1);
        for(var parent:mixed.get("parents")) bucket.add(seed.root("EA236 round "+round+" bucket "+parent.asInt()));
        for(var item:mixed.get("children")) seed.contribution(bucket.get(item.get("parent").asInt()-1),
                item.get("value").isNull()?null:item.get("value").intValue(),++contribution);
        assertEquals(12,contribution); var observations=new ArrayList<Map<String,Object>>();
        for(boolean logging:List.of(true,false)) {
            var f=new Fixture(url,round,logging);
            for(int index=0;index<scalar.size();index++) {
                var vector=vectors.get("scalarCases").get(index); String id=vector.get("id").asText(); f.reset();
                var result=Q.scalarTypeProbesWithMinimalFields().filterByTypePlatform(scalar.get(index).getId())
                        .sumOptionalIntegerAs("total").countAs("rows").limit(100).comment("inspect "+id)
                        .purpose(PURPOSE).aggregation(f.context).toList();
                assertEquals(1,result.size()); assertTrue(result.get(0).containsKey("total"));
                Object sum=vector.get("sum").isNull()?null:vector.get("sum").intValue(); number(sum,result.get(0).get("total"));
                number(vector.get("countStar").intValue(),result.get(0).get("rows"));
                var known=Q.scalarTypeProbesWithMinimalFields().filterByTypePlatform(scalar.get(index).getId())
                        .withOptionalIntegerIsKnown().countAs("values").limit(100).comment("inspect "+id+" nonnull count")
                        .purpose(PURPOSE).aggregation(f.context).toList();
                number(vector.get("countValue").intValue(),known.get(0).get("values"));
                var values=new LinkedHashMap<String,Object>(result.get(0)); values.put("countValue",known.get(0).get("values"));
                observations.add(Map.of("case",id,"logging",logging,"values",values,"sql",f.facts(logging,"success")));
            }
            f.reset(); var empty=Q.typePlatformsWithMinimalFields().withIdIs(bucket.get(3).getId()).selectName().limit(1)
                    .countScalarTypeProbesWith("rows",Q.scalarTypeProbesWithMinimalFields().limit(100).comment("count aggregate contributions"))
                    .sumOptionalIntegerOfScalarTypeProbesAs("total",Q.scalarTypeProbesWithMinimalFields().limit(100).comment("sum aggregate contributions"))
                    .comment("inspect EA-05 empty related SUM").purpose(PURPOSE).executeForOne(f.context);
            assertEquals(bucket.get(3).getId(),E.typePlatform(empty).getId().eval());
            number(0,projection(empty,"rows")); assertNull(projection(empty,"total"));
            var emptyValues=new LinkedHashMap<String,Object>(); emptyValues.put("count",projection(empty,"rows"));
            emptyValues.put("sum",projection(empty,"total"));
            var mapper=new ObjectMapper().findAndRegisterModules().registerModule(new io.teaql.jackson.TeaQLModule(f.context));
            var entityJSON=mapper.valueToTree(empty);
            assertTrue(entityJSON.has("_total")); assertTrue(entityJSON.get("_total").isNull());
            assertEquals(0,entityJSON.get("_rows").asInt());

            observations.add(Map.of("case","EA-05","logging",logging,"values",emptyValues,"entityJSON",entityJSON,"sql",f.facts(logging,"success")));
            Long[] ids=bucket.stream().map(TypePlatform::getId).toArray(Long[]::new);
            for(boolean includeAll:List.of(true,false)) {
                f.reset(); var targets=Q.typePlatformsWithMinimalFields().withIdIn(ids).selectName().orderByIdAscending().limit(100)
                        .countScalarTypeProbesWith("rows",Q.scalarTypeProbesWithMinimalFields().limit(100).comment("count facet contributions"))
                        .sumOptionalIntegerOfScalarTypeProbesAs("total",Q.scalarTypeProbesWithMinimalFields().limit(100).comment("sum facet contributions"));
                var list=Q.scalarTypeProbesWithMinimalFields().withTypePlatformMatching(Q.typePlatformsWithMinimalFields().withIdIn(ids))
                        .orderByIdAscending().offset(1,1).facetByTypePlatformAs("buckets",targets,includeAll)
                        .comment("inspect EA-06 complete membership behind one visible row").purpose(PURPOSE).executeForList(f.context);
                assertEquals(1,list.size()); var facets=list.getFacet("buckets"); assertNotNull(facets);
                assertEquals(includeAll?4:3,facets.size()); var values=new ArrayList<Map<String,Object>>();
                for(var raw:facets) {
                    var row=(TypePlatform)raw; int parent=Arrays.asList(ids).indexOf(E.typePlatform(row).getId().eval())+1;
                    var wanted=mixed.get("expected").get(parent-1); number(wanted.get("count").intValue(),projection(row,"rows"));
                    number(wanted.get("sum").isNull()?null:wanted.get("sum").intValue(),projection(row,"total"));
                    var value=new LinkedHashMap<String,Object>(); value.put("parent",parent); value.put("count",projection(row,"rows"));
                    value.put("sum",projection(row,"total")); values.add(value);
                    var facetJSON=mapper.valueToTree(row); assertTrue(facetJSON.has("_total"));
                    if(projection(row,"total")==null)assertTrue(facetJSON.get("_total").isNull());
                    else assertEquals(0,facetJSON.get("_total").decimalValue().compareTo(new BigDecimal(projection(row,"total").toString())));

                }
                observations.add(Map.of("case","EA-06","logging",logging,"includeAll",includeAll,"visibleRows",list.size(),
                        "values",values,"sql",f.facts(logging,"success")));
            }
            f.reset(); var fallback=Q.scalarTypeProbesWithMinimalFields().withIdIs(0L).sumOptionalIntegerAs("total").limit(1)
                    .comment("inspect EA-07 explicit fallback").purpose(PURPOSE).aggregation(f.context);
            assertNull(fallback.toList().get(0).get("total")); number(0,fallback.toNumber(0));
            assertNull(fallback.toList().get(0).get("total"));
            var fallbackValues=new LinkedHashMap<String,Object>(); fallbackValues.put("raw",fallback.toList().get(0).get("total"));
            fallbackValues.put("fallback",fallback.toNumber(0)); fallbackValues.put("rawRemains",fallback.toList().get(0).get("total"));
            observations.add(Map.of("case","EA-07","logging",logging,"values",fallbackValues,"sql",f.facts(logging,"success")));
            f.reset(); var grouped=Q.scalarTypeProbesWithMinimalFields().withIdIs(0L).sumOptionalIntegerAs("total")
                    .groupByTypePlatform().limit(100).comment("inspect EA-08 no synthetic group").purpose(PURPOSE).aggregation(f.context).toList();
            assertTrue(grouped.isEmpty()); observations.add(Map.of("case","EA-08","logging",logging,"groups",grouped,"sql",f.facts(logging,"success")));
            f.reset();
            try(var connection=DriverManager.getConnection(url);var statement=connection.createStatement()) {
                statement.execute("ALTER TABLE scalar_type_probe_data RENAME TO ea236_fault_scalar_data");
                try {
                    var error=assertThrows(RuntimeException.class,()->Q.scalarTypeProbesWithMinimalFields().withIdIs(0L)
                            .sumOptionalIntegerAs("total").limit(1).comment("inspect actual SQLite aggregate failure")
                            .purpose(PURPOSE).aggregation(f.context));
                    Throwable cause=error; while(cause.getCause()!=null) cause=cause.getCause();
                    assertTrue(cause instanceof SQLException); assertEquals(1,((SQLException)cause).getErrorCode());
                    assertTrue(cause.getMessage().contains("no such table: scalar_type_probe_data"));
                } finally { statement.execute("ALTER TABLE ea236_fault_scalar_data RENAME TO scalar_type_probe_data"); }
            }
            observations.add(Map.of("case","REAL-SQLITE-FAILURE","logging",logging,"sql",f.facts(logging,"failure")));
            f.reset(); var recovered=Q.typePlatformsWithMinimalFields().withIdIs(bucket.get(3).getId()).selectName().limit(1)
                    .comment("load independent detail after SQLite failure").purpose(PURPOSE).executeForOne(f.context);
            assertEquals(bucket.get(3).getId(),E.typePlatform(recovered).getId().eval());
            assertEquals(bucket.get(3).getName(),E.typePlatform(recovered).getName().eval());
            observations.add(Map.of("case","QUERY-RECOVERY","logging",logging,"sql",f.facts(logging,"success")));
        }
        var report=new LinkedHashMap<String,Object>(); report.put("schema","teaql.java-generated-empty-aggregate/v1");
        report.put("passed",true); report.put("round",round); report.put("rows",observations);
        report.put("scalarIds",scalar.stream().map(TypePlatform::getId).toList());
        report.put("bucketIds",bucket.stream().map(TypePlatform::getId).toList());
        assertEquals(22,observations.size()); System.out.println("JAVA_GENERATED_EMPTY_AGGREGATE="+JSON.writeValueAsString(report));
    }
}
