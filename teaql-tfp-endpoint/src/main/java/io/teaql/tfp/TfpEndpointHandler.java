package io.teaql.tfp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import io.teaql.core.MutationExecutor;
import io.teaql.core.QueryExecutor;
import io.teaql.core.UserContext;
import io.teaql.core.SearchCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.runtime.DefaultMutationRequest;
import io.teaql.runtime.DefaultQueryRequest;
import io.teaql.runtime.DefaultQueryResult;
import io.teaql.runtime.RuntimeTelemetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TfpEndpointHandler {

    private final QueryExecutor queryExecutor;
    private final MutationExecutor mutationExecutor;
    private final ObjectMapper objectMapper;
    private final RuntimeTelemetry telemetry;

    public TfpEndpointHandler(QueryExecutor queryExecutor, MutationExecutor mutationExecutor, ObjectMapper objectMapper) {
        this(queryExecutor, mutationExecutor, objectMapper, RuntimeTelemetry.NOOP);
    }

    public TfpEndpointHandler(QueryExecutor queryExecutor, MutationExecutor mutationExecutor,
            ObjectMapper objectMapper, RuntimeTelemetry telemetry) {
        this.queryExecutor = queryExecutor;
        this.mutationExecutor = mutationExecutor;
        this.objectMapper = objectMapper;
        this.telemetry = telemetry == null ? RuntimeTelemetry.NOOP : telemetry;
    }

    public Map<String, Object> handleQuery(UserContext context, byte[] payload) throws Exception {
        throw new TfpEndpointException("TFP_UNAUTHORIZED", "Trusted federation context is required");
    }

    public Map<String, Object> handleQuery(
            UserContext context, byte[] payload, Map<String, String> carrier) throws Exception {
        throw new TfpEndpointException("TFP_UNAUTHORIZED", "Trusted federation context is required");
    }

    public Map<String, Object> handleQuery(UserContext context, TrustedFederalContext trusted,
            byte[] payload) throws Exception {
        return handleQuery(context, trusted, payload, Collections.emptyMap());
    }

    public Map<String, Object> handleQuery(UserContext context, TrustedFederalContext trusted,
            byte[] payload, Map<String, String> carrier) throws Exception {
        try (RuntimeTelemetry.PropagationScope ignored =
                RuntimeTelemetry.activateSafely(telemetry, carrier)) {
            return handleQueryActive(context, trusted, payload);
        }
    }

    private Map<String, Object> handleQueryActive(UserContext context, TrustedFederalContext trusted,
            byte[] payload) throws Exception {
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("tfp", "server.query",
                        Map.of("teaql.tfp.role", "server")));
        try {
        JsonNode root = objectMapper.readTree(payload);
        rejectPrivilegedInput(root, "$", true);
        rejectUnknownTopLevel(root, java.util.Set.of("entity", "filterCondition", "limitValue",
                "offsetValue", "orderItems", "selectItems", "groupByItems", "aggregateItems",
                "commentText", "purposeText"));
        String entityName = root.path("entity").asText();

        requireAllowedEntity(trusted, entityName);
        Map<String, String> fields = trusted.readableFields(entityName);
        if (fields == null) throw new TfpEndpointException("TFP_POLICY_VIOLATION",
                "No readable field policy for entity");

        EntityDescriptor descriptor = EntityMetaFactory.get().resolveEntityDescriptor(entityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown entity: " + entityName);
        }

        BaseRequest.TempRequest request = new BaseRequest.TempRequest(descriptor.getTargetType(), entityName);

        if (root.has("limitValue") && !root.path("limitValue").isNull()) {
            int limit = root.path("limitValue").asInt();
            if (limit < 1 || limit > trusted.maxPageSize()) {
                throw new TfpEndpointException("TFP_POLICY_VIOLATION", "Invalid federation page size");
            }
            request.getSlice().setSize(limit);
        }
        if (root.has("offsetValue") && !root.path("offsetValue").isNull()) {
            int offset = root.path("offsetValue").asInt();
            if (offset < 0) throw new TfpEndpointException("TFP_INVALID_REQUEST", "Invalid offset");
            request.getSlice().setOffset(offset);
        }
        if (!root.path("groupByItems").isEmpty() || !root.path("aggregateItems").isEmpty())
            throw new TfpEndpointException("TFP_INVALID_REQUEST",
                    "Grouping and aggregation are not supported by this endpoint");

        JsonNode filter = root.get("filterCondition");
        if (filter != null && !filter.isNull()) request.appendSearchCriteria(parseFilter(request, filter, fields));
        request.appendSearchCriteria(request.createBasicSearchCriteria(
                trusted.tenantField(), Operator.EQUAL, trusted.tenantId()));

        for (JsonNode order : iterable(root.path("orderItems"))) {
            String field = mapField(fields, order.path("field").asText());
            String direction = order.path("direction").asText();
            if ("Asc".equalsIgnoreCase(direction)) request.addOrderByAscending(field);
            else if ("Desc".equalsIgnoreCase(direction)) request.addOrderByDescending(field);
            else throw new TfpEndpointException("TFP_INVALID_REQUEST", "Unsupported order direction");
        }
        for (JsonNode selected : iterable(root.path("selectItems"))) {
            request.selectProperty(mapField(fields, selected.asText()));
        }

        requireNonBlank(root, "commentText", "TFP_INVALID_REQUEST");
        requireNonBlank(root, "purposeText", "TFP_POLICY_VIOLATION");
        
        DefaultQueryRequest queryRequest = new DefaultQueryRequest(request);
        var result = queryExecutor.query(context, queryRequest);

        Map<String, Object> response = new HashMap<>();
        if (result instanceof DefaultQueryResult) {
            response.put("data", ((DefaultQueryResult) result).getResult().getData());
        } else {
            response.put("data", Collections.emptyList());
        }
        response.put("resultCode", 0);
        response.put("status", "YES");
        Object data = response.get("data");
        scope.success(Map.of("teaql.result.cardinality",
                data instanceof java.util.Collection<?> collection ? collection.size() : 0));
        return response;
        } catch (Exception | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    public Map<String, Object> handleMutation(UserContext context, byte[] payload) throws Exception {
        throw new TfpEndpointException("TFP_UNAUTHORIZED", "Trusted federation context is required");
    }

    public Map<String, Object> handleMutation(
            UserContext context, byte[] payload, Map<String, String> carrier) throws Exception {
        throw new TfpEndpointException("TFP_UNAUTHORIZED", "Trusted federation context is required");
    }

    public Map<String, Object> handleMutation(UserContext context, TrustedFederalContext trusted,
            byte[] payload) throws Exception {
        return handleMutation(context, trusted, payload, Collections.emptyMap());
    }

    public Map<String, Object> handleMutation(UserContext context, TrustedFederalContext trusted,
            byte[] payload, Map<String, String> carrier) throws Exception {
        try (RuntimeTelemetry.PropagationScope ignored =
                RuntimeTelemetry.activateSafely(telemetry, carrier)) {
            return handleMutationActive(context, trusted, payload);
        }
    }

    private Map<String, Object> handleMutationActive(UserContext context, TrustedFederalContext trusted,
            byte[] payload) throws Exception {
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("tfp", "server.mutation",
                        Map.of("teaql.tfp.role", "server")));
        try {
        JsonNode root = objectMapper.readTree(payload);
        rejectPrivilegedInput(root, "$", false);
        rejectUnknownTopLevel(root, java.util.Set.of("entity", "action", "payload", "id",
                "expectedVersion", "comment"));
        String entityName = root.path("entity").asText();
        String actionStr = root.path("action").asText();
        JsonNode entityPayload = root.path("payload");

        requireAllowedEntity(trusted, entityName);
        if (trusted.allowedActions(entityName) == null
                || !trusted.allowedActions(entityName).contains(actionStr)) {
            throw new TfpEndpointException("TFP_POLICY_VIOLATION", "Mutation action is not allowed");
        }
        if (!java.util.Set.of("Create", "Update", "Delete").contains(actionStr)) {
            throw new TfpEndpointException("TFP_INVALID_REQUEST", "Unsupported mutation action");
        }
        requireNonBlank(root, "comment", "TFP_AUDIT_REASON_REQUIRED");
        Map<String, String> writable = trusted.writableFields(entityName);
        if (writable == null || !entityPayload.isObject()) {
            throw new TfpEndpointException("TFP_INVALID_REQUEST", "Invalid mutation payload");
        }
        ObjectNode mappedPayload = objectMapper.createObjectNode();
        entityPayload.fields().forEachRemaining(entry -> {
            String mapped = writable.get(entry.getKey());
            if (mapped == null) throw new TfpEndpointException("TFP_FORBIDDEN_FIELD",
                    "Mutation field is not allowed: " + entry.getKey());
            mappedPayload.set(mapped, entry.getValue());
        });
        mappedPayload.set(trusted.tenantField(), objectMapper.valueToTree(trusted.tenantId()));
        if (!"Create".equals(actionStr)) {
            if (!root.hasNonNull("id")) throw new TfpEndpointException(
                    "TFP_INVALID_REQUEST", "Mutation id is required");
        }

        EntityDescriptor descriptor = EntityMetaFactory.get().resolveEntityDescriptor(entityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown entity: " + entityName);
        }

        Entity entity = (Entity) objectMapper.treeToValue(mappedPayload, descriptor.getTargetType());
        if (!"Create".equals(actionStr)) {
            entity.setProperty("id", root.get("id").longValue());
        }
        if (root.hasNonNull("expectedVersion")) {
            entity.setProperty("version", root.get("expectedVersion").longValue());
        }

        DefaultMutationRequest.Action action = "Delete".equalsIgnoreCase(actionStr) ? 
                DefaultMutationRequest.Action.DELETE : DefaultMutationRequest.Action.SAVE;

        DefaultMutationRequest mutationRequest = new DefaultMutationRequest(entity, action);
        mutationExecutor.mutate(context, mutationRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("affectedRows", 1);
        response.put("resultCode", 0);
        response.put("status", "YES");
        response.put("data", Collections.singletonList(entity));

        scope.success();
        return response;
        } catch (Exception | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    private void requireAllowedEntity(TrustedFederalContext trusted, String entity) {
        if (entity == null || entity.isBlank() || !trusted.allowedEntities().contains(entity)) {
            throw new TfpEndpointException("TFP_FORBIDDEN_ENTITY", "Entity is not allowed");
        }
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return Collections.emptyList();
        if (!node.isArray()) throw new TfpEndpointException("TFP_INVALID_REQUEST", "Expected array");
        return node;
    }

    private String mapField(Map<String, String> fields, String field) {
        String mapped = fields.get(field);
        if (mapped == null) throw new TfpEndpointException("TFP_FORBIDDEN_FIELD", "Field is not allowed: " + field);
        return mapped;
    }

    private SearchCriteria parseFilter(BaseRequest.TempRequest request, JsonNode node,
            Map<String, String> fields) {
        if (!node.isObject() || node.size() != 1) {
            throw new TfpEndpointException("TFP_INVALID_REQUEST", "Filter must contain one expression");
        }
        var entry = node.fields().next();
        if ("$and".equals(entry.getKey()) || "$or".equals(entry.getKey())) {
            if (!entry.getValue().isArray() || entry.getValue().isEmpty())
                throw new TfpEndpointException("TFP_INVALID_REQUEST", "Logical filter requires operands");
            java.util.List<SearchCriteria> parts = new java.util.ArrayList<>();
            entry.getValue().forEach(child -> parts.add(parseFilter(request, child, fields)));
            SearchCriteria[] values = parts.toArray(new SearchCriteria[0]);
            return "$and".equals(entry.getKey()) ? SearchCriteria.and(values) : SearchCriteria.or(values);
        }
        String field = mapField(fields, entry.getKey());
        JsonNode predicate = entry.getValue();
        if (!predicate.isObject() || predicate.size() != 1)
            throw new TfpEndpointException("TFP_INVALID_REQUEST", "Invalid field predicate");
        var operation = predicate.fields().next();
        Operator operator;
        switch (operation.getKey()) {
            case "$eq": operator = Operator.EQUAL; break;
            case "$ne": operator = Operator.NOT_EQUAL; break;
            case "$gt": operator = Operator.GREATER_THAN; break;
            case "$gte": operator = Operator.GREATER_THAN_OR_EQUAL; break;
            case "$lt": operator = Operator.LESS_THAN; break;
            case "$lte": operator = Operator.LESS_THAN_OR_EQUAL; break;
            case "$contains": operator = Operator.CONTAIN; break;
            case "$notContains": operator = Operator.NOT_CONTAIN; break;
            case "$startsWith": operator = Operator.BEGIN_WITH; break;
            case "$notStartsWith": operator = Operator.NOT_BEGIN_WITH; break;
            case "$endsWith": operator = Operator.END_WITH; break;
            case "$notEndsWith": operator = Operator.NOT_END_WITH; break;
            case "$in": operator = Operator.IN; break;
            case "$notIn": operator = Operator.NOT_IN; break;
            case "$between": operator = Operator.BETWEEN; break;
            case "$isKnown": operator = Operator.IS_NOT_NULL; break;
            case "$isUnknown": operator = Operator.IS_NULL; break;
            default: throw new TfpEndpointException("TFP_INVALID_REQUEST", "Unsupported predicate operator");
        }
        JsonNode operand = operation.getValue();
        if ((operator == Operator.EQUAL || operator == Operator.NOT_EQUAL
                || operator == Operator.GREATER_THAN || operator == Operator.GREATER_THAN_OR_EQUAL
                || operator == Operator.LESS_THAN || operator == Operator.LESS_THAN_OR_EQUAL)
                && operand.isNull()) {
            throw new TfpEndpointException("TFP_INVALID_REQUEST",
                    operation.getKey() + " does not accept null; use $isKnown or $isUnknown");
        }
        if (operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL) {
            if (!operand.isBoolean() || !operand.booleanValue())
                throw new TfpEndpointException("TFP_INVALID_REQUEST",
                        operation.getKey() + " requires true");
            return request.createBasicSearchCriteria(field, operator);
        }
        if (operator == Operator.BETWEEN) {
            if (!operand.isArray() || operand.size() != 2)
                throw new TfpEndpointException("TFP_INVALID_REQUEST",
                        "$between requires exactly two values");
            Object lower = objectMapper.convertValue(operand.get(0), Object.class);
            Object upper = objectMapper.convertValue(operand.get(1), Object.class);
            return request.createBasicSearchCriteria(field, operator, lower, upper);
        }
        if (operator == Operator.IN || operator == Operator.NOT_IN) {
            if (!operand.isArray() || operand.isEmpty() || operand.size() > 100)
                throw new TfpEndpointException("TFP_INVALID_REQUEST",
                        operation.getKey() + " size must be between 1 and 100");
        }
        Object value = objectMapper.convertValue(operand, Object.class);
        return request.createBasicSearchCriteria(field, operator, value);
    }

    private void requireNonBlank(JsonNode root, String field, String code) {
        if (!root.has(field) || root.path(field).asText().trim().isEmpty())
            throw new TfpEndpointException(code, field + " is required");
    }

    private void rejectPrivilegedInput(JsonNode node, String path, boolean query) {
        if (!node.isObject()) throw new TfpEndpointException("TFP_INVALID_REQUEST", "TFP payload must be an object");
        java.util.Set<String> forbidden = java.util.Set.of("tenant", "tenantId", "merchant", "merchantId",
                "user", "userId", "permissions", "requestPolicy", "purposePolicy", "trustedContext",
                "hardLimit", "hard_limit", "hardLimitValue", "hard_limit_value");
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            if (forbidden.contains(entry.getKey())) throw new TfpEndpointException(
                    "TFP_POLICY_VIOLATION", "Client cannot provide server-owned field at " + path);
            if (entry.getValue().isObject()) rejectPrivilegedInput(entry.getValue(), path + "." + entry.getKey(), query);
            else if (entry.getValue().isArray()) for (JsonNode child : entry.getValue())
                if (child.isObject()) rejectPrivilegedInput(child, path + "." + entry.getKey(), query);
        }
    }

    private void rejectUnknownTopLevel(JsonNode root, java.util.Set<String> allowed) {
        root.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) throw new TfpEndpointException(
                    "TFP_INVALID_REQUEST", "Unknown TFP field: " + field);
        });
    }
}
