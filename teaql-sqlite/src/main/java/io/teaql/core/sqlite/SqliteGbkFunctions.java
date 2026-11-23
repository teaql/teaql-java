package io.teaql.core.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.Function;
import org.sqlite.core.Codes;

final class SqliteGbkFunctions {
    private SqliteGbkFunctions() {}

    static void register(Connection connection) throws SQLException {
        Connection sqlite = connection instanceof org.sqlite.SQLiteConnection
                ? connection : connection.unwrap(org.sqlite.SQLiteConnection.class);
        Function.create(sqlite, "teaql_gbk", new Function() {
            @Override protected void xFunc() throws SQLException {
                int type = value_type(0);
                if (type == Codes.SQLITE_NULL) {
                    result();
                } else if (type != Codes.SQLITE_TEXT) {
                    error("GBK requires TEXT or NULL");
                } else {
                    try {
                        result(SqliteGbkEncoding.encode(value_text(0)));
                    } catch (IllegalArgumentException ignored) {
                        error("GBK cannot encode text in the portable profile");
                    }
                }
            }
        }, 1, Function.FLAG_DETERMINISTIC);
    }
}
