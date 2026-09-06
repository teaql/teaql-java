package io.teaql.query.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import io.teaql.core.BaseRequest;
import io.teaql.core.OrderBy;
import io.teaql.core.SearchCriteria;

/** Typed local UI-search envelope. Trusted metadata is never read from client input.
 * This does not change the legacy Java search grammar or the strict TFP decoder.
 */
public final class LocalDynamicSearch {
    private LocalDynamicSearch() {}

    public record Model(Map<String, String> fields, Map<String, String> relations) {
        public Model { fields = Map.copyOf(fields); relations = Map.copyOf(relations); }
    }
    public record Warning(String entity, String clause, String fieldPath) {
        public String code() { return DynamicSearchWarning.UNKNOWN_FIELD; }
        @com.fasterxml.jackson.annotation.JsonProperty("code")
        public String getCode() { return code(); }
    }
    public record Filter(String fieldPath, String operator, JsonNode value) {}
    public record Order(String fieldPath, String direction) {}
    public record Result(List<Filter> filters, List<Order> orders, List<Warning> warnings) {
        public Result {
            filters = List.copyOf(filters); orders = List.copyOf(orders); warnings = List.copyOf(warnings);
        }
    }

    private static final Logger LOG = Logger.getLogger(LocalDynamicSearch.class.getName());
    private static final Set<String> OPERATORS = Set.of(
            "$eq", "$ne", "$gt", "$gte", "$lt", "$lte", "$in", "$notIn", "$contains");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    /** Apply to a caller-owned Java request only after every native binding succeeds.
     * Bindings must be pure: construct criteria/orders without modifying the request,
     * and enforce authorization inside any related query they construct.
     * Paging, hard limits, projection, intent and existing filters are not replaced.
     */
    public static <T extends BaseRequest<?>> Result merge(T request, String source,
            Map<String, Model> models, Function<Filter, SearchCriteria> filterBinding,
            Function<Order, OrderBy> orderBinding, Consumer<Warning> warn) {
        Result normalized = normalize(source, request.getTypeName(), models, ignored -> {});
        List<SearchCriteria> criteria = new ArrayList<>();
        if (request.getSearchCriteria() != null) criteria.add(request.getSearchCriteria());
        for (Filter filter : normalized.filters()) {
            SearchCriteria bound = filterBinding.apply(filter);
            if (bound == null) throw invalid("Invalid trusted filter binding");
            criteria.add(bound);
        }
        List<OrderBy> orders = new ArrayList<>(request.getOrderBy().getOrderBys());
        for (Order order : normalized.orders()) {
            OrderBy bound = orderBinding.apply(order);
            if (bound == null) throw invalid("Invalid trusted order binding");
            orders.add(bound);
        }
        // Do not append into an existing mutable AND or ordering list: another
        // request can still own those nodes. Prepare replacements first.
        SearchCriteria combined = criteria.isEmpty() ? null : criteria.size() == 1 ? criteria.get(0)
                : SearchCriteria.and(criteria.toArray(SearchCriteria[]::new));
        request.replaceSearchCriteria(combined);
        request.getOrderBy().setOrderBys(orders);
        emit(normalized.warnings(), warn);
        return normalized;
    }

    public static Result normalize(String source, String entity, Map<String, Model> models,
            Consumer<Warning> warn) {
        return normalize(source, entity, models, warn, 100);
    }

    public static Result normalize(String source, String entity, Map<String, Model> models,
            Consumer<Warning> warn, int maxClauses) {
        if (maxClauses < 1 || !models.containsKey(entity)) throw invalid("Invalid trusted search setup");
        JsonNode root;
        try { root = JSON.readTree(source); }
        catch (Exception error) { throw invalid("Dynamic search requires valid JSON"); }
        checkObject(root, Set.of("filter", "orderBy"));
        JsonNode filter = root.get("filter"), order = root.get("orderBy");
        if (filter != null && !filter.isObject() || order != null && !order.isArray())
            throw invalid("Invalid search filter or ordering");
        if ((filter == null ? 0 : filter.size()) + (order == null ? 0 : order.size()) > maxClauses)
            throw invalid("Dynamic search exceeds clause limit");
        List<Filter> filters = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        if (filter != null) {
            var fields = filter.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String op = "$eq";
                JsonNode value = field.getValue();
                if (value.isObject()) {
                    if (value.size() != 1) throw invalid("Malformed search operator");
                    var operation = value.fields().next();
                    op = operation.getKey(); value = operation.getValue();
                    if (!OPERATORS.contains(op)) throw invalid("Unsupported search operator");
                }
                boolean list = op.equals("$in") || op.equals("$notIn");
                if (list && (!value.isArray() || value.size() > 1000))
                    throw invalid("Invalid or oversized search value list");
                String type = fieldType(field.getKey(), entity, models);
                if (type == null) {
                    warnings.add(new Warning(entity, "FILTER", field.getKey()));
                    continue;
                }
                if (op.equals("$contains") && !type.equals("string"))
                    throw invalid("String operator requires a string field");
                if (value.isArray()) {
                    if (!list) throw invalid("Unexpected search value list");
                    for (JsonNode item : value) scalar(item, type);
                } else scalar(value, type);
                filters.add(new Filter(field.getKey(), op, value.deepCopy()));
            }
        }
        if (order != null) for (JsonNode item : order) {
            checkObject(item, Set.of("field", "direction"));
            if (!item.has("field") || !item.get("field").isTextual()
                    || !item.has("direction") || !item.get("direction").isTextual()
                    || !Set.of("asc", "desc").contains(item.get("direction").textValue()))
                throw invalid("Invalid dynamic search ordering");
            String path = item.get("field").textValue();
            if (fieldType(path, entity, models) == null) warnings.add(new Warning(entity, "ORDER_BY", path));
            else orders.add(new Order(path, item.get("direction").textValue()));
        }
        Result result = new Result(filters, orders, warnings);
        emit(warnings, warn);
        return result;
    }

    private static void emit(List<Warning> warnings, Consumer<Warning> warn) {
        for (Warning warning : warnings) {
            if (warn != null) warn.accept(warning);
            else LOG.warning(() -> warning.code() + " entity=" + warning.entity()
                    + " clause=" + warning.clause() + " fieldPath=" + warning.fieldPath());
        }
    }

    private static void checkObject(JsonNode value, Set<String> keys) {
        if (value == null || !value.isObject()) throw invalid("Expected search object");
        value.fieldNames().forEachRemaining(key -> {
            if (!keys.contains(key)) throw invalid("Unsupported dynamic search control");
        });
    }

    private static String fieldType(String path, String entity, Map<String, Model> models) {
        String[] parts = path.split("\\.", -1);
        if (parts.length > 16) throw invalid("Invalid search field path");
        for (String part : parts) if (part.isEmpty() || part.startsWith("$")
                || Set.of("__proto__", "prototype", "constructor").contains(part))
            throw invalid("Invalid search field path");
        Model model = models.get(entity);
        for (int i = 0; i < parts.length - 1; i++) {
            String target = model.relations().get(parts[i]);
            if (target == null) return null;
            model = models.get(target);
            if (model == null) throw invalid("Invalid trusted relation metadata");
        }
        return model.fields().get(parts[parts.length - 1]);
    }

    private static void scalar(JsonNode value, String type) {
        if (value.isNull()) return;
        boolean number = value.isNumber() && Double.isFinite(value.doubleValue());
        String text = value.isTextual() ? value.textValue() : null;
        boolean valid = switch (type) {
            case "integer", "timestamp" -> number
                    && value.decimalValue().abs().compareTo(new java.math.BigDecimal("9007199254740991")) <= 0
                    && value.decimalValue().stripTrailingZeros().scale() <= 0;
            case "number" -> number;
            case "boolean" -> value.isBoolean();
            case "string" -> text != null;
            case "decimal" -> number || text != null && text.matches("[+-]?[0-9]+(?:\\.[0-9]+)?");
            case "date" -> validDate(text);
            default -> false;
        };
        if (!valid) throw invalid("Invalid value for known search field");
    }

    private static boolean validDate(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        try { return LocalDate.parse(value).getYear() >= 1; }
        catch (DateTimeParseException invalid) { return false; }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
