package io.teaql.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable, type-local fixed positions supplied by the generated library. */
public final class FieldLayout {
    private static final ClassValue<Installation> TYPES = new ClassValue<>() {
        @Override
        protected Installation computeValue(Class<?> type) {
            return new Installation();
        }
    };

    private static final class Installation {
        private volatile FieldLayout layout;

        FieldLayout resolve(Class<?> type) {
            FieldLayout current = layout;
            if (current != null) return current;
            synchronized (this) {
                if (layout == null) layout = new FieldLayout(type, "unindexed", Map.of(), Map.of(), Set.of(), false);
                return layout;
            }
        }

        synchronized FieldLayout install(FieldLayout candidate) {
            if (layout == null) {
                layout = candidate;
            } else if (!layout.generated || !layout.revision.equals(candidate.revision)
                    || !layout.indexes.equals(candidate.indexes) || !layout.aliases.equals(candidate.aliases)
                    || !layout.members.equals(candidate.members)
                    || !layout.relations.equals(candidate.relations)) {
                throw new IllegalArgumentException("Incompatible or late generated field-layout installation: " + candidate.entityType.getName());
            }
            return layout;
        }
    }

    private final Class<?> entityType;
    private final String revision;
    private final Map<String, Integer> indexes;
    private final Map<String, Integer> aliases;
    private final Map<String, String> members;
    private final Set<String> relations;
    private final boolean generated;
    private final LoadState empty;

    private FieldLayout(Class<?> entityType, String revision, Map<String, Integer> indexes,
                        Map<String, List<String>> mappings, Set<String> relations, boolean generated) {
        this.entityType = java.util.Objects.requireNonNull(entityType, "entityType");
        if (revision == null || revision.isBlank()) throw new IllegalArgumentException("Missing field-layout revision");
        this.revision = revision;
        this.generated = generated;
        this.indexes = Map.copyOf(indexes);
        this.relations = Set.copyOf(relations);
        Map<String, Integer> aliases = new HashMap<>();
        Map<String, String> members = new HashMap<>();
        Set<Integer> positions = new HashSet<>();
        if (!mappings.keySet().equals(indexes.keySet())) {
            throw new IllegalArgumentException("Incomplete field mappings for " + entityType.getName());
        }
        for (Map.Entry<String, Integer> field : indexes.entrySet()) {
            String name = field.getKey();
            Integer index = field.getValue();
            if (name == null || name.isBlank() || name.startsWith("#") || name.startsWith("_")) {
                throw new IllegalArgumentException("Invalid fixed field: " + name);
            }
            if (index == null || index < 0 || index >= indexes.size() || !positions.add(index)) {
                throw new IllegalArgumentException("Invalid or duplicate fixed index: " + name + "=" + index);
            }
            List<String> mapping = mappings.get(name);
            if (mapping == null || mapping.size() != 2) {
                throw new IllegalArgumentException("Expected member/column mapping for " + name);
            }
            addAlias(aliases, name, index);
            addAlias(aliases, mapping.get(0), index);
            addAlias(aliases, mapping.get(1), index);
            members.put(name, mapping.get(0));
        }
        if (generated && (!indexes.containsKey("id") || !indexes.containsKey("version"))) {
            throw new IllegalArgumentException("Generated layout must include id and version: " + entityType.getName());
        }
        this.aliases = Map.copyOf(aliases);
        this.members = Map.copyOf(members);
        empty = LoadState.empty(this);
    }

    public static FieldLayout generated(Class<?> entityType, String revision, Map<String, Integer> indexes,
                                        Map<String, List<String>> mappings, Set<String> relations) {
        return new FieldLayout(entityType, revision, indexes, mappings, relations, true);
    }

    @FrameworkInternal("Generated metadata and hydration infrastructure only")
    public static FieldLayout forType(Class<?> entityType) {
        return TYPES.get(entityType).resolve(entityType);
    }

    /** Generated static initialization supplies metadata directly, without reflection or row construction. */
    @FrameworkInternal("Generated metadata installation only")
    public static FieldLayout installGenerated(FieldLayout layout) {
        java.util.Objects.requireNonNull(layout, "layout");
        if (!layout.generated) throw new IllegalArgumentException("Only generated field layouts can be installed");
        return TYPES.get(layout.entityType).install(layout);
    }

    public Class<?> entityType() { return entityType; }
    public String revision() { return revision; }
    public boolean isGenerated() { return generated; }
    public Map<String, Integer> indexes() { return indexes; }
    public Integer findIndex(String fieldOrAlias) { return aliases.get(fieldOrAlias); }
    /** Generated language-native accessor key, not a SQL column or reflected bean property. */
    public String memberName(String canonicalField) { return members.get(canonicalField); }
    /** Shared named relation metadata; these members do not consume fixed-field positions. */
    @FrameworkInternal("Typed graph hydration and presentation only")
    public Set<String> relationNames() { return relations; }
    public LoadState emptyState() { return empty; }

    int indexForLoad(String name) {
        Integer index = findIndex(name);
        if (index != null) return index;
        if (name == null || name.isBlank() || name.startsWith("_")) {
            throw new IllegalArgumentException("Not a loadable field: " + name);
        }
        if (!generated || name.startsWith("#") || relations.contains(name)) return -1;
        throw new IllegalArgumentException("Unknown field in generated layout: " + entityType.getName() + "." + name);
    }

    public void validateExpectedMembers(Set<String> members) {
        if (!generated) return;
        Set<Integer> expected = new HashSet<>();
        for (String member : members) {
            Integer index = findIndex(member);
            if (index == null) throw new IllegalArgumentException("Missing generated field index: " + member);
            expected.add(index);
        }
        if (expected.size() != indexes.size()) {
            throw new IllegalArgumentException("Generated layout disagrees with entity descriptor: " + entityType.getName());
        }
    }

    private static void addAlias(Map<String, Integer> aliases, String alias, int index) {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Blank field alias");
        Integer previous = aliases.putIfAbsent(alias, index);
        if (previous != null && previous != index) throw new IllegalArgumentException("Ambiguous field alias: " + alias);
    }

}
