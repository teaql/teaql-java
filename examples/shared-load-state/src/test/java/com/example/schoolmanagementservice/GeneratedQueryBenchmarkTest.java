package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sqlite.SQLiteDataSource;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** Real generated Q/E; fixture expansion alone uses SQL outside measurements. */
@EnabledIfSystemProperty(named="teaql.generatedBenchmark",matches="true")
class GeneratedQueryBenchmarkTest {
    static final class Context extends DefaultUserContext implements SchoolManagementServiceUserContext {
        Context(TeaQLRuntime runtime){super(runtime);}
    }
    /** Controlled reconstruction of the previous generic JDBC query path. */
    static final class SwitchingSql extends JdbcSqlExecutor {
        private final SQLiteDataSource source;
        private final List<java.util.function.Consumer<Connection>> initializers=new ArrayList<>();
        boolean reference;
        SwitchingSql(SQLiteDataSource source){super(source);this.source=source;}
        @Override public SwitchingSql addConnectionInitializer(java.util.function.Consumer<Connection> initializer) {
            initializers.add(initializer);super.addConnectionInitializer(initializer);return this;
        }
        private Connection openReferenceConnection() throws SQLException {
            Connection connection=source.getConnection();
            try {for(var initializer:initializers)initializer.accept(connection);return connection;}
            catch(RuntimeException failure){try{connection.close();}catch(SQLException suppressed){failure.addSuppressed(suppressed);}throw failure;}
        }
        @Override public List<Map<String,Object>> queryForList(String sql,Object[] params) {
            if(!reference)return super.queryForList(sql,params);
            try(Connection connection=openReferenceConnection();PreparedStatement statement=connection.prepareStatement(sql)) {
                if(params!=null)for(int i=0;i<params.length;i++)statement.setObject(i+1,params[i]);
                try(ResultSet rows=statement.executeQuery()) {
                    var metadata=rows.getMetaData();int width=metadata.getColumnCount();String[] labels=new String[width];
                    for(int i=0;i<width;i++){String label=metadata.getColumnLabel(i+1);labels[i]=label==null?null:label.toLowerCase(Locale.ROOT);}
                    List<Map<String,Object>> result=new ArrayList<>();
                    while(rows.next()) {
                        Map<String,Object> row=new HashMap<>();
                        for(int i=0;i<width;i++)row.put(labels[i],rows.getObject(i+1));
                        result.add(row);
                    }
                    return result;
                }
            } catch(SQLException failure){throw new IllegalStateException(failure);}
        }
    }
    static String quoted(String name){return "\""+name.replace("\"","\"\"")+"\"";}
    static void expandFixture(SQLiteDataSource source) throws Exception {
        try(Connection connection=source.getConnection()) {
            List<String> columns=new ArrayList<>();
            try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery("PRAGMA table_info(school_data)")) {
                while(rows.next())columns.add(rows.getString("name"));
            }
            assertTrue(columns.containsAll(List.of("id","name","version")));
            assertTrue(columns.size()>130,"use the wide generated fixture");
            String target=columns.stream().map(GeneratedQueryBenchmarkTest::quoted).collect(Collectors.joining(","));
            String expressions=columns.stream().map(c->c.equals("id")||c.equals("name")?"?":quoted(c)).collect(Collectors.joining(","));
            String sql="INSERT INTO school_data("+target+") SELECT "+expressions+" FROM school_data WHERE id=1";
            int idParameter=columns.indexOf("id")<columns.indexOf("name")?1:2;
            connection.setAutoCommit(false);
            try(PreparedStatement insert=connection.prepareStatement(sql)) {
                for(long id=2;id<=10000;id++) {
                    insert.setLong(idParameter,id);insert.setString(3-idParameter,"bench-school-"+id);insert.addBatch();
                }
                int[] changed=insert.executeBatch();assertEquals(9999,changed.length);
            }
            connection.commit();
        }
    }
    static SmartList<School> query(UserContext context,String scenario,int count) {
        if(scenario.equals("reverse")) {
            var parent=Q.platformsWithMinimalFields().withIdIs(1L)
                    .selectSchoolListWith(Q.schoolsWithMinimalFields().selectName().orderByIdAscending().limit(count))
                    .limit(1).comment("what: load bounded generated reverse graph")
                    .purpose("why: measure relation hydration and shared state").executeForOne(context);
            assertTrue(parent.isPropertyLoaded("schoolList"));
            return parent.getSchoolList();
        }
        var request=scenario.equals("wide")||scenario.equals("dynamic")
                ?Q.schools().selectSelfFields():Q.schoolsWithMinimalFields().selectName();
        if(scenario.equals("dynamic"))request.selectDynamicFieldsWith(new DynamicFieldSelection().selectString("note"));
        if(scenario.equals("forward")) {
            request.selectPlatformWith(Q.platformsWithMinimalFields().selectName());
            request.selectSchoolTypeWith(Q.schoolTypesWithMinimalFields().selectCode());
        }
        return request.orderByIdAscending().limit(count).comment("what: load generated "+scenario+" Schools")
                .purpose("why: measure typed generated runtime query cost").executeForList(context);
    }
    static void validate(SmartList<School> rows,String scenario,int count) {
        assertEquals(count,rows.size());
        boolean complete=scenario.equals("wide")||scenario.equals("dynamic");
        LoadState first=rows.get(0).__internalLoadState();LoadState missing=null;
        for(int index=0;index<count;index++) {
            var row=rows.get(index);long id=index+1;
            assertEquals(id,row.getId());assertEquals(id<=2?2L:1L,row.getVersion());
            assertEquals("bench-school-"+id,E.school(row).getName().eval());
            assertEquals(complete,row.isPropertyLoaded("address"));
            assertFalse(row.__internalHasMutationLedger());
            if(complete) {
                assertEquals("Benchmark address",E.school(row).getAddress().eval());
                assertEquals(17,E.school(row).getStudentCapacity().eval());
                assertEquals(LocalDate.of(1995,9,1),E.school(row).getEstablishedDate().eval());
                for(String field:School.__TEAQL_FIXED_FIELD_INDEXES.keySet())assertTrue(row.isPropertyLoaded(field),field);
            }
            if(scenario.equals("forward")) {
                assertEquals("Campus Learning Platform",E.school(row).getPlatform().getName().eval());
                assertEquals("PRIMARY",E.school(row).getSchoolType().getCode().eval());
                assertEquals(1L,E.school(row).getPlatform().eval().getVersion());
            }
            if(scenario.equals("dynamic")) {
                var field=row.dynamicFields().field("note");
                assertEquals(id==1?DynamicFieldValue.State.VALUE:id==2?DynamicFieldValue.State.NULL:DynamicFieldValue.State.NOT_LOADED,field.state());
                assertEquals(id==1?"benchmark note":null,field.value());
                if(id<=2)assertSame(first,row.__internalLoadState());
                else if(missing==null){missing=row.__internalLoadState();assertNotSame(first,missing);}
                else assertSame(missing,row.__internalLoadState());
            } else assertSame(first,row.__internalLoadState());
        }
        // A sparse view can privately acquire another available field. The held
        // sibling and shared snapshot must not be widened by the assignment.
        if(!complete&&count>1) {
            rows.get(0).updateAddress("private divergent value");
            assertTrue(rows.get(0).isPropertyLoaded("address"));
            assertFalse(rows.get(1).isPropertyLoaded("address"));
            assertSame(first,rows.get(1).__internalLoadState());
            assertNotSame(first,rows.get(0).__internalLoadState());
        }
    }
    @Test void generatedWideDynamicAndRelationQueries() throws Exception {
        String mode=System.getenv().getOrDefault("TEAQL_GENERATED_BENCHMARK_LOG_MODE","off");
        assertTrue(Set.of("off","default").contains(mode));
        int samples=Integer.getInteger("teaql.generatedSamples",31);assertTrue(samples>=3&&samples<=31);
        var source=new SQLiteDataSource();source.setUrl("jdbc:sqlite:"+Files.createTempDirectory("teaql-generated-bench-").resolve("school.sqlite"));
        var sql=new SwitchingSql(source);
        var ids=new IdSpaceIdGenerator(new GeneratedSchoolLoadStateTest.DatabaseBridge(sql));ids.ensureIdSpaceTable();
        var service=new SqliteDataServiceExecutor("sqlite",sql,source);
        var context=new Context(TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default",service).dataService("sqlite",service).idGenerationService(ids)
                .queryExecutionLogging(mode.equals("default")).build().install(GeneratedRuntimeModule.module()));
        context.ensureSchema();
        var platform=Q.platforms().withIdIs(1L).limit(1).comment("what: reuse the generated root")
                .purpose("why: construct a compliant benchmark prototype").executeForOne(context);
        var prototype=Q.schools().comment("what: allocate a benchmark prototype")
                .purpose("why: validate the generated mutation contract before SQL fixture expansion").newEntity(context);
        prototype.updatePlatform(platform);prototype.updateSchoolTypeToPrimary();
        prototype.updateName("bench-school-1");prototype.updateAddress("Benchmark address");
        prototype.updateEstablishedDate(LocalDate.of(1995,9,1));prototype.updateStudentCapacity(17);prototype.updateActive(false);
        prototype.updateCreateTime(LocalDateTime.of(2023,11,14,22,13));prototype.updateUpdateTime(LocalDateTime.of(2023,11,14,22,13));
        prototype.auditAs("Create the single generated benchmark prototype").save(context);assertEquals(1L,prototype.getId());
        expandFixture(source);
        var provider=new JdbcDynamicFieldsProvider(sql);provider.ensureSchema();
        DynamicFieldContext definitionContext=new DynamicFieldContext() {
            public String scopeType(){return "GLOBAL";}public String scopeId(){return "default";}
            public String userId(){return "generated-benchmark";}public String purpose(){return "prepare controlled extensions";}
            public String comment(){return "what: register benchmark note";}public boolean strictIntent(){return true;}
            public long nextId(String type){return ids.nextId(type);}
        };
        var definition=new DynamicFieldDef();definition.setScope(DynamicFieldScope.global());
        definition.setOwnerType("School");definition.setCode("note");definition.setName("note");definition.setDataType(DynamicDataType.STRING);
        provider.registerFieldDef(definitionContext,definition);
        context.putAttribute(DynamicFieldsFacade.class.getName(),new DefaultDynamicFieldsFacade(provider));
        for(long id:new long[]{1,2}) {
            var request=Q.schools().withIdIs(id).selectSelfFields();request.selectDynamicFieldsWith(new DynamicFieldSelection().selectString("note"));
            var row=request.limit(1).comment("what: load trusted extension input").purpose("why: seed Value and NULL through audited save").executeForOne(context);
            row.updateDynamicField("note",id==1?"benchmark note":null);row.auditAs("Seed generated benchmark extension").save(context);
        }
        System.out.println("GENERATED_LOG_MODE,"+mode);System.out.println("GENERATED_FIXED_FIELDS,"+School.__TEAQL_FIXED_FIELD_INDEXES.size());
        boolean compare=Boolean.getBoolean("teaql.generatedCompare");
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        for(String scenario:List.of("sparse","wide","dynamic","forward","reverse"))for(int count:new int[]{1,100,10000}) {
            Supplier<SmartList<School>> action=()->query(context,scenario,count);
            for(int i=0;i<5;i++) {
                if(compare){sql.reference=true;DriverQueryBenchmarkTest.consumed=action.get();}
                sql.reference=false;DriverQueryBenchmarkTest.consumed=action.get();
            }
            long[] times=new long[samples],bytes=new long[samples];
            long[] referenceTimes=new long[samples],referenceBytes=new long[samples];
            for(int index=0;index<samples;index++) {
                DriverQueryBenchmarkTest.Sample<SmartList<School>> reference=null,result;
                if(compare&&index%2==0){sql.reference=true;reference=DriverQueryBenchmarkTest.measure(bean,action);}
                sql.reference=false;result=DriverQueryBenchmarkTest.measure(bean,action);
                if(compare&&index%2!=0){sql.reference=true;reference=DriverQueryBenchmarkTest.measure(bean,action);}
                sql.reference=false;
                validate(result.value(),scenario,count);
                if(reference!=null) {
                    validate(reference.value(),scenario,count);referenceTimes[index]=reference.nanos();referenceBytes[index]=reference.bytes();
                    System.out.printf("REFERENCE_SAMPLE,%s,%d,%d,%d,%d%n",scenario,count,index,referenceTimes[index],referenceBytes[index]);
                }
                times[index]=result.nanos();bytes[index]=result.bytes();
                System.out.printf("GENERATED_SAMPLE,%s,%d,%d,%d,%d%n",scenario,count,index,times[index],bytes[index]);
            }
            long total=Arrays.stream(times).sum();Arrays.sort(times);Arrays.sort(bytes);
            System.out.printf(Locale.ROOT,"GENERATED_SUMMARY,%s,%d,%d,%d,%d,%.2f,%d%n",scenario,count,samples,times[samples/2],times[(int)Math.ceil(samples*.95)-1],samples*1e9/total,bytes[samples/2]);
            if(compare) {
                long referenceTotal=Arrays.stream(referenceTimes).sum();Arrays.sort(referenceTimes);Arrays.sort(referenceBytes);
                System.out.printf(Locale.ROOT,"REFERENCE_SUMMARY,%s,%d,%d,%d,%d,%.2f,%d%n",scenario,count,samples,referenceTimes[samples/2],referenceTimes[(int)Math.ceil(samples*.95)-1],samples*1e9/referenceTotal,referenceBytes[samples/2]);
            }
        }
        System.out.println("PASS generated Java wide dynamic forward reverse Q/E sharing and private divergence benchmark");
    }
}
