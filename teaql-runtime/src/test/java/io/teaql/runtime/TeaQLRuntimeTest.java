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
import java.util.Map;

public class TeaQLRuntimeTest {

    @Test
    public void executionLoggingDefaultsOnAndCanBeDisabledWithoutDisablingAudit() {
        TeaQLRuntime defaultRuntime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .build();
        Assert.assertTrue(defaultRuntime.isExecutionLoggingEnabled());
        Assert.assertTrue(new DefaultUserContext(defaultRuntime).isExecutionLoggingEnabled());

        TeaQLRuntime quietRuntime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .executionLogging(false)
                .build();
        Assert.assertFalse(quietRuntime.isExecutionLoggingEnabled());
        Assert.assertFalse(new DefaultUserContext(quietRuntime).isExecutionLoggingEnabled());
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
            if ("name".equals(property)) {
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

    public static class RecordingMutationExecutor implements MutationExecutor {
        public final List<DefaultMutationRequest> requests = new ArrayList<>();

        @Override
        public MutationResult mutate(UserContext context, MutationRequest request) {
            if (request instanceof DefaultMutationRequest) {
                DefaultMutationRequest mutationRequest = (DefaultMutationRequest) request;
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

        @Override
        public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {}

        @Override
        public void writeAuditEvent(UserContext context, RawAuditEvent event) {
            auditEvents.add(event);
        }
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
                .requestPolicy(new RequestPolicy() {
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
        SearchRequest<DummyEntity> nested = bareDummyRequest();

        context.pushTrace("authorized root query");
        try {
            Assert.assertEquals(1, context.internalExecuteForList(nested).size());
            Assert.assertEquals(List.of("relation_load", "provider"), families);
        } finally {
            context.popTrace();
        }
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
        e1.markToRemove();

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
        e4.markToRemove();
        
        java.util.Map<EntityKey, BaseEntity> realEntities = new java.util.HashMap<>();
        realEntities.put(new EntityKey("Dummy", 101L), e1);
        realEntities.put(new EntityKey("Dummy", 102L), e2);
        realEntities.put(new EntityKey("Dummy", 103L), e3);
        realEntities.put(new EntityKey("Dummy", 104L), e4);

        java.lang.reflect.Method method = TeaQLRuntime.class.getDeclaredMethod(
            "executeLedgerPlan", UserContext.class, EntityMutationLedger.class, MutationExecutor.class, java.util.Map.class);
        method.setAccessible(true);
        method.invoke(runtime, new DefaultUserContext(runtime), root, executor, realEntities);

        List<DefaultMutationRequest> requests = executor.requests;

        List<DefaultMutationRequest> deletes = new ArrayList<>();
        List<DefaultMutationRequest> saves = new ArrayList<>();

        for (DefaultMutationRequest req : requests) {
            if (req.getAction() == DefaultMutationRequest.Action.DELETE) {
                deletes.add(req);
            } else {
                saves.add(req);
            }
        }

        boolean seenSave = false;
        for (DefaultMutationRequest req : requests) {
            if (req.getAction() == DefaultMutationRequest.Action.SAVE) {
                seenSave = true;
            } else if (req.getAction() == DefaultMutationRequest.Action.DELETE) {
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
        
        List<DefaultMutationRequest> requests = executor.requests;
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
            public MutationResult mutate(UserContext context, MutationRequest request) {
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
                .anyMatch(request -> request.getEntity() == parent));
        Assert.assertTrue(executor.requests.stream()
                .anyMatch(request -> request.getEntity() == child));
    }

    @Test
    public void testDelete() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .build();

        DummyEntity entity = new DummyEntity();
        entity.setComment("test delete");
        entity.set$status(EntityStatus.PERSISTED);
        runtime.delete(new DefaultUserContext(runtime), entity);

        Assert.assertFalse(executor.requests.isEmpty());
        Assert.assertEquals(DefaultMutationRequest.Action.DELETE, executor.requests.get(0).getAction());
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
        entity.auditAs("create audited entity").save(context);

        Assert.assertEquals(1, standardSink.auditEvents.size());
        RawAuditEvent raw = standardSink.auditEvents.get(0);
        Assert.assertEquals(MutationAuditKind.CREATED, raw.kind());
        Assert.assertEquals(Long.valueOf(700L), raw.entityId());
        Assert.assertEquals("private-value", raw.changes().stream()
                .filter(change -> "name".equals(change.field()))
                .findFirst().orElseThrow().newValue());
        Assert.assertEquals("create audited entity", raw.traceChain().get(0).getComment());

        Assert.assertEquals(1, appEvents.size());
        SafeAuditField safeName = appEvents.get(0).fields().stream()
                .filter(field -> "name".equals(field.name()))
                .findFirst().orElseThrow();
        Assert.assertTrue(safeName.masked());
        Assert.assertNotEquals("private-value", safeName.value());
    }
}
