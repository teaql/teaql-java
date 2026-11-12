package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.sql.SqlAstCompiler;
import io.teaql.core.sql.SqlEntityMetadata;
import java.util.*;

/** Invocation-local statistics plan. No generated code or entity hydration. */
final class LayeredAggregation {
    private record Layer(PortableSQLRepository<?> repository, SearchRequest<?> request,
            List<String> path, Integer parent, String foreignKey) {}
    private record Binding(int owner, SimpleNamedExpression expression, List<String> path, String label) {}

    private final List<Layer> layers = new ArrayList<>();
    private final List<Binding> dimensions = new ArrayList<>();
    private final List<Binding> measures = new ArrayList<>();
    private final List<List<String>> paths = new ArrayList<>();
    private final PortableSQLRepository<?> root;
    private String sql;

    private LayeredAggregation(PortableSQLRepository<?> root) { this.root = root; }
    String sql() { return sql; }

    static LayeredAggregation compile(PortableSQLRepository<?> repository, UserContext context,
            SearchRequest<?> request, Map<String, Object> parameters) {
        var plan = new LayeredAggregation(repository);
        plan.addLayer(repository, request, List.of(), null, null, new IdentityHashMap<>());
        plan.sql = plan.lower(context, parameters);
        return plan;
    }

    private void addLayer(PortableSQLRepository<?> repository, SearchRequest<?> request,
            List<String> path, Integer parent, String foreignKey, IdentityHashMap<SearchRequest<?>, Boolean> active) {
        if (layers.size() >= 64 || active.put(request, Boolean.TRUE) != null)
            throw invalid("Cyclic statistics or statistical plan exceeds 64 layers");
        try {
            String sourceRoute = Objects.toString(root.getEntityDescriptor().getDataService(), "default");
            String targetRoute = Objects.toString(repository.getEntityDescriptor().getDataService(), "default");
            if (!sourceRoute.equals(targetRoute)) throw invalid("Statistical populations must share a data service");
            if (!request.getPropagateAggregations().isEmpty())
                throw invalid("Independent propagated aggregates require separate statistical lowering");
            int owner = layers.size();
            layers.add(new Layer(repository, request, path, parent, foreignKey));
            for (var dimension : request.getAggregations().getSimpleDimensions()) {
                scalarProperty(dimension.getExpression());
                addBinding(dimensions, owner, dimension, path, "__teaql_d");
            }
            for (var measure : request.getAggregations().getAggregates()) {
                if (!(measure.getExpression() instanceof AggrExpression aggregate)
                        || !(aggregate.getOperator() instanceof AggrFunction function)
                        || function == AggrFunction.SELF || aggregate.getExpressions().size() != 1)
                    throw invalid("Statistical measure requires an aggregate expression");
                scalarProperty(aggregate.first());
                addBinding(measures, owner, measure, path, "__teaql_m");
            }
            var children = new TreeMap<>(request.getPropagateDimensions());
            Set<String> declared = new HashSet<>();
            for (var dimension : request.getAggregations().getComplexDimensions()) declared.add(dimension.name());
            if (!declared.equals(children.keySet())) throw invalid("Every complex dimension must have one parent request");
            if (parent != null && request.getAggregations().getDimensions().isEmpty()
                    && request.getAggregations().getAggregates().isEmpty()) {
                addBinding(dimensions, owner, new SimpleNamedExpression("id"), path, "__teaql_d");
                List<SimpleNamedExpression> projections = request.getProjections();
                if (projections.isEmpty()) {
                    projections = new ArrayList<>();
                    for (var property : new SqlEntityMetadata(repository.getEntityDescriptor()).getAllProperties()) {
                        if (property instanceof Relation relation && relation.getRelationKeeper() != relation.getOwner()) continue;
                        if (!property.isId()) projections.add(new SimpleNamedExpression(property.getName()));
                    }
                }
                for (var projection : projections) {
                    if (!"id".equals(projection.name())) {
                        scalarProperty(projection.getExpression());
                        addBinding(dimensions, owner, projection, path, "__teaql_d");
                    }
                }
            }
            for (var entry : children.entrySet()) {
                String property = entry.getKey();
                if (!(repository.findProperty(property) instanceof Relation relation)
                        || relation.getRelationKeeper() != relation.getOwner())
                    throw invalid("Roll-up requires an owned to-one reference: " + property);
                SearchRequest<?> child = entry.getValue();
                if (!relation.getReverseProperty().getOwner().getType().equals(child.getTypeName()))
                    throw invalid("Roll-up target does not match relation " + property);
                var target = repository.getResolver().resolve(child.getTypeName());
                addLayer(target, child, append(path, property), owner, property, active);
            }
        } finally { active.remove(request); }
    }

    private void addBinding(List<Binding> bindings, int owner, SimpleNamedExpression expression,
            List<String> parent, String prefix) {
        if (expression.name() == null || expression.name().isBlank()) throw invalid("Statistics alias must not be blank");
        var path = append(parent, expression.name());
        for (var existing : paths) {
            if (prefix(existing, path) || prefix(path, existing)) throw invalid("Conflicting statistical result path: " + path);
        }
        paths.add(path);
        bindings.add(new Binding(owner, expression, path, prefix + bindings.size()));
    }

    private String lower(UserContext context, Map<String, Object> parameters) {
        List<String> ctes = new ArrayList<>();
        for (int index = 0; index < layers.size(); index++) {
            var layer = layers.get(index);
            Set<String> fields = new TreeSet<>(Set.of("id"));
            for (var binding : dimensions) if (binding.owner == index) fields.addAll(binding.expression.properties(context));
            for (var binding : measures) if (binding.owner == index) fields.addAll(binding.expression.properties(context));
            for (var child : layers) if (Objects.equals(child.parent, index)) fields.add(child.foreignKey);
            var population = new QueryRequestSnapshot<>(layer.request, layer.request.requestScope());
            population.setAggregations(new Aggregations()); population.setSlice(null);
            population.getOrderBy().getOrderBys().clear(); population.getProjections().clear();
            population.getPropagateDimensions().clear();
            for (String field : fields) {
                layer.repository.findProperty(field);
                population.getProjections().add(new SimpleNamedExpression(field));
            }
            var compiler = new SqlAstCompiler();
            String data = compiler.buildDataSQL(new SqlEntityMetadata(layer.repository.getEntityDescriptor()),
                    layer.repository, context, population, parameters);
            ctes.add(q("__teaql_q" + index) + " AS (" + data + ")");
        }
        List<String> selections = new ArrayList<>();
        for (var dimension : dimensions)
            selections.add(field(dimension.owner, scalarProperty(dimension.expression.getExpression())) + " AS " + q(dimension.label));
        for (int index = 0; index < layers.size(); index++) selections.add(field(index, "id") + " AS " + q("__teaql_id" + index));
        for (int index = 0; index < measures.size(); index++) {
            var binding = measures.get(index);
            selections.add(field(binding.owner, scalarProperty(((AggrExpression) binding.expression.getExpression()).first())) + " AS " + q("__teaql_v" + index));
        }
        StringBuilder joins = new StringBuilder(q("__teaql_q0") + " AS " + q("l0"));
        for (int index = 1; index < layers.size(); index++) {
            var layer = layers.get(index);
            joins.append(" JOIN ").append(q("__teaql_q" + index)).append(" AS ").append(q("l" + index))
                    .append(" ON ").append(field(layer.parent, layer.foreignKey)).append(" = ").append(field(index, "id"));
        }
        ctes.add(q("__teaql_base") + " AS (SELECT " + String.join(", ", selections) + " FROM " + joins + ")");

        // Parent result bounds select dimension cells, never arbitrary parent
        // IDs before grouping. Their populations already include source filters.
        List<String> eligibility = new ArrayList<>();
        for (int owner = 1; owner < layers.size(); owner++) {
            var layer = layers.get(owner);
            if (layer.request.getSlice() == null) continue;
            var localDimensions = descendantDimensions(owner);
            var localMeasures = ownedMeasures(owner);
            String eligible = q("__teaql_eligible" + owner);
            String body = aggregateCells(context, parameters, ctes, owner, localDimensions,
                    localMeasures, "__teaql_base", "eligible");
            body = boundResult(context, parameters, layer, body, localDimensions, localMeasures);
            ctes.add(eligible + " AS (" + body + ")");
            eligibility.add("EXISTS (SELECT 1 FROM " + eligible + " AS " + q("e") + " WHERE "
                    + matchDimensions(localDimensions, "p", "e") + ")");
        }
        ctes.add(q("__teaql_population") + " AS (SELECT * FROM " + q("__teaql_base") + " AS " + q("p")
                + (eligibility.isEmpty() ? "" : " WHERE " + String.join(" AND ", eligibility)) + ")");
        String allDimensions = dimensionList(dimensions);
        ctes.add(q("__teaql_cells") + " AS (" + (dimensions.isEmpty() ? "SELECT 1 AS " + q("__teaql_unit")
                : "SELECT DISTINCT " + allDimensions + " FROM " + q("__teaql_population")) + ")");
        List<String> result = new ArrayList<>();
        for (var dimension : dimensions) result.add(q("c") + "." + q(dimension.label) + " AS " + q(dimension.label));
        StringBuilder resultJoins = new StringBuilder(q("__teaql_cells") + " AS " + q("c"));
        for (int owner = 0; owner < layers.size(); owner++) {
            var owned = ownedMeasures(owner); if (owned.isEmpty()) continue;
            String measureName = q("__teaql_measure" + owner), alias = "m" + owner;
            String body = aggregateCells(context, parameters, ctes, owner, dimensions, owned,
                    "__teaql_population", "final");
            ctes.add(measureName + " AS (" + body + ")");
            resultJoins.append(" LEFT JOIN ").append(measureName).append(" AS ").append(q(alias))
                    .append(" ON ").append(matchDimensions(dimensions, "c", alias));
            for (var measure : owned) {
                String value = q(alias) + "." + q(measure.label);
                if (((AggrExpression) measure.expression.getExpression()).getOperator() == AggrFunction.COUNT)
                    value = "COALESCE(" + value + ", 0)";
                result.add(value + " AS " + q(measure.label));
            }
        }
        String body = "SELECT " + String.join(", ", result) + " FROM " + resultJoins;
        body = boundResult(context, parameters, layers.get(0), body, dimensions, measures);
        return "WITH " + String.join(", ", ctes) + " " + body;
    }

    private String aggregateCells(UserContext context, Map<String, Object> parameters, List<String> ctes,
            int owner, List<Binding> cellDimensions, List<Binding> owned, String population, String suffix) {
        String grain = q("__teaql_grain_" + suffix + owner), id = q("__teaql_id" + owner);
        List<String> selected = new ArrayList<>();
        for (var dimension : cellDimensions) selected.add(q(dimension.label));
        selected.add(id);
        for (var measure : owned) selected.add(q("__teaql_v" + measures.indexOf(measure)));
        ctes.add(grain + " AS (SELECT DISTINCT " + String.join(", ", selected) + " FROM " + q(population) + " WHERE " + id + " IS NOT NULL)");
        List<String> projections = new ArrayList<>();
        for (var dimension : cellDimensions) projections.add(q(dimension.label));
        for (var measure : owned) {
            var aggregate = (AggrExpression) measure.expression.getExpression();
            String operand = q("__teaql_v" + measures.indexOf(measure));
            var repository = layers.get(owner).repository;
            String call = repository.aggregateSql(context, (AggrFunction) aggregate.getOperator(), aggregate.first(), operand);
            if (call == null) call = new io.teaql.core.sql.expression.AggrExpressionParser().genAggrSQL((AggrFunction) aggregate.getOperator(), operand);
            projections.add(call + " AS " + q(measure.label));
        }
        if (projections.isEmpty()) return "SELECT 1 AS " + q("__teaql_unit");
        return "SELECT " + String.join(", ", projections) + " FROM " + grain
                + (cellDimensions.isEmpty() ? "" : " GROUP BY " + dimensionList(cellDimensions));
    }

    private String boundResult(UserContext context, Map<String, Object> parameters, Layer layer, String sql,
            List<Binding> cellDimensions, List<Binding> cellMeasures) {
        List<Binding> bindings = new ArrayList<>(cellDimensions); bindings.addAll(cellMeasures);
        List<String> order = new ArrayList<>();
        for (var item : layer.request.getOrderBy().getOrderBys()) {
            Binding binding = null;
            String name = null;
            try { name = scalarProperty(item.getExpression()); } catch (TeaQLRuntimeException ignored) { }
            for (var candidate : bindings) {
                String relative = String.join(".", candidate.path.subList(layer.path.size(), candidate.path.size()));
                if (Objects.equals(relative, name) || candidate.owner == layers.indexOf(layer)
                        && candidate.expression.getExpression().equals(item.getExpression())) {
                    if (binding != null && binding != candidate) throw invalid("Ambiguous statistical order key: " + name);
                    binding = candidate;
                }
            }
            if (binding == null) throw invalid("Order key is not a statistical result: " + item.getExpression());
            String direction = item.getDirection().toUpperCase(Locale.ROOT);
            if (!Set.of("ASC", "DESC").contains(direction)) throw invalid("Invalid statistical order direction");
            order.add(q(binding.label) + " " + direction);
        }
        if (!order.isEmpty()) sql += " ORDER BY " + String.join(", ", order);
        String limit = layer.repository.prepareLimit(layer.request, parameters);
        return limit == null || limit.isBlank() ? sql : sql + " " + limit;
    }

    AggregationResult decode(List<Map<String, Object>> rows) {
        var result = new AggregationResult(); result.setName(layers.get(0).request.getAggregations().getName());
        List<AggregationItem> items = new ArrayList<>();
        for (var row : rows) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (var dimension : dimensions) insert(values, dimension.path, column(row, dimension.label));
            for (var measure : measures) insert(values, measure.path,
                    layers.get(measure.owner).repository.decodeAggregateValue(measure.expression, column(row, measure.label)));
            var item = new AggregationItem();
            for (var measure : measures) if (measure.owner == 0) item.addValue(measure.expression, values.remove(measure.expression.name()));
            values.forEach((name, value) -> item.addDimension(new SimpleNamedExpression(name), value));
            items.add(item);
        }
        result.setData(items); return result;
    }

    private List<Binding> ownedMeasures(int owner) { return measures.stream().filter(binding -> binding.owner == owner).toList(); }
    private List<Binding> descendantDimensions(int owner) {
        return dimensions.stream().filter(binding -> prefix(layers.get(owner).path, binding.path)).toList();
    }
    private String dimensionList(List<Binding> bindings) { return bindings.stream().map(binding -> q(binding.label)).collect(java.util.stream.Collectors.joining(", ")); }
    private String matchDimensions(List<Binding> bindings, String left, String right) {
        if (bindings.isEmpty()) return "1 = 1";
        return bindings.stream().map(binding -> {
            String a = q(left) + "." + q(binding.label), b = q(right) + "." + q(binding.label);
            return "(" + a + " = " + b + " OR (" + a + " IS NULL AND " + b + " IS NULL))";
        }).collect(java.util.stream.Collectors.joining(" AND "));
    }
    private String field(int owner, String property) { return q("l" + owner) + "." + q(property); }
    private String q(String name) { return root.escapeIdentifier(name); }
    private static String scalarProperty(Expression expression) {
        if (expression instanceof AggrExpression aggregate && aggregate.getOperator() == AggrFunction.SELF) expression = aggregate.first();
        if (expression instanceof PropertyReference reference) return reference.getPropertyName();
        throw invalid("Statistical dimension or operand requires a scalar property");
    }
    private static List<String> append(List<String> path, String name) { var result = new ArrayList<>(path); result.add(name); return List.copyOf(result); }
    private static boolean prefix(List<String> prefix, List<String> path) { return path.size() >= prefix.size() && path.subList(0, prefix.size()).equals(prefix); }
    private static Object column(Map<String, Object> row, String label) {
        for (var entry : row.entrySet()) if (label.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        throw invalid("Missing statistical result column: " + label);
    }
    @SuppressWarnings("unchecked") private static void insert(Map<String, Object> row, List<String> path, Object value) {
        for (int index = 0; index + 1 < path.size(); index++) row = (Map<String, Object>) row.computeIfAbsent(path.get(index), ignored -> new LinkedHashMap<>());
        row.put(path.get(path.size() - 1), value);
    }
    private static TeaQLRuntimeException invalid(String message) { return new TeaQLRuntimeException(message); }
}
