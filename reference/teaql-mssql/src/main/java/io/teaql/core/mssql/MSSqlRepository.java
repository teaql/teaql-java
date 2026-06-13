package io.teaql.core.mssql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.teaql.core.utils.LocalDateTimeUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.OrderBy;
import io.teaql.core.OrderBys;
import io.teaql.core.RepositoryException;
import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;

public class MSSqlRepository<T extends Entity> extends SQLRepository<T> {
    public MSSqlRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, dataSource);
    }

    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        try (Connection connection = dataSource.getConnection()) {
            String schemaName = connection.getSchema();
            if (schemaName == null) {
                schemaName = "dbo";
            }
            return String.format(
                    "select * from information_schema.columns where table_name = '%s' and table_schema = '%s'",
                    table, schemaName);
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }

    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {
    }

    @Override
    protected String getSqlValue(Object value) {
        if (value instanceof LocalDateTime) {
            return StrUtil.format("'{}'", LocalDateTimeUtil.formatNormal((LocalDateTime) value));
        }
        if (value instanceof LocalDate) {
            return StrUtil.format("'{}'", LocalDateTimeUtil.formatNormal((LocalDate) value));
        }
        return super.getSqlValue(value);
    }

    @Override
    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = (String) columnInfo.get("data_type");
        switch (dataType) {
            case "bigint":
                return "bigint";
            case "tinyint":
            case "boolean":
                return "boolean";
            case "bit":
                return "bit";
            case "varchar":
            case "character varying":
                return StrUtil.format("varchar({})", columnInfo.get("character_maximum_length"));
            case "date":
                return "date";
            case "datetime":
                return "datetime";
            case "int":
            case "integer":
                return "integer";
            case "decimal":
            case "numeric":
                return StrUtil.format(
                        "numeric({},{})", columnInfo.get("numeric_precision"), columnInfo.get("numeric_scale"));
            case "text":
                return "text";
            case "time without time zone":
                return "time";
            case "timestamp":
            case "timestamp(6)":
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + dataType);
        }
    }

    @Override
    public String prepareLimit(SearchRequest request) {
        Slice slice = request.getSlice();
        if (ObjectUtil.isEmpty(slice)) {
            return null;
        }
        return StrUtil.format(
                "OFFSET {} ROWS FETCH NEXT {} ROWS ONLY", slice.getOffset(), slice.getSize());
    }

    protected void ensureOrderByWhenNoOrderByClause(SearchRequest<T> request) {
        Slice slice = request.getSlice();
        if (slice == null) {
            return;
        }
        OrderBys orderBy = request.getOrderBy();
        if (orderBy.isEmpty()) {
            orderBy.addOrderBy(new OrderBy(BaseEntity.ID_PROPERTY));
        }
    }

    @Override
    public SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {

        ensureOrderByWhenNoOrderByClause(request);
        return super.loadInternal(userContext, request);
    }

    @Override
    public Stream<T> executeForStream(
            UserContext userContext, SearchRequest<T> request, int enhanceBatchSize) {
        ensureOrderByWhenNoOrderByClause(request);
        return super.executeForStream(userContext, request, enhanceBatchSize);
    }

    @Override
    protected String generateAddColumnSQL(UserContext ctx, String preColumnName, SQLColumn column) {
        String addColumnSql =
                StrUtil.format(
                        "ALTER TABLE {} ADD {} {}",
                        column.getTableName(),
                        column.getColumnName(),
                        column.getType());
        return addColumnSql;
    }

    @Override
    protected String generateAlterColumnSQL(UserContext ctx, SQLColumn column) {
        String alterColumnSql =
                StrUtil.format(
                        "ALTER TABLE {} ALTER COLUMN {} {}",
                        column.getTableName(),
                        column.getColumnName(),
                        column.getType());
        return alterColumnSql;
    }
}
