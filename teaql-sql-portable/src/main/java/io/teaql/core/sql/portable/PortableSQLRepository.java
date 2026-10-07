package io.teaql.core.sql.portable;

import java.math.BigDecimal;
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
import io.teaql.core.IdSetPaginationOptions;
import io.teaql.core.IdSetStore;
import io.teaql.core.RetainedIdSet;
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
    // Value-free, bounded hints for repeated authoritative reads of this fixed type.
    // Neither row values, actor policy nor mutation ownership enters this cache.
    private final MapProjection[] nativeReadbackProjections = new MapProjection[16];

    private static final Pattern NAMED_PARAM = Pattern.compile(":(\\w+)");
    public static final String CONTINUOUS_PAGE_PLAN = "teaql.continuousPage.plan";
    public static final String CONTINUOUS_PAGE_CURSOR_ID = "teaql.continuousPage.cursorId";
    public static final String ID_SET_PLAN = "teaql.idSet.plan";
    public static final String ID_SET_COUNT = "teaql.idSet.count";
    public static final String ID_SET_COUNT_ACCURACY = "teaql.idSet.countAccuracy";
    public static final String COMPILED_ROW_MAPPER = "teaql.sql.compiledRowMapper";
    private final ContinuousPageCursorStore defaultCursorStore =
            new InMemoryContinuousPageCursorStore();
    private final IdSetStore defaultIdSetStore = new InMemoryIdSetStore();
    private static final ConcurrentHashMap<String, IdSetBuildLock> ID_SET_BUILD_LOCKS = new ConcurrentHashMap<>();
    private static final class IdSetBuildLock {
        final Object monitor = new Object();
        final java.util.concurrent.atomic.AtomicInteger users = new java.util.concurrent.atomic.AtomicInteger(1);
    }

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
    private final EntityMetaFactory metadata;
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

    /**
     * A repository resolved by this data service can be embedded as a SQL
     * subquery when the nested request is explicitly unlimited. Bounded child
     * requests retain their materialized semantics because moving LIMIT into
     * an IN subquery can change which relation identities participate.
     */
    @Override
    public boolean canMixinSubQuery(UserContext userContext, SearchRequest subQuery) {
        return resolver != null && subQuery != null && subQuery.getSlice() == null;
    }

    public PortableSQLRepository(EntityDescriptor entityDescriptor, TeaQLDatabase database, PortableSQLRepositoryResolver resolver) {
        this(entityDescriptor, database, resolver, null);
    }

    public PortableSQLRepository(
            EntityDescriptor entityDescriptor,
            TeaQLDatabase database,
            PortableSQLRepositoryResolver resolver,
            EntityMetaFactory metadata) {
        this.entityDescriptor = entityDescriptor;
        this.database = database;
        this.resolver = resolver;
        this.metadata = metadata;
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
        if (parameters instanceof io.teaql.core.sql.SqlParameters tracked) {
            tracked.captureQueryContext(request);
        }
        String partitionProperty = request.getPartitionProperty();
        if (ObjectUtil.isNotEmpty(partitionProperty) && request.getSlice() != null) {
            ensureOrderByForPartition(request);
        }

        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        return compiler.buildDataSQL(sqlMetadata, this, userContext, request, parameters);
    }

    /** Framework-owned predicate lookup; keep all physical work on its request's collector. */
    public SmartList<Entity> materializeRelationPredicate(UserContext context, SearchRequest<?> child,
            SearchRequest<?> origin, String propertyName) {
        var source = SqlDiagnosticRequest.source(context, origin);
        var property = findProperty(propertyName);
        var lookup = property instanceof Relation
                ? SqlDiagnosticRequest.forRelation(child, source, origin, propertyName)
                : SqlDiagnosticRequest.forDerived(child, source, origin);
        return context.internalExecuteForList(lookup);
    }

    // ==========================================
    // Named parameter → positional parameter conversion
    // ==========================================

    private static class PositionalSQL {
        final String sql;
        final Object[] args;
        final SqlLogBindings logBindings;

        PositionalSQL(String sql, Object[] args, SqlLogBindings logBindings) {
            this.sql = sql;
            this.args = args;
            this.logBindings = logBindings;
        }
    }

    private record CompiledQueryPlan(
            String sql,
            int parameterCount,
            SqlLogBindings logBindings,
            io.teaql.core.CompiledRowMapper<?> rowMapper) {}

    private record ColumnBinding(
            int index,
            PropertyDescriptor property,
            EntityDescriptor relationDescriptor,
            int loadedPropertyIndex) {}

    private record QueryShape(String key, Object[] arguments) {}

    private PositionalSQL withQueryIntent(PositionalSQL sql, io.teaql.core.SqlIntentRedactions intent,
                                          SearchRequest<?> request) {
        if (intent == null) return sql;
        intent.capture(sql.logBindings.policies(), sql.args);
        return new PositionalSQL(sql.sql, sql.args, new SqlLogBindings(sql.logBindings.policies(),
                sql.logBindings.generated(), sql.logBindings.diagnosticSql(), intent.copy(),
                SqlDiagnosticRequest.statementTrace(request)));
    }

    private SqlLogBindings withMutationIntent(SqlLogBindings bindings, io.teaql.core.SqlIntentRedactions intent) {
        if (intent == null) return bindings;
        return new SqlLogBindings(bindings.policies(), bindings.generated(), bindings.diagnosticSql(), intent.copy(), bindings.executionTrace(), bindings.batchTraces());
    }

    private PositionalSQL toPositional(String namedSql, Map<String, Object> params) {
        List<Object> args = new ArrayList<>();
        List<io.teaql.core.SqlParameterLogPolicy> policies = new ArrayList<>();
        Matcher m = NAMED_PARAM.matcher(namedSql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String paramName = m.group(1);
            Object value = params.get(paramName);
            var policy = params instanceof io.teaql.core.sql.SqlParameters tracked
                    ? tracked.policy(paramName) : io.teaql.core.SqlParameterLogPolicy.UNKNOWN;
            if (params instanceof io.teaql.core.sql.SqlParameters tracked && !tracked.generated()
                    && policy != io.teaql.core.SqlParameterLogPolicy.CREDENTIAL)
                policy = io.teaql.core.SqlParameterLogPolicy.UNKNOWN;
            Collection<?> expandedValues = expandedParameterValues(value);
            if (expandedValues != null) {
                policies.addAll(java.util.Collections.nCopies(Math.max(1, expandedValues.size()), policy));
                appendExpandedParameter(expandedValues, args, m, sb);
                continue;
            }
            args.add(value);
            policies.add(policy);
            m.appendReplacement(sb, "?");
        }
        m.appendTail(sb);
        return new PositionalSQL(sb.toString(), args.toArray(), new SqlLogBindings(policies,
                params instanceof io.teaql.core.sql.SqlParameters tracked && tracked.generated()));
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

    private record IdSetExecution<E extends Entity>(
            SearchRequest<E> request, long[] pageIds, boolean optimized) {}

    @SuppressWarnings("unchecked")
    private IdSetExecution<T> prepareIdSetPage(UserContext context, SearchRequest<T> original,
            io.teaql.core.SqlIntentRedactions intent) {
        IdSetPaginationOptions options = original.idSetPaginationOptions();
        if (options == null) return idSetFallback(context, original, "ID_SET_DISABLED", null, "UNKNOWN");
        Slice slice = original.getSlice();
        if (slice == null || slice.getOffset() < 0 || slice.getSize() <= 0
                || original.continuousPageFetchOptions() != null
                || original.getPartitionProperty() != null || original.hasSimpleAgg()) {
            return idSetFallback(context, original, "ID_SET_FALLBACK_UNSUPPORTED_SHAPE", null, "UNKNOWN");
        }

        TempRequest working = new TempRequest(original);
        OrderBys stableOrders = new OrderBys();
        boolean hasId = false;
        if (original.getOrderBy() != null) {
            for (OrderBy order : original.getOrderBy().getOrderBys()) {
                if (!(order.getExpression() instanceof FunctionApply function)
                        || function.getOperator() != io.teaql.core.AggrFunction.SELF
                        || !(function.first() instanceof PropertyReference property)) {
                    return idSetFallback(context, original,
                            "ID_SET_FALLBACK_NON_DETERMINISTIC_ORDER", null, "UNKNOWN");
                }
                hasId |= "id".equals(property.getPropertyName());
                stableOrders.addOrderBy(order);
            }
        }
        if (!hasId) stableOrders.addOrderBy(new OrderBy("id"));
        working.setOrderBy(stableOrders);

        TempRequest idRequest = new TempRequest(working);
        idRequest.getProjections().clear();
        idRequest.selectProperty("id");
        idRequest.offset(0, options.maxIds() + 1);
        Map<String, Object> idParams = new io.teaql.core.sql.SqlParameters();
        String idSql = buildDataSQL(context, idRequest, idParams);
        if (ObjectUtil.isEmpty(idSql)) {
            return idSetFallback(context, original, "ID_SET_FALLBACK_UNSUPPORTED_SHAPE", null, "UNKNOWN");
        }
        String key = idSetQueryKey(context, working, options, idSql, idParams);
        // Capture current bindings even when retained IDs avoid executing the discovery query.
        PositionalSQL idStatement = withQueryIntent(toPositional(idSql, idParams), intent, idRequest);
        IdSetStore store = idSetStore(context);
        RetainedIdSet retained;
        try {
            retained = store.get(key).orElse(null);
        } catch (RuntimeException unavailable) {
            return idSetFallback(context, original, "ID_SET_FALLBACK_STORE_UNAVAILABLE", null, "UNKNOWN");
        }
        boolean hit = retained != null;
        if (retained == null) {
            IdSetBuildLock lock = ID_SET_BUILD_LOCKS.compute(key, (ignored, existing) -> {
                if (existing == null) return new IdSetBuildLock();
                existing.users.incrementAndGet(); return existing;
            });
            synchronized (lock.monitor) {
                try {
                    retained = store.get(key).orElse(null);
                    if (retained != null) hit = true;
                    else {
                        PositionalSQL positional = idStatement;
                        List<Map<String, Object>> rows = database.query(context, positional.sql, positional.args, positional.logBindings);
                        if (rows.size() > options.maxIds()) {
                            return idSetFallback(context, original,
                                    "ID_SET_FALLBACK_LIMIT_EXCEEDED",
                                    (long) options.maxIds() + 1, "LOWER_BOUND");
                        }
                        long[] ids = new long[rows.size()];
                        for (int i = 0; i < rows.size(); i++) {
                            Object value = rows.get(i).get("id");
                            if (!(value instanceof Number number) || number.longValue() < 0) {
                                return idSetFallback(context, original,
                                        "ID_SET_FALLBACK_UNSUPPORTED_SHAPE", null, "UNKNOWN");
                            }
                            ids[i] = number.longValue();
                        }
                        retained = new RetainedIdSet(key, ids,
                                Instant.now().plusSeconds(options.ttlSeconds()));
                        store.put(retained);
                    }
                } catch (RuntimeException unavailable) {
                    return idSetFallback(context, original,
                            "ID_SET_FALLBACK_STORE_UNAVAILABLE", null, "UNKNOWN");
                } finally {
                    if (lock.users.decrementAndGet() == 0) ID_SET_BUILD_LOCKS.remove(key, lock);
                }
            }
        }
        long[] all = retained.ids();
        int start = Math.min(slice.getOffset(), all.length);
        int end = Math.min(start + slice.getSize(), all.length);
        long[] pageIds = Arrays.copyOfRange(all, start, end);
        context.putAttribute(ID_SET_PLAN, hit ? "ID_SET_HIT" : "ID_SET_BUILD");
        context.putAttribute(ID_SET_COUNT, (long) all.length);
        context.putAttribute(ID_SET_COUNT_ACCURACY, "EXACT");
        TempRequest page = new TempRequest(working);
        page.offset(0, Math.max(1, pageIds.length));
        if (pageIds.length > 0) {
            page.appendSearchCriteria(page.createBasicSearchCriteria("id", Operator.IN, pageIds));
        }
        return new IdSetExecution<>((SearchRequest<T>) page, pageIds, true);
    }

    private IdSetExecution<T> idSetFallback(UserContext context, SearchRequest<T> request,
            String plan, Long count, String accuracy) {
        context.putAttribute(ID_SET_PLAN, plan);
        context.putAttribute(ID_SET_COUNT, count);
        context.putAttribute(ID_SET_COUNT_ACCURACY, accuracy);
        return new IdSetExecution<>(request, new long[0], false);
    }

    private IdSetStore idSetStore(UserContext context) {
        IdSetStore custom = context.capability(IdSetStore.class);
        return custom == null ? defaultIdSetStore : custom;
    }

    private String idSetQueryKey(UserContext context, SearchRequest<T> request,
            IdSetPaginationOptions options, String sql, Map<String, Object> parameters) {
        StringBuilder canonical = new StringBuilder(options.namespace())
                .append('|').append(request.getTypeName())
                .append("|route=").append(entityDescriptor.getDataService())
                .append("|source=").append(System.identityHashCode(database))
                .append('|').append(sql);
        new TreeMap<>(parameters).forEach((name, value) -> {
            if (!name.startsWith("limit") && !name.startsWith("offset")) {
                canonical.append('|').append(name).append('=').append(String.valueOf(value));
            }
        });
        observableOwner(context).forEach((name, value) ->
                canonical.append('|').append(name).append('=').append(value));
        if (context.activeRoot() != null) canonical.append("|root=").append(context.activeRoot());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return "teaql:id-set:v1:" + HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new TeaQLRuntimeException("SHA-256 unavailable", e);
        }
    }

    public SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {
        return loadWithIntent(userContext, request, SqlDiagnosticRequest.source(userContext, request));
    }

    SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request,
            io.teaql.core.SqlIntentRedactions intent) {
        // Preserve virtual dispatch through the existing provider customization hook.
        return loadInternal(userContext, intent == null ? request : SqlDiagnosticRequest.forExecution(request, intent));
    }

    private SmartList<T> loadWithIntent(UserContext userContext, SearchRequest<T> request,
            io.teaql.core.SqlIntentRedactions intent) {
        // Inspect the current typed request, including future child predicates, before
        // any SQL. Cached plans never own these values and binds are never rewritten.
        SqlLikeIntent.capture(userContext, request, this, intent);
        IdSetExecution<T> idSetExecution = prepareIdSetPage(userContext, request, intent);
        if (idSetExecution.optimized() && idSetExecution.pageIds().length == 0) {
            return SmartList.empty(request.returnType());
        }
        SearchRequest<T> requestedPage = idSetExecution.request();
        QueryShape shape = idSetExecution.optimized() ? null : simpleQueryShape(userContext, request);
        CompiledQueryPlan plan = shape == null ? null : compiledQueryPlans.get(shape.key());
        ContinuousPageExecution pageExecution;
        SearchRequest<T> executedRequest;
        PositionalSQL psql;
        if (plan != null && plan.parameterCount() == shape.arguments().length) {
            psql = new PositionalSQL(plan.sql(), shape.arguments(), plan.logBindings());
            pageExecution = fallback(userContext, request, null, "DISABLED");
            executedRequest = request;
        }
        else {
            Map<String, Object> params = new io.teaql.core.sql.SqlParameters();
            String sql = buildDataSQL(userContext, requestedPage, params);
            if (ObjectUtil.isEmpty(sql)) {
                return new SmartList<>();
            }
            pageExecution = idSetExecution.optimized()
                    ? fallback(userContext, requestedPage, null, "DISABLED")
                    : prepareContinuousPage(userContext, requestedPage, sql, params);
            executedRequest = pageExecution.request();
            if (pageExecution.request() != requestedPage) {
                params = new io.teaql.core.sql.SqlParameters();
                sql = buildDataSQL(userContext, executedRequest, params);
            }
            psql = toPositional(sql, params);
            if (shape != null && psql.args.length == shape.arguments().length) {
                CompiledQueryPlan candidate = new CompiledQueryPlan(
                        psql.sql,
                        psql.args.length,
                        psql.logBindings,
                        compileRowMapper(executedRequest));
                plan = cacheCompiledQueryPlan(shape.key(), candidate);
            }
        }
        // Attach only after inserting the reusable plan: no original values enter the plan cache.
        psql = withQueryIntent(psql, intent, request);
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
            List<T> entities = database.query(userContext, psql.sql, psql.args, mapper, psql.logBindings);
            if (entities.isEmpty() && ObjectUtil.isEmpty(request.getFacetRequests())) {
                registerContinuousPage(userContext, request, pageExecution, List.of());
                return SmartList.empty(request.returnType());
            }
            smartList = SmartList.takeOwnership(entities);
        }
        else {
            List<Map<String, Object>> rows = database.query(userContext, psql.sql, psql.args, psql.logBindings);
            if (rows.isEmpty() && ObjectUtil.isEmpty(request.getFacetRequests())) {
                registerContinuousPage(userContext, request, pageExecution, List.of());
                return SmartList.empty(request.returnType());
            }
            smartList = new SmartList<>(rows.size());
            MapProjection[] projections = new MapProjection[16];
            SimpleNamedExpression[] dynamicProperties = executedRequest.getSimpleDynamicProperties().toArray(SimpleNamedExpression[]::new);
            for (Map<String, Object> row : rows) {
                MapProjection projection = resolveMapProjection(executedRequest.returnType(), row, projections);
                smartList.add(mapRowToEntity(userContext, executedRequest, row, projection, dynamicProperties));
            }
        }
        registerContinuousPage(userContext, request, pageExecution, smartList.getData());
        if (idSetExecution.optimized()) {
            Map<Long, Integer> positions = new HashMap<>();
            long[] pageIds = idSetExecution.pageIds();
            for (int i = 0; i < pageIds.length; i++) positions.put(pageIds[i], i);
            smartList.getData().sort(java.util.Comparator.comparingInt(entity ->
                    positions.getOrDefault(entity.getId(), Integer.MAX_VALUE)));
        }
        
        java.util.List<io.teaql.core.FacetRequest> facetRequests = request.getFacetRequests();
        if (facetRequests != null && !facetRequests.isEmpty()) {
            io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
            for (io.teaql.core.FacetRequest facetRequest : facetRequests) {
                io.teaql.core.internal.TempRequest tr =
                        SqlDiagnosticRequest.forDerived(request, intent, request);
                tr.setAggregations(new io.teaql.core.Aggregations());
                tr.groupBy(facetRequest.getRelationName());
                tr.count("count");
                
                Map<String, Object> facetParams = new io.teaql.core.sql.SqlParameters();
                java.util.List<String> facetTables = compiler.collectAggregationTables(this.sqlMetadata, this, userContext, tr);
                String facetSql = compiler.buildAggregationSQL(this.sqlMetadata, this, userContext, tr, facetParams, facetTables);
                if (!io.teaql.core.utils.ObjectUtil.isEmpty(facetSql)) {
                    var facetIntent = intent == null ? null : intent.copy();
                    PositionalSQL psqlFacet = withQueryIntent(toPositional(facetSql, facetParams), facetIntent, request);
                    List<Map<String, Object>> facetRows = database.query(userContext, psqlFacet.sql, psqlFacet.args, psqlFacet.logBindings);
                    
                    SmartList<io.teaql.core.Entity> facetEntities = new SmartList<>();
                    io.teaql.core.SearchRequest<?> relationReq = facetRequest.getRequest();
                    if (relationReq != null) {
                        String relationType = relationReq.getTypeName();
                        PortableSQLRepository relationRepo = resolver.resolve(relationType);
                        if (relationRepo != null) {
                            List<Object> relIds = new ArrayList<>();
                            Map<Long, Object> idToCount = new HashMap<>();
                            for (Map<String, Object> facetRow : facetRows) {
                                Object relId = findFacetRelationValue(
                                        facetRow, facetRequest.getRelationName());
                                Object countVal = findColumnValue(facetRow, "count");
                                if (relId != null) {
                                    relIds.add(relId);
                                    idToCount.put(io.teaql.core.utils.Convert.convert(Long.class, relId), countVal);
                                }
                            }
                            io.teaql.core.internal.TempRequest fetchRelReq =
                                    SqlDiagnosticRequest.forRelation(
                                            relationReq, facetIntent, request, facetRequest.getRelationName());
                            if (facetRequest.isMergeCriteria()) {
                                // The count query already applied the source's
                                // filters. Its FK membership is the only valid
                                // restriction on the target: source predicates
                                // belong to another table/type (even "id").
                                fetchRelReq.appendSearchCriteria(fetchRelReq.createBasicSearchCriteria(
                                        BaseEntity.ID_PROPERTY, io.teaql.core.criteria.Operator.IN, relIds));
                            }
                            SmartList<?> loadedRels = relationRepo.loadInternal(userContext, fetchRelReq, facetIntent);
                            java.util.List<String> countAliases = relationReq.getAggregations().getAggregates()
                                    .stream().map(io.teaql.core.SimpleNamedExpression::name).toList();
                            if (countAliases.isEmpty()) countAliases = java.util.List.of("count");
                            for (Object obj : loadedRels) {
                                io.teaql.core.Entity rel = (io.teaql.core.Entity) obj;
                                Object cnt = idToCount.get(rel.getId());
                                int countInt = toIntOrZero(cnt);
                                if (rel instanceof io.teaql.core.BaseEntity) {
                                    for (String alias : countAliases) {
                                        ((io.teaql.core.BaseEntity) rel).addDynamicProperty(alias, countInt);
                                    }
                                }
                                facetEntities.add(rel);
                            }
                            // Materialization changes the row carrier, not the
                            // nested facet result. Preserve its collection-owned
                            // metadata without sharing the mutable map itself.
                            loadedRels.getFacets().forEach(facetEntities::addFacet);
                        }
                    }
                    smartList.addFacet(facetRequest.getFacetName(), facetEntities);
                }
            }
        }
        
        return smartList;
    }

    private CompiledQueryPlan cacheCompiledQueryPlan(String key, CompiledQueryPlan candidate) {
        // Misses compile outside the lock; only admission/eviction is atomic.
        // ConcurrentHashMap keeps the much more common hit path lock-free.
        synchronized (compiledQueryPlans) {
            CompiledQueryPlan existing = compiledQueryPlans.get(key);
            if (existing != null) return existing;
            if (compiledQueryPlans.size() >= MAX_COMPILED_QUERY_PLANS) compiledQueryPlans.clear();
            compiledQueryPlans.put(key, candidate);
            return candidate;
        }
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

        io.teaql.core.LoadState loadedShape = io.teaql.core.LoadState.projection(
                io.teaql.core.FieldLayout.forType(resultDescriptor.getTargetType()),
                selected.stream().map(PropertyDescriptor::getName).toList());
        ColumnBinding[] columns = bindings.toArray(ColumnBinding[]::new);

        return row -> {
            @SuppressWarnings("unchecked")
            T entity = (T) resultDescriptor.createEntity();
            BaseEntity base = (BaseEntity) entity;
            base.__internalUseLoadState(loadedShape);
            for (ColumnBinding binding : columns) {
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
        if (expression instanceof SQLExpressionParser) return false;
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
            key.append("N:").append(parameter.getName()).append(':');
            if (parameter.getOperator() != null)
                value = new io.teaql.core.sql.expression.ParameterParser().fixValue(parameter.getOperator(), value);
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

    /** Loaded private values may appear in intent even when absent from this write's bindings. */
    void captureMutationIntent(Entity entity, io.teaql.core.SqlIntentRedactions intent) {
        if (!(entity instanceof BaseEntity base)) return;
        for (PropertyDescriptor property : allProperties) {
            if (property instanceof Relation || !shouldHandle(property)) continue;
            String name = property.getName();
            var policy = List.of(parameterLogPolicy(name));
            if (base.isPropertyLoaded(name)) intent.capture(policy, new Object[] {base.getProperty(name)});
            if (base.getUpdatedProperties().contains(name))
                intent.capture(policy, new Object[] {base.getOldValue(name)});
        }
    }

    @SuppressWarnings("unchecked")
    public T loadPersistedById(UserContext userContext, Long id) {
        return loadPersistedById(userContext, id, null);
    }

    @SuppressWarnings("unchecked")
    T loadPersistedById(UserContext userContext, Long id, io.teaql.core.SqlIntentRedactions intent) {
        return loadPersistedById(userContext, id, intent, null);
    }

    @SuppressWarnings("unchecked")
    T loadPersistedById(UserContext userContext, Long id, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace trace) {
        String primaryTable = thisPrimaryTableName != null
                ? thisPrimaryTableName : tableName(entityDescriptor.getType());
        String sql = "SELECT * FROM " + escapeIdentifier(primaryTable)
                + " WHERE " + escapeIdentifier("id") + " = ?";
        Object[] arguments = new Object[]{id};
        var bindings = new SqlLogBindings(List.of(parameterLogPolicy("id")), true, null, intent, trace);
        if (types.size() > 1) {
            // An authoritative inherited view needs all participating tables,
            // including the root version and tombstones. Reuse the normal AST
            // compiler rather than copying the proposed values into readback.
            var readback = new io.teaql.core.internal.TempRequest(entityDescriptor.getTargetType(), entityDescriptor.getType()) {
                { withDeletedRows(); }
            };
            for (PropertyDescriptor property : allProperties) {
                if (shouldHandle(property)) readback.selectProperty(property.getName());
            }
            readback.appendSearchCriteria(new io.teaql.core.criteria.EQ(
                    new io.teaql.core.PropertyReference("id"), new io.teaql.core.Parameter("readbackId", id, io.teaql.core.criteria.Operator.EQUAL)));
            // Explicit version intent prevents the compiler's normal visible-row
            // default from hiding the negative tombstone we just committed.
            readback.appendSearchCriteria(readback.createBasicSearchCriteria("version", io.teaql.core.criteria.Operator.IS_NOT_NULL));
            readback.setSize(1);
            Map<String, Object> parameters = new io.teaql.core.sql.SqlParameters();
            var statement = toPositional(buildDataSQL(userContext, readback, parameters), parameters);
            sql = statement.sql;
            arguments = statement.args;
            bindings = new SqlLogBindings(statement.logBindings.policies(), true, null, intent, trace);
        }
        List<Map<String, Object>> rows = database.query(userContext, sql, arguments, bindings);
        if (rows.size() != 1) {
            throw new TeaQLRuntimeException(
                    "Persisted " + entityDescriptor.getType() + "(" + id + ") could not be read back");
        }
        T entity = (T) entityDescriptor.createEntity();
        Map<String, Object> row = rows.get(0);
        // SELECT * returns physical column names, unlike the aliased normal Q path.
        // Install the actual geometry once before setters hydrate native values.
        MapProjection projection;
        synchronized (nativeReadbackProjections) {
            projection = resolveMapProjection(entityDescriptor.getTargetType(), row, nativeReadbackProjections);
        }
        if (entity instanceof BaseEntity base && projection.loadedState() != null) {
            base.__internalUseLoadState(projection.loadedState());
        }
        for (PropertyDescriptor property : this.allProperties) {
            if (!shouldHandle(property)) continue;
            String columnKey = findColumnKey(row, property);
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
        var intent = SqlDiagnosticRequest.source(userContext, request);
        SqlLikeIntent.capture(userContext, request, this, intent);
        Map<String, Object> params = new io.teaql.core.sql.SqlParameters();
        String sql = buildDataSQL(userContext, request, params);
        if (ObjectUtil.isEmpty(sql)) return Stream.empty();
        PositionalSQL psql = withQueryIntent(toPositional(sql, params),
                intent, request);
        if (database.supportsCompiledStreamMapping()) {
            Object extension = request.getExtension(COMPILED_ROW_MAPPER);
            @SuppressWarnings("unchecked")
            io.teaql.core.CompiledRowMapper<T> mapper = extension instanceof io.teaql.core.CompiledRowMapper<?> supplied
                    ? (io.teaql.core.CompiledRowMapper<T>) supplied : compileRowMapper(request);
            if (mapper != null) {
                return database.queryForStream(userContext, psql.sql, psql.args, mapper, psql.logBindings);
            }
        }
        MapProjection[] projections = new MapProjection[16];
        SimpleNamedExpression[] dynamicProperties = request.getSimpleDynamicProperties().toArray(SimpleNamedExpression[]::new);
        return database.queryForStream(userContext, psql.sql, psql.args, psql.logBindings)
                .map(row -> {
                    MapProjection shape = resolveMapProjection(request.returnType(), row, projections);
                    return mapRowToEntity(userContext, request, row, shape, dynamicProperties);
                });
    }

    private record MapBinding(PropertyDescriptor property, String column) {}

    private record MapProjection(String[] columns, io.teaql.core.LoadState loadedState, MapBinding[] bindings) {
        boolean matches(Map<String, Object> row) {
            if (row.size() != columns.length) return false;
            for (String column : columns) if (!row.containsKey(column)) return false;
            return true;
        }
    }

    private MapProjection resolveMapProjection(Class<? extends Entity> type, Map<String, Object> row,
            MapProjection[] projections) {
        int empty = -1;
        for (int i = 0; i < projections.length; i++) {
            MapProjection shape = projections[i];
            if (shape != null && shape.matches(row)) return shape;
            if (shape == null && empty < 0) empty = i;
        }
        MapProjection shape = mapProjection(type, row);
        // Query-owned, bounded, value-free hints. Concurrent stream writes may
        // replace a hint, but each row keeps its own validated local shape.
        projections[empty < 0 ? projections.length - 1 : empty] = shape;
        return shape;
    }

    private MapProjection mapProjection(Class<? extends Entity> type, Map<String, Object> row) {
        io.teaql.core.LoadState loaded = null;
        List<String> selected = new ArrayList<>();
        List<MapBinding> bindings = new ArrayList<>();
        for (PropertyDescriptor property : allProperties) {
            if (!shouldHandle(property)) continue;
            String column = findColumnKey(row, property);
            if (column != null) {
                selected.add(property.getName());
                bindings.add(new MapBinding(property, column));
            }
        }
        if (BaseEntity.class.isAssignableFrom(type)) {
            loaded = io.teaql.core.LoadState.projection(io.teaql.core.FieldLayout.forType(type), selected);
        }
        return new MapProjection(row.keySet().toArray(String[]::new), loaded, bindings.toArray(MapBinding[]::new));
    }

    private T mapRowToEntity(UserContext userContext, SearchRequest<T> request, Map<String, Object> row,
            MapProjection projection, SimpleNamedExpression[] dynamicProperties) {
        Class<? extends T> returnType = request.returnType();
        T entity = createEntity(returnType);
        if (entity instanceof BaseEntity base && projection.loadedState() != null) base.__internalUseLoadState(projection.loadedState());
        for (MapBinding binding : projection.bindings()) {
            PropertyDescriptor property = binding.property();
            String columnKey = binding.column();
            if (!(property instanceof Relation)) {
                Object value = row.get(columnKey);
                Class targetType = property.getType().javaType();
                entity.setProperty(
                        property.getName(),
                        value == null
                                ? null
                                : convertColumnValue(targetType, value));
            } else if (property instanceof Relation) {
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
                    throw new TeaQLRuntimeException(
                            "Failed to hydrate selected relation "
                                    + entityDescriptor.getType() + "." + property.getName(), e);
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
        for (SimpleNamedExpression dp : dynamicProperties) {
            Object value = row.get(dp.name());
            if (row.containsKey(dp.name())) entity.addDynamicProperty(dp.name(), value);
        }

        return entity;
    }

    private String findColumnKey(Map<String, Object> row, PropertyDescriptor property) {
        String member = findColumnKey(row, property.getName());
        if (member != null) return member;
        SQLColumn column = getSqlColumn(property);
        return column == null ? null : findColumnKey(row, column.getColumnName());
    }

    private String findColumnKey(Map<String, Object> row, String propertyName) {
        if (row.containsKey(propertyName)) return propertyName;
        for (String key : row.keySet()) {
            if (key != null && key.equalsIgnoreCase(propertyName)) return key;
        }
        return null;
    }

    static Object convertTemporalColumnValue(Class<?> targetType, Object value) {
        if ((targetType == java.time.LocalDateTime.class || targetType == java.time.LocalDate.class
                || targetType == java.time.LocalTime.class) && targetType.isInstance(value)) {
            return value;
        }
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
            return java.time.LocalDate.parse(text.substring(0, Math.min(10, text.length())));
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
        if (metadata == null) {
            throw new IllegalStateException("Repository for " + entityDescriptor.getType()
                    + " has no runtime metadata snapshot for relation target "
                    + entityType.getName()
                    + "; schema-only repositories cannot hydrate relations");
        }
        for (EntityDescriptor descriptor : metadata.allEntityDescriptors()) {
            if (descriptor.getTargetType() == entityType) {
                return descriptor;
            }
        }
        throw new IllegalStateException("No entity descriptor registered for " + entityType.getName());
    }

    public void createInternal(UserContext userContext, Collection<T> createItems) {
        createInternal(userContext, createItems, null);
    }

    void createInternal(UserContext userContext, Collection<T> createItems, io.teaql.core.SqlIntentRedactions intent) {
        createInternal(userContext, createItems, intent, null);
    }

    void createInternal(UserContext userContext, Collection<T> createItems, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace trace) {
        createRows(userContext, new ArrayList<>(createItems), intent, trace, List.of());
    }

    void createBatchInternal(UserContext userContext, List<T> createItems, io.teaql.core.SqlIntentRedactions intent,
            List<io.teaql.core.SqlExecutionTrace> traces) {
        if (createItems.size() != traces.size()) {
            throw new IllegalArgumentException("Insert batch requires one trace per entity");
        }
        createRows(userContext, createItems, intent, null, List.copyOf(traces));
    }

    private record InsertShape(String table, List<String> columns) {
        private InsertShape { columns = List.copyOf(columns); }
    }

    private record InsertRow(Object[] values, SQLEntity entity, io.teaql.core.SqlExecutionTrace trace) {}

    private void createRows(UserContext userContext, List<T> createItems, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace fallback, List<io.teaql.core.SqlExecutionTrace> traces) {
        if (intent != null) createItems.forEach(item -> intent.captureTargetId(item.getId()));
        Map<InsertShape, List<InsertRow>> rows = new java.util.LinkedHashMap<>();
        for (int index = 0; index < createItems.size(); index++) {
            SQLEntity entity = convertToSQLEntityForInsert(userContext, createItems.get(index));
            Map<String, List> tableColumnValues = entity.getTableColumnValues();
            for (Map.Entry<String, List> entry : tableColumnValues.entrySet()) {
                String k = entry.getKey();
                List v = entry.getValue();
                if (auxiliaryTableNames.contains(k) && entity.allNullExceptID(v)) continue;
                var shape = new InsertShape(k, entity.getTableColumnNames().get(k));
                rows.computeIfAbsent(shape, key -> new ArrayList<>()).add(
                        new InsertRow(v.toArray(), entity, traces.isEmpty() ? fallback : traces.get(index)));
            }
        }
        // Capture every sibling/table before any statement is emitted. A root reason
        // can mention a masked value belonging to a later member of this batch.
        if (intent != null) rows.forEach((shape, members) -> {
            var policies = logBindings(shape.table(), shape.columns()).policies();
            members.forEach(member -> intent.capture(policies, member.values()));
        });
        var shapes = new ArrayList<>(rows.keySet());
        shapes.sort(java.util.Comparator.comparingInt((InsertShape shape) -> shape.table().equals(versionTableName) ? 0 : 1)
                .thenComparing(InsertShape::table).thenComparing(shape -> String.join(",", shape.columns())));
        for (InsertShape shape : shapes) {
            List<InsertRow> members = rows.get(shape);
            SQLEntity first = members.get(0).entity();
            io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
            String sql = compiler.buildInsertSQL(this, shape.table(), shape.columns(), first.getTraceChain());
            var bindings = logBindings(shape.table(), shape.columns(), sql, first.getTraceChain());
            bindings = traces.isEmpty() ? bindings.withTrace(fallback)
                    : bindings.withBatchTraces(members.stream().map(InsertRow::trace).toList());
            database.batchUpdate(userContext, sql, members.stream().map(InsertRow::values).toList(),
                    withMutationIntent(bindings, intent));
        }
    }

    private record PreparedWriteShape(String table, String sql, List<String> bindingColumns,
            boolean optimistic, boolean exactlyOne) {
        private PreparedWriteShape { bindingColumns = List.copyOf(bindingColumns); }
    }

    private record PreparedWriteRow(Object[] values, io.teaql.core.SqlExecutionTrace trace) {}

    void updateBatchInternal(UserContext context, List<T> entities, io.teaql.core.SqlIntentRedactions intent,
            List<io.teaql.core.SqlExecutionTrace> traces) {
        requireMemberTraces(entities, traces);
        Map<PreparedWriteShape, List<PreparedWriteRow>> rows = new java.util.LinkedHashMap<>();
        var compiler = new io.teaql.core.sql.SqlAstCompiler();
        for (int index = 0; index < entities.size(); index++) {
            T entity = entities.get(index);
            if (intent != null) intent.captureTargetId(entity.getId());
            SQLEntity converted = convertToSQLEntityForUpdate(context, entity);
            boolean versionUpdated = false;
            if (converted != null) {
                for (var entry : converted.getTableColumnValues().entrySet()) {
                    String table = entry.getKey();
                    List<String> columns = new ArrayList<>(converted.getTableColumnNames().get(table));
                    List<Object> values = new ArrayList<>(entry.getValue());
                    List<String> bindings = new ArrayList<>(columns);
                    String sql;
                    boolean optimistic = table.equals(versionTableName);
                    boolean primary = primaryTableNames.contains(table);
                    if (optimistic) {
                        versionUpdated = true;
                        columns.add("version");
                        bindings.add("version"); bindings.add("id"); bindings.add("version");
                        values.add(entity.getVersion() + 1); values.add(entity.getId()); values.add(entity.getVersion());
                        sql = compiler.buildUpdateVersionSQL(this, table, columns, null);
                    } else if (primary) {
                        bindings.add("id"); values.add(entity.getId());
                        sql = compiler.buildUpdatePrimarySQL(this, table, columns, null);
                    } else {
                        sql = dialect.buildSubsidiaryInsertSql(table, columns);
                    }
                    var shape = new PreparedWriteShape(table, sql, bindings, optimistic, optimistic || primary);
                    rows.computeIfAbsent(shape, ignored -> new ArrayList<>()).add(
                            new PreparedWriteRow(values.toArray(), traces.get(index)));
                }
            }
            if (!versionUpdated) {
                var shape = new PreparedWriteShape(versionTableName,
                        compiler.buildUpdateVersionTableVersionSQL(this, versionTableName),
                        List.of("version", "id", "version"), true, true);
                rows.computeIfAbsent(shape, ignored -> new ArrayList<>()).add(new PreparedWriteRow(
                        new Object[]{entity.getVersion() + 1, entity.getId(), entity.getVersion()}, traces.get(index)));
            }
        }
        executePreparedWrites(context, rows, intent);
    }

    void deleteBatchInternal(UserContext context, List<T> entities, io.teaql.core.SqlIntentRedactions intent,
            List<io.teaql.core.SqlExecutionTrace> traces) {
        versionBatchInternal(context, entities, intent, traces, false);
    }

    void recoverBatchInternal(UserContext context, List<T> entities, io.teaql.core.SqlIntentRedactions intent,
            List<io.teaql.core.SqlExecutionTrace> traces) {
        versionBatchInternal(context, entities, intent, traces, true);
    }

    private void versionBatchInternal(UserContext context, List<T> entities, io.teaql.core.SqlIntentRedactions intent,
            List<io.teaql.core.SqlExecutionTrace> traces, boolean recover) {
        requireMemberTraces(entities, traces);
        var compiler = new io.teaql.core.sql.SqlAstCompiler();
        var shape = new PreparedWriteShape(versionTableName,
                recover ? compiler.buildRecoverSQL(this, versionTableName) : compiler.buildDeleteSQL(this, versionTableName),
                List.of("version", "id", "version"), true, true);
        List<PreparedWriteRow> members = new ArrayList<>();
        for (int index = 0; index < entities.size(); index++) {
            T entity = entities.get(index);
            Long version = entity.getVersion();
            if (version == null || (recover ? version >= 0 : version <= 0)) {
                throw new IllegalArgumentException("Delete/recover batch requires the matching persisted version sign");
            }
            if (intent != null) intent.captureTargetId(entity.getId());
            members.add(new PreparedWriteRow(new Object[]{recover ? -version + 1 : -(version + 1),
                    entity.getId(), version}, traces.get(index)));
        }
        executePreparedWrites(context, Map.of(shape, members), intent);
    }

    private void requireMemberTraces(List<T> entities, List<io.teaql.core.SqlExecutionTrace> traces) {
        if (entities.size() != traces.size()) throw new IllegalArgumentException("Write batch requires one trace per entity");
        if (traces.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("Write batch trace must not be null");
    }

    private void executePreparedWrites(UserContext context,
            Map<PreparedWriteShape, List<PreparedWriteRow>> rows, io.teaql.core.SqlIntentRedactions intent) {
        // Capture the complete planned batch before emitting any table's statement.
        if (intent != null) rows.forEach((shape, members) -> {
            var policies = logBindings(shape.table(), shape.bindingColumns()).policies();
            members.forEach(member -> intent.capture(policies, member.values()));
        });
        var shapes = new ArrayList<>(rows.keySet());
        shapes.sort(java.util.Comparator.comparingInt((PreparedWriteShape shape) -> shape.optimistic() ? 0 : 1)
                .thenComparing(PreparedWriteShape::table).thenComparing(PreparedWriteShape::sql));
        for (var shape : shapes) {
            var members = rows.get(shape);
            if (members.isEmpty()) continue;
            var bindings = logBindings(shape.table(), shape.bindingColumns())
                    .withBatchTraces(members.stream().map(PreparedWriteRow::trace).toList());
            int[] counts = database.batchUpdate(context, shape.sql(), members.stream().map(PreparedWriteRow::values).toList(),
                    withMutationIntent(bindings, intent));
            if (counts.length != members.size()) throw new TeaQLRuntimeException("Prepared mutation returned an incomplete row-count array");
            if (shape.exactlyOne()) {
                for (int count : counts) {
                    if (count == 1) continue;
                    if (count == java.sql.Statement.SUCCESS_NO_INFO) {
                        throw new TeaQLRuntimeException("Prepared mutation requires an exact per-item affected-row count");
                    }
                    if (shape.optimistic()) throw new ConcurrentModifyException();
                    throw new TeaQLRuntimeException("primary table update failed");
                }
            }
        }
    }

    public void updateInternal(UserContext userContext, Collection<T> updateItems) {
        updateInternal(userContext, updateItems, null);
    }

    void updateInternal(UserContext userContext, Collection<T> updateItems, io.teaql.core.SqlIntentRedactions intent) {
        updateInternal(userContext, updateItems, intent, null);
    }

    void updateInternal(UserContext userContext, Collection<T> updateItems, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace trace) {
        if (intent != null) updateItems.forEach(item -> intent.captureTargetId(item.getId()));
        if (ObjectUtil.isEmpty(updateItems)) return;
        List<SQLEntity> sqlEntities = CollectionUtil.map(updateItems,
                i -> convertToSQLEntityForUpdate(userContext, i), true);
        if (ObjectUtil.isEmpty(sqlEntities)) return;

        for (SQLEntity sqlEntity : sqlEntities) {
            // A version-only graph participant (for example a dynamic-field
            // edit) still needs the optimistic guard below, even without native
            // column assignments. Null conversions, not empty column maps,
            // represent entities with no update intent.
            Map<String, List<String>> tableColumnNames = sqlEntity.getTableColumnNames();
            Map<String, List> tableColumnValues = sqlEntity.getTableColumnValues();

            AtomicBoolean versionTableUpdated = new AtomicBoolean(false);
            tableColumnValues.forEach((k, v) -> {
                List<String> columns = new ArrayList<>(tableColumnNames.get(k));
                List l = new ArrayList(v);
                boolean versionTable = this.versionTableName.equals(k);
                boolean primaryTable = this.primaryTableNames.contains(k);

                if (versionTable) {
                    updateVersionTable(userContext, sqlEntity, versionTableUpdated, k, columns, l, intent, trace);
                    return;
                }
                if (primaryTable) {
                    updatePrimaryTable(userContext, sqlEntity, k, columns, l, intent, trace);
                    return;
                }
                String updateSql = dialect.buildSubsidiaryInsertSql(k, columns);
                var bindings = logBindings(k, columns).withTrace(trace);
                if (intent != null) intent.capture(bindings.policies(), l.toArray());
                database.executeUpdate(userContext, updateSql, l.toArray(), withMutationIntent(bindings, intent));
            });

            if (!versionTableUpdated.get()) {
                updateVersionTableVersion(userContext, sqlEntity, intent, trace);
            }
        }
    }

    private void updateVersionTableVersion(UserContext userContext, SQLEntity sqlEntity, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace trace) {
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionTableVersionSQL(this, this.versionTableName);
        Object[] parameters = {sqlEntity.getVersion() + 1, sqlEntity.getId(), sqlEntity.getVersion()};
        var bindings = logBindings(this.versionTableName, List.of("version", "id", "version")).withTrace(trace);
        if (intent != null) intent.capture(bindings.policies(), parameters);
        int update = database.executeUpdate(userContext, updateSql, parameters, withMutationIntent(bindings, intent));
        if (update != 1) throw new ConcurrentModifyException();
    }

    private void updatePrimaryTable(UserContext userContext, SQLEntity sqlEntity, String k, List<String> columns, List l,
            io.teaql.core.SqlIntentRedactions intent, io.teaql.core.SqlExecutionTrace trace) {
        l.add(sqlEntity.getId());
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdatePrimarySQL(this, k, columns, sqlEntity.getTraceChain());
        List<String> bindings = new ArrayList<>(columns); bindings.add("id");
        var policies = logBindings(k, bindings, updateSql, sqlEntity.getTraceChain()).withTrace(trace);
        if (intent != null) intent.capture(policies.policies(), l.toArray());
        int update = database.executeUpdate(userContext, updateSql, l.toArray(), withMutationIntent(policies, intent));
        if (update != 1) throw new TeaQLRuntimeException("primary table update failed");
    }

    private void updateVersionTable(UserContext userContext, SQLEntity sqlEntity,
                                     AtomicBoolean versionTableUpdated, String k, List<String> columns, List l,
                                     io.teaql.core.SqlIntentRedactions intent, io.teaql.core.SqlExecutionTrace trace) {
        versionTableUpdated.set(true);
        columns.add("version");
        l.add(sqlEntity.getVersion() + 1);
        l.add(sqlEntity.getId());
        l.add(sqlEntity.getVersion());
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildUpdateVersionSQL(this, k, columns, sqlEntity.getTraceChain());
        List<String> bindings = new ArrayList<>(columns); bindings.add("id"); bindings.add("version");
        var policies = logBindings(k, bindings, updateSql, sqlEntity.getTraceChain()).withTrace(trace);
        if (intent != null) intent.capture(policies.policies(), l.toArray());
        int update = database.executeUpdate(userContext, updateSql, l.toArray(), withMutationIntent(policies, intent));
        if (update != 1) throw new ConcurrentModifyException();
    }

        public void deleteInternal(UserContext userContext, Collection<T> entities) {
        deleteInternal(userContext, entities, null);
    }

    void deleteInternal(UserContext userContext, Collection<T> entities, io.teaql.core.SqlIntentRedactions intent) {
        deleteInternal(userContext, entities, intent, null);
    }

    void deleteInternal(UserContext userContext, Collection<T> entities, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace trace) {
        if (intent != null) entities.forEach(item -> intent.captureTargetId(item.getId()));
        if (ObjectUtil.isEmpty(entities)) return;
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() > 0)
                .map(e -> new Object[]{-(e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        var bindings = logBindings(this.versionTableName, List.of("version", "id", "version")).withTrace(trace);
        if (intent != null) for (Object[] row : args) intent.capture(bindings.policies(), row);
        int[] rets = database.batchUpdate(userContext, updateSql, args, withMutationIntent(bindings, intent));
        for (int ret : rets) {
            if (ret != 1) throw new ConcurrentModifyException();
        }
    }

        public void recoverInternal(UserContext userContext, Collection<T> entities) {
        recoverInternal(userContext, entities, null);
    }

    void recoverInternal(UserContext userContext, Collection<T> entities, io.teaql.core.SqlIntentRedactions intent) {
        recoverInternal(userContext, entities, intent, null);
    }

    void recoverInternal(UserContext userContext, Collection<T> entities, io.teaql.core.SqlIntentRedactions intent,
            io.teaql.core.SqlExecutionTrace trace) {
        if (intent != null) entities.forEach(item -> intent.captureTargetId(item.getId()));
        if (ObjectUtil.isEmpty(entities)) return;
        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        String updateSql = compiler.buildDeleteSQL(this, this.versionTableName);
        List<Object[]> args = entities.stream()
                .filter(e -> e.getVersion() < 0)
                .map(e -> new Object[]{(-e.getVersion() + 1), e.getId(), e.getVersion()})
                .collect(Collectors.toList());
        var bindings = logBindings(this.versionTableName, List.of("version", "id", "version")).withTrace(trace);
        if (intent != null) for (Object[] row : args) intent.capture(bindings.policies(), row);
        int[] rets = database.batchUpdate(userContext, updateSql, args, withMutationIntent(bindings, intent));
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

    /**
     * Reconciles physical database objects only. Generated runtime modules use
     * this boundary before creating roots and constants through audited typed
     * mutations. It deliberately performs no application-data writes.
     */
    public void ensurePhysicalSchema(UserContext context) {
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
                throw schemaFailure("inspect", table, e);
            }
            ensure(context, dbTableInfo, table, columns);
        });

        ensureCanonicalRelationIndexes(context);

        ensureIdSpaceTable(context);
    }

    /**
     * Ensure the canonical index used by both per-parent window queries and
     * bounded probes. A model-specific ordering still requires an explicitly
     * declared matching index; this only covers the stable relation/id shape.
     */
    private void ensureCanonicalRelationIndexes(UserContext context) {
        PropertyDescriptor idProperty = entityDescriptor.findIdProperty();
        if (idProperty == null) return;
        SQLColumn idColumn = getSqlColumn(idProperty);
        for (PropertyDescriptor property : entityDescriptor.getOwnProperties()) {
            if (!(property instanceof Relation relation)) continue;
            if (relation.getRelationKeeper() != entityDescriptor) continue;
            SQLColumn relationColumn = getSqlColumn(property);
            if (!ObjectUtil.equals(relationColumn.getTableName(), idColumn.getTableName())) continue;

            String table = relationColumn.getTableName();
            String indexName = canonicalRelationIndexName(
                    table, relationColumn.getColumnName(), idColumn.getColumnName());
            try {
                if (database.indexExists(context, table, indexName).orElse(false)) continue;
            } catch (Exception failure) {
                throw canonicalRelationIndexFailure("inspect", table, indexName, failure);
            }
            String sql = "CREATE INDEX " + dialect.escapeIdentifier(indexName)
                    + " ON " + dialect.escapeIdentifier(table)
                    + " (" + dialect.escapeIdentifier(relationColumn.getColumnName())
                    + ", " + dialect.escapeIdentifier(idColumn.getColumnName()) + " DESC)";
            logInfo(sql + ";");
            if (ensureTableEnabled(context)) {
                try {
                    database.execute(context, sql);
                } catch (Exception failure) {
                    if (SchemaExceptionClassifier.isDuplicateIndex(failure)) {
                        try {
                            // PostgreSQL index names are schema-wide. A duplicate name on a
                            // different table must not be mistaken for this index being present.
                            if (database.indexExists(context, table, indexName).orElse(true)) {
                                continue;
                            }
                        } catch (Exception inspectionFailure) {
                            failure.addSuppressed(inspectionFailure);
                        }
                    }
                    throw canonicalRelationIndexFailure("create", table, indexName, failure);
                }
            }
        }
    }

    private IllegalStateException canonicalRelationIndexFailure(
            String operation, String table, String indexName, Exception failure) {
        return new IllegalStateException(
                "Failed to " + operation + " canonical relation index '" + indexName
                        + "' for entity '" + entityDescriptor.getType()
                        + "' on table '" + table + "'",
                failure);
    }

    private String canonicalRelationIndexName(String table, String relation, String id) {
        String raw = "idx_" + table + "_" + relation + "_" + id;
        if (raw.length() <= 30) return raw;
        String hash = Integer.toUnsignedString(raw.hashCode(), 36);
        return raw.substring(0, Math.max(1, 29 - hash.length())) + "_" + hash;
    }

    public void ensureIdSpaceTable(UserContext context) {
        List<Map<String, Object>> dbTableInfo;
        try {
            dbTableInfo = database.getTableColumns(getTqlIdSpaceTable());
        } catch (Exception e) {
            throw schemaFailure("inspect", getTqlIdSpaceTable(), e);
        }
        if (!ObjectUtil.isEmpty(dbTableInfo)) return;

        String sql = "CREATE TABLE " + getTqlIdSpaceTable() + " (\n"
                + "type_name varchar(100) NOT NULL PRIMARY KEY,\n"
                + "current_level bigint)\n";
        logInfo(sql + ";");
        if (ensureTableEnabled(context)) {
            try {
                database.execute(context, sql);
            } catch (Exception e) {
                throw schemaFailure("create", getTqlIdSpaceTable(), e);
            }
        }
    }

    protected void ensure(UserContext context, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        if (tableInfo.isEmpty()) {
            createTable(context, table, columns);
            return;
        }
        Map<String, Map<String, Object>> fields = CollStreamUtil.toIdentityMap(
                tableInfo, m -> metadataColumnName(m, table));
        for (SQLColumn column : columns) {
            String dbColumnName = column.getColumnName().toLowerCase();
            if (!fields.containsKey(dbColumnName)) {
                addColumn(context, column);
            } else {
                ensureCompatibleColumn(table, column, fields.get(dbColumnName));
            }
        }
    }

    private void ensureCompatibleColumn(
            String table, SQLColumn column, Map<String, Object> databaseColumn) {
        Object actualTypeValue = metadataValue(databaseColumn, "type_name", "data_type");
        if (actualTypeValue == null || actualTypeValue instanceof Number) return;

        String expectedType = dialect.mapColumnType(column.getType());
        String actualType = String.valueOf(actualTypeValue);
        if (!dialect.isCompatibleColumnType(expectedType, actualType)) {
            throw incompatibleColumn(table, column,
                    "expected type " + expectedType + " but database reports " + actualType);
        }

        ensureCompatibleNullability(table, column, databaseColumn);
        ensureCompatibleValueDomain(table, column, expectedType, actualType, databaseColumn);
    }

    private void ensureCompatibleNullability(
            String table, SQLColumn column, Map<String, Object> databaseColumn) {
        Object nullable = metadataValue(databaseColumn, "nullable", "is_nullable");
        if (nullable == null) return;
        boolean expectedNullable = !column.isIdColumn() && !column.isRequired();
        boolean actualNullable = nullableMetadata(nullable);
        if (expectedNullable != actualNullable) {
            throw incompatibleColumn(table, column,
                    "expected nullable=" + expectedNullable
                            + " but database reports nullable=" + actualNullable);
        }
    }

    private void ensureCompatibleValueDomain(
            String table,
            SQLColumn column,
            String expectedType,
            String actualType,
            Map<String, Object> databaseColumn) {
        List<Integer> expectedArguments = declaredTypeArguments(expectedType);
        String expectedFamily = declaredTypeFamily(expectedType);
        String actualFamily = declaredTypeFamily(actualType);

        if (isTextFamily(expectedFamily) && !expectedArguments.isEmpty()
                && !isUnboundedTextFamily(actualFamily)) {
            Integer actualLength = integerMetadata(metadataValue(
                    databaseColumn, "column_size", "character_maximum_length"));
            if (actualLength == null) {
                List<Integer> actualArguments = declaredTypeArguments(actualType);
                actualLength = actualArguments.isEmpty() ? null : actualArguments.get(0);
            }
            int requiredLength = expectedArguments.get(0);
            if (actualLength != null && actualLength < requiredLength) {
                throw incompatibleColumn(table, column,
                        "required max length=" + requiredLength
                                + " but database reports max length=" + actualLength);
            }
        }

        if (isDecimalFamily(expectedFamily) && expectedArguments.size() >= 2) {
            int expectedPrecision = expectedArguments.get(0);
            int expectedScale = expectedArguments.get(1);
            Integer actualPrecision = integerMetadata(metadataValue(
                    databaseColumn, "numeric_precision", "column_size"));
            Integer actualScale = integerMetadata(metadataValue(
                    databaseColumn, "numeric_scale", "decimal_digits"));
            if (actualPrecision == null || actualScale == null) {
                List<Integer> actualArguments = declaredTypeArguments(actualType);
                if (actualArguments.size() >= 2) {
                    actualPrecision = actualArguments.get(0);
                    actualScale = actualArguments.get(1);
                }
            }
            if (actualPrecision == null && actualScale == null) return;
            boolean covers = actualPrecision != null
                    && actualScale != null
                    && actualScale >= expectedScale
                    && actualPrecision - actualScale >= expectedPrecision - expectedScale;
            if (!covers) {
                throw incompatibleColumn(table, column,
                        "required precision=" + expectedPrecision + ", scale=" + expectedScale
                                + " but database reports precision=" + actualPrecision
                                + ", scale=" + actualScale);
            }
        }
    }

    private IllegalStateException incompatibleColumn(
            String table, SQLColumn column, String detail) {
        return new IllegalStateException(
                "Ensure Schema incompatible existing column for entity '"
                        + entityDescriptor.getType() + "' on table '" + table
                        + "', column '" + column.getColumnName() + "', dialect '"
                        + dialect.getClass().getSimpleName() + "': " + detail);
    }

    private static Object metadataValue(Map<String, Object> metadata, String... names) {
        for (String name : names) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey())) return entry.getValue();
            }
        }
        return null;
    }

    private static boolean nullableMetadata(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        String text = String.valueOf(value).trim();
        return "YES".equalsIgnoreCase(text)
                || "Y".equalsIgnoreCase(text)
                || "TRUE".equalsIgnoreCase(text)
                || "1".equals(text);
    }

    private static Integer integerMetadata(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String declaredTypeFamily(String type) {
        int argumentStart = type.indexOf('(');
        return (argumentStart < 0 ? type : type.substring(0, argumentStart))
                .trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static List<Integer> declaredTypeArguments(String type) {
        int start = type.indexOf('(');
        int end = type.indexOf(')', start + 1);
        if (start < 0 || end < 0) return List.of();
        return Arrays.stream(type.substring(start + 1, end).split(","))
                .map(String::trim)
                .map(value -> {
                    try {
                        return Integer.valueOf(value);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toList());
    }

    private static boolean isTextFamily(String family) {
        return family.contains("CHAR") || family.contains("TEXT") || family.contains("CLOB");
    }

    private static boolean isUnboundedTextFamily(String family) {
        return family.contains("TEXT") || family.contains("CLOB");
    }

    private static boolean isDecimalFamily(String family) {
        return "DECIMAL".equals(family)
                || "NUMERIC".equals(family)
                || "NUMBER".equals(family);
    }

    private String metadataColumnName(Map<String, Object> column, String table) {
        for (Map.Entry<String, Object> entry : column.entrySet()) {
            if ("column_name".equalsIgnoreCase(entry.getKey())
                    && entry.getValue() != null) {
                return String.valueOf(entry.getValue()).toLowerCase(java.util.Locale.ROOT);
            }
        }
        throw new IllegalStateException(
                "Missing column_name in schema metadata for entity '"
                        + entityDescriptor.getType() + "' on table '" + table + "'");
    }

    protected void createTable(UserContext context, String table, List<SQLColumn> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table).append(" (\n");
        sb.append(columns.stream()
                .map(column -> {
                    String dbColumn = dialect.escapeIdentifier(column.getColumnName()) + " " + dialect.mapColumnType(column.getType());
                    if (column.isIdColumn()) dbColumn += " NOT NULL PRIMARY KEY";
                    else if (column.isRequired()) dbColumn += " NOT NULL";
                    return dbColumn;
                })
                .collect(Collectors.joining(",\n")));
        sb.append(")\n");
        logInfo(sb + ";");
        if (ensureTableEnabled(context)) {
            try {
                database.execute(context, sb.toString());
            } catch (Exception e) {
                throw schemaFailure("create", table, e);
            }
        }
    }

    protected void addColumn(UserContext context, SQLColumn column) {
        String sql = StrUtil.format("ALTER TABLE {} ADD COLUMN {} {}{}",
                dialect.escapeIdentifier(column.getTableName()), dialect.escapeIdentifier(column.getColumnName()),
                dialect.mapColumnType(column.getType()), column.isRequired() ? " NOT NULL" : "");
        logInfo(sql + ";");
        if (ensureTableEnabled(context)) {
            try {
                database.execute(context, sql);
            } catch (Exception e) {
                throw schemaFailure("add column " + column.getColumnName(), column.getTableName(), e);
            }
        }
    }

    private IllegalStateException schemaFailure(String operation, String table, Exception cause) {
        return new IllegalStateException(
                "Failed to " + operation + " schema for entity '" + entityDescriptor.getType()
                        + "' on table '" + table + "'",
                cause);
    }


    private Object findColumnValue(Map<String, Object> row, String column) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(column)) return entry.getValue();
        }
        return null;
    }

    private Object findFacetRelationValue(Map<String, Object> row, String relationName) {
        Object value = findColumnValue(row, relationName);
        if (value != null) return value;
        PropertyDescriptor property = findProperty(relationName);
        if (property == null) return null;
        SQLColumn column = getSqlColumn(property);
        return column == null ? null : findColumnValue(row, column.getColumnName());
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
        // Only the declaring side stores the FK. A subclass also persists its
        // ancestor's declaring fields; the reverse side must never become a column.
        if (relation.getRelationKeeper() != relation.getOwner()) return false;
        for (EntityDescriptor owner = this.entityDescriptor; owner != null; owner = owner.getParent()) {
            if (relation.getOwner() == owner) return true;
        }
        return false;
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


    // ==========================================
    // SQL building helpers
    // ==========================================

    private void ensureOrderByForPartition(SearchRequest<T> request) {
        OrderBys orderBy = request.getOrderBy();
        boolean hasId = orderBy.properties(null).stream()
                .anyMatch(BaseEntity.ID_PROPERTY::equals);
        if (!hasId) orderBy.addOrderBy(new OrderBy(BaseEntity.ID_PROPERTY));
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
    public io.teaql.core.SqlParameterLogPolicy parameterLogPolicy(String name) {
        if (io.teaql.core.utils.SensitiveLogNames.credential(name))
            return io.teaql.core.SqlParameterLogPolicy.CREDENTIAL;
        for (PropertyDescriptor property : allProperties) {
            if (!property.getName().equals(name)) continue;
            return io.teaql.core.SqlFieldLogPolicy.resolve(entityDescriptor, property);
        }
        return io.teaql.core.SqlParameterLogPolicy.UNKNOWN;
    }

    private SqlLogBindings logBindings(String table, List<String> columns) {
        List<io.teaql.core.SqlParameterLogPolicy> policies = new ArrayList<>();
        for (String column : columns) {
            var policy = io.teaql.core.SqlParameterLogPolicy.UNKNOWN;
            for (PropertyDescriptor property : allProperties) {
                if (!shouldHandle(property)) continue;
                for (SQLColumn mapping : getSqlColumns(property)) {
                    if (table.equals(mapping.getTableName()) && column.equals(mapping.getColumnName()))
                        policy = parameterLogPolicy(property.getName());
                }
            }
            policies.add(policy);
        }
        return new SqlLogBindings(policies, true);
    }

    private SqlLogBindings logBindings(String table, List<String> columns, String sql, String trace) {
        SqlLogBindings bindings = logBindings(table, columns);
        if (trace == null || trace.isEmpty()) return bindings;
        String suffix = " /* [" + trace + "] */";
        // Execution retains its original trace comment. Logs use compiler-known SQL structure
        // and the separately projected context trace, never a raw business annotation inline.
        return sql.endsWith(suffix)
                ? new SqlLogBindings(bindings.policies(), true, sql.substring(0, sql.length() - suffix.length()))
                : new SqlLogBindings(bindings.policies(), false);
    }

    @Override
    public String prepareLimit(SearchRequest request, java.util.Map<String, Object> parameters) {
        Slice slice = request.getSlice();
        if (ObjectUtil.isEmpty(slice)) return null;
        
        String limitKey = "limit0";
        while (parameters.containsKey(limitKey)) limitKey += "_1";
        io.teaql.core.sql.SqlParameters.bind(parameters, limitKey, slice.getSize(), io.teaql.core.SqlParameterLogPolicy.PLAIN);
        
        String offsetPlaceholder;
        if (slice.getOffset() == 0) {
            // Zero is a runtime-controlled pagination constant, not caller SQL.
            offsetPlaceholder = "0";
        }
        else {
            String offsetKey = "offset0";
            while (parameters.containsKey(offsetKey)) offsetKey += "_1";
            io.teaql.core.sql.SqlParameters.bind(parameters, offsetKey, slice.getOffset(), io.teaql.core.SqlParameterLogPolicy.PLAIN);
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
        return aggregateWithIntent(userContext, request, SqlDiagnosticRequest.source(userContext, request));
    }

    AggregationResult doAggregateInternal(UserContext userContext, SearchRequest<T> request,
            io.teaql.core.SqlIntentRedactions intent) {
        return doAggregateInternal(userContext, intent == null ? request : SqlDiagnosticRequest.forExecution(request, intent));
    }

    private AggregationResult aggregateWithIntent(UserContext userContext, SearchRequest<T> request,
            io.teaql.core.SqlIntentRedactions intent) {
        if (!request.hasSimpleAgg()) return null;
        SqlLikeIntent.capture(userContext, request, this, intent);

        io.teaql.core.sql.SqlAstCompiler compiler = new io.teaql.core.sql.SqlAstCompiler();
        List<String> tables = compiler.collectAggregationTables(sqlMetadata, this, userContext, request);
        Map<String, Object> parameters = new io.teaql.core.sql.SqlParameters();
        Object preConfig = userContext.getObj(MULTI_TABLE);
        userContext.putAttribute(MULTI_TABLE, tables.size() > 1);

        try {
            String sql = compiler.buildAggregationSQL(sqlMetadata, this, userContext, request, parameters, tables);
            if (sql == null) return null;

            PositionalSQL psql = withQueryIntent(toPositional(sql, parameters), intent, request);
            List<Map<String, Object>> rows = database.query(userContext, psql.sql, psql.args, psql.logBindings);

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

    static void logInfo(String message) {
        // The runtime database adapter owns schema diagnostics and applies the
        // mutation-family logging switch plus safe projection. This legacy hook
        // must not write directly to stdout, even when its payload is omitted.
    }

    protected int toIntOrZero(Object cnt) {
        return cnt != null ? io.teaql.core.utils.Convert.convert(Integer.class, cnt) : 0;
    }

    protected io.teaql.core.EntityStatus resolvePersistedStatus(Long version) {
        return (version != null && version < 0)
                ? io.teaql.core.EntityStatus.PERSISTED_DELETED
                : io.teaql.core.EntityStatus.PERSISTED;
    }

}
