package io.teaql.core.sql;

import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.PropertyReference;
import io.teaql.core.Slice;
import io.teaql.core.UserContext;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.expression.ExpressionHelper;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SqlAstCompiler {

    public static final String MULTI_TABLE = "multi_table";
    public static final String IGNORE_SUBTYPES = "ignore_subtypes";
    public static final String ID = "id";
    public static final String TYPE_ALIAS = "_type_alias";

    /**
     * Builds the final SQL query string from a SearchRequest.
     */
    public String buildDataSQL(
            SqlEntityMetadata metadata,
            SqlCompilerDelegate repository,
            UserContext userContext, 
            SearchRequest<?> request, 
            Map<String, Object> parameters) {
            
        boolean preConfig = userContext.getBool(MULTI_TABLE, false);

        try {
            List<String> tables = collectDataTables(metadata, repository, userContext, request);
            if (tables.size() > 1) {
                userContext.put(MULTI_TABLE, true);
            }

            String idTable = tables.get(0);

            // conditions
            String whereSql = prepareCondition(metadata, repository, userContext, idTable, request.getSearchCriteria(), parameters);

            // from & joins
            String tableSQl = joinTables(metadata, repository, userContext, tables);

            // selects
            String selectSql = collectSelectSql(metadata, repository, userContext, request, idTable, parameters);

            // order by
            String orderBySql = prepareOrderBy(repository, userContext, request, idTable, parameters);

            String partitionProperty = request.getPartitionProperty();
            if (ObjectUtil.isNotEmpty(partitionProperty) && request.getSlice() != null) {
                return handlePartitionSql(metadata, repository, userContext, request, selectSql, tableSQl, whereSql, orderBySql, partitionProperty, idTable);
            } else {
                String sql = StrUtil.format("SELECT {} FROM {}", selectSql, tableSQl);

                if (whereSql != null && !SearchCriteria.TRUE.equalsIgnoreCase(whereSql)) {
                    sql = StrUtil.format("{} WHERE {}", sql, whereSql);
                }

                if (!ObjectUtil.isEmpty(orderBySql)) {
                    sql = StrUtil.format("{} {}", sql, orderBySql);
                }

                String limitSql = prepareLimit(repository, request);
                if (!ObjectUtil.isEmpty(limitSql)) {
                    sql = StrUtil.format("{} {}", sql, limitSql);
                }
                return sql;
            }
        } finally {
            userContext.put(MULTI_TABLE, preConfig);
        }
    }

    public String buildAggregationSQL(
            SqlEntityMetadata metadata, 
            SqlCompilerDelegate repository, 
            UserContext userContext, 
            SearchRequest<?> request, 
            Map<String, Object> parameters, 
            List<String> tables) {
            
        String idTable = tables.get(0);
        String whereSql = prepareCondition(metadata, repository, userContext, idTable, request.getSearchCriteria(), parameters);

        if (io.teaql.core.SearchCriteria.FALSE.equalsIgnoreCase(whereSql)) {
            return null;
        }

        String selectSql = collectAggregationSelectSql(metadata, repository, userContext, request, idTable, parameters);
        String sql = io.teaql.core.utils.StrUtil.format("SELECT {} FROM {}", selectSql, joinTables(metadata, repository, userContext, tables));

        if (whereSql != null && !io.teaql.core.SearchCriteria.TRUE.equalsIgnoreCase(whereSql)) {
            sql = io.teaql.core.utils.StrUtil.format("{} WHERE {}", sql, whereSql);
        }

        String groupBy = collectAggregationGroupBySql(metadata, repository, userContext, request, idTable, parameters);
        if (!io.teaql.core.utils.ObjectUtil.isEmpty(groupBy)) {
            sql = io.teaql.core.utils.StrUtil.format("{} {}", sql, groupBy);
        }
        return sql;
    }

    protected String handlePartitionSql(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request, String selectSql, String tableSQl, String whereSql, String orderBySql, String partitionProperty, String idTable) {
        PropertyDescriptor partitionPropertyDescriptor = repository.findProperty(partitionProperty);
        SQLColumn sqlColumn = repository.getSqlColumn(partitionPropertyDescriptor);
        String partitionTable = partitionPropertyDescriptor.isId() ? idTable : sqlColumn.getTableName();

        if (whereSql != null && !SearchCriteria.TRUE.equalsIgnoreCase(whereSql)) {
            whereSql = "WHERE " + whereSql;
        }

        return StrUtil.format(
                repository.getPartitionSQL(),
                selectSql,
                userContext.getBool(MULTI_TABLE, false) ? repository.escapeIdentifier(tableAlias(partitionTable)) + "." : "",
                repository.escapeIdentifier(sqlColumn.getColumnName()),
                orderBySql,
                tableSQl,
                whereSql != null ? whereSql : "",
                request.getSlice().getOffset() + 1,
                request.getSlice().getOffset() + request.getSlice().getSize() + 1);
    }

    public String joinTables(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, List<String> tables) {
        List<String> sortedTables = new ArrayList<>();
        for (String table : tables) {
            if (metadata.getPrimaryTableNames().contains(table)) {
                sortedTables.add(table);
            }
        }
        for (String table : tables) {
            if (!metadata.getPrimaryTableNames().contains(table)) {
                sortedTables.add(table);
            }
        }

        if (!userContext.getBool(MULTI_TABLE, false)) {
            return StrUtil.format("{}", repository.escapeIdentifier(sortedTables.get(0)));
        }

        StringBuilder sb = new StringBuilder();
        String preTable = null;
        for (String sortedTable : sortedTables) {
            if (preTable == null) {
                preTable = sortedTable;
                sb.append(StrUtil.format("{} AS {}", repository.escapeIdentifier(sortedTable), repository.escapeIdentifier(tableAlias(sortedTable))));
                continue;
            }
            sb.append(
                    StrUtil.format(
                            " {} JOIN {} AS {} ON {}.{} = {}.{}",
                            metadata.getPrimaryTableNames().contains(sortedTable) ? "INNER" : "LEFT",
                            repository.escapeIdentifier(sortedTable),
                            repository.escapeIdentifier(tableAlias(sortedTable)),
                            repository.escapeIdentifier(tableAlias(sortedTable)),
                            repository.escapeIdentifier(ID),
                            repository.escapeIdentifier(tableAlias(preTable)),
                            repository.escapeIdentifier(ID)));
        }
        return sb.toString();
    }

    private String collectSelectSql(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request, String idTable, Map<String, Object> pParameters) {
        List<SimpleNamedExpression> allSelects = new ArrayList<>();
        if (request.getProjections() != null) allSelects.addAll(request.getProjections());
        if (request.getSimpleDynamicProperties() != null) allSelects.addAll(request.getSimpleDynamicProperties());

        if (allSelects.isEmpty() && metadata != null) {
            for (PropertyDescriptor pd : metadata.getAllProperties()) {
                allSelects.add(new SimpleNamedExpression(pd.getName(), new PropertyReference(pd.getName())));
            }
        }

        String selects = allSelects.stream()
                .map(e -> ExpressionHelper.toSql(userContext, e, idTable, pParameters, repository))
                .collect(Collectors.joining(", "));

        if (!userContext.getBool(IGNORE_SUBTYPES, false)) {
            String typeSQL = repository.getTypeSQL(userContext);
            if (ObjectUtil.isNotEmpty(typeSQL)) {
                selects = selects + ", " + typeSQL;
            }
        }
        return selects;
    }

    private String collectAggregationGroupBySql(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request, String idTable, Map<String, Object> parameters) {
        List<io.teaql.core.SimpleNamedExpression> dimensions = request.getAggregations().getDimensions();
        if (dimensions.isEmpty()) {
            return null;
        }
        return dimensions.stream()
                .map(dimension -> {
                    io.teaql.core.Expression expression = dimension.getExpression();
                    while (expression instanceof io.teaql.core.SimpleNamedExpression) {
                        expression = ((io.teaql.core.SimpleNamedExpression) expression).getExpression();
                    }
                    return expression;
                })
                .map(expression -> io.teaql.core.sql.expression.ExpressionHelper.toSql(userContext, expression, idTable, parameters, repository))
                .collect(Collectors.joining(",", " GROUP BY ", ""));
    }

    private String collectAggregationSelectSql(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request, String idTable, Map<String, Object> params) {
        List<io.teaql.core.SimpleNamedExpression> allSelected = request.getAggregations().getSelectedExpressions();
        return allSelected.stream()
                .map(expression -> io.teaql.core.sql.expression.ExpressionHelper.toSql(userContext, expression, idTable, params, repository))
                .collect(Collectors.joining(","));
    }

    public List<String> collectAggregationTables(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request) {
        Set<String> tables = new HashSet<>();
        for (String target : request.aggregationProperties(userContext)) {
            io.teaql.core.meta.PropertyDescriptor property = repository.findProperty(target);
            if (property == null || property.isId()) continue;
            
            if (property instanceof SQLProperty) {
                for (SQLColumn col : ((SQLProperty) property).columns()) {
                    tables.add(col.getTableName());
                }
            }
        }
        tables.add(metadata.getThisPrimaryTableName());
        return new ArrayList<>(tables);
    }

    public List<String> collectDataTables(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request) {
        Set<String> tables = new HashSet<>();
        for (String target : request.dataProperties(userContext)) {
            PropertyDescriptor property = repository.findProperty(target);
            if (property == null || property.isId()) continue;
            
            if (property instanceof SQLProperty) {
                for (SQLColumn col : ((SQLProperty) property).columns()) {
                    tables.add(col.getTableName());
                }
            }
        }
        tables.add(metadata.getThisPrimaryTableName());
        return new ArrayList<>(tables);
    }

    private String tableAlias(String table) {
        return io.teaql.core.utils.NamingCase.toCamelCase(table);
    }

    protected String prepareLimit(SqlCompilerDelegate repository, SearchRequest<?> request) {
        return repository.prepareLimit(request);
    }

    private String prepareOrderBy(SqlCompilerDelegate repository, UserContext userContext, SearchRequest<?> request, String idTable, Map<String, Object> parameters) {
        if (ObjectUtil.isEmpty(request.getOrderBy())) return null;
        return ExpressionHelper.toSql(userContext, request.getOrderBy(), idTable, parameters, repository);
    }

    private String prepareCondition(SqlEntityMetadata metadata, SqlCompilerDelegate repository, UserContext userContext, String idTable, SearchCriteria criteria, Map<String, Object> parameters) {
        String sqlCond = SearchCriteria.TRUE;
        if (criteria != null) {
            sqlCond = ExpressionHelper.toSql(userContext, criteria, idTable, parameters, repository);
        }

        if (SearchCriteria.FALSE.equalsIgnoreCase(sqlCond)) {
            return sqlCond;
        }

        // Auto soft-delete filter
        if (metadata != null && metadata.getVersionTableName() != null) {
            boolean hasVersionFilter = (criteria != null && criteria.properties(userContext).contains("version"));
            if (!hasVersionFilter) {
                String versionCol = repository.getSqlColumn(repository.findProperty("version")).getColumnName();
                String versionCond;
                if (userContext.getBool(MULTI_TABLE, false)) {
                    versionCond = StrUtil.format("{}.{} > 0", 
                            repository.escapeIdentifier(tableAlias(metadata.getVersionTableName())),
                            repository.escapeIdentifier(versionCol));
                } else {
                    versionCond = StrUtil.format("{} > 0", 
                            repository.escapeIdentifier(versionCol));
                }
                if (sqlCond == null || SearchCriteria.TRUE.equalsIgnoreCase(sqlCond)) {
                    sqlCond = versionCond;
                } else {
                    sqlCond = StrUtil.format("({}) AND {}", sqlCond, versionCond);
                }
            }
        }
        return sqlCond;
    }

    public String buildInsertSQL(SqlCompilerDelegate repository, String tableName, List<String> columns, String traceChain) {
        String sql = StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({})",
                repository.escapeIdentifier(tableName),
                columns.stream().map(repository::escapeIdentifier).collect(java.util.stream.Collectors.joining(",")),
                StrUtil.repeatAndJoin("?", columns.size(), ",")
        );
        if (traceChain != null && !traceChain.isEmpty()) {
            sql += " /* [" + traceChain + "] */";
        }
        return sql;
    }

    public String buildUpdatePrimarySQL(SqlCompilerDelegate repository, String tableName, List<String> columns, String traceChain) {
        String sql = StrUtil.format(
                "UPDATE {} SET {} WHERE {} = ?",
                repository.escapeIdentifier(tableName),
                columns.stream().map(c -> repository.escapeIdentifier(c) + " = ?").collect(java.util.stream.Collectors.joining(" , ")),
                repository.escapeIdentifier("id")
        );
        if (traceChain != null && !traceChain.isEmpty()) {
            sql += " /* [" + traceChain + "] */";
        }
        return sql;
    }

    public String buildUpdateVersionSQL(SqlCompilerDelegate repository, String tableName, List<String> columns, String traceChain) {
        String sql = StrUtil.format(
                "UPDATE {} SET {} WHERE {} = ? AND {} = ?",
                repository.escapeIdentifier(tableName),
                columns.stream().map(c -> repository.escapeIdentifier(c) + " = ?").collect(java.util.stream.Collectors.joining(" , ")),
                repository.escapeIdentifier("id"),
                repository.escapeIdentifier("version")
        );
        if (traceChain != null && !traceChain.isEmpty()) {
            sql += " /* [" + traceChain + "] */";
        }
        return sql;
    }

    public String buildUpdateVersionTableVersionSQL(SqlCompilerDelegate repository, String tableName) {
        return StrUtil.format(
                "UPDATE {} SET {} = ? WHERE {} = ? and {} = ?",
                repository.escapeIdentifier(tableName),
                repository.escapeIdentifier("version"),
                repository.escapeIdentifier("id"),
                repository.escapeIdentifier("version")
        );
    }

    public String buildDeleteSQL(SqlCompilerDelegate repository, String versionTableName) {
        return StrUtil.format(
                "UPDATE {} SET {} = ? WHERE {} = ? AND {} = ?",
                repository.escapeIdentifier(versionTableName),
                repository.escapeIdentifier("version"),
                repository.escapeIdentifier("id"),
                repository.escapeIdentifier("version")
        );
    }

    public String buildRecoverSQL(SqlCompilerDelegate repository, String versionTableName) {
        return StrUtil.format(
                "UPDATE {} SET {} = ? WHERE {} = ? AND {} = ?",
                repository.escapeIdentifier(versionTableName),
                repository.escapeIdentifier("version"),
                repository.escapeIdentifier("id"),
                repository.escapeIdentifier("version")
        );
    }
}
