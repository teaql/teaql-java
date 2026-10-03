package io.teaql.runtime;

import io.teaql.core.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Request intent is a business gate, not an optional SQL logging annotation. */
public class RequestIntentGateTest {
    private static final class CountingProvider implements QueryExecutor, StreamingQueryExecutor, MutationExecutor {
        private final AtomicInteger calls = new AtomicInteger();
        @Override public QueryResult query(UserContext context, QueryRequest request) {
            calls.incrementAndGet(); return new DefaultQueryResult(new SmartList<>());
        }
        @Override public <T extends Entity> java.util.stream.Stream<T> queryForStream(
                UserContext context, QueryRequest request) {
            calls.incrementAndGet(); return java.util.stream.Stream.empty();
        }
        @Override public MutationResult mutate(UserContext context, PersistenceMutation request) {
            calls.incrementAndGet(); return null;
        }
        @Override public String name() { return "dummy"; }
        @Override public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
    }
    private static BaseRequest<TeaQLRuntimeTest.DummyEntity> request(String declaredComment, String declaredPurpose) {
        return new BaseRequest<>(TeaQLRuntimeTest.DummyEntity.class) {
            { internalComment(declaredComment); internalPurpose(declaredPurpose); }
            @Override public String getTypeName() { return "Dummy"; }
        };
    }

    private static void required(String code, Runnable action) {
        try {
            action.run();
            fail("Expected " + code + " before execution");
        } catch (TeaQLRuntimeException error) {
            assertTrue(error.getMessage(), error.getMessage().contains(code));
            if (code.equals("REQUEST_COMMENT_REQUIRED") || code.equals("QUERY_PURPOSE_REQUIRED")) {
                assertTrue("Intent failures must expose structured diagnostics", error instanceof RequestIntentException);
                var intentError = (RequestIntentException) error;
                assertEquals(code, intentError.getCode());
                assertEquals(code.equals("QUERY_PURPOSE_REQUIRED") ? "purpose" : "comment", intentError.getField());
            }
            assertFalse(error.getMessage().contains("SECRET-CANARY"));
        }
    }

    private static void requiredIntent(String code, String kind, Runnable action) {
        required(code, () -> {
            try {
                action.run();
            } catch (RequestIntentException error) {
                assertEquals(kind, error.getRequestKind());
                throw error;
            }
        });
    }

    @Test public void rootIntentMatrixRejectsBeforeAnyPolicyProviderOrSink() {
        for (boolean logging : new boolean[]{false, true}) {
            var provider = new CountingProvider();
            var sink = new TeaQLRuntimeTest.RecordingRuntimeLogSink();
            AtomicInteger queryPolicies = new AtomicInteger();
            AtomicInteger mutationRegistrations = new AtomicInteger();
            var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                    .dataService("dummy", provider).logSink(sink)
                    .queryExecutionLogging(logging).mutationExecutionLogging(logging)
                    .queryPolicy(new QueryPolicy() {
                        @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                            queryPolicies.incrementAndGet();
                        }
                    })
                    .mutationPolicyRegistry(plan -> {
                        mutationRegistrations.incrementAndGet();
                        return java.util.Optional.empty();
                    }).build();
            var context = new DefaultUserContext(runtime);
            // Ambient intent must never fill a missing request-owned slot.
            context.pushTrace(TraceKind.COMMENT, "Dummy", "unrelated old comment");
            context.pushTrace(TraceKind.PURPOSE, "Dummy", "unrelated old purpose");
            context.pushTrace(TraceKind.AUDIT_REASON, "Dummy", "unrelated old reason");
            for (String blank : new String[]{null, "", " \t\r\n", "\u0085", "\u00a0", "\u2003"}) {
                for (boolean missingComment : new boolean[]{true, false}) {
                    String code = missingComment ? "REQUEST_COMMENT_REQUIRED" : "QUERY_PURPOSE_REQUIRED";
                    var query = request(missingComment ? blank : "load SECRET-CANARY",
                            missingComment ? "render SECRET-CANARY" : blank);
                    requiredIntent(code, "query", () -> runtime.executeForList(context, query));
                    requiredIntent(code, "query", () -> runtime.executeForStream(context, query));
                    requiredIntent(code, "query", () -> runtime.aggregation(context, query));
                    requiredIntent(code, "query", () -> runtime.executeForPage(context, query, 0, 10));
                }
                var entity = new TeaQLRuntimeTest.DummyEntity();
                entity.setComment(blank);
                requiredIntent("REQUEST_COMMENT_REQUIRED", "mutation", () -> runtime.saveGraph(context, entity));
            }
            assertEquals(0, queryPolicies.get());
            assertEquals(0, mutationRegistrations.get());
            assertEquals(0, provider.calls.get());
            assertTrue(sink.executions.isEmpty());
            assertTrue(sink.auditEvents.isEmpty());
            assertTrue(sink.governanceEvents.isEmpty());
        }
    }

    @Test public void directQueryEnvelopeRequiresCommentEvenWithPurpose() {
        for (String comment : new String[]{null, "", " \t\r\n", "\u2003", "\u00a0"}) {
            required("REQUEST_COMMENT_REQUIRED", () ->
                    new DefaultQueryRequest(request(comment, "render orders SECRET-CANARY")));
        }
    }

    @Test public void requestOwnedTraceCannotFillMissingCommentBeforePolicyOrProvider() {
        for (boolean logging : new boolean[]{false, true}) {
            var provider = new CountingProvider();
            var sink = new TeaQLRuntimeTest.RecordingRuntimeLogSink();
            var queryPolicies = new AtomicInteger();
            var mutationRegistrations = new AtomicInteger();
            var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                    .dataService("dummy", provider).logSink(sink)
                    .queryExecutionLogging(logging).mutationExecutionLogging(logging)
                    .queryPolicy(new QueryPolicy() {
                        @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                            queryPolicies.incrementAndGet();
                        }
                    })
                    .mutationPolicyRegistry(plan -> {
                        mutationRegistrations.incrementAndGet();
                        return java.util.Optional.empty();
                    }).build();
            var context = new DefaultUserContext(runtime);
            var querySource = List.of(
                    new TraceNode(TraceKind.COMMENT, "Dummy", null, "SECRET-CANARY trace-only comment"),
                    new TraceNode(TraceKind.PURPOSE, "Dummy", null, "SECRET-CANARY trace-only purpose"));
            var query = new BaseRequest<TeaQLRuntimeTest.DummyEntity>(TeaQLRuntimeTest.DummyEntity.class) {
                { internalPurpose("declared query purpose"); }
                @Override public String getTypeName() { return "Dummy"; }
                @Override public List<TraceNode> sqlTraceSource() { return querySource; }
            };
            assertNull("the required property is actually omitted", query.comment());
            assertEquals(querySource, query.sqlTraceSource());
            requiredIntent("REQUEST_COMMENT_REQUIRED", "query", () -> new DefaultQueryRequest(query));
            requiredIntent("REQUEST_COMMENT_REQUIRED", "query", () -> runtime.executeForList(context, query));
            requiredIntent("REQUEST_COMMENT_REQUIRED", "query", () -> runtime.executeForStream(context, query));
            requiredIntent("REQUEST_COMMENT_REQUIRED", "query", () -> runtime.aggregation(context, query));
            requiredIntent("REQUEST_COMMENT_REQUIRED", "query", () -> runtime.executeForPage(context, query, 0, 10));

            var entity = new TeaQLRuntimeTest.DummyEntity();
            entity.__internalSet("id", 801L); entity.__internalSet("version", 1L);
            entity.set$status(EntityStatus.PERSISTED);
            entity.updateProperty("name", "pending mutation payload");
            var mutationSource = List.of(new TraceNode(
                    TraceKind.AUDIT_REASON, "Dummy", 801L, "SECRET-CANARY trace-only audit reason"));
            entity.setTraceChain(mutationSource);
            assertNull(entity.getComment());
            assertEquals(mutationSource, entity.getTraceChain());
            for (var action : EntityPersistenceMutation.Action.values())
                requiredIntent("REQUEST_COMMENT_REQUIRED", "mutation", () -> new EntityPersistenceMutation(entity, action));
            requiredIntent("REQUEST_COMMENT_REQUIRED", "mutation", () -> runtime.saveGraph(context, entity));

            assertEquals(0, queryPolicies.get());
            assertEquals(0, mutationRegistrations.get());
            assertEquals(0, provider.calls.get());
            assertTrue(sink.executions.isEmpty());
            assertTrue(sink.auditEvents.isEmpty());
            assertTrue(sink.governanceEvents.isEmpty());
            assertTrue("no ambient trace was supplied or created", context.getTraceChain().isEmpty());
        }
    }

    @Test public void explicitMutationCommentSurvivesEachBlankTypedRouteTail() {
        for (boolean logging : new boolean[]{false, true}) {
            for (var kind : List.of(TraceKind.ENTITY, TraceKind.PROVIDER, TraceKind.SQL)) {
                var provider = new TeaQLRuntimeTest.RecordingMutationExecutor();
                var sink = new TeaQLRuntimeTest.RecordingRuntimeLogSink();
                var policies = new AtomicInteger();
                var entity = new TeaQLRuntimeTest.DummyEntity();
                entity.__internalSet("id", 802L); entity.__internalSet("version", 1L);
                entity.set$status(EntityStatus.PERSISTED);
                entity.updateProperty("name", "changed field");
                String comment = "  explicit mutation request reason  ";
                entity.setComment(comment);
                var tail = new TraceNode(kind, kind == TraceKind.PROVIDER ? "dummy"
                        : kind == TraceKind.SQL ? "update" : "Dummy", null, "");
                // Deliberately supplied diagnostic input for TC-REQ-13, not
                // evidence that a generated graph constructs these route nodes.
                var source = List.of(new TraceNode(TraceKind.AUDIT_REASON, "Dummy", 802L, comment), tail);
                entity.setTraceChain(source);
                var request = new EntityPersistenceMutation(entity, EntityPersistenceMutation.Action.SAVE,
                        MutationIntent.of(comment), source);
                assertEquals(tail, request.getTraceChain().get(request.getTraceChain().size() - 1));
                assertEquals("", tail.getComment());
                assertEquals(comment, request.comment());
                assertEquals(comment, request.intent().readbackIntent().comment());

                var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                        .dataService("dummy", provider).logSink(sink)
                        .queryExecutionLogging(logging).mutationExecutionLogging(logging)
                        .mutationPolicyRegistry(plan -> java.util.Optional.of(new MutationPolicy() {
                            @Override public MutationPolicyIdentity identity() {
                                return new MutationPolicyIdentity("route-tail", "1", "test");
                            }
                            @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                                policies.incrementAndGet();
                                assertEquals(comment, plan.auditReason());
                                return MutationDecision.allow();
                            }
                        })).build();
                var context = new DefaultUserContext(runtime);
                runtime.saveGraph(context, entity);
                assertEquals(1, policies.get());
                assertEquals(1, provider.requests.size());
                var emitted = provider.requests.get(0);
                assertEquals(source, emitted.getTraceChain());
                assertEquals(comment, emitted.comment());
                assertEquals(comment, emitted.intent().readbackIntent().comment());
                assertEquals(1, sink.auditEvents.size());
                assertEquals(comment, sink.auditEvents.get(0).reason());
                assertTrue(context.getTraceChain().isEmpty());
            }
        }
    }

    @Test public void directQueryEnvelopeRequiresPurpose() {
        for (String purpose : new String[]{null, "", "\u2003"}) {
            required("QUERY_PURPOSE_REQUIRED", () ->
                    new DefaultQueryRequest(request("load orders SECRET-CANARY", purpose)));
        }
    }

    @Test public void everyRootQueryGateRunsBeforePolicyAndWithLoggingDisabled() {
        AtomicInteger policyCalls = new AtomicInteger();
        var provider = new CountingProvider();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider)
                .queryExecutionLogging(false).mutationExecutionLogging(false)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                        policyCalls.incrementAndGet();
                    }
                }).build();
        var context = new DefaultUserContext(runtime);
        context.pushTrace(TraceKind.COMMENT, "Dummy", "unrelated old comment");
        context.pushTrace(TraceKind.PURPOSE, "Dummy", "unrelated old purpose");
        required("REQUEST_COMMENT_REQUIRED", () -> runtime.executeForList(context, request(null, "render orders")));
        required("REQUEST_COMMENT_REQUIRED", () -> runtime.executeForStream(context, request(null, "render orders")));
        required("REQUEST_COMMENT_REQUIRED", () -> runtime.aggregation(context, request(null, "render orders")));
        required("REQUEST_COMMENT_REQUIRED", () -> runtime.executeForPage(context, request(null, "render orders"), 0, 10));
        assertEquals(0, policyCalls.get());
        assertEquals(0, provider.calls.get());
    }

    @Test public void mutationEnvelopeRequiresExplicitAuditComment() {
        var entity = new TeaQLRuntimeTest.DummyEntity();
        required("REQUEST_COMMENT_REQUIRED", () ->
                new EntityPersistenceMutation(entity, EntityPersistenceMutation.Action.SAVE));
        entity.setComment("\u2003");
        required("REQUEST_COMMENT_REQUIRED", () ->
                new EntityPersistenceMutation(entity, EntityPersistenceMutation.Action.DELETE));
    }

    @Test public void graphMutationGateRunsBeforePolicyAndProvider() {
        AtomicInteger policyCalls = new AtomicInteger();
        var provider = new CountingProvider();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider)
                .mutationPolicyRegistry(plan -> java.util.Optional.of(new MutationPolicy() {
                    @Override public MutationPolicyIdentity identity() {
                        return new MutationPolicyIdentity("test", "1", "test");
                    }
                    @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                        policyCalls.incrementAndGet(); return MutationDecision.allow();
                    }
                })).queryExecutionLogging(false).mutationExecutionLogging(false).build();
        var entity = new TeaQLRuntimeTest.DummyEntity();
        entity.setComment("\u2003");
        var context = new DefaultUserContext(runtime);
        context.pushTrace(TraceKind.AUDIT_REASON, "Dummy", "old unrelated reason");
        required("REQUEST_COMMENT_REQUIRED", () -> runtime.saveGraph(context, entity));
        assertEquals(0, policyCalls.get());
        assertEquals(0, provider.calls.get());
    }

    @Test public void ambientTraceCannotAuthorizeAnUnscopedNestedQuery() {
        AtomicInteger policyCalls = new AtomicInteger();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                        policyCalls.incrementAndGet();
                    }
                }).build();
        var context = new DefaultUserContext(runtime);
        context.pushTrace("forged root trace");
        required("INTERNAL QUERY CONTEXT REQUIRED", () -> context.internalExecuteForList(request(null, null)));
        required("INTERNAL QUERY CONTEXT REQUIRED", () -> context.internalExecuteForStream(request(null, null)));
        assertEquals(0, policyCalls.get());
    }

    @Test public void mutationPlanCannotExposeAnUnvalidatedReasonToPolicy() {
        required("REQUEST_COMMENT_REQUIRED", () ->
                new MutationPlan("exec", "request", "Dummy", null, List.of()));
    }

    @Test public void queryAndMutationEnvelopesKeepTheirValidatedSnapshot() {
        class MutableRequest extends BaseRequest<TeaQLRuntimeTest.DummyEntity> {
            MutableRequest() { super(TeaQLRuntimeTest.DummyEntity.class); }
            void changeIntent(String comment, String purpose) { internalComment(comment); internalPurpose(purpose); }
        }
        var builder = new MutableRequest();
        builder.changeIntent(" load original ", "render original");
        var query = new DefaultQueryRequest(builder);
        builder.changeIntent("another operation", "another purpose");
        assertEquals(" load original ", query.comment());
        assertEquals("render original", query.purpose());
        var entity = new TeaQLRuntimeTest.DummyEntity(); entity.setComment("submit original");
        var mutation = new EntityPersistenceMutation(entity, EntityPersistenceMutation.Action.SAVE);
        entity.setComment("another operation");
        assertEquals("submit original", mutation.comment());
        assertEquals("submit original", mutation.intent().readbackIntent().comment());
        assertFalse(query.intent().toString().contains("original"));
        assertFalse(mutation.intent().toString().contains("original"));
    }

    @Test public void nestedQueryCarriesExplicitIntentWithoutAmbientContextOrLogging() {
        var captured = new java.util.ArrayList<QueryRequest>();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .queryExecutionLogging(false).mutationExecutionLogging(false)
                .dataService("dummy", new TeaQLRuntimeTest.DummyQueryExecutor() {
                    @Override public QueryResult query(UserContext context, QueryRequest request) {
                        captured.add(request); return super.query(context, request);
                    }
                }).build();
        var root = QueryIntent.of("load order graph", "render order details");
        var child = new BaseRequest<TeaQLRuntimeTest.DummyEntity>(TeaQLRuntimeTest.DummyEntity.class) {
            @Override public String getTypeName() { return "Dummy"; }
            @Override public QueryIntent inheritedQueryIntent() { return root; }
        };
        var context = new DefaultUserContext(runtime);
        assertTrue(context.getTraceChain().isEmpty());
        assertNull(child.comment()); assertNull(child.purpose());
        assertEquals(1, context.internalExecuteForList(child).size());
        assertSame(root, captured.get(0).intent());
        assertEquals(root.comment(), captured.get(0).comment());
        assertEquals(root.purpose(), captured.get(0).purpose());
        assertTrue(context.getTraceChain().isEmpty());
    }

    @Test public void policyAndCommittedAuditUseTheCapturedMutationRequestReason() {
        var provider = new TeaQLRuntimeTest.RecordingMutationExecutor();
        var sink = new TeaQLRuntimeTest.RecordingRuntimeLogSink();
        var entity = new TeaQLRuntimeTest.DummyEntity();
        entity.__internalSet("id", 701L); entity.__internalSet("version", 1L);
        entity.set$status(EntityStatus.PERSISTED);
        entity.updateProperty("name", "changed field"); entity.setComment("root operation reason");
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider).logSink(sink)
                .mutationPolicyRegistry(key -> java.util.Optional.of(new MutationPolicy() {
                    @Override public MutationPolicyIdentity identity() {
                        return new MutationPolicyIdentity("test", "1", "test");
                    }
                    @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                        assertEquals("root operation reason", plan.auditReason());
                        entity.setComment("a different later comment");
                        return MutationDecision.allow();
                    }
                })).build();
        runtime.saveGraph(new DefaultUserContext(runtime), entity);
        assertEquals(1, provider.requests.size());
        assertEquals("root operation reason", provider.requests.get(0).comment());
        assertEquals(1, sink.auditEvents.size());
        assertEquals("root operation reason", sink.auditEvents.get(0).reason());
    }
}
