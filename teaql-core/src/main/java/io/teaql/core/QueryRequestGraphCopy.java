package io.teaql.core;

import io.teaql.core.criteria.*;
import java.util.*;

/** One bounded copy operation; identity is shared only inside the copied AST. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class QueryRequestGraphCopy {
    private static final Set<Class<?>> IMMUTABLE_NUMBERS = Set.of(Byte.class, Short.class, Integer.class,
            Long.class, Float.class, Double.class, java.math.BigInteger.class, java.math.BigDecimal.class);
    private static final Set<Class<?>> FUNCTIONS = Set.of(FunctionApply.class, AggrExpression.class,
            AND.class, OR.class, NOT.class, Between.class, OneOperatorCriteria.class, TwoOperatorCriteria.class,
            EQ.class, InEquation.class, NotBeginWith.class, NotEndWith.class, NotIn.class,
            LTE.class, InLarge.class, GT.class, GTE.class, BeginWith.class, IN.class, LT.class,
            Contain.class, NotContain.class, EndWith.class, IsNull.class, IsNotNull.class);
    private final boolean typed;
    private final IdentityHashMap<SearchRequest<?>, BaseRequest<?>> requests = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Boolean> active = new IdentityHashMap<>();
    private final IdentityHashMap<Expression, Expression> expressions = new IdentityHashMap<>();
    private int requestDepth;
    private int nodes;

    QueryRequestGraphCopy(boolean typed) { this.typed = typed; }

    BaseRequest<?> request(SearchRequest<?> source, int depth) {
        if (source == null) return null;
        if (active.containsKey(source) || depth > 64) throw invalid("Cyclic or oversized query request graph");
        var previous = requests.get(source);
        if (previous != null) return previous;
        if (!typed) return new QueryRequestSnapshot(source, source.requestScope(), source.diagnosticSource(), this, depth);
        if (!(source instanceof BaseRequest<?> builder)) throw invalid("Query copy requires a typed request factory");
        var target = builder.internalCopyTarget();
        if (target == null || target == source || target.getClass() != source.getClass())
            throw invalid("Query copy factory must return a fresh request of the same type");
        populate(source, target, depth);
        return target;
    }

    void populate(SearchRequest<?> source, BaseRequest<?> target, int depth) {
        if (depth > 64 || active.put(source, true) != null || ++nodes > 10000)
            throw invalid("Cyclic or oversized query request graph");
        requests.put(source, target);
        int previousDepth = requestDepth; requestDepth = depth;
        try {
            if (source instanceof BaseRequest<?> builder) {
                builder.propagateMetadata(target);
                target.propagateAggregationCache = builder.propagateAggregationCache;
            }
            target.comment = source.comment(); target.purpose = source.purpose();
            target.projections = named(source.getProjections());
            target.simpleDynamicProperties = named(source.getSimpleDynamicProperties());
            target.dynamicPropertyMetadata = source.getDynamicPropertyMetadata();
            target.searchCriteria = (SearchCriteria) expression(source.getSearchCriteria(), 0);
            target.having = (SearchCriteria) expression(source.getHaving(), 0);
            target.orderBys = new OrderBys();
            if (source.getOrderBy() != null) for (var order : source.getOrderBy().getOrderBys()) {
                var copy = new OrderBy("id"); copy.setDirection(order.getDirection());
                copy.setExpression(expression(order.getExpression(), 0)); target.orderBys.addOrderBy(copy);
            }
            var slice = source.getSlice();
            target.slice = slice == null || depth > 0 && !slice.hasExplicitPage() ? null : slice.copy();
            target.partitionProperty = source.getPartitionProperty();
            target.aggregations = new Aggregations();
            if (source.getAggregations() != null) {
                var aggregation = source.getAggregations(); target.aggregations.setName(aggregation.getName());
                target.aggregations.setAggregates(named(aggregation.getAggregates()));
                target.aggregations.setSimpleDimensions(named(aggregation.getSimpleDimensions()));
                target.aggregations.setComplexDimensions(named(aggregation.getComplexDimensions()));
            }
            target.enhanceRelations = children(source.enhanceRelations());
            target.enhanceChildren = children(source.enhanceChildren());
            target.propagateAggregations = children(source.getPropagateAggregations());
            target.propagateDimensions = children(source.getPropagateDimensions());
            target.dynamicAggregateAttributes = new ArrayList<>();
            for (var aggregation : source.getDynamicAggregateAttributes())
                target.dynamicAggregateAttributes.add((SimpleAggregation) expression(aggregation, 0));
            target.facetRequests = new ArrayList<>();
            for (var facet : source.getFacetRequests()) {
                var copy = new FacetRequest(); copy.setFacetName(facet.getFacetName()); copy.setRelationName(facet.getRelationName());
                copy.setMergeCriteria(facet.isMergeCriteria()); copy.setOperation(facet.getOperation());
                copy.setRequest(request(facet.getRequest(), depth + 1)); target.facetRequests.add(copy);
            }
            target.queryOperations = new HashMap<>(source.queryOperations()); target.pageCountOperation = source.pageCountOperation();
            target.rollUpOperations = new HashMap<>();
            target.propagateDimensions.keySet().forEach(relation -> {
                var operation = source.rollUpOperation(relation);
                if (operation != null) target.rollUpOperations.put(relation, operation);
            });
            target.cacheAggregation = source.tryCacheAggregation(); target.aggregateCacheTime = source.getAggregateCacheTime();
            target.hardLimit = source.hardLimit(); target.continuousPageFetchOptions = source.continuousPageFetchOptions();
            target.idSetPaginationOptions = source.idSetPaginationOptions(); target.topNProbeParentThreshold = source.topNProbeParentThreshold();
            target.extensions = new HashMap<>();
            if (source.getExtensions() != null) source.getExtensions().forEach((key, value) -> target.extensions.put(key, value(value, 0)));
            target.searchForText = source.getSearchForText(); target.dynamicFieldSelection = source.getDynamicFieldSelection();
        } finally { requestDepth = previousDepth; active.remove(source); }
    }

    private Map<String, SearchRequest> children(Map<String, SearchRequest> source) {
        var result = new LinkedHashMap<String, SearchRequest>();
        if (source != null) source.forEach((name, child) -> result.put(name, request(child, requestDepth + 1)));
        return result;
    }

    private List<SimpleNamedExpression> named(List<SimpleNamedExpression> source) {
        var result = new ArrayList<SimpleNamedExpression>();
        if (source != null) for (var item : source) result.add((SimpleNamedExpression) expression(item, 0));
        return result;
    }

    private Expression expression(Expression source, int depth) {
        if (source == null) return null;
        if (depth > 64 || active.containsKey(source)) throw invalid("Cyclic or oversized query expression");
        var previous = expressions.get(source); if (previous != null) return previous;
        if (++nodes > 10000) throw invalid("Query expression graph exceeds its node limit");
        active.put(source, true);
        try {
            Expression copy;
            if (source.getClass() == PropertyReference.class) copy = new PropertyReference(((PropertyReference) source).getPropertyName());
            else if (source.getClass() == Parameter.class) {
                var parameter = (Parameter) source;
                if (parameter.getOperator() == null) throw invalid("Query parameter is missing an operator");
                copy = new Parameter(parameter.getName(), value(parameter.getValue(), 0), parameter.getOperator());
            } else if (source.getClass() == Constant.class) {
                var constant = new Constant(); constant.setValue(value(((Constant) source).getValue(), 0)); copy = constant;
            } else if (source.getClass() == SimpleNamedExpression.class) {
                var named = (SimpleNamedExpression) source; copy = new SimpleNamedExpression(named.name(), expression(named.getExpression(), depth + 1));
            } else if (source.getClass() == SubQuerySearchCriteria.class) {
                var membership = (SubQuerySearchCriteria) source;
                copy = new SubQuerySearchCriteria(membership.getPropertyName(), request(membership.getDependsOn(), requestDepth + 1),
                        membership.getDependsOnPropertyName(), membership.navigationOwned());
            } else if (source.getClass() == VersionSearchCriteria.class)
                copy = new VersionSearchCriteria((SearchCriteria) expression(((VersionSearchCriteria) source).getSearchCriteria(), depth + 1));
            else if (source.getClass() == TypeCriteria.class)
                copy = new TypeCriteria((Parameter) expression(((TypeCriteria) source).getTypeParameter(), depth + 1));
            else if (source.getClass() == SimpleAggregation.class) {
                var aggregate = (SimpleAggregation) source;
                var result = new SimpleAggregation(aggregate.getName(), request(aggregate.getAggregateRequest(), requestDepth + 1), aggregate.isSingleNumber());
                result.setOperation(aggregate.getOperation()); copy = result;
            } else if (source instanceof FunctionApply function && FUNCTIONS.contains(source.getClass())) {
                var arguments = function.getExpressions().stream().map(argument -> expression(argument, depth + 1)).toArray(Expression[]::new);
                int arity = source instanceof Between ? 3 : source instanceof TwoOperatorCriteria ? 2
                        : source instanceof NOT || source instanceof AggrExpression || source instanceof OneOperatorCriteria ? 1 : -1;
                if (arity >= 0 && arguments.length != arity) throw invalid("Invalid query function arity");
                if ((source instanceof AND || source instanceof OR)
                        && Arrays.stream(arguments).anyMatch(argument -> !(argument instanceof SearchCriteria)))
                    throw invalid("Boolean query functions require conditions");
                if (source instanceof AND) copy = new AND(Arrays.copyOf(arguments, arguments.length, SearchCriteria[].class));
                else if (source instanceof OR) copy = new OR(Arrays.copyOf(arguments, arguments.length, SearchCriteria[].class));
                else if (source instanceof NOT) copy = new NOT((SearchCriteria) arguments[0]);
                else if (source instanceof AggrExpression) copy = new AggrExpression((AggrFunction) function.getOperator(), arguments[0]);
                else if (source instanceof Between) copy = new Between(arguments[0], arguments[1], arguments[2]);
                else if (source instanceof TwoOperatorCriteria) copy = new TwoOperatorCriteria(function.getOperator(), arguments[0], arguments[1]);
                else if (source instanceof OneOperatorCriteria) copy = new OneOperatorCriteria((Operator) function.getOperator(), arguments[0]);
                else copy = new FunctionApply(function.getOperator(), arguments);
            } else {
                // Execution snapshots retain trusted custom parser extensions.
                // Explicit copies cannot silently claim to freeze unknown ASTs.
                if (typed) throw invalid("Query copy does not support expression type " + source.getClass().getName());
                copy = source;
            }
            expressions.put(source, copy); return copy;
        } finally { active.remove(source); }
    }

    private Object value(Object source, int depth) {
        if (source == null || source instanceof String || immutableNumber(source) || source instanceof Boolean
                || source instanceof Character || source instanceof Enum<?> || source instanceof UUID
                || (source instanceof java.time.temporal.TemporalAccessor && source.getClass().getPackageName().equals("java.time"))
                || source instanceof java.time.Duration || source instanceof java.time.Period) return source;
        if (depth > 64 || active.put(source, true) != null) throw invalid("Cyclic or oversized query parameter container");
        try {
            // Date.clone preserves the JDK SQL subclasses (including Timestamp
            // nanoseconds) without adding a java.sql dependency to core.
            if (source instanceof Date date && (source.getClass() == Date.class
                    || source.getClass().getPackageName().equals("java.sql"))) return date.clone();
            if (source instanceof List<?> list) { var result = new ArrayList<>(); for (var item : list) result.add(value(item, depth + 1)); return result; }
            if (source instanceof Set<?> set) { var result = new LinkedHashSet<>(); for (var item : set) result.add(value(item, depth + 1)); return result; }
            if (source instanceof Map<?, ?> map) { var result = new LinkedHashMap<>(); map.forEach((key, item) -> result.put(value(key, depth + 1), value(item, depth + 1))); return result; }
            if (source instanceof byte[] bytes) return bytes.clone();
            if (source instanceof short[] values) return values.clone();
            if (source instanceof int[] values) return values.clone();
            if (source instanceof long[] values) return values.clone();
            if (source instanceof float[] values) return values.clone();
            if (source instanceof double[] values) return values.clone();
            if (source instanceof boolean[] values) return values.clone();
            if (source instanceof char[] values) return values.clone();
            if (source instanceof Object[] array) { var result = array.clone(); for (int i=0; i<array.length; i++) result[i] = value(array[i], depth + 1); return result; }
            if (typed) throw invalid("Query copy does not support parameter type " + source.getClass().getName());
            return source;
        } finally { active.remove(source); }
    }

    private static boolean immutableNumber(Object value) {
        if (value == null) return false;
        return IMMUTABLE_NUMBERS.contains(value.getClass());
    }

    private static TeaQLRuntimeException invalid(String message) { return new TeaQLRuntimeException(message); }
}
