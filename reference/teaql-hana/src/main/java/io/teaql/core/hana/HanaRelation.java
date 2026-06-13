package io.teaql.core.hana;

import java.sql.ResultSet;

import io.teaql.core.sql.GenericSQLRelation;

public class HanaRelation extends GenericSQLRelation {
    @Override
    protected boolean findName(ResultSet resultSet, String name) {
        return super.findName(resultSet, name);
    }

    @Override
    protected Object getValue(ResultSet resultSet) {
        return super.getValue(resultSet);
    }
}
