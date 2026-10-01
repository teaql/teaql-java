package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.criteria.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.*;
import io.teaql.core.sql.expression.*;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlParameterPropagationTest {
    public static class Customer extends BaseEntity { @Override public String typeName() { return "Customer"; } }
    public static class Request extends BaseRequest<Customer> {
        public Request() { super(Customer.class); }
        @Override public String getTypeName() { return "Customer"; }
    }
    private record Captured(String sql, Object[] args, SqlLogBindings bindings) {}
    private static class Fixture {
        final List<Captured> captured = new ArrayList<>();
        final SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        final PortableSQLRepository<Customer> repository;
        final TeaQLRuntime runtime;
        final java.util.concurrent.atomic.AtomicInteger compilations = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger loads = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger aggregates = new java.util.concurrent.atomic.AtomicInteger();
        Fixture() { this(true); }
        Fixture(boolean declared) {
            descriptor.setType("Customer"); descriptor.setTargetType(Customer.class);
            descriptor.setEntitySupplier(Customer::new); descriptor.setDataService("test");
            descriptor.with("table_name", "customer_data");
            for (String field : List.of("id", "version", "title", "status", "password")) {
                var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                        field.equals("id") || field.equals("version") ? Long.class : String.class);
                property.setColumnType(field.equals("id") || field.equals("version") ? "BIGINT" : "VARCHAR(200)");
            }
            if (declared) descriptor.setAuditMaskFields(List.of("title"));
            var factory = new SimpleEntityMetaFactory(); factory.register(descriptor);
            runtime = TeaQLRuntime.builder().metadata(factory).build();
            TeaQLDatabase db = (TeaQLDatabase) Proxy.newProxyInstance(TeaQLDatabase.class.getClassLoader(),
                    new Class<?>[]{TeaQLDatabase.class}, (proxy, method, args) -> {
                        if (method.getName().equals("supportsCompiledRowMapping")) return false;
                        if (method.getName().equals("query") && args.length == 4 && args[3] instanceof SqlLogBindings bindings) {
                            captured.add(new Captured((String) args[1], (Object[]) args[2], bindings));
                            return List.of();
                        }
                        if (method.getName().equals("batchUpdate") && args.length == 4 && args[3] instanceof SqlLogBindings bindings) {
                            var rows = (List<Object[]>) args[2];
                            for (var row : rows) captured.add(new Captured((String) args[1], row, bindings));
                            int[] counts = new int[rows.size()]; Arrays.fill(counts, 1); return counts;
                        }
                        throw new AssertionError("unexpected database call " + method);
                    });
            repository = new PortableSQLRepository<>(descriptor, db, null, factory) {
                @Override public SmartList<Customer> loadInternal(UserContext context, SearchRequest<Customer> request) {
                    loads.incrementAndGet(); return super.loadInternal(context, request);
                }
                @Override protected AggregationResult doAggregateInternal(UserContext context, SearchRequest<Customer> request) {
                    aggregates.incrementAndGet(); return super.doAggregateInternal(context, request);
                }
                @Override public String buildDataSQL(UserContext context, SearchRequest request, Map<String, Object> params) {
                    compilations.incrementAndGet();
                    return super.buildDataSQL(context, request, params);
                }
            };
        }
        DefaultUserContext context() { return new DefaultUserContext(runtime); }
        Request request(String field, Operator op, Object... values) {
            Request request = new Request() {
                { internalComment("verify SQL parameter provenance"); internalPurpose("test compiled plan privacy"); }
            };
            request.selectSelf(); request.offset(0, 10);
            request.appendSearchCriteria(request.createBasicSearchCriteria(field, op, values));
            return request;
        }
    }

    @Test public void legacyMissingMaskMetadataFailsClosedUntilExplicitlyDeclared() {
        var f = new Fixture(false);
        assertEquals(SqlParameterLogPolicy.UNKNOWN, f.repository.parameterLogPolicy("status"));
        assertEquals(SqlParameterLogPolicy.CREDENTIAL, f.repository.parameterLogPolicy("password"));
        f.repository.loadInternal(f.context(), f.request("status", Operator.EQUAL, "PRIVATE-CANARY"));
        var captured = f.captured.get(0);
        assertEquals(SqlParameterLogPolicy.UNKNOWN,
                captured.bindings.policies().get(Arrays.asList(captured.args).indexOf("PRIVATE-CANARY")));
        f.descriptor.setAuditMaskFields(List.of());
        assertEquals(SqlParameterLogPolicy.PLAIN, f.repository.parameterLogPolicy("status"));
    }

    @Test public void cachedAndUncachedQueriesCarryFieldPoliciesAndDerivedLikeValues() {
        var f = new Fixture();
        for (String value : List.of("Riverside", "Lakeside")) {
            Request request = f.request("title", Operator.CONTAIN, value);
            request.appendSearchCriteria(request.createBasicSearchCriteria("status", Operator.EQUAL, "ACTIVE"));
            f.repository.loadInternal(f.context(), request);
        }
        assertEquals(2, f.captured.size());
        assertEquals("the second query must hit the compiled plan", 1, f.compilations.get());
        for (int i = 0; i < 2; i++) {
            var c = f.captured.get(i);
            assertTrue(c.bindings.generated());
            assertEquals(c.args.length, c.bindings.policies().size());
            int title = Arrays.asList(c.args).indexOf(i == 0 ? "%Riverside%" : "%Lakeside%");
            assertTrue(Arrays.toString(c.args), title >= 0);
            assertEquals(SqlParameterLogPolicy.MASKED, c.bindings.policies().get(title));
            assertEquals(SqlParameterLogPolicy.PLAIN, c.bindings.policies().get(Arrays.asList(c.args).indexOf("ACTIVE")));
            assertEquals(SqlParameterLogPolicy.PLAIN, c.bindings.policies().get(c.args.length - 1));
        }
    }

    @Test public void cachedPlansNeverRetainQueryIntentAndScopesKeepCurrentBindingsOnly() {
        var f = new Fixture();
        for (String value : List.of("Riverside", "Lakeside")) {
            var source = new SqlIntentRedactions();
            source.capture(List.of(SqlParameterLogPolicy.CREDENTIAL), new Object[]{"ancestor-" + value});
            var scoped = new SqlDiagnosticRequest(f.request("title", Operator.EQUAL, value), source);
            f.repository.loadInternal(f.context(), scoped);
            var captured = f.captured.get(f.captured.size() - 1);
            var values = new ArrayList<Object>();
            captured.bindings.intentRedactions().appendTo(values, false);
            assertEquals(List.of("ancestor-" + value, value), values);
            var ancestor = new ArrayList<Object>(); source.appendTo(ancestor, false);
            assertEquals(List.of("ancestor-" + value), ancestor);
            assertFalse(scoped.getExtensions().toString().contains("ancestor-"));
        }
        assertEquals("same query shape compiles only once", 1, f.compilations.get());
    }

    @Test public void scopedExecutionPreservesRepositoryOverridesAndSharesOnlyInvocationOutput() {
        var f = new Fixture();
        var source = new SqlIntentRedactions();
        f.repository.loadInternal(f.context(), f.request("title", Operator.EQUAL, "Riverside"), source);
        assertEquals(1, f.loads.get());
        var values = new ArrayList<Object>(); source.appendTo(values, false);
        assertEquals(List.of("Riverside"), values);
        var aggregate = f.request("title", Operator.EQUAL, "Lakeside"); aggregate.count("count");
        var aggregateSource = new SqlIntentRedactions();
        f.repository.doAggregateInternal(f.context(), aggregate, aggregateSource);
        assertEquals(1, f.aggregates.get());
        values.clear(); aggregateSource.appendTo(values, false);
        assertEquals(List.of("Lakeside"), values);
    }

    @Test public void diagnosticWrapperPreservesQuerySemanticsBeyondSqlCriteria() {
        var selection = new io.teaql.data.dynamic.DynamicFieldSelection().selectString("preferred_channel");
        var original = new Request() {
            @Override public String getSearchForText() { return "Riverside"; }
            @Override public String comment() { return "query comment"; }
            @Override public String purpose() { return "query purpose"; }
            @Override public int hardLimit() { return 27; }
            @Override public boolean tryUseSubQuery() { return false; }
            @Override public Customer internalNewEntity() { return new Customer(); }
            @Override public io.teaql.data.dynamic.DynamicFieldSelection getDynamicFieldSelection() { return selection; }
        };
        for (var request : List.of(new SqlDiagnosticRequest(original, new SqlIntentRedactions()),
                SqlDiagnosticRequest.forExecution(original, new SqlIntentRedactions()))) {
            assertEquals(original.getSearchForText(), request.getSearchForText());
            assertEquals(original.comment(), request.comment());
            assertEquals(original.purpose(), request.purpose());
            assertEquals(original.hardLimit(), request.hardLimit());
            assertFalse(request.tryUseSubQuery());
            assertTrue(request.internalNewEntity() instanceof Customer);
            assertSame(selection, request.getDynamicFieldSelection());
        }
    }

    @Test public void expandedCollectionsAndCredentialPredicatesKeepPolicies() {
        var f = new Fixture();
        Request request = f.request("title", Operator.IN, "Riverside", "Lakeside");
        request.appendSearchCriteria(request.createBasicSearchCriteria("password", Operator.EQUAL, "PASSWORD-CANARY"));
        f.repository.loadInternal(f.context(), request);
        var c = f.captured.get(0);
        for (String value : List.of("Riverside", "Lakeside"))
            assertEquals(SqlParameterLogPolicy.MASKED, c.bindings.policies().get(Arrays.asList(c.args).indexOf(value)));
        assertEquals(SqlParameterLogPolicy.CREDENTIAL,
                c.bindings.policies().get(Arrays.asList(c.args).indexOf("PASSWORD-CANARY")));
    }

    @Test public void parameterNameCannotPretendToBeAnOrdinaryField() {
        var f = new Fixture(); var params = new SqlParameters();
        Request request = new Request(); request.selectSelf(); request.offset(0, 1);
        request.appendSearchCriteria(new EQ(new PropertyReference("title"), new Parameter("status", "Riverside", Operator.EQUAL)));
        f.repository.buildDataSQL(f.context(), request, params);
        assertEquals(SqlParameterLogPolicy.MASKED, params.policy("status"));
    }

    private static class Raw extends Parameter implements SQLExpressionParser<Raw> {
        Raw() { super("raw", "RAW-CANARY", Operator.EQUAL); }
        @Override public String toSql(UserContext context, Raw expression, String table,
                Map<String, Object> params, SQLColumnResolver resolver) { return "'RAW-CANARY'"; }
    }

    @Test public void customSqlCannotClaimGeneratedSqlProvenance() {
        var f = new Fixture(); var parameters = new SqlParameters();
        var request = new Request(); request.selectSelf(); request.offset(0, 1);
        request.appendSearchCriteria(new EQ(new PropertyReference("status"), new Raw()));
        String sql = f.repository.buildDataSQL(f.context(), request, parameters);
        assertTrue(sql.contains("RAW-CANARY"));
        assertFalse(parameters.generated());
    }

    @Test public void compilationStateIsIndependentAcrossConcurrentCalls() throws Exception {
        var f = new Fixture(); var pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> calls = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                String field = i % 2 == 0 ? "title" : "status";
                calls.add(() -> {
                    var parameters = new SqlParameters();
                    f.repository.buildDataSQL(f.context(), f.request(field, Operator.EQUAL, "SAME-VALUE"), parameters);
                    assertEquals(field.equals("title") ? SqlParameterLogPolicy.MASKED : SqlParameterLogPolicy.PLAIN,
                            parameters.policy(field));
                    assertEquals(SqlParameterLogPolicy.UNKNOWN, parameters.currentPolicy());
                    return null;
                });
            }
            for (Future<Void> result : pool.invokeAll(calls)) result.get();
        } finally { pool.shutdownNow(); }
    }

    @Test public void rawMapReplacementCannotRetainOldOrdinaryClassification() {
        var parameters = new SqlParameters();
        SqlParameters.bind(parameters, "status", "ACTIVE", SqlParameterLogPolicy.PLAIN);
        parameters.put("status", "REPLACEMENT-CANARY");
        assertEquals(SqlParameterLogPolicy.UNKNOWN, parameters.policy("status"));
        SqlParameters.bind(parameters, "status", "ACTIVE", SqlParameterLogPolicy.PLAIN);
        parameters.putAll(Map.of("status", "SECOND-CANARY"));
        assertEquals(SqlParameterLogPolicy.UNKNOWN, parameters.policy("status"));
    }

    @Test public void deleteAndRecoverCaptureVersionsWithoutContaminatingLaterReads() {
        var f = new Fixture();
        f.descriptor.setAuditMaskFields(List.of("title", "version"));
        for (long version : new long[]{3, -3}) {
            var entity = new Customer();
            entity.__internalSet("id", 77L); entity.__internalSet("version", version);
            var intent = new SqlIntentRedactions();
            if (version > 0) f.repository.deleteInternal(f.context(), List.of(entity), intent);
            else f.repository.recoverInternal(f.context(), List.of(entity), intent);
            var secrets = new ArrayList<Object>();
            intent.appendTo(secrets, false);
            assertTrue("ordinary mutation target ID must redact SQL intent", secrets.contains("77"));
            assertTrue(secrets.contains(Long.toString(version)));
            assertTrue(secrets.contains(Long.toString(version > 0 ? -4 : 4)));
            var write = f.captured.get(f.captured.size() - 1);
            assertNotNull("write SQL must carry invocation-local provenance", write.bindings.intentRedactions());
            var writeSecrets = new ArrayList<Object>();
            write.bindings.intentRedactions().appendTo(writeSecrets, false);
            assertTrue(writeSecrets.contains("77"));
            var metadata = new ExecutionMetadata();
            write.bindings.applyTo(metadata);
            metadata.setBackend("sqlite");
            metadata.setParameterizedQuery(write.sql);
            metadata.setParameters(Arrays.asList(write.args));
            metadata.setAuditReason("what: mutate Customer 77");
            metadata.setAffectedRows(1L);
            for (boolean debug : new boolean[]{false, true}) {
                var projected = io.teaql.runtime.LogPrivacy.sql(metadata, debug);
                assertEquals("what: mutate Customer [REDACTED]", projected.getAuditReason());
                assertEquals("1 rows affected", projected.getResultSummary());
                assertTrue(projected.getDebugQuery().contains("77"));
                assertNull(projected.getIntentRedactions());
            }
            secrets.clear(); intent.appendTo(secrets, true);
            assertEquals(List.of("77"), secrets);
            assertThrows(TeaQLRuntimeException.class, () -> f.repository.loadPersistedById(f.context(), 77L));
            assertNull(f.captured.get(f.captured.size() - 1).bindings.intentRedactions());
        }
    }
}
