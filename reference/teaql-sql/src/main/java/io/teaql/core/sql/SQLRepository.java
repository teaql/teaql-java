package io.teaql.core.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.MapUtil;
import io.teaql.core.utils.NamingCase;
import io.teaql.core.utils.ClassUtil;
import io.teaql.core.utils.NumberUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.ReflectUtil;
import io.teaql.core.utils.StrUtil;

import static io.teaql.core.log.Markers.SQL_SELECT;
import static io.teaql.core.log.Markers.SQL_UPDATE;

import io.teaql.core.AggregationItem;
import io.teaql.core.AggregationResult;
import io.teaql.core.Aggregations;
import io.teaql.core.BaseEntity;
import io.teaql.core.ConcurrentModifyException;
import io.teaql.core.Entity;
import io.teaql.core.EntityStatus;
import io.teaql.core.Expression;
import io.teaql.core.OrderBy;
import io.teaql.core.OrderBys;
import io.teaql.core.Repository;
import io.teaql.core.RepositoryException;
import io.teaql.core.SearchCriteria;
import io.teaql.core.SearchRequest;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.Slice;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.log.Markers;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.PropertyType;
import io.teaql.core.meta.Relation;
import io.teaql.core.repository.AbstractRepository;
import io.teaql.core.repository.StreamEnhancer;
import io.teaql.core.sql.expression.ExpressionHelper;
import io.teaql.core.sql.expression.SQLExpressionParser;

public class SQLRepository<T extends Entity> extends AbstractRepository<T>
        implements SqlCompilerDelegate {
    public static final String TYPE_ALIAS = "_type_";
    public static final String IGNORE_SUBTYPES = "IGNORE_SUBTYPES";
    public static final String MULTI_TABLE = "MULTI_TABLE";
    private final EntityDescriptor entityDescriptor;
    private final DataSource dataSource;
    private final NamedParameterJdbcTemplate jdbcTemplate;
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
    private SqlEntityMetadata sqlMetadata;
    private io.teaql.core.sql.dialect.SqlDialect dialect = new io.teaql.core.sql.dialect.PostgreSqlDialect();

    public io.teaql.core.sql.dialect.SqlDialect getDialect() {
        return dialect;
    }

    public void setDialect(io.teaql.core.sql.dialect.SqlDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return dialect.escapeIdentifier(identifier);
    }

    public SQLRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        this.entityDescriptor = entityDescriptor;
        this.dataSource = TracedDataSource.wrap(dataSource);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);
        initSQLMeta(entityDescriptor);
        initExpressionParsers(entityDescriptor, dataSource);
    }

    protected void executeUpdate(UserContext ctx, String sql){
        try {
            ctx.info("executeUpdate: {}" ,sql);

            jdbcTemplate.getJdbcTemplate().execute(sql);
        }
        catch (DataAccessException pE) {
            ctx.error("Error when executeUpdate: {} ",sql);
            throw new RepositoryException(pE);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    protected void initExpressionParsers(EntityDescriptor entityDescriptor, DataSource dataSource) {
        Set<Class<?>> parsers =
                ClassUtil.scanPackageBySuper(
                        ExpressionHelper.class.getPackageName(), SQLExpressionParser.class);
        for (Class<?> parser : parsers) {
            if (!ClassUtil.isInterface(parser) && !ClassUtil.isAbstract(parser)) {
                SQLExpressionParser o = (SQLExpressionParser) ReflectUtil.newInstance(parser);
                registerExpressionParser(o);
            }
        }
    }

    public void registerExpressionParser(SQLExpressionParser sqlExpressionParser) {
        if (sqlExpressionParser == null) {
            return;
        }
        Class type = sqlExpressionParser.type();
        if (type != null) {
            expressionParsers.put(type, sqlExpressionParser);
        }
    }

    public void registerExpressionParser(Class<? extends SQLExpressionParser> parser) {
        if (!ClassUtil.isInterface(parser) && !ClassUtil.isAbstract(parser)) {
            SQLExpressionParser o = ReflectUtil.newInstance(parser);
            registerExpressionParser(o);
        }
    }

    private void initSQLMeta(EntityDescriptor entityDescriptor) {
        this.sqlMetadata = new SqlEntityMetadata(entityDescriptor);
        EntityDescriptor descriptor = entityDescriptor;
        while (descriptor != null) {
            types.add(descriptor.getType());
            List<PropertyDescriptor> properties = descriptor.getProperties();
            for (PropertyDescriptor property : properties) {
                allProperties.add(property);
                if (property instanceof Relation && !shouldHandle((Relation) property)) {
                    continue;
                }
                List<SQLColumn> sqlColumns = getSqlColumns(property);
                if (ObjectUtil.isEmpty(sqlColumns)) {
                    throw new RepositoryException(
                            "property :" + property.getName() + " miss sql table columns");
                }

                String firstTable = sqlColumns.get(0).getTableName();
                if (property.isVersion()) {
                    this.versionTableName = firstTable;
                }
                if (property.isId()) {
                    if (!this.primaryTableNames.contains(firstTable)) {
                        this.primaryTableNames.add(firstTable);
                    }
                    if (property.getOwner() == this.entityDescriptor) {
                        this.thisPrimaryTableName = firstTable;
                    }
                }
                this.allTableNames.addAll(CollStreamUtil.toList(sqlColumns, SQLColumn::getTableName));
            }
            descriptor = descriptor.getParent();
        }
        this.auxiliaryTableNames =
                new ArrayList<>(CollectionUtil.subtract(this.allTableNames, this.primaryTableNames));
    }

    @Override
    public EntityDescriptor getEntityDescriptor() {
        return this.entityDescriptor;
    }

    public void updateInternal(UserContext userContext, Collection<T> updateItems) {
        if (ObjectUtil.isEmpty(updateItems)) {
            return;
        }
        List<SQLEntity> sqlEntities =
                CollectionUtil.map(updateItems, i -> convertToSQLEntityForUpdate(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) {
            return;
        }
        for (SQLEntity sqlEntity : sqlEntities) {
            if (sqlEntity.isEmpty()) {
                continue;
            }
            Map<String, List<String>> tableColumnNames = sqlEntity.getTableColumnNames();
            Map<String, List> tableColumnValues = sqlEntity.getTableColumnValues();

            // versionTableUpdated flag
            AtomicBoolean versionTableUpdated = new AtomicBoolean(false);
            tableColumnValues.forEach(
                    (k, v) -> {
                        List<String> columns = new ArrayList<>(tableColumnNames.get(k));

                        List l = new ArrayList(v);

                        boolean versionTable = this.versionTableName.equals(k);
                        boolean primaryTable = this.primaryTableNames.contains(k);
                        if (versionTable) {
                            updateVersionTable(userContext, sqlEntity, versionTableUpdated, k, columns, l);
                        }
                        else if (primaryTable) {
                            updatePrimaryTable(userContext, sqlEntity, k, columns, l);
                        }
                        else {
                            try {
                                jdbcTemplate
                                        .getJdbcTemplate()
                                        .update(prepareSubsidiaryTableSql(k, columns), l.toArray(new Object[0]));
                            }
                            catch (DataAccessException pE) {
                                throw new RepositoryException(pE);
                            }
                        }
                    });

            // if we don't update version table yet, then update version in version table
            if (!versionTableUpdated.get()) {
                updateVersionTableVersion(userContext, sqlEntity);
            }
        }
    }

    public String prepareSubsidiaryTableSql(String tableName, List<String> tableColumns) {
        return dialect.buildSubsidiaryInsertSql(tableName, tableColumns);
    }

    private void updateVersionTableVersion(UserContext userContext, SQLEntity sqlEntity) {
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionTableVersionSQL(this, this.versionTableName);
        Object[] parameters = {sqlEntity.getVersion() + 1, sqlEntity.getId(), sqlEntity.getVersion()};
        int update;
        try {
            update = jdbcTemplate.getJdbcTemplate().update(updateSql, parameters);
        }
        catch (DataAccessException pE) {
            throw new RepositoryException(pE);
        }
        SQLLogger.logSQLAndParameters(
                Markers.SQL_UPDATE, userContext, updateSql, parameters, update + " UPDATED");
        if (update != 1) {
            throw new ConcurrentModifyException();
        }
    }

    private void updatePrimaryTable(
            UserContext userContext, SQLEntity sqlEntity, String k, List<String> columns, List l) {
        l.add(sqlEntity.getId());
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildUpdatePrimarySQL(this, k, columns, sqlEntity.getTraceChain());
        Object[] parameters = l.toArray(new Object[0]);
        int update;
        try {
            update = jdbcTemplate.getJdbcTemplate().update(updateSql, parameters);
        }
        catch (DataAccessException pE) {
            throw new RepositoryException(pE);
        }
        SQLLogger.logSQLAndParameters(
                SQL_UPDATE, userContext, updateSql, parameters, update + " UPDATED");
        if (update != 1) {
            throw new RepositoryException("primary table update failed");
        }
    }

    private void updateVersionTable(
            UserContext userContext,
            SQLEntity sqlEntity,
            AtomicBoolean versionTableUpdated,
            String k,
            List<String> columns,
            List l) {
        // version table updated
        versionTableUpdated.set(true);
        // version column updated
        columns.add(VERSION);
        l.add(sqlEntity.getVersion() + 1); // version +1
        l.add(sqlEntity.getId());
        l.add(sqlEntity.getVersion());
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionSQL(this, k, columns, sqlEntity.getTraceChain());
        Object[] parameters = l.toArray(new Object[0]);
        int update;
        try {
            update = jdbcTemplate.getJdbcTemplate().update(updateSql, parameters);
        }
        catch (DataAccessException pE) {
            throw new RepositoryException(pE);
        }
        SQLLogger.logSQLAndParameters(
                SQL_UPDATE, userContext, updateSql, parameters, update + " UPDATED");
        if (update != 1) {
            throw new ConcurrentModifyException();
        }
    }

    private SQLEntity convertToSQLEntityForUpdate(UserContext userContext, T entity) {
        // update the updated properties only
        List<String> updatedProperties = entity.getUpdatedProperties();
        if (ObjectUtil.isEmpty(updatedProperties)) {
            return null;
        }
        SQLEntity sqlEntity = new SQLEntity();
        sqlEntity.setId(entity.getId());
        sqlEntity.setVersion(entity.getVersion());
        sqlEntity.setTraceChain(entity.getTraceChain());
        for (String updatedProperty : updatedProperties) {
            PropertyDescriptor property = findProperty(updatedProperty);
            // id ,version are maintained by the framework
            if (property.isId() || property.isVersion()) {
                continue;
            }
            Object v = entity.getProperty(property.getName());
            List<SQLData> data = convertToSQLData(userContext, entity, property, v);
            sqlEntity.addPropertySQLData(data);
        }
        return sqlEntity;
    }

    public void createInternal(UserContext userContext, Collection<T> createItems) {
        List<SQLEntity> sqlEntities =
                CollectionUtil.map(createItems, i -> convertToSQLEntityForInsert(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) {
            return;
        }

        SQLEntity sqlEntity = sqlEntities.get(0);

        // collect table/columns for the first entity(all entities with the same structure)
        Map<String, List<String>> tableColumns = sqlEntity.getTableColumnNames();

        // collect all rows for entities, we will insert them in the batch
        Map<String, List<Object[]>> rows = new HashMap<>();
        for (SQLEntity entity : sqlEntities) {
            Map<String, List> tableColumnValues = entity.getTableColumnValues();
            for (Map.Entry<String, List> entry : tableColumnValues.entrySet()) {
                String k = entry.getKey();
                List v = entry.getValue();
                List<Object[]> values = rows.get(k);
                if (values == null) {
                    values = new ArrayList<>();
                    rows.put(k, values);
                }
                // for auxiliary tables, we only save the row if there is values except id
                if (auxiliaryTableNames.contains(k) && entity.allNullExceptID(v)) {
                    continue;
                }
                values.add(v.toArray(new Object[0]));
            }
        }

        // sort tables, we will insert version table first.
        TreeMap<String, List<Object[]>> sorted =
                MapUtil.sort(
                        rows,
                        (table1, table2) -> {
                            if (table1.equals(versionTableName)) {
                                return -1;
                            }
                            if (table2.equals(versionTableName)) {
                                return 1;
                            }
                            return 0;
                        });
        sorted.forEach(
                (k, v) -> {
                    if (v.isEmpty()) {
                        return;
                    }
                    List<String> columns = tableColumns.get(k);
                    SqlAstCompiler compiler = new SqlAstCompiler();
                    String sql = compiler.buildInsertSQL(this, k, columns, sqlEntity.getTraceChain());
                    int[] rets;
                    try {
                        rets = jdbcTemplate.getJdbcTemplate().batchUpdate(sql, v);
                    }
                    catch (DataAccessException pE) {
                        throw new RepositoryException(pE);
                    }
                    int i = 0;
                    for (int ret : rets) {
                        SQLLogger.logSQLAndParameters(
                                SQL_UPDATE, userContext, sql, v.get(i++), ret + " UPDATED");
                    }
                });
    }

    private SQLEntity convertToSQLEntityForInsert(UserContext userContext, T entity) {
        System.out.println("CONVERTING TO SQL ENTITY: " + entity.typeName() + " with status " + ((BaseEntity)entity).get$status());
        SQLEntity sqlEntity = new SQLEntity();
        sqlEntity.setId(entity.getId());
        sqlEntity.setVersion(entity.getVersion());
        sqlEntity.setTraceChain(entity.getTraceChain());

        for (PropertyDescriptor propertyDescriptor : this.allProperties) {
            if (propertyDescriptor instanceof Relation) {
                if (!shouldHandle((Relation) propertyDescriptor)) {
                    continue;
                }
            }
            Object v = entity.getProperty(propertyDescriptor.getName());
            System.out.println("Extracting property " + propertyDescriptor.getName() + " from " + entity.typeName() + ", got: " + (v == null ? "null" : v.getClass().getSimpleName() + " with id " + (v instanceof Entity ? ((Entity)v).getId() : v)));
            List<SQLData> data = convertToSQLData(userContext, entity, propertyDescriptor, v);
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

    private List<SQLData> convertToSQLData(
            UserContext ctx, T entity, PropertyDescriptor property, Object propertyValue) {
        if (property instanceof SQLProperty) {
            return ((SQLProperty) property).toDBRaw(ctx, entity, propertyValue);
        }
        throw new RepositoryException("SQLRepository only support SQLProperty");
    }

    private Object toSQLValue(Entity entity, PropertyDescriptor property) {
        return entity.getProperty(property.getName());
    }

    private Object toDBValue(Object property, PropertyDescriptor pProperty) {
        return pProperty;
    }

    @Override
    public void deleteInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) {
            return;
        }
        List<Object[]> args =
                entities.stream()
                        .filter(e -> e.getVersion() > 0)
                        .map(e -> new Object[] {-(e.getVersion() + 1), e.getId(), e.getVersion()})
                        .collect(Collectors.toList());
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        int[] rets;
        try {
            rets = jdbcTemplate.getJdbcTemplate().batchUpdate(updateSql, args);
        }
        catch (DataAccessException pE) {
            throw new RepositoryException(pE);
        }
        int i = 0;
        for (int ret : rets) {
            SQLLogger.logSQLAndParameters(
                    SQL_UPDATE, userContext, updateSql, args.get(i++), ret + " UPDATED");
            if (ret != 1) {
                throw new ConcurrentModifyException();
            }
        }
    }

    @Override
    public void recoverInternal(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isEmpty(entities)) {
            return;
        }
        List<Object[]> args =
                entities.stream()
                        .filter(e -> e.getVersion() < 0)
                        .map(
                                e ->
                                        new Object[] {
                                                // delete the version
                                                (-e.getVersion() + 1), e.getId(), e.getVersion()
                                        })
                        .collect(Collectors.toList());
        SqlAstCompiler compiler = new SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        int[] rets;
        try {
            rets = jdbcTemplate.getJdbcTemplate().batchUpdate(updateSql, args);
        }
        catch (DataAccessException pE) {
            throw new RepositoryException(pE);
        }
        int i = 0;
        for (int ret : rets) {
            SQLLogger.logSQLAndParameters(
                    SQL_UPDATE, userContext, updateSql, args.get(i++), ret + " UPDATED");
            if (ret != 1) {
                throw new ConcurrentModifyException();
            }
        }
    }

    public SQLColumn getSqlColumn(PropertyDescriptor property) {
        List<SQLColumn> sqlColumns = getSqlColumns(property);
        SQLColumn sqlColumn = CollectionUtil.getFirst(sqlColumns);
        return sqlColumn;
    }

    private List<SQLColumn> getSqlColumns(PropertyDescriptor property) {
        if (property instanceof SQLProperty) {
            return ((SQLProperty) property).columns();
        }
        throw new RepositoryException("SQLRepository only support SQLProperty");
    }

    public SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {
        Map<String, Object> params = new HashMap<>();
        String sql = buildDataSQL(userContext, request, params);


        List<T> results = new ArrayList<>();
        if (!ObjectUtil.isEmpty(sql)) {
            try {
                processParametersForLoadInternal(userContext,params);
                results = jdbcTemplate.query(sql, params, getMapper(userContext, request));
            }
            catch (DataAccessException pE) {
                throw new RepositoryException(pE);
            }
            SQLLogger.logNamedSQL(SQL_SELECT, userContext, sql, params, results);
        }
        SmartList<T> smartList = new SmartList<>(results);
        return smartList;
    }

    protected void processParametersForLoadInternal(UserContext userContext,Map<String, Object> params) {
        //do nothing here
    }


    @Override
    public Stream<T> executeForStream(
            UserContext userContext, SearchRequest<T> request, int enhanceBatch) {
        Map<String, Object> params = new HashMap<>();
        String sql = buildDataSQL(userContext, request, params);
        if (ObjectUtil.isEmpty(sql)) {
            return Stream.empty();
        }
        Stream<T> stream = jdbcTemplate.queryForStream(sql, params, getMapper(userContext, request));
        return (Stream<T>)
                StreamSupport.stream(
                                new StreamEnhancer(userContext, this, stream, request, enhanceBatch), false)
                        .map(
                                item -> {
                                    if (userContext instanceof DefaultUserContext) {
                                        ((DefaultUserContext) userContext).afterLoad(getEntityDescriptor(), (Entity) item);
                                    }
                                    return item;
                                })
                        .onClose(stream::close);
    }

    protected AggregationResult doAggregateInternal(
            UserContext userContext, SearchRequest<T> request) {
        if (!request.hasSimpleAgg()) {
            return null;
        }
        
        SqlAstCompiler compiler = new SqlAstCompiler();
        List<String> tables = compiler.collectAggregationTables(sqlMetadata, this, userContext, request);
        Map<String, Object> parameters = new HashMap<>();
        
        Object preConfig = userContext.getObj(MULTI_TABLE);
        userContext.put(MULTI_TABLE, tables.size() > 1);

        try {
            String sql = compiler.buildAggregationSQL(sqlMetadata, this, userContext, request, parameters, tables);
            
            if (sql == null) {
                return null;
            }

            List<AggregationItem> aggregationItems;
            try {
                processParametersForLoadInternal(userContext, parameters);
                aggregationItems = jdbcTemplate.query(sql, parameters, getAggregationMapper(request));
            }
            catch (DataAccessException pE) {
                throw new RepositoryException(pE);
            }
            AggregationResult result = new AggregationResult();
            result.setName(request.getAggregations().getName());
            result.setData(aggregationItems);
            SQLLogger.logNamedSQL(SQL_SELECT, userContext, sql, parameters, result);
            return result;
        }
        finally {
            userContext.put(MULTI_TABLE, preConfig);
        }
    }

    private RowMapper<AggregationItem> getAggregationMapper(SearchRequest<T> request) {
        return (rs, index) -> {
            AggregationItem item = new AggregationItem();
            Aggregations aggregations = request.getAggregations();
            List<SimpleNamedExpression> functions = aggregations.getAggregates();
            List<SimpleNamedExpression> dimensions = aggregations.getDimensions();
            for (SimpleNamedExpression function : functions) {

                item.addValue(function, ResultSetTool.getValue(rs, function.name()));
            }
            for (SimpleNamedExpression dimension : dimensions) {

                item.addDimension(dimension, ResultSetTool.getValue(rs, dimension.name()));
            }
            return item;
        };
    }

    private String collectAggregationGroupBySql(
            UserContext userContext,
            SearchRequest<T> request,
            String idTable,
            Map<String, Object> parameters) {
        List<SimpleNamedExpression> dimensions = request.getAggregations().getDimensions();
        if (dimensions.isEmpty()) {
            return null;
        }
        return dimensions.stream()
                .map(
                        dimension -> {
                            Expression expression = dimension.getExpression();
                            while (expression instanceof SimpleNamedExpression) {
                                expression = dimension.getExpression();
                            }
                            return expression;
                        })
                .map(
                        expression ->
                                ExpressionHelper.toSql(userContext, expression, idTable, parameters, this))
                .collect(Collectors.joining(",", "GROUP BY ", ""));
    }

    private String collectAggregationSelectSql(
            UserContext userContext,
            SearchRequest<T> request,
            String idTable,
            Map<String, Object> params) {
        List<SimpleNamedExpression> allSelected = request.getAggregations().getSelectedExpressions();
        return allSelected.stream()
                .map(expression -> ExpressionHelper.toSql(userContext, expression, idTable, params, this))
                .collect(Collectors.joining(","));
    }

    private List<String> collectAggregationTables(UserContext userContext, SearchRequest<T> request) {
        return collectTablesFromProperties(userContext, request.aggregationProperties(userContext));
    }

    private RowMapper<T> getMapper(UserContext pUserContext, SearchRequest<T> pRequest) {
        return (rs, rowIndex) -> {
            Class<? extends T> returnType = pRequest.returnType();
            T entity = ReflectUtil.newInstance(returnType);
            for (PropertyDescriptor property : this.allProperties) {
                setProperty(pUserContext, entity, property, rs);
            }
            setRealType(entity, rs);
            List<SimpleNamedExpression> simpleDynamicProperties = pRequest.getSimpleDynamicProperties();
            for (SimpleNamedExpression simpleDynamicProperty : simpleDynamicProperties) {
                String name = simpleDynamicProperty.name();
                entity.addDynamicProperty(name, ResultSetTool.getValue(rs, name));
            }

            if (entity.getVersion() != null && entity.getVersion() < 0) {
                if (entity instanceof BaseEntity) {
                    ((BaseEntity) entity).set$status(EntityStatus.PERSISTED_DELETED);
                }
            }
            else {
                if (entity instanceof BaseEntity) {
                    ((BaseEntity) entity).set$status(EntityStatus.PERSISTED);
                }
            }
            return entity;
        };
    }

    private void setRealType(T entity, ResultSet rs) {
        try {
            entity.setRuntimeType(rs.getString(TYPE_ALIAS));
        }
        catch (SQLException pE) {
        }
    }

    private void setProperty(
            UserContext userContext, T pEntity, PropertyDescriptor pProperty, ResultSet resultSet) {
        if (!shouldHandle(pProperty)) {
            return;
        }

        if (pProperty instanceof SQLProperty) {
            ((SQLProperty) pProperty).setPropertyValue(userContext, pEntity, resultSet);
            return;
        }
        throw new RepositoryException(
                "SQLRepository property[" + pProperty.getName() + "]error，only support SQLProperty");
    }

    private boolean shouldHandle(PropertyDescriptor pProperty) {
        if (pProperty instanceof Relation) {
            return shouldHandle((Relation) pProperty);
        }
        return true;
    }

    public String getPartitionSQL() {
        return dialect.getPartitionSQL();
    }

    public String buildDataSQL(
            UserContext userContext, SearchRequest request, Map<String, Object> parameters) {

        //if rawSql is provided, we will not build Data SQL
        String rawSql = (String) request.getExtension("rawSql");
        if (ObjectUtil.isNotEmpty(rawSql)) {
            return rawSql;
        }

        String partitionProperty = request.getPartitionProperty();
        if (ObjectUtil.isNotEmpty(partitionProperty) && request.getSlice() != null) {
            ensureOrderByForPartition(request);
        }

        SqlAstCompiler compiler = new SqlAstCompiler();
        return compiler.buildDataSQL(sqlMetadata, this, userContext, request, parameters);
    }

    private void ensureOrderByForPartition(SearchRequest<T> request) {
        OrderBys orderBy = request.getOrderBy();
        if (orderBy.isEmpty()) {
            orderBy.addOrderBy(new OrderBy(ID));
        }
    }

    public String joinTables(UserContext userContext, List<String> tables) {
        List<String> sortedTables = new ArrayList<>();
        for (String table : tables) {
            if (primaryTableNames.contains(table)) {
                sortedTables.add(table);
            }
        }
        for (String table : tables) {
            if (!primaryTableNames.contains(table)) {
                sortedTables.add(table);
            }
        }

        if (!userContext.getBool(MULTI_TABLE, false)) {
            return StrUtil.format("{}", sortedTables.get(0));
        }

        StringBuilder sb = new StringBuilder();
        String preTable = null;
        for (String sortedTable : sortedTables) {
            if (preTable == null) {
                preTable = sortedTable;
                sb.append(StrUtil.format("{} AS {}", sortedTable, tableAlias(sortedTable)));
                continue;
            }
            sb.append(
                    StrUtil.format(
                            " {} JOIN {} AS {} ON {}.{} = {}.{}",
                            primaryTableNames.contains(sortedTable) ? "INNER" : "LEFT",
                            sortedTable,
                            tableAlias(sortedTable),
                            tableAlias(sortedTable),
                            ID,
                            tableAlias(preTable),
                            ID));
        }
        return sb.toString();
    }

    private String collectSelectSql(
            UserContext userContext,
            SearchRequest request,
            String idTable,
            Map<String, Object> pParameters) {
        List<SimpleNamedExpression> allSelects = new ArrayList<>();
        List<SimpleNamedExpression> projections = request.getProjections();
        if (projections != null) {
            allSelects.addAll(projections);
        }
        List<SimpleNamedExpression> simpleDynamicProperties = request.getSimpleDynamicProperties();
        if (simpleDynamicProperties != null) {
            allSelects.addAll(simpleDynamicProperties);
        }
        String selects =
                allSelects.stream()
                        .map(e -> ExpressionHelper.toSql(userContext, e, idTable, pParameters, this))
                        .collect(Collectors.joining(", "));

        if (!userContext.getBool(IGNORE_SUBTYPES, false)) {
            String typeSQL = getTypeSQL(userContext);
            if (ObjectUtil.isNotEmpty(typeSQL)) {
                selects = selects + ", " + typeSQL;
            }
        }
        return selects;
    }

    public String getTypeSQL(UserContext userContext) {
        String typeSQL = null;
        if (getEntityDescriptor().hasChildren()) {
            typeSQL = StrUtil.format("{} AS {}", getChildType(), TYPE_ALIAS);
            if (userContext.getBool(MULTI_TABLE, false)) {
                typeSQL =
                        StrUtil.format(
                                "{}.{} AS {}", tableAlias(thisPrimaryTableName), getChildType(), TYPE_ALIAS);
            }
        }
        return typeSQL;
    }

    private List<String> collectDataTables(UserContext userContext, SearchRequest<T> request) {
        List<String> allRelationProperties = request.dataProperties(userContext);
        return collectTablesFromProperties(userContext, allRelationProperties);
    }

    private ArrayList<String> collectTablesFromProperties(
            UserContext userContext, List<String> properties) {
        Set<String> tables = new HashSet<>();
        for (String target : properties) {
            PropertyDescriptor property = findProperty(target);
            if (property.isId()) {
                continue;
            }
            List<SQLColumn> sqlColumns = getSqlColumns(property);
            for (SQLColumn sqlColumn : sqlColumns) {
                tables.add(sqlColumn.getTableName());
            }
        }
        // ensure this primary table to ensure type
        tables.add(thisPrimaryTableName);
        return ListUtil.toList(tables);
    }

    private String tableAlias(String table) {
        return NamingCase.toCamelCase(table);
    }

    public String prepareLimit(SearchRequest request) {
        return dialect.prepareLimit(request);
    }

    private String prepareOrderBy(
            UserContext userContext,
            SearchRequest request,
            String idTable,
            Map<String, Object> parameters) {
        OrderBys orderBys = request.getOrderBy();

        if (ObjectUtil.isEmpty(orderBys)) {
            return null;
        }
        return ExpressionHelper.toSql(userContext, orderBys, idTable, parameters, this);
    }

    private String prepareCondition(
            UserContext userContext,
            String idTable,
            SearchCriteria searchCriteria,
            Map<String, Object> parameters) {
        if (ObjectUtil.isEmpty(searchCriteria)) {
            return SearchCriteria.TRUE;
        }
        return ExpressionHelper.toSql(userContext, searchCriteria, idTable, parameters, this);
    }

    public boolean hasSameDataSource(UserContext pUserContext, Repository repository) {
        if (repository instanceof SQLRepository) {
            return this.dataSource == ((SQLRepository<?>) repository).dataSource;
        }
        return false;
    }

    public String tableName(String type) {
        return NamingCase.toUnderlineCase(type + "_data");
    }

    public void ensureSchema(UserContext ctx) {
        List<SQLColumn> allColumns = new ArrayList<>();
        List<PropertyDescriptor> ownProperties = entityDescriptor.getOwnProperties();
        for (PropertyDescriptor ownProperty : ownProperties) {
            List<SQLColumn> sqlColumns = getSqlColumns(ownProperty);
            allColumns.addAll(sqlColumns);
        }
        if (entityDescriptor.hasChildren()) {
            SQLColumn childTypeCell = new SQLColumn(thisPrimaryTableName, getChildType());
            childTypeCell.setType(getChildSqlType());
            allColumns.add(childTypeCell);
        }
        Map<String, List<SQLColumn>> tableColumns =
                CollStreamUtil.groupByKey(allColumns, SQLColumn::getTableName);
        tableColumns.forEach(
                (table, columns) -> {
                    String sql = findTableColumnsSql(dataSource, table);
                    List<Map<String, Object>> dbTableInfo;
                    try {
                        dbTableInfo = jdbcTemplate.queryForList(sql, Collections.emptyMap());
                    }
                    catch (Exception exception) {
                        dbTableInfo = ListUtil.empty();
                    }
                    ensure(ctx, dbTableInfo, table, columns);
                });
        ensureInitData(ctx);
        ensureIndexAndForeignKey(ctx);
        ensureIdSpaceTable(ctx);
    }

    protected String findTableColumnsSql(DataSource dataSource, String table) {
        return String.format("select * from information_schema.columns where table_name = '%s'", table);
    }
    protected List<Map<String, Object>> queryForList(String sql, Map<?,?> map){

        return jdbcTemplate.queryForList(sql, Collections.emptyMap());

    }
    protected void ensureIdSpaceTable(UserContext ctx) {
        String sql = findIdSpaceTableSql();
        List<Map<String, Object>> dbTableInfo;
        try {
            dbTableInfo = jdbcTemplate.queryForList(sql, Collections.emptyMap());
        }
        catch (Exception exception) {
            dbTableInfo = ListUtil.empty();
        }

        if (!ObjectUtil.isEmpty(dbTableInfo)) {
            return;
        }

        String createIdSpaceSql = getIdSpaceSql();
        ctx.info(createIdSpaceSql + ";");
        if (ensureTableEnabled(ctx)) {
            try {
                jdbcTemplate.getJdbcTemplate().execute(createIdSpaceSql);
            }
            catch (Exception pE) {
                ctx.info("Ignored exception during create table: " + pE.getMessage());
            }
        }
    }

    public String getIdSpaceSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ")
                .append(getTqlIdSpaceTable())
                .append(" (\n")
                .append("type_name varchar(100) PRIMARY KEY,\n")
                .append("current_level bigint)\n");
        String createIdSpaceSql = sb.toString();
        return createIdSpaceSql;
    }

    protected String findIdSpaceTableSql() {
        return findTableColumnsSql(dataSource, getTqlIdSpaceTable());
    }

    private Map<String, Object> getOneRow(ResultSet rs, ResultSetMetaData metaData)
            throws SQLException {
        Map<String, Object> oneRow = new HashMap<>();
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i + 1);
            Object value = rs.getObject(i + 1);
            oneRow.put(columnName, value);
        }
        return oneRow;
    }

    protected String getSQLForUpdateWhenPrepareId() {
        return "SELECT current_level from {} WHERE type_name = '{}'";
    }

    @Override
    public Long prepareId(UserContext userContext, T entity) {
        if (entity.getId() != null) {
            return entity.getId();
        }
        if (userContext instanceof DefaultUserContext) {
            Long id = ((DefaultUserContext) userContext).generateId(entity);
            if (id != null) {
                return id;
            }
        }

        AtomicLong current = new AtomicLong();
        try {
            final DataSourceTransactionManager transactionManager =
                    new DataSourceTransactionManager(dataSource);
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(
                    tx -> {
                        String type = CollectionUtil.getLast(types);
                        Number dbCurrent = null;
                        try {
                            dbCurrent =
                                    jdbcTemplate.queryForObject(
                                            StrUtil.format(getSQLForUpdateWhenPrepareId(), getTqlIdSpaceTable(), type),
                                            Collections.emptyMap(),
                                            Long.class);
                        }
                        catch (Exception e) {

                        }

                        if (dbCurrent == null) {
                            current.set(1l);
                            jdbcTemplate
                                    .getJdbcTemplate()
                                    .execute(
                                            StrUtil.format(
                                                    "INSERT INTO {} VALUES ('{}', {})", getTqlIdSpaceTable(), type, current));
                            return;
                        }
                        dbCurrent = NumberUtil.add(dbCurrent, 1);
                        jdbcTemplate
                                .getJdbcTemplate()
                                .execute(
                                        StrUtil.format(
                                                "UPDATE {} SET current_level = {} WHERE type_name = '{}'",
                                                getTqlIdSpaceTable(),
                                                dbCurrent,
                                                type));
                        current.set(dbCurrent.longValue());
                    });
        }
        catch (Exception pE) {
            throw new RepositoryException(pE);
        }
        return current.get();
    }

    protected void ensureIndexAndForeignKey(UserContext ctx) {
        List<SQLConstraint> constraints = fetchFKs(ctx);
        List<Relation> ownRelations = entityDescriptor.getOwnRelations();
        for (Relation ownRelation : ownRelations) {
            ensureForeignKeyForRelation(ctx, constraints, ownRelation);
        }
        for (String table : allTableNames) {
            if (table.equals(versionTableName)) {
                continue;
            }
            ensureFK(ctx, constraints, table, "id", versionTableName, "id");
        }
    }

    private void ensureForeignKeyForRelation(
            UserContext ctx, List<SQLConstraint> constraints, Relation relation) {
        if (relation instanceof GenericSQLRelation sqlRelation) {
            String tableName = sqlRelation.getTableName();
            String columnName = sqlRelation.getColumnName();
            EntityDescriptor owner = relation.getReverseProperty().getOwner();
            String fTableName = tableName(owner.getType());
            String fColumnName = "id";
            ensureFK(ctx, constraints, tableName, columnName, fTableName, fColumnName);
        }
    }

    private void ensureFK(
            UserContext ctx,
            List<SQLConstraint> constraints,
            String tableName,
            String columnName,
            String fTableName,
            String fColumnName) {
        Optional<SQLConstraint> sqlConstraint =
                constraints.stream()
                        .filter(
                                constraint ->
                                        ObjectUtil.equals(tableName, constraint.tableName())
                                                && ObjectUtil.equals(columnName, constraint.columnName())
                                                && ObjectUtil.equals(fTableName, constraint.fTableName())
                                                && ObjectUtil.equals(fColumnName, constraint.fColumnName()))
                        .findFirst();
        if (sqlConstraint.isEmpty()) {
            String pkSql =
                    prepareCreatePKSQL(
                            new SQLConstraint(
                                    StrUtil.format("FK_{}_{}", tableName, columnName),
                                    tableName,
                                    columnName,
                                    fTableName,
                                    fColumnName));
            if (ObjectUtil.isEmpty(pkSql)) {
                return;
            }
            ctx.info(pkSql + ";");
            if (ensureTableEnabled(ctx)) {
                try {
                    jdbcTemplate.getJdbcTemplate().execute(pkSql);
                }
                catch (DataAccessException pE) {
                    throw new RepositoryException(pE);
                }
            }
        }
    }

    protected String prepareCreatePKSQL(SQLConstraint constraint) {
        return StrUtil.format(
                """
                        ALTER TABLE {}
                            ADD CONSTRAINT {}
                            FOREIGN KEY ({})
                            REFERENCES {}({})
                                ON DELETE CASCADE;
                        """,
                constraint.tableName(),
                constraint.name(),
                constraint.columnName(),
                constraint.fTableName(),
                constraint.fColumnName());
    }

    protected List<SQLConstraint> fetchFKs(UserContext ctx) {
        try {
            return jdbcTemplate.query(
                    fetchFKsSQL(), Collections.emptyMap(), new DataClassRowMapper<>(SQLConstraint.class));
        } catch (Exception e) {
            ctx.warn("Failed to fetch foreign keys: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    protected String fetchFKsSQL() {
        return """
                SELECT
                    tc.constraint_name AS name,
                    tc.table_name AS tableName,
                    kcu.column_name AS columnName,
                    ccu.table_name AS fTableName,
                    ccu.column_name AS fColumnName
                FROM
                    information_schema.table_constraints AS tc
                    JOIN information_schema.key_column_usage AS kcu
                      ON tc.constraint_name = kcu.constraint_name
                    JOIN information_schema.constraint_column_usage AS ccu
                      ON ccu.constraint_name = tc.constraint_name
                WHERE
                    tc.constraint_type = 'FOREIGN KEY'
                """;
    }

    public void ensureInitData(UserContext ctx) {
        if (entityDescriptor.isRoot()) {
            ensureRoot(ctx);
        }
        if (entityDescriptor.isConstant()) {
            ensureConstant(ctx);
        }
    }

    private void ensureConstant(UserContext ctx) {
        PropertyDescriptor identifier = entityDescriptor.getIdentifier();
        List<String> candidates = identifier.getCandidates();
        List<PropertyDescriptor> ownProperties = entityDescriptor.getOwnProperties();
        List<String> columns = new ArrayList<>();
        for (PropertyDescriptor ownProperty : ownProperties) {
            SQLColumn sqlColumn = getSqlColumn(ownProperty);
            columns.add(sqlColumn.getColumnName());
        }
        for (int i = 0; i < candidates.size(); i++) {
            String code = candidates.get(i);
            List oneConstant = new ArrayList();
            for (PropertyDescriptor ownProperty : ownProperties) {
                oneConstant.add(getConstantPropertyValue(ctx, ownProperty, i, code));
            }

            Map<String, Object> dbRootRow = null;
            try {
                dbRootRow =
                        jdbcTemplate.queryForMap(
                                StrUtil.format(
                                        "SELECT * FROM {} WHERE id = '{}'",
                                        tableName(entityDescriptor.getType()),
                                        getConstantPropertyValue(ctx, entityDescriptor.findIdProperty(), i, code)),
                                Collections.emptyMap());
            }
            catch (Exception e) {

            }

            if (dbRootRow != null) {
                long version = Long.parseLong(String.valueOf(dbRootRow.get("version")));
                if (version > 0) {
                    continue;
                }
                // update version
                String sql =
                        StrUtil.format(
                                "UPDATE {} SET version = {} where id = '{}'",
                                tableName(entityDescriptor.getType()),
                                -version,
                                getConstantPropertyValue(ctx, entityDescriptor.findIdProperty(), i, code));
                ctx.info(sql + ";");
                if (ensureTableEnabled(ctx)) {
                    try {
                        jdbcTemplate.getJdbcTemplate().execute(sql);
                    }
                    catch (DataAccessException pE) {
                        throw new RepositoryException(pE);
                    }
                }
                continue;
            }

            String sql =
                    StrUtil.format(
                            "INSERT INTO {} ({}) VALUES ({})",
                            tableName(entityDescriptor.getType()),
                            CollectionUtil.join(columns, ","),
                            CollectionUtil.join(oneConstant, ",", value -> getSqlValue(value)));
            ctx.info(sql + ";");
            if (ensureTableEnabled(ctx)) {
                try {
                    jdbcTemplate.getJdbcTemplate().execute(sql);
                }
                catch (Exception pE) {
                    ctx.info("Ignored insert exception in ensureConstant: " + pE.getMessage());
                }
            }
        }
    }

    private long genIdForCandidateCode(String code) {
        return Math.abs(code.toUpperCase().hashCode());
    }

    private Object getConstantPropertyValue(
            UserContext ctx, PropertyDescriptor property, int index, String identifier) {
        if (property.isVersion()) {
            return 1l;
        }

        PropertyType type = property.getType();
        if (BaseEntity.class.isAssignableFrom(type.javaType())) {
            String referType = type.javaType().getSimpleName();
            EntityDescriptor refer = ctx.resolveEntityDescriptor(referType);
//            if (refer.isRoot()) {
//                return "1";
//            }
            // set others as null
            return "1";
        }

        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) {
            return ReflectUtil.invoke(ctx, createFunction);
        }

        List<String> candidates = property.getCandidates();

        if (property.isIdentifier()) {
            return identifier;
            //return NamingCase.toPascalCase(identifier);
        }

        if (ObjectUtil.isNotEmpty(candidates)) {
            return CollectionUtil.get(candidates, index);
        }

        if (property.isId()) {
            return genIdForCandidateCode(identifier);
        }
        return null;
    }

    private void ensureRoot(UserContext ctx) {
        Map<String, Object> dbRootRow = null;
        try {
            dbRootRow =
                    jdbcTemplate.queryForMap(
                            StrUtil.format(
                                    "SELECT * FROM {} WHERE id = '1'", tableName(entityDescriptor.getType())),
                            Collections.emptyMap());
        }
        catch (Exception e) {

        }

        if (dbRootRow != null) {
            long version = Long.parseLong(String.valueOf(dbRootRow.get("version")));
            if (version > 0) {
                return;
            }
            // update version
            String sql =
                    StrUtil.format(
                            "UPDATE {} SET version = {} where id = '1'\n",
                            tableName(entityDescriptor.getType()),
                            -version);
            ctx.info(sql + ";");
            if (ensureTableEnabled(ctx)) {
                try {
                    jdbcTemplate.getJdbcTemplate().execute(sql);
                }
                catch (DataAccessException pE) {
                    throw new RepositoryException(pE);
                }
            }
            return;
        }
        List<String> columns = new ArrayList();
        List rootRow = new ArrayList();
        List<PropertyDescriptor> ownProperties = entityDescriptor.getOwnProperties();
        for (PropertyDescriptor ownProperty : ownProperties) {
            Object value = getRootPropertyValue(ctx, ownProperty);
            rootRow.add(value);
            SQLColumn sqlColumn = getSqlColumn(ownProperty);
            columns.add(sqlColumn.getColumnName());
        }
        String sql =
                StrUtil.format(
                        "INSERT INTO {} ({}) VALUES ({})\n",
                        tableName(entityDescriptor.getType()),
                        CollectionUtil.join(columns, ","),
                        CollectionUtil.join(rootRow, ",", value -> getSqlValue(value)));
        ctx.info(sql + ";");
        if (ensureTableEnabled(ctx)) {
            try {
                jdbcTemplate.getJdbcTemplate().execute(sql);
            }
            catch (Exception pE) {
                ctx.info("Ignored insert exception in ensureRoot: " + pE.getMessage());
            }
        }
    }

    protected String getSqlValue(Object value) {
        return StrUtil.wrapIfMissing(String.valueOf(value), "'", "'");
    }

    private Object getRootPropertyValue(UserContext ctx, PropertyDescriptor property) {
        if (property.isId()) {
            return 1l;
        }
        if (property.isVersion()) {
            return 1l;
        }
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) {
            return ReflectUtil.invoke(ctx, createFunction);
        }
        return property.getAdditionalInfo().get("candidates");
    }

    protected void ensure(
            UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        // table not found
        if (tableInfo.isEmpty()) {
            createTable(ctx, table, columns);
            return;
        }

        Map<String, Map<String, Object>> fields = getFields(tableInfo);

        for (int i = 0; i < columns.size(); i++) {
            SQLColumn column = columns.get(i);
            String tableName = column.getTableName();
            String columnName = column.getColumnName();
            String type = column.getType();

            String preColumnName = null;
            if (i > 0) {
                preColumnName = columns.get(i - 1).getColumnName();
            }
            String dbColumnName = getPureColumnName(columnName).toLowerCase();
            Map<String, Object> field = fields.get(dbColumnName);
            if (field == null) {
                addColumn(ctx, preColumnName, column);
                continue;
            }

            String dbType = calculateDBType(field);
            if (isTypeMatch(dbType, type)) continue;

            alterColumn(ctx, tableInfo,table,columns,column);
        }
    }

    protected boolean isTypeMatch(String dbType, String type) {
        return dbType.equalsIgnoreCase(type);
    }

    protected Map<String, Map<String, Object>> getFields(List<Map<String, Object>> tableInfo) {
        return CollStreamUtil.toIdentityMap(
                tableInfo, m -> String.valueOf(m.get(getSchemaColumnNameFieldName())).toLowerCase());
    }

    protected String getSchemaColumnNameFieldName() {
        return "column_name";
    }

    /**
     * Strip wrapping identifier quotes from a SQL column name.
     * Handles double-quotes, backticks, and square brackets so that
     * column names from entity descriptors can be compared with bare
     * names returned by database introspection.
     */
    protected String getPureColumnName(String columnName) {
        if (columnName == null || columnName.length() < 2) {
            return columnName;
        }
        // double-quote
        String result = StrUtil.unWrap(columnName, '"');
        if (!result.equals(columnName)) return result;
        // backtick
        result = StrUtil.unWrap(columnName, '`');
        if (!result.equals(columnName)) return result;
        // square brackets
        result = StrUtil.unWrap(columnName, '[', ']');
        return result;
    }

    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = (String) columnInfo.get("data_type");
        switch (dataType) {
            case "bigint":
                return "bigint";
            case "tinyint":
            case "boolean":
                return "boolean";
            case "varchar":
            case "character varying":
                return StrUtil.format("varchar({})", columnInfo.get("character_maximum_length"));
            case "date":
                return "date";
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
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + dataType);
        }
    }

    protected void alterColumn(UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns, SQLColumn column) {
        String alterColumnSql = generateAlterColumnSQL(ctx, column);
        ctx.info(alterColumnSql + ";");
        if (ensureTableEnabled(ctx)) {
            try {
                jdbcTemplate.getJdbcTemplate().execute(alterColumnSql);
            }
            catch (DataAccessException pE) {
                throw new RepositoryException(pE);
            }
        }
    }

    protected String generateAlterColumnSQL(UserContext ctx, SQLColumn column) {
        String alterColumnSql =
                StrUtil.format(
                        "ALTER TABLE {} ALTER COLUMN {} TYPE {}",
                        column.getTableName(),
                        column.getColumnName(),
                        column.getType());
        return alterColumnSql;
    }

    private void addColumn(UserContext ctx, String preColumnName, SQLColumn column) {
        String addColumnSql = generateAddColumnSQL(ctx, preColumnName, column);
        ctx.info(addColumnSql + ";");
        if (ensureTableEnabled(ctx)) {
            try {
                jdbcTemplate.getJdbcTemplate().execute(addColumnSql);
            }
            catch (DataAccessException pE) {
                throw new RepositoryException(pE);
            }
        }
    }

    protected String generateAddColumnSQL(UserContext ctx, String preColumnName, SQLColumn column) {
        String addColumnSql =
                StrUtil.format(
                        "ALTER TABLE {} ADD COLUMN {} {}",
                        column.getTableName(),
                        column.getColumnName(),
                        column.getType());
        return addColumnSql;
    }

    protected String wrapColumnStatementForCreatingTable(
            UserContext ctx, String table, SQLColumn column) {

        String dbColumn = column.getColumnName() + " " + column.getType();
        if (column.isIdColumn()) {
            dbColumn = dbColumn + " PRIMARY KEY";
        }
        return dbColumn;
    }

    protected void createTable(UserContext ctx, String table, List<SQLColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table).append(" (\n");
        sb.append(
                columns.stream()
                        .map(column -> wrapColumnStatementForCreatingTable(ctx, table, column))
                        .collect(Collectors.joining(",\n")));
        sb.append(")\n");
        String createTableSql = sb.toString();
        ctx.info(createTableSql + ";");

        if (ensureTableEnabled(ctx)) {
            try {
                jdbcTemplate.getJdbcTemplate().execute(createTableSql);
            }
            catch (Exception pE) {
                ctx.info("Ignored exception during create table " + table + ": " + pE.getMessage());
            }
        }
    }

    @Override
    public List<SQLColumn> getPropertyColumns(String idTable, String propertyName) {
        if (getChildType().equalsIgnoreCase(propertyName)) {
            if (entityDescriptor.hasChildren()) {
                SQLColumn sqlColumn = new SQLColumn(tableAlias(thisPrimaryTableName), getChildType());
                sqlColumn.setType(getChildSqlType());
                return ListUtil.of(sqlColumn);
            }
            else {
                return ListUtil.empty();
            }
        }

        PropertyDescriptor property = findProperty(propertyName);
        List<SQLColumn> sqlColumns = getSqlColumns(property);
        for (SQLColumn sqlColumn : sqlColumns) {
            if (property.isId()) {
                sqlColumn.setTableName(tableAlias(idTable));
            }
            else {
                sqlColumn.setTableName(tableAlias(sqlColumn.getTableName()));
            }
        }
        return sqlColumns;
    }

    public String getChildType() {
        return childType;
    }

    public void setChildType(String pChildType) {
        childType = pChildType;
    }

    public String getChildSqlType() {
        return childSqlType;
    }

    public void setChildSqlType(String pChildSqlType) {
        childSqlType = pChildSqlType;
    }

    public String getTqlIdSpaceTable() {
        return tqlIdSpaceTable;
    }

    public void setTqlIdSpaceTable(String pTqlIdSpaceTable) {
        tqlIdSpaceTable = pTqlIdSpaceTable;
    }

    public Map<Class, SQLExpressionParser> getExpressionParsers() {
        return expressionParsers;
    }

    public boolean canMixinSubQuery(UserContext userContext, SearchRequest subQuery) {
        return true;
    }
}
