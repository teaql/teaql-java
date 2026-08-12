package io.teaql.core.hana;

import io.teaql.core.sql.dialect.PostgreSqlDialect;

/** SAP HANA uses LIMIT/OFFSET and the standard ROW_NUMBER window syntax. */
public final class HanaDialect extends PostgreSqlDialect {
    @Override
    public String mapColumnType(String type) {
        return "LARGE_TEXT".equalsIgnoreCase(type) ? "NCLOB" : type;
    }
}
