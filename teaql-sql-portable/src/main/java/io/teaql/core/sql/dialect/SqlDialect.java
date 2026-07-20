package io.teaql.core.sql.dialect;

import io.teaql.core.SearchRequest;
import java.util.List;

public interface SqlDialect {
    /**
     * Escape an identifier (table name, column name) to avoid SQL keyword conflicts.
     */
    String escapeIdentifier(String identifier);

    /**
     * Generate the LIMIT / OFFSET clause for pagination.
     */
    String prepareLimit(SearchRequest<?> request);

    /**
     * Get the SQL template for window function based partition querying.
     * e.g., "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) as _rank from {} {}) as t where t._rank >= {} and t._rank < {}"
     */
    String getPartitionSQL();

    /**
     * Build the insert or update SQL for a subsidiary table.
     * For MySQL this is usually REPLACE INTO.
     * For Postgres this could be INSERT ... ON CONFLICT DO UPDATE.
     */
    String buildSubsidiaryInsertSql(String tableName, List<String> columns);

    /**
     * Map a generic column type (like VARCHAR(<max>)) to a dialect-specific type (like TEXT or VARCHAR(MAX)).
     */
    default String mapColumnType(String type) {
        return type;
    }
}
