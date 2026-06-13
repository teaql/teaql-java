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
