package io.teaql.provider.jdbc;

import io.teaql.core.BaseEntity;
import io.teaql.core.CompiledRowMapper;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

public class FiniteJdbcScalarTest {
    @Test public void bindingRejectsEveryNonfiniteBeforeCallingTheDriverSetter() {
        var calls = new AtomicInteger();
        var statement = (PreparedStatement)Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> { calls.incrementAndGet(); return null; });
        for (Number number : List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            var error = assertThrows(SQLException.class, () -> JdbcSqlExecutor.bind(statement,1,number));
            assertEquals("Non-finite JDBC floating-point parameter 1",error.getMessage());
            assertEquals(0,calls.get());
        }
    }
    @Test public void finiteValuesNegativeZeroAndNullReachTheDriverWithoutNarrowing() throws Exception {
        var values = new ArrayList<Object>();
        var statement = (PreparedStatement)Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> { values.add(args[1]); return null; });
        var expected = Arrays.asList(null, -0.0d, Double.MIN_VALUE, Double.MAX_VALUE, -0.0f, Float.MIN_VALUE, Float.MAX_VALUE, Long.MAX_VALUE);
        for (Object value : expected) JdbcSqlExecutor.bind(statement,1,value);
        assertEquals(expected,values);
        assertEquals(Double.doubleToRawLongBits(-0.0d),Double.doubleToRawLongBits((Double)values.get(1)));
    }
    static SQLiteDataSource database() {
        var source = new SQLiteDataSource(); source.setUrl("jdbc:sqlite:file:finite-" + UUID.randomUUID() + "?mode=memory&cache=shared"); return source;
    }
    static void corrupt(Connection db, double value) throws SQLException {
        db.createStatement().execute("CREATE TABLE numbers (id INTEGER PRIMARY KEY, amount REAL, version INTEGER)");
        try (var statement = db.prepareStatement("INSERT INTO numbers VALUES (1,?,1)")) {
            statement.setDouble(1,value); statement.executeUpdate();
        }
    }
    static CompiledRowMapper<BaseEntity> numeric(Class<?> type) {
        return row -> { row.get(1,type); return new BaseEntity(); };
    }
    static void unchanged(Connection db, double value) throws SQLException {
        try (var statement = db.createStatement(); var rows = statement.executeQuery("SELECT amount,version FROM numbers")) {
            assertTrue(rows.next()); assertEquals(Double.doubleToLongBits(value),Double.doubleToLongBits(rows.getDouble(1)));
            assertEquals(1,rows.getLong(2)); assertFalse(rows.next());
        }
    }
    @Test public void corruptLegacyInfinityRejectsMapTypedAndStreamingReadsWithoutChangingStorage() throws Exception {
        for (double number : new double[]{Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            var source = database();
            try (var witness = source.getConnection()) {
                corrupt(witness,number); var sql = new JdbcSqlExecutor(source);
                assertThrows(RuntimeException.class, () -> sql.queryForList("SELECT amount FROM numbers",new Object[0]));
                assertThrows(RuntimeException.class, () -> {try (var stream = sql.queryForStream("SELECT amount FROM numbers",new Object[0])) { stream.toList(); }});
                assertThrows(RuntimeException.class, () -> sql.query("SELECT amount FROM numbers",new Object[0],numeric(Double.class)));
                assertThrows(RuntimeException.class, () -> {try (var stream = sql.queryForStream("SELECT amount FROM numbers",new Object[0],numeric(Double.class))) { stream.toList(); }});
                assertEquals(1,sql.queryForList("SELECT id FROM numbers",new Object[0]).size());
                unchanged(witness,number);
            }
        }
    }
    @Test public void finiteDoubleThatOverflowsFloatRejectsTypedNarrowing() throws Exception {
        var source = database();
        try (var witness = source.getConnection()) {
            double wide = 3.5e38; corrupt(witness,wide); var sql = new JdbcSqlExecutor(source);
            assertEquals(wide,(Double)sql.queryForList("SELECT amount FROM numbers",new Object[0]).get(0).get("amount"),0);
            assertThrows(RuntimeException.class, () -> sql.query("SELECT amount FROM numbers",new Object[0],numeric(Float.class)));
            assertThrows(RuntimeException.class, () -> {try (var stream = sql.queryForStream("SELECT amount FROM numbers",new Object[0],numeric(Float.class))) { stream.toList(); }});
            unchanged(witness,wide);
        }
    }
}
