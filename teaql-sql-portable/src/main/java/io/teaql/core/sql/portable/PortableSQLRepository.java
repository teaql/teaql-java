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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
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
import io.teaql.core.ContinuousPageCursor;
import io.teaql.core.ContinuousPageCursorStore;
import io.teaql.core.ContinuousPageFetchOptions;
import io.teaql.core.FunctionApply;
import io.teaql.core.Parameter;
import io.teaql.core.PropertyReference;
import io.teaql.core.criteria.GT;
import io.teaql.core.criteria.LT;
import io.teaql.core.criteria.Operator;
import io.teaql.core.internal.TempRequest;


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
    public static final String CONTINUOUS_PAGE_PLAN = "teaql.continuousPage.plan";
    public static final String CONTINUOUS_PAGE_CURSOR_ID = "teaql.continuousPage.cursorId";
    public static final String COMPILED_ROW_MAPPER = "teaql.sql.compiledRowMapper";
    private final ContinuousPageCursorStore defaultCursorStore =
            new InMemoryContinuousPageCursorStore();

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
    private final Map<String, CompiledQueryPlan> compiledQueryPlans = new ConcurrentHashMap<>();
    private static final int MAX_COMPILED_QUERY_PLANS = 512;

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

    private record CompiledQueryPlan(
            String sql,
            int parameterCount,
            io.teaql.core.CompiledRowMapper<?> rowMapper) {}

    private record ColumnBinding(
            int index,
            PropertyDescriptor property,
            EntityDescriptor relationDescriptor,
            int loadedPropertyIndex) {}

    private record QueryShape(String key, Object[] arguments) {}

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
                continue;
            }
            args.add(value);
            m.appendReplacement(sb, "?");
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

    private ContinuousPageExecution<T> prepareContinuousPage(
            UserContext context,
            SearchRequest<T> request,
            String originalSql,
            Map<String, Object> originalParameters) {
        ContinuousPageFetchOptions options = request.continuousPageFetchOptions();
        Slice slice = request.getSlice();
        if (options == null) return fallback(context, request, null, "DISABLED");
        if (slice == null || slice.getOffset() <= 0 || slice.getSize() <= 0) {
            return fallback(context, request, queryKey(context, request, options, originalSql, originalParameters),
                    slice == null || slice.getOffset() <= 0 ? "FIRST_PAGE" : "INVALID_SLICE");
        }
        if (request.getPartitionProperty() != null || request.hasSimpleAgg()) {
            return fallback(context, request, null, "UNSUPPORTED_QUERY_SHAPE");
        }
        OrderBys orderBys = request.getOrderBy();
        if (orderBys == null || orderBys.getOrderBys().size() != 1) {
            return fallback(context, request, null, "ORDER_NOT_SINGLE");
        }
        OrderBy order = orderBys.getOrderBys().get(0);
        if (!(order.getExpression() instanceof FunctionApply function)
                || function.getOperator() != io.teaql.core.AggrFunction.SELF
                || !(function.first() instanceof PropertyReference property)
                || !"id".equals(property.getPropertyName())) {
            return fallback(context, request, null, "ORDER_NOT_SEEKABLE_ID");
        }
        String direction = order.getDirection() == null ? "ASC" : order.getDirection().toUpperCase();
        if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
            return fallback(context, request, null, "ORDER_DIRECTION_UNSUPPORTED");
        }

        String queryKey = queryKey(context, request, options, originalSql, originalParameters);
        ContinuousPageCursorStore store = cursorStore(context);
        Optional<ContinuousPageCursor> found;
        try {
            found = store.get(queryKey, slice.getOffset());
        } catch (RuntimeException unavailable) {
            return fallback(context, request, queryKey, "STORE_UNAVAILABLE");
        }
        if (found.isEmpty()) return fallback(context, request, queryKey, "CACHE_MISS");
        ContinuousPageCursor cursor = found.get();
        if (cursor.formatVersion() != ContinuousPageCursor.CURRENT_FORMAT_VERSION
                || !request.getTypeName().equals(cursor.entity())
                || !"id".equals(cursor.orderField())
                || !direction.equals(cursor.direction())
                || cursor.pageSize() != slice.getSize()
                || cursor.nextOffset() != slice.getOffset()
                || cursor.boundary() == null) {
            return fallback(context, request, queryKey, "CURSOR_INVALID");
        }

        TempRequest optimized = new TempRequest(request);
        optimized.offset(0, slice.getSize());
        Parameter boundary = new Parameter(
                "continuousPageBoundary", cursor.boundary(),
                "DESC".equals(direction) ? Operator.LESS_THAN : Operator.GREATER_THAN);
        optimized.appendSearchCriteria("DESC".equals(direction)
                ? new LT(new PropertyReference("id"), boundary)
                : new GT(new PropertyReference("id"), boundary));
        context.putAttribute(CONTINUOUS_PAGE_PLAN, "CURSOR_SEEK");
        context.putAttribute(CONTINUOUS_PAGE_CURSOR_ID, cursor.cursorId());
        return new ContinuousPageExecution<>((SearchRequest<T>) optimized, queryKey, direction, true);
    }

    private ContinuousPageExecution<T> fallback(
            UserContext context, SearchRequest<T> request, String queryKey, String reason) {
        context.putAttribute(CONTINUOUS_PAGE_PLAN, "OFFSET_FALLBACK:" + reason);
        context.putAttribute(CONTINUOUS_PAGE_CURSOR_ID, null);
        return new ContinuousPageExecution<>(request, queryKey, null, false);
    }

    private void registerContinuousPage(
            UserContext context,
            SearchRequest<T> originalRequest,
            ContinuousPageExecution execution,
            List<T> results) {
        ContinuousPageFetchOptions options = originalRequest.continuousPageFetchOptions();
        Slice slice = originalRequest.getSlice();
        if (options == null || slice == null || results.size() != slice.getSize() || results.isEmpty()) return;

        String queryKey = execution.queryKey();
        if (queryKey == null) return;
        String direction = execution.direction();
        if (direction == null) {
            OrderBys orderBys = originalRequest.getOrderBy();
            if (orderBys == null || orderBys.getOrderBys().size() != 1) return;
            OrderBy order = orderBys.getOrderBys().get(0);
            if (!(order.getExpression() instanceof FunctionApply function)
                    || function.getOperator() != io.teaql.core.AggrFunction.SELF
                    || !(function.first() instanceof PropertyReference property)
                    || !"id".equals(property.getPropertyName())) return;
            direction = order.getDirection().toUpperCase();
            if (!"ASC".equals(direction) && !"DESC".equals(direction)) return;
        }
        T last = results.get(results.size() - 1);
        if (last.getId() == null) return;
        Instant now = Instant.now();
        ContinuousPageCursor cursor = new ContinuousPageCursor(
                ContinuousPageCursor.CURRENT_FORMAT_VERSION,
                "cpg_" + UUID.randomUUID(),
                options.namespace(),
                queryKey,
                originalRequest.getTypeName(),
                "id",
                direction,
                last.getId(),
                slice.getOffset(),
                (long) slice.getOffset() + results.size(),
                slice.getSize(),
                now,
                now,
                now.plusSeconds(options.ttlSeconds()),
                observableOwner(context),
                Map.of("plan", execution.optimized() ? "CURSOR_SEEK" : "OFFSET_FALLBACK"));
        try {
            cursorStore(context).put(queryKey, cursor);
        } catch (RuntimeException ignored) {
            context.putAttribute(CONTINUOUS_PAGE_PLAN, "OFFSET_FALLBACK:STORE_UNAVAILABLE");
        }
    }

    private ContinuousPageCursorStore cursorStore(UserContext context) {
        ContinuousPageCursorStore custom = context.capability(ContinuousPageCursorStore.class);
        return custom == null ? defaultCursorStore : custom;
    }

    private String queryKey(
            UserContext context,
            SearchRequest<T> request,
            ContinuousPageFetchOptions options,
            String sql,
            Map<String, Object> parameters) {
        String paginationNeutralSql = sql.replaceAll(
                "(?i)\\bOFFSET\\s+(?:\\?|:[A-Za-z_][A-Za-z0-9_]*|\\d+)", "OFFSET ?");
        StringBuilder canonical = new StringBuilder(options.namespace())
                .append('|').append(request.getTypeName()).append('|').append(paginationNeutralSql);
        new TreeMap<>(parameters).forEach((key, value) -> {
            if (!key.startsWith("limit") && !key.startsWith("offset")) {
                canonical.append('|').append(key).append('=').append(String.valueOf(value));
            }
        });
        observableOwner(context).forEach((key, value) ->
                canonical.append('|').append(key).append('=').append(value));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return "teaql:continuous-page:v1:" + HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new TeaQLRuntimeException("SHA-256 unavailable", e);
        }
    }

    private Map<String, String> observableOwner(UserContext context) {
        Map<String, String> owner = new TreeMap<>();
        for (String key : List.of("tenantId", "merchantId", "userId", "sessionId", "applicationId",
                "permissionScopeHash", "policyVersion")) {
            Object value = context.getAttribute(key);
            if (value != null && !String.valueOf(value).isBlank()) owner.put(key, String.valueOf(value));
        }
        return owner;
    }

    private record ContinuousPageExecution<E extends Entity>(
            SearchRequest<E> request,
            String queryKey,
            String direction,
            boolean optimized) {}

    public SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {
        QueryShape shape = simpleQueryShape(userContext, request);
        CompiledQueryPlan plan = shape == null ? null : compiledQueryPlans.get(shape.key());
        ContinuousPageExecution pageExecution;
        SearchRequest<T> executedRequest;
        PositionalSQL psql;
        if (plan != null && plan.parameterCount() == shape.arguments().length) {
            psql = new PositionalSQL(plan.sql(), shape.arguments());
            pageExecution = fallback(userContext, request, null, "DISABLED");
            executedRequest = request;
        }
        else {
            Map<String, Object> params = new HashMap<>();
            String sql = buildDataSQL(userContext, request, params);
            if (ObjectUtil.isEmpty(sql)) {
                return new SmartList<>();
            }
            pageExecution = prepareContinuousPage(userContext, request, sql, params);
            executedRequest = pageExecution.request();
            if (pageExecution.request() != request) {
                params = new HashMap<>();
                sql = buildDataSQL(userContext, executedRequest, params);
            }
            psql = toPositional(sql, params);
            if (shape != null && psql.args.length == shape.arguments().length) {
                if (compiledQueryPlans.size() >= MAX_COMPILED_QUERY_PLANS) {
                    compiledQueryPlans.clear();
                }
                CompiledQueryPlan candidate = new CompiledQueryPlan(
                        psql.sql,
                        psql.args.length,
                        compileRowMapper(executedRequest));
                CompiledQueryPlan existing = compiledQueryPlans.putIfAbsent(shape.key(), candidate);
                plan = existing == null ? candidate : existing;
            }
        }
        SmartList<T> smartList;
        Object mapperExtension = request.getExtension(COMPILED_ROW_MAPPER);
        io.teaql.core.CompiledRowMapper<?> selectedMapper =
                mapperExtension instanceof io.teaql.core.CompiledRowMapper<?> explicitMapper
                        ? explicitMapper
                        : plan == null ? compileRowMapper(executedRequest) : plan.rowMapper();
        if (selectedMapper != null && database.supportsCompiledRowMapping()) {
            io.teaql.core.CompiledRowMapper<?> rawMapper = selectedMapper;
            @SuppressWarnings("unchecked")
            io.teaql.core.CompiledRowMapper<T> mapper =
                    (io.teaql.core.CompiledRowMapper<T>) rawMapper;
            List<T> entities = database.query(userContext, psql.sql, psql.args, mapper);
            if (entities.isEmpty() && ObjectUtil.isEmpty(request.getFacetRequests())) {
                registerContinuousPage(userContext, request, pageExecution, List.of());
                return SmartList.empty(request.returnType());
            }
            smartList = SmartList.takeOwnership(entities);
        }
        else {
            List<Map<String, Object>> rows = database.query(userContext, psql.sql, psql.args);
            if (rows.isEmpty() && ObjectUtil.isEmpty(request.getFacetRequests())) {
                registerContinuousPage(userContext, request, pageExecution, List.of());
                return SmartList.empty(request.returnType());
            }
            smartList = new SmartList<>(rows.size());
            for (Map<String, Object> row : rows) {
                smartList.add(mapRowToEntity(userContext, executedRequest, row));
            }
        }
        registerContinuousPage(userContext, request, pageExecution, smartList.getData());
        
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
                                int countInt = toIntOrZero(cnt);
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

    private io.teaql.core.CompiledRowMapper<T> compileRowMapper(SearchRequest<T> request) {
        // Subtype discriminators and dynamic projections carry values that are not entity properties.
        // Keep those uncommon shapes on the generic mapper until their binding model is explicit.
        if (entityDescriptor.hasChildren()
                || ObjectUtil.isNotEmpty(request.getSimpleDynamicProperties())) return null;

        List<SimpleNamedExpression> projections = request.getProjections();
        List<PropertyDescriptor> selected = new ArrayList<>();
        if (ObjectUtil.isEmpty(projections)) {
            for (PropertyDescriptor property : allProperties) {
                if (shouldHandle(property)) selected.add(property);
            }
        }
        else {
            for (SimpleNamedExpression projection : projections) {
                PropertyDescriptor property = null;
                for (PropertyDescriptor candidate : allProperties) {
                    if (candidate.getName().equals(projection.name())) {
                        property = candidate;
                        break;
                    }
                }
                if (property == null || !shouldHandle(property)) return null;
                selected.add(property);
            }
        }

        EntityDescriptor resultDescriptor = resolveDescriptor(request.returnType());
        List<ColumnBinding> bindings = new ArrayList<>(selected.size());
        int index = 1;
        for (PropertyDescriptor property : selected) {
            EntityDescriptor relationDescriptor = property instanceof Relation
                    ? resolveDescriptor((Class<? extends Entity>) property.getType().javaType())
                    : null;
            bindings.add(new ColumnBinding(
                    index++,
                    property,
                    relationDescriptor,
                    BaseEntity.loadedPropertyIndex(
                            (Class<? extends BaseEntity>) resultDescriptor.getTargetType(),
                            property.getName())));
        }

        return row -> {
            @SuppressWarnings("unchecked")
            T entity = (T) resultDescriptor.createEntity();
            BaseEntity base = (BaseEntity) entity;
            for (ColumnBinding binding : bindings) {
                PropertyDescriptor property = binding.property();
                Object value;
                if (binding.relationDescriptor() == null) {
                    value = row.get(binding.index(), property.getType().javaType());
                }
                else {
                    Long id = row.get(binding.index(), Long.class);
                    if (id == null) value = null;
                    else {
                        BaseEntity reference = (BaseEntity) binding.relationDescriptor().createEntity();
                        reference.__internalSet(BaseEntity.ID_PROPERTY, id);
                        reference.set$status(io.teaql.core.EntityStatus.REFER);
                        value = reference;
                    }
                }
                base.__internalHydrate(
                        property.getName(), value, binding.loadedPropertyIndex());
            }
            base.set$status(resolvePersistedStatus(entity.getVersion()));
            base.clearUpdatedProperties();
            return entity;
        };
    }

    private QueryShape simpleQueryShape(UserContext context, SearchRequest<T> request) {
        if (!(request instanceof io.teaql.core.BaseRequest<?> )
                || request.continuousPageFetchOptions() != null
                || request.hasSimpleAgg()
                || ObjectUtil.isNotEmpty(request.getPartitionProperty())
                || ObjectUtil.isNotEmpty(request.getSearchForText())
                || ObjectUtil.isNotEmpty(request.getSimpleDynamicProperties())
                || ObjectUtil.isNotEmpty(request.getDynamicAggregateAttributes())
                || ObjectUtil.isNotEmpty(request.enhanceRelations())
                || ObjectUtil.isNotEmpty(request.enhanceChildren())
                || ObjectUtil.isNotEmpty(request.getFacetRequests())
                || ObjectUtil.isNotEmpty(request.getPropagateAggregations())
                || ObjectUtil.isNotEmpty(request.getPropagateDimensions())
                || request.getDynamicFieldSelection() != null) {
            return null;
        }
        StringBuilder key = new StringBuilder(192)
                .append(dialect.getClass().getName()).append('|')
                .append(request.getClass().getName()).append('|')
                .append(request.returnType().getName()).append('|');
        List<Object> arguments = new ArrayList<>();
        for (SimpleNamedExpression projection : request.getProjections()) {
            key.append("S:").append(projection.name()).append('=');
            if (!appendExpressionShape(projection.getExpression(), key, arguments, false)) return null;
            key.append(';');
        }
        key.append("W:");
        if (!appendExpressionShape(request.getSearchCriteria(), key, arguments, false)) return null;
        key.append("|O:");
        for (OrderBy order : request.getOrderBy().getOrderBys()) {
            if (!appendExpressionShape(order.getExpression(), key, arguments, false)) return null;
            key.append(':').append(order.getDirection()).append(';');
        }
        Slice slice = request.getSlice();
        if (slice == null) {
            key.append("|L:none");
        }
        else {
            key.append("|L:param:O:").append(slice.getOffset() == 0 ? "zero" : "param");
            arguments.add(slice.getSize());
            if (slice.getOffset() != 0) arguments.add(slice.getOffset());
        }
        key.append("|M:").append(context.getBool(io.teaql.core.sql.SqlAstCompiler.MULTI_TABLE, false))
                .append(':').append(context.getBool(MULTI_TABLE, false))
                .append("|I:").append(context.getBool(
                        io.teaql.core.sql.SqlAstCompiler.IGNORE_SUBTYPES, false));
        return new QueryShape(key.toString(), arguments.toArray());
    }

    private boolean appendExpressionShape(
            Expression expression, StringBuilder key, List<Object> arguments, boolean inlineParameter) {
        if (expression == null) {
            key.append("null");
            return true;
        }
        if (expression instanceof io.teaql.core.criteria.VersionSearchCriteria version) {
            SearchCriteria nested = version.getSearchCriteria();
            boolean active = isActiveVersionPredicate(nested);
            key.append(active ? "VACTIVE(" : "V(");
            boolean supported = appendExpressionShape(nested, key, arguments, active);
            key.append(')');
            return supported;
        }
        if (expression instanceof PropertyReference property) {
            key.append("P:").append(property.getPropertyName());
            return true;
        }
        if (expression instanceof Parameter parameter) {
            Object value = parameter.getValue();
            if (inlineParameter) {
                key.append("C:0");
                return true;
            }
            Collection<?> expanded = expandedParameterValues(value);
            if (expanded == null) {
                key.append("?:1");
                arguments.add(value);
            }
            else {
                key.append("?:").append(expanded.size());
                if (expanded.isEmpty()) arguments.add(null);
                else arguments.addAll(expanded);
            }
            return true;
        }
        if (expression instanceof FunctionApply function) {
            key.append("F:").append(function.getOperator()).append('(');
            for (Expression child : function.getExpressions()) {
                if (!appendExpressionShape(child, key, arguments, inlineParameter)) return false;
                key.append(',');
            }
            key.append(')');
            return true;
        }
        return false;
    }

    private boolean isActiveVersionPredicate(SearchCriteria criteria) {
        if (!(criteria instanceof io.teaql.core.criteria.TwoOperatorCriteria function)
                || function.getOperator() != Operator.GREATER_THAN
                || !(function.first() instanceof PropertyReference property)
                || !"version".equals(property.getPropertyName())
                || !(function.second() instanceof Parameter parameter)
                || !(parameter.getValue() instanceof Number number)) return false;
        return number.longValue() == 0L && number.doubleValue() == 0D;
    }

    @SuppressWarnings("unchecked")
    public T loadPersistedById(UserContext userContext, Long id) {
        String sql = "SELECT * FROM " + escapeIdentifier(tableName(entityDescriptor.getType()))
                + " WHERE " + escapeIdentifier("id") + " = ?";
        List<Map<String, Object>> rows = database.query(userContext, sql, new Object[] {id});
        if (rows.size() != 1) {
            throw new TeaQLRuntimeException(
                    "Persisted " + entityDescriptor.getType() + "(" + id + ") could not be read back");
        }
        T entity = (T) entityDescriptor.createEntity();
        Map<String, Object> row = rows.get(0);
        for (PropertyDescriptor property : this.allProperties) {
            if (!shouldHandle(property)) continue;
            String columnKey = findColumnKey(row, property.getName());
            if (columnKey == null) continue;
            Object value = row.get(columnKey);
            if (!(property instanceof Relation)) {
                Class targetType = property.getType().javaType();
                entity.setProperty(property.getName(), value == null
                        ? null : convertColumnValue(targetType, value));
            } else if (value == null) {
                entity.setProperty(property.getName(), null);
            } else {
                Entity ref = createEntity((Class<? extends Entity>) property.getType().javaType());
                ((BaseEntity) ref).__internalSet(
                        "id", io.teaql.core.utils.Convert.convert(Long.class, value));
                ((BaseEntity) ref).set$status(io.teaql.core.EntityStatus.REFER);
                entity.setProperty(property.getName(), ref);
            }
        }
        if (entity instanceof BaseEntity baseEntity) {
            baseEntity.set$status(resolvePersistedStatus(entity.getVersion()));
            baseEntity.clearUpdatedProperties();
        }
        return entity;
    }

    public Stream<T> streamInternal(UserContext userContext, SearchRequest<T> request) {
        Map<String, Object> params = new HashMap<>();
        String sql = buildDataSQL(userContext, request, params);
        if (ObjectUtil.isEmpty(sql)) return Stream.empty();
        PositionalSQL psql = toPositional(sql, params);
        return database.queryForStream(userContext, psql.sql, psql.args)
                .map(row -> mapRowToEntity(userContext, request, row));
    }

    private T mapRowToEntity(UserContext userContext, SearchRequest<T> request, Map<String, Object> row) {
        Class<? extends T> returnType = request.returnType();
        T entity = createEntity(returnType);
        for (PropertyDescriptor property : this.allProperties) {
            if (!shouldHandle(property)) continue;
            if (!(property instanceof Relation)) {
                String columnKey = findColumnKey(row, property.getName());
                if (columnKey == null) continue;
                Object value = row.get(columnKey);
                Class targetType = property.getType().javaType();
                entity.setProperty(
                        property.getName(),
                        value == null
                                ? null
                                : convertColumnValue(targetType, value));
            } else if (property instanceof Relation) {
                String columnKey = findColumnKey(row, property.getName());
                if (columnKey == null) continue;
                Object value = row.get(columnKey);
                if (value == null) {
                    entity.setProperty(property.getName(), null);
                    continue;
                }
                try {
                    Entity ref = createEntity((Class<? extends Entity>) property.getType().javaType());
                    ((BaseEntity) ref).__internalSet("id", io.teaql.core.utils.Convert.convert(Long.class, value));
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
        // Subtype
        Object typeAlias = row.get(TYPE_ALIAS);
        if (typeAlias != null) {
            entity.setRuntimeType(String.valueOf(typeAlias));
        }
        // Status
        Long version = entity.getVersion();
        if (entity instanceof BaseEntity be) {
            io.teaql.core.EntityStatus status = resolvePersistedStatus(version);
            be.set$status(status);
        }
        // Dynamic properties
        List<SimpleNamedExpression> simpleDynamicProperties = request.getSimpleDynamicProperties();
        for (SimpleNamedExpression dp : simpleDynamicProperties) {
            Object value = row.get(dp.name());
            if (value != null) entity.addDynamicProperty(dp.name(), value);
        }

        return entity;
    }

    private String findColumnKey(Map<String, Object> row, String propertyName) {
        if (row.containsKey(propertyName)) return propertyName;
        for (String key : row.keySet()) {
            if (key != null && key.equalsIgnoreCase(propertyName)) return key;
        }
        return null;
    }

    static Object convertTemporalColumnValue(Class<?> targetType, Object value) {
        if (targetType == java.time.LocalDateTime.class
                && value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (targetType == java.time.LocalDateTime.class) {
            // SQLite returns text and some JDBC drivers return vendor temporal
            // wrappers. Both expose the standard timestamp representation.
            String text = String.valueOf(value);
            return java.time.LocalDateTime.parse(text.replace(' ', 'T'));
        }
        if (targetType == java.time.LocalDate.class) {
            if (value instanceof java.sql.Date date) return date.toLocalDate();
            if (value instanceof java.sql.Timestamp timestamp) {
                return timestamp.toLocalDateTime().toLocalDate();
            }
            String text = String.valueOf(value);
            if (text.length() >= 10) {
                return java.time.LocalDate.parse(text.substring(0, 10));
            }
        }
        if (targetType == java.time.LocalTime.class) {
            if (value instanceof java.sql.Time time) return time.toLocalTime();
            if (value instanceof java.sql.Timestamp timestamp) {
                return timestamp.toLocalDateTime().toLocalTime();
            }
            String text = String.valueOf(value);
            int separator = Math.max(text.indexOf('T'), text.indexOf(' '));
            return java.time.LocalTime.parse(separator >= 0 ? text.substring(separator + 1) : text);
        }
        return io.teaql.core.utils.Convert.convert(targetType, value);
    }

    private Object convertColumnValue(Class<?> targetType, Object value) {
        return convertTemporalColumnValue(targetType, value);
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
                    return;
                }
                if (primaryTable) {
                    updatePrimaryTable(userContext, sqlEntity, k, columns, l);
                    return;
                }
                String updateSql = dialect.buildSubsidiaryInsertSql(k, columns);
                database.executeUpdate(userContext, updateSql, l.toArray());
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

    /**
     * Prepares (allocates) an ID for the given entity if it doesn't have one.
     * Delegates to {@link IdSpaceIdGenerator} using this repository's {@link TeaQLDatabase}.
     *
     * @deprecated Use {@link IdSpaceIdGenerator} via {@code TeaQLRuntime.Builder.idGenerationService()} instead.
     *             This method is retained for backward compatibility with direct {@code PortableSQLDataService.mutate()} calls.
     */
    public Long prepareId(UserContext userContext, T entity) {
        if (entity.getId() != null) return entity.getId();

        String type = CollectionUtil.getLast(types);
        IdSpaceIdGenerator idGen = new IdSpaceIdGenerator(database, getTqlIdSpaceTable());
        return idGen.nextId(type);
    }

    // ==========================================
    // Schema management
    // ==========================================

    public void ensureSchema(UserContext context) {
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
            ensure(context, dbTableInfo, table, columns);
        });

        ensureInitData(context);
        ensureIdSpaceTable(context);
    }

    public void ensureIdSpaceTable(UserContext context) {
        List<Map<String, Object>> dbTableInfo;
        try {
            dbTableInfo = database.getTableColumns(getTqlIdSpaceTable());
        } catch (Exception e) {
            dbTableInfo = ListUtil.empty();
        }
        if (!ObjectUtil.isEmpty(dbTableInfo)) return;

        String sql = "CREATE TABLE " + getTqlIdSpaceTable() + " (\n"
                + "type_name varchar(100) NOT NULL PRIMARY KEY,\n"
                + "current_level bigint)\n";
        logInfo(sql + ";");
        if (ensureTableEnabled(context)) {
            try { database.execute(context, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    protected void ensure(UserContext context, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        if (tableInfo.isEmpty()) {
            createTable(context, table, columns);
            return;
        }
        Map<String, Map<String, Object>> fields = CollStreamUtil.toIdentityMap(
                tableInfo, m -> String.valueOf(m.get("column_name")).toLowerCase());
        for (SQLColumn column : columns) {
            String dbColumnName = column.getColumnName().toLowerCase();
            if (!fields.containsKey(dbColumnName)) {
                addColumn(context, column);
            }
        }
    }

    protected void createTable(UserContext context, String table, List<SQLColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table).append(" (\n");
        sb.append(columns.stream()
                .map(column -> {
                    String dbColumn = dialect.escapeIdentifier(column.getColumnName()) + " " + dialect.mapColumnType(column.getType());
                    if (column.isIdColumn()) dbColumn += " NOT NULL PRIMARY KEY";
                    return dbColumn;
                })
                .collect(Collectors.joining(",\n")));
        sb.append(")\n");
        logInfo(sb + ";");
        if (ensureTableEnabled(context)) {
            try { database.execute(context, sb.toString()); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    protected void addColumn(UserContext context, SQLColumn column) {
        String sql = StrUtil.format("ALTER TABLE {} ADD COLUMN {} {}",
                dialect.escapeIdentifier(column.getTableName()), dialect.escapeIdentifier(column.getColumnName()), dialect.mapColumnType(column.getType()));
        logInfo(sql + ";");
        if (ensureTableEnabled(context)) {
            try { database.execute(context, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    public void ensureInitData(UserContext context) {
        if (entityDescriptor.isRoot()) ensureRoot(context);
        if (entityDescriptor.isConstant()) ensureConstant(context);
    }

    private void ensureRoot(UserContext context) {
        List<Map<String, Object>> dbRow;
        try {
            dbRow = database.query(context,
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
            if (ensureTableEnabled(context)) {
                try { database.execute(context, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
            }
            return;
        }

        List<String> columns = new ArrayList<>();
        List<Object> rootRow = new ArrayList<>();
        for (PropertyDescriptor ownProperty : entityDescriptor.getOwnProperties()) {
            columns.add(getSqlColumn(ownProperty).getColumnName());
            rootRow.add(getRootPropertyValue(context, ownProperty));
        }
        String sql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})",
                tableName(entityDescriptor.getType()),
                CollectionUtil.join(columns, ","),
                CollectionUtil.join(rootRow, ",", value -> getSqlValue(value)));
        logInfo(sql + ";");
        if (ensureTableEnabled(context)) {
            try { database.execute(context, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
        }
    }

    private void ensureConstant(UserContext context) {
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
                    .map(p -> getConstantPropertyValue(context, p, i, code))
                    .collect(Collectors.toList());

            try {
                List<Map<String, Object>> existing = database.query(context,
                        StrUtil.format("SELECT * FROM {} WHERE id = '{}'",
                                tableName(entityDescriptor.getType()),
                                getConstantPropertyValue(context, entityDescriptor.findIdProperty(), i, code)),
                        new Object[0]);
                if (!existing.isEmpty()) {
                    long version = Long.parseLong(String.valueOf(existing.get(0).get("version")));
                    if (version > 0) continue;
                    String sql = StrUtil.format("UPDATE {} SET version = {} where id = '{}'",
                            tableName(entityDescriptor.getType()), -version,
                            getConstantPropertyValue(context, entityDescriptor.findIdProperty(), i, code));
                    logInfo(sql + ";");
                    if (ensureTableEnabled(context)) {
                        try { database.execute(context, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
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
            if (ensureTableEnabled(context)) {
                try { database.execute(context, sql); } catch (Exception e) { logInfo("Ignored: " + e.getMessage()); }
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

    private List<SQLData> convertToSQLData(UserContext context, T entity, PropertyDescriptor property, Object value) {
        return io.teaql.core.sql.portable.SQLPropertyUtil.toDBRaw(context, entity, value, property);
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
        if (value instanceof Boolean) return boolToSqlString(value);
        return StrUtil.wrapIfMissing(String.valueOf(value), "'", "'");
    }

    private Object getRootPropertyValue(UserContext context, PropertyDescriptor property) {
        if (property.isId()) return 1L;
        if (property.isVersion()) return 1L;
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) return context.evaluate(createFunction);
        return property.getAdditionalInfo().get("candidates");
    }

    private Object getConstantPropertyValue(UserContext context, PropertyDescriptor property, int index, String identifier) {
        if (property.isVersion()) return 1L;
        PropertyType type = property.getType();
        if (BaseEntity.class.isAssignableFrom(type.javaType())) return "1";
        String createFunction = property.getAdditionalInfo().get("createFunction");
        if (!ObjectUtil.isEmpty(createFunction)) return context.evaluate(createFunction);
        List<String> candidates = property.getCandidates();
        if (property.isIdentifier()) return identifier;
        if (ObjectUtil.isNotEmpty(candidates)) return CollectionUtil.get(candidates, index);
        if (property.isId()) return Math.abs((long) identifier.toUpperCase().hashCode());
        return null;
    }

    private long genIdForCandidateCode(String code) {
        return Math.abs((long) code.toUpperCase().hashCode());
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
        
        String offsetPlaceholder;
        if (slice.getOffset() == 0) {
            // Zero is a runtime-controlled pagination constant, not caller SQL.
            offsetPlaceholder = "0";
        }
        else {
            String offsetKey = "offset0";
            while (parameters.containsKey(offsetKey)) offsetKey += "_1";
            parameters.put(offsetKey, slice.getOffset());
            offsetPlaceholder = ":" + offsetKey;
        }
        
        return dialect.prepareParameterizedLimit(
                ":" + limitKey, offsetPlaceholder, !request.getOrderBy().isEmpty());
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
        userContext.putAttribute(MULTI_TABLE, tables.size() > 1);

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
                    String columnKey = findColumnKey(row, function.name());
                    item.addValue(function, columnKey == null ? null : row.get(columnKey));
                }
                for (SimpleNamedExpression dimension : request.getAggregations().getDimensions()) {
                    String columnKey = findColumnKey(row, dimension.name());
                    item.addDimension(dimension, columnKey == null ? null : row.get(columnKey));
                }
                return item;
            }).collect(Collectors.toList());
            result.setData(items);
            return result;
        } finally {
            userContext.putAttribute(MULTI_TABLE, preConfig);
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

    protected boolean ensureTableEnabled(UserContext context) {
        return context.getBool("ensureTable", true);
    }

    private void logInfo(String message) {
        System.out.println("[SQL-PORTABLE] " + message);
    }

    protected int toIntOrZero(Object cnt) {
        return cnt != null ? io.teaql.core.utils.Convert.convert(Integer.class, cnt) : 0;
    }

    protected io.teaql.core.EntityStatus resolvePersistedStatus(Long version) {
        return (version != null && version < 0)
                ? io.teaql.core.EntityStatus.PERSISTED_DELETED
                : io.teaql.core.EntityStatus.PERSISTED;
    }

    protected String boolToSqlString(Object value) {
        return ((Boolean) value) ? "1" : "0";
    }
}
