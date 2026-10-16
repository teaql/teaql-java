package io.teaql.core.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.Test;

public class SqliteDecimalFunctionsTest {

    @Test
    public void exactStorageComparisonAndSumSurviveJdbcDecimalBinding() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteDecimalFunctions.register(connection);
            SqliteDecimalFunctions.register(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE amounts (value DECIMAL_TEXT COLLATE TEAQL_DECIMAL "
                        + "CHECK (value IS NULL OR teaql_decimal_valid(value)))");
            }
            for (String value : new String[] {"3.50", "2", "10", "123456789012.3456789", "-0.1"}) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO amounts (value) VALUES (?)")) {
                    statement.setBigDecimal(1, new BigDecimal(value));
                    statement.executeUpdate();
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT typeof(value), value FROM amounts ORDER BY value LIMIT 1")) {
                rows.next();
                assertEquals("text", rows.getString(1));
                assertEquals("-0.1", rows.getString(2));
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT value FROM amounts WHERE value = '3.5'")) {
                rows.next();
                assertEquals("3.50", rows.getString(1));
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT teaql_decimal_sum(value), typeof(sum(value)) FROM amounts")) {
                rows.next();
                assertEquals("123456789027.7456789", rows.getString(1));
                assertEquals("real", rows.getString(2));
            }
            try (Statement statement = connection.createStatement()) {
                assertThrows(java.sql.SQLException.class,
                        () -> statement.executeUpdate("INSERT INTO amounts (value) VALUES ('not-a-decimal')"));
            }
        }
    }
}
