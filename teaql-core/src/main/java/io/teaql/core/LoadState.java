package io.teaql.core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable availability snapshot; it never contains entity values or mutation ownership. */
public final class LoadState {
    private static final String[] NO_DYNAMIC_CODES = new String[0];
    private final FieldLayout layout;
    private final long bits;
    private final Set<Integer> overflow;
    // Dynamic field selection, named relation materialization and unindexed adapter fields.
    private final Set<String> selectedNames;
    private final int hash;
    // Value-free memoization shared by the snapshot, never a per-entity payload.
    private volatile String[] dynamicCodes;

    private LoadState(FieldLayout layout, long bits, Set<Integer> overflow, Set<String> selectedNames) {
        this(layout, bits, overflow, selectedNames, selectedNames.isEmpty() ? NO_DYNAMIC_CODES : null);
    }

    private LoadState(FieldLayout layout, long bits, Set<Integer> overflow, Set<String> selectedNames,
                      String[] dynamicCodes) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.bits = bits;
        this.overflow = overflow;
        this.selectedNames = selectedNames;
        this.dynamicCodes = dynamicCodes;
        int result = System.identityHashCode(layout);
        result = 31 * result + Long.hashCode(bits);
        result = 31 * result + overflow.hashCode();
        hash = 31 * result + selectedNames.hashCode();
    }

    static LoadState empty(FieldLayout layout) {
        return new LoadState(layout, 0L, Set.of(), Set.of());
    }

    /** Build once per actual query shape, not once per row. */
    public static LoadState projection(FieldLayout layout, Iterable<String> fields) {
        long bits = 0L;
        Set<Integer> overflow = null;
        Set<String> names = null;
        for (String field : fields) {
            int index = layout.indexForLoad(field);
            if (index >= 0 && index < Long.SIZE) bits |= 1L << index;
            else if (index >= Long.SIZE) {
                if (overflow == null) overflow = new HashSet<>();
                overflow.add(index);
            } else {
                if (names == null) names = new HashSet<>();
                names.add(field);
            }
        }
        if (bits == 0L && overflow == null && names == null) return layout.emptyState();
        return new LoadState(layout, bits, overflow == null ? Set.of() : Set.copyOf(overflow),
                names == null ? Set.of() : Set.copyOf(names));
    }

    public FieldLayout layout() { return layout; }
    public long bits() { return bits; }
    public Set<Integer> overflow() { return overflow; }
    public Set<String> selectedNames() { return selectedNames; }

    /** Build once for a dynamic-field batch shape, preserving fixed slots and relation markers. */
    public LoadState withDynamicSelection(Set<String> codes) {
        // A custom sorted comparator may equate different literal field codes.
        // Preserve the original String-identity semantics before the fast membership check.
        if (codes instanceof java.util.SortedSet<?> sorted && sorted.comparator() != null) {
            codes = Set.copyOf(codes);
        }
        String[] current = dynamicCodes();
        boolean same = current.length == codes.size();
        if (same) {
            for (String code : current) {
                if (!codes.contains(code)) { same = false; break; }
            }
        }
        if (same) return this;
        String[] stableCodes = codes.isEmpty() ? NO_DYNAMIC_CODES : codes.toArray(String[]::new);
        Set<String> names = new HashSet<>(selectedNames);
        names.removeIf(name -> name.startsWith("#"));
        for (String code : stableCodes) names.add("#" + Objects.requireNonNull(code, "dynamic code"));
        return new LoadState(layout, bits, overflow, Set.copyOf(names), stableCodes);
    }

    private String[] dynamicCodes() {
        String[] current = dynamicCodes;
        if (current != null) return current;
        java.util.List<String> codes = null;
        for (String name : selectedNames) {
            if (name.startsWith("#")) {
                if (codes == null) codes = new java.util.ArrayList<>();
                codes.add(name.substring(1));
            }
        }
        current = codes == null ? NO_DYNAMIC_CODES : codes.toArray(String[]::new);
        dynamicCodes = current;
        return current;
    }

    public boolean isLoaded(String fieldOrAlias) {
        if (fieldOrAlias == null) return false;
        Integer index = layout.findIndex(fieldOrAlias);
        if (index == null) return selectedNames.contains(fieldOrAlias);
        return index < Long.SIZE ? (bits & (1L << index)) != 0 : overflow.contains(index);
    }

    /** Value-only updates keep this exact reference; availability changes create a private snapshot. */
    public LoadState withLoaded(String field, boolean loaded) {
        int index = layout.indexForLoad(field);
        if (isLoaded(field) == loaded) return this;
        if (index >= 0 && index < Long.SIZE) {
            long next = loaded ? bits | (1L << index) : bits & ~(1L << index);
            return new LoadState(layout, next, overflow, selectedNames, dynamicCodes);
        }
        if (index >= Long.SIZE) {
            Set<Integer> next = new HashSet<>(overflow);
            if (loaded) next.add(index); else next.remove(index);
            return new LoadState(layout, bits, Set.copyOf(next), selectedNames, dynamicCodes);
        }
        Set<String> next = new HashSet<>(selectedNames);
        if (loaded) next.add(field); else next.remove(field);
        return new LoadState(layout, bits, overflow, Set.copyOf(next));
    }

    @Override
    public int hashCode() { return hash; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LoadState state)) return false;
        return layout == state.layout && bits == state.bits
                && overflow.equals(state.overflow) && selectedNames.equals(state.selectedNames);
    }
}
