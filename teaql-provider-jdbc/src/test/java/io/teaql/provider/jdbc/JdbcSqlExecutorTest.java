package io.teaql.provider.jdbc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcSqlExecutorTest {

    private DataSource dataSource;
    private JdbcSqlExecutor sqlExecutor;

    @Before
    public void setUp() throws Exception {
        dataSource = new SimpleDataSource("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        sqlExecutor = new JdbcSqlExecutor(dataSource);

        // Create a test table
        sqlExecutor.execute("CREATE TABLE test_user (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
    }

    @After
    public void tearDown() throws Exception {
        // Drop table to clean up memory DB
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE test_user");
        }
    }

    @Test
    public void testExecuteAndInsert() throws Exception {
        // Insert a record using positional update
        int affected = sqlExecutor.update(
            "INSERT INTO test_user (id, name, age) VALUES (?, ?, ?)",
            new Object[]{1, "Alice", 25}
        );
        assertEquals(1, affected);

        // Verify the insertion using raw JDBC query
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_user WHERE id = 1")) {
            assertTrue(rs.next());
            assertEquals("Alice", rs.getString("name"));
            assertEquals(25, rs.getInt("age"));
        }
    }

    @Test
    public void testQueryForListNormalizesAliasesForPortableMapping() {
        List<Map<String, Object>> rows = sqlExecutor.queryForList(
                "SELECT 1 AS \"customerOrder\", 2 AS plain_name",
                new Object[0]);

        assertEquals(1, rows.size());
        assertEquals(1, ((Number) rows.get(0).get("customerorder")).intValue());
        assertEquals(2, ((Number) rows.get(0).get("plain_name")).intValue());
    }

    @Test
    public void testTypedJdbcParametersRemainPortable() {
        sqlExecutor.update(
                "INSERT INTO test_user (id, name, age) VALUES (?, ?, ?)",
                new Object[] {1L, "Typed", 25});
        List<Map<String, Object>> rows = sqlExecutor.queryForList(
                "SELECT id FROM test_user WHERE id > ? AND age <= ?",
                new Object[] {0L, new java.math.BigDecimal("25")});
        assertEquals(1, rows.size());
        assertEquals(1, ((Number) rows.get(0).get("id")).intValue());
    }

    @Test
    public void testNullParameterUsesSqlNull() {
        sqlExecutor.update(
                "INSERT INTO test_user (id, name, age) VALUES (?, ?, ?)",
                new Object[] {4L, null, 40});
        List<Map<String, Object>> rows = sqlExecutor.queryForList(
                "SELECT name FROM test_user WHERE id = ?", new Object[] {4L});
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).containsKey("name"));
        assertEquals(null, rows.get(0).get("name"));
    }

    @Test
    public void testSqliteLocalDateParametersUseIsoTextOrdering() throws Exception {
        DataSource sqlite = new SimpleDataSource("jdbc:sqlite::memory:", "", "");
        try (Connection connection = sqlite.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE dated (id INTEGER, value TEXT)");
            statement.execute("INSERT INTO dated VALUES (1, '2026-01-05'), (2, '2026-01-06'), (3, '2026-01-12'), (4, '2026-01-13')");
            try (java.sql.PreparedStatement query = connection.prepareStatement(
                    "SELECT count(*) FROM dated WHERE value >= ? AND value <= ?")) {
                JdbcSqlExecutor.bind(query, 1, java.time.LocalDate.of(2026, 1, 6));
                JdbcSqlExecutor.bind(query, 2, java.time.LocalDate.of(2026, 1, 12));
                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(2, result.getInt(1));
                }
            }
        }
    }

    @Test
    public void testBatchUpdate() throws Exception {
        List<Object[]> batch = new ArrayList<>();
        batch.add(new Object[]{2, "Bob", 30});
        batch.add(new Object[]{3, "Charlie", 35});

        int[] affected = sqlExecutor.batchUpdate(
            "INSERT INTO test_user (id, name, age) VALUES (?, ?, ?)",
            batch
        );
        assertEquals(2, affected.length);
        assertEquals(1, affected[0]);
        assertEquals(1, affected[1]);

        // Verify using raw JDBC query
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_user")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    private static class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        public SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException { return null; }
        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {}
        @Override
        public void setLoginTimeout(int seconds) throws SQLException {}
        @Override
        public int getLoginTimeout() throws SQLException { return 0; }
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
    }
}
