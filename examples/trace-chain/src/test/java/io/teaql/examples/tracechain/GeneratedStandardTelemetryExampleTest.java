package io.teaql.examples.tracechain;

import com.teaql.tracechainservice.E;
import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.GeneratedRuntimeModule;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Generated Q/E + automatic bootstrap, with no application SDK setup or tracing code. */
public class GeneratedStandardTelemetryExampleTest {
    @Test public void startupAndRepeatedBootstrapWithOrdinarySqlLoggingDisabled() throws Exception {
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + Files.createTempDirectory("teaql-java-standard-telemetry-").resolve("example.db"));
        var driver = new JdbcSqlExecutor(source);
        var provider = new SqliteDataServiceExecutor("sqlite", driver, source);
        var ordinaryLogs = new AtomicInteger();
        var audits = new ArrayList<SafeAuditEvent>();
        try (var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", provider).dataService("sqlite", provider)
                .idGenerationService(new IdSpaceIdGenerator(new IdDatabase(driver)))
                .queryExecutionLogging(false).mutationExecutionLogging(false)
                .logSink((caller, metadata) -> ordinaryLogs.incrementAndGet()).build()
                .install(GeneratedRuntimeModule.module())) {
            var context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits.add(event));
            context.ensureSchema();
            assertEquals("one generated root seed must commit", 1, audits.size());
            context.ensureSchema();
            assertEquals("repeated bootstrap must not write or produce another committed audit", 1, audits.size());
            for (int iteration = 0; iteration < 2; iteration++) {
                var rows = Q.platformsWithMinimalFields().withIdIs(1L).orderByIdAscending().limit(1)
                        .comment("load seeded platform").purpose("verify automatic standard telemetry")
                        .executeForList(context);
                assertEquals(1, rows.size());
                assertEquals(Long.valueOf(1), E.platform(rows.get(0)).getId().eval());
            }
            assertEquals("ordinary logging remains separately disabled", 0, ordinaryLogs.get());
            runtime.getTelemetry().flush();
        }
        System.out.println("JAVA_STANDARD_TELEMETRY_EXAMPLE PASSED ensures=2 committedSeeds=1 queries=2 rows=1 ordinarySqlLogs=0");
    }
}
