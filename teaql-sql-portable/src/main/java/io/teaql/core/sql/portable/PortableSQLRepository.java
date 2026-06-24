package io.teaql.core.sql.portable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.teaql.core.AggregationItem;
import io.teaql.core.AggregationResult;
import io.teaql.core.Aggregations;
import io.teaql.core.BaseEntity;
import io.teaql.core.ConcurrentModifyException;
import io.teaql.core.Entity;
import io.teaql.core.Expression;
import io.teaql.core.OrderBy;
import io.teaql.core.OrderBys;

import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.Slice;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;


import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.PropertyType;
import io.teaql.core.meta.Relation;

import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLColumnResolver;
import io.teaql.core.sql.SqlCompilerDelegate;
import io.teaql.core.sql.SQLConstraint;
import io.teaql.core.sql.SQLData;
import io.teaql.core.sql.SQLEntity;

import io.teaql.core.sql.SQLProperty;

import io.teaql.core.sql.expression.ExpressionHelper;
import io.teaql.core.sql.expression.SQLExpressionParser;
import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.NamingCase;
import io.teaql.core.utils.NumberUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

/**
 * Portable SQL Repository implementation.
 * No spring-jdbc dependency, accesses SQL databases via the TeaQLDatabase abstraction.
 * The current primary use case is Android, where the application supplies an Android-backed
 * TeaQLDatabase implementation.
 * Reuses SQLRepository's SQL building logic (buildDataSQL, etc.).
 */
public class PortableSQLRepository<T extends Entity> implements SqlCompilerDelegate {

    private static final Pattern NAMED_PARAM = Pattern.compile(":(\\w+)");

    private io.teaql.core.sql.SqlEntityMetadata sqlMetadata;
    private io.teaql.core.sql.dialect.SqlDialect dialect = new io.teaql.core.sql.dialect.PostgreSqlDialect();

    public io.teaql.core.sql.dialect.SqlDialect getDialect() {
        return dialect;
    }

    public void setDialect(io.teaql.core.sql.dialect.SqlDialect dialect) {
        this.dialect = dialect;
    }

        public String escapeIdentifier(String identifier) {
        return dialect.escapeIdentifier(identifier);
    }

    private static Map<Class, String> arrayTypeMap;
    public static final String TYPE_ALIAS = "_type_";
    public static final String IGNORE_SUBTYPES = "IGNORE_SUBTYPES";
    public static final String MULTI_TABLE = "MULTI_TABLE";

    private final EntityDescriptor entityDescriptor;
    private final TeaQLDatabase database;
    private String childType = "_child_type";
    private String childSqlType = "VARCHAR(100)";
    private String tqlIdSpaceTable = "teaql_id_space";
    private String versionTableName;
    private List<String> primaryTableNames = new ArrayList<>();
    private String thisPrimaryTableName;
    private Set<String> allTableNames = new LinkedHashSet<>();
    private List<String> types = new ArrayList<>();
    private List<String> auxiliaryTableNames;
    private List<PropertyDescriptor> allProperties = new ArrayList<>();
    private Map<Class, SQLExpressionParser> expressionParsers = new ConcurrentHashMap<>();

    public interface PortableSQLRepositoryResolver {
        PortableSQLRepository<?> resolve(String typeName);
    }

    private PortableSQLRepositoryResolver resolver;

    public PortableSQLRepositoryResolver getResolver() {
        return resolver;
    }

    public PortableSQLRepository(EntityDescriptor entityDescriptor, TeaQLDatabase database, PortableSQLRepositoryResolver resolver) {
        this.entityDescriptor = entityDescriptor;
        this.database = database;
        this.resolver = resolver;
        initSQLMeta(entityDescriptor);
        initExpressionParsers();
    }

    private void initExpressionParsers() {
        registerExpressionParser(new io.teaql.core.sql.expression.ANDExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.AggrExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.BetweenParser());
        registerExpressionParser(new io.teaql.core.sql.expression.FunctionApplyParser());
        registerExpressionParser(new io.teaql.core.sql.expression.NOTExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.NamedExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.ORExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.OneOperatorExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.OrderByExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.OrderBysParser());
        registerExpressionParser(new io.teaql.core.sql.expression.ParameterParser());
        registerExpressionParser(new io.teaql.core.sql.expression.PropertyParser());
        registerExpressionParser(new io.teaql.core.sql.expression.RawSqlParser());
        registerExpressionParser(new io.teaql.core.sql.expression.SubQueryParser());
        registerExpressionParser(new io.teaql.core.sql.expression.TwoOperatorExpressionParser());
        registerExpressionParser(new io.teaql.core.sql.expression.TypeCriteriaParser());
        registerExpressionParser(new io.teaql.core.sql.expression.VersionSearchCriteriaParser());
    }

    protected void registerExpressionParser(SQLExpressionParser sqlExpressionParser) {
        if (sqlExpressionParser == null) {
            return;
        }
        Class type = sqlExpressionParser.type();
        if (type != null) {
            expressionParsers.put(type, sqlExpressionParser);
        }
    }

    @Override
    public Map<Class, SQLExpressionParser> getExpressionParsers() {
        return expressionParsers;
    }

    // ==========================================
    // SQL building logic (reused from SQLRepository)
    // ==========================================

    public String buildDataSQL(UserContext userContext, SearchRequest request, Map<String, Object> parameters) {
        String rawSql = (String) request.getExtension("rawSql");
        if (ObjectUtil.isNotEmpty(rawSql)) {
            return rawSql;
        }

        String partitionProperty = request.getPartitionProperty();
        if (ObjectUtil.isNotEmpty(partitionProperty) && request.getSlice() != null) {
            ensureOrderByForPartition(request);
        }

        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        return compiler.buildDataSQL(sqlMetadata, this, userContext, request, parameters);
    }

    // ==========================================
    // Named parameter → positional parameter conversion
    // ==========================================

    private static class PositionalSQL {
        final String sql;
        final Object[] args;

        PositionalSQL(String sql, Object[] args) {
            this.sql = sql;
            this.args = args;
        }
    }

    private PositionalSQL toPositional(String namedSql, Map<String, Object> params) {
        List<Object> args = new ArrayList<>();
        Matcher m = NAMED_PARAM.matcher(namedSql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String paramName = m.group(1);
            Object value = params.get(paramName);
            Collection<?> expandedValues = expandedParameterValues(value);
            if (expandedValues != null) {
                appendExpandedParameter(expandedValues, args, m, sb);
            } else {
                args.add(value);
                m.appendReplacement(sb, "?");
            }
        }
        m.appendTail(sb);
        return new PositionalSQL(sb.toString(), args.toArray());
    }

    private Collection<?> expandedParameterValues(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        if (value instanceof Object[] array) {
            return Arrays.asList(array);
        }
        if (value instanceof int[] array) {
            List<Integer> values = new ArrayList<>(array.length);
            for (int item : array) values.add(item);
            return values;
        }
        if (value instanceof long[] array) {
            List<Long> values = new ArrayList<>(array.length);
            for (long item : array) values.add(item);
            return values;
        }
        if (value instanceof short[] array) {
            List<Short> values = new ArrayList<>(array.length);
            for (short item : array) values.add(item);
            return values;
        }
        if (value instanceof byte[] array) {
            List<Byte> values = new ArrayList<>(array.length);
            for (byte item : array) values.add(item);
            return values;
        }
        if (value instanceof double[] array) {
            List<Double> values = new ArrayList<>(array.length);
            for (double item : array) values.add(item);
            return values;
        }
        if (value instanceof float[] array) {
            List<Float> values = new ArrayList<>(array.length);
            for (float item : array) values.add(item);
            return values;
        }
        if (value instanceof boolean[] array) {
            List<Boolean> values = new ArrayList<>(array.length);
            for (boolean item : array) values.add(item);
            return values;
        }
        if (value instanceof char[] array) {
            List<Character> values = new ArrayList<>(array.length);
            for (char item : array) values.add(item);
            return values;
        }
        return null;
    }

    private void appendExpandedParameter(
            Collection<?> values, List<Object> args, Matcher matcher, StringBuffer sql) {
        if (values.isEmpty()) {
            args.add(null);
            matcher.appendReplacement(sql, "?");
            return;
        }

        StringBuilder placeholders = new StringBuilder();
        for (Object item : values) {
            args.add(item);
            if (placeholders.length() > 0) placeholders.append(", ");
            placeholders.append("?");
        }
        matcher.appendReplacement(sql, placeholders.toString());
    }

    // ==========================================
    // Data operations (TeaQLDatabase replaces spring-jdbc)
    // ==========================================

        public EntityDescriptor getEntityDescriptor() {
        return this.entityDescriptor;
    }

    public SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {
        Map<String, Object> params = new HashMap<>();
        String sql = buildDataSQL(userContext, request, params);
        if (ObjectUtil.isEmpty(sql)) {
            return new SmartList<>();
        }
        PositionalSQL psql = toPositional(sql, params);
        List<Map<String, Object>> rows = database.query(userContext, psql.sql, psql.args);
        List<T> results = rows.stream()
                .map(row -> mapRowToEntity(userContext, request, row))
                .collect(Collectors.toList());
        SmartList<T> smartList = new SmartList<>(results);
        
        java.util.List<io.teaql.core.FacetRequest> facetRequests = request.getFacetRequests();
        if (facetRequests != null && !facetRequests.isEmpty()) {
            io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
            for (io.teaql.core.FacetRequest facetRequest : facetRequests) {
                io.teaql.core.internal.TempRequest tr = new io.teaql.core.internal.TempRequest(request);
                tr.setAggregations(new io.teaql.core.Aggregations());
                tr.groupBy(facetRequest.getRelationName());
                tr.count("count");
                
                Map<String, Object> facetParams = new HashMap<>();
                java.util.List<String> facetTables = compiler.collectAggregationTables(this.sqlMetadata, this, userContext, tr);
                String facetSql = compiler.buildAggregationSQL(this.sqlMetadata, this, userContext, tr, facetParams, facetTables);
                if (!io.teaql.core.utils.ObjectUtil.isEmpty(facetSql)) {
                    PositionalSQL psqlFacet = toPositional(facetSql, facetParams);
                    List<Map<String, Object>> facetRows = database.query(userContext, psqlFacet.sql, psqlFacet.args);
                    
                    SmartList<io.teaql.core.Entity> facetEntities = new SmartList<>();
                    io.teaql.core.SearchRequest<?> relationReq = facetRequest.getRequest();
                    if (relationReq != null) {
                        String relationType = relationReq.getTypeName();
                        PortableSQLRepository relationRepo = resolver.resolve(relationType);
                        if (relationRepo != null) {
                            List<Object> relIds = new ArrayList<>();
                            Map<Long, Object> idToCount = new HashMap<>();
                            for (Map<String, Object> facetRow : facetRows) {
                                Object relId = facetRow.get(facetRequest.getRelationName());
                                Object countVal = facetRow.get("count");
                                if (relId != null) {
                                    relIds.add(relId);
                                    idToCount.put(io.teaql.core.utils.Convert.convert(Long.class, relId), countVal);
                                }
                            }
                            io.teaql.core.internal.TempRequest fetchRelReq = new io.teaql.core.internal.TempRequest(relationReq);
                            if (facetRequest.isMergeCriteria()) {
                                fetchRelReq.appendSearchCriteria(request.getSearchCriteria());
                            }
                            SmartList<?> loadedRels = relationRepo.loadInternal(userContext, fetchRelReq);
                            for (Object obj : loadedRels) {
                                io.teaql.core.Entity rel = (io.teaql.core.Entity) obj;
                                Object cnt = idToCount.get(rel.getId());
                                int countInt = cnt != null ? io.teaql.core.utils.Convert.convert(Integer.class, cnt) : 0;
                                if (rel instanceof io.teaql.core.BaseEntity) {
                                    ((io.teaql.core.BaseEntity) rel).addDynamicProperty("count", countInt);
                                }
                                facetEntities.add(rel);
                            }
                        }
                    }
                    smartList.addFacet(facetRequest.getFacetName(), facetEntities);
                }
            }
        }
        
        return smartList;
    }

    private T mapRowToEntity(UserContext userContext, SearchRequest<T> request, Map<String, Object> row) {
        Class<? extends T> returnType = request.returnType();
        T entity = createEntity(returnType);
        for (PropertyDescriptor property : this.allProperties) {
            if (!shouldHandle(property)) continue;
            if (!(property instanceof Relation)) {
                Object value = row.get(property.getName());
                if (value != null) {
                    Class targetType = property.getType().javaType();
                    entity.setProperty(property.getName(),
                            io.teaql.core.utils.Convert.convert(targetType, value));
                }
            } else if (property instanceof Relation) {
                Object value = row.get(property.getName());
                if (value != null) {
                    try {
                        Entity ref = createEntity((Class<? extends Entity>) property.getType().javaType());
                        ((BaseEntity) ref).internalSet("id", io.teaql.core.utils.Convert.convert(Long.class, value));
                        if (ref instanceof BaseEntity) {
                            ((BaseEntity) ref).set$status(io.teaql.core.EntityStatus.REFER);
                        }
                        entity.setProperty(property.getName(), ref);
                    } catch (Exception e) {
                        System.out.println("mapRowToEntity relation mapping error for property " + property.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        // Subtype
        Object typeAlias = row.get(TYPE_ALIAS);
        if (typeAlias != null) {
            entity.setRuntimeType(String.valueOf(typeAlias));
        }
        // Status
        Long version = entity.getVersion();
        if (version != null && version < 0) {
            if (entity instanceof BaseEntity) ((BaseEntity) entity).set$status(io.teaql.core.EntityStatus.PERSISTED_DELETED);
        } else {
            if (entity instanceof BaseEntity) ((BaseEntity) entity).set$status(io.teaql.core.EntityStatus.PERSISTED);
        }
        // Dynamic properties
        List<SimpleNamedExpression> simpleDynamicProperties = request.getSimpleDynamicProperties();
        for (SimpleNamedExpression dp : simpleDynamicProperties) {
            Object value = row.get(dp.name());
            if (value != null) entity.addDynamicProperty(dp.name(), value);
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    private <E extends Entity> E createEntity(Class<? extends E> entityType) {
        EntityDescriptor descriptor = resolveDescriptor(entityType);
        return (E) descriptor.createEntity();
    }

    private EntityDescriptor resolveDescriptor(Class<? extends Entity> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type cannot be null");
        }
        if (entityDescriptor.getTargetType() == entityType) {
            return entityDescriptor;
        }
        EntityMetaFactory metadata = EntityMetaFactory.get();
        if (metadata != null) {
            for (EntityDescriptor descriptor : metadata.allEntityDescriptors()) {
                if (descriptor.getTargetType() == entityType) {
                    return descriptor;
                }
            }
        }
        throw new IllegalStateException("No entity descriptor registered for " + entityType.getName());
    }

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
            database.batchUpdate(userContext, sql, v);
        });
    }

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
                    database.executeUpdate(userContext, updateSql, l.toArray());
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
        int update = database.executeUpdate(userContext, updateSql, parameters);
        if (update != 1) throw new ConcurrentModifyException();
    }

    private void updatePrimaryTable(UserContext userContext, SQLEntity sqlEntity, String k, List<String> columns, List l) {
        l.add(sqlEntity.getId());
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdatePrimarySQL(this, k, columns, sqlEntity.getTraceChain());
        int update = database.executeUpdate(userContext, updateSql, l.toArray());
        if (update != 1) throw new TeaQLRuntimeException("primary table update failed");
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
        int update = database.executeUpdate(userContext, updateSql, l.toArray());
        if (update != 1) throw new ConcurrentModifyException();
    }

        public void deleteInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) return;
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() > 0)
                .map(e -> new Object[]{-(e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        int[] rets = database.batchUpdate(userContext, updateSql, args);
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }

        public void recoverInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) return;
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() < 0)
                .map(e -> new Object[]{(-e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        int[] rets = database.batchUpdate(userContext, updateSql, args);
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }

    // ==========================================
    // ID generation
    // ==========================================

        public Long prepareId(UserContext userContext, T entity) {
        if (entity.getId() != null) return entity.getId();


        String type = CollectionUtil.getLast(types);
        AtomicLong current = new AtomicLong();

        database.executeInTransaction(userContext, () -> {
            Number dbCurrent = null;
            try {
                List<Map<String, Object>> rows = database.query(
                        StrUtil.format("SELECT current_level from {} WHERE type_name = '{}'", getTqlIdSpaceTable(), type),
                        new Object[0]);
                if (!rows.isEmpty()) {
                    Object val = rows.get(0).get("current_level");
                    if (val instanceof Number) dbCurrent = (Number) val;
                    else if (val != null) dbCurrent = Long.parseLong(String.valueOf(val));
                }
            } catch (Exception ignored) {
            }

            if (dbCurrent == null) {
                current.set(1L);
                database.executeUpdate(
                        StrUtil.format("INSERT INTO {} VALUES ('{}', {})", getTqlIdSpaceTable(), type, current),
                        new Object[0]);
            } else {
                dbCurrent = NumberUtil.add(dbCurrent, 1);
                database.executeUpdate(
                        StrUtil.format("UPDATE {} SET current_level = {} WHERE type_name = '{}'",
                                getTqlIdSpaceTable(), dbCurrent, type),
                        new Object[0]);
                current.set(dbCurrent.longValue());
            }
        });
        return current.get();
    }

    // ==========================================
    // Schema management
    // ==========================================

    public void ensureSchema(UserContext ctx) {
        List<SQLColumn> allColumns = new ArrayList<>();
        for (PropertyDescriptor ownProperty : entityDescriptor.getOwnProperties()) {
            allColumns.addAll(getSqlColumns(ownProperty));
        }
        if (entityDescriptor.hasChildren()) {
            SQLColumn childTypeCell = new SQLColumn(thisPrimaryTableName, getChildType());
            childTypeCell.setType(getChildSqlType());
            allColumns.add(childTypeCell);
        }

        Map<String, List<SQLColumn>> tableColumns = CollStreamUtil.groupByKey(allColumns, SQLColumn::getTableName);
        tableColumns.forEach((table, columns) -> {
            List<Map<String, Object>> dbTableInfo;
            try {
                dbTableInfo = database.getTableColumns(table);
            } catch (Exception e) {
                dbTableInfo = ListUtil.empty();
            }
            ensure(ctx, dbTableInfo, table, columns);
        });

        ensureInitData(ctx);
        ensureIdSpaceTable(ctx);
    }

    public void ensureIdSpaceTable(UserContext ctx) {
        List<Map<String, Object>> dbTableInfo;
        try {
            dbTableInfo = database.getTableColumns(getTqlIdSpaceTable());
        } catch (Exception e) {
            dbTableInfo = ListUtil.empty();
        }
        if (!ObjectUtil.isEmpty(dbTableInfo)) return;

        String sql = "CREATE TABLE " + getTqlIdSpaceTable() + " (\n"
                + "type_name varchar(100) PRIMARY KEY,\n"
                + "current_level bigint)\n";
        logInfo(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    protected void ensure(UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        if (tableInfo.isEmpty()) {
            createTable(ctx, table, columns);
            return;
        }
        Map<String, Map<String, Object>> fields = CollStreamUtil.toIdentityMap(
                tableInfo, m -> String.valueOf(m.get("column_name")).toLowerCase());
        for (SQLColumn column : columns) {
            String dbColumnName = column.getColumnName().toLowerCase();
            if (!fields.containsKey(dbColumnName)) {
                addColumn(ctx, column);
            }
        }
    }

    protected void createTable(UserContext ctx, String table, List<SQLColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table).append(" (\n");
        sb.append(columns.stream()
                .map(column -> {
                    String dbColumn = column.getColumnName() + " " + column.getType();
                    if (column.isIdColumn()) dbColumn += " PRIMARY KEY";
                    return dbColumn;
                })
                .collect(Collectors.joining(",\n")));
        sb.append(")\n");
        logInfo(sb + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sb.toString()); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    protected void addColumn(UserContext ctx, SQLColumn column) {
        String sql = StrUtil.format("ALTER TABLE {} ADD COLUMN {} {}",
                column.getTableName(), column.getColumnName(), column.getType());
        logInfo(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    public void ensureInitData(UserContext ctx) {
        if (entityDescriptor.isRoot()) ensureRoot(ctx);
        if (entityDescriptor.isConstant()) ensureConstant(ctx);
    }

    private void ensureRoot(UserContext ctx) {
        List<Map<String, Object>> dbRow;
        try {
            dbRow = database.query(ctx,
                    StrUtil.format("SELECT * FROM {} WHERE id = '1'", tableName(entityDescriptor.getType())),
                    new Object[0]);
        } catch (Exception e) {
            dbRow = ListUtil.empty();
        }

        if (!dbRow.isEmpty()) {
            long version = Long.parseLong(String.valueOf(dbRow.get(0).get("version")));
            if (version > 0) return;
            String sql = StrUtil.format("UPDATE {} SET version = {} where id = '1'", tableName(entityDescriptor.getType()), -version);
            logInfo(sql + ";");
            if (ensureTableEnabled(ctx)) {
                try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
            }
            return;
        }

        List<String> columns = new ArrayList<>();
        List<Object> rootRow = new ArrayList<>();
        for (PropertyDescriptor ownProperty : entityDescriptor.getOwnProperties()) {
            columns.add(getSqlColumn(ownProperty).getColumnName());
            rootRow.add(getRootPropertyValue(ctx, ownProperty));
        }
        String sql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})",
                tableName(entityDescriptor.getType()),
                CollectionUtil.join(columns, ","),
                CollectionUtil.join(rootRow, ",", value -> getSqlValue(value)));
        logInfo(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    private void ensureConstant(UserContext ctx) {
        PropertyDescriptor identifier = entityDescriptor.getIdentifier();
        List<String> candidates = identifier.getCandidates();
        List<PropertyDescriptor> ownProperties = entityDescriptor.getOwnProperties();
        List<String> columns = ownProperties.stream()
                .map(p -> getSqlColumn(p).getColumnName())
                .collect(Collectors.toList());

        for (int idx = 0; idx < candidates.size(); idx++) {
            final int i = idx;
            String code = candidates.get(i);
            List<Object> oneConstant = ownProperties.stream()
                    .map(p -> getConstantPropertyValue(ctx, p, i, code))
                    .collect(Collectors.toList());

            try {
                List<Map<String, Object>> existing = database.query(ctx,
                        StrUtil.format("SELECT * FROM {} WHERE id = '{}'",
                                tableName(entityDescriptor.getType()),
                                getConstantPropertyValue(ctx, entityDescriptor.findIdProperty(), i, code)),
                        new Object[0]);
                if (!existing.isEmpty()) {
                    long version = Long.parseLong(String.valueOf(existing.get(0).get("version")));
                    if (version > 0) continue;
                    String sql = StrUtil.format("UPDATE {} SET version = {} where id = '{}'",
                            tableName(entityDescriptor.getType()), -version,
                            getConstantPropertyValue(ctx, entityDescriptor.findIdProperty(), i, code));
                    logInfo(sql + ";");
                    if (ensureTableEnabled(ctx)) {
                        try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
                    }
                    continue;
                }
            } catch (Exception ignored) {
            }

            String sql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})",
                    tableName(entityDescriptor.getType()),
                    CollectionUtil.join(columns, ","),
                    CollectionUtil.join(oneConstant, ",", value -> getSqlValue(value)));
            logInfo(sql + ";");
            if (ensureTableEnabled(ctx)) {
                try { database.execute(ctx, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
            }
        }
    }

    // ==========================================
    // Helper methods
    // ==========================================

    private SQLEntity convertToSQLEntityForInsert(UserContext userContext, T entity) {
        SQLEntity sqlEntity = new SQLEntity();
        sqlEntity.setId(entity.getId());
        sqlEntity.setVersion(entity.getVersion());
        for (PropertyDescriptor pd : this.allProperties) {
            if (pd instanceof Relation && !shouldHandle((Relation) pd)) continue;
            Object v = entity.getProperty(pd.getName());
            List<SQLData> data = convertToSQLData(userContext, entity, pd, v);
            sqlEntity.addPropertySQLData(data);
        }
        for (int i = 0; i < this.types.size() - 1; i++) {
            String tableName = this.primaryTableNames.get(i + 1);
            String type = this.types.get(i);
            SQLData childTypeCell = new SQLData();
            childTypeCell.setTableName(tableName);
            childTypeCell.setColumnName(getChildType());
            childTypeCell.setValue(type);
            sqlEntity.addPropertySQLData(childTypeCell);
        }
        return sqlEntity;
    }

    private SQLEntity convertToSQLEntityForUpdate(UserContext userContext, T entity) {
        List<String> updatedProperties = entity.getUpdatedProperties();
        if (ObjectUtil.isEmpty(updatedProperties)) return null;
        SQLEntity sqlEntity = new SQLEntity();
        sqlEntity.setId(entity.getId());
        sqlEntity.setVersion(entity.getVersion());
        for (String updatedProperty : updatedProperties) {
            PropertyDescriptor property = findProperty(updatedProperty);
            if (property.isId() || property.isVersion()) continue;
            Object v = entity.getProperty(property.getName());
            List<SQLData> data = convertToSQLData(userContext, entity, property, v);
            sqlEntity.addPropertySQLData(data);
        }
        return sqlEntity;
    }

    private List<SQLData> convertToSQLData(UserContext ctx, T entity, PropertyDescriptor property, Object value) {
        return io.teaql.core.sql.portable.SQLPropertyUtil.toDBRaw(ctx, entity, value, property);
    }

    private boolean shouldHandle(PropertyDescriptor pProperty) {
        if (pProperty instanceof Relation) return shouldHandle((Relation) pProperty);
        return true;
    }

    public boolean shouldHandle(Relation relation) {
        return relation.getRelationKeeper() == this.entityDescriptor;
    }

    private void initSQLMeta(EntityDescriptor entityDescriptor) {
        this.sqlMetadata = new io.teaql.core.sql.SqlEntityMetadata(entityDescriptor);
        EntityDescriptor descriptor = entityDescriptor;
        while (descriptor != null) {
            types.add(descriptor.getType());
            for (PropertyDescriptor property : descriptor.getProperties()) {
                allProperties.add(property);
                if (property instanceof Relation && !shouldHandle((Relation) property)) continue;
                List<SQLColumn> sqlColumns = getSqlColumns(property);
                if (ObjectUtil.isEmpty(sqlColumns)) {
                    throw new TeaQLRuntimeException("property :" + property.getName() + " miss sql table columns");
                }
                String firstTable = sqlColumns.get(0).getTableName();
                if (property.isVersion()) this.versionTableName = firstTable;
                if (property.isId()) {
                    if (!this.primaryTableNames.contains(firstTable)) this.primaryTableNames.add(firstTable);
                    if (property.getOwner() == this.entityDescriptor) this.thisPrimaryTableName = firstTable;
                }
                this.allTableNames.addAll(CollStreamUtil.toList(sqlColumns, SQLColumn::getTableName));
            }
            descriptor = descriptor.getParent();
        }
        this.auxiliaryTableNames = new ArrayList<>(CollectionUtil.subtract(this.allTableNames, this.primaryTableNames));
    }

    public PropertyDescriptor findProperty(String propertyName) {
        for (PropertyDescriptor pd : allProperties) {
            if (pd.getName().equals(propertyName)) return pd;
        }
        throw new TeaQLRuntimeException("Property not found: " + propertyName);
    }

    private List<SQLColumn> getSqlColumns(PropertyDescriptor property) {
        return io.teaql.core.sql.portable.SQLPropertyUtil.getColumns(property);
    }

    public SQLColumn getSqlColumn(PropertyDescriptor property) {
        return CollectionUtil.getFirst(getSqlColumns(property));
    }

    public String tableName(String type) {
        return NamingCase.toUnderlineCase(type + "_data");
    }

    private String tableAlias(String table) {
        return NamingCase.toCamelCase(table);
    }

    protected String getSqlValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number) return String.valueOf(value);
        if (value instanceof Boolean) return ((Boolean) value) ? "1" : "0";
        return StrUtil.wrapIfMissing(String.valueOf(value), "'", "'");
    }

    private Object getRootPropertyValue(UserContext ctx, PropertyDescriptor property) {
        if (property.isId()) return 1L;
        if (property.isVersion()) return 1L;
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) return ctx.evaluate(createFunction);
        return property.getAdditionalInfo().get("candidates");
    }

    private Object getConstantPropertyValue(UserContext ctx, PropertyDescriptor property, int index, String identifier) {
        if (property.isVersion()) return 1L;
        PropertyType type = property.getType();
        if (BaseEntity.class.isAssignableFrom(type.javaType())) return "1";
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) return ctx.evaluate(createFunction);
        List<String> candidates = property.getCandidates();
        if (property.isIdentifier()) return identifier;
        if (ObjectUtil.isNotEmpty(candidates)) return CollectionUtil.get(candidates, index);
        if (property.isId()) return Math.abs(identifier.toUpperCase().hashCode());
        return null;
    }

    private long genIdForCandidateCode(String code) {
        return Math.abs(code.toUpperCase().hashCode());
    }

    // ==========================================
    // SQL building helpers
    // ==========================================

    private void ensureOrderByForPartition(SearchRequest<T> request) {
        OrderBys orderBy = request.getOrderBy();
        if (orderBy.isEmpty()) orderBy.addOrderBy(new OrderBy("id"));
    }

        public List<SQLColumn> getPropertyColumns(String idTable, String propertyName) {
        if (getChildType().equalsIgnoreCase(propertyName)) {
            if (entityDescriptor.hasChildren()) {
                SQLColumn sqlColumn = new SQLColumn(tableAlias(thisPrimaryTableName), getChildType());
                sqlColumn.setType(getChildSqlType());
                return ListUtil.of(sqlColumn);
            }
            return ListUtil.empty();
        }
        PropertyDescriptor property = findProperty(propertyName);
        List<SQLColumn> sqlColumns = getSqlColumns(property);
        for (SQLColumn sqlColumn : sqlColumns) {
            if (property.isId()) sqlColumn.setTableName(tableAlias(idTable));
            else sqlColumn.setTableName(tableAlias(sqlColumn.getTableName()));
        }
        return sqlColumns;
    }

    public String prepareLimit(SearchRequest request) {
        return prepareLimit(request, new java.util.HashMap<>());
    }

    @Override
    public String prepareLimit(SearchRequest request, java.util.Map<String, Object> parameters) {
        Slice slice = request.getSlice();
        if (ObjectUtil.isEmpty(slice)) return null;
        
        String limitKey = "limit0";
        while (parameters.containsKey(limitKey)) limitKey += "_1";
        parameters.put(limitKey, slice.getSize());
        
        String offsetKey = "offset0";
        while (parameters.containsKey(offsetKey)) offsetKey += "_1";
        parameters.put(offsetKey, slice.getOffset());
        
        if (dialect instanceof io.teaql.core.sql.dialect.OracleDialect) {
            return StrUtil.format("OFFSET :{} ROWS FETCH NEXT :{} ROWS ONLY", offsetKey, limitKey);
        }
        return StrUtil.format("LIMIT :{} OFFSET :{}", limitKey, offsetKey);
    }

    public String getTypeSQL(UserContext userContext) {
        if (!getEntityDescriptor().hasChildren()) return null;
        if (userContext.getBool(MULTI_TABLE, false)) {
            return StrUtil.format("{}.{} AS {}", tableAlias(thisPrimaryTableName), getChildType(), TYPE_ALIAS);
        }
        return StrUtil.format("{} AS {}", getChildType(), TYPE_ALIAS);
    }

    public String getPartitionSQL() {
        return dialect.getPartitionSQL();
    }

    // ==========================================
    // Aggregation queries
    // ==========================================

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
            List<Map<String, Object>> rows = database.query(userContext, psql.sql, psql.args);

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
    }

    // ==========================================
    // Stream support
    // ==========================================

        public Stream<T> executeForStream(UserContext userContext, SearchRequest<T> request, int enhanceBatch) {
        return loadInternal(userContext, request).stream();
    }

    // ==========================================
    // Getter/Setter
    // ==========================================

    public String getChildType() { return childType; }
    public void setChildType(String pChildType) { childType = pChildType; }
    public String getChildSqlType() { return childSqlType; }
    public void setChildSqlType(String pChildSqlType) { childSqlType = pChildSqlType; }
    public String getTqlIdSpaceTable() { return tqlIdSpaceTable; }
    public void setTqlIdSpaceTable(String pTqlIdSpaceTable) { tqlIdSpaceTable = pTqlIdSpaceTable; }
    public TeaQLDatabase getDatabase() { return database; }

    protected boolean ensureTableEnabled(UserContext ctx) {
        return ctx.getBool("ensureTable", true);
    }

    private void logInfo(String message) {
        System.out.println("[SQL-PORTABLE] " + message);
    }
}
