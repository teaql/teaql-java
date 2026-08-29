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
        metadata.register(descriptor); EntityMetaFactory.registerGlobal(metadata);
    }

    private static TrustedFederalContext trusted() {
        Map<String, String> readable = Map.of(
                "id", "id", "status", "status", "orderNumber", "orderNumber",
                "reviewed", "reviewed");
        return new TrustedFederalContext("tenantId", 1L, "conformance-agent", "tfp-conformance",
                Set.of("CustomerOrder"), Map.of("CustomerOrder", readable),
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
}
