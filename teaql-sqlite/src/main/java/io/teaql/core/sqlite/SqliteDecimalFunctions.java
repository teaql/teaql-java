package io.teaql.core.sqlite;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.Collation;
import org.sqlite.Function;
import org.sqlite.core.Codes;

/** Connection-local exact Decimal primitives for SQLite's text-affinity columns. */
final class SqliteDecimalFunctions {

    static final String COLLATION = "TEAQL_DECIMAL";
    static final String VALID = "teaql_decimal_valid";
    static final String SUM = "teaql_decimal_sum";

    private SqliteDecimalFunctions() {}

    static void register(Connection connection) throws SQLException {
        Connection sqlite = connection instanceof org.sqlite.SQLiteConnection
                ? connection : connection.unwrap(org.sqlite.SQLiteConnection.class);
        Collation.create(sqlite, COLLATION, new Collation() {
            @Override
            protected int xCompare(String left, String right) {
                try {
                    return new BigDecimal(left).compareTo(new BigDecimal(right));
                } catch (NumberFormatException corruptLegacyValue) {
                    // A collation cannot report an error. Schema CHECK and row
                    // decoding reject invalid text at their respective edges.
                    return left.compareTo(right);
                }
            }
        });
        Function.create(sqlite, VALID, new Function() {
            @Override
            protected void xFunc() throws SQLException {
                if (value_type(0) != Codes.SQLITE_TEXT) {
                    result(0);
                    return;
                }
                String text = value_text(0);
                boolean valid;
                try {
                    valid = text != null && new BigDecimal(text).toPlainString().equals(text);
                } catch (NumberFormatException invalid) {
                    valid = false;
                }
                result(valid ? 1 : 0);
            }
        }, 1, Function.FLAG_DETERMINISTIC);
        Function.create(sqlite, SUM, new Function.Aggregate() {
            private BigDecimal total;

            @Override
            protected void xStep() throws SQLException {
                if (value_type(0) == Codes.SQLITE_NULL) return;
                if (value_type(0) != Codes.SQLITE_TEXT) {
                    error("invalid decimal storage type");
                    return;
                }
                String text = value_text(0);
                try {
                    BigDecimal value = new BigDecimal(text);
                    total = total == null ? value : total.add(value);
                } catch (NumberFormatException invalid) {
                    error("invalid decimal text");
                }
            }

            @Override
            protected void xFinal() throws SQLException {
                if (total == null) result();
                else result(total.toPlainString());
            }
        }, 1);
    }
}
