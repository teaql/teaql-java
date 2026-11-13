package io.teaql.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import io.teaql.core.postgres.PostgresDataServiceExecutor;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.GenericSQLRelation;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real production-provider logs, not reconstructed compiler diagnostics. */
public class PostgresStatisticalDiagnosticsTest {
    private static final String CANARY = "PRIVATE-PLATFORM-279";
    public static class School extends BaseEntity { @Override public String typeName() { return "DiagSchool"; } }
    public static class Kind extends BaseEntity { @Override public String typeName() { return "DiagKind"; } }
    public static class Platform extends BaseEntity { @Override public String typeName() { return "DiagPlatform"; } }
    private static class Request<T extends BaseEntity> extends BaseRequest<T> {
        private final String type;
        Request(Class<T> entity, String type) { super(entity); this.type = type; }
        @Override public String getTypeName() { return type; }
        public Request<T> comment(String value) { internalComment(value); return this; }
    }

    private static final class Telemetry implements RuntimeTelemetry {
        final List<Diagnostic> events = new ArrayList<>();
        final AtomicLong sequence = new AtomicLong();
        @Override public boolean recordsSqlDiagnostics() { return true; }
        @Override public void recordDiagnostic(Diagnostic event) { events.add(event); }
        @Override public Scope start(Operation operation) {
            var correlation = new SqlExecutionCorrelation("11111111111111111111111111111111",
                    String.format(Locale.ROOT, "%016x", sequence.incrementAndGet()));
            return new Scope() {
                @Override public void success(Map<String,Object> attributes) {}
                @Override public void failure(Throwable failure) {}
                @Override public SqlExecutionCorrelation correlation() { return correlation; }
            };
        }
    }

    /** Each provider lease closes normally while the test owns the transaction. */
    private static final class TransactionDataSource implements DataSource, AutoCloseable {
        final Connection connection;
        TransactionDataSource() throws Exception {
            String url = System.getenv("TEAQL_TEST_POSTGRES_JDBC_URL");
            assertNotNull("Live acceptance requires TEAQL_TEST_POSTGRES_JDBC_URL", url);
            connection = DriverManager.getConnection(url, System.getenv("TEAQL_TEST_POSTGRES_USER"),
                    System.getenv("TEAQL_TEST_POSTGRES_PASSWORD"));
            try {
                try (var statement = connection.createStatement(); var row = statement.executeQuery(
                        "SELECT current_database(),shobj_description(oid,'pg_database') FROM pg_database WHERE datname=current_database()")) {
                    assertTrue(row.next()); assertEquals("teaql_rollup_279", row.getString(1));
                    assertEquals("TeaQL #279 owned layered statistics fixture", row.getString(2));
                }
                connection.setAutoCommit(false);
            } catch (Throwable error) { connection.close(); throw error; }
        }
        @Override public Connection getConnection() {
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("close")) return null;
                        try { return method.invoke(connection, args); }
                        catch (InvocationTargetException error) { throw error.getCause(); }
                    });
        }
        @Override public Connection getConnection(String user, String password) { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter value) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public void setLoginTimeout(int value) {}
        @Override public Logger getParentLogger() { return Logger.getLogger("TeaQL diagnostics test"); }
        @Override public boolean isWrapperFor(Class<?> type) { return type.isInstance(this); }
        @Override public <T> T unwrap(Class<T> type) throws SQLException {
            if (type.isInstance(this)) return type.cast(this);
            throw new SQLException("Unsupported test data source unwrap");
        }
        @Override public void close() throws SQLException {
            try { connection.rollback(); } finally { connection.close(); }
        }
        void seed() throws SQLException {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE diag_platform_279(id BIGINT PRIMARY KEY,version BIGINT,name TEXT) ON COMMIT DROP");
                statement.execute("CREATE TEMP TABLE diag_kind_279(id BIGINT PRIMARY KEY,version BIGINT,label TEXT,ordinal BIGINT,platform BIGINT) ON COMMIT DROP");
                statement.execute("CREATE TEMP TABLE diag_school_279(id BIGINT PRIMARY KEY,version BIGINT,\"Legacy Capacity\" BIGINT,kind BIGINT) ON COMMIT DROP");
                statement.execute("INSERT INTO diag_kind_279 VALUES(1001,1,'shared',1,1),(1002,1,'shared',2,1)");
                statement.execute("INSERT INTO diag_school_279 VALUES(1,1,800,1001),(2,1,800,1001),(3,1,800,1001),(4,1,800,1001),(5,1,100,1002)");
            }
            try (var statement = connection.prepareStatement("INSERT INTO diag_platform_279 VALUES(1,1,?)")) {
                statement.setString(1, CANARY); statement.executeUpdate();
            }
        }
    }

    private static EntityDescriptor entity(String name, Class<? extends BaseEntity> type, String table, String[][] fields) {
        var descriptor = new EntityDescriptor(); descriptor.setType(name); descriptor.setTargetType(type); descriptor.setDataService("postgres");
        var definitions = new ArrayList<String[]>(); definitions.add(new String[]{"id","id"}); definitions.add(new String[]{"version","version"});
        definitions.addAll(Arrays.asList(fields));
        for (var field : definitions) {
            boolean text = Set.of("name","label").contains(field[0]);
            var property = new GenericSQLProperty(table,field[1],text ? "TEXT" : "BIGINT");
            property.setName(field[0]); property.setOwner(descriptor); property.setType(new SimplePropertyType(text ? String.class : Long.class));
            descriptor.getProperties().add(property);
        }
        return descriptor;
    }
    private static void relation(SimpleEntityMetaFactory metadata, EntityDescriptor owner, String name, String target) {
        var property = (GenericSQLRelation)owner.addObjectProperty(metadata,name,target,owner.getType()+"List",
                metadata.resolveEntityDescriptor(target).getTargetType(),GenericSQLRelation::new);
        property.setTableName(owner.getProperties().stream().filter(p->p instanceof GenericSQLProperty)
                .map(p->((GenericSQLProperty)p).getTableName()).findFirst().orElseThrow());
        property.setColumnName(name); property.setColumnType("BIGINT");
    }
    private static SimpleEntityMetaFactory metadata() {
        var metadata = new SimpleEntityMetaFactory();
        var platform = entity("DiagPlatform",Platform.class,"diag_platform_279",new String[][]{{"name","name"}});
        platform.setAuditMaskFields(List.of("name"));
        var kind = entity("DiagKind",Kind.class,"diag_kind_279",new String[][]{{"label","label"},{"ordinal","ordinal"}});
        var school = entity("DiagSchool",School.class,"diag_school_279",new String[][]{{"capacity","Legacy Capacity"}});
        metadata.register(platform); metadata.register(kind); metadata.register(school);
        relation(metadata,kind,"platform","DiagPlatform"); relation(metadata,school,"kind","DiagKind");
        return metadata;
    }
    private static Request<School> request() {
        var eligible = new Request<>(Platform.class,"DiagPlatform"); eligible.groupBy("id"); eligible.count("eligible"); eligible.top(10);
        eligible.comment("Read independently eligible platforms");
        var wrapper = new Request<>(Kind.class,"DiagKind"); wrapper.selectProperty("id"); wrapper.unlimited();
        wrapper.comment("Route through ordinary type identities");
        wrapper.appendSearchCriteria(new SubQuerySearchCriteria("platform",eligible,"id"));
        var platform = new Request<>(Platform.class,"DiagPlatform"); platform.groupBy("name"); platform.count("platforms");
        platform.comment("Group visible platforms "+CANARY);
        platform.appendSearchCriteria(platform.createBasicSearchCriteria("name",Operator.EQUAL,CANARY));
        var kind = new Request<>(Kind.class,"DiagKind"); kind.count("types"); kind.comment("Group visible types");
        kind.groupBy("platform",platform,QueryOperation.nativeMethod(QueryOperation.Kind.AGGREGATE,"rollUpToPlatform",null,"platform"));
        var request = new Request<>(School.class,"DiagSchool"); request.count("schools"); request.sum("total","capacity"); request.top(10);
        request.groupBy("kind",kind,QueryOperation.nativeMethod(QueryOperation.Kind.AGGREGATE,"rollUpToKind",null,"kind"));
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind",wrapper,"id"));
        return request.comment("Count the source population");
    }

    @Test public void realProviderLogsAndTelemetryPreserveMaskedMultihopRequestTree() throws Exception {
        try (var source = new TransactionDataSource()) {
            source.seed(); var metadata = metadata(); var output = new ByteArrayOutputStream();
            var logs = new ArrayList<ExecutionMetadata>(); var sink = new DefaultTextRuntimeLogSink(new PrintStream(output));
            var telemetry = new Telemetry();
            var provider = new PostgresDataServiceExecutor("postgres",new JdbcSqlExecutor(source));
            var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("postgres",provider)
                    .telemetry(telemetry).logSink((ctx,event)->{logs.add(event);sink.writeExecutionLog(ctx,event);}).build());
            var request = request(); var json = new ObjectMapper();
            for (int round=1; round<=2; round++) {
                logs.clear(); output.reset(); telemetry.events.clear();
                var rows = request.purpose("Inspect real PostgreSQL diagnostic behavior").aggregation(context).toList();
                assertEquals(1,rows.size()); assertEquals(5,((Number)rows.get(0).get("schools")).longValue());
                assertEquals(3300,((Number)rows.get(0).get("total")).longValue());
                assertEquals("one real SQL record, not one invented record per layer",1,logs.size());
                var log=logs.get(0); var nodes=log.getRequestTree();
                var parent=nodes.stream().filter(node->"Group visible types".equals(node.detail())).findFirst().orElseThrow();
                var grandparent=nodes.stream().filter(node->node.detail().startsWith("Group visible platforms ")).findFirst().orElseThrow();
                var wrapper=nodes.stream().filter(node->"Route through ordinary type identities".equals(node.detail())).findFirst().orElseThrow();
                var eligible=nodes.stream().filter(node->"Read independently eligible platforms".equals(node.detail())).findFirst().orElseThrow();
                assertEquals("rollUpToKind",nodes.get(parent.parentId()).method());
                assertEquals("rollUpToPlatform",nodes.get(grandparent.parentId()).method());
                assertEquals(Integer.valueOf(parent.id()),nodes.get(grandparent.parentId()).parentId());
                assertEquals(Integer.valueOf(1),wrapper.parentId());
                assertEquals(Integer.valueOf(wrapper.id()),nodes.get(eligible.parentId()).parentId());
                var text=output.toString(java.nio.charset.StandardCharsets.UTF_8);
                assertFalse(text,text.contains(CANARY)); assertTrue(text,text.contains("/* masked */"));
                assertFalse(log.getDebugQuery().contains(CANARY));
                assertEquals(1,telemetry.events.size());
                var event=telemetry.events.get(0); var tree=json.readTree((String)event.attributes().get("teaql.request_tree"));
                assertEquals(json.valueToTree(nodes),tree); assertFalse(event.attributes().toString().contains(CANARY));
                assertNotNull(event.correlation()); assertEquals(log.getSqlExecutionCorrelation(),event.correlation());
                System.out.println("JAVA_POSTGRES_DIAGNOSTICS="+json.writeValueAsString(Map.of("round",round,
                        "physicalStatements",logs.size(),"schools",5,"total",3300,"requestTree",nodes,"telemetryTree",tree,"text",text)));
            }
        }
    }

    @Test public void realProviderFailureRetainsMaskedRequestTreeAndStatementIdentity() throws Exception {
        try (var source = new TransactionDataSource()) {
            source.seed(); var output = new ByteArrayOutputStream(); var logs = new ArrayList<ExecutionMetadata>();
            var sink = new DefaultTextRuntimeLogSink(new PrintStream(output)); var telemetry = new Telemetry();
            var provider = new PostgresDataServiceExecutor("postgres",new JdbcSqlExecutor(source));
            var context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata()).dataService("postgres",provider)
                    .telemetry(telemetry).logSink((ctx,event)->{logs.add(event);sink.writeExecutionLog(ctx,event);}).build());
            var request = request(); var json = new ObjectMapper();
            for (int round=1; round<=2; round++) {
                logs.clear(); telemetry.events.clear(); output.reset();
                var point = source.connection.setSavepoint();
                try {
                    // Deterministic real driver failure in this transaction's
                    // temporary table, not a fake transport or shared schema.
                    try (var statement = source.connection.createStatement()) {
                        statement.execute("ALTER TABLE diag_school_279 DROP COLUMN \"Legacy Capacity\"");
                    }
                    assertThrows(RuntimeException.class, () -> request.purpose("Inspect failed PostgreSQL diagnostics").aggregation(context).toList());
                    assertEquals(1,logs.size()); assertEquals(1,telemetry.events.size());
                    var log=logs.get(0); var event=telemetry.events.get(0);
                    assertEquals("failure",log.getExecutionOutcome());
                    assertEquals("failure",event.attributes().get("teaql.sql.outcome"));
                    assertNotNull(log.getSqlExecutionCorrelation()); assertEquals(log.getSqlExecutionCorrelation(),event.correlation());
                    var tree=json.readTree((String)event.attributes().get("teaql.request_tree"));
                    assertEquals(json.valueToTree(log.getRequestTree()),tree);
                    assertTrue(log.getRequestTree().stream().anyMatch(node->"rollUpToKind".equals(node.method())));
                    assertTrue(log.getRequestTree().stream().anyMatch(node->"rollUpToPlatform".equals(node.method())));
                    var text=output.toString(java.nio.charset.StandardCharsets.UTF_8);
                    assertFalse(text,text.contains(CANARY)); assertTrue(text,text.contains("/* masked */"));
                    System.out.println("JAVA_POSTGRES_FAILURE_DIAGNOSTICS="+json.writeValueAsString(Map.of("round",round,
                            "physicalStatements",logs.size(),"outcome",log.getExecutionOutcome(),"requestTree",log.getRequestTree(),
                            "telemetryTree",tree,"text",text)));
                } finally {
                    source.connection.rollback(point); source.connection.releaseSavepoint(point);
                }
            }
        }
    }
}
