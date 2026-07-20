package io.teaql.core.sql;

import java.sql.ResultSet;
import java.util.List;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;

public interface SQLProperty {

    List<SQLColumn> columns();

    List<SQLData> toDBRaw(UserContext ctx, Entity entity, Object value);

    void setPropertyValue(UserContext ctx, Entity entity, ResultSet rs);
}
