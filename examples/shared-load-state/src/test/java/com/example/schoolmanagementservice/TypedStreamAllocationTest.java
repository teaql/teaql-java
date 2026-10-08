package com.example.schoolmanagementservice;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Real JDBC cursor allocations; no prepared Map inputs hide provider costs. */
class TypedStreamAllocationTest {
    static final class Adapter extends JdbcSqlExecutor {
        boolean compiled;
        int typedCalls, mapCalls;
        Adapter(SQLiteDataSource source) { super(source); }
        @Override public boolean supportsCompiledStreamMapping() { return compiled; }
        @Override public <T extends Entity> Stream<T> queryForStream(String sql,Object[] args,CompiledRowMapper<T> mapper) {
            typedCalls++; return super.queryForStream(sql,args,mapper);
        }
        @Override public Stream<Map<String,Object>> queryForStream(String sql,Object[] args) {
            mapCalls++; return super.queryForStream(sql,args);
        }
    }

    @Test void realSQLiteTypedStreamRemovesMapsWithoutChangingLoadedState() throws Exception {
        assertNotNull(HydrationAllocationTest.Probe.LAYOUT);
        var metadata=new SimpleEntityMetaFactory(); var descriptor=new SQLEntityDescriptor();
        descriptor.setType("Probe"); descriptor.setTargetType(HydrationAllocationTest.Probe.class);
        descriptor.setEntitySupplier(HydrationAllocationTest.Probe::new);
        for(String field:List.of("id","version","name","note")) {
            var property=(GenericSQLProperty)descriptor.addSimpleProperty(field,
                    field.equals("id")||field.equals("version")?Long.class:String.class);
            property.setColumnType(field.equals("id")||field.equals("version")?"BIGINT":"VARCHAR(255)");
        }
        metadata.register(descriptor);
        var source=new SQLiteDataSource();
        var path=Files.createTempDirectory("teaql-java-typed-stream-").resolve("probe.sqlite");
        source.setUrl("jdbc:sqlite:"+path); System.out.println("STREAM_DATABASE "+path);
        var adapter=new Adapter(source);
        var service=new SqliteDataServiceExecutor("sqlite",adapter,source);
        var context=new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("default",service).queryExecutionLogging(false).build());
        context.ensureSchema();
        // Synthetic allocation input only; not a business/bootstrap mutation
        // alternative. Fixture construction is excluded from measurements.
        try(var connection=source.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("INSERT INTO probe_data(id,version,name,note) VALUES(?,?,?,?)")) {
                for(int id=1;id<=10_000;id++) {
                    statement.setLong(1,id);statement.setLong(2,1);statement.setString(3,"row-"+id);
                    statement.setString(4,id%2==0?null:"nullable note");statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        }
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported()); bean.setThreadAllocatedMemoryEnabled(true);
        for(boolean full:List.of(false,true)) {
            var warm=new HydrationAllocationTest.Request(metadata,full,1);
            var executable=warm.purpose("why: warm both cursor hydration paths");
            for(int i=0;i<300;i++) for(boolean compiled:List.of(false,true)) {
                adapter.compiled=compiled; try(var stream=executable.executeForStream(context)) {HydrationAllocationTest.consumed=stream.toList();}
            }
            for(int count:new int[]{1,100,10_000}) {
                var request=new HydrationAllocationTest.Request(metadata,full,count);request.addOrderByAscending("id");
                var query=request.purpose("why: isolate real provider Map allocation from typed hydration");
                long typedBytes=0,mapBytes=0;
                for(boolean compiled:List.of(false,true)) {
                    adapter.compiled=compiled; int typedBefore=adapter.typedCalls,mapBefore=adapter.mapCalls;
                    var sample=HydrationAllocationTest.measure(bean,()->{try(var stream=query.executeForStream(context)){return stream.toList();}});
                    @SuppressWarnings("unchecked") var rows=(List<HydrationAllocationTest.Probe>)sample.result();
                    assertEquals(count,rows.size());var state=rows.get(0).__internalLoadState();
                    for(int i=0;i<count;i++) {
                        var row=rows.get(i);assertEquals((long)i+1,row.getId());assertEquals(1L,row.getVersion());
                        assertEquals("row-"+(i+1),row.name);assertEquals(full,row.isPropertyLoaded("note"));
                        assertEquals(full && (i+1)%2!=0?"nullable note":null,row.note);
                        assertSame(state,row.__internalLoadState());assertFalse(row.__internalHasMutationLedger());
                        assertTrue(row.getUpdatedProperties().isEmpty());
                    }
                    assertEquals(typedBefore+(compiled?1:0),adapter.typedCalls);assertEquals(mapBefore+(compiled?0:1),adapter.mapCalls);
                    System.out.printf("SQLITE_STREAM,%s,%s,%d,%d%n",compiled?"typed":"map",full?"full":"sparse",count,sample.bytes());
                    if(compiled)typedBytes=sample.bytes();else mapBytes=sample.bytes();
                }
                if(count==10_000)assertTrue(typedBytes<mapBytes,"real JDBC typed cursor must eliminate Map allocation");
            }
        }
        System.out.println("PASS Java real typed stream loaded state and provider allocation controls");
    }
}
