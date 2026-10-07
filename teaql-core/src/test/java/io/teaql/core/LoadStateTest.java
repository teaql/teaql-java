package io.teaql.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

public class LoadStateTest {
    @BeforeClass
    public static void installFixtureMetadata() {
        assertTrue(IndexedEntity.__TEAQL_FIELD_LAYOUT.isGenerated());
    }

    @Test
    public void readonlyHydrationKeepsLedgersLazyAndFirstMutationCapturesThePrivateBaseline() {
        var shape = LoadState.projection(IndexedEntity.__TEAQL_FIELD_LAYOUT, List.of("id", "version", "base_url"));
        var first = new IndexedEntity(); var second = new IndexedEntity();
        for (var entity : List.of(first, second)) {
            entity.__internalUseLoadState(shape);
            entity.__internalHydrate("id", 1001L, 0);
            entity.__internalHydrate("version", 7L, 1);
            entity.__internalHydrate("baseUrl", null, 2);
            entity.set$status(EntityStatus.PERSISTED);
            assertFalse(entity.__internalHasMutationLedger());
            assertEquals(Long.valueOf(7), entity.getOriginalVersion());
            assertTrue(entity.getUpdatedProperties().isEmpty());
            entity.clearUpdatedProperties();
        }
        first.setComment("private change");
        first.handleUpdate("baseUrl", null, "changed");
        first.__internalSet("baseUrl", "changed");
        assertTrue(first.__internalHasMutationLedger());
        assertFalse(second.__internalHasMutationLedger());
        assertEquals(Long.valueOf(7), first.getOriginalVersion());
        assertEquals("changed", first.getProperty("baseUrl"));
        assertNull(second.getProperty("baseUrl"));
        assertSame(shape, first.__internalLoadState()); assertSame(shape, second.__internalLoadState());
        assertEquals("private change", first.getEntityMutationLedger().getComment());
        assertNotSame(first.getEntityMutationLedger(), second.getEntityMutationLedger());
    }

    @Test
    public void rollbackRestoresTheLazyLoadedVersionWithoutMaterializingALedger() {
        var entity = new IndexedEntity();
        entity.__internalHydrate("id", 1001L, 0);
        entity.__internalHydrate("version", 7L, 1);
        entity.__internalHydrate("version", 8L, 1);
        entity.__internalRestorePersistenceState(7L, EntityStatus.PERSISTED, true);
        assertEquals(Long.valueOf(7), entity.getOriginalVersion());
        assertFalse(entity.__internalHasMutationLedger());
        assertEquals(Long.valueOf(7), entity.getEntityMutationLedger().getOriginalVersion(new EntityKey(entity.typeName(), 1001L)));
    }

    @Test
    public void dynamicSelectionMemoizationPreservesRelationsAndCopyOnWrite() {
        var layout = FieldLayout.forType(IndexedEntity.class);
        var base = LoadState.projection(layout, List.of("id", "field_64", "childList"));
        Set<String> mutable = new java.util.HashSet<>(Set.of("note"));
        var selected = base.withDynamicSelection(mutable);
        mutable.clear();
        assertTrue(selected.isLoaded("#note"));
        assertSame(selected, selected.withDynamicSelection(Set.of("note")));
        var caseInsensitive = new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("NOTE");
        var literalCase = selected.withDynamicSelection(caseInsensitive);
        assertNotSame(selected, literalCase);
        assertTrue(literalCase.isLoaded("#NOTE"));
        assertFalse(literalCase.isLoaded("#note"));
        var changed = selected.withDynamicSelection(Set.of("other"));
        assertNotSame(selected, changed);
        assertFalse(changed.isLoaded("#note"));
        assertTrue(changed.isLoaded("#other"));
        assertTrue(selected.isLoaded("#note"));
        assertTrue(changed.isLoaded("childList"));
        assertTrue(changed.isLoaded("field_64"));
        var fixedChange = changed.withLoaded("field_65", true);
        assertSame(fixedChange, fixedChange.withDynamicSelection(Set.of("other")));
        var cleared = changed.withDynamicSelection(Set.of());
        assertEquals(base, cleared);
        assertSame(cleared, cleared.withDynamicSelection(Set.of()));
        assertTrue(cleared.isLoaded("childList"));
        var fromProjection = LoadState.projection(layout, List.of("id", "#note", "childList"));
        assertSame(fromProjection, fromProjection.withDynamicSelection(Set.of("note")));
        assertEquals(fromProjection.hashCode(), fromProjection.withDynamicSelection(Set.of("note")).hashCode());
    }
    static final class IndexedEntity extends BaseEntity {
        public static final String __TEAQL_FIELD_LAYOUT_REVISION = "load-state-fixture-v1";
        public static final Map<String, Integer> __TEAQL_FIXED_FIELD_INDEXES = indexes();
        public static final Map<String, List<String>> __TEAQL_FIXED_FIELD_MAPPINGS = mappings();
        public static final Set<String> __TEAQL_FIXED_RELATION_NAMES = Set.of("childList");
        public static final FieldLayout __TEAQL_FIELD_LAYOUT = FieldLayout.installGenerated(FieldLayout.generated(
                IndexedEntity.class, __TEAQL_FIELD_LAYOUT_REVISION, __TEAQL_FIXED_FIELD_INDEXES,
                __TEAQL_FIXED_FIELD_MAPPINGS, __TEAQL_FIXED_RELATION_NAMES));
        private Object baseUrl;
        private static Map<String, Integer> indexes() {
            Map<String, Integer> indexes = new HashMap<>();
            indexes.put("id", 0);
            indexes.put("version", 1);
            indexes.put("base_url", 2);
            for (int i = 3; i < 130; i++) indexes.put("field_" + i, i);
            return Map.copyOf(indexes);
        }
        private static Map<String, List<String>> mappings() {
            Map<String, List<String>> mappings = new HashMap<>();
            for (String name : __TEAQL_FIXED_FIELD_INDEXES.keySet()) mappings.put(name, List.of(name, name));
            mappings.put("base_url", List.of("baseUrl", "legacy_url"));
            return Map.copyOf(mappings);
        }
        @Override
        public void __internalSet(String property, Object value) {
            if ("baseUrl".equals(property)) baseUrl = value;
            else super.__internalSet(property, value);
        }
        @Override
        public Object __internalGet(String property) {
            if ("baseUrl".equals(property)) return baseUrl;
            return super.__internalGet(property);
        }
    }

    static final class LateLayoutEntity extends BaseEntity { }

    @Test
    public void explicitInstallationIsSharedAndCannotReplaceAnAlreadyUsedLayout() {
        var installed = IndexedEntity.__TEAQL_FIELD_LAYOUT;
        assertSame(installed, FieldLayout.installGenerated(installed));
        var identical = FieldLayout.generated(IndexedEntity.class, installed.revision(),
                IndexedEntity.__TEAQL_FIXED_FIELD_INDEXES, IndexedEntity.__TEAQL_FIXED_FIELD_MAPPINGS,
                IndexedEntity.__TEAQL_FIXED_RELATION_NAMES);
        assertSame(installed, FieldLayout.installGenerated(identical));
        var changed = FieldLayout.generated(IndexedEntity.class, "another-revision",
                IndexedEntity.__TEAQL_FIXED_FIELD_INDEXES, IndexedEntity.__TEAQL_FIXED_FIELD_MAPPINGS,
                IndexedEntity.__TEAQL_FIXED_RELATION_NAMES);
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.installGenerated(changed));
        assertFalse(FieldLayout.forType(LateLayoutEntity.class).isGenerated());
        var late = FieldLayout.generated(LateLayoutEntity.class, "late", Map.of("id", 0, "version", 1),
                Map.of("id", List.of("id", "id"), "version", List.of("version", "version")), Set.of());
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.installGenerated(late));
    }

    @Test
    public void generatedPositionsAreIndependentOfAccessAndConcurrency() {
        int[] slots = {0, 31, 32, 63, 64, 65, 129};
        FieldLayout layout = FieldLayout.forType(IndexedEntity.class);
        java.util.stream.IntStream.range(0, 100).parallel().forEach(i -> {
            for (int slot : slots) assertEquals(Integer.valueOf(slot), layout.findIndex(name(slot)));
            assertSame(layout, FieldLayout.forType(IndexedEntity.class));
        });
        assertEquals(2, BaseEntity.loadedPropertyIndex(IndexedEntity.class, "baseUrl"));
        assertEquals(2, BaseEntity.loadedPropertyIndex(IndexedEntity.class, "legacy_url"));
    }

    @Test
    public void bitmapBoundariesAndOverflowNeverWrap() {
        FieldLayout layout = FieldLayout.forType(IndexedEntity.class);
        LoadState state = LoadState.projection(layout, List.of("id", "field_31", "field_32", "field_63"));
        assertTrue(state.bits() < 0L);
        assertTrue(state.overflow().isEmpty());
        assertFalse(state.isLoaded("field_64"));
        LoadState wide = state.withLoaded("field_64", true).withLoaded("field_65", true).withLoaded("field_129", true);
        assertEquals(state.bits(), wide.bits());
        assertEquals(Set.of(64, 65, 129), wide.overflow());
        LoadState cleared = wide.withLoaded("field_65", false);
        assertFalse(cleared.isLoaded("field_65"));
        assertTrue(wide.isLoaded("field_65"));
        assertThrows(UnsupportedOperationException.class, () -> wide.overflow().add(100));
    }

    @Test
    public void smartListSharesWholeSnapshotAndOnlyAvailabilityDetaches() {
        List<IndexedEntity> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            IndexedEntity row = new IndexedEntity();
            row.setProperty("baseUrl", i == 0 ? null : "row-" + i);
            row.markPropertyLoaded("field_64");
            row.markPropertyLoaded("#extra_note");
            rows.add(row);
        }
        SmartList<IndexedEntity> list = SmartList.takeOwnership(rows);
        LoadState shared = list.first().__internalLoadState();
        for (IndexedEntity row : list) assertSame(shared, row.__internalLoadState());
        assertTrue(list.first().isPropertyLoaded("baseUrl"));
        assertNull(list.first().getProperty("baseUrl"));
        assertSame(shared, list.get(1).__internalLoadState().withLoaded("baseUrl", true));
        list.get(1).setProperty("baseUrl", "changed");
        assertSame(shared, list.get(1).__internalLoadState());
        list.first().markPropertyLoaded("field_65");
        assertNotSame(shared, list.first().__internalLoadState());
        assertFalse(list.get(1).isPropertyLoaded("field_65"));
        assertNotSame(list.first().getEntityMutationLedger(), list.get(1).getEntityMutationLedger());
        assertTrue(shared.selectedNames().contains("#extra_note"));
        assertEquals(Set.of(64), shared.overflow());
    }

    @Test
    public void appendAndMixedProjectionNeverUnionStatesAndSurviveListLifetime() {
        IndexedEntity full = new IndexedEntity();
        full.setProperty("baseUrl", false);
        IndexedEntity minimal = new IndexedEntity();
        minimal.setProperty("id", 7L);
        SmartList<IndexedEntity> list = new SmartList<>(List.of(full, minimal));
        IndexedEntity anotherMinimal = new IndexedEntity();
        anotherMinimal.setProperty("id", 8L);
        list.add(anotherMinimal);
        assertSame(minimal.__internalLoadState(), anotherMinimal.__internalLoadState());
        assertFalse(minimal.isPropertyLoaded("baseUrl"));
        assertEquals(false, full.getProperty("baseUrl"));
        LoadState kept = minimal.__internalLoadState();
        list.setData(new ArrayList<>());
        assertSame(kept, minimal.__internalLoadState());
        assertTrue(minimal.isPropertyLoaded("id"));
    }

    @Test
    public void hydrateIndexMismatchFailsBeforeWritingAndUnknownNamesCannotCorruptBits() {
        IndexedEntity row = new IndexedEntity();
        assertThrows(IllegalArgumentException.class, () -> row.__internalHydrate("baseUrl", "bad", 1));
        assertFalse(row.isPropertyLoaded("baseUrl"));
        assertNull(row.getProperty("baseUrl"));
        assertThrows(IllegalArgumentException.class, () -> row.markPropertyLoaded("typo"));
        assertEquals(0L, row.__internalLoadState().bits());
    }

    @Test
    public void compiledProjectionIsSharedWithoutDirtyHydrationAndRollbackIsPrivate() {
        FieldLayout layout = FieldLayout.forType(IndexedEntity.class);
        LoadState shape = LoadState.projection(layout, List.of("id", "baseUrl"));
        IndexedEntity left = new IndexedEntity();
        IndexedEntity right = new IndexedEntity();
        for (IndexedEntity row : List.of(left, right)) {
            row.__internalUseLoadState(shape);
            row.__internalHydrate("id", row == left ? 1L : 2L, 0);
            row.__internalHydrate("baseUrl", "", 2);
            assertSame(shape, row.__internalLoadState());
            assertTrue(row.getUpdatedProperties().isEmpty());
        }
        left.__internalHydrate("version", 1L, 1);
        assertTrue(left.isPropertyLoaded("version"));
        left.__internalRestorePersistenceState(null, EntityStatus.PERSISTED, false);
        assertFalse(left.isPropertyLoaded("version"));
        assertFalse(right.isPropertyLoaded("version"));
        assertSame(shape, right.__internalLoadState());
    }

    @Test
    public void malformedDescriptorsAndIncompatibleLayoutsAreRejected() {
        Map<String, List<String>> mappings = Map.of("id", List.of("id", "id"), "version", List.of("version", "version"));
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.generated(IndexedEntity.class, "v1",
                Map.of("id", 0, "version", 0), mappings, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.generated(IndexedEntity.class, "v1",
                Map.of("id", 0, "version", 64), mappings, Set.of()));
        FieldLayout otherRevision = FieldLayout.generated(IndexedEntity.class, "v2",
                Map.of("id", 0, "version", 1), mappings, Set.of());
        IndexedEntity row = new IndexedEntity();
        assertThrows(IllegalArgumentException.class, () -> row.__internalUseLoadState(otherRevision.emptyState()));
        assertThrows(IllegalArgumentException.class,
                () -> row.__internalLoadState().layout().validateExpectedMembers(Set.of("id", "version")));
    }

    @Test
    public void ordinaryCompiledShapesRetainOneSnapshotAtAllRequiredRowScales() {
        FieldLayout layout = FieldLayout.forType(IndexedEntity.class);
        for (int count : new int[]{1, 100, 10_000}) {
            LoadState shape = LoadState.projection(layout, List.of("id", "baseUrl"));
            List<IndexedEntity> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                IndexedEntity row = new IndexedEntity();
                row.__internalUseLoadState(shape);
                row.__internalHydrate("id", (long) i + 1, 0);
                row.__internalHydrate("baseUrl", i == 0 ? null : "row-" + i, 2);
                assertSame(shape, row.__internalLoadState());
                assertTrue(row.__internalLoadState().overflow().isEmpty());
                assertTrue(row.__internalLoadState().selectedNames().isEmpty());
                rows.add(row);
            }
            SmartList<IndexedEntity> list = SmartList.takeOwnership(rows);
            for (IndexedEntity row : list) assertSame(shape, row.__internalLoadState());
        }
    }

    private static String name(int index) { return index == 0 ? "id" : "field_" + index; }
}
