package com.example.schoolmanagementservice;

import io.teaql.core.BaseEntity;
import io.teaql.core.FieldLayout;
import io.teaql.core.LoadState;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** State-bookkeeping probe, excluding entity payloads, JDBC and fixture construction. */
public class LoadStateAllocationTest {
    private static volatile Object consumed;
    private static final class Probe extends BaseEntity {}
    private record Sample(long bytes, long nanos) {}
    static void verifyGeneratedSharing(io.teaql.core.SmartList<?> list) {
        var bean=(com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        var control=measure(bean,1,() -> consumed=new byte[8]);
        assertTrue(control.bytes()>0,"allocation counter must observe its positive control");
        var state=((BaseEntity)list.first()).__internalLoadState();
        for(var row:list)assertSame(state,((BaseEntity)row).__internalLoadState());
        Runnable action=list::__internalShareLoadStates;
        for(int i=0;i<10_000;i++)action.run();
        // Warm the exact counter/action invocation path, not only the list loop.
        // Counter/JIT initialization is excluded from the steady-state claim.
        for(int i=0;i<100;i++)measure(bean,100,action);
        for(int iterations:new int[]{1,100,10_000}) {
            int consecutiveZero=0;
            for(int attempt=0;attempt<20 && consecutiveZero<3;attempt++) {
                long bytes=measure(bean,iterations,action).bytes();
                // A bounded stable window excludes intermittent JVM counter/JIT
                // setup, but cannot accept an action that allocates on every call.
                consecutiveZero=bytes==0?consecutiveZero+1:0;
                System.out.printf("GENERATED_LIST_FINALIZE,%d,%d,%d%n",iterations,attempt,bytes);
            }
            assertEquals(3,consecutiveZero,"shared generated lists require three consecutive allocation-free samples");
        }
        for(var row:list)assertSame(state,((BaseEntity)row).__internalLoadState());
        System.out.println("PASS generated Java homogeneous list finalization allocates zero and preserves shared state");
    }
    private static final class SharingProbe extends BaseEntity {
        static {
            Map<String,Integer> indexes = new LinkedHashMap<>();
            Map<String,List<String>> mappings = new LinkedHashMap<>();
            for(int i=0;i<130;i++) {
                String name=i==0?"id":i==1?"version":"field_"+i;
                indexes.put(name,i);mappings.put(name,List.of(name,name));
            }
            FieldLayout.installGenerated(FieldLayout.generated(SharingProbe.class,"sharing-v1",indexes,mappings,Set.of()));
        }
    }

    @Test
    public void homogeneousListFinalizationAllocationProbe() {
        var bean=(com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        new SharingProbe(); // Install type metadata outside measurement.
        var layout=FieldLayout.forType(SharingProbe.class);
        for(boolean wide:new boolean[]{false,true}) {
            List<String> fields=new ArrayList<>();
            for(int i=0;i<(wide?130:3);i++)fields.add(i==0?"id":i==1?"version":"field_"+i);
            var shape=LoadState.projection(layout,fields).withDynamicSelection(Set.of("note"));
            for(int count:new int[]{1,100,10_000}) {
                List<SharingProbe> rows=new ArrayList<>(count);
                for(int i=0;i<count;i++){var row=new SharingProbe();row.__internalUseLoadState(shape);rows.add(row);}
                var list=io.teaql.core.SmartList.takeOwnership(rows);
                Runnable action=list::__internalShareLoadStates;
                for(int i=0;i<4_000;i++)action.run();
                var sample=measure(bean,100,action);
                for(var row:rows)assertSame(shape,row.__internalLoadState());
                System.out.printf("LIST_FINALIZE,%s,%d,%d%n",wide,count,sample.bytes());
                assertEquals(0,sample.bytes(),"already-shared list finalization must not build a grouping index");
            }
        }
    }

    // Retained old no-op algorithm, not a production compatibility path.
    private static LoadState referenceDynamicNoop(LoadState state, Set<String> codes) {
        Set<String> names = new java.util.HashSet<>(state.selectedNames());
        names.removeIf(name -> name.startsWith("#"));
        for (String code : codes) names.add("#" + code);
        if (!names.equals(state.selectedNames())) throw new AssertionError("reference accepts only unchanged geometry");
        return state;
    }

    private static Sample measure(com.sun.management.ThreadMXBean bean, int count, Runnable action) {
        long thread = Thread.currentThread().getId();
        long startBytes = bean.getThreadAllocatedBytes(thread);
        long startTime = System.nanoTime();
        for (int i = 0; i < count; i++) action.run();
        long nanos = System.nanoTime() - startTime;
        long bytes = bean.getThreadAllocatedBytes(thread) - startBytes;
        return new Sample(bytes, nanos);
    }

    private static Sample steadyNoop(com.sun.management.ThreadMXBean bean, String name,
            int width, int count, Runnable action, LoadState expected) {
        int zeroRun = 0;
        for (int attempt = 0; attempt < 20; attempt++) {
            Sample sample = measure(bean, count, action);
            assertSame(expected, consumed);
            System.out.printf("NOOP_WINDOW,%s,%d,%d,%d,%d,%d%n",
                    name, width, count, attempt, sample.bytes(), sample.nanos());
            zeroRun = sample.bytes() == 0 ? zeroRun + 1 : 0;
            if (zeroRun == 3) return sample;
        }
        throw new AssertionError("No three consecutive zero-allocation samples for " + name);
    }

    @Test
    public void loadedAvailabilityAllocationProbe() {
        assertTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean, "Requires a JVM thread-allocation counter");
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported(), "JVM thread allocation counter must be supported");
        bean.setThreadAllocatedMemoryEnabled(true);
        assertTrue(measure(bean, 1, () -> consumed = new byte[4096]).bytes() >= 4096,
                "positive allocation calibration must be visible");
        System.out.println("case,width,iterations,allocated_bytes,elapsed_ns");
        for (int width : new int[]{4, 64, 130}) {
            Map<String, Integer> indexes = new LinkedHashMap<>();
            Map<String, List<String>> mappings = new LinkedHashMap<>();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < width; i++) {
                String name = i == 0 ? "id" : i == 1 ? "version" : "field_" + i;
                indexes.put(name, i);
                mappings.put(name, List.of(name, name));
                names.add(name);
            }
            var layout = FieldLayout.generated(Probe.class, "width-" + width, indexes, mappings, Set.of("child_list"));
            var base = LoadState.projection(layout, names);
            Set<String> codes = Set.of("note");
            var selected = base.withDynamicSelection(codes);
            Runnable fixed = () -> consumed = base.withLoaded("id", true);
            Runnable dynamic = () -> consumed = selected.withDynamicSelection(codes);
            Set<String> viewCodes = new java.util.HashMap<>(Map.of("note", "private payload")).keySet();
            Runnable dynamicView = () -> consumed = selected.withDynamicSelection(viewCodes);
            Runnable reference = () -> consumed = referenceDynamicNoop(selected, codes);
            for (int i = 0; i < 30_000; i++) { fixed.run(); dynamic.run(); dynamicView.run(); reference.run(); }
            for (int iterations : new int[]{1, 100, 10_000}) {
                Sample result = steadyNoop(bean, "fixed", width, iterations, fixed, base);
                System.out.printf("loaded_fixed_noop,%d,%d,%d,%d%n", width, iterations, result.bytes(), result.nanos());
                assertSame(base, consumed);
                assertEquals(0, result.bytes(), "fixed value-only availability must not allocate");
                result = steadyNoop(bean, "dynamic", width, iterations, dynamic, selected);
                System.out.printf("loaded_dynamic_noop,%d,%d,%d,%d%n", width, iterations, result.bytes(), result.nanos());
                assertSame(selected, consumed);
                assertEquals(0, result.bytes(), "unchanged dynamic selection must not allocate");
                result = steadyNoop(bean, "dynamic-map-view", width, iterations, dynamicView, selected);
                System.out.printf("loaded_dynamic_map_view_noop,%d,%d,%d,%d%n", width, iterations, result.bytes(), result.nanos());
                assertSame(selected, consumed);
                assertEquals(0, result.bytes(), "map-backed selected-code views must not allocate iterators");
                result = measure(bean, iterations, reference);
                System.out.printf("reference_dynamic_noop,%d,%d,%d,%d%n", width, iterations, result.bytes(), result.nanos());
                assertSame(selected, consumed);
            }
        }
    }
}
