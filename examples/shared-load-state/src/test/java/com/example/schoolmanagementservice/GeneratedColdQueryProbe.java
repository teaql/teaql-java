package com.example.schoolmanagementservice;

import com.example.schoolmanagementservice.school.School;
import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.sqlite.SQLiteDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Independent JVM, pre-created fixture, first generated Q only; no warmup. */
public final class GeneratedColdQueryProbe {
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2 || !Set.of("prepare", "query").contains(arguments[0]))
            throw new IllegalArgumentException("Usage: GeneratedColdQueryProbe prepare|query <database>");
        boolean prepare=arguments[0].equals("prepare");
        Path database=Path.of(arguments[1]);
        assertEquals(!prepare,Files.exists(database));
        String mode=System.getenv().getOrDefault("TEAQL_COLD_LOG_MODE","off");
        assertTrue(Set.of("off","default").contains(mode));
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();
        var source=new SQLiteDataSource();source.setUrl("jdbc:sqlite:"+database);
        var sql=new JdbcSqlExecutor(source);
        var ids=new IdSpaceIdGenerator(new GeneratedSchoolLoadStateTest.DatabaseBridge(sql));
        var service=new SqliteDataServiceExecutor("sqlite",sql,source);
        var context=new GeneratedQueryBenchmarkTest.Context(TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory()).dataService("default",service).dataService("sqlite",service)
                .idGenerationService(ids).queryExecutionLogging(mode.equals("default")).build()
                .install(GeneratedRuntimeModule.module()));
        long initNs=System.nanoTime()-start,initBytes=bean.getThreadAllocatedBytes(thread)-before;
        if (prepare) {
            ids.ensureIdSpaceTable();context.ensureSchema();
            var platform=Q.platforms().withIdIs(1L).limit(1)
                    .comment("what: reuse cold fixture root").purpose("why: seed through generated audited mutation").executeForOne(context);
            for(String name:new String[]{"cold-school-1","cold-school-2"}) {
                var school=Q.schools().comment("what: allocate a cold fixture School")
                        .purpose("why: prepare outside the cold measurement process").newEntity(context);
                school.updatePlatform(platform);school.updateSchoolTypeToPrimary();
                school.updateName(name);school.updateAddress("Cold fixture address");
                school.updateEstablishedDate(LocalDate.of(1995,9,1));school.updateStudentCapacity(17);school.updateActive(false);
                school.updateCreateTime(LocalDateTime.of(2023,11,14,22,13));school.updateUpdateTime(LocalDateTime.of(2023,11,14,22,13));
                school.auditAs("prepare the generated cold-query fixture").save(context);
            }
            System.out.println("PASS prepared generated Java cold-query fixture");return;
        }
        before=bean.getThreadAllocatedBytes(thread);start=System.nanoTime();
        var rows=Q.schoolsWithMinimalFields().selectName().orderByIdAscending().limit(2)
                .comment("what: load the first bounded generated cold-query result")
                .purpose("why: separate process-cold startup from warmed query costs").executeForList(context);
        long queryNs=System.nanoTime()-start,queryBytes=bean.getThreadAllocatedBytes(thread)-before;
        assertEquals(2,rows.size());var state=rows.get(0).__internalLoadState();
        assertSame(state,rows.get(1).__internalLoadState());
        for(int index=0;index<rows.size();index++) {
            var row=rows.get(index);assertEquals((long)index+1,row.getId());assertEquals(1L,row.getVersion());
            assertEquals("cold-school-"+(index+1),E.school(row).getName().eval());
            assertFalse(row.isPropertyLoaded("address"));assertEquals(EntityStatus.PERSISTED,row.get$status());
        }
        // Check provenance after the cold query, never initialize these classes as a warmup.
        String root=Path.of(System.getenv("TEAQL_LOCAL_RUNTIME_ROOT")).toRealPath().toString();
        for(Class<?> type:new Class<?>[]{LoadState.class,JdbcSqlExecutor.class,TeaQLRuntime.class,School.class}) {
            Path location=Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            assertTrue(location.startsWith(root),"not local runtime/generated code: "+location);
            System.out.println("COLD_CLASS "+type.getName()+" "+location);
        }
        System.out.printf("JAVA_COLD_QUERY,%s,%d,%d,%d,%d,%d%n",mode,ProcessHandle.current().pid(),initNs,queryNs,initBytes,queryBytes);
        System.out.println("PASS generated Java process-cold Q/E and shared snapshot");
    }
}
