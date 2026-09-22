package io.teaql.core.sql.portable;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Narrow, deterministic classification for schema-reconciliation races. */
final class SchemaExceptionClassifier {

    private SchemaExceptionClassifier() {}

    static boolean isDuplicateIndex(Throwable failure) {
        Set<Throwable> visited = new HashSet<>();
        for (Throwable current = failure;
                current != null && visited.add(current);
                current = current.getCause()) {
            if (current instanceof SQLException sqlException
                    && isDuplicateIndex(sqlException)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDuplicateIndex(SQLException failure) {
        String state = failure.getSQLState();
        int code = failure.getErrorCode();
        String message = failure.getMessage() == null
                ? ""
                : failure.getMessage().toLowerCase(Locale.ROOT);

        // PostgreSQL duplicate_table is also used for an existing relation/index name.
        if ("42P07".equals(state)) return true;
        // MySQL ER_DUP_KEYNAME.
        if (code == 1061) return true;
        // SQLite reports SQLITE_ERROR (1), so the exact duplicate-index wording is required.
        return code == 1 && message.contains("index") && message.contains("already exists");
    }
}
