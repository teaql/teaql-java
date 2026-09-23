package io.teaql.dm8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.dm8.Dm8DataServiceExecutor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Assume;
import org.junit.Test;

public class Dm8SchemaIntegrationTest {

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
        String url = System.getenv("TEAQL_TEST_DM8_URL");
        String user = System.getenv("TEAQL_TEST_DM8_USER");
        String password = System.getenv("TEAQL_TEST_DM8_PASSWORD");
        if ("true".equalsIgnoreCase(System.getenv("TEAQL_REQUIRE_LIVE_DB"))) {
            assertTrue("TEAQL_TEST_DM8_URL must be set", url != null && !url.isBlank());
            assertTrue("TEAQL_TEST_DM8_USER must be set", user != null && !user.isBlank());
            assertTrue("TEAQL_TEST_DM8_PASSWORD must be set", password != null && !password.isBlank());
        }
        Assume.assumeTrue("DM8 live test credentials are not configured",
                url != null && user != null && password != null);

        DataSource dataSource = new TestDataSource(url, user, password);
        JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
        String table = "dm8_long_text_" + UUID.randomUUID().toString().substring(0, 8);

        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType("Dm8LongTextProbe");
        descriptor.setTargetType(BaseEntity.class);
        descriptor.setEntitySupplier(BaseEntity::new);
        descriptor.setDataService("dm8");
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
                .dataService("dm8", new Dm8DataServiceExecutor("dm8", jdbc))
                .build();
        UserContext context = new DefaultUserContext(runtime);
        context.ensureSchema();
        context.ensureSchema();

        List<Map<String, Object>> columns = jdbc.queryForList(
                "SELECT data_type FROM all_tab_columns WHERE owner = USER "
                        + "AND table_name = UPPER(?) AND column_name = 'PAYLOAD'",
                new Object[] {table});
        assertEquals(1, columns.size());
        assertEquals("CLOB", String.valueOf(columns.get(0).values().iterator().next())
                .toUpperCase(java.util.Locale.ROOT));
    }
}
