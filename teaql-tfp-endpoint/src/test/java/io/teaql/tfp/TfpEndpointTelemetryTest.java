package io.teaql.tfp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.teaql.runtime.RuntimeTelemetry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class TfpEndpointTelemetryTest {
    private final RecordingTelemetry telemetry = new RecordingTelemetry();

    @Before
    public void registerMetadata() {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("Probe");
        descriptor.setTargetType(Probe.class);
        metadata.register(descriptor);
        EntityMetaFactory.registerGlobal(metadata);
    }

    @Test
    public void recordsServerQuerySuccessAndCardinality() throws Exception {
        SmartList<Probe> rows = new SmartList<>();
        rows.add(new Probe());
        TfpEndpointHandler handler = new TfpEndpointHandler(
                queryExecutor(request -> new DefaultQueryResult(rows)),
                mutationExecutor(), new ObjectMapper(), telemetry);

        Map<String, Object> response = handler.handleQuery(null,
                "{\"entity\":\"Probe\",\"limitValue\":10}".getBytes());

        assertEquals(rows, response.get("data"));
        assertEquals("tfp", telemetry.operations.get(0).family());
        assertEquals("server.query", telemetry.operations.get(0).name());
        assertEquals("server", telemetry.operations.get(0).attributes().get("teaql.tfp.role"));
        assertEquals(1, telemetry.completions.get(0).get("teaql.result.cardinality"));
    }

    @Test
    public void recordsFailureAndRethrowsOriginalError() {
        IllegalStateException original = new IllegalStateException("provider failed");
        TfpEndpointHandler handler = new TfpEndpointHandler(
                queryExecutor(request -> { throw original; }),
                mutationExecutor(), new ObjectMapper(), telemetry);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> handler.handleQuery(null, "{\"entity\":\"Probe\"}".getBytes()));

        assertSame(original, thrown);
        assertSame(original, telemetry.failures.get(0));
    }

    @Test
    public void recordsServerMutationLifecycle() throws Exception {
        TfpEndpointHandler handler = new TfpEndpointHandler(
                queryExecutor(request -> new DefaultQueryResult(new SmartList<>())),
                mutationExecutor(), new ObjectMapper(), telemetry);

        handler.handleMutation(null,
                "{\"entity\":\"Probe\",\"action\":\"Save\",\"payload\":{}}".getBytes());

        assertEquals("tfp", telemetry.operations.get(0).family());
        assertEquals("server.mutation", telemetry.operations.get(0).name());
        assertEquals("server", telemetry.operations.get(0).attributes().get("teaql.tfp.role"));
        assertEquals(1, telemetry.completions.size());
    }

    @Test
    public void activatesCarrierBeforeStartingServerSpanAndRestoresAfterward() throws Exception {
        telemetry.expectedCarrier = Map.of("TraceParent", "00-trace-span-01");
        TfpEndpointHandler handler = new TfpEndpointHandler(
                queryExecutor(request -> new DefaultQueryResult(new SmartList<>())),
                mutationExecutor(), new ObjectMapper(), telemetry);

        handler.handleQuery(null, "{\"entity\":\"Probe\"}".getBytes(),
                telemetry.expectedCarrier);

        assertEquals(List.of("activate", "start:server.query", "close"),
                telemetry.propagationEvents);
    }

    public static final class Probe extends BaseEntity {
        @Override
        public String typeName() { return "Probe"; }
    }

    private static QueryExecutor queryExecutor(
            java.util.function.Function<QueryRequest, QueryResult> query) {
        return new QueryExecutor() {
            @Override
            public QueryResult query(io.teaql.core.UserContext context, QueryRequest request) {
                return query.apply(request);
            }

            @Override
            public String name() { return "test"; }

            @Override
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
    }

    private static MutationExecutor mutationExecutor() {
        return new MutationExecutor() {
            @Override
            public MutationResult mutate(io.teaql.core.UserContext context, MutationRequest request) {
                return null;
            }

            @Override
            public String name() { return "test"; }

            @Override
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
    }

    private static final class RecordingTelemetry implements RuntimeTelemetry {
        private final List<Operation> operations = new ArrayList<>();
        private final List<Map<String, Object>> completions = new ArrayList<>();
        private final List<Throwable> failures = new ArrayList<>();
        private final List<String> propagationEvents = new ArrayList<>();
        private Map<String, String> expectedCarrier;

        @Override
        public PropagationScope extractAndActivate(Map<String, String> carrier) {
            assertSame(expectedCarrier, carrier);
            propagationEvents.add("activate");
            return () -> propagationEvents.add("close");
        }

        @Override
        public Scope start(Operation operation) {
            if (expectedCarrier != null) propagationEvents.add("start:" + operation.name());
            operations.add(operation);
            return new Scope() {
                @Override
                public void success(Map<String, Object> attributes) {
                    completions.add(attributes);
                }

                @Override
                public void failure(Throwable error) {
                    failures.add(error);
                }
            };
        }
    }
}
