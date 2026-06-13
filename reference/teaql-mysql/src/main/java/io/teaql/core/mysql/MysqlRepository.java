package io.teaql.core.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CaseInsensitiveMap;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Entity;
import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;

public class MysqlRepository<T extends Entity> extends SQLRepository<T> {
    public MysqlRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, dataSource);
        registerExpressionParser(MysqlAggrExpressionParser.class);
        registerExpressionParser(MysqlParameterParser.class);
        registerExpressionParser(MysqlTwoOperatorExpressionParser.class);
    }

    protected String generateAlterColumnSQL(UserContext ctx, SQLColumn column) {
        String alterColumnSql =
                StrUtil.format(
                        "ALTER TABLE {} MODIFY COLUMN {}  {}",
                        column.getTableName(),
                        column.getColumnName(),
                        column.getType());
        return alterColumnSql;
    }
/*

SELECT
    tc.CONSTRAINT_NAME AS name,
    tc.TABLE_NAME AS tableName,
    kcu.COLUMN_NAME AS columnName,
    kcu.REFERENCED_TABLE_NAME AS fTableName,
    kcu.REFERENCED_COLUMN_NAME AS fColumnName
FROM
    information_schema.TABLE_CONSTRAINTS AS tc
JOIN information_schema.KEY_COLUMN_USAGE AS kcu
    ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
WHERE
    tc.CONSTRAINT_TYPE = 'FOREIGN KEY';

*/
    protected String fetchFKsSQL() {
        return """
            SELECT
                tc.CONSTRAINT_NAME AS name,
                tc.TABLE_NAME AS tableName,
                kcu.COLUMN_NAME AS columnName,
                kcu.REFERENCED_TABLE_NAME AS fTableName,
                kcu.REFERENCED_COLUMN_NAME AS fColumnName
            FROM
                information_schema.TABLE_CONSTRAINTS AS tc
            JOIN information_schema.KEY_COLUMN_USAGE AS kcu
                ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
            WHERE
                tc.CONSTRAINT_TYPE = 'FOREIGN KEY';
                """;
    }
    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {
        super.ensureIndexAndForeignKey(ctx);


    }

    @Override
    protected void ensure(
            UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        tableInfo = CollStreamUtil.toList(tableInfo, CaseInsensitiveMap::new);
        super.ensure(ctx, tableInfo, table, columns);
    }

    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            return String.format(
                    "select * from information_schema.columns where table_name = '%s' and table_schema = '%s'",
                    table, databaseName);
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }

    @Override
    public boolean canMixinSubQuery(UserContext userContext, SearchRequest subQuery) {
        Slice slice = subQuery.getSlice();
        return slice == null;
    }
}
