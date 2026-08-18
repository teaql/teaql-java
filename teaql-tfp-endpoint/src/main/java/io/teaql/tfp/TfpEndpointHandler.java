package io.teaql.tfp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseRequest;
import io.teaql.core.Entity;
import io.teaql.core.MutationExecutor;
import io.teaql.core.QueryExecutor;
import io.teaql.core.UserContext;
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
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("tfp", "server.query",
                        Map.of("teaql.tfp.role", "server")));
        try {
        JsonNode root = objectMapper.readTree(payload);
        String entityName = root.path("entity").asText();

        EntityDescriptor descriptor = EntityMetaFactory.get().resolveEntityDescriptor(entityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown entity: " + entityName);
        }

        BaseRequest.TempRequest request = new BaseRequest.TempRequest(descriptor.getTargetType(), entityName);

        if (root.has("limitValue") && !root.path("limitValue").isNull()) {
            request.getSlice().setSize(root.path("limitValue").asInt());
        }
        if (root.has("offsetValue") && !root.path("offsetValue").isNull()) {
            request.getSlice().setOffset(root.path("offsetValue").asInt());
        }

        // TODO: Full translation for OrderItems, SelectItems, FilterCondition based on TFP protocol
        // Current implementation is a basic mapper for query.
        
        DefaultQueryRequest queryRequest = new DefaultQueryRequest(request);
        var result = queryExecutor.query(context, queryRequest);

        Map<String, Object> response = new HashMap<>();
        if (result instanceof DefaultQueryResult) {
            response.put("data", ((DefaultQueryResult) result).getResult());
        } else {
            response.put("data", Collections.emptyList());
        }
        response.put("resultCode", 0);
        response.put("status", "YES");
        Object data = response.get("data");
        scope.success(Map.of("teaql.result.cardinality",
                data instanceof io.teaql.core.SmartList<?> list ? list.size()
                        : data instanceof java.util.Collection<?> collection ? collection.size() : 0));
        return response;
        } catch (Exception | Error error) {
            scope.failure(error);
            throw error;
        }
    }

    public Map<String, Object> handleMutation(UserContext context, byte[] payload) throws Exception {
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("tfp", "server.mutation",
                        Map.of("teaql.tfp.role", "server")));
        try {
        JsonNode root = objectMapper.readTree(payload);
        String entityName = root.path("entity").asText();
        String actionStr = root.path("action").asText();
        JsonNode entityPayload = root.path("payload");

        EntityDescriptor descriptor = EntityMetaFactory.get().resolveEntityDescriptor(entityName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown entity: " + entityName);
        }

        Entity entity = (Entity) objectMapper.treeToValue(entityPayload, descriptor.getTargetType());

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
}
