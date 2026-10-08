package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.criteria.Operator;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.stream.Stream;

public class TeaQLRuntimeTest {

    @Test
    public void brokenSqlDiagnosticSinkDoesNotFailTheBusinessOperation() {
        AtomicInteger attempts = new AtomicInteger();
        RuntimeLogSink broken = (context, projected) -> {
            attempts.incrementAndGet();
            Assert.assertFalse(projected.getDebugQuery().contains("PASSWORD-CANARY"));
            throw new IllegalStateException("LOG-SINK-FAILURE");
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory()).logSink(broken).build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        ExecutionMetadata metadata = new ExecutionMetadata();
        metadata.setOperation(DataServiceOperation.QUERY);
        metadata.setParameterizedQuery("SELECT id FROM customer WHERE password = ?");
        metadata.setParameters(List.of("PASSWORD-CANARY"));

        context.recordExecutionMetadata(metadata);

        Assert.assertEquals(1, attempts.get());
    }

    @Test
    public void executionLoggingDefaultsOnAndQueryMutationCanBeDisabledIndependently() {
        TeaQLRuntime defaultRuntime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .build();
        Assert.assertTrue(defaultRuntime.isQueryExecutionLoggingEnabled());
        Assert.assertTrue(defaultRuntime.isMutationExecutionLoggingEnabled());

        TeaQLRuntime queryDisabled = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .queryExecutionLogging(false)
                .build();
        DefaultUserContext queryDisabledContext = new DefaultUserContext(queryDisabled);
        Assert.assertFalse(queryDisabledContext.isQueryExecutionLoggingEnabled());
        Assert.assertTrue(queryDisabledContext.isMutationExecutionLoggingEnabled());

        TeaQLRuntime mutationDisabled = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .mutationExecutionLogging(false)
                .build();
        DefaultUserContext mutationDisabledContext = new DefaultUserContext(mutationDisabled);
        Assert.assertTrue(mutationDisabledContext.isQueryExecutionLoggingEnabled());
        Assert.assertFalse(mutationDisabledContext.isMutationExecutionLoggingEnabled());
    }

    public static class DummyChecker implements Checker<DummyEntity> {
        int calls;

        @Override public String type() { return "Dummy"; }

        @Override
        public void checkAndFix(UserContext context, DummyEntity entity, ObjectLocation location) {
            calls++;
            if (!needCheck(context, entity)) return;
            markAsChecked(context, entity);
            requiredCheck(context, newLocation(location, "name"), entity.name);
        }
    }

    public static class DummyEntity extends BaseEntity {
        private String name;
        private DummyEntity child;

        @Override
        public String typeName() {
            return "Dummy";
        }

        @Override
        public void __internalSet(String property, Object value) {
            if ("name".equals(property)) {
                this.name = (String) value;
            } else {
                super.__internalSet(property, value);
            }
        }
        
        @Override
        public Object __internalGet(String property) {
            if ("child".equals(property)) {
                return child;
            } else if ("name".equals(property)) {
                return this.name;
            } else {
                return super.__internalGet(property);
            }
        }
    }

    public static class DummyQueryExecutor implements QueryExecutor {
        @Override
        public QueryResult query(UserContext context, QueryRequest request) {
            SmartList<DummyEntity> list = new SmartList<>();
            list.add(new DummyEntity());
            return new DefaultQueryResult(list);
        }

        @Override
        public String name() {
            return "dummy";
        }

        @Override
        public DataServiceCapabilities capabilities() {
            return null;
        }
    }

    public static class RecordingStreamingQueryExecutor implements StreamingQueryExecutor {
        private SearchRequest<?> request;

        @Override
        public <T extends Entity> Stream<T> queryForStream(
                UserContext context, QueryRequest request) {
            this.request = ((DefaultQueryRequest) request).getSearchRequest();
            return Stream.empty();
        }

        @Override public String name() { return "dummy"; }

        @Override public DataServiceCapabilities capabilities() { return null; }
    }

    public static class RecordingMutationExecutor implements MutationExecutor {
        public final List<EntityPersistenceMutation> requests = new ArrayList<>();

        @Override
        public MutationResult mutate(UserContext context, PersistenceMutation request) {
            if (request instanceof EntityPersistenceMutation) {
                EntityPersistenceMutation mutationRequest = (EntityPersistenceMutation) request;
                requests.add(mutationRequest);
                return new DefaultMutationResult(mutationRequest.getEntity());
            }
            return null;
        }

        @Override
        public String name() {
            return "dummy";
        }

        @Override
        public DataServiceCapabilities capabilities() {
            return null;
        }
    }

    public static class PageQueryExecutor implements QueryExecutor {
        public final List<SearchRequest<?>> requests = new ArrayList<>();
        @Override public QueryResult query(UserContext context, QueryRequest query) {
            SearchRequest<?> request = ((DefaultQueryRequest) query).getSearchRequest();
            requests.add(request);
            if (request.hasSimpleAgg()) {
                AggregationItem item = new AggregationItem();
                item.setValues(Map.of(
                        new SimpleNamedExpression(TeaQLConstants.ROOT_LIST_PARAMETER_NAME), 5));
                AggregationResult total = new AggregationResult();
                total.setData(List.of(item));
                return new DefaultQueryResult(new SmartList<>(), total);
            }
            SmartList<DummyEntity> rows = new SmartList<>();
            for (int id = request.getSlice().getOffset() + 1;
                    id <= request.getSlice().getOffset() + request.getSlice().getSize(); id++) {
                DummyEntity entity = new DummyEntity(); entity.updateId((long) id); rows.add(entity);
            }
            return new DefaultQueryResult(rows);
        }
        @Override public String name() { return "page"; }
        @Override public DataServiceCapabilities capabilities() { return null; }
    }

    public static class RecordingRuntimeLogSink implements RuntimeLogSink {
        public final List<RawAuditEvent> auditEvents = new ArrayList<>();
        public final List<ExecutionMetadata> executions = new ArrayList<>();
        public final List<MutationGovernanceEvent> governanceEvents = new ArrayList<>();

        @Override
        public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) { executions.add(metadata); }

        @Override
        public void writeAuditEvent(UserContext context, RawAuditEvent event) {
            auditEvents.add(event);
        }

        @Override
        public void writeMutationGovernanceEvent(
                UserContext context, MutationGovernanceEvent event) {
            governanceEvents.add(event);
        }
    }

    private static MutationPolicy allowingPolicy(
            MutationPolicyIdentity identity, java.util.concurrent.atomic.AtomicInteger calls) {
        return new MutationPolicy() {
            @Override public MutationPolicyIdentity identity() { return identity; }
            @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                calls.incrementAndGet();
                return MutationDecision.allow();
            }
        };
    }

    private static DummyEntity changedEntity(long id, String comment) {
        DummyEntity entity = new DummyEntity();
        entity.updateId(id);
        entity.__internalSet("version", 1L);
        entity.set$status(EntityStatus.PERSISTED);
        entity.updateProperty("name", "private-value");
        entity.setComment(comment);
        return entity;
    }

    @Test
    public void missingMutationPolicyWarnsButDoesNotBlockSave() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        RecordingRuntimeLogSink sink = new RecordingRuntimeLogSink();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .logSink(sink)
                .build();

        runtime.saveGraph(new DefaultUserContext(runtime), changedEntity(901L, "missing policy"));

        Assert.assertEquals(1, executor.requests.size());
        Assert.assertEquals(1, sink.governanceEvents.size());
        Assert.assertEquals("MUTATION-POLICY-001", sink.governanceEvents.get(0).warningCode());
        Assert.assertEquals(List.of("MUTATION-POLICY-001"),
                sink.auditEvents.get(0).governance().warningCodes());
    }

    @Test
    public void unapprovedPolicyRunsAndWarnsWhileMatchingApprovalIsQuiet() {
        MutationPolicyIdentity identity =
                new MutationPolicyIdentity("customer.dummy", "1", "sha256:abc");
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        MutationPolicy policy = allowingPolicy(identity, calls);
        RecordingRuntimeLogSink unapprovedSink = new RecordingRuntimeLogSink();
        TeaQLRuntime unapproved = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new RecordingMutationExecutor())
                .mutationPolicyRegistry(key -> java.util.Optional.of(policy))
                .logSink(unapprovedSink)
                .build();
        unapproved.saveGraph(
                new DefaultUserContext(unapproved), changedEntity(902L, "unapproved policy"));
        Assert.assertEquals(1, calls.get());
        Assert.assertEquals("MUTATION-POLICY-002",
                unapprovedSink.governanceEvents.get(0).warningCode());

        RecordingRuntimeLogSink approvedSink = new RecordingRuntimeLogSink();
        TeaQLRuntime approved = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new RecordingMutationExecutor())
                .mutationPolicyRegistry(key -> java.util.Optional.of(policy))
                .mutationPolicyApprovalProvider(key -> java.util.Optional.of(
                        new MutationPolicyApproval(identity, "security", java.time.Instant.EPOCH)))
                .logSink(approvedSink)
                .build();
        approved.saveGraph(new DefaultUserContext(approved), changedEntity(903L, "approved policy"));
        Assert.assertEquals(2, calls.get());
        Assert.assertTrue(approvedSink.governanceEvents.isEmpty());
        Assert.assertEquals(MutationPolicyApprovalStatus.APPROVED,
                approvedSink.auditEvents.get(0).governance().approvalStatus());
    }

    @Test
    public void deniedMutationWritesNothingAndRetainsLedger() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        MutationPolicy denying = new MutationPolicy() {
            @Override public MutationPolicyIdentity identity() {
                return new MutationPolicyIdentity("customer.dummy", "1", "sha256:deny");
            }
            @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                return MutationDecision.deny(
                        "DUMMY-NAME-DENIED", "name cannot be changed", List.of("name"));
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .mutationPolicyRegistry(key -> java.util.Optional.of(denying))
                .build();
        DummyEntity entity = changedEntity(904L, "denied policy");

        try {
            runtime.saveGraph(new DefaultUserContext(runtime), entity);
            Assert.fail("denied mutation was persisted");
        } catch (TeaQLRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("DUMMY-NAME-DENIED"));
        }

        Assert.assertTrue(executor.requests.isEmpty());
        Assert.assertTrue(entity.getEntityMutationLedger()
                .changedFieldNames(new EntityKey("Dummy", 904L)).contains("name"));
    }

    @Test
    public void governanceAuditContainsFieldNamesButNotRawValues() {
        RecordingRuntimeLogSink sink = new RecordingRuntimeLogSink();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new RecordingMutationExecutor())
                .logSink(sink)
                .build();
        runtime.saveGraph(new DefaultUserContext(runtime), changedEntity(905L, "safe snapshot"));

        MutationGovernanceSnapshot governance = sink.auditEvents.get(0).governance();
        Assert.assertEquals(List.of("name"), governance.operations().get(0).changedFields());
        Assert.assertFalse(governance.toString().contains("private-value"));
    }

    @Test
    public void graphPolicyReviewsAllOperationsExactlyOnce() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        java.util.concurrent.atomic.AtomicInteger reviews = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<MutationPlan> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        MutationPolicy policy = new MutationPolicy() {
            @Override public MutationPolicyIdentity identity() {
                return new MutationPolicyIdentity("customer.graph", "1", "sha256:graph");
            }
            @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                reviews.incrementAndGet();
                captured.set(plan);
                return MutationDecision.allow();
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .mutationPolicyRegistry(key -> java.util.Optional.of(policy))
                .build();
        ContainerEntity root = new ContainerEntity();
        root.updateId(910L);
        root.set$status(EntityStatus.PERSISTED);
        DummyEntity child = changedEntity(911L, "child change");
        root.updateProperty("rel1", child);
        root.setComment("review complete graph");

        runtime.saveGraph(new DefaultUserContext(runtime), root);

        Assert.assertEquals(1, reviews.get());
        Assert.assertNotNull(captured.get());
        Assert.assertTrue(captured.get().operations().stream()
                .anyMatch(operation -> operation.entity().equals(new EntityKey("Dummy", 911L))));
        Assert.assertTrue(captured.get().operations().size() >= 2);
    }

    @Test
    public void governanceWarningSinkFailureDoesNotBlockPersistence() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        RuntimeLogSink throwingSink = new RuntimeLogSink() {
            @Override public void writeExecutionLog(
                    UserContext context, ExecutionMetadata metadata) {}
            @Override public void writeMutationGovernanceEvent(
                    UserContext context, MutationGovernanceEvent event) {
                throw new IllegalStateException("sink unavailable");
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .logSink(throwingSink)
                .build();

        runtime.saveGraph(
                new DefaultUserContext(runtime), changedEntity(912L, "warning sink failure"));

        Assert.assertEquals(1, executor.requests.size());
    }

    @Test
    public void executionMetadataRetainsStructuredIntentAndThreeLevelTrace() {
        RecordingRuntimeLogSink sink = new RecordingRuntimeLogSink();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory()).logSink(sink).build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        context.pushTrace(TraceKind.OPERATION, "School", "query");
        context.pushTrace(TraceKind.REQUEST, "School", "School");
        context.pushTrace(TraceKind.COMMENT, "School", "what: load school graph");
        context.pushTrace(TraceKind.PURPOSE, "School", "why: render details");
        context.pushTrace(TraceKind.RELATION, "School.platform", "platform");
        context.pushTrace(TraceKind.RELATION, "Platform.organization", "organization");
        context.pushTrace(TraceKind.RELATION, "Organization.region", "region");
        ExecutionMetadata metadata = new ExecutionMetadata();
        metadata.setOperation(DataServiceOperation.QUERY);
        metadata.setParameterizedQuery("SELECT name FROM school_data WHERE id = ?");
        metadata.setParameters(List.of(7L));
        metadata.setDebugQuery("SELECT name FROM school_data WHERE id = 7");
        metadata.setResultCount(1);
        context.recordExecutionMetadata(metadata);

        Assert.assertEquals(1, sink.executions.size());
        ExecutionMetadata recorded = sink.executions.get(0);
        Assert.assertEquals("what: load school graph", recorded.getComment());
        Assert.assertEquals("why: render details", recorded.getPurpose());
        Assert.assertEquals(7, recorded.getTraceChain().size());
        Assert.assertEquals(TraceKind.OPERATION, recorded.getTraceChain().get(0).getKind());
        Assert.assertEquals(TraceKind.REQUEST, recorded.getTraceChain().get(1).getKind());
        Assert.assertEquals(TraceKind.RELATION, recorded.getTraceChain().get(4).getKind());
        Assert.assertEquals(TraceKind.PROVIDER, recorded.getTraceChain().get(5).getKind());
        Assert.assertEquals(TraceKind.SQL, recorded.getTraceChain().get(6).getKind());
        Assert.assertEquals("SELECT name FROM school_data WHERE id = ?", recorded.getParameterizedQuery());
        Assert.assertTrue(recorded.getDebugQuery().contains("WHERE id = '[REDACTED]' /* masked */"));
        Assert.assertFalse(recorded.getDebugQuery().contains("WHERE id = 7"));
    }

    public static class DummyMetaFactory implements EntityMetaFactory {
        @Override
        public EntityDescriptor resolveEntityDescriptor(String type) {
            EntityDescriptor desc = new EntityDescriptor();
            desc.setType(type);
            desc.setDataService("dummy");
            return desc;
        }

        @Override
        public void register(EntityDescriptor type) {}

        @Override
        public List<EntityDescriptor> allEntityDescriptors() { return null; }
    }

    @Test
    public void testBuilder() {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .build();
        Assert.assertNotNull(runtime);
    }

    @Test
    public void modifiedDescendantIsCheckedThroughUntouchedPartialParent() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        DummyChecker checker = new DummyChecker() {
            @Override public void checkAndFix(UserContext context, DummyEntity entity, ObjectLocation location) {
                if (!needCheck(context, entity)) return;
                markAsChecked(context, entity);
                requiredCheck(context, newLocation(location, "name"), entity.name);
                checkAndFix(context, entity.child, newLocation(location, "child"));
            }
        };
        DummyMetaFactory metadata = new DummyMetaFactory() {
            @Override public EntityDescriptor resolveEntityDescriptor(String type) {
                var descriptor = super.resolveEntityDescriptor(type);
                var relation = new io.teaql.core.meta.Relation();
                relation.setName("child");
                descriptor.getProperties().add(relation);
                return descriptor;
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(metadata)
                .dataService("dummy", executor).build()
                .install(RuntimeModule.of().withCheckers(checker));
        var context = new DefaultUserContext(runtime);
        var root = new DummyEntity(); root.name = "complete parent";
        root.set$status(EntityStatus.UPDATED); root.setComment("check modified descendants");
        root.child = new DummyEntity(); root.child.set$status(EntityStatus.PERSISTED);
        root.child.child = new DummyEntity(); root.child.child.set$status(EntityStatus.UPDATED);
        // Cycle exercises the independent persistence walk's identity boundary.
        root.child.child.child = root;
        var error = Assert.assertThrows(CheckException.class, () -> runtime.saveGraph(context, root));
        Assert.assertEquals(1, error.getViolates().size());
        Assert.assertEquals("child.child.name", error.getViolates().get(0).getLocation().modelPath());
        Assert.assertTrue(executor.requests.isEmpty());
        Assert.assertEquals(EntityStatus.PERSISTED, root.child.get$status());
    }

    @Test
    public void checkerFailurePreventsMutationAndTraversalStateIsSaveScoped() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        DummyChecker checker = new DummyChecker();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .idGenerationService((context, entity) -> 42L)
                .build()
                .install(RuntimeModule.of().withCheckers(checker));
        DefaultUserContext context = new DefaultUserContext(runtime);
        DummyEntity entity = new DummyEntity();
        entity.setComment("create invalid dummy");

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                runtime.saveGraph(context, entity);
                Assert.fail("invalid entity must fail before mutation");
            } catch (CheckException expected) {
                Assert.assertEquals(1, expected.getViolates().size());
                Assert.assertEquals("name", expected.getViolates().get(0).getLocation().toString());
            }
        }
        Assert.assertEquals(2, checker.calls);
        Assert.assertTrue(executor.requests.isEmpty());
        Assert.assertNull(entity.getId());
        Assert.assertNull(context.getAttribute(Checker.TEAQL_DATA_CHECK_RESULT));
        Assert.assertNull(context.getAttribute(Checker.TEAQL_DATA_CHECKED_ITEMS));
    }

    @Test
    public void defaultContextObservesActualLocalCacheOperations() {
        List<String> events = new ArrayList<>();
        List<RuntimeTelemetry.Operation> operations = new ArrayList<>();
        RuntimeTelemetry telemetry = operation -> {
            operations.add(operation);
            events.add("start:" + operation.name());
            return new RuntimeTelemetry.Scope() {
                @Override public void success(Map<String, Object> attributes) {
                    events.add("success:" + attributes.get("teaql.cache.result"));
                }
                @Override public void failure(Throwable error) {
                    events.add("failure:" + error.getClass().getSimpleName());
                }
            };
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory()).telemetry(telemetry).build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        String key = "runtime-cache-" + System.nanoTime();

        context.putToLocalCache(key, "value");
        Assert.assertEquals("value", context.getFromLocalCache(key, String.class));
        context.removeFromLocalCache(key);
        Assert.assertNull(context.getFromLocalCache(key, String.class));

        Assert.assertEquals(List.of(
                "start:local.put", "success:stored",
                "start:local.get", "success:hit",
                "start:local.remove", "success:removed",
                "start:local.get", "success:miss"), events);
        Assert.assertTrue(operations.stream().allMatch(op -> "cache".equals(op.family())));
        Assert.assertTrue(operations.stream().allMatch(op ->
                !op.attributes().toString().contains(key)));
    }

    @Test
    public void cacheTelemetryFailureDoesNotChangeCacheResult() {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .telemetry(operation -> { throw new IllegalStateException("adapter failed"); })
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        String key = "runtime-cache-fail-open-" + System.nanoTime();

        context.putToLocalCache(key, "value");
        Assert.assertEquals("value", context.getFromLocalCache(key, String.class));
        context.removeFromLocalCache(key);
    }

    @Test
    public void testExecuteForList() {
        List<String> telemetryEvents = new ArrayList<>();
        RuntimeTelemetry telemetry = operation -> {
            telemetryEvents.add("start:" + operation.family());
            return new RuntimeTelemetry.Scope() {
                @Override public void success(Map<String, Object> attributes) {
                    telemetryEvents.add("success:" + operation.family());
                }
                @Override public void failure(Throwable error) {
                    telemetryEvents.add("failure:" + operation.family());
                }
            };
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new DummyQueryExecutor())
                .telemetry(telemetry)
                .build();

        SearchRequest<DummyEntity> request = new BaseRequest<DummyEntity>(DummyEntity.class) {
            {
                internalComment("test");
                internalPurpose("test request");
            }
            @Override
            public String getTypeName() {
                return "Dummy";
            }
        };

        SmartList<DummyEntity> result = runtime.executeForList(new DefaultUserContext(runtime), request);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(List.of(
                "start:query", "start:provider", "success:provider", "success:query"),
                telemetryEvents);
    }

    @Test
    public void pagedExecutionReturnsRowsAndExactPolicyFilteredTotal() {
        PageQueryExecutor executor = new PageQueryExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory()).dataService("dummy", executor)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> query) {
                        BaseRequest<?> request = (BaseRequest<?>) query;
                        request.appendSearchCriteria(request.createBasicSearchCriteria(
                                "status", Operator.EQUAL, "ACTIVE"));
                    }
                })
                .build();
        BaseRequest<DummyEntity> request = new BaseRequest<>(DummyEntity.class) {
            { internalComment("list active"); internalPurpose("test exact page total"); }
            @Override public String getTypeName() { return "Dummy"; }
        };
        SmartList<DummyEntity> page = runtime.executeForPage(
                new DefaultUserContext(runtime), request, 2, 2);
        Assert.assertEquals(List.of(3L, 4L), page.toList(Entity::getId));
        Assert.assertEquals(5, page.getTotalCount());
        Assert.assertEquals(2, executor.requests.size());
        Assert.assertFalse(executor.requests.get(0).hasSimpleAgg());
        Assert.assertTrue(executor.requests.get(1).hasSimpleAgg());
        Assert.assertSame(executor.requests.get(0).getSearchCriteria(),
                executor.requests.get(1).getSearchCriteria());
    }

    @Test
    public void streamingExecutionAppliesRequestPolicyBeforeProvider() {
        RecordingStreamingQueryExecutor executor = new RecordingStreamingQueryExecutor();
        AtomicInteger policyCalls = new AtomicInteger();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(
                            UserContext context, SearchRequest<?> query) {
                        policyCalls.incrementAndGet();
                        BaseRequest<?> request = (BaseRequest<?>) query;
                        request.appendSearchCriteria(request.createBasicSearchCriteria(
                                "status", Operator.EQUAL, "ACTIVE"));
                    }
                })
                .build();
        BaseRequest<DummyEntity> request = new BaseRequest<>(DummyEntity.class) {
            { internalComment("stream active entities"); }
            @Override public String getTypeName() { return "Dummy"; }
        };

        try (Stream<DummyEntity> ignored = request
                .purpose("prove policy cannot be bypassed by streaming")
                .executeForStream(new DefaultUserContext(runtime))) {
            Assert.assertEquals(0, ignored.count());
        }

        Assert.assertEquals(1, policyCalls.get());
        Assert.assertSame(request, executor.request);
        Assert.assertNotNull(executor.request.getSearchCriteria());
    }

    @Test
    public void internalStreamingRequiresAuthorizedRootAndAppliesRequestPolicy() {
        RecordingStreamingQueryExecutor executor = new RecordingStreamingQueryExecutor();
        AtomicInteger policyCalls = new AtomicInteger();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(
                            UserContext context, SearchRequest<?> query) {
                        policyCalls.incrementAndGet();
                    }
                })
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        SearchRequest<DummyEntity> request = bareDummyRequest();

        try {
            context.internalExecuteForStream(request);
            Assert.fail("internal stream without a trusted root trace must be rejected");
        } catch (TeaQLRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("INTERNAL QUERY CONTEXT REQUIRED"));
        }
        Assert.assertEquals(0, policyCalls.get());

        context.pushTrace("authorized root query");
        SearchRequest<DummyEntity> scoped = inheritedDummyRequest();
        try (Stream<DummyEntity> ignored = context.internalExecuteForStream(scoped)) {
            Assert.assertEquals(0, ignored.count());
        } finally {
            context.popTrace();
        }
        Assert.assertEquals(1, policyCalls.get());
        Assert.assertSame(scoped, executor.request);
    }

    @Test
    public void materializedListHardLimitRejectsClientOverride() {
        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(new DummyMetaFactory())
                .dataService("dummy", new DummyQueryExecutor()).build();
        BaseRequest<DummyEntity> request = new BaseRequest<DummyEntity>(DummyEntity.class) {
            { internalComment("test"); internalPurpose("test hard limit"); }
            @Override public String getTypeName() { return "Dummy"; }
        };
        request.top(10_001);
        try {
            runtime.executeForList(new DefaultUserContext(runtime), request);
            Assert.fail("limit above hard limit must fail");
        } catch (TeaQLRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("QUERY HARD LIMIT"));
        }
        try {
            request.top(20_000);
            runtime.executeForList(new DefaultUserContext(runtime), request);
            Assert.fail("client-controlled hard-limit override must not be available");
        } catch (TeaQLRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("exceeds hard limit 10000"));
        }
    }

    @Test
    public void testNestedQueryInheritsAuthorizedRootTrace() {
        List<String> families = new ArrayList<>();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new DummyQueryExecutor())
                .telemetry(operation -> {
                    families.add(operation.family());
                    return RuntimeTelemetry.NoopScope.INSTANCE;
                })
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        SearchRequest<DummyEntity> nested = inheritedDummyRequest();

        context.pushTrace("authorized root query");
        try {
            Assert.assertEquals(1, context.internalExecuteForList(nested).size());
            Assert.assertEquals(List.of("relation_load", "provider"), families);
        } finally {
            context.popTrace();
        }
    }

    @Test
    public void TOPN_010_nestedRelationTelemetryCarriesPlanDimensions() {
        List<RuntimeTelemetry.Operation> operations = new ArrayList<>();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new DummyQueryExecutor())
                .telemetry(operation -> {
                    operations.add(operation);
                    return RuntimeTelemetry.NoopScope.INSTANCE;
                })
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        BaseRequest<DummyEntity> nested = (BaseRequest<DummyEntity>) inheritedDummyRequest();
        nested.putExtension("teaql.internal.top_n.parent_count", 3);
        nested.putExtension("teaql.internal.top_n.per_parent_limit", 2);
        nested.putExtension("teaql.internal.top_n.probe_threshold", 3);
        nested.putExtension("teaql.internal.top_n.selected_plan", "probe");
        nested.putExtension("teaql.internal.top_n.probe_count", 3);

        context.pushTrace("authorized root query");
        try {
            context.internalExecuteForList(nested);
        } finally {
            context.popTrace();
        }

        Map<String, Object> attributes = operations.get(0).attributes();
        Assert.assertEquals(3, attributes.get("teaql.relation.parent_count"));
        Assert.assertEquals(2, attributes.get("teaql.relation.per_parent_limit"));
        Assert.assertEquals(3, attributes.get("teaql.relation.configured_probe_threshold"));
        Assert.assertEquals("probe", attributes.get("teaql.relation.selected_plan"));
        Assert.assertEquals(3, attributes.get("teaql.relation.probe_count"));
    }

    @Test
    public void testNestedQueryWithoutAuthorizedRootTraceIsRejected() {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new DummyQueryExecutor())
                .build();
        try {
            new DefaultUserContext(runtime).internalExecuteForList(bareDummyRequest());
            Assert.fail("internal query without a trusted root trace must be rejected");
        } catch (TeaQLRuntimeException expected) {
            Assert.assertTrue(expected.getMessage().contains("INTERNAL QUERY CONTEXT REQUIRED"));
        }
    }

    private static SearchRequest<DummyEntity> bareDummyRequest() {
        return new BaseRequest<DummyEntity>(DummyEntity.class) {
            @Override
            public String getTypeName() {
                return "Dummy";
            }
        };
    }

    private static SearchRequest<DummyEntity> inheritedDummyRequest() {
        return new BaseRequest<DummyEntity>(DummyEntity.class) {
            private final QueryIntent rootIntent = QueryIntent.of("load dummy graph", "verify relation loading");
            { internalComment(rootIntent.comment()); internalPurpose(rootIntent.purpose()); }
            @Override public String getTypeName() { return "Dummy"; }
            @Override public QueryIntent inheritedQueryIntent() { return rootIntent; }
        };
    }

    public static class ContainerEntity extends BaseEntity {
        private DummyEntity rel1;
        private DummyEntity rel2;
        private DummyEntity rel3;
        private DummyEntity rel4;
        private Object relList;

        @Override
        public String typeName() {
            return "Container";
        }

        @Override
        public void __internalSet(String property, Object value) {
            switch (property) {
                case "rel1": this.rel1 = (DummyEntity) value; break;
                case "rel2": this.rel2 = (DummyEntity) value; break;
                case "rel3": this.rel3 = (DummyEntity) value; break;
                case "rel4": this.rel4 = (DummyEntity) value; break;
                case "relList": this.relList = value; break;
                default: super.__internalSet(property, value);
            }
        }

        @Override
        public Object __internalGet(String property) {
            switch (property) {
                case "rel1": return this.rel1;
                case "rel2": return this.rel2;
                case "rel3": return this.rel3;
                case "rel4": return this.rel4;
                case "relList": return this.relList;
                default: return super.__internalGet(property);
            }
        }
    }

    public static class AdvancedMetaFactory implements EntityMetaFactory {
        @Override
        public EntityDescriptor resolveEntityDescriptor(String type) {
            EntityDescriptor desc = new EntityDescriptor();
            desc.setType(type);
            desc.setDataService("dummy");
            if ("Container".equals(type)) {
                io.teaql.core.meta.Relation p1 = new io.teaql.core.meta.Relation(); p1.setName("rel1");  
                io.teaql.core.meta.Relation p2 = new io.teaql.core.meta.Relation(); p2.setName("rel2");  
                io.teaql.core.meta.Relation p3 = new io.teaql.core.meta.Relation(); p3.setName("rel3");  
                io.teaql.core.meta.Relation p4 = new io.teaql.core.meta.Relation();
                p4.setName("rel4");  

                io.teaql.core.meta.Relation pList = new io.teaql.core.meta.Relation();
                pList.setName("relList");  

                desc.setProperties(java.util.Arrays.asList(p1, p2, p3, p4, pList));
                desc.setEntitySupplier(() -> new ContainerEntity());
            } else {
                desc.setEntitySupplier(() -> new DummyEntity());
            }
            return desc;
        }

        @Override
        public void register(EntityDescriptor type) {}

        @Override
        public List<EntityDescriptor> allEntityDescriptors() { return null; }
    }

    @Test
    public void testSaveGraphLedgerClassificationAndExecutionOrder() throws Exception {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .build();

        // Simulate related entities
        EntityMutationLedger root = new EntityMutationLedger();
        root.pushChangeSet();
        root.setComment("root comment");

        DummyEntity e1 = new DummyEntity(); // to delete
        e1.updateId(101L);
        e1.set$status(EntityStatus.PERSISTED);
        e1.setEntityMutationLedger(root);
        e1.markForDeletion();

        DummyEntity e2 = new DummyEntity(); // to update
        e2.updateId(102L);
        e2.set$status(EntityStatus.PERSISTED);
        e2.setEntityMutationLedger(root);
        e2.updateProperty("name", "updated");

        DummyEntity e3 = new DummyEntity(); // to insert
        e3.updateId(103L);
        e3.set$status(EntityStatus.NEW);
        e3.setEntityMutationLedger(root);
        e3.updateProperty("name", "inserted");
        root.markAsNew(new EntityKey(e3.typeName(), e3.getId()));

        DummyEntity e4 = new DummyEntity(); // to delete only
        e4.updateId(104L);
        e4.set$status(EntityStatus.PERSISTED);
        e4.setEntityMutationLedger(root);
        e4.markForDeletion();
        
        java.util.Map<EntityKey, BaseEntity> realEntities = new java.util.HashMap<>();
        realEntities.put(new EntityKey("Dummy", 101L), e1);
        realEntities.put(new EntityKey("Dummy", 102L), e2);
        realEntities.put(new EntityKey("Dummy", 103L), e3);
        realEntities.put(new EntityKey("Dummy", 104L), e4);

        java.lang.reflect.Method method = TeaQLRuntime.class.getDeclaredMethod(
            "executeLedgerPlan", UserContext.class, EntityMutationLedger.class,
            MutationExecutor.class, java.util.Map.class, MutationGovernanceSnapshot.class, MutationIntent.class,
            java.util.Map.class, MutationTraceScope.class);
        method.setAccessible(true);
        method.invoke(runtime, new DefaultUserContext(runtime), root, executor, realEntities, null,
                MutationIntent.of("root comment"), java.util.Map.of(),
                MutationTraceScope.append(null, "Dummy", null, "root comment"));

        List<EntityPersistenceMutation> requests = executor.requests;

        List<EntityPersistenceMutation> deletes = new ArrayList<>();
        List<EntityPersistenceMutation> saves = new ArrayList<>();

        for (EntityPersistenceMutation req : requests) {
            if (req.getAction() == EntityPersistenceMutation.Action.DELETE) {
                deletes.add(req);
            } else {
                saves.add(req);
            }
        }

        boolean seenSave = false;
        for (EntityPersistenceMutation req : requests) {
            if (req.getAction() == EntityPersistenceMutation.Action.SAVE) {
                seenSave = true;
            } else if (req.getAction() == EntityPersistenceMutation.Action.DELETE) {
                Assert.assertFalse("DELETE should execute before SAVE", seenSave);
            }
        }

        Assert.assertTrue(deletes.stream().anyMatch(r -> r.getEntity().getId().equals(101L)));
        Assert.assertTrue(saves.stream().anyMatch(r -> r.getEntity().getId().equals(102L)));
        Assert.assertTrue(saves.stream().anyMatch(r -> r.getEntity().getId().equals(103L)));
        Assert.assertTrue(deletes.stream().anyMatch(r -> r.getEntity().getId().equals(104L))); // 104 was marked to remove, so it should be deleted.
        Assert.assertFalse(saves.stream().anyMatch(r -> r.getEntity().getId().equals(104L))); // 104 shouldn't be saved
        
        Assert.assertTrue(requests.stream().allMatch(r -> "root comment".equals(r.getEntity().getComment())));
        // The current change set is not cleared by executeLedgerPlan, it's cleared by saveGraph, so we can't test that here unless we do it.
        // I will omit that check or just check saveGraph does it.
    }

    @Test
    public void testSaveGraphMergesRelatedEntityMutationLedgers() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .build();

        ContainerEntity rootEntity = new ContainerEntity();
        rootEntity.updateId(1L);
        rootEntity.set$status(EntityStatus.PERSISTED);

        DummyEntity toOneChild = new DummyEntity();
        toOneChild.updateId(2L);
        toOneChild.set$status(EntityStatus.PERSISTED);
        
        DummyEntity listChild1 = new DummyEntity();
        listChild1.updateId(3L);
        listChild1.set$status(EntityStatus.PERSISTED);

        DummyEntity listChild2 = new DummyEntity();
        listChild2.updateId(4L);
        listChild2.set$status(EntityStatus.PERSISTED);
        
        rootEntity.updateProperty("rel1", toOneChild);
        rootEntity.updateProperty("relList", java.util.Arrays.asList(listChild1, listChild2));
        
        toOneChild.updateProperty("name", "toOne");
        listChild1.updateProperty("name", "list1");
        listChild2.updateProperty("name", "list2");
        
        rootEntity.setComment("test");
        runtime.saveGraph(new DefaultUserContext(runtime), rootEntity);
        
        // Verify all entities share the same root now
        Assert.assertSame(rootEntity.getEntityMutationLedger(), toOneChild.getEntityMutationLedger());
        Assert.assertSame(rootEntity.getEntityMutationLedger(), listChild1.getEntityMutationLedger());
        Assert.assertSame(rootEntity.getEntityMutationLedger(), listChild2.getEntityMutationLedger());
        
        List<EntityPersistenceMutation> requests = executor.requests;
        Assert.assertTrue(requests.stream().anyMatch(r -> r.getEntity().getId().equals(2L)));
        Assert.assertTrue(requests.stream().anyMatch(r -> r.getEntity().getId().equals(3L)));
        Assert.assertTrue(requests.stream().anyMatch(r -> r.getEntity().getId().equals(4L)));
    }

    @Test
    public void testSaveGraphDoesNotConsumeIndependentLedgerFromSameContext() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);

        DummyEntity first = new DummyEntity();
        first.updateId(11L);
        first.set$status(EntityStatus.PERSISTED);
        first.updateProperty("name", "save me");
        first.setComment("save first graph only");

        DummyEntity independent = new DummyEntity();
        independent.updateId(22L);
        independent.set$status(EntityStatus.PERSISTED);
        independent.updateProperty("name", "keep pending");

        Assert.assertNotSame(first.getEntityMutationLedger(), independent.getEntityMutationLedger());

        runtime.saveGraph(context, first);

        Assert.assertTrue(executor.requests.stream()
                .anyMatch(request -> request.getEntity().getId().equals(11L)));
        Assert.assertFalse(executor.requests.stream()
                .anyMatch(request -> request.getEntity().getId().equals(22L)));
        Assert.assertFalse(independent.getEntityMutationLedger()
                .changedFieldNames(new EntityKey("Dummy", 22L))
                .isEmpty());
        Assert.assertTrue(first.getEntityMutationLedger().currentChangeSet().isEmpty());
    }

    @Test
    public void testFailedSaveRetainsOnlyItsOwnPendingLedger() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor() {
            @Override
            public MutationResult mutate(UserContext context, PersistenceMutation request) {
                throw new TeaQLRuntimeException("expected mutation failure");
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);

        DummyEntity failing = new DummyEntity();
        failing.updateId(31L);
        failing.set$status(EntityStatus.PERSISTED);
        failing.updateProperty("name", "must remain pending");
        failing.setComment("fail this graph");

        DummyEntity independent = new DummyEntity();
        independent.updateId(32L);
        independent.set$status(EntityStatus.PERSISTED);
        independent.updateProperty("name", "independent pending");

        try {
            runtime.saveGraph(context, failing);
            Assert.fail("failed mutation was accepted");
        } catch (TeaQLRuntimeException expected) {
            Assert.assertEquals("expected mutation failure", expected.getMessage());
        }

        Assert.assertFalse(failing.getEntityMutationLedger()
                .changedFieldNames(new EntityKey("Dummy", 31L)).isEmpty());
        Assert.assertFalse(independent.getEntityMutationLedger()
                .changedFieldNames(new EntityKey("Dummy", 32L)).isEmpty());
        Assert.assertNotSame(failing.getEntityMutationLedger(), independent.getEntityMutationLedger());
    }

    @Test
    public void testSaveGraphAllocatesIdsAndRecordsChangesForNewChildren() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        AtomicLong ids = new AtomicLong(100);
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .idGenerationService((context, entity) -> ids.getAndIncrement())
                .build();

        ContainerEntity parent = new ContainerEntity();
        DummyEntity child = new DummyEntity();
        child.updateProperty("name", "new child");
        SmartList<DummyEntity> children = new SmartList<>();
        children.add(child);
        parent.updateProperty("relList", children);
        parent.auditAs("create graph").save(new DefaultUserContext(runtime));

        Assert.assertEquals(Long.valueOf(100), parent.getId());
        Assert.assertEquals(Long.valueOf(101), child.getId());
        Assert.assertSame(parent.getEntityMutationLedger(), child.getEntityMutationLedger());
        Assert.assertTrue(executor.requests.stream()
                .anyMatch(request -> "Container".equals(request.getEntity().typeName())
                        && parent.getId().equals(request.getEntity().getId())));
        Assert.assertTrue(executor.requests.stream()
                .anyMatch(request -> "Dummy".equals(request.getEntity().typeName())
                        && child.getId().equals(request.getEntity().getId())));
        Assert.assertTrue(executor.requests.stream()
                .noneMatch(request -> request.getEntity() == parent || request.getEntity() == child));
    }

    @Test
    public void testDelete() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .build();

        DummyEntity entity = new DummyEntity();
        entity.__internalSet("id", 1L);
        entity.__internalSet("version", 1L);
        entity.setComment("test delete");
        entity.set$status(EntityStatus.PERSISTED);
        entity.markForDeletion()
                .auditAs("test delete")
                .save(new DefaultUserContext(runtime));

        Assert.assertFalse(executor.requests.isEmpty());
        Assert.assertEquals(EntityPersistenceMutation.Action.DELETE, executor.requests.get(0).getAction());
    }

    @Test
    public void testMutationEmitsStandardAndMaskedApplicationAuditEvents() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        RecordingRuntimeLogSink standardSink = new RecordingRuntimeLogSink();
        DummyMetaFactory metadata = new DummyMetaFactory() {
            @Override
            public EntityDescriptor resolveEntityDescriptor(String type) {
                return super.resolveEntityDescriptor(type)
                        .auditMaskFields(java.util.List.of("name"))
                        .auditValueMaxLength(32);
            }
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(metadata)
                .dataService("dummy", executor)
                .idGenerationService((context, entity) -> 700L)
                .logSink(standardSink)
                .executionLogging(false)
                .build();
        DefaultUserContext context = new DefaultUserContext(runtime);
        List<SafeAuditEvent> appEvents = new ArrayList<>();
        context.putAttribute(AppAuditEventSink.class.getName(),
                (AppAuditEventSink) (auditContext, event) -> appEvents.add(event));

        DummyEntity entity = new DummyEntity();
        entity.updateProperty("name", "private-value");
        entity.auditAs("create audited entity 700").save(context);

        Assert.assertEquals(1, standardSink.auditEvents.size());
        RawAuditEvent raw = standardSink.auditEvents.get(0);
        Assert.assertEquals(MutationAuditKind.CREATED, raw.kind());
        Assert.assertEquals(Long.valueOf(700L), raw.entityId());
        Assert.assertEquals("[REDACTED]", raw.changes().stream()
                .filter(change -> "name".equals(change.field()))
                .findFirst().orElseThrow().newValue());
        Assert.assertTrue(raw.traceChain().stream().anyMatch(node ->
                node.getKind() == TraceKind.AUDIT_REASON
                        && "create audited entity [REDACTED]".equals(node.getComment())));

        Assert.assertEquals(1, appEvents.size());
        Assert.assertEquals(Long.valueOf(700L), appEvents.get(0).entityId());
        Assert.assertTrue(appEvents.get(0).traceChain().stream().anyMatch(node ->
                node.getKind() == TraceKind.AUDIT_REASON
                        && "create audited entity [REDACTED]".equals(node.getComment())));
        SafeAuditField safeName = appEvents.get(0).fields().stream()
                .filter(field -> "name".equals(field.name()))
                .findFirst().orElseThrow();
        Assert.assertTrue(safeName.masked());
        Assert.assertNotEquals("private-value", safeName.value());
        Assert.assertEquals("pr*********ue", safeName.value());
    }
}
