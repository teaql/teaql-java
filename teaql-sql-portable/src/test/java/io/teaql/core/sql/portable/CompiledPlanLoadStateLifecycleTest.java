package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompiledPlanLoadStateLifecycleTest {
    public static class Row extends BaseEntity {
        static {
            Map<String, Integer> slots = new LinkedHashMap<>();
            Map<String, List<String>> mappings = new LinkedHashMap<>();
            slots.put("id", 0); slots.put("version", 1);
            for (int i = 0; i < 10; i++) slots.put("p" + i, i + 2);
            slots.keySet().forEach(name -> mappings.put(name, List.of(name, name)));
            FieldLayout.installGenerated(FieldLayout.generated(Row.class, "cache-lifetime-v1", slots, mappings, Set.of()));
        }
        private final String[] payload = new String[10];
        @Override public String typeName() { return "CacheRow"; }
        @Override public Object __internalGet(String name) {
            if (name.startsWith("p")) return payload[Integer.parseInt(name.substring(1))];
            return super.__internalGet(name);
        }
        @Override public void __internalSet(String name, Object value) {
            if (name.startsWith("p")) { payload[Integer.parseInt(name.substring(1))] = (String) value; return; }
            super.__internalSet(name, value);
        }
    }
    private static class Request extends BaseRequest<Row> {
        Request(int mask) {
            super(Row.class);
            selectProperty("id"); selectProperty("version");
            for (int i = 0; i < 10; i++) if ((mask & (1 << i)) != 0) selectProperty("p" + i);
            offset(0, 1);
            internalComment("check compiled projection cache lifecycle");
            internalPurpose("retain immutable row states without retaining historic plans");
        }
        @Override public String getTypeName() { return "CacheRow"; }
    }
    private static class Fixture {
        volatile CyclicBarrier mapperBarrier;
        final AtomicInteger compilations = new AtomicInteger();
        final PortableSQLRepository<Row> repository;
        final TeaQLRuntime runtime;
        Fixture() {
            new Row(); // Install fixed metadata before the repository compiles its first shape.
            SQLEntityDescriptor descriptor = new SQLEntityDescriptor() {
                @Override public boolean hasChildren() {
                    // Test-only rendezvous at actual mapper construction, after the old
                    // implementation's capacity check. No production hook is added.
                    var barrier = mapperBarrier;
                    if (barrier != null && StackWalker.getInstance().walk(frames ->
                            frames.anyMatch(frame -> frame.getMethodName().equals("compileRowMapper")))) {
                        try { barrier.await(10, TimeUnit.SECONDS); }
                        catch (Exception error) { throw new AssertionError(error); }
                    }
                    return super.hasChildren();
                }
            };
            descriptor.setType("CacheRow"); descriptor.setTargetType(Row.class);
            descriptor.setEntitySupplier(Row::new); descriptor.setDataService("cache-test");
            descriptor.with("table_name", "cache_row_data"); descriptor.setAuditMaskFields(List.of());
            for (String name : List.of("id", "version", "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9")) {
                var property = (GenericSQLProperty) descriptor.addSimpleProperty(name,
                        name.equals("id") || name.equals("version") ? Long.class : String.class);
                property.setColumnType(name.equals("id") || name.equals("version") ? "BIGINT" : "VARCHAR(40)");
            }
            var metadata = new SimpleEntityMetaFactory(); metadata.register(descriptor);
            runtime = TeaQLRuntime.builder().metadata(metadata).build();
            TeaQLDatabase database = (TeaQLDatabase) Proxy.newProxyInstance(TeaQLDatabase.class.getClassLoader(),
                    new Class<?>[]{TeaQLDatabase.class}, (proxy, method, args) -> {
                        if (method.getName().equals("supportsCompiledRowMapping")) return true;
                        if (method.getName().equals("query") && args.length == 5 && args[3] instanceof CompiledRowMapper<?> mapper) {
                            return List.of(mapper.map(new DataRow() {
                                @Override public Object get(int index) { return index < 3 ? 1L : "value"; }
                                @Override public <V> V get(int index, Class<V> type) { return type.cast(get(index)); }
                            }));
                        }
                        throw new AssertionError("unexpected provider call " + method);
                    });
            repository = new PortableSQLRepository<>(descriptor, database, null, metadata) {
                @Override public String buildDataSQL(UserContext context, SearchRequest request, Map<String, Object> params) {
                    compilations.incrementAndGet(); return super.buildDataSQL(context, request, params);
                }
            };
        }
        SmartList<Row> load(int mask) { return repository.loadInternal(new DefaultUserContext(runtime), new Request(mask)); }
        Map<?, ?> plans() throws Exception {
            var field = PortableSQLRepository.class.getDeclaredField("compiledQueryPlans");
            field.setAccessible(true); return (Map<?, ?>) field.get(repository);
        }
    }

    @Test public void concurrentMissesCannotExceedThePlanCapacity() throws Exception {
        Fixture fixture = new Fixture();
        for (int mask = 1; mask <= 508; mask++) fixture.load(mask);
        assertEquals(508, fixture.plans().size());
        fixture.mapperBarrier = new CyclicBarrier(8);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<SmartList<Row>>> futures = new ArrayList<>();
            for (int mask = 509; mask <= 516; mask++) {
                int selected = mask; futures.add(workers.submit(() -> fixture.load(selected)));
            }
            for (var future : futures) assertEquals(1, future.get(15, TimeUnit.SECONDS).size());
        } finally {
            fixture.mapperBarrier = null; workers.shutdownNow();
        }
        assertTrue("cache capacity must hold after simultaneous misses: " + fixture.plans().size(), fixture.plans().size() <= 512);
    }

    @Test public void evictionKeepsLiveResultsButDoesNotRetainReleasedSnapshots() throws Exception {
        Fixture fixture = new Fixture();
        WeakReference<LoadState> state = evictWhileResultAlive(fixture);
        for (int attempt = 0; state.get() != null && attempt < 50; attempt++) {
            System.gc(); Thread.sleep(20);
        }
        assertNull("evicted plans must not retain a released result's load geometry", state.get());
    }

    @Test public void concurrentSameShapeMissesReuseOneWinningImmutableState() throws Exception {
        Fixture fixture = new Fixture();
        fixture.mapperBarrier = new CyclicBarrier(8);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<SmartList<Row>>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) futures.add(workers.submit(() -> fixture.load(3)));
            LoadState shared = null;
            for (var future : futures) {
                var row = future.get(15, TimeUnit.SECONDS).get(0);
                if (shared == null) shared = row.__internalLoadState();
                else assertSame(shared, row.__internalLoadState());
                assertTrue(row.isPropertyLoaded("p0")); assertTrue(row.isPropertyLoaded("p1"));
                assertFalse(row.isPropertyLoaded("p2"));
            }
        } finally {
            fixture.mapperBarrier = null; workers.shutdownNow();
        }
        assertEquals(1, fixture.plans().size());
    }

    private static WeakReference<LoadState> evictWhileResultAlive(Fixture fixture) throws Exception {
        SmartList<Row> live = fixture.load(1);
        LoadState state = live.get(0).__internalLoadState();
        var oldPlan = fixture.plans().values().iterator().next();
        assertTrue(state.isLoaded("p0")); assertFalse(state.isLoaded("p1"));
        assertTrue(state.overflow().isEmpty()); assertTrue(state.selectedNames().isEmpty());
        assertSame(state, fixture.load(1).get(0).__internalLoadState());
        assertEquals("identical request shape must hit the plan cache", 1, fixture.compilations.get());
        for (int mask = 2; mask <= 513; mask++) fixture.load(mask);
        assertTrue(fixture.plans().size() <= 512);
        assertFalse("the oldest plan must have been evicted", fixture.plans().containsValue(oldPlan));
        assertEquals("value", live.get(0).__internalGet("p0"));
        assertSame(state, live.get(0).__internalLoadState());
        assertFalse(live.get(0).isPropertyLoaded("p1"));
        return new WeakReference<>(state);
    }
}
