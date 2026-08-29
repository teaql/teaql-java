package io.teaql.tfp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.teaql.core.BaseEntity;
import io.teaql.core.DataServiceCapabilities;
import io.teaql.core.MutationExecutor;
import io.teaql.core.MutationRequest;
import io.teaql.core.MutationResult;
import io.teaql.core.QueryExecutor;
import io.teaql.core.QueryRequest;
import io.teaql.core.QueryResult;
import io.teaql.core.SmartList;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.DefaultQueryResult;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/** Standalone process used only by teaql-conformance cross-language gates. */
public final class TfpConformanceServer {
    private TfpConformanceServer() {}

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("TEAQL_TFP_JAVA_PORT", "19092"));
        ObjectMapper mapper = new ObjectMapper();
        registerMetadata();
        TfpEndpointHandler endpoint = new TfpEndpointHandler(queryExecutor(), mutationExecutor(), mapper);
        TrustedFederalContext trusted = trusted();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/query", exchange -> handle(exchange, mapper,
                () -> endpoint.handleQuery(null, trusted, exchange.getRequestBody().readAllBytes(), headers(exchange))));
        server.createContext("/mutate", exchange -> handle(exchange, mapper,
                () -> endpoint.handleMutation(null, trusted, exchange.getRequestBody().readAllBytes(), headers(exchange))));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        System.out.println("Java TFP conformance server listening on 127.0.0.1:" + port);
        new CountDownLatch(1).await();
    }

    private static void handle(HttpExchange exchange, ObjectMapper mapper, Action action) {
        int status = 200;
        Object response;
        try {
            if (!"POST".equals(exchange.getRequestMethod())) throw new TfpEndpointException(
                    "TFP_INVALID_REQUEST", "POST is required");
            response = action.run();
        } catch (TfpEndpointException error) {
            status = 400;
            response = Map.of("code", error.getCode(), "message", error.getMessage());
        } catch (Exception error) {
            error.printStackTrace(System.err);
            status = 500;
            response = Map.of("code", "TFP_EXECUTION_FAILED", "message", "TFP request failed");
        }
        try {
            byte[] body = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        } catch (Exception ignored) {
            // The conformance process will fail its client gate if the response cannot be written.
        } finally {
            exchange.close();
        }
    }

    private static Map<String, String> headers(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) result.put(name, values.get(0));
        });
        return result;
    }

    private static void registerMetadata() {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("CustomerOrder"); descriptor.setTargetType(CustomerOrder.class);
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        metadata.register(descriptor);
        EntityDescriptor statusDescriptor = new EntityDescriptor();
        statusDescriptor.setType("OrderStatus"); statusDescriptor.setTargetType(OrderStatus.class);
        statusDescriptor.addSimpleProperty("id", Long.class);
        statusDescriptor.addSimpleProperty("code", String.class);
        statusDescriptor.addSimpleProperty("label", String.class);
        metadata.register(statusDescriptor);
        EntityMetaFactory.registerGlobal(metadata);
    }

    private static TrustedFederalContext trusted() {
        Map<String, String> readable = Map.of(
                "id", "id", "status", "status", "orderNumber", "orderNumber",
                "reviewed", "reviewed");
        return new TrustedFederalContext("tenantId", 1L, "conformance-agent", "tfp-conformance",
                Set.of("CustomerOrder", "OrderStatus"), Map.of(
                        "CustomerOrder", readable,
                        "OrderStatus", Map.of("id", "id", "code", "code", "label", "label")),
                Map.of("CustomerOrder", Map.of("status", "status")),
                Map.of("CustomerOrder", Set.of("Create", "Update", "Delete")), 100);
    }

    private static QueryExecutor queryExecutor() {
        return new QueryExecutor() {
            public QueryResult query(io.teaql.core.UserContext context, QueryRequest request) {
                CustomerOrder order = new CustomerOrder();
                order.updateId(7L); order.updateVersion(1L); order.setTenantId(1L);
                order.setStatus("NEW"); order.setOrderNumber("ORD-007");
                order.setReviewed(Boolean.TRUE);
                SmartList<CustomerOrder> rows = new SmartList<>(); rows.add(order);
                var searchRequest = ((io.teaql.runtime.DefaultQueryRequest) request).getSearchRequest();
                if (!searchRequest.getFacetRequests().isEmpty()) {
                    SmartList<OrderStatus> statuses = new SmartList<>();
                    OrderStatus newest = new OrderStatus(); newest.updateId(1001L);
                    newest.setCode("NEW"); newest.setLabel("New");
                    newest.addDynamicProperty("orderCount", 1); statuses.add(newest);
                    OrderStatus paid = new OrderStatus(); paid.updateId(1002L);
                    paid.setCode("PAID"); paid.setLabel("Paid");
                    paid.addDynamicProperty("orderCount", 0); statuses.add(paid);
                    rows.addFacet(searchRequest.getFacetRequests().get(0).getFacetName(), statuses);
                }
                return new DefaultQueryResult(rows);
            }
            public String name() { return "tfp-conformance"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
    }

    private static MutationExecutor mutationExecutor() {
        return new MutationExecutor() {
            public MutationResult mutate(io.teaql.core.UserContext context, MutationRequest request) {
                return null;
            }
            public String name() { return "tfp-conformance"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
    }

    private interface Action { Object run() throws Exception; }

    public static final class CustomerOrder extends BaseEntity {
        private String status;
        private String orderNumber;
        private Long tenantId;
        private Boolean reviewed;
        public String typeName() { return "CustomerOrder"; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String value) { orderNumber = value; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long value) { tenantId = value; }
        public Boolean getReviewed() { return reviewed; }
        public void setReviewed(Boolean value) { reviewed = value; }
    }

    public static final class OrderStatus extends BaseEntity {
        private String code;
        private String label;
        public String typeName() { return "OrderStatus"; }
        public String getCode() { return code; }
        public void setCode(String value) { code = value; }
        public String getLabel() { return label; }
        public void setLabel(String value) { label = value; }
        @Override public Object __internalGet(String property) {
            return switch (property) {
                case "code" -> code;
                case "label" -> label;
                default -> super.__internalGet(property);
            };
        }
    }
}
