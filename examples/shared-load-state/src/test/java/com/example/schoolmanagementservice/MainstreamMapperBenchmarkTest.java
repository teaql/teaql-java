package com.example.schoolmanagementservice;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sqlite.SQLiteDataSource;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Mainstream mapper track, not matched-SQL or generated-library performance. */
@EnabledIfSystemProperty(named="teaql.ormBenchmark", matches="true")
public class MainstreamMapperBenchmarkTest {
    public record Sparse(Long id, Long version, String name) {}
    public record Full(Long id, Long version, String name, String note) {}
    public interface Mapper {
        @Select("SELECT id,version,name FROM probe_data WHERE version>0 AND name LIKE #{prefix} ORDER BY id ASC LIMIT #{limit}")
        @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="version",javaType=Long.class),@Arg(column="name",javaType=String.class)})
        List<Sparse> sparse(@Param("prefix") String prefix, @Param("limit") int limit);
        @Select("SELECT id,version,name,note FROM probe_data WHERE version>0 AND name LIKE #{prefix} ORDER BY id ASC LIMIT #{limit}")
        @ConstructorArgs({@Arg(column="id",javaType=Long.class),@Arg(column="version",javaType=Long.class),@Arg(column="name",javaType=String.class),@Arg(column="note",javaType=String.class)})
        List<Full> full(@Param("prefix") String prefix, @Param("limit") int limit);
    }
    static List<?> load(SqlSessionFactory factory, boolean full, int limit) {
        // A fresh method-scoped session prevents repeated-query result caching.
        try (SqlSession session=factory.openSession()) {
            var mapper=session.getMapper(Mapper.class);
            return full?mapper.full("row-%",limit):mapper.sparse("row-%",limit);
        }
    }
    @Test void mainstreamMapperAndGovernedQueriesAgree() throws Exception {
        int samples=Integer.getInteger("teaql.ormSamples",31);
        assertTrue(samples>=3 && samples<=31);
        assertNotNull(HydrationAllocationTest.Probe.LAYOUT);
        var metadata=new SimpleEntityMetaFactory();
        var descriptor=new SQLEntityDescriptor();descriptor.setType("Probe");
        descriptor.setTargetType(HydrationAllocationTest.Probe.class);
        descriptor.setEntitySupplier(HydrationAllocationTest.Probe::new);
        for(String field:List.of("id","version","name","note")) {
            var property=(GenericSQLProperty)descriptor.addSimpleProperty(field,
                    field.equals("id")||field.equals("version")?Long.class:String.class);
            property.setColumnType(field.equals("id")||field.equals("version")?"BIGINT":"VARCHAR(255)");
        }
        metadata.register(descriptor);
        var source=new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:"+Files.createTempDirectory("teaql-mapper-").resolve("probe.sqlite"));
        var executor=new JdbcSqlExecutor(source);
        var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("default",new SqliteDataServiceExecutor("sqlite",executor,source))
                .queryExecutionLogging(false).build());
        context.ensureSchema();
        // Deterministic benchmark setup only; never a runtime seeding API.
        try(Connection connection=source.getConnection()) {
            System.out.println("ENGINE_VERSION,sqlite,"+connection.getMetaData().getDatabaseProductVersion());
            System.out.println("DRIVER_VERSION,"+connection.getMetaData().getDriverVersion());
            connection.setAutoCommit(false);
            try(PreparedStatement insert=connection.prepareStatement("INSERT INTO probe_data(id,version,name,note) VALUES(?,?,?,?)")) {
                for(long id=1;id<=10_002;id++) {
                    insert.setLong(1,id);insert.setLong(2,id==10_001?-1:1);
                    insert.setString(3,id==10_002?"not-selected":"row-"+id);
                    insert.setString(4,id%2==0?null:"nullable note");insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
        }
        var configuration=new Configuration(new Environment("bench",new JdbcTransactionFactory(),source));
        configuration.addMapper(Mapper.class);
        var factory=new SqlSessionFactoryBuilder().build(configuration);
        System.out.println("FRAMEWORK,mybatis,"+SqlSession.class.getPackage().getImplementationVersion());
        System.out.println("ORM_LOG_MODE,off");
        System.out.println("ORM_CONNECTION_POLICY,file-backed-method-scoped-both-lanes");
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        for(boolean full:List.of(false,true))for(int count:new int[]{1,100,10_000}) {
            var request=new HydrationAllocationTest.Request(metadata,full,count);
            request.appendSearchCriteria(request.createBasicSearchCriteria("name",Operator.BEGIN_WITH,"row-"));
            request.addOrderByAscending("id");
            var query=request.purpose("why: compare mainstream bounded typed retrieval");
            Supplier<List<?>> mapperAction=()->load(factory,full,count);
            Supplier<SmartList<HydrationAllocationTest.Probe>> teaqlAction=()->query.executeForList(context);
            for(int i=0;i<20;i++){DriverQueryBenchmarkTest.consumed=mapperAction.get();DriverQueryBenchmarkTest.consumed=teaqlAction.get();}
            List<DriverQueryBenchmarkTest.Sample<Void>> mapperSamples=new ArrayList<>(),teaqlSamples=new ArrayList<>();
            for(int sample=0;sample<samples;sample++) {
                DriverQueryBenchmarkTest.Sample<List<?>> mapped;
                DriverQueryBenchmarkTest.Sample<SmartList<HydrationAllocationTest.Probe>> typed;
                if(sample%2==0){mapped=DriverQueryBenchmarkTest.measure(bean,mapperAction);typed=DriverQueryBenchmarkTest.measure(bean,teaqlAction);}
                else{typed=DriverQueryBenchmarkTest.measure(bean,teaqlAction);mapped=DriverQueryBenchmarkTest.measure(bean,mapperAction);}
                assertEquals(count,mapped.value().size());assertEquals(count,typed.value().size());
                var state=typed.value().get(0).__internalLoadState();
                for(int index=0;index<count;index++) {
                    var row=typed.value().get(index);Object other=mapped.value().get(index);
                    long id=index+1;String note=full&&id%2!=0?"nullable note":null;
                    if(other instanceof Full value)assertEquals(new Full(id,1L,"row-"+id,note),value);
                    else assertEquals(new Sparse(id,1L,"row-"+id),other);
                    assertEquals(id,row.getId());assertEquals(1L,row.getVersion());
                    assertEquals("row-"+id,row.name);assertEquals(note,row.note);
                    assertEquals(full,row.isPropertyLoaded("note"));assertSame(state,row.__internalLoadState());
                    assertFalse(row.__internalHasMutationLedger());
                }
                mapperSamples.add(new DriverQueryBenchmarkTest.Sample<>(null,mapped.bytes(),mapped.nanos()));
                teaqlSamples.add(new DriverQueryBenchmarkTest.Sample<>(null,typed.bytes(),typed.nanos()));
            }
            DriverQueryBenchmarkTest.report("mybatis",full,count,mapperSamples);
            DriverQueryBenchmarkTest.report("teaql",full,count,teaqlSamples);
        }
        System.out.println("PASS mainstream MyBatis and TeaQL bounded typed results with independent version and prefix filters");
    }
}
