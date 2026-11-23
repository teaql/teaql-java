package io.teaql.runtime.memory;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.runtime.DefaultQueryResult;
import java.util.*;
import java.util.function.Function;

/** Execution-local memory query materialization. Stored rows are never result carriers. */
final class MemoryQueries {
    private final UserContext context;
    private final Function<String, List<Entity>> rows;
    private final CriteriaFilter<Entity> filter = new CriteriaFilter<>();

    MemoryQueries(UserContext context, Function<String, List<Entity>> rows) {
        this.context = context;
        this.rows = rows;
    }

    DefaultQueryResult execute(SearchRequest<?> request) {
        SearchCriteria criteria = resolveCriteria(request.getSearchCriteria(), request);
        var members = new ArrayList<Entity>();
        for (Entity row : rows.apply(request.getTypeName())) if (filter.accept(row, criteria)) members.add(row);
        var aggregation = MemoryAggregations.calculate(request.getAggregations(), members);
        if (aggregation != null) return new DefaultQueryResult(new SmartList<>(), aggregation);

        sort(members, request);
        var result = new SmartList<Entity>();
        Slice slice = request.getSlice();
        int start = slice == null ? 0 : Math.min(members.size(), Math.max(0, slice.getOffset()));
        int end = slice == null ? members.size() : (int) Math.min(members.size(), (long) start + Math.max(0, slice.getSize()));
        var states = new HashMap<LoadState, LoadState>();
        for (Entity source : members.subList(start, end)) {
            BaseEntity row = materialize(source, request);
            var state = row.__internalLoadState().withDynamicPropertyMetadata(request.getDynamicPropertyMetadata());
            row.__internalUseLoadState(states.computeIfAbsent(state, ignored -> state));
            result.add(row);
        }
        attachAggregates(result, request);
        attachFacets(result, members, request);
        return new DefaultQueryResult(result, null);
    }

    private static Object scalar(Object value) {
        return value instanceof Entity entity ? entity.getId() : value;
    }

    private SearchCriteria resolveCriteria(SearchCriteria criteria, SearchRequest<?> owner) {
        if (criteria instanceof SubQuerySearchCriteria sub) {
            var query = child(owner, sub.getDependsOn(), null, "subquery");
            query.setAggregations(new Aggregations());
            query.groupBy("membership", sub.getDependsOnPropertyName());
            // A grouped aggregate describes full membership, independent of a page slice.
            var values = context.internalAggregation(query).toList().stream()
                    .map(value -> value.get("membership")).filter(Objects::nonNull).toList();
            return new TwoOperatorCriteria(Operator.IN, new PropertyReference(sub.getPropertyName()),
                    new Parameter("membership", values, Operator.IN));
        }
        if (criteria instanceof AND and) return new AND(and.getExpressions().stream()
                .map(value -> resolveCriteria((SearchCriteria) value, owner)).toArray(SearchCriteria[]::new));
        if (criteria instanceof OR or) return new OR(or.getExpressions().stream()
                .map(value -> resolveCriteria((SearchCriteria) value, owner)).toArray(SearchCriteria[]::new));
        if (criteria instanceof NOT not) return new NOT(resolveCriteria((SearchCriteria) not.first(), owner));
        if (criteria instanceof VersionSearchCriteria version)
            return new VersionSearchCriteria(resolveCriteria(version.getSearchCriteria(), owner));
        return criteria;
    }

    private static QueryRequestSnapshot<?> child(SearchRequest<?> owner, SearchRequest<?> template,
            QueryOperation operation, String name) {
        var parent = QueryRequestScope.capture(owner);
        if (operation != null) parent = parent.deriveOperation(owner.getTypeName(), operation);
        return new QueryRequestSnapshot<>(template, parent.deriveChild(name, template.comment()));
    }

    private static String property(Expression expression) {
        if (expression instanceof AggrExpression aggregate && aggregate.getOperator() == AggrFunction.SELF)
            expression = aggregate.first();
        if (expression instanceof PropertyReference reference) return reference.getPropertyName();
        throw new TeaQLRuntimeException("Unsupported memory property expression: " + expression);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void sort(List<Entity> members, SearchRequest<?> request) {
        if (request.getOrderBy() == null) return;
        Comparator<Entity> comparator = null;
        for (OrderBy order : request.getOrderBy().getOrderBys()) {
            String name = property(order.getExpression());
            if (!"ASC".equals(order.getDirection()) && !"DESC".equals(order.getDirection()))
                throw new TeaQLRuntimeException("Unsupported memory sort direction");
            Comparator<Entity> next = Comparator.comparing(row -> (Comparable) scalar(row.getProperty(name)),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            if ("DESC".equals(order.getDirection())) next = next.reversed();
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        if (comparator != null) members.sort(comparator);
    }

    private BaseEntity materialize(Entity source, SearchRequest<?> request) {
        if (!(request.internalNewEntity() instanceof BaseEntity row))
            throw new TeaQLRuntimeException("Memory query requires a BaseEntity hydration factory");
        var selected = new LinkedHashMap<String, String>();
        if (request.getProjections().isEmpty()) {
            EntityDescriptor descriptor = EntityMetaFactory.requireFrom(context).resolveEntityDescriptor(request.getTypeName());
            for (; descriptor != null; descriptor = descriptor.getParent())
                for (var field : descriptor.getOwnProperties()) selected.put(field.getName(), field.getName());
        } else for (var field : request.getProjections()) selected.put(field.name(), property(field.getExpression()));
        selected.put(BaseEntity.ID_PROPERTY, BaseEntity.ID_PROPERTY);
        selected.put(BaseEntity.VERSION_PROPERTY, BaseEntity.VERSION_PROPERTY);
        for (var field : selected.entrySet()) {
            Object value = source.getProperty(field.getValue());
            if (value instanceof Entity reference) {
                // An FK is loaded, but its unrequested detail is not. Never expose the stored graph.
                var descriptor = EntityMetaFactory.requireFrom(context).resolveEntityDescriptor(reference.typeName());
                if (!(descriptor.createEntity() instanceof BaseEntity refer))
                    throw new TeaQLRuntimeException("Memory relation requires a BaseEntity hydration factory");
                refer.__internalSet("id", reference.getId());
                refer.set$status(EntityStatus.REFER);
                value = refer;
            }
            row.__internalHydrate(field.getKey(), value, BaseEntity.loadedPropertyIndex(row.getClass(), field.getKey()));
        }
        row.set$status(row.getVersion() != null && row.getVersion() < 0 ? EntityStatus.PERSISTED_DELETED : EntityStatus.PERSISTED);
        row.setTraceChain(QueryRequestScope.capture(request).traceChain());
        return row;
    }

    private void attachAggregates(SmartList<Entity> parents, SearchRequest<?> request) {
        for (SimpleAggregation attribute : request.getDynamicAggregateAttributes()) {
            var template = attribute.getAggregateRequest();
            String partition = template.getPartitionProperty();
            if (partition == null || partition.isBlank()) throw new TeaQLRuntimeException("Memory related aggregate requires a partition property");
            var query = child(request, template, attribute.getOperation(), template.getTypeName());
            if (query.getAggregations().getAggregates().isEmpty()) query.count(attribute.getName());
            if (attribute.isSingleNumber() && query.getAggregations().getAggregates().size() != 1)
                throw new TeaQLRuntimeException("Memory scalar related aggregate requires exactly one aggregate");
            query.groupBy(partition);
            query.appendSearchCriteria(query.createBasicSearchCriteria(partition, Operator.IN, parents.mapById().keySet()));
            var calculated = context.internalAggregation(query);
            var byId = parents.mapById();
            if (attribute.isSingleNumber()) {
                var selected = query.getAggregations().getAggregates().get(0);
                var expression = selected.getExpression();
                Object empty = expression instanceof AggrExpression aggregate && aggregate.getOperator() == AggrFunction.COUNT ? 0L : null;
                for (Entity parent : parents) parent.addDynamicProperty(attribute.getName(), empty);
                for (var values : calculated.toList()) {
                    var parent = byId.get(values.get(partition));
                    if (parent != null) parent.addDynamicProperty(attribute.getName(), values.get(selected.name()));
                }
            } else for (var values : calculated.toList()) {
                var parent = byId.get(values.remove(partition));
                if (parent != null) parent.appendDynamicProperty(attribute.getName(), values);
            }
        }
    }

    private void attachFacets(SmartList<Entity> result, List<Entity> members, SearchRequest<?> request) {
        for (FacetRequest facet : request.getFacetRequests()) {
            var target = child(request, facet.getRequest(), facet.getOperation(), facet.getRequest().getTypeName());
            var counts = new LinkedHashMap<Object, Long>();
            for (Entity member : members) {
                Object id = scalar(member.getProperty(facet.getRelationName()));
                if (id != null) counts.merge(id, 1L, Long::sum);
            }
            var aliases = new ArrayList<String>();
            for (var selected : target.getAggregations().getAggregates()) {
                if (!(selected.getExpression() instanceof AggrExpression aggregate) || aggregate.getOperator() != AggrFunction.COUNT)
                    throw new TeaQLRuntimeException("Memory facet counts require COUNT expressions");
                aliases.add(selected.name());
            }
            if (aliases.isEmpty()) aliases.add("count");
            target.setAggregations(new Aggregations());
            var memberIds = members.stream().map(Entity::getId).toList();
            for (var aggregate : target.getDynamicAggregateAttributes()) {
                var contribution = aggregate.getAggregateRequest();
                if (contribution.getTypeName().equals(request.getTypeName()))
                    contribution.appendSearchCriteria(target.createBasicSearchCriteria("id", Operator.IN, memberIds));
            }
            if (facet.isMergeCriteria()) target.appendSearchCriteria(target.createBasicSearchCriteria("id", Operator.IN, counts.keySet()));
            SmartList<Entity> options = context.internalExecuteForList(target);
            for (Entity option : options) for (String alias : aliases)
                option.addDynamicProperty(alias, counts.getOrDefault(option.getId(), 0L));
            result.addFacet(facet.getFacetName(), options);
        }
    }
}
