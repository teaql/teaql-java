package io.teaql.core.sql.dialect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSqlDialect implements SqlDialect {

    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "ALL", "ALTER", "AND", "AS", "ASC", "BETWEEN", "BY", "CASE", "CREATE", "DELETE", "DESC",
            "DISTINCT", "DROP", "EXISTS", "FALSE", "FROM", "GROUP", "HAVING", "IN", "INSERT", "INTO", "IS",
            "JOIN", "LIKE", "LIMIT", "NOT", "NULL", "OFFSET", "ON", "OR", "ORDER", "SELECT", "SET",
            "TABLE", "TRUE", "TYPE", "UNION", "UPDATE", "VALUES", "WHERE"
    ));

    protected boolean needsEscape(String identifier) {
        if (identifier == null || identifier.isEmpty() || "*".equals(identifier)) {
            return false;
        }
        if (identifier.startsWith("`") || identifier.startsWith("\"") || identifier.startsWith("[")) {
            return false;
        }
        if (SQL_KEYWORDS.contains(identifier.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return true;
            }
        }
        return false;
    }
}
