package io.teaql.core.sqlite;

import java.math.BigDecimal;
import java.math.BigInteger;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.AggrFunction;
import io.teaql.core.sql.dialect.PostgreSqlDialect;
import java.util.Locale;

/** SQLite-specific physical Decimal storage; ordinary SQL remains portable. */
final class SqliteDialect extends PostgreSqlDialect {

    @Override
    public String mapColumnType(String type) {
        String mapped = super.mapColumnType(type);
        String normalized = mapped.trim().toUpperCase(Locale.ROOT);
        for (String family : new String[] {"DECIMAL", "NUMERIC"}) {
            if (normalized.equals(family) || normalized.startsWith(family + "(")) {
                return "DECIMAL_TEXT" + mapped.trim().substring(family.length());
            }
        }
        return mapped;
    }

    @Override
    public String columnTypeAndConstraintsSql(SQLColumn column) {
        String type = mapColumnType(column.getType());
        if (!type.toUpperCase(Locale.ROOT).startsWith("DECIMAL_TEXT")) return type;
        String name = escapeIdentifier(column.getColumnName());
        return type + " COLLATE " + SqliteDecimalFunctions.COLLATION
                + " CHECK (" + name + " IS NULL OR " + SqliteDecimalFunctions.VALID
                + "(" + name + "))";
    }

    @Override
    public String decimalAggregateSql(AggrFunction function, String columnSql, boolean directColumn) {
        if (!directColumn) {
            throw new IllegalArgumentException(
                    "SQLite exact Decimal aggregation requires a direct Decimal column");
        }
        return switch (function) {
            case SUM -> SqliteDecimalFunctions.SUM + "(" + columnSql + ")";
            case SELF, MIN, MAX, COUNT -> null;
            default -> throw new IllegalArgumentException(
                    "SQLite exact Decimal aggregate is unsupported: " + function);
        };
    }

    @Override
    public Object decodeDecimalAggregate(Object value) {
        if (value == null || value instanceof BigDecimal) return value;
        if (value instanceof CharSequence text) return new BigDecimal(text.toString());
        if (value instanceof BigInteger || value instanceof Long || value instanceof Integer) {
            return new BigDecimal(value.toString());
        }
        throw new IllegalArgumentException("SQLite exact Decimal aggregate returned an inexact value");
    }
}
