package com.example.requesttreeorderservice;

import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.UserContext;
import io.teaql.core.duck.DuckDataServiceExecutor;
import io.teaql.core.duck.DuckDbCommittedInbox;
import io.teaql.core.duck.DuckDbModelCheckedCommittedTarget;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteCommittedEventStore.InboxOutcome;
import io.teaql.core.sqlite.SqliteCommittedMutationRecorder;
import io.teaql.core.sqlite.SqliteCommittedOutboxPublisher;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import static org.junit.Assert.*;

/** Application-owned generated Q/Save replay across a committed SQLite to DuckDB boundary. */
public class StandardOrderCommittedTargetTest {
    private static final String SOURCE = "generated-java-order-source";
    private static final String HASH = "sha256:" + "0".repeat(64);

    private record DriverManagerDataSource(String url) implements DataSource {
        @Override public Connection getConnection() throws SQLException { return DriverManager.getConnection(url); }
        @Override public Connection getConnection(String user, String password) throws SQLException {
            return getConnection();
        }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter writer) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override public <T> T unwrap(Class<T> type) throws SQLException {
            throw new SQLException("not a wrapper");
        }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
    }

    private static InternalIdGenerationService ids() {
        var next = new AtomicLong(3000);
        return new InternalIdGenerationService() {
            @Override public Long generateId(UserContext context, io.teaql.core.Entity entity) {
                return next.incrementAndGet();
            }
            @Override public void ensureFloor(String type, long floor) {
                next.accumulateAndGet(floor, Math::max);
            }
        };
    }

    private static TeaQLRuntime sqliteRuntime(SqliteDataServiceExecutor service,
            InternalIdGenerationService idService, SqliteCommittedMutationRecorder recorder) {
        var builder = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", service).dataService("sqlite", service)
                .idGenerationService(idService).executionLogging(false);
        if (recorder != null) builder.committedMutationRecorder(recorder);
        return builder.build().install(GeneratedRuntimeModule.module());
    }

    @Test public void generatedSaveReplicatesToDuckDbAndGeneratedQReadsIt() throws Exception {
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + Files.createTempDirectory("teaql-generated-commit-source-")
                .resolve("source.sqlite"));
        var sourceSql = new JdbcSqlExecutor(source);
        var sourceService = new SqliteDataServiceExecutor("sqlite", sourceSql, source);
        var idService = ids();
        try (var bootstrap = sqliteRuntime(sourceService, idService, null)) {
            new DefaultUserContext(bootstrap).ensureSchema();
        }
        var recorder = new SqliteCommittedMutationRecorder(sourceSql, SOURCE, HASH);
        try (var sourceRuntime = sqliteRuntime(sourceService, idService, recorder)) {
            var sourceContext = new DefaultUserContext(sourceRuntime);
            sourceContext.ensureSchema();
            var platform = Q.platforms().limit(1).comment("load source Platform")
                    .purpose("attach committed Customer to domain root")
                    .executeForOne(sourceContext);
            assertNotNull(platform);
            var customer = Q.customers().comment("prepare committed Customer")
                    .purpose("verify generated audited save").newEntity(sourceContext);
            customer.updateName("Committed target customer");
            customer.updatePlatform(platform);
            customer.auditAs("create committed target customer").save(sourceContext);
            var outbox = recorder.eventStore().outboxAfter(SOURCE, 0, 10);
            assertEquals("bootstrap must not enter this Outbox", 1, outbox.size());

            var targetPath = Files.createTempDirectory("teaql-generated-commit-target-")
                    .resolve("target.db");
            var targetSql = new JdbcSqlExecutor(new DriverManagerDataSource("jdbc:duckdb:" + targetPath));
            var targetService = new DuckDataServiceExecutor("sqlite", targetSql);
            try (var targetRuntime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", targetService).dataService("sqlite", targetService)
                    .idGenerationService(ids()).executionLogging(false).build()
                    .install(GeneratedRuntimeModule.module())) {
                var targetContext = new DefaultUserContext(targetRuntime);
                targetContext.ensureSchema();
                var target = new DuckDbModelCheckedCommittedTarget(targetSql, SOURCE, HASH);
                target.ensureInboxSchema();
                var delivered = new SqliteCommittedOutboxPublisher(sourceSql, SOURCE,
                        "generated-java-duckdb-target", HASH).publishBatch(10, record -> {
                    var outcome = target.apply(targetContext, new DuckDbCommittedInbox.Record(
                            record.sourceRuntimeId(), record.sourceSequence(), record.changeSetId(),
                            record.modelHash(), record.payloadSha256(), record.payloadJson()));
                    return new InboxOutcome(outcome.duplicate(), outcome.checkpoint());
                });
                assertEquals(1, delivered.acknowledged());
                assertEquals(0, delivered.status().backlog());
                var replicated = Q.customers().withIdIs(customer.getId()).limit(1)
                        .comment("load replicated Customer through generated Q")
                        .purpose("prove DuckDB target is queryable by application")
                        .executeForOne(targetContext);
                assertNotNull(replicated);
                assertEquals("Committed target customer", replicated.getName());
                assertEquals(customer.getVersion(), replicated.getVersion());
                replicated.updateName("must not modify the analytical target");
                var denied=assertThrows(RuntimeException.class,()->replicated.auditAs("attempt ordinary target write").save(targetContext));
                assertTrue(denied.toString(),denied.toString().contains("READ_ONLY_TARGET"));
                var unchanged=Q.customers().withIdIs(customer.getId()).limit(1)
                        .comment("reload target after denied ordinary write")
                        .purpose("verify generated mutation did not change replicated facts")
                        .executeForOne(targetContext);
                assertEquals("Committed target customer",unchanged.getName());assertEquals(customer.getVersion(),unchanged.getVersion());
                assertEquals(1,recorder.eventStore().outboxAfter(SOURCE,0,10).size());
                System.out.println("CMR15_GENERATED_READ_ONLY_PASS language=java backend=duckdb generated_audited_save=1 fresh_query=1 source_isolated=1");
            }
        }
        System.out.println("JAVA_COMMITTED_GENERATED_Q_PASS source=sqlite target=duckdb outbox=1");
    }
}
