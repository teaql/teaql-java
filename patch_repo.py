import re

with open("teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java", "r") as f:
    content = f.read()

# 1. Inject dialect and metadata
content = re.sub(
    r'(private Map<Class, SQLExpressionParser> expressionParsers = new ConcurrentHashMap<>();)',
    r'\1\n    private io.teaql.core.sql.SqlEntityMetadata sqlMetadata;\n    private io.teaql.core.sql.dialect.SqlDialect dialect = new io.teaql.core.sql.dialect.PostgreSqlDialect();\n\n    public io.teaql.core.sql.dialect.SqlDialect getDialect() {\n        return dialect;\n    }\n\n    public void setDialect(io.teaql.core.sql.dialect.SqlDialect dialect) {\n        this.dialect = dialect;\n    }\n\n    @Override\n    public String escapeIdentifier(String identifier) {\n        return dialect.escapeIdentifier(identifier);\n    }\n',
    content
)

# 2. Init sqlMetadata in initSQLMeta
content = re.sub(
    r'(private void initSQLMeta\(EntityDescriptor entityDescriptor\) \{\n)',
    r'\1        this.sqlMetadata = new io.teaql.core.sql.SqlEntityMetadata(entityDescriptor);\n',
    content
)

# 3. Replace buildDataSQL
build_data_sql = """    public String buildDataSQL(UserContext userContext, SearchRequest request, Map<String, Object> parameters) {
        String rawSql = request.getRawSql();
        if (ObjectUtil.isNotEmpty(rawSql)) {
            return rawSql;
        }

        String partitionProperty = request.getPartitionProperty();
        if (ObjectUtil.isNotEmpty(partitionProperty) && request.getSlice() != null) {
            ensureOrderByForPartition(request);
        }

        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        return compiler.buildDataSQL(sqlMetadata, this, userContext, request, parameters);
    }"""
content = re.sub(r'    public String buildDataSQL\(UserContext userContext, SearchRequest request, Map<String, Object> parameters\) \{.*?(?=    // ==========================================)', build_data_sql + "\n\n", content, flags=re.DOTALL)

# 4. Replace ensureOrderByForPartition, delete prepareCondition, prepareOrderBy, prepareLimit, etc.
to_remove_helpers_pattern = r'    private String prepareCondition\(.*?(?=    public String joinTables)'
content = re.sub(to_remove_helpers_pattern, """    private void ensureOrderByForPartition(SearchRequest<T> request) {
        OrderBys orderBy = request.getOrderBy();
        if (orderBy.isEmpty()) orderBy.addOrderBy(new OrderBy("id"));
    }

""", content, flags=re.DOTALL)

# 5. Remove joinTables, collectSelectSql, getTypeSQL, collectDataTables, collectTablesFromProperties
to_remove_join_tables_pattern = r'    public String joinTables\(.*?    @Override\n    public List<SQLColumn> getPropertyColumns\('
content = re.sub(to_remove_join_tables_pattern, r'    @Override\n    public List<SQLColumn> getPropertyColumns(', content, flags=re.DOTALL)

# 6. Replace createInternal
create_internal_replacement = """    @Override
    public void createInternal(UserContext userContext, Collection<T> createItems) {
        List<SQLEntity> sqlEntities = CollectionUtil.map(createItems,
                i -> convertToSQLEntityForInsert(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) return;

        SQLEntity sqlEntity = sqlEntities.get(0);
        Map<String, List<String>> tableColumns = sqlEntity.getTableColumnNames();

        Map<String, List<Object[]>> rows = new HashMap<>();
        for (SQLEntity entity : sqlEntities) {
            Map<String, List> tableColumnValues = entity.getTableColumnValues();
            for (Map.Entry<String, List> entry : tableColumnValues.entrySet()) {
                String k = entry.getKey();
                List v = entry.getValue();
                List<Object[]> values = rows.computeIfAbsent(k, key -> new ArrayList<>());
                if (auxiliaryTableNames.contains(k) && entity.allNullExceptID(v)) continue;
                values.add(v.toArray());
            }
        }

        TreeMap<String, List<Object[]>> sorted = MapUtil.sort(rows, (t1, t2) -> {
            if (t1.equals(versionTableName)) return -1;
            if (t2.equals(versionTableName)) return 1;
            return 0;
        });

        sorted.forEach((k, v) -> {
            if (v.isEmpty()) return;
            List<String> columns = tableColumns.get(k);
            io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
            String sql = compiler.buildInsertSQL(this, k, columns, sqlEntity.getTraceChain());
            database.batchUpdate(sql, v);
        });
    }"""
content = re.sub(r'    @Override\n    public void createInternal\(.*?(?=    @Override\n    public void updateInternal\()', create_internal_replacement + "\n\n", content, flags=re.DOTALL)


# 7. Replace updateInternal, updateVersionTableVersion, updatePrimaryTable, updateVersionTable
update_internal_replacement = """    @Override
    public void updateInternal(UserContext userContext, Collection<T> updateItems) {
        if (ObjectUtil.isEmpty(updateItems)) return;
        List<SQLEntity> sqlEntities = CollectionUtil.map(updateItems,
                i -> convertToSQLEntityForUpdate(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) return;

        for (SQLEntity sqlEntity : sqlEntities) {
            if (sqlEntity.isEmpty()) continue;
            Map<String, List<String>> tableColumnNames = sqlEntity.getTableColumnNames();
            Map<String, List> tableColumnValues = sqlEntity.getTableColumnValues();

            AtomicBoolean versionTableUpdated = new AtomicBoolean(false);
            tableColumnValues.forEach((k, v) -> {
                List<String> columns = new ArrayList<>(tableColumnNames.get(k));
                List l = new ArrayList(v);
                boolean versionTable = this.versionTableName.equals(k);
                boolean primaryTable = this.primaryTableNames.contains(k);

                if (versionTable) {
                    updateVersionTable(userContext, sqlEntity, versionTableUpdated, k, columns, l);
                } else if (primaryTable) {
                    updatePrimaryTable(userContext, sqlEntity, k, columns, l);
                } else {
                    String updateSql = dialect.buildSubsidiaryInsertSql(k, columns);
                    database.executeUpdate(updateSql, l.toArray());
                }
            });

            if (!versionTableUpdated.get()) {
                updateVersionTableVersion(userContext, sqlEntity);
            }
        }
    }

    private void updateVersionTableVersion(UserContext userContext, SQLEntity sqlEntity) {
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionTableVersionSQL(this, this.versionTableName);
        Object[] parameters = {sqlEntity.getVersion() + 1, sqlEntity.getId(), sqlEntity.getVersion()};
        int update = database.executeUpdate(updateSql, parameters);
        if (update != 1) throw new ConcurrentModifyException();
    }

    private void updatePrimaryTable(UserContext userContext, SQLEntity sqlEntity, String k, List<String> columns, List l) {
        l.add(sqlEntity.getId());
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdatePrimarySQL(this, k, columns, sqlEntity.getTraceChain());
        int update = database.executeUpdate(updateSql, l.toArray());
        if (update != 1) throw new RepositoryException("primary table update failed");
    }

    private void updateVersionTable(UserContext userContext, SQLEntity sqlEntity,
                                     AtomicBoolean versionTableUpdated, String k, List<String> columns, List l) {
        versionTableUpdated.set(true);
        columns.add("version");
        l.add(sqlEntity.getVersion() + 1);
        l.add(sqlEntity.getId());
        l.add(sqlEntity.getVersion());
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionSQL(this, k, columns, sqlEntity.getTraceChain());
        int update = database.executeUpdate(updateSql, l.toArray());
        if (update != 1) throw new ConcurrentModifyException();
    }"""
content = re.sub(r'    @Override\n    public void updateInternal\(.*?(?=    @Override\n    public void deleteInternal\()', update_internal_replacement + "\n\n", content, flags=re.DOTALL)


# 8. Replace deleteInternal and recoverInternal
delete_recover_replacement = """    @Override
    public void deleteInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) return;
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() > 0)
                .map(e -> new Object[]{-(e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        int[] rets = database.batchUpdate(updateSql, args);
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }

    @Override
    public void recoverInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) return;
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() < 0)
                .map(e -> new Object[]{(-e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        int[] rets = database.batchUpdate(updateSql, args);
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }"""
content = re.sub(r'    @Override\n    public void deleteInternal\(.*?(?=    // ==========================================\n    // ID generation)', delete_recover_replacement + "\n\n", content, flags=re.DOTALL)


# 9. Replace doAggregateInternal and remove aggregation helpers
aggregation_replacement = """    @Override
    protected AggregationResult doAggregateInternal(UserContext userContext, SearchRequest<T> request) {
        if (!request.hasSimpleAgg()) return null;

        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        List<String> tables = compiler.collectAggregationTables(sqlMetadata, this, userContext, request);
        Map<String, Object> parameters = new HashMap<>();
        Object preConfig = userContext.getObj(MULTI_TABLE);
        userContext.put(MULTI_TABLE, tables.size() > 1);

        try {
            String sql = compiler.buildAggregationSQL(sqlMetadata, this, userContext, request, parameters, tables);
            if (sql == null) return null;

            PositionalSQL psql = toPositional(sql, parameters);
            List<Map<String, Object>> rows = database.query(psql.sql, psql.args);

            AggregationResult result = new AggregationResult();
            result.setName(request.getAggregations().getName());
            List<AggregationItem> items = rows.stream().map(row -> {
                AggregationItem item = new AggregationItem();
                for (SimpleNamedExpression function : request.getAggregations().getAggregates()) {
                    item.addValue(function, row.get(function.name()));
                }
                for (SimpleNamedExpression dimension : request.getAggregations().getDimensions()) {
                    item.addDimension(dimension, row.get(dimension.name()));
                }
                return item;
            }).collect(Collectors.toList());
            result.setData(items);
            return result;
        } finally {
            userContext.put(MULTI_TABLE, preConfig);
        }
    }"""
content = re.sub(r'    @Override\n    protected AggregationResult doAggregateInternal\(.*?(?=    // ==========================================\n    // Stream support)', aggregation_replacement + "\n\n", content, flags=re.DOTALL)

with open("teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java", "w") as f:
    f.write(content)
