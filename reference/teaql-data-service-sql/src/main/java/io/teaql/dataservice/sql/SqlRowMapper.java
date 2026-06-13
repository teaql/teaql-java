package io.teaql.coreservice.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SqlRowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
