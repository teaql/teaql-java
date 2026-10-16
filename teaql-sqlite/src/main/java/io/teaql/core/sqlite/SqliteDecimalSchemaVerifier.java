package io.teaql.core.sqlite;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Fail closed when an existing Decimal column lacks TeaQL's exact semantics. */
final class SqliteDecimalSchemaVerifier {

    private SqliteDecimalSchemaVerifier() {}

    static void verify(SqlExecutionAdapter adapter, String table, String column) {
        List<Map<String, Object>> schema = adapter.queryForList(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name=?",
                new Object[] {table});
        if (schema.size() != 1) throw incompatible(table, column, "table definition is missing");
        String ddl = String.valueOf(value(schema.get(0), "sql"));
        String expected = column.toLowerCase(Locale.ROOT);
        List<String> check = List.of("check", "(", expected, "is", "null", "or",
                SqliteDecimalFunctions.VALID, "(", expected, ")", ")");
        boolean validated = columns(ddl).stream().anyMatch(tokens -> tokens.size() > 2
                && expected.equals(tokens.get(0)) && "decimal_text".equals(tokens.get(1))
                && hasStandaloneCheck(tokens, check));
        if (!validated) throw incompatible(table, column, "required Decimal CHECK is missing");

        List<Map<String, Object>> program = adapter.queryForList(
                "EXPLAIN SELECT 1 FROM " + quote(table) + " NOT INDEXED WHERE "
                        + quote(column) + " = ?", new Object[] {"0"});
        boolean collated = program.stream().anyMatch(row -> {
            String opcode = String.valueOf(value(row, "opcode"));
            Object p4 = value(row, "p4");
            return ("Eq".equals(opcode) || "Ne".equals(opcode))
                    && p4 != null && String.valueOf(p4).startsWith(SqliteDecimalFunctions.COLLATION);
        });
        if (!collated) throw incompatible(table, column, "TEAQL_DECIMAL collation is missing");
    }

    private static IllegalStateException incompatible(String table, String column, String reason) {
        return new IllegalStateException("Ensure Schema incompatible exact Decimal column '"
                + column + "' on table '" + table + "': " + reason
                + "; migrate the column explicitly before retrying");
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static Object value(Map<String, Object> row, String name) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private static boolean hasStandaloneCheck(List<String> tokens, List<String> check) {
        int depth = 0;
        for (int index = 0; index < tokens.size(); index++) {
            if (depth == 0 && "check".equals(tokens.get(index))
                    && index + check.size() <= tokens.size()
                    && tokens.subList(index, index + check.size()).equals(check)) return true;
            if ("(".equals(tokens.get(index))) depth++;
            else if (")".equals(tokens.get(index))) depth--;
        }
        return false;
    }

    private static List<List<String>> columns(String ddl) {
        List<String> tokens = tokens(ddl);
        int opening = tokens.indexOf("(");
        if (opening < 0) return List.of();
        List<List<String>> columns = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int depth = 1;
        for (int index = opening + 1; index < tokens.size(); index++) {
            String token = tokens.get(index);
            if ("(".equals(token)) depth++;
            else if (")".equals(token) && --depth == 0) {
                columns.add(current);
                break;
            } else if (",".equals(token) && depth == 1) {
                columns.add(current);
                current = new ArrayList<>();
                continue;
            }
            current.add(token);
        }
        return columns;
    }

    private static List<String> tokens(String sql) {
        List<String> tokens = new ArrayList<>();
        for (int offset = 0; offset < sql.length();) {
            char current = sql.charAt(offset);
            if (Character.isWhitespace(current)) {
                offset++;
            } else if (current == '-' && offset + 1 < sql.length() && sql.charAt(offset + 1) == '-') {
                offset += 2;
                while (offset < sql.length() && sql.charAt(offset) != '\n') offset++;
            } else if (current == '/' && offset + 1 < sql.length() && sql.charAt(offset + 1) == '*') {
                offset += 2;
                while (offset + 1 < sql.length()
                        && !(sql.charAt(offset) == '*' && sql.charAt(offset + 1) == '/')) offset++;
                offset = Math.min(sql.length(), offset + 2);
            } else if (current == '\'' || current == '"' || current == '`' || current == '[') {
                char close = current == '[' ? ']' : current;
                offset++;
                StringBuilder content = new StringBuilder();
                while (offset < sql.length()) {
                    char next = sql.charAt(offset++);
                    if (next == close) {
                        if (offset < sql.length() && sql.charAt(offset) == close) {
                            content.append(close);
                            offset++;
                        } else break;
                    } else content.append(next);
                }
                tokens.add(current == '\'' ? "<sql-string>"
                        : content.toString().toLowerCase(Locale.ROOT));
            } else if (Character.isLetterOrDigit(current) || current == '_' || current == '$') {
                int start = offset++;
                while (offset < sql.length()) {
                    char next = sql.charAt(offset);
                    if (!(Character.isLetterOrDigit(next) || next == '_' || next == '$')) break;
                    offset++;
                }
                tokens.add(sql.substring(start, offset).toLowerCase(Locale.ROOT));
            } else {
                tokens.add(String.valueOf(current));
                offset++;
            }
        }
        return tokens;
    }
}
