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
    public void testListRowMutationCannotChangeSiblingColumnsOrValues() {
        var rows = sqlExecutor.queryForList("SELECT 1 AS id, NULL AS note UNION ALL SELECT 2, 'private'", new Object[0]);
        assertTrue(rows.get(0) instanceof JdbcColumnRow);
        assertTrue(rows.get(0).containsKey("note"));
        rows.get(0).put("note", "changed");
        rows.get(0).remove("id");
        rows.get(0).put("extra", null);
        assertEquals(2, ((Number) rows.get(1).get("id")).intValue());
        assertEquals("private", rows.get(1).get("note"));
        assertTrue(!rows.get(1).containsKey("extra"));
    }

    @Test
    public void testMapStreamRowsSurviveCloseAndDuplicateAliasesKeepLastValue() {
        List<Map<String, Object>> rows;
        try (var stream = sqlExecutor.queryForStream("SELECT 1 AS \"Name\", 2 AS \"NAME\" UNION ALL SELECT 3, 4", new Object[0])) {
            rows = stream.toList();
        }
        assertTrue(rows.get(0) instanceof JdbcColumnRow);
        assertEquals(1, rows.get(0).size());
        assertEquals(2, ((Number) rows.get(0).get("name")).intValue());
        rows.get(0).put("name", 5);
        assertEquals(4, ((Number) rows.get(1).get("name")).intValue());
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
    public void testCompiledRowMapperReadsTypedValuesWithoutIntermediateMap() {
        sqlExecutor.update(
                "INSERT INTO test_user (id, name, age) VALUES (?, ?, ?)",
                new Object[] {7, null, 42});

        List<TypedUser> rows = sqlExecutor.query(
                "SELECT id, name, age * 1.0 FROM test_user WHERE id = ?",
                new Object[] {7},
                row -> new TypedUser(
                        row.get(1, Long.class),
                        row.get(2, String.class),
                        row.get(3, java.math.BigDecimal.class)));

        assertEquals(1, rows.size());
        assertEquals(Long.valueOf(7), rows.get(0).id);
        assertEquals(null, rows.get(0).name);
        assertEquals(0, new java.math.BigDecimal("42.0").compareTo(rows.get(0).age));
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

    @Test
    public void testQueryForStreamReadsLazilyAndSupportsEarlyClose() {
        for (int id = 1; id <= 5; id++) {
            sqlExecutor.update(
                    "INSERT INTO test_user (id, name, age) VALUES (?, ?, ?)",
                    new Object[] {id, "user-" + id, 20 + id});
        }

        try (java.util.stream.Stream<Map<String, Object>> rows =
                     sqlExecutor.queryForStream("SELECT id FROM test_user ORDER BY id", new Object[0])) {
            assertEquals(2, rows.limit(2).count());
        }

        // Closing a partially consumed stream must release the cursor and its connection.
        Number total = (Number) sqlExecutor
                .queryForList("SELECT count(*) AS total FROM test_user", new Object[0])
                .get(0).get("total");
        assertEquals(5, total.intValue());
    }

    @Test
    public void streamingPrepareFailureClosesAcquiredConnection() throws Exception {
        failedStreamClosesConnection("SELECT * FROM table_does_not_exist");
    }

    @Test
    public void streamingExecuteFailureClosesAcquiredConnection() throws Exception {
        failedStreamClosesConnection("SELECT id FROM test_user WHERE id = ?");
    }

    private void failedStreamClosesConnection(String sql) throws Exception {
        var acquired = new ArrayList<Connection>();
        var tracking = (DataSource) java.lang.reflect.Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                    try {
                        var value = method.invoke(dataSource, args);
                        if (value instanceof Connection connection) acquired.add(connection);
                        return value;
                    } catch (java.lang.reflect.InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
        try {
            org.junit.Assert.assertThrows(RuntimeException.class,
                    () -> new JdbcSqlExecutor(tracking).queryForStream(sql, new Object[0]));
            assertEquals(1, acquired.size());
            assertTrue("failed stream leaked its connection", acquired.get(0).isClosed());
        } finally {
            for (var connection : acquired) connection.close();
        }
    }

    private static final class TypedRow extends io.teaql.core.BaseEntity {
        String name;
        Integer age;
    }

    @Test
    public void compiledStreamIsLazyTypedAndClosesOnExhaustionEarlyCloseAndMapperFailure() throws Exception {
        sqlExecutor.update("INSERT INTO test_user (id,name,age) VALUES (?,?,?)", new Object[]{1,"typed",null});
        sqlExecutor.update("INSERT INTO test_user (id,name,age) VALUES (?,?,?)", new Object[]{2,"second",20});
        for (String outcome : List.of("exhaustion", "early", "runtime", "error")) {
            var acquired = new ArrayList<Connection>();
            var tracking = (DataSource) java.lang.reflect.Proxy.newProxyInstance(
                    DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                        try {
                            Object value = method.invoke(dataSource,args);
                            if (value instanceof Connection connection) acquired.add(connection);
                            return value;
                        } catch (java.lang.reflect.InvocationTargetException error) { throw error.getCause(); }
                    });
            var adapter = new JdbcSqlExecutor(tracking);
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            io.teaql.core.CompiledRowMapper<TypedRow> mapper = row -> {
                calls.incrementAndGet();
                if (outcome.equals("runtime")) throw new IllegalStateException("mapper failed");
                if (outcome.equals("error")) throw new AssertionError("mapper error");
                TypedRow value = new TypedRow();
                value.__internalSet("id",row.get(1,Long.class));
                value.name=row.get(2,String.class); value.age=row.get(3,Integer.class);
                return value;
            };
            var stream = adapter.queryForStream("SELECT id,name,age FROM test_user ORDER BY id",new Object[0],mapper);
            assertTrue(adapter.supportsCompiledStreamMapping());
            assertEquals(0,calls.get()); assertEquals(1,acquired.size());
            if (outcome.equals("runtime")) org.junit.Assert.assertThrows(IllegalStateException.class,stream::findFirst);
            else if (outcome.equals("error")) org.junit.Assert.assertThrows(AssertionError.class,stream::findFirst);
            else if (outcome.equals("early")) {
                TypedRow first=stream.findFirst().orElseThrow();
                assertEquals(1L,first.getId().longValue()); assertEquals("typed",first.name);
                org.junit.Assert.assertNull(first.age); assertEquals(1,calls.get());
                assertTrue(!acquired.get(0).isClosed());
            } else {
                var rows=stream.toList(); assertEquals(2,rows.size());
                assertEquals(Integer.valueOf(20),rows.get(1).age);
                assertTrue(acquired.get(0).isClosed());
            }
            if (outcome.equals("runtime") || outcome.equals("error")) assertTrue(acquired.get(0).isClosed());
            stream.close(); stream.close();
            assertTrue(acquired.get(0).isClosed());
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

    private static final class TypedUser extends io.teaql.core.BaseEntity {
        private final Long id;
        private final String name;
        private final java.math.BigDecimal age;

        private TypedUser(Long id, String name, java.math.BigDecimal age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }
}
