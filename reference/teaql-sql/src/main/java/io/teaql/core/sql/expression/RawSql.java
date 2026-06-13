package io.teaql.core.sql.expression;

import java.util.Objects;
import io.teaql.core.SearchCriteria;

public class RawSql implements SearchCriteria {
    private final String sql;

    public RawSql(String pSql) {
        sql = pSql;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof RawSql rawSql)) return false;
        return Objects.equals(getSql(), rawSql.getSql());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSql());
    }
}
