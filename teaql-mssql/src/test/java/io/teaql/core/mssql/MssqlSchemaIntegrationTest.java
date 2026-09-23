package io.teaql.core.mssql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import org.junit.Assume;
import org.junit.Test;

public class MssqlSchemaIntegrationTest {

    private record TestDataSource(String url, String user, String password) implements DataSource {
        @Override public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }
        @Override public Connection getConnection(String username, String secret) throws SQLException {
            return DriverManager.getConnection(url, username, secret);
        }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter writer) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override public <T> T unwrap(Class<T> type) throws SQLException {
            throw new SQLException("Not a wrapper for " + type.getName());
        }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
    }

    @Test
    public void ensureSchemaUsesSelectedDialectForLargeText() {
        String url = System.getenv("TEAQL_TEST_MSSQL_URL");
        String user = System.getenv("TEAQL_TEST_MSSQL_USER");
        String password = System.getenv("TEAQL_TEST_MSSQL_PASSWORD");
        if ("true".equalsIgnoreCase(System.getenv("TEAQL_REQUIRE_LIVE_DB"))) {
            assertTrue("TEAQL_TEST_MSSQL_URL must be set", url != null && !url.isBlank());
            assertTrue("TEAQL_TEST_MSSQL_USER must be set", user != null && !user.isBlank());
            assertTrue("TEAQL_TEST_MSSQL_PASSWORD must be set", password != null && !password.isBlank());
        }
        Assume.assumeTrue("SQL Server live test credentials are not configured",
                url != null && user != null && password != null);

        DataSource dataSource = new TestDataSource(url, user, password);
        JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
        String table = "mssql_long_text_" + UUID.randomUUID().toString().substring(0, 8);

        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType("MssqlLongTextProbe");
        descriptor.setTargetType(BaseEntity.class);
        descriptor.setEntitySupplier(BaseEntity::new);
        descriptor.setDataService("mssql");
        for (String name : List.of("id", "version")) {
            GenericSQLProperty property = (GenericSQLProperty) descriptor.addSimpleProperty(name, Long.class);
            property.setTableName(table);
            property.setColumnName(name);
            property.setColumnType("BIGINT");
        }
        GenericSQLProperty payload = (GenericSQLProperty) descriptor.addSimpleProperty("payload", String.class);
        payload.setTableName(table);
        payload.setColumnName("payload");
        payload.setColumnType("LARGE_TEXT");
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        metadata.register(descriptor);

        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(metadata)
                .dataService("mssql", new MssqlDataServiceExecutor("mssql", jdbc))
                .build();
        UserContext context = new DefaultUserContext(runtime);
        context.ensureSchema();
        context.ensureSchema();

        List<Map<String, Object>> columns = jdbc.queryForList(
                "SELECT data_type, character_maximum_length FROM information_schema.columns "
                        + "WHERE table_schema = SCHEMA_NAME() AND table_name = ? AND column_name = ?",
                new Object[] {table, "payload"});
        assertEquals(1, columns.size());
        Map<String, Object> column = columns.get(0);
        assertEquals("nvarchar", String.valueOf(column.get("data_type")).toLowerCase(java.util.Locale.ROOT));
        assertEquals(-1, ((Number) column.get("character_maximum_length")).intValue());
    }
}
