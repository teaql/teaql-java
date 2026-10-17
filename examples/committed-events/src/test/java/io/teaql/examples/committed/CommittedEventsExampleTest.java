package io.teaql.examples.committed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.duck.DuckDbCommittedInbox;
import io.teaql.core.duck.DuckDbModelCheckedCommittedTarget;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteCommittedEventStore;
import io.teaql.core.sqlite.SqliteCommittedEventStore.InboxOutcome;
import io.teaql.core.sqlite.SqliteCommittedMutationRecorder;
import io.teaql.core.sqlite.SqliteModelCheckedCommittedTarget;
import io.teaql.core.sqlite.SqliteCommittedOutboxPublisher;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CommittedEventsExampleTest {
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
        @Override public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException("not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
    }

    public static final class Order extends BaseEntity {
        private final Map<String, Object> fields = new HashMap<>();

        @Override public String typeName() { return "Order"; }
        @Override public Object __internalGet(String field) {
            return field.equals("name") || field.equals("memo") ? fields.get(field) : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name") || field.equals("memo")) fields.put(field, value);
            else super.__internalSet(field, value);
        }
    }

    @Test public void realSaveFreezesDatabaseResultsBeforeCommit() throws Exception {
        Path path = Files.createTempFile("teaql-committed-example-", ".sqlite");
        path.toFile().deleteOnExit();
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + path);
        JdbcSqlExecutor sql = new JdbcSqlExecutor(source);
        SqliteCommittedMutationRecorder recorder = new SqliteCommittedMutationRecorder(
                sql, "example-java-source", "sha256:" + "0".repeat(64));

        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType("Order");
        descriptor.setTargetType(Order.class);
        descriptor.setEntitySupplier(Order::new);
        descriptor.setDataService("sqlite");
        for (String field : new String[] {"id", "version", "name", "memo"}) {
            boolean text = field.equals("name") || field.equals("memo");
            GenericSQLProperty property = (GenericSQLProperty) descriptor.addSimpleProperty(
                    field, text ? String.class : Long.class);
            property.setColumnType(text ? "VARCHAR(255)" : "BIGINT");
        }
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        metadata.register(descriptor);
        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(metadata)
                .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", sql, source))
                .idGenerationService((caller, entity) -> 42L)
                .committedMutationRecorder(recorder)
                .executionLogging(false).build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        context.ensureSchema();
        sql.execute("CREATE TRIGGER order_memo AFTER INSERT ON order_data "
                + "BEGIN UPDATE order_data SET memo = 'set by database' WHERE id = NEW.id; END");

        Order order = new Order();
        order.__internalInitializeNewEntityId(42L);
        order.updateProperty("name", "draft");
        order.auditAs("create example order").save(context);
        var first = recorder.eventStore().outboxAfter("example-java-source", 0, 10);
        assertEquals(1, first.size());
        assertEquals(1, first.get(0).sourceSequence());
        JsonNode created = new ObjectMapper().readTree(first.get(0).payloadJson());
        assertEquals("set by database", created.path("mutations").get(0).path("fields").path("memo").asText());
        assertEquals("create example order", created.path("audit").path("comment").asText());

        order.updateProperty("name", "submitted");
        order.auditAs("submit example order").save(context);
        JdbcSqlExecutor reopened = new JdbcSqlExecutor(source);
        var durable = new SqliteCommittedEventStore(reopened)
                .outboxAfter("example-java-source", 0, 10);
        assertEquals(2, durable.size());
        JsonNode submitted = new ObjectMapper().readTree(durable.get(1).payloadJson());
        assertEquals("1", submitted.path("mutations").get(0).path("base_version").asText());
        assertEquals("2", submitted.path("mutations").get(0).path("committed_version").asText());

        Path replicaPath = Files.createTempFile("teaql-committed-target-example-", ".sqlite");
        replicaPath.toFile().deleteOnExit();
        SQLiteDataSource replicaSource = new SQLiteDataSource();
        replicaSource.setUrl("jdbc:sqlite:" + replicaPath);
        JdbcSqlExecutor replicaSql = new JdbcSqlExecutor(replicaSource);
        TeaQLRuntime replicaRuntime = TeaQLRuntime.builder().metadata(metadata)
                .dataService("sqlite", new SqliteDataServiceExecutor("sqlite", replicaSql, replicaSource))
                .executionLogging(false).build();
        DefaultUserContext replicaContext = new DefaultUserContext(replicaRuntime);
        replicaContext.ensureSchema();
        SqliteModelCheckedCommittedTarget target = new SqliteModelCheckedCommittedTarget(
                replicaSql, "example-java-source", "sha256:" + "0".repeat(64));
        target.ensureSchema();
        var published = new SqliteCommittedOutboxPublisher(reopened, "example-java-source",
                "example-analytics", "sha256:" + "0".repeat(64))
                .publishBatch(10, record -> target.apply(replicaContext, record));
        assertEquals(2, published.acknowledged());
        assertEquals(0, published.status().backlog());
        var duplicate = target.apply(replicaContext, durable.get(0));
        assertTrue(duplicate.duplicate());
        assertEquals(1, duplicate.checkpoint());
        var replicated = replicaSql.queryForList("SELECT version, name, memo FROM order_data WHERE id = ?",
                new Object[] {42L}).get(0);
        assertEquals(2L, ((Number) replicated.get("version")).longValue());
        assertEquals("submitted", replicated.get("name"));
        assertEquals("set by database", replicated.get("memo"));
        assertEquals(2, replicaSql.queryForList("SELECT * FROM teaql_event_applied_payload",
                new Object[0]).size());
        replicaRuntime.close();

        Path duckDirectory = Files.createTempDirectory("teaql-committed-duckdb-example-");
        duckDirectory.toFile().deleteOnExit();
        Path duckPath = duckDirectory.resolve("target.db");
        duckPath.toFile().deleteOnExit();
        JdbcSqlExecutor duckSql = new JdbcSqlExecutor(
                new DriverManagerDataSource("jdbc:duckdb:" + duckPath));
        duckSql.execute("CREATE TABLE order_data (id BIGINT PRIMARY KEY, version BIGINT NOT NULL, "
                + "name VARCHAR, memo VARCHAR)");
        DuckDbModelCheckedCommittedTarget duckTarget =
                new DuckDbModelCheckedCommittedTarget(duckSql, "example-java-source",
                        "sha256:" + "0".repeat(64));
        duckTarget.ensureInboxSchema();
        var duckPublished = new SqliteCommittedOutboxPublisher(reopened, "example-java-source",
                "example-duckdb-analytics", "sha256:" + "0".repeat(64))
                .publishBatch(10, record -> {
                    var outcome = duckTarget.apply(context, new DuckDbCommittedInbox.Record(
                            record.sourceRuntimeId(), record.sourceSequence(), record.changeSetId(),
                            record.modelHash(), record.payloadSha256(), record.payloadJson()));
                    return new InboxOutcome(outcome.duplicate(), outcome.checkpoint());
                });
        assertEquals(2, duckPublished.acknowledged());
        assertEquals(0, duckPublished.status().backlog());
        var duckOrder = duckSql.queryForList("SELECT version, name, memo FROM order_data WHERE id = ?",
                new Object[] {42L}).get(0);
        assertEquals(2L, ((Number) duckOrder.get("version")).longValue());
        assertEquals("submitted", duckOrder.get("name"));
        assertEquals("set by database", duckOrder.get("memo"));
        assertEquals(2, duckSql.queryForList("SELECT * FROM teaql_event_applied_payload",
                new Object[0]).size());

        sql.execute("DROP TABLE teaql_event_outbox");
        order.updateProperty("name", "should roll back");
        assertThrows(RuntimeException.class,
                () -> order.auditAs("reject when outbox unavailable").save(context));
        assertEquals("submitted", sql.queryForList("SELECT name FROM order_data WHERE id = ?",
                new Object[] {42L}).get(0).get("name"));
        assertEquals(2L, ((Number) sql.queryForList("SELECT version FROM order_data WHERE id = ?",
                new Object[] {42L}).get(0).get("version")).longValue());
        runtime.close();
    }
}
