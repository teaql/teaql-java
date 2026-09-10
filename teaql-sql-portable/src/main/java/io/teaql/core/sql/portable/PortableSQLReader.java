package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.internal.TempRequest;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.PropertyType;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SqlAstCompiler;
import io.teaql.core.sql.expression.SQLExpressionParser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles all read operations for PortableSQLRepository.
 * Responsibilities:
 * - Building SELECT queries
 * - Mapping result rows to entities
 * - Executing aggregation queries
 * - Facet queries
 */
public class PortableSQLReader<T extends Entity> {

    private static final Pattern NAMED_PARAM = Pattern.compile(":(\\w+)");

    private final PortableSQLRepository<T> repository;
    private final EntityDescriptor entityDescriptor;
    private final TeaQLDatabase database;

    public PortableSQLReader(PortableSQLRepository<T> repository, EntityDescriptor entityDescriptor, TeaQLDatabase database) {
        this.repository = repository;
        this.entityDescriptor = entityDescriptor;
        this.database = database;
    }

    /**
     * Load entities based on the search request.
     */
    public SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {
        Map<String, Object> params = new HashMap<>();
        String sql = repository.buildDataSQL(userContext, request, params);
        if (sql == null || sql.isEmpty()) {
            return new SmartList<>();
        }
        PositionalSQL psql = toPositional(sql, params);
        List<Map<String, Object>> rows = database.query(userContext, psql.sql, psql.args);
        List<T> results = rows.stream()
                .map(row -> mapRowToEntity(userContext, request, row))
                .collect(Collectors.toList());
        SmartList<T> smartList = new SmartList<>(results);

        // Handle facet requests
        List<FacetRequest> facetRequests = request.getFacetRequests();
        if (facetRequests != null && !facetRequests.isEmpty()) {
            processFacets(userContext, request, smartList, facetRequests);
        }

        return smartList;
    }

    /**
     * Execute aggregation query and return results.
     */
    protected AggregationResult doAggregateInternal(UserContext userContext, SearchRequest<T> request) {
        if (!request.hasSimpleAgg()) return null;

        SqlAstCompiler compiler = new SqlAstCompiler();
        List<String> tables = compiler.collectAggregationTables(repository.getSqlMetadata(), repository, userContext, request);
        Map<String, Object> parameters = new HashMap<>();
        Object preConfig = userContext.getObj(PortableSQLRepository.MULTI_TABLE);
        userContext.putAttribute(PortableSQLRepository.MULTI_TABLE, tables.size() > 1);

        try {
            String sql = compiler.buildAggregationSQL(repository.getSqlMetadata(), repository, userContext, request, parameters, tables);
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
            userContext.putAttribute(PortableSQLRepository.MULTI_TABLE, preConfig);
        }
    }

    /**
     * Execute stream query with batch support.
     */
    public Stream<T> executeForStream(UserContext userContext, SearchRequest<T> request, int enhanceBatch) {
        return loadInternal(userContext, request).stream();
    }

    // ==========================================
    // Row mapping
    // ==========================================

    private T mapRowToEntity(UserContext userContext, SearchRequest<T> request, Map<String, Object> row) {
        Class<? extends T> returnType = request.returnType();
        T entity = createEntity(returnType);
        for (PropertyDescriptor property : repository.getAllProperties()) {
            if (!repository.shouldHandle(property)) continue;
            if (!(property instanceof io.teaql.core.meta.Relation)) {
                if (!row.containsKey(property.getName())) continue;
                Object value = row.get(property.getName());
                Class targetType = property.getType().javaType();
                entity.setProperty(
                        property.getName(),
                        value == null
                                ? null
                                : io.teaql.core.utils.Convert.convert(targetType, value));
            } else if (property instanceof io.teaql.core.meta.Relation) {
                if (!row.containsKey(property.getName())) continue;
                Object value = row.get(property.getName());
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
        Object typeAlias = row.get(PortableSQLRepository.TYPE_ALIAS);
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

    private io.teaql.core.EntityStatus resolvePersistedStatus(Long version) {
        return (version != null && version < 0)
                ? io.teaql.core.EntityStatus.PERSISTED_DELETED
                : io.teaql.core.EntityStatus.PERSISTED;
    }

    // ==========================================
    // Facet processing
    // ==========================================

    private void processFacets(UserContext userContext, SearchRequest<T> request,
                              SmartList<T> smartList, List<FacetRequest> facetRequests) {
        SqlAstCompiler compiler = new SqlAstCompiler();
        for (FacetRequest facetRequest : facetRequests) {
            TempRequest tr = new TempRequest(request);
            tr.setAggregations(new Aggregations());
            tr.groupBy(facetRequest.getRelationName());
            tr.count("count");

            Map<String, Object> facetParams = new HashMap<>();
            List<String> facetTables = compiler.collectAggregationTables(repository.getSqlMetadata(), repository, userContext, tr);
            String facetSql = compiler.buildAggregationSQL(repository.getSqlMetadata(), repository, userContext, tr, facetParams, facetTables);
            if (facetSql != null && !facetSql.isEmpty()) {
                PositionalSQL psqlFacet = toPositional(facetSql, facetParams);
                List<Map<String, Object>> facetRows = database.query(userContext, psqlFacet.sql, psqlFacet.args);

                SmartList<Entity> facetEntities = new SmartList<>();
                SearchRequest<?> relationReq = facetRequest.getRequest();
                if (relationReq != null) {
                    String relationType = relationReq.getTypeName();
                    PortableSQLRepository<?> relationRepo = repository.getResolver().resolve(relationType);
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
                        SearchRequest<?> fetchRelReq = new TempRequest(relationReq);
                        if (facetRequest.isMergeCriteria()) {
                            ((TempRequest) fetchRelReq).appendSearchCriteria(request.getSearchCriteria());
                        }
                        SmartList<?> loadedRels = relationRepo.loadInternal(userContext, (SearchRequest) fetchRelReq);
                        for (Object obj : loadedRels) {
                            Entity rel = (Entity) obj;
                            Object cnt = idToCount.get(rel.getId());
                            int countInt = toIntOrZero(cnt);
                            if (rel instanceof BaseEntity) {
                                ((BaseEntity) rel).addDynamicProperty("count", countInt);
                            }
                            facetEntities.add(rel);
                        }
                    }
                }
                smartList.addFacet(facetRequest.getFacetName(), facetEntities);
            }
        }
    }

    // ==========================================
    // Parameter handling
    // ==========================================

    PositionalSQL toPositional(String namedSql, Map<String, Object> params) {
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

    private int toIntOrZero(Object cnt) {
        return cnt != null ? io.teaql.core.utils.Convert.convert(Integer.class, cnt) : 0;
    }

    // ==========================================
    // Inner class for positional SQL
    // ==========================================

    static class PositionalSQL {
        final String sql;
        final Object[] args;

        PositionalSQL(String sql, Object[] args) {
            this.sql = sql;
            this.args = args;
        }
    }
}
