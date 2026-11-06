package io.teaql.provider.jdbc;

import io.teaql.core.BaseEntity;
import io.teaql.core.CompiledRowMapper;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Real JDBC values through the same typed DataRow used by compiled entity mappers. */
@RunWith(Parameterized.class)
public class JdbcExactNumericReadTest {
    private record Vector(String name, String expression, Class<?> target, Object expected, boolean rejected) {}
    private final String driver;
    private final boolean streaming;
    private final Vector vector;
    private final List<Connection> connections = new ArrayList<>();

    public JdbcExactNumericReadTest(String name, String driver, boolean streaming, Vector vector) {
        this.driver = driver; this.streaming = streaming; this.vector = vector;
    }

    private static Vector good(String name, String sql, Class<?> type, Object expected) {
        return new Vector(name, sql, type, expected, false);
    }
    private static Vector bad(String name, String sql, Class<?> type) {
        return new Vector(name, sql, type, null, true);
    }
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> vectors() {
        var common = List.of(
                good("integer-zero", "CAST(0 AS BIGINT)", Integer.class, 0),
                good("integer-maximum", "CAST(2147483647 AS BIGINT)", Integer.class, Integer.MAX_VALUE),
                good("integer-minimum", "CAST(-2147483648 AS BIGINT)", Integer.class, Integer.MIN_VALUE),
                good("long-maximum", "CAST(9223372036854775807 AS BIGINT)", Long.class, Long.MAX_VALUE),
                good("long-minimum", "CAST(-9223372036854775808 AS BIGINT)", Long.class, Long.MIN_VALUE),
                good("byte-maximum", "CAST(127 AS INTEGER)", Byte.class, Byte.MAX_VALUE),
                good("byte-minimum", "CAST(-128 AS INTEGER)", Byte.class, Byte.MIN_VALUE),
                good("short-maximum", "CAST(32767 AS INTEGER)", Short.class, Short.MAX_VALUE),
                good("short-minimum", "CAST(-32768 AS INTEGER)", Short.class, Short.MIN_VALUE),
                good("null-integer", "CAST(NULL AS INTEGER)", Integer.class, null),
                good("null-long", "CAST(NULL AS BIGINT)", Long.class, null),
                good("integral-decimal-long", "CAST(9007199254740993 AS DECIMAL(30,0))", Long.class, 9007199254740993L),
                good("integral-double-large-exact-binary", "CAST(1152921504606846976 AS DOUBLE)", Long.class, 1152921504606846976L),
                good("decimal-from-large-long", "CAST(9007199254740993 AS BIGINT)", BigDecimal.class, new BigDecimal("9007199254740993")),
                good("decimal-from-minimum-long", "CAST(-9223372036854775808 AS BIGINT)", BigDecimal.class, new BigDecimal("-9223372036854775808")),
                good("decimal-exact-text", "CAST('12345678901234567890.123456789' AS VARCHAR)", BigDecimal.class, new BigDecimal("12345678901234567890.123456789")),
                good("finite-float", "CAST(3.50 AS DOUBLE)", Float.class, 3.50F),
                good("finite-double", "CAST(2.25 AS DOUBLE)", Double.class, 2.25D),
                good("approximate-float-remains-approximate", "CAST(0.1 AS DOUBLE)", Float.class, 0.1F),
                bad("fraction-to-long", "CAST(1.9 AS DECIMAL(30,3))", Long.class),
                bad("fraction-to-integer", "CAST(-1.9 AS DECIMAL(30,3))", Integer.class),
                bad("fraction-to-short", "CAST(0.5 AS DECIMAL(30,3))", Short.class),
                bad("fraction-to-byte", "CAST(-0.5 AS DECIMAL(30,3))", Byte.class),
                bad("integer-upper-overflow", "CAST(2147483648 AS BIGINT)", Integer.class),
                bad("integer-lower-overflow", "CAST(-2147483649 AS BIGINT)", Integer.class),
                bad("short-upper-overflow", "CAST(32768 AS INTEGER)", Short.class),
                bad("short-lower-overflow", "CAST(-32769 AS INTEGER)", Short.class),
                bad("byte-upper-overflow", "CAST(128 AS INTEGER)", Byte.class),
                bad("byte-lower-overflow", "CAST(-129 AS INTEGER)", Byte.class),
                bad("float-overflow", "1e40", Float.class),
                bad("double-positive-infinity", "1e999", Double.class),
                bad("double-negative-infinity", "-1e999", Double.class),
                bad("decimal-private-invalid-text", "CAST('PRIVATE-JDBC-VALUE-901' AS VARCHAR)", BigDecimal.class));
        var result = new ArrayList<Object[]>();
        for (String driver : List.of("h2", "sqlite")) {
            var selected = new ArrayList<>(common);
            if (driver.equals("h2")) selected.addAll(List.of(
                    bad("long-upper-overflow", "CAST(9223372036854775808 AS DECIMAL(40,0))", Long.class),
                    bad("long-lower-overflow", "CAST(-9223372036854775809 AS DECIMAL(40,0))", Long.class),
                    bad("long-upper-fraction", "CAST(9223372036854775807.5 AS DECIMAL(40,1))", Long.class),
                    bad("long-lower-fraction", "CAST(-9223372036854775808.5 AS DECIMAL(40,1))", Long.class),
                    bad("double-nan", "CAST('NaN' AS DOUBLE)", Double.class),
                    bad("float-nan", "CAST('NaN' AS DOUBLE)", Float.class)));
            // SQLite has already rounded an out-of-range decimal SQL literal to
            // a binary float. Test its observable upper limit, not lost digits.
            else selected.add(bad("long-rounded-upper-overflow", "9223372036854775808.0", Long.class));
            for (boolean stream : List.of(false, true)) for (Vector vector : selected) {
                result.add(new Object[]{driver + "/" + (stream ? "stream/" : "list/") + vector.name, driver, stream, vector});
            }
        }
        return result;
    }

    @Test public void typedReadIsExactOrFailsBeforeReturningAnEntity() throws Exception {
        String url = driver.equals("h2") ? "jdbc:h2:mem:exact_" + UUID.randomUUID() : "jdbc:sqlite::memory:";
        var source = new TrackingDataSource(url);
        var adapter = new JdbcSqlExecutor(source);
        String sql = "SELECT " + vector.expression + " AS required_numeric";
        // Calibrate the SQL and actual driver value independently. Unsupported
        // SQL must be an error, not mistaken for successful conversion rejection.
        // Bypass the adapter only for calibration: its untyped reads must also
        // reject non-finite values, so they cannot serve as a raw-driver oracle.
        try (var connection = source.getConnection();
             var statement = connection.createStatement();
             var raw = statement.executeQuery(sql)) {
            assertEquals(1, raw.getMetaData().getColumnCount());
            assertTrue(raw.getMetaData().getColumnLabel(1).equalsIgnoreCase("required_numeric"));
            assertTrue(raw.next());
            if (!vector.rejected && vector.expected == null) assertNull(raw.getObject(1));
            else assertNotNull(raw.getObject(1));
            assertFalse(raw.next());
        }
        if (vector.rejected) {
            var error = assertThrows(IllegalArgumentException.class, () -> read(adapter, sql, vector.target));
            assertTrue(error.getMessage(), error.getMessage().contains("column 1"));
            assertTrue(error.getMessage(), error.getMessage().contains("required_numeric"));
            assertTrue(error.getMessage(), error.getMessage().contains(vector.target.getName()));
            assertFalse(error.toString(), error.toString().contains("PRIVATE-JDBC-VALUE-901"));
            assertNull("conversion causes must not retain untrusted field values", error.getCause());
        } else assertEquals(vector.name, vector.expected, read(adapter, sql, vector.target));
        assertEquals("calibration plus typed query", 2, connections.size());
        assertClosed();
        assertEquals("same adapter recovers after a bad value", Integer.valueOf(7), read(adapter, "SELECT 7 AS required_numeric", Integer.class));
        assertEquals(3, connections.size()); assertClosed();
    }

    private Object read(JdbcSqlExecutor adapter, String sql, Class<?> type) throws Exception {
        CompiledRowMapper<ScalarRow> mapper = row -> { var entity = new ScalarRow(); entity.value = row.get(1, type); return entity; };
        if (!streaming) return adapter.query(sql, new Object[0], mapper).get(0).value;
        try (Stream<ScalarRow> rows = adapter.queryForStream(sql, new Object[0], mapper)) {
            try { return rows.findFirst().orElseThrow().value; }
            catch (RuntimeException failure) {
                // Check automatic failure cleanup before try-with-resources
                // closes the stream; otherwise a cursor leak would be hidden.
                assertClosed(); throw failure;
            }
        }
    }
    private void assertClosed() throws SQLException { for (Connection c : connections) assertTrue("query leaked its owned connection", c.isClosed()); }
    private static final class ScalarRow extends BaseEntity { Object value; }
    private final class TrackingDataSource implements DataSource {
        private final String url;
        TrackingDataSource(String url) { this.url = url; }
        @Override public Connection getConnection() throws SQLException {
            Connection delegate = DriverManager.getConnection(url);
            Connection tracked = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (p,m,a) -> {
                try { return m.invoke(delegate, a); } catch (InvocationTargetException e) { throw e.getCause(); }
            });
            connections.add(tracked); return tracked;
        }
        @Override public Connection getConnection(String u,String p) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
