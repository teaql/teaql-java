package io.teaql.mysql;

import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

public class MysqlTemporalSqlTest {
    @Test
    public void preparedAndDiagnosticSqlAreEquivalent() throws Exception {
        String url = System.getenv("TEAQL_TEST_MYSQL_URL");
        Assume.assumeTrue(url != null && !url.isBlank());
        try (Connection connection = DriverManager.getConnection(url)) {
            run(connection, "DROP TABLE IF EXISTS teaql_temporal_java_fixture");
            run(connection, "CREATE TABLE teaql_temporal_java_fixture(id INTEGER, d DATE, local_time DATETIME(3))");
            String sql = "INSERT INTO teaql_temporal_java_fixture VALUES (?, ?, ?) /* ignored ? */";
            Object[] values = {1, LocalDate.of(2024, 2, 29), LocalDateTime.of(2026, 8, 19, 3, 30, 0, 123_000_000)};
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, values[0]); statement.setObject(2, values[1]); statement.setObject(3, values[2]);
                statement.executeUpdate();
            }
            run(connection, SqlDataServiceExecutor.debugSql(sql, values, "mysql").replaceFirst("VALUES \\(1,", "VALUES (2,"));
            assertRowsEqual(connection);
            run(connection, "DROP TABLE teaql_temporal_java_fixture");
        }
    }
    private static void run(Connection c, String sql) throws SQLException { try (Statement s = c.createStatement()) { s.execute(sql); } }
    private static void assertRowsEqual(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT d, local_time FROM teaql_temporal_java_fixture ORDER BY id")) {
            assertTrue(r.next()); Object d = r.getObject(1), t = r.getObject(2);
            assertTrue(r.next()); assertEquals(d, r.getObject(1)); assertEquals(t, r.getObject(2)); assertFalse(r.next());
        }
    }
}
