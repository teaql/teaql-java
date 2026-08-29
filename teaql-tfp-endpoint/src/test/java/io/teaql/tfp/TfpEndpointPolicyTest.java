package io.teaql.tfp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.DataServiceCapabilities;
import io.teaql.core.FunctionApply;
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
import io.teaql.core.criteria.Operator;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class TfpEndpointPolicyTest {
    private QueryRequest capturedQuery;
    private MutationRequest capturedMutation;

    @Before
    public void metadata() {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("Probe"); descriptor.setTargetType(Probe.class);
        metadata.register(descriptor); EntityMetaFactory.registerGlobal(metadata);
    }

    @Test
    public void requiresTrustedContextAndNeverDropsFilter() throws Exception {
        TfpEndpointHandler handler = handler();
        TfpEndpointException unauthorized = assertThrows(TfpEndpointException.class,
                () -> handler.handleQuery(null, query("id")));
        org.junit.Assert.assertEquals("TFP_UNAUTHORIZED", unauthorized.getCode());

        Map<String, Object> response = handler.handleQuery(null, trusted(), query("id"));
        assertNotNull(capturedQuery);
        assertNotNull(((io.teaql.runtime.DefaultQueryRequest) capturedQuery).getSearchRequest().getSearchCriteria());
        org.junit.Assert.assertTrue(response.get("data") instanceof java.util.List<?>);
    }

    @Test
    public void rejectsForbiddenFilterUnknownFieldsAndUnsafeMutation() {
        TfpEndpointHandler handler = handler();
        assertCode("TFP_FORBIDDEN_FIELD", () -> handler.handleQuery(null, trusted(), query("secret")));
        assertCode("TFP_POLICY_VIOLATION", () -> handler.handleQuery(null, trusted(),
                "{\"entity\":\"Probe\",\"hardLimit\":999,\"commentText\":\"x\",\"purposeText\":\"x\"}".getBytes()));
        assertCode("TFP_AUDIT_REASON_REQUIRED", () -> handler.handleMutation(null, trusted(),
                "{\"entity\":\"Probe\",\"action\":\"Create\",\"payload\":{},\"comment\":\" \"}".getBytes()));
        assertCode("TFP_FORBIDDEN_FIELD", () -> handler.handleMutation(null, trusted(),
                "{\"entity\":\"Probe\",\"action\":\"Create\",\"payload\":{\"secret\":1},\"comment\":\"x\"}".getBytes()));
    }

    @Test
    public void updateLoadsIdentityAndExpectedVersionWithoutJacksonSetters() throws Exception {
        TfpEndpointHandler handler = handler();
        handler.handleMutation(null, trusted(), ("{\"entity\":\"Probe\",\"action\":\"Update\","
                + "\"id\":42,\"expectedVersion\":3,\"payload\":{\"status\":\"PAID\"},"
                + "\"comment\":\"cross-language update\"}").getBytes());

        io.teaql.runtime.DefaultMutationRequest request =
                (io.teaql.runtime.DefaultMutationRequest) capturedMutation;
        Probe entity = (Probe) request.getEntity();
        org.junit.Assert.assertEquals(Long.valueOf(42), entity.getId());
        org.junit.Assert.assertEquals(Long.valueOf(3), entity.getVersion());
        org.junit.Assert.assertEquals("PAID", entity.getStatus());
    }

    @Test
    public void parsesExtendedPortablePredicatesAndNullableBoolean() throws Exception {
        TfpEndpointHandler handler = handler();
        String[] filters = {
                "{\"id\":{\"$ne\":8}}",
                "{\"id\":{\"$notIn\":[8,9]}}",
                "{\"id\":{\"$gt\":6}}",
                "{\"id\":{\"$lt\":8}}",
                "{\"id\":{\"$between\":[7,9]}}",
                "{\"orderNumber\":{\"$notContains\":\"BAD\"}}",
                "{\"orderNumber\":{\"$startsWith\":\"ORD\"}}",
                "{\"orderNumber\":{\"$notStartsWith\":\"BAD\"}}",
                "{\"orderNumber\":{\"$endsWith\":\"007\"}}",
                "{\"orderNumber\":{\"$notEndsWith\":\"999\"}}",
                "{\"reviewed\":{\"$isKnown\":true}}",
                "{\"reviewed\":{\"$isUnknown\":true}}",
                "{\"reviewed\":{\"$eq\":true}}",
                "{\"reviewed\":{\"$eq\":false}}"
        };
        Operator[] operators = {
                Operator.NOT_EQUAL, Operator.NOT_IN, Operator.GREATER_THAN,
                Operator.LESS_THAN, Operator.BETWEEN, Operator.NOT_CONTAIN,
                Operator.BEGIN_WITH, Operator.NOT_BEGIN_WITH, Operator.END_WITH,
                Operator.NOT_END_WITH, Operator.IS_NOT_NULL, Operator.IS_NULL,
                Operator.EQUAL, Operator.EQUAL
        };
        for (int i = 0; i < filters.length; i++) {
            String filter = filters[i];
            handler.handleQuery(null, trusted(), queryWithFilter(filter));
            FunctionApply all = (FunctionApply) ((io.teaql.runtime.DefaultQueryRequest) capturedQuery)
                    .getSearchRequest().getSearchCriteria();
            FunctionApply translated = (FunctionApply) all.first();
            org.junit.Assert.assertEquals(filter, operators[i], translated.getOperator());
        }
        for (String filter : new String[] {
                "{\"id\":{\"$between\":[7]}}",
                "{\"id\":{\"$notIn\":[]}}",
                "{\"reviewed\":{\"$isKnown\":false}}",
                "{\"reviewed\":{\"$isUnknown\":null}}",
                "{\"reviewed\":{\"$eq\":null}}"
        }) {
            assertCode("TFP_INVALID_REQUEST",
                    () -> handler.handleQuery(null, trusted(), queryWithFilter(filter)));
        }
    }

    private TfpEndpointHandler handler() {
        QueryExecutor query = new QueryExecutor() {
            public QueryResult query(io.teaql.core.UserContext c, QueryRequest request) {
                capturedQuery = request; return new DefaultQueryResult(new SmartList<>());
            }
            public String name() { return "test"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
        MutationExecutor mutation = new MutationExecutor() {
            public MutationResult mutate(io.teaql.core.UserContext c, MutationRequest request) {
                capturedMutation = request; return null;
            }
            public String name() { return "test"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
        return new TfpEndpointHandler(query, mutation, new ObjectMapper());
    }

    private byte[] query(String field) {
        return ("{\"entity\":\"Probe\",\"filterCondition\":{\"" + field
                + "\":{\"$eq\":1}},\"limitValue\":10,\"commentText\":\"test\",\"purposeText\":\"test\"}").getBytes();
    }

    private byte[] queryWithFilter(String filter) {
        return ("{\"entity\":\"Probe\",\"filterCondition\":" + filter
                + ",\"limitValue\":10,\"commentText\":\"test\",\"purposeText\":\"test\"}")
                .getBytes();
    }

    private TrustedFederalContext trusted() {
        return new TrustedFederalContext("id", 7L, "tester", "tests", Set.of("Probe"),
                Map.of("Probe", Map.of("id", "id", "orderNumber", "orderNumber", "reviewed", "reviewed")),
                Map.of("Probe", Map.of("status", "status")),
                Map.of("Probe", Set.of("Create", "Update")), 100);
    }

    private void assertCode(String code, Throwing action) {
        TfpEndpointException error = assertThrows(TfpEndpointException.class, () -> action.run());
        org.junit.Assert.assertEquals(code, error.getCode());
    }
    private interface Throwing { void run() throws Exception; }
    public static final class Probe extends BaseEntity {
        private String status;
        public String typeName() { return "Probe"; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }
}
