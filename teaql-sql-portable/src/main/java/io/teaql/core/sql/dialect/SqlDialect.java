package io.teaql.core.sql.dialect;

import io.teaql.core.SearchRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * Generate a pagination clause using caller-provided parameter placeholders.
     * The placeholders already include the parameter marker (for example
     * {@code :limit0}); dialects must not inline pagination values here.
     */
    default String prepareParameterizedLimit(String limitPlaceholder, String offsetPlaceholder) {
        return "LIMIT " + limitPlaceholder + " OFFSET " + offsetPlaceholder;
    }

    /**
     * Generate parameterized pagination with awareness of whether the query
     * already has an ORDER BY clause. Most dialects do not need this detail.
     */
    default String prepareParameterizedLimit(
            String limitPlaceholder, String offsetPlaceholder, boolean hasOrderBy) {
        return prepareParameterizedLimit(limitPlaceholder, offsetPlaceholder);
    }

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

    /**
     * Compare the storage families reported by the provider with the type selected for the model.
     * Providers may override this when their storage type system has wider compatibility rules.
     */
    default boolean isCompatibleColumnType(String expected, String actual) {
        String expectedFamily = normalizedTypeFamily(expected);
        String actualFamily = normalizedTypeFamily(actual);
        return expectedFamily.isEmpty()
                || actualFamily.isEmpty()
                || expectedFamily.equals(actualFamily);
    }

    private static String normalizedTypeFamily(String type) {
        if (type == null) return "";
        String family = type.trim().toUpperCase(Locale.ROOT)
                .replaceFirst("\\s*\\(.*", "")
                .replaceAll("\\s+", " ");
        return Map.ofEntries(
                        Map.entry("CHARACTER VARYING", "VARCHAR"),
                        Map.entry("VARCHAR2", "VARCHAR"),
                        Map.entry("NVARCHAR2", "NVARCHAR"),
                        Map.entry("CHARACTER", "CHAR"),
                        Map.entry("INT", "INTEGER"),
                        Map.entry("INT4", "INTEGER"),
                        Map.entry("INT8", "BIGINT"),
                        Map.entry("DEC", "DECIMAL"),
                        Map.entry("NUMERIC", "DECIMAL"),
                        Map.entry("DOUBLE PRECISION", "DOUBLE"),
                        Map.entry("FLOAT8", "DOUBLE"),
                        Map.entry("FLOAT4", "REAL"),
                        Map.entry("BOOL", "BOOLEAN"),
                        Map.entry("TIMESTAMP WITHOUT TIME ZONE", "TIMESTAMP"),
                        Map.entry("TIMESTAMP WITH TIME ZONE", "TIMESTAMPTZ"))
                .getOrDefault(family, family);
    }
}
