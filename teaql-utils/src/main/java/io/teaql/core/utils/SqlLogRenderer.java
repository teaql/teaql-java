package io.teaql.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * JDBC diagnostic SQL rendering, never SQL execution. The callback receives a zero-based
 * binding position and must return an already safe SQL literal, including any mask annotation.
 * This class has no access to raw execution parameters when rendering a safe projection.
 */
public final class SqlLogRenderer {
    private SqlLogRenderer() {}

    public static String render(String sql, int parameterCount, IntFunction<String> literal,
                                String databaseKind) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(literal, "literal");
        if (parameterCount < 0) throw invalid();
        String kind = kind(databaseKind);
        StringBuilder output = new StringBuilder();
        int index = 0;
        for (int i = 0; i < sql.length();) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (c == '\'' || c == '"' || c == '`' || (c == '[' && kind.contains("mssql"))) {
                char end = c == '[' ? ']' : c;
                int start = i++;
                boolean closed = false;
                while (i < sql.length()) {
                    char ch = sql.charAt(i++);
                    if (ch == '\\' && kind.contains("mysql")) {
                        if (i >= sql.length()) throw invalid();
                        i++;
                    } else if (ch == end) {
                        if (i < sql.length() && sql.charAt(i) == end) i++;
                        else { closed = true; break; }
                    }
                }
                if (!closed) throw invalid();
                output.append(sql, start, i);
            } else if (c == '-' && next == '-') {
                int start = i;
                while (i < sql.length() && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') i++;
                output.append(sql, start, i);
            } else if (c == '/' && next == '*') {
                int start = i;
                i += 2;
                int depth = 1;
                // MySQL executable comments are not inert diagnostic text.
                if (i < sql.length() && (sql.charAt(i) == '!' || sql.charAt(i) == '+')) throw invalid();
                while (i < sql.length() && depth > 0) {
                    if (i + 1 < sql.length() && sql.startsWith("/*", i)) { depth++; i += 2; }
                    else if (i + 1 < sql.length() && sql.startsWith("*/", i)) { depth--; i += 2; }
                    else i++;
                }
                if (depth != 0) throw invalid();
                output.append(sql, start, i);
            } else if (c == '$' && kind.contains("postgres")) {
                int end = sql.indexOf('$', i + 1);
                String tag = end < 0 ? "!" : sql.substring(i + 1, end);
                if (tag.isEmpty() || tag.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    String delimiter = sql.substring(i, end + 1);
                    int close = sql.indexOf(delimiter, end + 1);
                    if (close < 0) throw invalid();
                    output.append(sql, i, close + delimiter.length());
                    i = close + delimiter.length();
                } else { output.append(c); i++; }
            } else if (c == '?' && next == '?' && kind.contains("postgres")) {
                // PgJDBC escapes a literal question-mark operator as ??.
                output.append('?'); i += 2;
            } else if (c == '?') {
                if (index >= parameterCount) throw invalid();
                String value = literal.apply(index++);
                if (value == null) throw invalid();
                output.append(value); i++;
            } else {
                // E'...' escape strings and MySQL # comments need a dedicated parser;
                // reject instead of silently treating an inner ? as a bind position.
                if (kind.contains("postgres") && (c == 'E' || c == 'e') && next == '\'') throw invalid();
                if (kind.contains("mysql") && c == '#') throw invalid();
                output.append(c); i++;
            }
        }
        if (index != parameterCount) throw invalid();
        return output.toString();
    }

    /** A typed literal for a value already authorized for this log destination. */
    public static String literal(Object value, String databaseKind) {
        String kind = kind(databaseKind);
        if (value == null) return "NULL";
        if (value instanceof LocalDate date) {
            String quoted = quote(date.toString(), kind);
            if (kind.contains("mysql") || kind.contains("mssql")) return "CAST(" + quoted + " AS DATE)";
            return kind.contains("sqlite") ? quoted : "DATE " + quoted;
        }
        if (value instanceof LocalDateTime time) {
            String quoted = quote(java.sql.Timestamp.valueOf(time).toString(), kind);
            if (kind.contains("mysql")) return "CAST(" + quoted + " AS DATETIME(3))";
            if (kind.contains("mssql")) return "CAST(" + quoted + " AS DATETIME2(3))";
            return kind.contains("sqlite") ? quoted : "TIMESTAMP " + quoted;
        }
        if (value instanceof String || value instanceof Character || value instanceof java.util.Date
                || value instanceof java.time.temporal.Temporal || value instanceof java.util.UUID)
            return quote(value.toString(), kind);
        if (value instanceof Boolean b) return b ? "TRUE" : "FALSE";
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger || value instanceof BigDecimal)
            return value.toString();
        if (value instanceof Float f && Float.isFinite(f)) return f.toString();
        if (value instanceof Double d && Double.isFinite(d)) return d.toString();
        if (value instanceof byte[] bytes) {
            String hex = java.util.HexFormat.of().formatHex(bytes);
            if (kind.contains("postgres")) return "decode('" + hex + "','hex')";
            if (kind.contains("mssql")) return "0x" + hex;
            if (kind.contains("oracle")) return "HEXTORAW('" + hex + "')";
            return "X'" + hex + "'";
        }
        // Do not invoke arbitrary toString(): it can leak nested secrets or produce invalid SQL.
        throw new IllegalArgumentException("SQL log literal type is unsupported");
    }

    private static String quote(String value, String kind) {
        if (value.indexOf('\0') >= 0) throw new IllegalArgumentException("SQL log literal contains NUL");
        if (kind.contains("mysql")) value = value.replace("\\", "\\\\");
        return "'" + value.replace("'", "''") + "'";
    }

    private static String kind(String value) {
        return Objects.toString(value, "sqlite").toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException invalid() {
        // Never include SQL or raw parameter values in failure diagnostics.
        return new IllegalArgumentException("SQL log binding mismatch or unsupported SQL syntax");
    }
}
