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

    @Test
    public void loadedAvailabilityAllocationProbe() {
        assertTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean, "Requires a JVM thread-allocation counter");
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(bean.isThreadAllocatedMemorySupported(), "JVM thread allocation counter must be supported");
        bean.setThreadAllocatedMemoryEnabled(true);
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
                Sample result = measure(bean, iterations, fixed);
                System.out.printf("loaded_fixed_noop,%d,%d,%d,%d%n", width, iterations, result.bytes(), result.nanos());
                assertSame(base, consumed);
                assertEquals(0, result.bytes(), "fixed value-only availability must not allocate");
                result = measure(bean, iterations, dynamic);
                System.out.printf("loaded_dynamic_noop,%d,%d,%d,%d%n", width, iterations, result.bytes(), result.nanos());
                assertSame(selected, consumed);
                assertEquals(0, result.bytes(), "unchanged dynamic selection must not allocate");
                result = measure(bean, iterations, dynamicView);
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
