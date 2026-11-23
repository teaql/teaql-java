package io.teaql.core.duck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.duck.DuckDbCommittedInbox.Failure;
import io.teaql.core.duck.DuckDbCommittedInbox.Record;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DuckDbModelCheckedCommittedTargetTest {
    private static final String SOURCE = "sqlite-order-source";
    private static final String HASH = "sha256:" + "0".repeat(64);
    private static final long ORDER_ID = 9_007_199_254_740_993L;
    private static final long ITEM_ID = ORDER_ID + 1;

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

    private Path directory;
    private Path database;
    private JdbcSqlExecutor sql;
    private DefaultUserContext context;
    private DuckDbModelCheckedCommittedTarget target;

    @Before public void setUp() throws Exception {
        directory = Files.createTempDirectory("teaql-duckdb-model-target-");
        database = directory.resolve("target.db");
        sql = new JdbcSqlExecutor(new DriverManagerDataSource("jdbc:duckdb:" + database));
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        EntityDescriptor order = new EntityDescriptor();
        order.setType("order");
        order.addSimpleProperty("id", Long.class);
        order.addSimpleProperty("version", Long.class);
        order.addSimpleProperty("order_number", String.class);
        order.addSimpleProperty("status", String.class);
        order.addSimpleProperty("planned_date", java.time.LocalDate.class).with("sqlType", "DATE");
        order.addSimpleProperty("occurred_at", java.time.Instant.class)
                .with("sqlType", "TIMESTAMPTZ").with("required", "true");
        order.addSimpleProperty("amount", BigDecimal.class).with("sqlType", "DECIMAL(19,7)");
        order.addSimpleProperty("active", Boolean.class)
                .with("sqlType", "BOOLEAN").with("required", "true");
        order.addSimpleProperty("ratio", Double.class).with("required", "true");
        order.addSimpleProperty("optional_note", String.class);
        order.addSimpleProperty("optional_label", String.class);
        metadata.register(order);
        EntityDescriptor item = new EntityDescriptor();
        item.setType("order_item");
        item.addSimpleProperty("id", Long.class);
        item.addSimpleProperty("version", Long.class);
        item.addObjectProperty(metadata, "order", "order", "items", BaseEntity.class)
                .with("columnName", "order_id");
        item.addSimpleProperty("quantity", Long.class);
        metadata.register(item);
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
        sql.execute("CREATE TABLE order_data (id BIGINT PRIMARY KEY, version BIGINT NOT NULL, "
                + "order_number VARCHAR NOT NULL, status VARCHAR NOT NULL, planned_date DATE, "
                + "occurred_at TIMESTAMPTZ NOT NULL, amount DECIMAL(19,7), active BOOLEAN NOT NULL, "
                + "ratio DOUBLE NOT NULL, optional_note VARCHAR, optional_label VARCHAR)");
        sql.execute("CREATE TABLE order_item_data (id BIGINT PRIMARY KEY, version BIGINT NOT NULL, "
                + "order_id BIGINT NOT NULL REFERENCES order_data(id), quantity BIGINT NOT NULL)");
        target = new DuckDbModelCheckedCommittedTarget(sql, SOURCE, HASH);
        target.ensureInboxSchema();
    }

    @After public void tearDown() throws Exception {
        Files.deleteIfExists(database);
        Files.deleteIfExists(directory);
    }

    @Test public void appliesWholeOrderGraphTwiceAndRetainsExactTypedValuesAcrossRestart() {
        Record first = record(1, "order-graph-0001", created());
        Record second = record(2, "order-graph-0002", submitted());
        assertEquals(new DuckDbCommittedInbox.Outcome(false, 1), target.apply(context, first));
        assertEquals(new DuckDbCommittedInbox.Outcome(true, 1), target.apply(context, first));
        assertEquals(new DuckDbCommittedInbox.Outcome(false, 2), target.apply(context, second));
        assertEquals(new DuckDbCommittedInbox.Outcome(true, 2), target.apply(context, second));
        assertEquals(1, count("order_data"));
        assertEquals(1, count("order_item_data"));
        assertEquals(2, count("teaql_event_inbox"));
        assertEquals(2, count("teaql_event_applied_payload"));
        assertEquals(1, count("teaql_event_checkpoint"));
        Map<String, Object> order = sql.queryForList("SELECT id, version, status, "
                + "CAST(planned_date AS VARCHAR) AS planned_date, epoch_ms(occurred_at) AS occurred_at, "
                + "amount, active, ratio, optional_note, optional_label FROM order_data", new Object[0]).get(0);
        assertEquals(ORDER_ID, ((Number) order.get("id")).longValue());
        assertEquals(2L, ((Number) order.get("version")).longValue());
        assertEquals("SUBMITTED", order.get("status"));
        assertEquals("2026-10-16", order.get("planned_date"));
        assertEquals(-315_619_199_877L, ((Number) order.get("occurred_at")).longValue());
        assertEquals(0, ((BigDecimal) order.get("amount")).compareTo(BigDecimal.ZERO));
        assertEquals(true, order.get("active"));
        assertEquals(0.5, ((Number) order.get("ratio")).doubleValue(), 0.0);
        assertEquals("submitted note", order.get("optional_note"));
        assertEquals(null, order.get("optional_label"));
        Map<String, Object> item = sql.queryForList("SELECT id, version, order_id, quantity "
                + "FROM order_item_data", new Object[0]).get(0);
        assertEquals(ITEM_ID, ((Number) item.get("id")).longValue());
        assertEquals(-2L, ((Number) item.get("version")).longValue());
        assertEquals(ORDER_ID, ((Number) item.get("order_id")).longValue());
        assertEquals(2L, ((Number) item.get("quantity")).longValue());
        assertEquals(first.payloadJson(), sql.queryForList("SELECT payload_json FROM "
                + "teaql_event_applied_payload WHERE source_sequence = 1", new Object[0])
                .get(0).get("payload_json"));
        DuckDbModelCheckedCommittedTarget reopened = new DuckDbModelCheckedCommittedTarget(
                new JdbcSqlExecutor(new DriverManagerDataSource("jdbc:duckdb:" + database)), SOURCE, HASH);
        assertTrue(reopened.apply(context, second).duplicate());
    }

    @Test public void appliesSharedConformanceFixtureWhenConfigured() throws Exception {
        String fixturePath = System.getenv("TEAQL_COMMITTED_EVENT_FIXTURE");
        Assume.assumeTrue("shared fixture path was not supplied",
                fixturePath != null && !fixturePath.isBlank());
        JsonNode fixture = new ObjectMapper().readTree(Files.readString(Path.of(fixturePath)));
        assertEquals(1, fixture.path("fixture_version").asInt());
        assertEquals(2, fixture.path("sets").size());
        for (int index = 0; index < 2; index++) {
            JsonNode set = fixture.path("sets").get(index);
            long sequence = Long.parseLong(set.path("source_sequence").asText());
            Record source = recordBody(sequence, set.path("change_set_id").asText(), set.toString());
            assertEquals(sequence, target.apply(context, source).checkpoint());
            assertTrue(target.apply(context, source).duplicate());
        }
        Map<String, Object> order = sql.queryForList("SELECT id, version, status, "
                + "CAST(planned_date AS VARCHAR) AS planned_date, epoch_ms(occurred_at) AS occurred_at, "
                + "amount, active, ratio, optional_note, optional_label FROM order_data",
                new Object[0]).get(0);
        assertEquals(ORDER_ID, ((Number) order.get("id")).longValue());
        assertEquals(2L, ((Number) order.get("version")).longValue());
        assertEquals("SUBMITTED", order.get("status"));
        assertEquals("2026-10-16", order.get("planned_date"));
        assertEquals(-315_619_199_877L, ((Number) order.get("occurred_at")).longValue());
        assertEquals(0, ((BigDecimal) order.get("amount")).compareTo(BigDecimal.ZERO));
        assertEquals(true, order.get("active"));
        assertEquals(0.5, ((Number) order.get("ratio")).doubleValue(), 0.0);
        assertEquals("submitted note", order.get("optional_note"));
        assertEquals(null, order.get("optional_label"));
        Map<String, Object> item = sql.queryForList("SELECT id, version, order_id, quantity "
                + "FROM order_item_data", new Object[0]).get(0);
        assertEquals(ITEM_ID, ((Number) item.get("id")).longValue());
        assertEquals(-2L, ((Number) item.get("version")).longValue());
        assertEquals(ORDER_ID, ((Number) item.get("order_id")).longValue());
        assertEquals(2L, ((Number) item.get("quantity")).longValue());
        assertEquals(2, count("teaql_event_inbox"));
        assertEquals(2, count("teaql_event_applied_payload"));
        assertEquals(1, count("teaql_event_checkpoint"));
    }

    @Test public void contextEnsureSchemaInstallsCompatibleModelStorage() throws Exception {
        Path installedDatabase = directory.resolve("installed-schema.db");
        try {
            JdbcSqlExecutor installedSql = new JdbcSqlExecutor(
                    new DriverManagerDataSource("jdbc:duckdb:" + installedDatabase));
            EntityMetaFactory metadata = EntityMetaFactory.requireFrom(context);
            metadata.allEntityDescriptors().forEach(entity -> entity.setDataService("duckdb"));
            DefaultUserContext installedContext = new DefaultUserContext(TeaQLRuntime.builder()
                    .metadata(metadata)
                    .dataService("duckdb", new DuckDataServiceExecutor("duckdb", installedSql))
                    .build());
            installedContext.ensureSchema();
            installedContext.ensureSchema();
            DuckDbModelCheckedCommittedTarget installed =
                    new DuckDbModelCheckedCommittedTarget(installedSql, SOURCE, HASH);
            installed.ensureInboxSchema();
            assertEquals(1L, installed.apply(installedContext,
                    record(1, "installed-create", created())).checkpoint());
            assertEquals(2L, installed.apply(installedContext,
                    record(2, "installed-update", submitted())).checkpoint());
            List<Map<String, Object>> columns = installedSql.queryForList(
                    "SELECT column_name, data_type FROM information_schema.columns "
                            + "WHERE table_name = 'order_data'", new Object[0]);
            Map<String, String> types = new java.util.HashMap<>();
            for (Map<String, Object> column : columns) {
                types.put((String) column.get("column_name"), (String) column.get("data_type"));
            }
            assertEquals("DATE", types.get("planned_date"));
            assertEquals("TIMESTAMP WITH TIME ZONE", types.get("occurred_at"));
            assertEquals("DECIMAL(19,7)", types.get("amount"));
            assertEquals("BOOLEAN", types.get("active"));
            assertEquals("SUBMITTED", installedSql.queryForList(
                    "SELECT status FROM order_data", new Object[0]).get(0).get("status"));
            assertEquals(-2L, ((Number) installedSql.queryForList(
                    "SELECT version FROM order_item_data", new Object[0])
                    .get(0).get("version")).longValue());
            List<Map<String, Object>> indexes = installedSql.queryForList(
                    "SELECT index_name FROM duckdb_indexes() "
                            + "WHERE table_name = 'order_item_data'", new Object[0]);
            assertEquals(1, indexes.size());
            String indexName = (String) indexes.get(0).get("index_name");
            assertTrue(indexName.matches("[A-Za-z0-9_]+"));
            installedSql.execute("DROP INDEX \"" + indexName + "\"");
            installedSql.execute("CREATE INDEX \"" + indexName
                    + "\" ON order_item_data(quantity)");
            IllegalStateException mismatch = assertThrows(IllegalStateException.class,
                    installedContext::ensureSchema);
            assertTrue(mismatch.getMessage().contains("canonical relation index"));
        } finally {
            Files.deleteIfExists(installedDatabase);
        }
    }

    @Test public void lateGraphFailureRollsBackDomainAndInbox() {
        String broken = created().replace("\"quantity\":\"2\"", "\"quantity\":\"2\",\"unknown\":1");
        expect("INVALID_ENVELOPE", () -> target.apply(context, record(1, "bad", broken)));
        assertEmptyTarget();
        String missingParent = created().replace("\"order\":{\"entity_type\":\"order\",\"entity_id\":\"9007199254740993\"}",
                "\"order\":{\"entity_type\":\"order\",\"entity_id\":\"9007199254740995\"}");
        assertFalse(missingParent.equals(created()));
        expect("TARGET_FAILURE", () -> target.apply(context, record(1, "bad", missingParent)));
        assertEmptyTarget();
        assertFalse(target.apply(context, record(1, "good", created())).duplicate());
    }

    @Test public void invalidCodecAndPhysicalMappingFailClosed() {
        expectRejectedCreate(created().replace("\"amount\":\"0\"", "\"amount\":\"0.00000001\""));
        expectRejectedCreate(created().replace("\"amount\":\"0\"", "\"amount\":\"1234567890123\""));
        expectRejectedCreate(created().replace("\"occurred_at\":\"-315619199877\"",
                "\"occurred_at\":\"2026-10-16T00:00:00Z\""));
        expectRejectedCreate(created().replace("\"planned_date\":\"2026-10-16\"",
                "\"planned_date\":\"2026-10-16T00:00:00Z\""));
        expectRejectedCreate(created().replace("\"active\":true", "\"active\":\"true\""));
        expectRejectedCreate(created().replace("\"quantity\":\"2\"", "\"quantity\":2"));
        expectRejectedCreate(created().replace("\"trace_chain\":[]", "\"trace_chain\":null"));
        sql.execute("DROP TABLE order_item_data");
        sql.execute("ALTER TABLE order_data ALTER COLUMN amount SET DATA TYPE DECIMAL(18,2)");
        sql.execute("CREATE TABLE order_item_data (id BIGINT PRIMARY KEY, version BIGINT NOT NULL, "
                + "order_id BIGINT NOT NULL REFERENCES order_data(id), quantity BIGINT NOT NULL)");
        expectRejectedCreate(created());
    }

    @Test public void exactFractionalDecimalSurvivesDeclaredDuckDbStorage() {
        String fractional = created().replace("\"amount\":\"0\"", "\"amount\":\"0.1\"");
        assertFalse(target.apply(context, record(1, "fractional", fractional)).duplicate());
        BigDecimal actual = (BigDecimal) sql.queryForList("SELECT amount FROM order_data",
                new Object[0]).get(0).get("amount");
        assertEquals(0, actual.compareTo(new BigDecimal("0.1")));
    }

    @Test public void zoneFreePhysicalTimestampCannotReceiveCommittedInstant() {
        sql.execute("DROP TABLE order_item_data");
        sql.execute("DROP TABLE order_data");
        sql.execute("CREATE TABLE order_data (id BIGINT PRIMARY KEY, version BIGINT NOT NULL, "
                + "order_number VARCHAR NOT NULL, status VARCHAR NOT NULL, planned_date DATE, "
                + "occurred_at TIMESTAMP NOT NULL, amount DECIMAL(19,7), active BOOLEAN NOT NULL, "
                + "ratio DOUBLE NOT NULL, optional_note VARCHAR, optional_label VARCHAR)");
        sql.execute("CREATE TABLE order_item_data (id BIGINT PRIMARY KEY, version BIGINT NOT NULL, "
                + "order_id BIGINT NOT NULL REFERENCES order_data(id), quantity BIGINT NOT NULL)");
        expectRejectedCreate(created());
    }

    private void expectRejectedCreate(String mutations) {
        expect("INVALID_ENVELOPE", () -> target.apply(context, record(1, "bad", mutations)));
        assertEmptyTarget();
    }

    private void assertEmptyTarget() {
        for (String table : List.of("order_data", "order_item_data", "teaql_event_inbox",
                "teaql_event_applied_payload", "teaql_event_checkpoint")) {
            assertEquals(table, 0, count(table));
        }
    }

    private long count(String table) {
        return ((Number) sql.queryForList("SELECT count(*) AS row_count FROM " + table,
                new Object[0]).get(0).get("row_count")).longValue();
    }

    private static Record record(long sequence, String changeSetId, String mutations) {
        String payload = """
                {"protocol_version":1,"change_set_id":"%s","source_runtime_id":"%s",
                 "source_sequence":"%d","source_model_hash":"%s","source_transaction_id":"tx-%d",
                 "committed_at":"2026-10-16T00:00:00Z",
                 "audit":{"actor_ref":"fixture:operator","comment":"Apply order graph",
                          "purpose":"Verify target application","trace_id":"fixture-trace"},
                 "mutations":%s}
                """.formatted(changeSetId, SOURCE, sequence, HASH, sequence, mutations);
        return recordBody(sequence, changeSetId, payload);
    }

    private static Record recordBody(long sequence, String changeSetId, String payload) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
            return new Record(SOURCE, sequence, changeSetId, HASH, digest, payload);
        } catch (Exception impossible) { throw new AssertionError(impossible); }
    }

    private static String created() {
        return """
                [{"entity_type":"order","entity_id":"9007199254740993","operation":"create",
                  "base_version":null,"committed_version":"1",
                  "fields":{"order_number":"ORD-0001","status":"DRAFT",
                            "planned_date":"2026-10-16","occurred_at":"-315619199877",
                            "amount":"0","active":true,"ratio":0.5,"optional_note":null,
                            "optional_label":"initial label"},"trace_chain":[]},
                 {"entity_type":"order_item","entity_id":"9007199254740994","operation":"create",
                  "base_version":null,"committed_version":"1",
                  "fields":{"order":{"entity_type":"order","entity_id":"9007199254740993"},
                            "quantity":"2"},"trace_chain":[]}]
                """;
    }

    private static String submitted() {
        return """
                [{"entity_type":"order","entity_id":"9007199254740993","operation":"update",
                  "base_version":"1","committed_version":"2",
                  "fields":{"status":"SUBMITTED","optional_note":"submitted note",
                            "optional_label":null},"trace_chain":[]},
                 {"entity_type":"order_item","entity_id":"9007199254740994","operation":"delete",
                  "base_version":"1","committed_version":"-2","fields":{},"trace_chain":[]}]
                """;
    }

    private static void expect(String code, Runnable action) {
        assertEquals(code, assertThrows(Failure.class, action::run).code());
    }
}
