package io.teaql.tfp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.DataServiceCapabilities;
import io.teaql.core.FunctionApply;
import io.teaql.core.MutationExecutor;
import io.teaql.core.MutationDecision;
import io.teaql.core.MutationPlan;
import io.teaql.core.MutationPolicy;
import io.teaql.core.MutationPolicyIdentity;
import io.teaql.core.MutationPolicyRegistry;
import io.teaql.core.PersistenceMutation;
import io.teaql.core.MutationResult;
import io.teaql.core.QueryExecutor;
import io.teaql.core.QueryRequest;
import io.teaql.core.QueryResult;
import io.teaql.core.SmartList;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.DefaultMutationResult;
import io.teaql.runtime.DefaultQueryResult;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.EntityPersistenceMutation;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.core.criteria.Operator;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class TfpEndpointPolicyTest {
    private QueryRequest capturedQuery;
    private PersistenceMutation capturedMutation;
    private UserContext context;
    private DefaultUserContext mutationContext;
    private SimpleEntityMetaFactory metadata;

    @Before
    public void metadata() {
        metadata = new SimpleEntityMetaFactory();
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("Probe"); descriptor.setTargetType(Probe.class);
        descriptor.addSimpleProperty("id", Long.class);
        descriptor.withEntitySupplier(() -> {
            Probe probe = new Probe();
            probe.factoryCreated = true;
            return probe;
        });
        descriptor.addSimpleProperty("status", String.class);
        metadata.register(descriptor);
        EntityDescriptor status = new EntityDescriptor();
        status.setType("ProbeStatus"); status.setTargetType(ProbeStatus.class);
        metadata.register(status);
        EntityMetaFactory.registerGlobal(null);
        context = context(metadata, MutationPolicyRegistry.empty());
        mutationContext = (DefaultUserContext) context;
    }

    @Test
    public void requiresTrustedContextAndNeverDropsFilter() throws Exception {
        TfpEndpointHandler handler = handler();
        TfpEndpointException unauthorized = assertThrows(TfpEndpointException.class,
                () -> handler.handleQuery(context, query("id")));
        org.junit.Assert.assertEquals("TFP_UNAUTHORIZED", unauthorized.getCode());

        Map<String, Object> response = handler.handleQuery(context, trusted(), query("id"));
        assertNotNull(capturedQuery);
        assertNotNull(((io.teaql.runtime.DefaultQueryRequest) capturedQuery).getSearchRequest().getSearchCriteria());
        org.junit.Assert.assertTrue(response.get("data") instanceof java.util.List<?>);
    }

    @Test
    public void rejectsGovernedRequestsWithoutAnInvokingContext() {
        io.teaql.core.TeaQLRuntimeException error = assertThrows(
                io.teaql.core.TeaQLRuntimeException.class,
                () -> handler().handleQuery(null, trusted(), query("id")));
        org.junit.Assert.assertEquals(
                "Entity metadata requires a non-null UserContext", error.getMessage());
    }

    @Test
    public void rejectsForbiddenFilterUnknownFieldsAndUnsafeMutation() {
        TfpEndpointHandler handler = handler();
        assertCode("TFP_FORBIDDEN_FIELD", () -> handler.handleQuery(context, trusted(), query("secret")));
        assertCode("TFP_POLICY_VIOLATION", () -> handler.handleQuery(context, trusted(),
                "{\"entity\":\"Probe\",\"hardLimit\":999,\"commentText\":\"x\",\"purposeText\":\"x\"}".getBytes()));
        assertCode("TFP_POLICY_VIOLATION", () -> handler.handleQuery(context, trusted(),
                "{\"entity\":\"Probe\",\"idSetPagination\":{\"namespace\":\"attacker\",\"maxIds\":9999999},\"commentText\":\"x\",\"purposeText\":\"x\"}".getBytes()));
        assertCode("TFP_AUDIT_REASON_REQUIRED", () -> handler.handleMutation(context, trusted(),
                "{\"entity\":\"Probe\",\"action\":\"Create\",\"payload\":{},\"comment\":\" \"}".getBytes()));
        assertCode("TFP_FORBIDDEN_FIELD", () -> handler.handleMutation(context, trusted(),
                "{\"entity\":\"Probe\",\"action\":\"Create\",\"payload\":{\"secret\":1},\"comment\":\"x\"}".getBytes()));
    }

    @Test
    public void updateLoadsIdentityAndExpectedVersionWithoutJacksonSetters() throws Exception {
        TfpEndpointHandler handler = handler();
        handler.handleMutation(mutationContext, trusted(), ("{\"entity\":\"Probe\",\"action\":\"Update\"," 
                + "\"id\":42,\"expectedVersion\":3,\"payload\":{\"status\":\"PAID\"},"
                + "\"comment\":\"cross-language update\"}").getBytes());

        io.teaql.runtime.EntityPersistenceMutation request =
                (io.teaql.runtime.EntityPersistenceMutation) capturedMutation;
        Probe entity = (Probe) request.getEntity();
        org.junit.Assert.assertTrue(entity.factoryCreated);
        org.junit.Assert.assertEquals(Long.valueOf(42), entity.getId());
        org.junit.Assert.assertEquals(Long.valueOf(3), entity.getVersion());
        org.junit.Assert.assertEquals("PAID", entity.getStatus());
    }

    @Test
    public void mutationCannotBypassRuntimeMutationPolicy() {
        capturedMutation = null;
        MutationPolicy denying = new MutationPolicy() {
            @Override public MutationPolicyIdentity identity() {
                return new MutationPolicyIdentity("tfp.probe", "1", "sha256:deny");
            }
            @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                return MutationDecision.deny(
                        "TFP-POLICY-DENIED", "federated mutation denied", java.util.List.of());
            }
        };
        TfpEndpointHandler handler = handler(key -> java.util.Optional.of(denying));

        TeaQLRuntimeException error = assertThrows(TeaQLRuntimeException.class,
                () -> handler.handleMutation(mutationContext, trusted(),
                        ("{\"entity\":\"Probe\",\"action\":\"Update\","
                                + "\"id\":42,\"expectedVersion\":3,"
                                + "\"payload\":{\"status\":\"PAID\"},"
                                + "\"comment\":\"must be governed\"}").getBytes()));

        org.junit.Assert.assertTrue(error.getMessage().contains("TFP-POLICY-DENIED"));
        org.junit.Assert.assertNull(capturedMutation);
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
            handler.handleQuery(context, trusted(), queryWithFilter(filter));
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
                    () -> handler.handleQuery(context, trusted(), queryWithFilter(filter)));
        }
    }

    @Test
    public void mapsTrustedFacetIntoNativeRequest() throws Exception {
        TfpEndpointHandler handler = handler();
        handler.handleQuery(context, trusted(), ("{\"entity\":\"Probe\","
                + "\"filterCondition\":{\"id\":{\"$gt\":0}},"
                + "\"facets\":[{\"facetName\":\"statusFacet\",\"relationName\":\"status\","
                + "\"includeAllFacets\":true,\"query\":{\"entity\":\"ProbeStatus\","
                + "\"selectItems\":[\"id\",\"code\"],\"aggregateItems\":[{"
                + "\"function\":\"Count\",\"field\":\"id\",\"alias\":\"probeCount\"}],"
                + "\"commentText\":\"load status facet\",\"purposeText\":\"render filters\"}}],"
                + "\"limitValue\":10,\"commentText\":\"load probes\","
                + "\"purposeText\":\"render list\"}").getBytes());
        var request = ((io.teaql.runtime.DefaultQueryRequest) capturedQuery).getSearchRequest();
        org.junit.Assert.assertEquals(1, request.getFacetRequests().size());
        var facet = request.getFacetRequests().get(0);
        org.junit.Assert.assertEquals("statusFacet", facet.getFacetName());
        org.junit.Assert.assertEquals("status", facet.getRelationName());
        org.junit.Assert.assertEquals("ProbeStatus", facet.getRequest().getTypeName());
        org.junit.Assert.assertEquals("probeCount",
                facet.getRequest().getAggregations().getAggregates().get(0).name());
    }

    @Test
    public void isolatesTopLevelFacetAndMutationMetadataByInvokingContext() throws Exception {
        TfpEndpointHandler handler = handler();
        SimpleEntityMetaFactory alternate = new SimpleEntityMetaFactory();
        EntityDescriptor probe = new EntityDescriptor();
        probe.setType("Probe");
        probe.setTargetType(AlternateProbe.class);
        probe.addSimpleProperty("id", Long.class);
        probe.addSimpleProperty("status", String.class);
        alternate.register(probe);
        EntityDescriptor status = new EntityDescriptor();
        status.setType("ProbeStatus");
        status.setTargetType(AlternateProbeStatus.class);
        alternate.register(status);
        UserContext alternateContext = context(alternate);

        byte[] faceted = ("{\"entity\":\"Probe\","
                + "\"facets\":[{\"facetName\":\"statusFacet\",\"relationName\":\"status\","
                + "\"query\":{\"entity\":\"ProbeStatus\",\"selectItems\":[\"id\"],"
                + "\"aggregateItems\":[{\"function\":\"Count\",\"field\":\"id\","
                + "\"alias\":\"probeCount\"}],\"commentText\":\"load statuses\","
                + "\"purposeText\":\"render facet\"}}],\"limitValue\":10,"
                + "\"commentText\":\"load probes\",\"purposeText\":\"render list\"}")
                .getBytes();
        handler.handleQuery(alternateContext, trusted(), faceted);
        var alternateRequest =
                ((io.teaql.runtime.DefaultQueryRequest) capturedQuery).getSearchRequest();
        org.junit.Assert.assertEquals(AlternateProbe.class, alternateRequest.returnType());
        org.junit.Assert.assertTrue(
                ((BaseRequest<?>) alternateRequest).isOneOfSelfField("status"));
        org.junit.Assert.assertEquals(
                AlternateProbeStatus.class,
                alternateRequest.getFacetRequests().get(0).getRequest().returnType());

        handler.handleMutation(alternateContext, trusted(),
                "{\"entity\":\"Probe\",\"action\":\"Create\"," 
                        .concat("\"payload\":{\"status\":\"NEW\"},\"comment\":\"create probe\"}")
                        .getBytes());
        org.junit.Assert.assertEquals(
                AlternateProbe.class,
                ((io.teaql.runtime.EntityPersistenceMutation) capturedMutation)
                        .getEntity().getClass());

        handler.handleQuery(context, trusted(), query("id"));
        var originalRequest =
                ((io.teaql.runtime.DefaultQueryRequest) capturedQuery).getSearchRequest();
        org.junit.Assert.assertEquals(Probe.class, originalRequest.returnType());
    }

    private TfpEndpointHandler handler() {
        return handler(MutationPolicyRegistry.empty());
    }

    private TfpEndpointHandler handler(MutationPolicyRegistry mutationPolicies) {
        QueryExecutor query = new QueryExecutor() {
            public QueryResult query(io.teaql.core.UserContext c, QueryRequest request) {
                capturedQuery = request; return new DefaultQueryResult(new SmartList<>());
            }
            public String name() { return "test"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
        mutationContext = (DefaultUserContext) context(metadata, mutationPolicies);
        context = mutationContext;
        return new TfpEndpointHandler(query, new ObjectMapper());
    }

    private MutationExecutor mutationExecutor() {
        return new MutationExecutor() {
            public MutationResult mutate(io.teaql.core.UserContext c, PersistenceMutation request) {
                capturedMutation = request;
                return new DefaultMutationResult(((EntityPersistenceMutation) request).getEntity());
            }
            public String name() { return "test"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
    }

    private UserContext context(SimpleEntityMetaFactory metadata) {
        return context(metadata, MutationPolicyRegistry.empty());
    }

    private UserContext context(
            SimpleEntityMetaFactory metadata, MutationPolicyRegistry mutationPolicies) {
        return new DefaultUserContext(TeaQLRuntime.builder()
                .metadata(metadata)
                .dataService("default", mutationExecutor())
                .mutationPolicyRegistry(mutationPolicies)
                .idGenerationService((userContext, entity) -> 1000L)
                .build());
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
        return new TrustedFederalContext("id", 7L, "tester", "tests", Set.of("Probe", "ProbeStatus"),
                Map.of(
                        "Probe", Map.of("id", "id", "status", "status", "orderNumber", "orderNumber", "reviewed", "reviewed"),
                        "ProbeStatus", Map.of("id", "id", "code", "code")),
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
        private boolean factoryCreated;
        public String typeName() { return "Probe"; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        @Override public void __internalSet(String property, Object value) {
            if ("status".equals(property)) status = (String) value;
            else super.__internalSet(property, value);
        }
        @Override public Object __internalGet(String property) {
            return "status".equals(property) ? status : super.__internalGet(property);
        }
    }
    public static final class ProbeStatus extends BaseEntity {
        public String typeName() { return "ProbeStatus"; }
    }
    public static final class AlternateProbe extends BaseEntity {
        private String status;
        public String typeName() { return "Probe"; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }
    public static final class AlternateProbeStatus extends BaseEntity {
        public String typeName() { return "ProbeStatus"; }
    }
}
